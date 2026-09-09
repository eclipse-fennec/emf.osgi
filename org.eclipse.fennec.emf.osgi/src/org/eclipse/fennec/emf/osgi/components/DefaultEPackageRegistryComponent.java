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
package org.eclipse.fennec.emf.osgi.components;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.RegistryPropertyListener;
import org.eclipse.fennec.emf.osgi.RegistryTrackingService;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.helper.DelegatingEPackageRegistry;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import aQute.bnd.annotation.service.ServiceCapability;

/**
 * An implementation of a package registry that delegates failed lookups to its parent, the static
 * {@link EPackage.Registry} service. The parent is a mandatory constructor level reference, so this
 * registry only exists as long as the parent it was built on: a parent that goes away or a target
 * filter that selects another one takes this registry service down with it, instead of silently
 * changing the set of resolvable packages underneath everyone who is currently using it.
 * {@link DefaultEPackageRegistryComponent} to be enabled as OSGi component
 */
@Component(name = DefaultEPackageRegistryComponent.NAME, service = {})
@ProviderType
@ServiceCapability(EPackage.Registry.class)
public class DefaultEPackageRegistryComponent extends SelfRegisteringServiceComponent implements RegistryPropertyListener {
	
	/** DEFAULT_E_PACKAGE_REGISTRY */
	public static final String NAME = "DefaultEPackageRegistry";

	private final Set<EPackageConfigurator> ePackageConfigurators = new CopyOnWriteArraySet<>();

	private final BundleContext bundleContext;

	/** The reference of the parent registry service failed lookups are delegated to */
	private final ServiceReference<EPackage.Registry> parentRegistryRef;

	/** Notifies us about property changes of the parent registry, so we can propagate them */
	private final RegistryTrackingService registryTracker;

	/**
	 * The delegate registry.
	 */
	protected transient EPackage.Registry registry;

	/**
	 * Creates an instance that delegates failed lookups to its parent registry.
	 * @param ctx the bundle context
	 * @param parentRegistryRef the reference of the parent registry, failed lookups are delegated to it
	 * @param registryTracker the tracker that reports property changes of the parent registry
	 */
	@Activate
	public DefaultEPackageRegistryComponent(BundleContext ctx,
			@Reference(name = "parentRegistry",
					service = EPackage.Registry.class,
					cardinality = ReferenceCardinality.MANDATORY,
					target = "(emf.default.epackage.registry=true)")
			ServiceReference<EPackage.Registry> parentRegistryRef,
			@Reference
			RegistryTrackingService registryTracker) {
		super(ctx, NAME, Map.of("default.resourceset.epackage.registry", true));
		this.bundleContext = ctx;
		this.parentRegistryRef = parentRegistryRef;
		this.registryTracker = registryTracker;

		EPackage.Registry parent = requireNonNull(ctx.getService(parentRegistryRef),
				"The parent EPackage registry service is gone, this registry cannot be activated without it");
		registry = new DelegatingEPackageRegistry(parent);
		getPropertyContext().addSubContext(FrameworkUtil.asMap(parentRegistryRef.getProperties()));
		registerService(ctx, EPackage.Registry.class, registry);

		registryTracker.registerListener(this, Set.of(parentServiceId()));
	}
	
	
	@Deactivate
	public void deactivate() {
		registryTracker.unregisterListener(this);
		doDeactivate();
		ePackageConfigurators.clear();
		bundleContext.ungetService(parentRegistryRef);
	}

	/**
	 * Adds {@link EPackageConfigurator}, to register a new {@link EPackage}
	 * @param configurator the {@link EPackageConfigurator} to be registered
	 * @param properties the service properties
	 */
	@Reference(name="ePackageConfigurator", policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.MULTIPLE, target="(" + EMFNamespaces.EMF_MODEL_SCOPE + "=" + EMFNamespaces.EMF_MODEL_SCOPE_RESOURCE_SET + ")", unbind = "removeEPackageConfigurator")
	protected void addEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		synchronized (ePackageConfigurators) {
			ePackageConfigurators.add(configurator);
			getPropertyContext().addSubContext(properties);
		}
		configurator.configureEPackage(registry);
		updateRegistrationProperties();
	}

	/**
	 * Removes a {@link EPackageConfigurator} from the registry and unconfigures it
	 * @param configurator the configurator to be removed
	 * @param modelInfo the model information
	 * @param properties the service properties
	 */
	protected void removeEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		synchronized (ePackageConfigurators) {
			ePackageConfigurators.remove(configurator);
			getPropertyContext().removeSubContext(properties);
		}
		configurator.unconfigureEPackage(registry);
		updateRegistrationProperties();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.fennec.emf.osgi.RegistryPropertyListener#onRegistryPropertiesChanged(long, java.lang.String, java.util.Map)
	 */
	@Override
	public void onRegistryPropertiesChanged(long serviceId, String serviceName, Map<String, Object> newProperties) {
		if (serviceId == parentServiceId()) {
			getPropertyContext().addSubContext(newProperties);
			updateRegistrationProperties();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.fennec.emf.osgi.RegistryPropertyListener#onRegistryServiceRemoved(long, java.lang.String)
	 */
	@Override
	public void onRegistryServiceRemoved(long serviceId, String serviceName) {
		// The parent is a static mandatory reference, losing it deactivates this component
	}

	/**
	 * Returns the service id of the parent registry
	 * @return the service id of the parent registry
	 */
	private long parentServiceId() {
		return (Long) parentRegistryRef.getProperty(Constants.SERVICE_ID);
	}
}
