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
package org.eclipse.fennec.emf.osgi.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EcoreHelper}.
 */
class EcoreHelperTest {

	// === createResourceSet ===

	@Test
	void testCreateResourceSet() {
		ResourceSet rs = EcoreHelper.createResourceSet();
		assertNotNull(rs);
		// EcorePackage registered
		assertEquals(EcorePackage.eINSTANCE, rs.getPackageRegistry().getEPackage(EcorePackage.eNS_URI));
		// Factory map entries
		Map<String, Object> factoryMap = rs.getResourceFactoryRegistry().getExtensionToFactoryMap();
		assertNotNull(factoryMap.get("ecore"));
		assertNotNull(factoryMap.get("xmi"));
		assertNotNull(factoryMap.get("*"));
	}

	// === Generic loading ===

	@Test
	void testLoadFromInputStreamWithType() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		try (InputStream is = getClass().getResourceAsStream("manual.ecore")) {
			EPackage ePackage = helper.load(is, URI.createURI("test://manual.ecore"), EPackage.class);
			assertNotNull(ePackage);
			assertEquals("manual", ePackage.getName());
		}
	}

	@Test
	void testLoadFromURLWithType() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		URL url = getClass().getResource("manual.ecore");
		assertNotNull(url);
		EPackage ePackage = helper.load(url, EPackage.class);
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
	}

	@Test
	void testLoadFromClasspathWithType() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.load("manual.ecore", getClass(), EPackage.class);
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
	}

	@Test
	void testLoadFromFilePathWithType() throws IOException {
		URL url = getClass().getResource("manual.ecore");
		assertNotNull(url);
		Path path = Path.of(java.net.URI.create(url.toExternalForm()));
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.load(path, EPackage.class);
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
	}

	@Test
	void testLoadAsEObject() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EObject eObject = helper.load("manual.ecore", getClass(), EObject.class);
		assertNotNull(eObject);
		assertTrue(eObject instanceof EPackage);
		assertEquals("manual", ((EPackage) eObject).getName());
	}

	@Test
	void testLoadTypeMismatch() {
		EcoreHelper helper = new EcoreHelper();
		// manual.ecore root is EPackage, not EClass
		assertThrows(IOException.class,
				() -> helper.load("manual.ecore", getClass(), EClass.class));
	}

	@Test
	void testLoadClasspathNotFound() {
		EcoreHelper helper = new EcoreHelper();
		assertThrows(IOException.class,
				() -> helper.load("nonexistent.xmi", getClass(), EObject.class));
	}

	// === Detach (public, EObject) ===

	@Test
	void testDetachRemovesResourceFromSet() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.load("manual.ecore", getClass(), EPackage.class);
		assertNotNull(ePackage.eResource());
		assertEquals(1, helper.getResourceSet().getResources().size());

		helper.detach(ePackage);
		assertNull(ePackage.eResource());
		assertTrue(helper.getResourceSet().getResources().isEmpty());
	}

	@Test
	void testDetachEObject() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EObject eObject = helper.load("manual.ecore", getClass(), EObject.class);
		assertNotNull(eObject.eResource());

		helper.detach(eObject);
		assertNull(eObject.eResource());
		assertTrue(helper.getResourceSet().getResources().isEmpty());
	}

	// === Attached EPackage loading ===

	@Test
	void testLoadEcoreFromInputStream() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		try (InputStream is = getClass().getResourceAsStream("manual.ecore")) {
			EPackage ePackage = helper.loadEcore(is, URI.createURI("test://manual.ecore"));
			assertNotNull(ePackage);
			assertEquals("manual", ePackage.getName());
			assertEquals("http://fennec.eclipse.org/example/model/manual/1.0", ePackage.getNsURI());
		}
	}

	@Test
	void testLoadEcoreFromURL() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		URL url = getClass().getResource("manual.ecore");
		assertNotNull(url, "Test resource manual.ecore must be on classpath");
		EPackage ePackage = helper.loadEcore(url);
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
	}

	@Test
	void testLoadEcoreFromClasspath() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
	}

	@Test
	void testLoadEcoreFromFilePath() throws IOException {
		URL url = getClass().getResource("manual.ecore");
		assertNotNull(url);
		// Convert URL to file path (works for file:// URLs)
		Path path = Path.of(java.net.URI.create(url.toExternalForm()));
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore(path);
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
	}

	@Test
	void testLoadEcoreAttachedHasResource() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		assertNotNull(ePackage.eResource(), "Attached EPackage must have a Resource");
		// URI is set to the EPackage nsURI
		assertEquals(URI.createURI(ePackage.getNsURI()), ePackage.eResource().getURI());
	}

	@Test
	void testLoadEcoreAttachedDiagnosticsAccessible() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		// Diagnostics must be accessible (and empty for valid ecore)
		assertNotNull(ePackage.eResource().getErrors());
		assertNotNull(ePackage.eResource().getWarnings());
		assertTrue(ePackage.eResource().getErrors().isEmpty());
		assertTrue(ePackage.eResource().getWarnings().isEmpty());
	}

	// === Detached loading ===

	@Test
	void testLoadEcoreDetachedNoResource() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcoreDetached("manual.ecore", getClass());
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
		assertNull(ePackage.eResource(), "Detached EPackage must not have a Resource");
		assertTrue(helper.getResourceSet().getResources().isEmpty(),
				"Detached resource must be removed from ResourceSet");
	}

	// === Content verification ===

	@Test
	void testLoadEcoreVerifyContent() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		assertEquals("manual", ePackage.getName());
		assertEquals("http://fennec.eclipse.org/example/model/manual/1.0", ePackage.getNsURI());
		assertEquals("manual", ePackage.getNsPrefix());
		assertFalse(ePackage.getEClassifiers().isEmpty());
		assertEquals("Foo", ePackage.getEClassifiers().get(0).getName());
	}

	// === Error cases ===

	@Test
	void testLoadEcoreNotFound() {
		EcoreHelper helper = new EcoreHelper();
		assertThrows(IOException.class,
				() -> helper.loadEcore("nonexistent.ecore", getClass()));
	}

	@Test
	void testLoadEcoreNullInputStream() {
		EcoreHelper helper = new EcoreHelper();
		assertThrows(NullPointerException.class,
				() -> helper.loadEcore(null, URI.createURI("test://test.ecore")));
	}

	// === Static one-shot loading ===

	@Test
	void testStaticLoadEcoreWithResourceSet() throws IOException {
		URL url = getClass().getResource("manual.ecore");
		assertNotNull(url);
		ResourceSet rs = EcoreHelper.createResourceSet();
		EPackage ePackage = EcoreHelper.loadEcore(url, rs);
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
	}

	// === getEClass / getFeature ===

	@Test
	void testGetEClass() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		EClass fooClass = EcoreHelper.getEClass(ePackage, "Foo");
		assertNotNull(fooClass);
		assertEquals("Foo", fooClass.getName());
	}

	@Test
	void testGetEClassNotFound() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		assertThrows(IllegalArgumentException.class,
				() -> EcoreHelper.getEClass(ePackage, "NonExistent"));
	}

	@Test
	void testGetFeature() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		EClass fooClass = EcoreHelper.getEClass(ePackage, "Foo");
		EStructuralFeature feature = EcoreHelper.getFeature(fooClass, "value");
		assertNotNull(feature);
		assertEquals("value", feature.getName());
	}

	@Test
	void testGetFeatureNotFound() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		EClass fooClass = EcoreHelper.getEClass(ePackage, "Foo");
		assertThrows(IllegalArgumentException.class,
				() -> EcoreHelper.getFeature(fooClass, "nonExistent"));
	}

	// === extractProperties ===

	@Test
	void testExtractPropertiesNoProperties() {
		assertNull(EcoreHelper.extractProperties(null, null));
		assertEquals("/test", EcoreHelper.extractProperties("/test", null));
		assertEquals("/test", EcoreHelper.extractProperties("/test;blub", null));
	}

	@Test
	void testExtractPropertiesPath() {
		Map<String, String> props = new HashMap<>();
		assertEquals("/test", EcoreHelper.extractProperties("/test", props));
		assertTrue(props.isEmpty());
		props.clear();

		assertEquals("", EcoreHelper.extractProperties("", props));
		assertTrue(props.isEmpty());
		props.clear();

		assertEquals("/test", EcoreHelper.extractProperties("/test;", props));
		assertTrue(props.isEmpty());
		props.clear();
	}

	@Test
	void testExtractPropertiesWorking() {
		Map<String, String> props = new HashMap<>();

		assertEquals("/test", EcoreHelper.extractProperties("/test;blub", props));
		assertFalse(props.isEmpty());
		assertTrue(props.containsKey("blub"));
		assertNull(props.get("blub"));
		props.clear();

		assertEquals("/test", EcoreHelper.extractProperties("/test;blub=bla", props));
		assertFalse(props.isEmpty());
		assertTrue(props.containsKey("blub"));
		assertEquals("bla", props.get("blub"));
		props.clear();

		assertEquals("/test", EcoreHelper.extractProperties("/test;blub=bla;blub=blubber", props));
		assertFalse(props.isEmpty());
		assertEquals(1, props.size());
		assertTrue(props.containsKey("blub"));
		assertEquals("blubber", props.get("blub"));
		props.clear();

		assertEquals("/test", EcoreHelper.extractProperties("/test;blub=bla;foo=bar", props));
		assertFalse(props.isEmpty());
		assertEquals(2, props.size());
		assertEquals("bla", props.get("blub"));
		assertEquals("bar", props.get("foo"));
		props.clear();

		assertEquals("/test", EcoreHelper.extractProperties("/test;blub;foo=bar", props));
		assertFalse(props.isEmpty());
		assertEquals(2, props.size());
		assertNull(props.get("blub"));
		assertEquals("bar", props.get("foo"));
		props.clear();

		assertEquals("", EcoreHelper.extractProperties(";blub=bla", props));
		assertFalse(props.isEmpty());
		assertEquals("bla", props.get("blub"));
		props.clear();
	}

	// === Bug fix verifications ===

	/**
	 * Fix 1: Verifies that a failed load (type mismatch) does not leak a Resource
	 * in the ResourceSet.
	 */
	@Test
	void testLoadTypeMismatchCleansUpResource() {
		EcoreHelper helper = new EcoreHelper();
		assertTrue(helper.getResourceSet().getResources().isEmpty());
		assertThrows(IOException.class,
				() -> helper.load("manual.ecore", getClass(), EClass.class));
		// Resource must be removed from ResourceSet after type mismatch error
		assertTrue(helper.getResourceSet().getResources().isEmpty(),
				"Failed load must not leave orphan Resource in ResourceSet");
	}

	/**
	 * Fix 1: Verifies that an empty/invalid InputStream does not leak a Resource.
	 */
	@Test
	void testLoadEmptyStreamCleansUpResource() {
		EcoreHelper helper = new EcoreHelper();
		InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
		assertThrows(IOException.class,
				() -> helper.load(emptyStream, URI.createURI("test://empty.ecore"), EPackage.class));
		assertTrue(helper.getResourceSet().getResources().isEmpty(),
				"Failed load from empty stream must not leave orphan Resource");
	}

	/**
	 * Fix 3: Verifies that loading an ecore without nsURI does not throw NPE.
	 */
	@Test
	void testLoadEcoreWithoutNsUri() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("no-nsuri.ecore", getClass());
		assertNotNull(ePackage);
		assertEquals("nouri", ePackage.getName());
		assertNull(ePackage.getNsURI());
		// Resource URI should remain the original (not overwritten with null nsURI)
		assertNotNull(ePackage.eResource());
		assertNotNull(ePackage.eResource().getURI());
	}

	/**
	 * Fix 4: Verifies that releaseAll works with multiple resources without
	 * ConcurrentModificationException.
	 */
	@Test
	void testReleaseAllMultipleResources() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		helper.loadEcore("manual.ecore", getClass());
		helper.loadEcore("no-nsuri.ecore", getClass());
		assertEquals(2, helper.getResourceSet().getResources().size());

		// Must not throw ConcurrentModificationException
		helper.releaseAll();
		assertTrue(helper.getResourceSet().getResources().isEmpty());
	}

	// === releaseAll ===

	@Test
	void testReleaseAll() throws IOException {
		EcoreHelper helper = new EcoreHelper();
		EPackage ePackage = helper.loadEcore("manual.ecore", getClass());
		assertNotNull(ePackage.eResource());
		assertFalse(helper.getResourceSet().getResources().isEmpty());

		helper.releaseAll();
		assertTrue(helper.getResourceSet().getResources().isEmpty());
		// After release, the EPackage's resource is unloaded
		assertNull(ePackage.eResource());
	}
}
