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
package org.gecko.emf.osgi.extender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.fennec.emf.osgi.extender.ModelExtenderConfigurator;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ModelExtenderConfigurator}.
 * <p>
 * Verifies null-guard in constructor, configure/unconfigure lifecycle with
 * a real {@link EPackage.Registry}, and idempotent unconfigure behaviour.
 */
class ModelExtenderConfiguratorTest {

	// --- Constructor null guards ---

	@Test
	void constructorRejectsNullEPackage() {
		assertThrows(NullPointerException.class, () -> new ModelExtenderConfigurator(null));
	}

	@Test
	void constructorRejectsNullNsURI() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("noUri");
		// nsURI is null by default
		assertThrows(NullPointerException.class, () -> new ModelExtenderConfigurator(ePackage));
	}

	// --- Configure / unconfigure lifecycle ---

	@Test
	void configureRegistersPackageInRegistry() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("testPkg");
		ePackage.setNsURI("http://test.org/testPkg");
		ePackage.setNsPrefix("tp");

		ModelExtenderConfigurator configurator = new ModelExtenderConfigurator(ePackage);
		EPackage.Registry registry = new EPackageRegistryImpl();

		// Pre-condition: not registered
		assertNull(registry.getEPackage("http://test.org/testPkg"));

		// Act
		configurator.configureEPackage(registry);

		// Assert
		assertEquals(ePackage, registry.getEPackage("http://test.org/testPkg"));
	}

	@Test
	void unconfigureRemovesPackageFromRegistry() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("testPkg");
		ePackage.setNsURI("http://test.org/testPkg");
		ePackage.setNsPrefix("tp");

		ModelExtenderConfigurator configurator = new ModelExtenderConfigurator(ePackage);
		EPackage.Registry registry = new EPackageRegistryImpl();

		configurator.configureEPackage(registry);
		assertTrue(registry.containsKey("http://test.org/testPkg"));

		// Act
		configurator.unconfigureEPackage(registry);

		// Assert
		assertFalse(registry.containsKey("http://test.org/testPkg"));
		assertNull(registry.getEPackage("http://test.org/testPkg"));
	}

	@Test
	void unconfigureOnEmptyRegistryDoesNotThrow() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setNsURI("http://test.org/notRegistered");

		ModelExtenderConfigurator configurator = new ModelExtenderConfigurator(ePackage);
		EPackage.Registry registry = new EPackageRegistryImpl();

		// Should not throw when removing a non-existent key
		configurator.unconfigureEPackage(registry);
		assertFalse(registry.containsKey("http://test.org/notRegistered"));
	}

	@Test
	void configureDoesNotAffectOtherPackagesInRegistry() {
		EPackage pkg1 = EcoreFactory.eINSTANCE.createEPackage();
		pkg1.setNsURI("http://test.org/pkg1");

		EPackage pkg2 = EcoreFactory.eINSTANCE.createEPackage();
		pkg2.setNsURI("http://test.org/pkg2");

		ModelExtenderConfigurator configurator1 = new ModelExtenderConfigurator(pkg1);
		ModelExtenderConfigurator configurator2 = new ModelExtenderConfigurator(pkg2);

		EPackage.Registry registry = new EPackageRegistryImpl();

		configurator1.configureEPackage(registry);
		configurator2.configureEPackage(registry);

		// Both present
		assertEquals(pkg1, registry.getEPackage("http://test.org/pkg1"));
		assertEquals(pkg2, registry.getEPackage("http://test.org/pkg2"));

		// Removing one does not affect the other
		configurator1.unconfigureEPackage(registry);
		assertNull(registry.getEPackage("http://test.org/pkg1"));
		assertEquals(pkg2, registry.getEPackage("http://test.org/pkg2"));
	}

	@Test
	void configureWithRealEcorePackage() {
		// Use the real EcorePackage singleton
		ModelExtenderConfigurator configurator = new ModelExtenderConfigurator(EcorePackage.eINSTANCE);
		EPackage.Registry registry = new EPackageRegistryImpl();

		configurator.configureEPackage(registry);

		assertEquals(EcorePackage.eINSTANCE, registry.getEPackage(EcorePackage.eNS_URI));
	}

	@Test
	void configureOverwritesExistingEntryWithSameNsURI() {
		EPackage original = EcoreFactory.eINSTANCE.createEPackage();
		original.setNsURI("http://test.org/shared");
		original.setName("original");

		EPackage replacement = EcoreFactory.eINSTANCE.createEPackage();
		replacement.setNsURI("http://test.org/shared");
		replacement.setName("replacement");

		EPackage.Registry registry = new EPackageRegistryImpl();
		new ModelExtenderConfigurator(original).configureEPackage(registry);
		new ModelExtenderConfigurator(replacement).configureEPackage(registry);

		// Last one wins
		assertEquals(replacement, registry.getEPackage("http://test.org/shared"));
	}
}
