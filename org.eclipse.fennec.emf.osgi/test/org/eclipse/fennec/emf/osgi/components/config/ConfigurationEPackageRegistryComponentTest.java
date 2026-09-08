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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Tests the delegation of the {@link ConfigurationEPackageRegistryComponent} to its bound
 * parent {@link EPackage.Registry} service, see issue #105.
 */
class ConfigurationEPackageRegistryComponentTest {

	private static final String OWN_NS_URI = "http://example.org/fennec/test/own";
	private static final String PARENT_NS_URI = "http://example.org/fennec/test/parent";
	private static final String OTHER_PARENT_NS_URI = "http://example.org/fennec/test/otherparent";
	private static final String SINGLETON_NS_URI = "http://example.org/fennec/test/singleton";
	private static final String SHADOWED_NS_URI = "http://example.org/fennec/test/shadowed";

	private static final Map<String, Object> CONFIGURATOR_PROPERTIES = Map.of(Constants.SERVICE_ID,
			Long.valueOf(201), EMFNamespaces.EMF_NAME, "ownmodel");

	private final EPackage parentPackage = createPackage("parent", PARENT_NS_URI);
	private final EPackage otherParentPackage = createPackage("otherparent", OTHER_PARENT_NS_URI);
	private final EPackage singletonPackage = createPackage("singleton", SINGLETON_NS_URI);
	private final EPackage ownPackage = createPackage("own", OWN_NS_URI);
	private final EPackage shadowingPackage = createPackage("shadowing", SHADOWED_NS_URI);
	private final EPackage shadowedPackage = createPackage("shadowed", SHADOWED_NS_URI);

	private final EPackage.Registry parentRegistry = new EPackageRegistryImpl();
	private final EPackage.Registry otherParentRegistry = new EPackageRegistryImpl();

	@SuppressWarnings("unchecked")
	private final ServiceRegistration<EPackage.Registry> registration = mock(ServiceRegistration.class);

	private BundleContext bundleContext;
	private ConfigurationEPackageRegistryComponent component;
	private EPackage.Registry registryService;
	private ServiceReference<EPackage.Registry> parentReference;
	private ServiceReference<EPackage.Registry> otherParentReference;

	@BeforeEach
	void setUp() {
		parentRegistry.put(PARENT_NS_URI, parentPackage);
		parentRegistry.put(SHADOWED_NS_URI, shadowedPackage);
		otherParentRegistry.put(OTHER_PARENT_NS_URI, otherParentPackage);
		EPackage.Registry.INSTANCE.put(SINGLETON_NS_URI, singletonPackage);

		bundleContext = mock(BundleContext.class);
		when(bundleContext.getBundles()).thenReturn(new Bundle[0]);
		when(bundleContext.registerService(eq(EPackage.Registry.class), any(EPackage.Registry.class), any()))
				.thenReturn(registration);

		component = new ConfigurationEPackageRegistryComponent(bundleContext,
				Map.of(PROP_RESOURCE_SET_FACTORY_NAME, "test"));
		registryService = capturedRegistryService();

		parentReference = reference(parentRegistry, 101, "parentmodel");
		otherParentReference = reference(otherParentRegistry, 102, null);
	}

	@AfterEach
	void tearDown() {
		EPackage.Registry.INSTANCE.remove(SINGLETON_NS_URI);
	}

	@Test
	void resolvesPackageFromBoundParent() {
		component.addParentRegistry(parentReference);

		assertSame(parentPackage, registryService.getEPackage(PARENT_NS_URI));
		assertSame(parentPackage.getEFactoryInstance(), registryService.getEFactory(PARENT_NS_URI));
		assertSame(parentPackage, registryService.get(PARENT_NS_URI));
		assertTrue(registryService.containsKey(PARENT_NS_URI));
		assertTrue(registryService.keySet().contains(PARENT_NS_URI));
	}

	@Test
	void doesNotResolveUnknownPackageFromBoundParent() {
		component.addParentRegistry(parentReference);

		assertNull(registryService.getEPackage("http://example.org/fennec/test/unknown"));
		assertFalse(registryService.containsKey("http://example.org/fennec/test/unknown"));
	}

	@Test
	void ownPackageShadowsParentPackage() {
		component.addParentRegistry(parentReference);
		component.addEPackageConfigurator(configuratorFor(SHADOWED_NS_URI, shadowingPackage),
				CONFIGURATOR_PROPERTIES);

		assertSame(shadowingPackage, registryService.getEPackage(SHADOWED_NS_URI));
	}

	@Test
	void ownPackagesStayResolvableAlongsideTheParent() {
		component.addParentRegistry(parentReference);
		component.addEPackageConfigurator(configuratorFor(OWN_NS_URI, ownPackage), CONFIGURATOR_PROPERTIES);

		assertSame(ownPackage, registryService.getEPackage(OWN_NS_URI));
		assertSame(parentPackage, registryService.getEPackage(PARENT_NS_URI));
	}

	@Test
	void resolvesFromSingletonWhileNoParentIsBound() {
		assertSame(singletonPackage, registryService.getEPackage(SINGLETON_NS_URI));
	}

	@Test
	void parentRemovalRevertsToSingletonOnlyResolution() {
		component.addParentRegistry(parentReference);
		component.removeParentRegistry(parentReference);

		assertNull(registryService.getEPackage(PARENT_NS_URI));
		assertSame(singletonPackage, registryService.getEPackage(SINGLETON_NS_URI));
		verify(bundleContext).ungetService(parentReference);
	}

	@Test
	void replacingTheParentSwitchesTheDelegate() {
		component.addParentRegistry(parentReference);
		// a dynamic mandatory reference binds the replacement before the old one is unbound
		component.addParentRegistry(otherParentReference);
		component.removeParentRegistry(parentReference);

		assertSame(otherParentPackage, registryService.getEPackage(OTHER_PARENT_NS_URI));
		assertNull(registryService.getEPackage(PARENT_NS_URI));
	}

	@Test
	void updatingTheParentKeepsTheDelegation() {
		component.addParentRegistry(parentReference);
		component.updateParentRegistry(parentReference);

		assertSame(parentPackage, registryService.getEPackage(PARENT_NS_URI));
	}

	@Test
	void bindingTheOwnRegistryAsParentDoesNotRecurse() {
		component.addParentRegistry(reference(registryService, 103, null));

		assertNull(registryService.getEPackage(PARENT_NS_URI));
		assertSame(singletonPackage, registryService.getEPackage(SINGLETON_NS_URI));
	}

	@Test
	void propagatesTheParentProperties() {
		component.addParentRegistry(parentReference);

		assertTrue(propagatedNames(EMFNamespaces.EMF_NAME).contains("parentmodel"));
	}

	private EPackageConfigurator configuratorFor(String nsURI, EPackage ePackage) {
		return new EPackageConfigurator() {

			@Override
			public void configureEPackage(EPackage.Registry registry) {
				registry.put(nsURI, ePackage);
			}

			@Override
			public void unconfigureEPackage(EPackage.Registry registry) {
				registry.remove(nsURI);
			}
		};
	}

	private ServiceReference<EPackage.Registry> reference(EPackage.Registry registry, long serviceId,
			String modelName) {
		@SuppressWarnings("unchecked")
		ServiceReference<EPackage.Registry> serviceRef = mock(ServiceReference.class);
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_ID, Long.valueOf(serviceId));
		if (modelName != null) {
			properties.put(EMFNamespaces.EMF_NAME, modelName);
		}
		when(serviceRef.getProperties()).thenReturn(properties);
		when(bundleContext.getService(serviceRef)).thenReturn(registry);
		return serviceRef;
	}

	private EPackage.Registry capturedRegistryService() {
		ArgumentCaptor<EPackage.Registry> captor = ArgumentCaptor.forClass(EPackage.Registry.class);
		verify(bundleContext).registerService(eq(EPackage.Registry.class), captor.capture(), any());
		return captor.getValue();
	}

	private List<Object> propagatedNames(String key) {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Dictionary<String, Object>> captor = ArgumentCaptor.forClass(Dictionary.class);
		verify(registration, atLeastOnce()).setProperties(captor.capture());
		Object value = captor.getValue().get(key);
		return value instanceof Object[] values ? Arrays.asList(values) : Arrays.asList(value);
	}

	private static EPackage createPackage(String name, String nsURI) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName(name);
		ePackage.setNsPrefix(name);
		ePackage.setNsURI(nsURI);
		return ePackage;
	}
}
