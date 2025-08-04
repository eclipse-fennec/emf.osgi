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
import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.constants.VersionConstant;
import org.eclipse.fennec.emf.osgi.ecore.EcorePackagesRegistrator;
import org.eclipse.fennec.emf.osgi.provider.DefaultResourceSetFactory;
import org.osgi.annotation.bundle.Capability;
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
 * Implementation of a {@link ResourceSetFactory}. It hold the {@link EPackage} registry as well as the {@link Factory} registry.
 * {@link EPackage} are dynamically injected as {@link EPackageProvider} instance. 
 * {@link Factory} instance are injected dynamically as {@link ServiceReference} instance. So they can be registered using
 * their properties for contentTyp or fileExtension.
 * Third additional {@link ResourceSetConfigurator} instance can be injected to customize the {@link ResourceSet} for
 * further extension like custom serialization. 
 * @author Mark Hoffmann
 * @since 28.06.2017
 */
@Component(name="DefaultResourcesetFactory", enabled=true, immediate=true, service= {})
@Capability(
		namespace = EMFNamespaces.EMF_NAMESPACE,
		name = ResourceSetFactory.EMF_CAPABILITY_NAME,
		version = VersionConstant.FENNECPROJECTS_EMF_VERSION
		)
@ServiceCapability(value = ResourceSet.class)
@ServiceCapability(value = ResourceSetFactory.class)
public class DefaultResourceSetFactoryComponent extends DefaultResourceSetFactory implements RegistryPropertyListener {

	
	private ServiceReference<Registry> resourceFactoryRegistryReference;
	private BundleContext cxt;
	private ServiceReference<org.eclipse.emf.ecore.EPackage.Registry> defaultResourceSetRegistry;
	private ServiceReference<org.eclipse.emf.ecore.EPackage.Registry> staticRegistry;
	private RegistryTrackingService registryTracker;

	/**
	 * Called before component activation
	 * @param ctx the component context
	 */
	@Activate
	public DefaultResourceSetFactoryComponent(BundleContext ctx,
			@Reference(name="resourceSetEPackageRegistry", target = "(default.resourceset.epackage.registry=true)")
			ServiceReference<EPackage.Registry> defaultResourceSetRegistry,
			@Reference(name="staticEPackageRegistry", target = "(emf.default.epackage.registry=true)")
			ServiceReference<EPackage.Registry> staticRegistry,
			@Reference(name="resourceFactoryRegistry")
			ServiceReference<Resource.Factory.Registry> resourceFactoryRegistryReference,
			@Reference
			RegistryTrackingService registryTracker
			) {
		this.cxt = ctx;
		this.defaultResourceSetRegistry = defaultResourceSetRegistry;
		this.staticRegistry = staticRegistry;
		this.resourceFactoryRegistryReference = resourceFactoryRegistryReference;
		this.registryTracker = registryTracker;
		super.setStaticEPackageRegistryProperties(FrameworkUtil.asMap(defaultResourceSetRegistry.getProperties()));
		super.setEPackageRegistry(ctx.getService(defaultResourceSetRegistry), FrameworkUtil.asMap(defaultResourceSetRegistry.getProperties()));
		super.setResourceFactoryRegistry(ctx.getService(resourceFactoryRegistryReference), FrameworkUtil.asMap(resourceFactoryRegistryReference.getProperties()));
		//TODO: Tut dat note, dass das so rumoxidiert?
		EcorePackagesRegistrator.start();
		
		// Register for property change notifications on the services we care about
		Set<Long> trackedServiceIds = new HashSet<>();
		trackedServiceIds.add((Long) defaultResourceSetRegistry.getProperty(Constants.SERVICE_ID));
		trackedServiceIds.add((Long) staticRegistry.getProperty(Constants.SERVICE_ID));
		trackedServiceIds.add((Long) resourceFactoryRegistryReference.getProperty(Constants.SERVICE_ID));
		registryTracker.registerListener(this, trackedServiceIds);
		
		doActivate(cxt);
	}
	
	/**
	 * Called on component deactivation
	 */
	@Deactivate
	@Override
	public void deactivate() {
		registryTracker.unregisterListener(this);
		super.deactivate();
		cxt.ungetService(resourceFactoryRegistryReference);
		EcorePackagesRegistrator.stop();
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
