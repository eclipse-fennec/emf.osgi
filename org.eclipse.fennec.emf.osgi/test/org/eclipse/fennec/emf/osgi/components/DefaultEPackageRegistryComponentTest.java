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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
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
 * Tests the delegation of the {@link DefaultEPackageRegistryComponent} to its bound parent, the
 * static {@link EPackage.Registry} service, see issue #105.
 */
class DefaultEPackageRegistryComponentTest {

	private static final String STATIC_NS_URI = "http://example.org/fennec/default/static";
	private static final String OTHER_STATIC_NS_URI = "http://example.org/fennec/default/otherstatic";
	private static final String SINGLETON_NS_URI = "http://example.org/fennec/default/singleton";
	private static final String SHADOWED_NS_URI = "http://example.org/fennec/default/shadowed";

	private static final Map<String, Object> CONFIGURATOR_PROPERTIES = Map.of(Constants.SERVICE_ID,
			Long.valueOf(303), EMFNamespaces.EMF_NAME, "resourcesetmodel");

	private final EPackage staticPackage = createPackage("static", STATIC_NS_URI);
	private final EPackage otherStaticPackage = createPackage("otherstatic", OTHER_STATIC_NS_URI);
	private final EPackage singletonPackage = createPackage("singleton", SINGLETON_NS_URI);
	private final EPackage shadowingPackage = createPackage("shadowing", SHADOWED_NS_URI);
	private final EPackage shadowedPackage = createPackage("shadowed", SHADOWED_NS_URI);

	private final EPackage.Registry staticRegistry = new EPackageRegistryImpl();
	private final EPackage.Registry otherStaticRegistry = new EPackageRegistryImpl();

	@SuppressWarnings("unchecked")
	private final ServiceRegistration<EPackage.Registry> registration = mock(ServiceRegistration.class);

	private BundleContext bundleContext;
	private DefaultEPackageRegistryComponent component;
	private EPackage.Registry registryService;
	private ServiceReference<EPackage.Registry> staticReference;
	private ServiceReference<EPackage.Registry> otherStaticReference;

	@BeforeEach
	void setUp() {
		staticRegistry.put(STATIC_NS_URI, staticPackage);
		staticRegistry.put(SHADOWED_NS_URI, shadowedPackage);
		otherStaticRegistry.put(OTHER_STATIC_NS_URI, otherStaticPackage);
		EPackage.Registry.INSTANCE.put(SINGLETON_NS_URI, singletonPackage);

		bundleContext = mock(BundleContext.class);
		when(bundleContext.getBundles()).thenReturn(new Bundle[0]);
		when(bundleContext.registerService(eq(EPackage.Registry.class), any(EPackage.Registry.class), any()))
				.thenReturn(registration);

		component = new DefaultEPackageRegistryComponent(bundleContext);
		registryService = capturedRegistryService();

		staticReference = reference(staticRegistry, 301);
		otherStaticReference = reference(otherStaticRegistry, 302);
	}

	@AfterEach
	void tearDown() {
		EPackage.Registry.INSTANCE.remove(SINGLETON_NS_URI);
	}

	@Test
	void resolvesPackageFromBoundStaticRegistry() {
		component.addParentRegistry(staticReference);

		assertSame(staticPackage, registryService.getEPackage(STATIC_NS_URI));
		assertSame(staticPackage.getEFactoryInstance(), registryService.getEFactory(STATIC_NS_URI));
		assertSame(staticPackage, registryService.get(STATIC_NS_URI));
		assertTrue(registryService.containsKey(STATIC_NS_URI));
	}

	@Test
	void ownPackageShadowsStaticPackage() {
		component.addParentRegistry(staticReference);
		component.addEPackageConfigurator(configuratorFor(SHADOWED_NS_URI, shadowingPackage),
				CONFIGURATOR_PROPERTIES);

		assertSame(shadowingPackage, registryService.getEPackage(SHADOWED_NS_URI));
	}

	@Test
	void resolvesFromSingletonWhileNoParentIsBound() {
		assertSame(singletonPackage, registryService.getEPackage(SINGLETON_NS_URI));
	}

	@Test
	void parentRemovalRevertsToSingletonOnlyResolution() {
		component.addParentRegistry(staticReference);
		component.removeParentRegistry(staticReference);

		assertNull(registryService.getEPackage(STATIC_NS_URI));
		assertSame(singletonPackage, registryService.getEPackage(SINGLETON_NS_URI));
		verify(bundleContext).ungetService(staticReference);
	}

	@Test
	void replacingTheParentSwitchesTheDelegate() {
		component.addParentRegistry(staticReference);
		// a dynamic mandatory reference binds the replacement before the old one is unbound
		component.addParentRegistry(otherStaticReference);
		component.removeParentRegistry(staticReference);

		assertSame(otherStaticPackage, registryService.getEPackage(OTHER_STATIC_NS_URI));
		assertNull(registryService.getEPackage(STATIC_NS_URI));
	}

	@Test
	void bindingTheOwnRegistryAsParentDoesNotRecurse() {
		component.addParentRegistry(reference(registryService, 304));

		assertNull(registryService.getEPackage(STATIC_NS_URI));
		assertSame(singletonPackage, registryService.getEPackage(SINGLETON_NS_URI));
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

	private ServiceReference<EPackage.Registry> reference(EPackage.Registry registry, long serviceId) {
		@SuppressWarnings("unchecked")
		ServiceReference<EPackage.Registry> serviceRef = mock(ServiceReference.class);
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_ID, Long.valueOf(serviceId));
		when(serviceRef.getProperties()).thenReturn(properties);
		when(bundleContext.getService(serviceRef)).thenReturn(registry);
		return serviceRef;
	}

	private EPackage.Registry capturedRegistryService() {
		ArgumentCaptor<EPackage.Registry> captor = ArgumentCaptor.forClass(EPackage.Registry.class);
		verify(bundleContext).registerService(eq(EPackage.Registry.class), captor.capture(), any());
		return captor.getValue();
	}

	private static EPackage createPackage(String name, String nsURI) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName(name);
		ePackage.setNsPrefix(name);
		ePackage.setNsURI(nsURI);
		return ePackage;
	}
}
