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
package org.gecko.emf.osgi.components.config;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.gecko.emf.osgi.EMFNamespaces;
import org.gecko.emf.osgi.EPackageConfigurator;
import org.gecko.emf.osgi.ResourceFactoryConfigurator;
import org.gecko.emf.osgi.ResourceSetConfigurator;
import org.gecko.emf.osgi.ResourceSetFactory;
import org.gecko.emf.osgi.ecore.EcoreConfigurator;
import org.gecko.emf.osgi.provider.DefaultResourceSetFactory;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
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
		version = ResourceSetFactory.GECKOPROJECTS_EMF_VERSION
		)
@ProviderType
public class ConfigurationResourceSetFactoryComponent extends DefaultResourceSetFactory {

	private Dictionary<String, Object> properties;

	private ComponentServiceObjects<Registry> resourceFactoryRegistryObjects;

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
			@Reference(name="ePackageRegistry", unbind = "unsetRegistry")
			EPackage.Registry registry,
			@Reference(name="resourceFactoryRegistry", unbind="unsetResourceFactoryRegistry", updated = "modifiedResourceFactoryRegistry")
			ComponentServiceObjects<Resource.Factory.Registry> resourceFactoryRegistryObjects
			) {
		this.resourceFactoryRegistryObjects = resourceFactoryRegistryObjects;
		super.setEPackageRegistry(registry);
		super.setResourceFactoryRegistry(resourceFactoryRegistryObjects.getService(), FrameworkUtil.asMap(resourceFactoryRegistryObjects.getServiceReference().getProperties()));
		EcoreConfigurator ecoreConfigurator = new EcoreConfigurator();
		addEPackageConfigurator(ecoreConfigurator, EcoreConfigurator.PROPERTIES);
		addResourceFactoryConfigurator(ecoreConfigurator, EcoreConfigurator.PROPERTIES);
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
		registerServices(ctx);
	}
	
	/**
	 * Called on component deactivation
	 */
	@Deactivate
	public void deactivate() {
		super.deactivate();
		this.resourceFactoryRegistryObjects.ungetService(resourceFactoryRegistry);
	}

	protected void unsetRegistry(org.eclipse.emf.ecore.EPackage.Registry registry) {
		super.unsetEPackageRegistry(registry);
	}
	
	public void modifiedResourceFactoryRegistry(Resource.Factory.Registry resourceFactoryRegistry, Map<String, Object> properties) {
		super.modifiedResourceFactoryRegistry(resourceFactoryRegistry, properties);
	}
	
	/**
	 * Removed the registry on shutdown
	 * @param resourceFactoryRegistry the registry to be removed
	 */
	@Override
	public void unsetResourceFactoryRegistry(Resource.Factory.Registry resourceFactoryRegistry) {
		super.unsetResourceFactoryRegistry(resourceFactoryRegistry);
	}

	/**
	 * Injects {@link EPackageConfigurator}, to register a new {@link EPackage}
	 * @param configurator the {@link EPackageConfigurator} to be registered
	 * @param properties the service properties
	 */
	@Override
	@Reference(name="ePackageConfigurator", policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.MULTIPLE, target="(!(emf.model.name=ecore))", updated = "modifyEPackageConfigurator", unbind = "removeEPackageConfigurator")
	public void addEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		super.addEPackageConfigurator(configurator, properties);
	}
	
	/**
	 * Injects {@link EPackageConfigurator}, to update a new {@link EPackage}
	 * @param configurator the {@link EPackageConfigurator} to be updated
	 * @param properties the service properties
	 */
	public void modifyEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		super.addEPackageConfigurator(configurator, properties);
	}

	/**
	 * Removes a {@link EPackageConfigurator} from the registry and unconfigures it
	 * @param configurator the configurator to be removed
	 * @param properties the service properties
	 */
	@Override
	public void removeEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		super.removeEPackageConfigurator(configurator, properties);
	}

	/**
	 * Adds a resource factory configurator to the registry 
	 * @param configurator the resource factory configurator to be registered
	 * @param properties the service properties
	 */
	@Override
	@Reference(name="resourceFactoryConfigurator", policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.MULTIPLE, target="(!(emf.model.name=ecore))", updated = "modifyResourceFactoryConfigurator")
	public void addResourceFactoryConfigurator(ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		super.addResourceFactoryConfigurator(configurator, properties);
	}
	
	/**
	 * Modifies a resource factory configurator to the registry 
	 * @param configurator the resource factory configurator to be updated
	 * @param properties the service properties
	 */
	public void modifyResourceFactoryConfigurator(ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		super.addResourceFactoryConfigurator(configurator, properties);
	}

	/**
	 * Removes a resource factory configurator from the registry
	 * @param configurator the resource factory configurator to be removed
	 * @param properties the service properties
	 */
	@Override
	public void removeResourceFactoryConfigurator(ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		super.removeResourceFactoryConfigurator(configurator, properties);
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
