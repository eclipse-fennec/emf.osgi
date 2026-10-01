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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.extender.ModelHelper;
import org.eclipse.fennec.emf.osgi.extender.model.Model;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.eclipse.fennec.emf.osgi.helper.EcoreHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link ModelHelper}.
 * <p>
 * Tests the publicly accessible utility methods: {@code extractProperties},
 * {@code readModelsFromBundle} (with null paths), and {@code loadModelInstance}
 * using a real {@code .ecore} file from the test resources.
 */
class ModelHelperTest {

	/**
	 * Resolves the {@code manual.ecore} test resource located in the same package.
	 */
	private URL getManualEcoreUrl() {
		URL url = getClass().getResource("manual.ecore");
		assertNotNull(url, "manual.ecore test resource not found on classpath");
		return url;
	}

	// ===== extractProperties - extended tests beyond ModelUtilsTest =====

	@Test
	void extractPropertiesNullPathReturnsNull() {
		assertNull(ModelHelper.extractProperties(null, null));
	}

	@Test
	void extractPropertiesEmptyPathReturnsEmpty() {
		Map<String, String> props = new HashMap<>();
		assertEquals("", ModelHelper.extractProperties("", props));
		assertTrue(props.isEmpty());
	}

	@Test
	void extractPropertiesNullMapDoesNotThrow() {
		// Passing null map should still return the plain path
		assertEquals("/model", ModelHelper.extractProperties("/model;key=val", null));
	}

	@Test
	void extractPropertiesMultipleSemicolonsOnly() {
		Map<String, String> props = new HashMap<>();
		String result = ModelHelper.extractProperties("/path;;;", props);
		assertEquals("/path", result);
	}

	// ===== readModelsFromBundle - null paths guard (N3 related) =====

	@Test
	void readModelsFromBundleWithNullPathsReturnsEmptyList() {
		List<Model> models = ModelHelper.readModelsFromBundle(null, null, null, new ModelHelper.Diagnostic());
		assertNotNull(models);
		assertTrue(models.isEmpty());
	}

	@Test
	void readModelsFromBundleWithEmptyPathsReturnsEmptyList() {
		List<Model> models = ModelHelper.readModelsFromBundle(null, null, Collections.emptySet(), new ModelHelper.Diagnostic());
		assertNotNull(models);
		assertTrue(models.isEmpty());
	}

	// ===== Diagnostic =====

	@Test
	void diagnosticInitiallyEmpty() {
		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();
		assertTrue(diagnostic.warnings.isEmpty());
		assertTrue(diagnostic.errors.isEmpty());
	}

	@Test
	void diagnosticCollectsWarningsAndErrors() {
		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();
		diagnostic.warnings.add("warn1");
		diagnostic.errors.add("err1");
		diagnostic.errors.add("err2");

		assertEquals(1, diagnostic.warnings.size());
		assertEquals(2, diagnostic.errors.size());
		assertEquals("warn1", diagnostic.warnings.get(0));
		assertEquals("err1", diagnostic.errors.get(0));
	}

	// ===== loadModelInstance - cross-package references by relative path =====

	private static final String TARGET_ECORE = """
		<?xml version="1.0" encoding="UTF-8"?>
		<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
		    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
		    name="target" nsURI="http://test/target" nsPrefix="target">
		  <eClassifiers xsi:type="ecore:EClass" name="Thing"/>
		</ecore:EPackage>
		""";

	private static final String USER_ECORE = """
		<?xml version="1.0" encoding="UTF-8"?>
		<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
		    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
		    name="user" nsURI="http://test/user" nsPrefix="user">
		  <eClassifiers xsi:type="ecore:EClass" name="Holder">
		    <eStructuralFeatures xsi:type="ecore:EReference" name="thing" eType="ecore:EClass target.ecore#//Thing"/>
		  </eClassifiers>
		</ecore:EPackage>
		""";

	/**
	 * Regression guard for the fingerprint guide's claim: a relative reference resolves on demand
	 * from the sibling file while hashing, so the value equals the fully resolved one.
	 */
	@Test
	void loadModelInstanceResolvesRelativeReferenceFromSiblingFile(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("target.ecore"), TARGET_ECORE);
		Path userFile = Files.writeString(dir.resolve("user.ecore"), USER_ECORE);

		// the value a fully resolved model has - what the generator burns into the constant
		ResourceSet reference = EcoreHelper.createResourceSet();
		reference.getResource(URI.createFileURI(dir.resolve("target.ecore").toString()), true);
		EPackage resolved = (EPackage) reference.getResource(URI.createFileURI(userFile.toString()), true)
				.getContents().get(0);
		EcoreUtil.resolveAll(reference);
		String expected = FingerprintHelper.fingerprint(resolved);

		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();
		Model model = ModelHelper.loadModelInstance(7L, EcoreHelper.createResourceSet(), userFile.toUri().toURL(),
				null, diagnostic);

		assertTrue(diagnostic.warnings.isEmpty(), "Expected no warnings, got: " + diagnostic.warnings);
		assertEquals(expected, model.getProperties().get(EMFNamespaces.EMF_MODEL_FINGERPRINT),
				"a relative reference resolved from the sibling file must hash like the generated model");
	}

	@Test
	void loadModelInstanceWarnsAboutUnresolvedRelativeReference(@TempDir Path dir) throws IOException {
		// no target.ecore next to it
		Path userFile = Files.writeString(dir.resolve("user.ecore"), USER_ECORE);

		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();
		Model model = ModelHelper.loadModelInstance(7L, EcoreHelper.createResourceSet(), userFile.toUri().toURL(),
				null, diagnostic);

		assertNotNull(model.getProperties().get(EMFNamespaces.EMF_MODEL_FINGERPRINT));
		assertEquals(1, diagnostic.warnings.size(), "Expected one warning, got: " + diagnostic.warnings);
		String warning = diagnostic.warnings.get(0);
		assertTrue(warning.contains("http://test/user"), warning);
		assertTrue(warning.contains("1 unresolved proxy target(s)"), warning);
		assertTrue(warning.contains("target.ecore#//Thing"), warning);
		assertTrue(diagnostic.errors.isEmpty(), "Expected no errors, got: " + diagnostic.errors);
	}

	// ===== loadModelInstance - using real ecore file =====

	@Test
	void loadModelInstanceFromRealEcore() throws IOException {
		URL ecoreUrl = getManualEcoreUrl();

		EcoreHelper ecoreHelper = new EcoreHelper();
		ResourceSet resourceSet = ecoreHelper.getResourceSet();
		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();
		Map<String, String> properties = new HashMap<>();

		Model model = ModelHelper.loadModelInstance(42L, resourceSet, ecoreUrl, properties, diagnostic);

		assertNotNull(model);
		EPackage ePackage = model.getEPackage();
		assertNotNull(ePackage);
		assertEquals("manual", ePackage.getName());
		assertEquals("http://fennec.eclipse.org/example/model/manual/1.0", ePackage.getNsURI());
		assertEquals("manual", ePackage.getNsPrefix());
		assertEquals(42L, model.getBundleId());

		// Verify standard service properties
		Dictionary<String, Object> svcProps = model.getProperties();
		assertEquals("manual", svcProps.get(EMFNamespaces.EMF_NAME));
		assertEquals("http://fennec.eclipse.org/example/model/manual/1.0", svcProps.get(EMFNamespaces.EMF_MODEL_NSURI));
		assertEquals(EMFNamespaces.MODEL_REGISTRATION_EXTENDER, svcProps.get(EMFNamespaces.EMF_MODEL_REGISTRATION));
		// Default scope should be "static" when not specified in properties
		assertEquals(EMFNamespaces.EMF_MODEL_SCOPE_STATIC, svcProps.get(EMFNamespaces.EMF_MODEL_SCOPE));
		// The fingerprint is part of the shared property core and must match the
		// static helper's value for the loaded package (issue #54)
		assertEquals(FingerprintHelper.fingerprint(ePackage), svcProps.get(EMFNamespaces.EMF_MODEL_FINGERPRINT));

		// No errors or warnings expected for a valid ecore
		assertTrue(diagnostic.errors.isEmpty(), "Expected no errors, got: " + diagnostic.errors);
		assertTrue(diagnostic.warnings.isEmpty(), "Expected no warnings, got: " + diagnostic.warnings);

		ecoreHelper.releaseAll();
	}

	@Test
	void loadModelInstanceWithCustomScopePropertyDoesNotOverride() throws IOException {
		URL ecoreUrl = getClass().getClassLoader().getResource("org/eclipse/fennec/emf/osgi/helper/manual.ecore");
		if (ecoreUrl == null) {
			ecoreUrl = new URL("file:../org.eclipse.fennec.emf.osgi.api/test/org/eclipse/fennec/emf/osgi/helper/manual.ecore");
		}

		EcoreHelper ecoreHelper = new EcoreHelper();
		ResourceSet resourceSet = ecoreHelper.getResourceSet();
		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();

		// Provide custom scope property
		Map<String, String> properties = new HashMap<>();
		properties.put(EMFNamespaces.EMF_MODEL_SCOPE, "resourceset");

		Model model = ModelHelper.loadModelInstance(1L, resourceSet, ecoreUrl, properties, diagnostic);
		assertNotNull(model);

		Dictionary<String, Object> svcProps = model.getProperties();
		// Custom scope should be preserved, not overwritten with "static"
		assertEquals("resourceset", svcProps.get(EMFNamespaces.EMF_MODEL_SCOPE));

		ecoreHelper.releaseAll();
	}

	@Test
	void loadModelInstanceWithNullPropertiesMap() throws IOException {
		URL ecoreUrl = getClass().getClassLoader().getResource("org/eclipse/fennec/emf/osgi/helper/manual.ecore");
		if (ecoreUrl == null) {
			ecoreUrl = new URL("file:../org.eclipse.fennec.emf.osgi.api/test/org/eclipse/fennec/emf/osgi/helper/manual.ecore");
		}

		EcoreHelper ecoreHelper = new EcoreHelper();
		ResourceSet resourceSet = ecoreHelper.getResourceSet();
		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();

		// Null properties map - should not throw and should still set standard properties
		Model model = ModelHelper.loadModelInstance(1L, resourceSet, ecoreUrl, null, diagnostic);
		assertNotNull(model);

		Dictionary<String, Object> svcProps = model.getProperties();
		assertEquals("manual", svcProps.get(EMFNamespaces.EMF_NAME));
		assertEquals(EMFNamespaces.EMF_MODEL_SCOPE_STATIC, svcProps.get(EMFNamespaces.EMF_MODEL_SCOPE));

		ecoreHelper.releaseAll();
	}

	@Test
	void loadModelInstanceWithInlineProperties() throws IOException {
		URL ecoreUrl = getClass().getClassLoader().getResource("org/eclipse/fennec/emf/osgi/helper/manual.ecore");
		if (ecoreUrl == null) {
			ecoreUrl = new URL("file:../org.eclipse.fennec.emf.osgi.api/test/org/eclipse/fennec/emf/osgi/helper/manual.ecore");
		}

		EcoreHelper ecoreHelper = new EcoreHelper();
		ResourceSet resourceSet = ecoreHelper.getResourceSet();
		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();

		// Provide additional custom properties (as if extracted from path)
		Map<String, String> properties = new HashMap<>();
		properties.put("custom.key", "custom.value");

		Model model = ModelHelper.loadModelInstance(5L, resourceSet, ecoreUrl, properties, diagnostic);
		assertNotNull(model);

		Dictionary<String, Object> svcProps = model.getProperties();
		// Custom property should be present
		assertEquals("custom.value", svcProps.get("custom.key"));
		// Standard properties should still be present
		assertEquals("manual", svcProps.get(EMFNamespaces.EMF_NAME));

		ecoreHelper.releaseAll();
	}

	@Test
	void loadModelInstanceInvalidUrlThrowsIOException() {
		EcoreHelper ecoreHelper = new EcoreHelper();
		ResourceSet resourceSet = ecoreHelper.getResourceSet();
		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();

		assertThrows(Exception.class, () ->
			ModelHelper.loadModelInstance(1L, resourceSet,
				new URL("file:///nonexistent/path/does_not_exist.ecore"),
				new HashMap<>(), diagnostic));

		ecoreHelper.releaseAll();
	}

	// ===== readModel with null path (N3 fix) =====

	@Test
	void readModelWithNullPathRecordsError() {
		ModelHelper.Diagnostic diagnostic = new ModelHelper.Diagnostic();
		// null path entry in the set - extractProperties returns null for null input
		List<Model> models = ModelHelper.readModel(null, null, null, diagnostic);
		assertTrue(models.isEmpty());
		assertFalse(diagnostic.errors.isEmpty());
		assertTrue(diagnostic.errors.get(0).contains("null"));
	}
}
