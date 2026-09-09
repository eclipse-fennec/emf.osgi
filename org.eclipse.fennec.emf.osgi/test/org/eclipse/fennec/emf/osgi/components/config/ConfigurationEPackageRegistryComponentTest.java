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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.fennec.emf.osgi.RegistryTrackingService;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Tests the delegation of the {@link ConfigurationEPackageRegistryComponent} to its parent
 * {@link EPackage.Registry} service, see issue #105.
 */
class ConfigurationEPackageRegistryComponentTest {

	private static final long PARENT_SERVICE_ID = 101;
	private static final String OWN_NS_URI = "http://example.org/fennec/test/own";
	private static final String PARENT_NS_URI = "http://example.org/fennec/test/parent";
	private static final String UNKNOWN_NS_URI = "http://example.org/fennec/test/unknown";
	private static final String SHADOWED_NS_URI = "http://example.org/fennec/test/shadowed";

	private static final Map<String, Object> CONFIGURATOR_PROPERTIES = Map.of(Constants.SERVICE_ID,
			Long.valueOf(201), EMFNamespaces.EMF_NAME, "ownmodel");

	private final EPackage parentPackage = createPackage("parent", PARENT_NS_URI);
	private final EPackage ownPackage = createPackage("own", OWN_NS_URI);
	private final EPackage shadowingPackage = createPackage("shadowing", SHADOWED_NS_URI);
	private final EPackage shadowedPackage = createPackage("shadowed", SHADOWED_NS_URI);

	private final EPackage.Registry parentRegistry = new EPackageRegistryImpl();

	@SuppressWarnings("unchecked")
	private final ServiceRegistration<EPackage.Registry> registration = mock(ServiceRegistration.class);

	private final RegistryTrackingService registryTracker = mock(RegistryTrackingService.class);

	private BundleContext bundleContext;
	private ConfigurationEPackageRegistryComponent component;
	private EPackage.Registry registryService;
	private ServiceReference<EPackage.Registry> parentReference;

	@BeforeEach
	void setUp() {
		parentRegistry.put(PARENT_NS_URI, parentPackage);
		parentRegistry.put(SHADOWED_NS_URI, shadowedPackage);

		bundleContext = mock(BundleContext.class);
		when(bundleContext.getBundles()).thenReturn(new Bundle[0]);
		when(bundleContext.registerService(eq(EPackage.Registry.class), any(EPackage.Registry.class), any()))
				.thenReturn(registration);

		parentReference = reference(parentRegistry, PARENT_SERVICE_ID, "parentmodel");
		component = new ConfigurationEPackageRegistryComponent(bundleContext,
				Map.of(PROP_RESOURCE_SET_FACTORY_NAME, "test"), parentReference, registryTracker);
		registryService = capturedRegistryService();
	}

	@Test
	void resolvesPackageFromTheParent() {
		assertSame(parentPackage, registryService.getEPackage(PARENT_NS_URI));
		assertSame(parentPackage.getEFactoryInstance(), registryService.getEFactory(PARENT_NS_URI));
		assertSame(parentPackage, registryService.get(PARENT_NS_URI));
		assertTrue(registryService.containsKey(PARENT_NS_URI));
		assertTrue(registryService.keySet().contains(PARENT_NS_URI));
	}

	@Test
	void doesNotResolveUnknownPackageFromTheParent() {
		assertNull(registryService.getEPackage(UNKNOWN_NS_URI));
		assertFalse(registryService.containsKey(UNKNOWN_NS_URI));
	}

	@Test
	void ownPackageShadowsParentPackage() {
		component.addEPackageConfigurator(configuratorFor(SHADOWED_NS_URI, shadowingPackage),
				CONFIGURATOR_PROPERTIES);

		assertSame(shadowingPackage, registryService.getEPackage(SHADOWED_NS_URI));
	}

	@Test
	void ownPackagesStayResolvableAlongsideTheParent() {
		component.addEPackageConfigurator(configuratorFor(OWN_NS_URI, ownPackage), CONFIGURATOR_PROPERTIES);

		assertSame(ownPackage, registryService.getEPackage(OWN_NS_URI));
		assertSame(parentPackage, registryService.getEPackage(PARENT_NS_URI));
	}

	@Test
	void requiresTheParentRegistryService() {
		ServiceReference<EPackage.Registry> goneReference = reference(null, 102, null);

		assertThrows(NullPointerException.class,
				() -> new ConfigurationEPackageRegistryComponent(bundleContext,
						Map.of(PROP_RESOURCE_SET_FACTORY_NAME, "test"), goneReference, registryTracker));
	}

	@Test
	void propagatesTheParentProperties() {
		component.addEPackageConfigurator(configuratorFor(OWN_NS_URI, ownPackage), CONFIGURATOR_PROPERTIES);

		assertTrue(propagatedValues(EMFNamespaces.EMF_NAME).contains("parentmodel"));
	}

	@Test
	void propagatesChangedParentProperties() {
		component.onRegistryPropertiesChanged(PARENT_SERVICE_ID, "EPackage.Registry",
				Map.of(Constants.SERVICE_ID, Long.valueOf(PARENT_SERVICE_ID), EMFNamespaces.EMF_NAME, "addedmodel"));

		assertTrue(propagatedValues(EMFNamespaces.EMF_NAME).contains("addedmodel"));
	}

	@Test
	void ignoresPropertiesOfOtherRegistries() {
		component.onRegistryPropertiesChanged(999, "EPackage.Registry",
				Map.of(Constants.SERVICE_ID, Long.valueOf(999), EMFNamespaces.EMF_NAME, "othermodel"));

		verify(registration, never()).setProperties(any());
	}

	@Test
	void tracksThePropertiesOfTheParentRegistry() {
		verify(registryTracker).registerListener(component, Set.of(Long.valueOf(PARENT_SERVICE_ID)));
	}

	@Test
	void deactivationReleasesTheParentRegistry() {
		component.deactivate();

		verify(registryTracker).unregisterListener(component);
		verify(registration).unregister();
		verify(bundleContext).ungetService(parentReference);
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
		when(serviceRef.getProperty(Constants.SERVICE_ID)).thenReturn(Long.valueOf(serviceId));
		when(bundleContext.getService(serviceRef)).thenReturn(registry);
		return serviceRef;
	}

	private EPackage.Registry capturedRegistryService() {
		ArgumentCaptor<EPackage.Registry> captor = ArgumentCaptor.forClass(EPackage.Registry.class);
		verify(bundleContext).registerService(eq(EPackage.Registry.class), captor.capture(), any());
		return captor.getValue();
	}

	private List<Object> propagatedValues(String key) {
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
