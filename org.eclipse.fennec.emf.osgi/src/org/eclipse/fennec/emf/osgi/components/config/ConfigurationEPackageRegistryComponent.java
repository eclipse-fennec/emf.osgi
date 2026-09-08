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
package org.eclipse.fennec.emf.osgi.components.config;


import static org.eclipse.fennec.emf.osgi.constants.EMFNamespaces.PROP_RESOURCE_SET_FACTORY_NAME;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.components.DefaultEPackageRegistryComponent;
import org.eclipse.fennec.emf.osgi.components.SelfRegisteringServiceComponent;
import org.eclipse.fennec.emf.osgi.components.SwitchableEPackageRegistry;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.helper.DelegatingEPackageRegistry;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;

import aQute.bnd.annotation.service.ServiceCapability;

/**
 * An implementation of a package registry that delegates failed lookups to its parent registry,
 * the {@link EPackage.Registry} service selected by the <code>parentRegistry.target</code> filter.
 * This implementation is derived from the {@link DefaultEPackageRegistryComponent} to be configurable as OSGi component factory
 */
@Component(configurationPid=EMFNamespaces.EPACKAGE_REGISTRY_CONFIG_NAME, configurationPolicy=ConfigurationPolicy.REQUIRE)
@Designate(ocd = EPackageRegistryConfig.class, factory = true)
@ProviderType
@ServiceCapability(EPackage.Registry.class)
public class ConfigurationEPackageRegistryComponent extends SelfRegisteringServiceComponent{

	private static final Logger LOG = Logger.getLogger(ConfigurationEPackageRegistryComponent.class.getName());

	private final Set<EPackageConfigurator> ePackageConfigurators = new CopyOnWriteArraySet<>();

	/** The parent registry failed lookups are delegated to */
	private final SwitchableEPackageRegistry parentRegistry = new SwitchableEPackageRegistry();

	/** The reference of the service currently targeted by {@link #parentRegistry} */
	private final AtomicReference<ServiceReference<EPackage.Registry>> parentRegistryRef = new AtomicReference<>();

	private final BundleContext bundleContext;

	/** The configured name of this registry, used for logging */
	private final String registryName;

	/**
	 * The delegate registry.
	 */
	protected transient EPackage.Registry registry;

	/**
	 * Creates an instance that delegates failed lookups to its parent registry.
	 */
	@Activate
	public ConfigurationEPackageRegistryComponent(BundleContext ctx,
			Map<String, Object> properties) {
		super(ctx, (String) properties.get(PROP_RESOURCE_SET_FACTORY_NAME), properties);
		bundleContext = ctx;
		registryName = (String) properties.get(PROP_RESOURCE_SET_FACTORY_NAME);
		registry = new DelegatingEPackageRegistry(parentRegistry);
		registerService(ctx, EPackage.Registry.class, registry);
	}

	@Deactivate
	public void deactivate() {
		doDeactivate();
		ePackageConfigurators.clear();
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

	/**
	 * Adds the parent {@link EPackage.Registry}, delegates failed lookups to it and propagates
	 * its properties
	 * @param serviceRef the service reference of the parent registry
	 */
	@Reference(name = "parentRegistry",
			service = EPackage.Registry.class,
			cardinality = ReferenceCardinality.MANDATORY,
			policy = ReferencePolicy.DYNAMIC,
			target = "(default.resourceset.epackage.registry=true)",
			unbind = "removeParentRegistry",
			updated = "updateParentRegistry")
	protected void addParentRegistry(ServiceReference<EPackage.Registry> serviceRef) {
		switchParentRegistry(serviceRef);
		getPropertyContext().addSubContext(FrameworkUtil.asMap(serviceRef.getProperties()));
		updateRegistrationProperties();
	}

	/**
	 * Updates the propagated properties when the parent registry changes
	 * @param serviceRef the service reference of the parent registry
	 */
	protected void updateParentRegistry(ServiceReference<EPackage.Registry> serviceRef) {
		getPropertyContext().addSubContext(FrameworkUtil.asMap(serviceRef.getProperties()));
		updateRegistrationProperties();
	}

	/**
	 * Delegates failed lookups to the {@link EPackage.Registry} service behind the given reference.
	 * A target filter that made this registry its own parent is ignored - delegating to ourselves
	 * would turn every lookup into an endless recursion. The obtained service is released again in
	 * {@link #removeParentRegistry(ServiceReference)}.
	 * @param serviceRef the service reference of the parent registry
	 */
	private void switchParentRegistry(ServiceReference<EPackage.Registry> serviceRef) {
		EPackage.Registry parent = bundleContext.getService(serviceRef);
		if (parent == null) {
			LOG.log(Level.WARNING, "[{0}] The parent EPackage registry service is gone already, failed lookups are delegated to the static registry.",
					registryName);
		} else if (parent == registry) {
			LOG.log(Level.WARNING, "[{0}] The parentRegistry target matches the own registry service of this component, failed lookups are delegated to the static registry instead.",
					registryName);
		} else {
			parentRegistryRef.set(serviceRef);
			parentRegistry.setTarget(parent);
		}
	}

	/**
	 * Stops delegating to the {@link EPackage.Registry} service behind the given reference and
	 * releases it. A reference that has already been replaced by a newly bound parent only gets
	 * released, so the replacement stays in charge.
	 * @param serviceRef the service reference of the parent registry
	 */
	private void releaseParentRegistry(ServiceReference<EPackage.Registry> serviceRef) {
		if (parentRegistryRef.compareAndSet(serviceRef, null)) {
			parentRegistry.setTarget(null);
		}
		bundleContext.ungetService(serviceRef);
	}

	/**
	 * Stops delegating and removes the propagated properties when the parent registry is removed
	 * @param serviceRef the service reference of the parent registry
	 */
	protected void removeParentRegistry(ServiceReference<EPackage.Registry> serviceRef) {
		releaseParentRegistry(serviceRef);
		getPropertyContext().removeSubContext(FrameworkUtil.asMap(serviceRef.getProperties()));
		updateRegistrationProperties();
	}
}
