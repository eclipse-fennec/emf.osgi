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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.RegistryPropertyListener;
import org.eclipse.fennec.emf.osgi.RegistryTrackingService;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.constants.VersionConstant;
import org.eclipse.fennec.emf.osgi.ecore.EcorePackagesRegistrator;
import org.eclipse.fennec.emf.osgi.provider.DefaultResourceSetFactory;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Implementation of a {@link ResourceSetFactory}. It hold the {@link EPackage} registry as well as the {@link Factory} registry.
 * {@link EPackage} are dynamically injected as {@link EPackageProvider} instance. 
 * {@link Factory} instance are injected dynamically as {@link ServiceReference} instance. So they can be registered using
 * their properties for contentTyp or fileExtension.
 * Third additional {@link ResourceSetConfigurator} instance can be injected to customize the {@link ResourceSet} for
 * further extension like custom serialization. 
 * @author Mark Hoffmann
 * @since 28.06.2017
 */
@Component(service= {}, configurationPid=EMFNamespaces.RESOURCE_SET_FACTORY_CONFIG_NAME, configurationPolicy=ConfigurationPolicy.REQUIRE)
@Capability(
		namespace = EMFNamespaces.EMF_NAMESPACE,
		name = ResourceSetFactory.EMF_CAPABILITY_NAME,
		version = VersionConstant.FENNECPROJECTS_EMF_VERSION
		)
@ProviderType
public class ConfigurationResourceSetFactoryComponent extends DefaultResourceSetFactory implements RegistryPropertyListener {

	private Dictionary<String, Object> properties;
	private BundleContext cxt;
	private ServiceReference<org.eclipse.emf.ecore.EPackage.Registry> defaultResourceSetRegistry;
	private ServiceReference<org.eclipse.emf.ecore.EPackage.Registry> staticRegistry;
	private ServiceReference<Resource.Factory.Registry> resourceFactoryRegistryReference;
	private RegistryTrackingService registryTracker;

	/**
	 * Creates a new instance.
	 */
	public ConfigurationResourceSetFactoryComponent() {
	}
	
	/**
	 * Called before component activation
	 * @param ctx the component context
	 */
	@Activate
	public ConfigurationResourceSetFactoryComponent(ComponentContext ctx,
			@Reference(name="resourceSetePackageRegistry", unbind = "unsetResourceSetPackageRegistry", target = "(" + EMFNamespaces.EMF_MODEL_SCOPE +"=" + EMFNamespaces.EMF_MODEL_SCOPE_RESOURCE_SET + ")")
			ServiceReference<EPackage.Registry> defaultResourceSetRegistry,
			@Reference(name="staticEPackageRegistry", unbind = "unsetStaticPackageRegistry", target = "(emf.default.epackage.registry=true)")
			ServiceReference<EPackage.Registry> staticRegistry,
			@Reference(name="resourceFactoryRegistry")
			ServiceReference<Resource.Factory.Registry> resourceFactoryRegistryReference,
			@Reference
			RegistryTrackingService registryTracker
			) {
		this.cxt = ctx.getBundleContext();
		this.defaultResourceSetRegistry = defaultResourceSetRegistry;
		this.staticRegistry = staticRegistry;
		this.resourceFactoryRegistryReference = resourceFactoryRegistryReference;
		this.registryTracker = registryTracker;
		
		// Get the actual services and set up the factory
		EPackage.Registry registry = cxt.getService(defaultResourceSetRegistry);
		Resource.Factory.Registry resourceFactoryRegistry = cxt.getService(resourceFactoryRegistryReference);
		
		super.setStaticEPackageRegistryProperties(FrameworkUtil.asMap(defaultResourceSetRegistry.getProperties()));
		super.setEPackageRegistry(registry, FrameworkUtil.asMap(defaultResourceSetRegistry.getProperties()));
		super.setResourceFactoryRegistry(resourceFactoryRegistry, FrameworkUtil.asMap(resourceFactoryRegistryReference.getProperties()));
		EcorePackagesRegistrator.start();
		
		// Register for property change notifications on the services we care about
		Set<Long> trackedServiceIds = new HashSet<>();
		trackedServiceIds.add((Long) defaultResourceSetRegistry.getProperty(Constants.SERVICE_ID));
		trackedServiceIds.add((Long) staticRegistry.getProperty(Constants.SERVICE_ID));
		trackedServiceIds.add((Long) resourceFactoryRegistryReference.getProperty(Constants.SERVICE_ID));
		registryTracker.registerListener(this, trackedServiceIds);
	}
	
	/*
	 * We have a two step activation for history reasons. We use the constructor injection to 
	 * make sure everything mandatory is available before activation has there had been cases, 
	 * where mandatory fields have been null at activation for some reason. Felix SCR will also
	 * look for a method named activate which is present through the DefaultResourceSetFactory
	 * and will call it after the Constructor. So to avoid activating this twice by accident, 
	 * we will name the method here explicitly.
	 *
	 * (non-Javadoc)
	 * @see org.gecko.emf.osgi.provider.DefaultResourceSetFactory#activate(org.osgi.service.component.ComponentContext)
	 */
	@Activate
	public void activate(ComponentContext ctx) {
		properties = ctx.getProperties();
		doActivate(ctx.getBundleContext());
	}
	
	/**
	 * Called on component deactivation
	 */
	@Override
	@Deactivate
	public void deactivate() {
		registryTracker.unregisterListener(this);
		super.deactivate();
		if (cxt != null) {
			cxt.ungetService(defaultResourceSetRegistry);
			cxt.ungetService(staticRegistry);
			cxt.ungetService(resourceFactoryRegistryReference);
		}
		EcorePackagesRegistrator.stop();
	}

	protected void unsetRegistry(org.eclipse.emf.ecore.EPackage.Registry registry) {
		// Handle registry unset if needed
	}
	
	// Implementation of RegistryPropertyListener interface
	
	@Override
	public void onRegistryPropertiesChanged(long serviceId, String serviceName, Map<String, Object> newProperties) {
		// Determine which registry changed and call appropriate parent method
		if (serviceId == (Long) defaultResourceSetRegistry.getProperty(Constants.SERVICE_ID)) {
			super.updateEPackageRegistry(newProperties);
		} else if (serviceId == (Long) staticRegistry.getProperty(Constants.SERVICE_ID)) {
			super.updateStaticEPackageRegistry(newProperties);
		} else if (serviceId == (Long) resourceFactoryRegistryReference.getProperty(Constants.SERVICE_ID)) {
			super.modifiedResourceFactoryRegistry(newProperties);
		}
	}

	@Override
	public void onRegistryServiceRemoved(long serviceId, String serviceName) {
		// Handle service removal if needed
		// For now, the constructor-injected services should remain available
		// until component deactivation, so this may not need special handling
	}
	
	

	/**
	 * Inject a {@link Registry} for resource factories
	 * @param resourceFactoryRegistry the resource factory to be injected
	 */
	@Override
	@Reference(policy=ReferencePolicy.STATIC, unbind="unsetResourceFactoryRegistry", updated = "modifiedResourceFactoryRegistry")
	public void setResourceFactoryRegistry(Resource.Factory.Registry resourceFactoryRegistry, Map<String, Object> properties) {
//		do nothing here - registry is injected via constructor
	}
	
	@Override
	public void modifiedResourceFactoryRegistry(Map<String, Object> properties) {
		super.modifiedResourceFactoryRegistry(properties);
	}
	
	/**
	 * Removed the registry on shutdown
	 * @param resourceFactoryRegistry the registry to be removed
	 */
	@Override
	public void unsetResourceFactoryRegistry(Resource.Factory.Registry resourceFactoryRegistry, Map<String, Object> properties) {
		super.unsetResourceFactoryRegistry(resourceFactoryRegistry, properties);
	}

	/**
	 * Injects {@link EPackageConfigurator}, to register a new {@link EPackage}
	 * @param configurator the {@link EPackageConfigurator} to be registered
	 * @param properties the service properties
	 */
	@Reference(name="ePackageConfigurator", policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.MULTIPLE, target="(" + EMFNamespaces.EMF_MODEL_SCOPE + "=" + EMFNamespaces.EMF_MODEL_SCOPE_RESOURCE_SET + ")", updated = "modifyEPackageConfigurator", unbind = "removeEPackageConfigurator")
	public void addEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		// Handle EPackage configurator addition
	}
	
	/**
	 * Injects {@link EPackageConfigurator}, to update a new {@link EPackage}
	 * @param configurator the {@link EPackageConfigurator} to be updated
	 * @param properties the service properties
	 */
	public void modifyEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		// Handle EPackage configurator modification
	}

	/**
	 * Removes a {@link EPackageConfigurator} from the registry and unconfigures it
	 * @param configurator the configurator to be removed
	 * @param properties the service properties
	 */
	public void removeEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		// Handle EPackage configurator removal
	}

	/**
	 * Adds new {@link ResourceSetConfigurator} to this factory
	 * @param resourceSetConfigurator the new configurator to be added
	 * @param properties the service properties
	 */
	@Override
	@Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.MULTIPLE, updated = "modifyResourceSetConfigurator")
	public void addResourceSetConfigurator(ResourceSetConfigurator resourceSetConfigurator, Map<String, Object> properties) {
		super.addResourceSetConfigurator(resourceSetConfigurator, properties);
	}
	
	/**
	 * Modifies new {@link ResourceSetConfigurator} to this factory
	 * @param resourceSetConfigurator the new configurator to be modified
	 * @param properties the service properties
	 */
	public void modifyResourceSetConfigurator(ResourceSetConfigurator resourceSetConfigurator, Map<String, Object> properties) {
		super.addResourceSetConfigurator(resourceSetConfigurator, properties);
	}

	/**
	 * Removes a {@link ResourceSetConfigurator} from the list for this factory
	 * @param resourceSetConfigurator
	 * @param properties the service properties
	 */
	@Override
	public void removeResourceSetConfigurator(ResourceSetConfigurator resourceSetConfigurator, Map<String, Object> properties) {
		super.removeResourceSetConfigurator(resourceSetConfigurator, properties);
	}

	@Override
	protected Dictionary<String, Object> getDictionary() {
		Dictionary<String, Object> props = super.getDictionary();
		Enumeration<String> keys = properties.keys();
		while(keys.hasMoreElements()) {
			String key = keys.nextElement();
			props.put(key, properties.get(key));
		}
		return props;
	}
	

}
