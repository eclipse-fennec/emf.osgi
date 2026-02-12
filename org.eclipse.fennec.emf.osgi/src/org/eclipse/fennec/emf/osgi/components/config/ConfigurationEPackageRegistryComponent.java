/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.emf.osgi.components.config;


import static org.eclipse.fennec.emf.osgi.constants.EMFNamespaces.PROP_RESOURCE_SET_FACTORY_NAME;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.fennec.emf.osgi.components.SelfRegisteringServiceComponent;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
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

import aQute.bnd.annotation.service.ServiceCapability;

/**
 * An implementation of a package registry that can delegate failed lookup to another registry.
 * This implementation is derived from the default {@link ConfigurationEPackageRegistryComponent} to be enabled as OSGi component
 */
@Component(configurationPid=EMFNamespaces.EPACKAGE_REGISTRY_CONFIG_NAME, configurationPolicy=ConfigurationPolicy.REQUIRE)
@ProviderType
@ServiceCapability(EPackage.Registry.class)
public class ConfigurationEPackageRegistryComponent extends SelfRegisteringServiceComponent{

	private static final long serialVersionUID = 1L;

	private final Set<EPackageConfigurator> ePackageConfigurators = new CopyOnWriteArraySet<>();
	
	/**
	 * The delegate registry.
	 */
	protected transient EPackage.Registry registry;

	/**
	 * Creates a non-delegating instance.
	 */
	@Activate
	public ConfigurationEPackageRegistryComponent(BundleContext ctx,
			Map<String, Object> properties) {
		super(ctx, (String) properties.get(PROP_RESOURCE_SET_FACTORY_NAME), properties);
		registry = new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
		registerService(ctx, EPackage.Registry.class, registry);
	}

	@Deactivate
	public void deactivate() {
		doDeactivate();
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
	 * Adds the parent {@link EPackage.Registry} and propagates its properties
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
		Map<String, Object> properties = FrameworkUtil.asMap(serviceRef.getProperties());
		getPropertyContext().addSubContext(properties);
		updateRegistrationProperties();
	}

	/**
	 * Updates the propagated properties when the parent registry changes
	 * @param serviceRef the service reference of the parent registry
	 */
	protected void updateParentRegistry(ServiceReference<EPackage.Registry> serviceRef) {
		Map<String, Object> properties = FrameworkUtil.asMap(serviceRef.getProperties());
		getPropertyContext().addSubContext(properties);
		updateRegistrationProperties();
	}

	/**
	 * Removes the propagated properties when the parent registry is removed
	 * @param serviceRef the service reference of the parent registry
	 */
	protected void removeParentRegistry(ServiceReference<EPackage.Registry> serviceRef) {
		Map<String, Object> properties = FrameworkUtil.asMap(serviceRef.getProperties());
		getPropertyContext().removeSubContext(properties);
		updateRegistrationProperties();
	}
}
