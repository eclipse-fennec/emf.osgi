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
package org.eclipse.fennec.emf.osgi.components;

import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.constants.VersionConstant;
import org.eclipse.fennec.emf.osgi.ecore.EcorePackagesRegistrator;
import org.eclipse.fennec.emf.osgi.provider.DefaultResourceSetFactory;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import aQute.bnd.annotation.service.ServiceCapability;

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
@Component(name="DefaultResourcesetFactory", enabled=true, immediate=true, service= {}, reference = {
		@Reference(name="ResourceFactoryRegistry", policy=ReferencePolicy.STATIC, unbind="unsetResourceFactoryRegistry", updated = "modifiedResourceFactoryRegistry")
})
@Capability(
		namespace = EMFNamespaces.EMF_NAMESPACE,
		name = ResourceSetFactory.EMF_CAPABILITY_NAME,
		version = VersionConstant.GECKOPROJECTS_EMF_VERSION
		)
@ServiceCapability(value = ResourceSet.class)
@ServiceCapability(value = ResourceSetFactory.class)
public class DefaultResourceSetFactoryComponent extends DefaultResourceSetFactory {

	
	private ServiceReference<Registry> resourceFactoryRegistryReference;
	private BundleContext cxt;
	private ServiceReference<org.eclipse.emf.ecore.EPackage.Registry> defaultResourceSetRegistry;
	private ServiceReference<org.eclipse.emf.ecore.EPackage.Registry> staticRegistry;

	/**
	 * Called before component activation
	 * @param ctx the component context
	 */
	@Activate
	public DefaultResourceSetFactoryComponent(BundleContext ctx,
			@Reference(name="resourceSetEPackageRegistry")
			ServiceReference<EPackage.Registry> defaultResourceSetRegistry,
			@Reference(name="staticEPackageRegistry")
			ServiceReference<EPackage.Registry> staticRegistry,
			@Reference(name="resourceFactoryRegistry")
			ServiceReference<Resource.Factory.Registry> resourceFactoryRegistryReference
			) {
		this.cxt = ctx;
		this.defaultResourceSetRegistry = defaultResourceSetRegistry;
		this.staticRegistry = staticRegistry;
		this.resourceFactoryRegistryReference = resourceFactoryRegistryReference;
		super.setStaticEPackageRegistryProperties(FrameworkUtil.asMap(defaultResourceSetRegistry.getProperties()));
		super.setEPackageRegistry(ctx.getService(defaultResourceSetRegistry), FrameworkUtil.asMap(defaultResourceSetRegistry.getProperties()));
		super.setResourceFactoryRegistry(ctx.getService(resourceFactoryRegistryReference), FrameworkUtil.asMap(resourceFactoryRegistryReference.getProperties()));
		//TODO: Tut dat note, dass das so rumoxidiert?
		EcorePackagesRegistrator.start();
		doActivate(cxt);
	}
	
	/**
	 * Called on component deactivation
	 */
	@Deactivate
	@Override
	public void deactivate() {
		super.deactivate();
		cxt.ungetService(resourceFactoryRegistryReference);
		EcorePackagesRegistrator.stop();
	}

	/**
	 * This is a bit of a hack. To make sure that we have the Registry as early as possible, we use constructor Injection. 
	 * We want to know about the property updates as well, which is not possible for constructor injected references. 
	 * We know that our Default registry is a singleton. Thus, we create a second Reference, that does nothing on set, 
	 * but reacts to changes of the properties. Not nice, but it works.   
	 */
	@Reference(policy=ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY, unbind="unsetEPackageRegistryProperties", updated = "modifieEPackageRegistryProperties")
	public void setEPackageRegistryProperties(Map<String, Object> properties) {
//		Do nothing here
	}

	public void modifieEPackageRegistryProperties(Map<String, Object> properties) {
//		Do nothing here
	}

	public void unsetEPackageRegistryProperties(Map<String, Object> properties) {
//		Do nothing here
	}

	/**
	 * This is a bit of a hack. To make sure that we have the Registry as early as possible, we use constructor Injection. 
	 * We want to know about the property updates as well, which is not possible for constructor injected references. 
	 * We know that our Default registry is a singleton. Thus, we create a second Reference, that does nothing on set, 
	 * but reacts to changes of the properties. Not nice, but it works.   
	 */
	@Reference(policy=ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY, unbind="unsetEPackageRegistryProperties", updated = "modifieEPackageRegistryProperties")
	public void setStaticEPackageRegistryProperties(EPackage.Registry registry, Map<String, Object> properties) {
//		Do nothing here
	}
	
	public void modifieStaticEPackageRegistryProperties(Map<String, Object> properties) {
//		Do nothing here
	}
	
	public void unsetStaticEPackageRegistryProperties(Map<String, Object> properties) {
//		Do nothing here
	}
	

	/**
	 * This is a bit of a hack. To make sure that we have the Registry as early as possible, we use constructor Injection. 
	 * We want to know about the property updates as well, which is not possible for constructor injected references. 
	 * We know that our Default registry is a singleton. Thus, we create a second Reference, that does nothing on set, 
	 * but reacts to changes of the properties. Not nice, but it works.   
	 * @param resourceFactoryRegistry the resource factory to be injected
	 */
	@Override
	@Reference(policy=ReferencePolicy.STATIC, unbind="unsetResourceFactoryRegistry", updated = "modifiedResourceFactoryRegistry")
	public void setResourceFactoryRegistry(Resource.Factory.Registry resourceFactoryRegistry, Map<String, Object> properties) {
//		Do nothing here
	}
	
	/**
	 * Removed the registry on shutdown
	 * @param resourceFactoryRegistry the registry to be removed
	 */
	@Override
	public void unsetResourceFactoryRegistry(Resource.Factory.Registry resourceFactoryRegistry, Map<String, Object> properties) {
		super.unsetResourceFactoryRegistry(resourceFactoryRegistry, properties);
		cxt.ungetService(resourceFactoryRegistryReference);
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
}
