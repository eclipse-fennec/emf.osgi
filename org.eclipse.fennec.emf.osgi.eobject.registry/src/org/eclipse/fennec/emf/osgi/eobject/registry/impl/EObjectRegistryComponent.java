/********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Data In Motion Consulting - initial implementation
 ********************************************************************/
package org.eclipse.fennec.emf.osgi.eobject.registry.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectProvider;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryConstants;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryListener;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;

/**
 * One registry instance per factory configuration. The component itself is not the
 * service: it runs the configured initial provider on a private executor (activation
 * never blocks) and registers the registry manually under both faces -
 * {@link EObjectRegistry} and {@link EObjectRegistryWriter} - only when the initial
 * load completed successfully. Consumers therefore never observe a half-loaded
 * registry, and dynamic sources referencing the writer by name cannot write before
 * initialization. A failed load is logged and the registry stays unpublished - the
 * absent service is the observable signal.
 * <p>
 * The initial provider is a static, mandatory, constructor-injected reference: SCR does
 * the matching and waiting, and a missing provider shows up as an unsatisfied reference
 * in {@code scr:info}. There is deliberately no {@code @Modified} method - any
 * configuration change restarts the instance, which makes {@code name} identity by
 * construction.
 *
 * @author Data In Motion Consulting
 */
@Component(name = "EObjectRegistry", configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = EObjectRegistryConfig.class, factory = true)
public class EObjectRegistryComponent {

	private static final Logger logger = Logger.getLogger(EObjectRegistryComponent.class.getName());

	private final BundleContext ctx;
	private final EObjectRegistryConfig config;
	private final EObjectRegistryImpl registry;
	private final ExecutorService executor;
	private final Object lifecycleLock = new Object();
	private ServiceRegistration<?> registration;
	private boolean active = true;

	@Activate
	public EObjectRegistryComponent(BundleContext ctx,
			@Reference(name = "initialProvider") EObjectProvider initialProvider, EObjectRegistryConfig config) {
		this.ctx = Objects.requireNonNull(ctx, "ctx");
		Objects.requireNonNull(initialProvider, "initialProvider");
		this.config = Objects.requireNonNull(config, "config");
		if (config.name() == null || config.name().isBlank()) {
			throw new IllegalArgumentException("EObjectRegistry configuration requires a non-blank name");
		}
		registry = new EObjectRegistryImpl(config.name());
		executor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "eobject-registry-" + config.name());
			thread.setDaemon(true);
			return thread;
		});
		executor.execute(() -> initialLoad(initialProvider));
	}

	@Deactivate
	public void deactivate() {
		synchronized (lifecycleLock) {
			active = false;
			if (registration != null) {
				try {
					registration.unregister();
				} catch (IllegalStateException e) {
					// already unregistered
				}
				registration = null;
			}
		}
		executor.shutdownNow();
		try {
			executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void initialLoad(EObjectProvider initialProvider) {
		try {
			initialProvider.load(registry).join();
		} catch (Exception e) {
			logger.log(Level.SEVERE, String.format(
					"Registry %s: initial load failed - the registry stays unpublished", config.name()), e);
			return;
		}
		synchronized (lifecycleLock) {
			if (!active) {
				return;
			}
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME, config.name());
			if (config.content_types() != null && config.content_types().length > 0) {
				props.put(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_CONTENT_TYPES, config.content_types());
			}
			registration = ctx.registerService(
					new String[] { EObjectRegistry.class.getName(), EObjectRegistryWriter.class.getName() }, registry,
					props);
			logger.info(() -> String.format("Registry %s: initial load complete (%d entries) - services published",
					config.name(), registry.entries().size()));
		}
	}

	/**
	 * Listener whiteboard: listeners carry the name of the registry they want to
	 * observe; only matching ones are bound (a factory component cannot parameterize a
	 * reference target per instance). Replay of current content happens inside
	 * {@code addListener} - a listener bound while the initial load is still running
	 * simply sees the loading entries as live events.
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeListener")
	void addListener(EObjectRegistryListener listener, Map<String, Object> properties) {
		if (matchesName(properties)) {
			registry.addListener(listener);
		}
	}

	void removeListener(EObjectRegistryListener listener) {
		registry.removeListener(listener);
	}

	private boolean matchesName(Map<String, Object> properties) {
		Object value = properties.get(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME);
		if (value instanceof String single) {
			return config.name().equals(single);
		}
		if (value instanceof String[] multiple) {
			return Arrays.asList(multiple).contains(config.name());
		}
		if (value instanceof Collection<?> collection) {
			return collection.contains(config.name());
		}
		return false;
	}
}
