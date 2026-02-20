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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.extender.model.Model;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Model}.
 * <p>
 * Verifies constructor validation (null guards), getters, defensive copy
 * behaviour of {@link Model#getProperties()}, and {@link Model#toString()}.
 */
class ModelTest {

	// --- Constructor null guards ---

	@Test
	void constructorRejectsNullEPackage() {
		Dictionary<String, Object> props = new Hashtable<>();
		assertThrows(NullPointerException.class, () -> new Model(null, props, 1L));
	}

	@Test
	void constructorRejectsNullProperties() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setNsURI("http://test");
		assertThrows(NullPointerException.class, () -> new Model(ePackage, null, 1L));
	}

	// --- Happy path ---

	@Test
	void gettersReturnConstructorValues() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("test");
		ePackage.setNsURI("http://test");

		Dictionary<String, Object> props = new Hashtable<>();
		props.put("key", "value");

		Model model = new Model(ePackage, props, 42L);

		assertEquals(ePackage, model.getEPackage());
		assertEquals(42L, model.getBundleId());
		Dictionary<String, Object> returnedProps = model.getProperties();
		assertEquals("value", returnedProps.get("key"));
	}

	@Test
	void getPropertiesReturnsDefensiveCopy() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("test");
		ePackage.setNsURI("http://test");

		Dictionary<String, Object> props = new Hashtable<>();
		props.put("key", "value");

		Model model = new Model(ePackage, props, 1L);

		Dictionary<String, Object> copy1 = model.getProperties();
		Dictionary<String, Object> copy2 = model.getProperties();

		// Each call returns a new instance
		assertNotSame(copy1, copy2);

		// Mutating the copy does not affect the model
		copy1.put("extra", "data");
		assertEquals(null, model.getProperties().get("extra"));
	}

	@Test
	void constructorDoesNotRetainReferenceToOriginalDictionary() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("test");
		ePackage.setNsURI("http://test");

		Dictionary<String, Object> props = new Hashtable<>();
		props.put("key", "value");

		Model model = new Model(ePackage, props, 1L);

		// Mutating the original dictionary after construction should not affect the model
		props.put("key", "changed");
		props.put("new", "entry");

		assertEquals("value", model.getProperties().get("key"));
		assertEquals(null, model.getProperties().get("new"));
	}

	// --- toString ---

	@Test
	void toStringContainsNamespaceAndBundleId() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("myModel");
		ePackage.setNsURI("http://example.org/myModel");

		Dictionary<String, Object> props = new Hashtable<>();
		Model model = new Model(ePackage, props, 7L);

		String str = model.toString();
		assertTrue(str.contains("http://example.org/myModel"), "toString should contain nsURI");
		assertTrue(str.contains("7"), "toString should contain bundleId");
	}

	// --- Edge cases ---

	@Test
	void emptyPropertiesDictionary() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setNsURI("http://test");

		Dictionary<String, Object> props = new Hashtable<>();
		Model model = new Model(ePackage, props, 0L);

		assertNotNull(model.getProperties());
		assertEquals(0, model.getProperties().size());
	}

	@Test
	void negativeBundleId() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setNsURI("http://test");

		Dictionary<String, Object> props = new Hashtable<>();
		Model model = new Model(ePackage, props, -1L);

		assertEquals(-1L, model.getBundleId());
	}

	@Test
	void realEcorePackageAsModel() {
		// Use a real EMF EPackage (EcorePackage) to verify behaviour with non-trivial packages
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("emf.name", EcorePackage.eINSTANCE.getName());

		Model model = new Model(EcorePackage.eINSTANCE, props, 99L);

		assertEquals(EcorePackage.eINSTANCE, model.getEPackage());
		assertEquals("ecore", model.getProperties().get("emf.name"));
		assertTrue(model.toString().contains(EcorePackage.eNS_URI));
	}
}
