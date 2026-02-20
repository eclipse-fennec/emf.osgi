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
package org.eclipse.fennec.emf.osgi.extender.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.test.assertj.dictionary.DictionaryAssert;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * OSGi integration tests for the EMF model extender.
 * <p>
 * Verifies that ecore models declared via the extender pattern are correctly
 * discovered, loaded, and registered as OSGi services ({@link EPackage},
 * {@link EPackageConfigurator}, and made available through {@link ResourceSet}).
 * <p>
 * The test bundles ({@code org.eclipse.fennec.emf.osgi.example.model.extender}
 * and {@code org.eclipse.fennec.emf.osgi.example.model.manual}) provide test
 * models with inline properties and multiple folder configurations.
 *
 * @author Mark Hoffmann
 * @since 14.10.2022
 */
@ExtendWith(ServiceExtension.class)
@ExtendWith(BundleContextExtension.class)
public class EMFModelExtenderTest {

	private BundleContext ctx;

	@BeforeEach
	public void before(@InjectBundleContext BundleContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * Verifies that a single extender-managed model is available through
	 * a filtered {@link ResourceSet} service and contains expected classifiers.
	 */
	@Test
	public void simpleTest(@InjectService(filter = "(" + EMFNamespaces.EMF_NAME + "=manual)") ServiceAware<ResourceSet> rsAware) {
		ResourceSet rs = rsAware.getService();
		assertNotNull(rs);
		EFactory eFactory = rs.getPackageRegistry().getEFactory("http://fennec.eclipse.org/example/model/manual/1.0");
		assertNotNull(eFactory);
		EPackage ePackage = eFactory.getEPackage();
		assertNotNull(ePackage);
		// Foo class exists
		EClass foo = (EClass) ePackage.getEClassifier("Foo");
		assertNotNull(foo);
		EClass bar = (EClass) ePackage.getEClassifier("Bar");
		assertNull(bar);
	}
	
	/**
	 * Verifies that an extender-managed model is registered as an {@link EPackage} service.
	 */
	@Test
	public void simpleTestEPackage(@InjectService(filter = "(" + EMFNamespaces.EMF_NAME + "=manual)") ServiceAware<EPackage> epackageAware) {
		assertNotNull(epackageAware);
		EPackage ePackage = epackageAware.getService();
		assertNotNull(ePackage);
		// Foo class exists
		EClass foo = (EClass) ePackage.getEClassifier("Foo");
		assertNotNull(foo);
		EClass bar = (EClass) ePackage.getEClassifier("Bar");
		assertNull(bar);
	}
	
	/**
	 * Verifies that the {@code emf.registration} service property is set to
	 * {@code extender} for models registered by the extender.
	 */
	@Test
	public void simpleTestEPackageRegistrationProperty(@InjectService(filter = "(" + EMFNamespaces.EMF_NAME + "=manual)") ServiceAware<EPackage> epackageAware) {
		assertNotNull(epackageAware);
		assertThat(epackageAware.isEmpty()).isFalse();
		DictionaryAssert.assertThat(epackageAware.getServiceReference().getProperties()).containsKey(EMFNamespaces.EMF_MODEL_REGISTRATION)	
		.extractingByKey(EMFNamespaces.EMF_MODEL_REGISTRATION).isEqualTo(EMFNamespaces.MODEL_REGISTRATION_EXTENDER);
	}
	
	/**
	 * Verifies that models from multiple folders are all available in a single
	 * {@link ResourceSet} when filtered for both model names.
	 */
	@Test
	public void simpleMultipleFolders(@InjectService(filter = "(&(" + EMFNamespaces.EMF_NAME + "=manual)(" + EMFNamespaces.EMF_NAME + "=foobar))") ServiceAware<ResourceSet> rsAware) {
		ResourceSet rs = rsAware.getService();
		assertNotNull(rs);
		EFactory manualFactory = rs.getPackageRegistry().getEFactory("http://fennec.eclipse.org/example/model/manual/1.0");
		assertNotNull(manualFactory);
		EPackage manualPackage = manualFactory.getEPackage();
		assertNotNull(manualPackage);
		// Foo class exists
		EClass manualFoo = (EClass) manualPackage.getEClassifier("Foo");
		assertNotNull(manualFoo);
		// Bar class does not exist in this package
		EClass manualBar = (EClass) manualPackage.getEClassifier("Bar");
		assertNull(manualBar);
		EFactory foobarFactory = rs.getPackageRegistry().getEFactory("http://foo.bar");
		assertNotNull(foobarFactory);
		EPackage foobarPackage = foobarFactory.getEPackage();
		assertNotNull(foobarPackage);
		// Foo class exists
		EClass foobarFoo = (EClass) foobarPackage.getEClassifier("Foo");
		assertNotNull(foobarFoo);
		// Bar class exists in this package
		EClass foobarBar = (EClass) foobarPackage.getEClassifier("Bar");
		assertNotNull(foobarBar);
	}
	
	/**
	 * Verifies that models from multiple folders are registered as separate
	 * {@link EPackage} services with distinct filter properties.
	 */
	@Test
	public void simpleMultipleFoldersEPackage(@InjectService(filter = "(" + EMFNamespaces.EMF_NAME + "=manual)") ServiceAware<EPackage> manualAware, @InjectService(filter = "(" + EMFNamespaces.EMF_NAME + "=foobar)") ServiceAware<EPackage> fooAware) {
		EPackage manualPackage = manualAware.getService();
		assertNotNull(manualPackage);
		// Foo class exists
		EClass manualFoo = (EClass) manualPackage.getEClassifier("Foo");
		assertNotNull(manualFoo);
		// Bar class does not exist in this package
		EClass manualBar = (EClass) manualPackage.getEClassifier("Bar");
		assertNull(manualBar);
		EPackage foobarPackage = fooAware.getService();
		assertNotNull(foobarPackage);
		// Foo class exists
		EClass foobarFoo = (EClass) foobarPackage.getEClassifier("Foo");
		assertNotNull(foobarFoo);
		// Bar class exists in this package
		EClass foobarBar = (EClass) foobarPackage.getEClassifier("Bar");
		assertNotNull(foobarBar);
	}
	
	/**
	 * Verifies that inline path properties ({@code ;foo=bar;test=me}) are propagated
	 * to the {@link EPackageConfigurator} service registrations and can be used for filtering.
	 */
	@Test
	public void simpleMultiplePropertiesFolders01(@InjectService(filter = "(|(foo=bar)(foo=baz))") ServiceAware<EPackageConfigurator> configAware) {
		List<ServiceReference<EPackageConfigurator>> references = configAware.getServiceReferences();
		assertEquals(2, references.size());
		EPackage.Registry registry = new EPackageRegistryImpl();
		EFactory manualFactory = registry.getEFactory("http://fennec.eclipse.org/example/model/manual/1.0");
		assertNull(manualFactory);
		EFactory foobarFactory = registry.getEFactory("http://foo.bar");
		assertNull(foobarFactory);
		
		for (ServiceReference<EPackageConfigurator> reference : references) {
			assertEquals("me", reference.getProperty("test"));
			String foo = (String) reference.getProperty("foo");
			switch (foo) {
			case "bar":
				assertEquals("manual", reference.getProperty(EMFNamespaces.EMF_NAME));
				break;
			case "baz":
				assertEquals("foobar", reference.getProperty(EMFNamespaces.EMF_NAME));
				break;
			default:
				fail("Unecpected value");
				break;
			}
			ctx.getService(reference).configureEPackage(registry);
		}
		manualFactory = registry.getEFactory("http://fennec.eclipse.org/example/model/manual/1.0");
		assertNotNull(manualFactory);
		EPackage manualPackage = manualFactory.getEPackage();
		assertNotNull(manualPackage);
		// Foo class exists
		EClass manualFoo = (EClass) manualPackage.getEClassifier("Foo");
		assertNotNull(manualFoo);
		// Bar class does not exist in this package
		EClass manualBar = (EClass) manualPackage.getEClassifier("Bar");
		assertNull(manualBar);
		foobarFactory = registry.getEFactory("http://foo.bar");
		assertNotNull(foobarFactory);
		EPackage foobarPackage = foobarFactory.getEPackage();
		assertNotNull(foobarPackage);
		// Foo class exists
		EClass foobarFoo = (EClass) foobarPackage.getEClassifier("Foo");
		assertNotNull(foobarFoo);
		// Bar class exists in this package
		EClass foobarBar = (EClass) foobarPackage.getEClassifier("Bar");
		assertNotNull(foobarBar);
	}
	
	/**
	 * Verifies that a shared inline property ({@code test=me}) matches multiple
	 * configurators and that each correctly registers its EPackage.
	 */
	@Test
	public void simpleMultiplePropertiesFolders02(@InjectService(filter = "(test=me)") ServiceAware<EPackageConfigurator> configAware) {
		List<ServiceReference<EPackageConfigurator>> references = configAware.getServiceReferences();
		assertEquals(2, references.size());
		EPackage.Registry registry = new EPackageRegistryImpl();
		EFactory manualFactory = registry.getEFactory("http://fennec.eclipse.org/example/model/manual/1.0");
		assertNull(manualFactory);
		EFactory foobarFactory = registry.getEFactory("http://foo.bar");
		assertNull(foobarFactory);
		
		List<EPackageConfigurator> configurators = configAware.getServices();
		for (EPackageConfigurator configurator : configurators) {
			configurator.configureEPackage(registry);
		}
		manualFactory = registry.getEFactory("http://fennec.eclipse.org/example/model/manual/1.0");
		assertNotNull(manualFactory);
		EPackage manualPackage = manualFactory.getEPackage();
		assertNotNull(manualPackage);
		// Foo class exists
		EClass manualFoo = (EClass) manualPackage.getEClassifier("Foo");
		assertNotNull(manualFoo);
		// Bar class does not exist in this package
		EClass manualBar = (EClass) manualPackage.getEClassifier("Bar");
		assertNull(manualBar);
		foobarFactory = registry.getEFactory("http://foo.bar");
		assertNotNull(foobarFactory);
		EPackage foobarPackage = foobarFactory.getEPackage();
		assertNotNull(foobarPackage);
		// Foo class exists
		EClass foobarFoo = (EClass) foobarPackage.getEClassifier("Foo");
		assertNotNull(foobarFoo);
		// Bar class exists in this package
		EClass foobarBar = (EClass) foobarPackage.getEClassifier("Bar");
		assertNotNull(foobarBar);
	}
	
	/**
	 * Verifies that a single ecore file path with inline properties is correctly loaded
	 * and registered with the expected custom property ({@code toast=me}).
	 */
	@Test
	public void simpleMultiplePropertiesFile(@InjectService(filter = "(toast=me)") ServiceAware<EPackageConfigurator> configAware) {
		List<ServiceReference<EPackageConfigurator>> references = configAware.getServiceReferences();
		assertEquals(1, references.size());
		EPackage.Registry registry = new EPackageRegistryImpl();
		EFactory toastFactory = registry.getEFactory("http://foo.bar/toast");
		assertNull(toastFactory);
		
		List<EPackageConfigurator> configurators = configAware.getServices();
		for (EPackageConfigurator configurator : configurators) {
			configurator.configureEPackage(registry);
		}
		EFactory manualFactory = registry.getEFactory("http://fennec.eclipse.org/example/model/manual/1.0");
		assertNull(manualFactory);
		EFactory foobarFactory = registry.getEFactory("http://foo.bar");
		assertNull(foobarFactory);
		
		toastFactory = registry.getEFactory("http://foo.bar/toast");
		assertNotNull(toastFactory);
		EPackage toastPackage = toastFactory.getEPackage();
		assertNotNull(toastPackage);
		// Foo class does not exists
		EClass foobarFoo = (EClass) toastPackage.getEClassifier("Foo");
		assertNull(foobarFoo);
		// Bar class does not exists in this package
		EClass foobarBar = (EClass) toastPackage.getEClassifier("Bar");
		assertNull(foobarBar);
		// FooToast class exists
		EClass toastFoo = (EClass) toastPackage.getEClassifier("FooToast");
		assertNotNull(toastFoo);
		// BarToast class exists in this package
		EClass toastBar = (EClass) toastPackage.getEClassifier("BarToast");
		assertNotNull(toastBar);
	}
	
	/**
	 * Verifies that a single ecore file with inline properties is also registered
	 * as an {@link EPackage} service with the matching custom property.
	 */
	@Test
	public void simpleMultiplePropertiesFileEPackage(@InjectService(filter = "(toast=me)") ServiceAware<EPackage> epackageAware) {
		List<ServiceReference<EPackage>> references = epackageAware.getServiceReferences();
		assertEquals(1, references.size());
		EPackage.Registry registry = new EPackageRegistryImpl();
		EFactory toastFactory = registry.getEFactory("http://foo.bar/toast");
		assertNull(toastFactory);
		
		List<EPackage> epackages = epackageAware.getServices();
		for (EPackage epackage : epackages) {
			registry.put(epackage.getNsURI(), epackage);
		}
		EFactory manualFactory = registry.getEFactory("http://fennec.eclipse.org/example/model/manual/1.0");
		assertNull(manualFactory);
		EFactory foobarFactory = registry.getEFactory("http://foo.bar");
		assertNull(foobarFactory);
		
		toastFactory = registry.getEFactory("http://foo.bar/toast");
		assertNotNull(toastFactory);
		EPackage toastPackage = toastFactory.getEPackage();
		assertNotNull(toastPackage);
		// Foo class does not exists
		EClass foobarFoo = (EClass) toastPackage.getEClassifier("Foo");
		assertNull(foobarFoo);
		// Bar class does not exists in this package
		EClass foobarBar = (EClass) toastPackage.getEClassifier("Bar");
		assertNull(foobarBar);
		// FooToast class exists
		EClass toastFoo = (EClass) toastPackage.getEClassifier("FooToast");
		assertNotNull(toastFoo);
		// BarToast class exists in this package
		EClass toastBar = (EClass) toastPackage.getEClassifier("BarToast");
		assertNotNull(toastBar);
	}

}
