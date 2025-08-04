/**
 * Copyright (c) 2012 - 2023 Data In Motion and others.
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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource.Factory;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.ecore.GeckoXMLResourceFactory;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.util.converter.Converters;

import aQute.bnd.annotation.service.ServiceCapability;

/**
 * Component for the {@link ResourceFactoryRegistryImpl}. We add new Resource.Factories dynamically. We store the service properties of each injected factory individually and cleaning them up afterwards. 
 * @author Mark Hoffmann
 * @since 25.07.2017
 */
@Component(name=DefaultResourceFactoryRegistryComponent.COMPONENT_NAME)
@ServiceCapability(value = Registry.class)
@ProviderType
public class DefaultResourceFactoryRegistryComponent extends SelfRegisteringServiceComponent {

	/** DEFAULT_RESOURCE_FACTORY_REGISTRY */
	public static final String COMPONENT_NAME = "DefaultResourceFactoryRegistry";
	private static Logger logger = Logger.getLogger(DefaultResourceFactoryRegistryComponent.class.getName());
	private final Registry registry;
	/**
	 * Creates a new instance.
	 */
	@Activate
	public DefaultResourceFactoryRegistryComponent(BundleContext ctx,
			@Reference(name="ePackageRegistry")
			EPackage.Registry packageRegistry) {
		super(COMPONENT_NAME, Collections.emptyMap());
		registry = new ResourceFactoryRegistryImpl();
		registerService(ctx, Registry.class, registry);
		addFactory(new GeckoXMLResourceFactory(packageRegistry), GeckoXMLResourceFactory.PROPERTIES);
	}
	
	@Deactivate
	public void deactivate() {
		doDeactivate();
	}
	
	/**
	 * We let inject the resource factories. The better services wins.
	 * @param factory the factory to add
	 * @param properties the factories service properties
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.GREEDY, 
			target = 
				"(|" + 
					"( " + EMFNamespaces.EMF_MODEL_CONTENT_TYPE + "=*) " + 
					"( " + EMFNamespaces.EMF_MODEL_FILE_EXT + "=*) " + 
					"( " + EMFNamespaces.EMF_MODEL_PROTOCOL + "=*) " + 
				")")
	public void addFactory(Factory factory, Map<String, Object> properties) {
		EMFConfigurator configuration = Converters.standardConverter().convert(properties).to(EMFConfigurator.class);
		Arrays.asList(configuration.contentType()).forEach(s -> registry.getContentTypeToFactoryMap().put(s, factory)); 
		Arrays.asList(configuration.fileExtension()).forEach(s -> registry.getExtensionToFactoryMap().put(s, factory)); 
		Arrays.asList(configuration.protocol()).forEach(s -> registry.getProtocolToFactoryMap().put(s, factory));
		getPropertyContext().addSubContext(properties);
		updateRegistrationProperties();
	}
	
	/**
	 * Removes a factory
	 * @param factory the factory to be removed
	 * @param properties the service properties 
	 */
	public void removeFactory(Factory factory, Map<String, Object> properties) {
		EMFConfigurator configuration = Converters.standardConverter().convert(properties).to(EMFConfigurator.class);
		Arrays.asList(configuration.contentType()).forEach(s->verifyRemove(registry.getContentTypeToFactoryMap(), s, factory)); 
		Arrays.asList(configuration.fileExtension()).forEach(s->verifyRemove(registry.getExtensionToFactoryMap(), s, factory)); 
		Arrays.asList(configuration.protocol()).forEach(s->verifyRemove(registry.getProtocolToFactoryMap(), s, factory));
		getPropertyContext().removeSubContext(properties);
		updateRegistrationProperties();
	}
	
	/**
	 * We allow overwriting of resource factories. The best one wins. For the removal we have to check
	 * @param factoryMap
	 * @param parameter
	 * @param compare
	 */
	private void verifyRemove(Map<String, Object> factoryMap, String parameter, Object compare) {
		requireNonNull(factoryMap);
		requireNonNull(compare);
		Object removed = factoryMap.get(parameter);
		if(nonNull(removed) && compare.equals(removed)) {
			factoryMap.remove(parameter);
		} else {
			logger.fine("Cannot remove the factory, because a newer on was already set");
		}
	}
}