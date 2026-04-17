/**
 * Copyright (c) 2012 - 2025 Data In Motion and others.
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
package org.eclipse.fennec.emf.gecko.compatibility.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

@ExtendWith(ServiceExtension.class)
@ExtendWith(BundleContextExtension.class)
@SuppressWarnings("deprecation")
public class GeckoCompatibilityWrapperTest {

	private static final long TIMEOUT_MS = 5000;
	private static final long POLL_INTERVAL_MS = 100;

	@InjectBundleContext
	BundleContext bundleContext;

	// --- Helper methods ---

	private <T> ServiceReference<T> waitForServiceReference(Class<T> clazz, String filter, long timeout) throws Exception {
		long deadline = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < deadline) {
			ServiceReference<?>[] refs = bundleContext.getServiceReferences(clazz.getName(), filter);
			if (refs != null && refs.length > 0) {
				@SuppressWarnings("unchecked")
				ServiceReference<T> ref = (ServiceReference<T>) refs[0];
				return ref;
			}
			Thread.sleep(POLL_INTERVAL_MS);
		}
		return null;
	}

	private <T> boolean waitForNoServiceReference(Class<T> clazz, String filter, long timeout) throws Exception {
		long deadline = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < deadline) {
			ServiceReference<?>[] refs = bundleContext.getServiceReferences(clazz.getName(), filter);
			if (refs == null || refs.length == 0) {
				return true;
			}
			Thread.sleep(POLL_INTERVAL_MS);
		}
		return false;
	}

	// --- Test 1: UriMapProvider Gecko→Fennec ---

	@Test
	public void testUriMapProviderGeckoToFennec() throws Exception {
		Map<URI, URI> expectedMap = new HashMap<>();
		expectedMap.put(URI.createURI("http://source"), URI.createURI("http://target"));

		org.gecko.emf.osgi.UriMapProvider geckoProvider = () -> expectedMap;

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "urimap-g2f");

		ServiceRegistration<org.gecko.emf.osgi.UriMapProvider> reg = null;
		try {
			reg = bundleContext.registerService(org.gecko.emf.osgi.UriMapProvider.class, geckoProvider, props);

			ServiceReference<org.eclipse.fennec.emf.osgi.UriMapProvider> fennecRef =
					waitForServiceReference(org.eclipse.fennec.emf.osgi.UriMapProvider.class, "(test.id=urimap-g2f)", TIMEOUT_MS);

			assertNotNull(fennecRef, "Fennec UriMapProvider wrapper should appear");

			org.eclipse.fennec.emf.osgi.UriMapProvider fennecProvider = bundleContext.getService(fennecRef);
			assertNotNull(fennecProvider, "Should be able to get the Fennec service");

			Map<URI, URI> result = fennecProvider.getUriMap();
			assertThat(result).isEqualTo(expectedMap);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	// --- Test 2: UriHandlerProvider Gecko→Fennec ---

	@Test
	public void testUriHandlerProviderGeckoToFennec() throws Exception {
		URIHandler expectedHandler = new URIHandlerImpl();

		org.gecko.emf.osgi.UriHandlerProvider geckoProvider = () -> expectedHandler;

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "urihandler-g2f");

		ServiceRegistration<org.gecko.emf.osgi.UriHandlerProvider> reg = null;
		try {
			reg = bundleContext.registerService(org.gecko.emf.osgi.UriHandlerProvider.class, geckoProvider, props);

			ServiceReference<org.eclipse.fennec.emf.osgi.UriHandlerProvider> fennecRef =
					waitForServiceReference(org.eclipse.fennec.emf.osgi.UriHandlerProvider.class, "(test.id=urihandler-g2f)", TIMEOUT_MS);

			assertNotNull(fennecRef, "Fennec UriHandlerProvider wrapper should appear");

			org.eclipse.fennec.emf.osgi.UriHandlerProvider fennecProvider = bundleContext.getService(fennecRef);
			assertNotNull(fennecProvider, "Should be able to get the Fennec service");

			URIHandler result = fennecProvider.getURIHandler();
			assertThat(result).isSameAs(expectedHandler);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	// --- Test 3: EPackageConfigurator Gecko→Fennec ---

	@Test
	public void testEPackageConfiguratorGeckoToFennec() throws Exception {
		AtomicBoolean configureCalled = new AtomicBoolean(false);
		AtomicBoolean unconfigureCalled = new AtomicBoolean(false);

		org.gecko.emf.osgi.configurator.EPackageConfigurator geckoConfigurator =
				new org.gecko.emf.osgi.configurator.EPackageConfigurator() {
					@Override
					public void configureEPackage(EPackage.Registry registry) {
						configureCalled.set(true);
					}

					@Override
					public void unconfigureEPackage(EPackage.Registry registry) {
						unconfigureCalled.set(true);
					}
				};

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "epackage-g2f");

		ServiceRegistration<org.gecko.emf.osgi.configurator.EPackageConfigurator> reg = null;
		try {
			reg = bundleContext.registerService(org.gecko.emf.osgi.configurator.EPackageConfigurator.class, geckoConfigurator, props);

			ServiceReference<org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator> fennecRef =
					waitForServiceReference(org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator.class, "(test.id=epackage-g2f)", TIMEOUT_MS);

			assertNotNull(fennecRef, "Fennec EPackageConfigurator wrapper should appear");

			org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator fennecConfigurator = bundleContext.getService(fennecRef);
			assertNotNull(fennecConfigurator, "Should be able to get the Fennec service");

			EPackage.Registry registry = EPackage.Registry.INSTANCE;

			fennecConfigurator.configureEPackage(registry);
			assertTrue(configureCalled.get(), "configureEPackage should delegate to Gecko configurator");

			fennecConfigurator.unconfigureEPackage(registry);
			assertTrue(unconfigureCalled.get(), "unconfigureEPackage should delegate to Gecko configurator");
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	// --- Test 4: ResourceSetConfigurator Gecko→Fennec ---

	@Test
	public void testResourceSetConfiguratorGeckoToFennec() throws Exception {
		ResourceSet testResourceSet = new ResourceSetImpl();
		AtomicBoolean configureCalled = new AtomicBoolean(false);

		org.gecko.emf.osgi.configurator.ResourceSetConfigurator geckoConfigurator =
				resourceSet -> {
					assertThat(resourceSet).isSameAs(testResourceSet);
					configureCalled.set(true);
				};

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "rsconfig-g2f");

		ServiceRegistration<org.gecko.emf.osgi.configurator.ResourceSetConfigurator> reg = null;
		try {
			reg = bundleContext.registerService(org.gecko.emf.osgi.configurator.ResourceSetConfigurator.class, geckoConfigurator, props);

			ServiceReference<org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator> fennecRef =
					waitForServiceReference(org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator.class, "(test.id=rsconfig-g2f)", TIMEOUT_MS);

			assertNotNull(fennecRef, "Fennec ResourceSetConfigurator wrapper should appear");

			org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator fennecConfigurator = bundleContext.getService(fennecRef);
			assertNotNull(fennecConfigurator, "Should be able to get the Fennec service");

			fennecConfigurator.configureResourceSet(testResourceSet);
			assertTrue(configureCalled.get(), "configureResourceSet should delegate to Gecko configurator with same ResourceSet instance");
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	// --- Test 5: ResourceSetFactory Fennec→Gecko ---

	@Test
	public void testResourceSetFactoryFennecToGecko() throws Exception {
		ResourceSet expectedResourceSet = new ResourceSetImpl();

		org.eclipse.fennec.emf.osgi.ResourceSetFactory fennecFactory = () -> expectedResourceSet;

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "rsfactory-f2g");

		ServiceRegistration<org.eclipse.fennec.emf.osgi.ResourceSetFactory> reg = null;
		try {
			reg = bundleContext.registerService(org.eclipse.fennec.emf.osgi.ResourceSetFactory.class, fennecFactory, props);

			ServiceReference<org.gecko.emf.osgi.ResourceSetFactory> geckoRef =
					waitForServiceReference(org.gecko.emf.osgi.ResourceSetFactory.class, "(test.id=rsfactory-f2g)", TIMEOUT_MS);

			assertNotNull(geckoRef, "Gecko ResourceSetFactory wrapper should appear");

			org.gecko.emf.osgi.ResourceSetFactory geckoFactory = bundleContext.getService(geckoRef);
			assertNotNull(geckoFactory, "Should be able to get the Gecko service");

			ResourceSet result = geckoFactory.createResourceSet();
			assertThat(result).isSameAs(expectedResourceSet);

			assertThat(geckoFactory.getResourceSetConfigurators()).isEmpty();
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	// --- Test 6: Property Forwarding ---

	@Test
	public void testPropertyForwarding() throws Exception {
		org.gecko.emf.osgi.UriMapProvider geckoProvider = Collections::emptyMap;

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "propforward");
		props.put("custom.property", "custom-value");
		props.put("another.prop", Integer.valueOf(42));

		ServiceRegistration<org.gecko.emf.osgi.UriMapProvider> reg = null;
		try {
			reg = bundleContext.registerService(org.gecko.emf.osgi.UriMapProvider.class, geckoProvider, props);

			ServiceReference<org.eclipse.fennec.emf.osgi.UriMapProvider> fennecRef =
					waitForServiceReference(org.eclipse.fennec.emf.osgi.UriMapProvider.class, "(test.id=propforward)", TIMEOUT_MS);

			assertNotNull(fennecRef, "Fennec UriMapProvider wrapper should appear");

			assertThat(fennecRef.getProperty("test.id")).isEqualTo("propforward");
			assertThat(fennecRef.getProperty("custom.property")).isEqualTo("custom-value");
			assertThat(fennecRef.getProperty("another.prop")).isEqualTo(Integer.valueOf(42));
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	// --- Test 7: Service Removal ---

	@Test
	public void testServiceRemoval() throws Exception {
		org.gecko.emf.osgi.UriMapProvider geckoProvider = Collections::emptyMap;

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "removal-test");

		ServiceRegistration<org.gecko.emf.osgi.UriMapProvider> reg =
				bundleContext.registerService(org.gecko.emf.osgi.UriMapProvider.class, geckoProvider, props);

		ServiceReference<org.eclipse.fennec.emf.osgi.UriMapProvider> fennecRef =
				waitForServiceReference(org.eclipse.fennec.emf.osgi.UriMapProvider.class, "(test.id=removal-test)", TIMEOUT_MS);

		assertNotNull(fennecRef, "Fennec UriMapProvider wrapper should appear after registration");

		// Now unregister the Gecko service
		reg.unregister();

		// The Fennec wrapper should disappear
		boolean gone = waitForNoServiceReference(org.eclipse.fennec.emf.osgi.UriMapProvider.class, "(test.id=removal-test)", TIMEOUT_MS);
		assertTrue(gone, "Fennec UriMapProvider wrapper should disappear after Gecko service is unregistered");
	}

	// --- Test 8: EMFModelInfo Fennec→Gecko wrapper via real service ---

	@Test
	public void testEMFModelInfoFennecToGecko() throws Exception {
		// The real Fennec EMFModelInfo service is registered by EMFModelInfoImpl.
		// The wrapper should automatically re-register it under the Gecko interface.
		ServiceReference<org.gecko.emf.osgi.model.info.EMFModelInfo> geckoRef =
				waitForServiceReference(org.gecko.emf.osgi.model.info.EMFModelInfo.class, null, TIMEOUT_MS);

		assertNotNull(geckoRef, "Gecko EMFModelInfo wrapper should appear for the real Fennec EMFModelInfo service");

		org.gecko.emf.osgi.model.info.EMFModelInfo geckoModelInfo = bundleContext.getService(geckoRef);
		assertNotNull(geckoModelInfo, "Should be able to get the Gecko EMFModelInfo service");

		// Verify delegation works - no EPackageConfigurators are registered in this test runtime,
		// so lookups return empty, but the calls should not throw
		Optional<EClassifier> classifier = geckoModelInfo.getEClassifierForClass(String.class);
		assertNotNull(classifier);

		Optional<EClassifier> classifierByName = geckoModelInfo.getEClassifierForClass("java.lang.String");
		assertNotNull(classifierByName);

		// Verify hierarchy lookup delegates without error
		EClass eClassEClass = EcorePackage.Literals.ECLASS;
		List<EClass> hierarchy = geckoModelInfo.getUpperTypeHierarchyForEClass(eClassEClass);
		assertNotNull(hierarchy);
	}

	// --- Test 9: ResourceFactoryConfigurator Gecko→Fennec Resource.Factory services ---

	@Test
	public void testResourceFactoryConfiguratorGeckoToFennec() throws Exception {
		Resource.Factory testFactory = uri -> new ResourceImpl(uri);

		org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator geckoConfigurator =
				new org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator() {
					@Override
					public void configureResourceFactory(Resource.Factory.Registry registry) {
						registry.getExtensionToFactoryMap().put("testExt", testFactory);
						registry.getContentTypeToFactoryMap().put("test/content", testFactory);
						registry.getProtocolToFactoryMap().put("testproto", testFactory);
					}

					@Override
					public void unconfigureResourceFactory(Resource.Factory.Registry registry) {
						registry.getExtensionToFactoryMap().remove("testExt");
						registry.getContentTypeToFactoryMap().remove("test/content");
						registry.getProtocolToFactoryMap().remove("testproto");
					}
				};

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "rfconfig-g2f");
		props.put(EMFNamespaces.EMF_CONFIGURATOR_NAME, "testResourceFactoryConfigurator");

		ServiceRegistration<org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator> reg = null;
		try {
			reg = bundleContext.registerService(org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator.class, geckoConfigurator, props);

			ServiceReference<Resource.Factory> factoryRef =
					waitForServiceReference(Resource.Factory.class, "(test.id=rfconfig-g2f)", TIMEOUT_MS);

			assertNotNull(factoryRef, "Resource.Factory service should appear for the wrapped ResourceFactoryConfigurator");

			// Verify the factory is the same instance
			Resource.Factory registeredFactory = bundleContext.getService(factoryRef);
			assertThat(registeredFactory).isSameAs(testFactory);

			// Verify the properties are correctly set from the registry entries
			Object fileExt = factoryRef.getProperty(EMFNamespaces.EMF_MODEL_FILE_EXT);
			assertNotNull(fileExt, "fileExtension property should be set");
			assertThat(toStringList(fileExt)).contains("testExt");

			Object contentType = factoryRef.getProperty(EMFNamespaces.EMF_MODEL_CONTENT_TYPE);
			assertNotNull(contentType, "contentType property should be set");
			assertThat(toStringList(contentType)).contains("test/content");

			Object protocol = factoryRef.getProperty(EMFNamespaces.EMF_MODEL_PROTOCOL);
			assertNotNull(protocol, "protocol property should be set");
			assertThat(toStringList(protocol)).contains("testproto");

			// Verify original properties are forwarded
			assertThat(factoryRef.getProperty(EMFNamespaces.EMF_CONFIGURATOR_NAME)).isEqualTo("testResourceFactoryConfigurator");
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	// --- Test 10: ResourceFactoryConfigurator with multiple factories ---

	@Test
	public void testResourceFactoryConfiguratorMultipleFactories() throws Exception {
		Resource.Factory factoryA = uri -> new ResourceImpl(uri);
		Resource.Factory factoryB = uri -> new ResourceImpl(uri);

		org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator geckoConfigurator =
				new org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator() {
					@Override
					public void configureResourceFactory(Resource.Factory.Registry registry) {
						registry.getExtensionToFactoryMap().put("extA", factoryA);
						registry.getExtensionToFactoryMap().put("extB", factoryB);
						registry.getContentTypeToFactoryMap().put("type/a", factoryA);
					}

					@Override
					public void unconfigureResourceFactory(Resource.Factory.Registry registry) {
						registry.getExtensionToFactoryMap().remove("extA");
						registry.getExtensionToFactoryMap().remove("extB");
						registry.getContentTypeToFactoryMap().remove("type/a");
					}
				};

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "rfconfig-multi");

		ServiceRegistration<org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator> reg = null;
		try {
			reg = bundleContext.registerService(org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator.class, geckoConfigurator, props);

			// Wait for at least one factory to appear
			ServiceReference<Resource.Factory> anyRef =
					waitForServiceReference(Resource.Factory.class, "(test.id=rfconfig-multi)", TIMEOUT_MS);
			assertNotNull(anyRef, "At least one Resource.Factory service should appear");

			// Collect all Resource.Factory services for this configurator
			ServiceReference<?>[] allRefs = bundleContext.getServiceReferences(
					Resource.Factory.class.getName(), "(test.id=rfconfig-multi)");
			assertNotNull(allRefs, "Should find Resource.Factory services");

			// Two distinct factory instances should produce two service registrations
			assertThat(allRefs).hasSize(2);

			// Find factoryA's registration - it should have extA extension and type/a content type
			boolean foundA = false;
			boolean foundB = false;
			for (ServiceReference<?> ref : allRefs) {
				Resource.Factory svc = (Resource.Factory) bundleContext.getService(ref);
				if (svc == factoryA) {
					foundA = true;
					assertThat(toStringList(ref.getProperty(EMFNamespaces.EMF_MODEL_FILE_EXT))).contains("extA");
					assertThat(toStringList(ref.getProperty(EMFNamespaces.EMF_MODEL_CONTENT_TYPE))).contains("type/a");
				} else if (svc == factoryB) {
					foundB = true;
					assertThat(toStringList(ref.getProperty(EMFNamespaces.EMF_MODEL_FILE_EXT))).contains("extB");
				}
			}
			assertTrue(foundA, "FactoryA should be registered with extension 'extA' and content type 'type/a'");
			assertTrue(foundB, "FactoryB should be registered with extension 'extB'");
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	// --- Test 11: ResourceFactoryConfigurator service removal ---

	@Test
	public void testResourceFactoryConfiguratorServiceRemoval() throws Exception {
		Resource.Factory testFactory = uri -> new ResourceImpl(uri);

		org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator geckoConfigurator =
				new org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator() {
					@Override
					public void configureResourceFactory(Resource.Factory.Registry registry) {
						registry.getExtensionToFactoryMap().put("removeExt", testFactory);
					}

					@Override
					public void unconfigureResourceFactory(Resource.Factory.Registry registry) {
						registry.getExtensionToFactoryMap().remove("removeExt");
					}
				};

		Hashtable<String, Object> props = new Hashtable<>();
		props.put("test.id", "rfconfig-removal");

		ServiceRegistration<org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator> reg =
				bundleContext.registerService(org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator.class, geckoConfigurator, props);

		ServiceReference<Resource.Factory> factoryRef =
				waitForServiceReference(Resource.Factory.class, "(test.id=rfconfig-removal)", TIMEOUT_MS);
		assertNotNull(factoryRef, "Resource.Factory service should appear");

		// Now unregister the Gecko configurator
		reg.unregister();

		// The Resource.Factory wrapper should disappear
		boolean gone = waitForNoServiceReference(Resource.Factory.class, "(test.id=rfconfig-removal)", TIMEOUT_MS);
		assertTrue(gone, "Resource.Factory service should disappear after ResourceFactoryConfigurator is unregistered");
	}

	@SuppressWarnings("unchecked")
	private List<String> toStringList(Object value) {
		if (value instanceof List) {
			return (List<String>) value;
		} else if (value instanceof Collection) {
			return List.copyOf((Collection<String>) value);
		} else if (value instanceof String[]) {
			return List.of((String[]) value);
		} else if (value instanceof String) {
			return List.of((String) value);
		}
		return List.of();
	}
}
