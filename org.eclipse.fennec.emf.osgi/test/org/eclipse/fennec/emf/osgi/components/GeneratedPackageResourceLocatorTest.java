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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.resourceset.SynchronizedResourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link GeneratedPackageResourceLocator} and {@link GeneratedPackageLocations}. The Ecore
 * package stands in for a generated package, see issue #114.
 */
class GeneratedPackageResourceLocatorTest {

	private static final String BSN = "org.example.model";
	private static final URI LOCATION = URI.createURI("platform:/plugin/org.example.model/model/ecore.ecore");
	private static final String SCHEMA = """
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
			    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="shapes" nsURI="http://example.org/shapes" nsPrefix="shapes">
			  <eClassifiers xsi:type="ecore:EClass" name="Shape">
			    <eStructuralFeatures xsi:type="ecore:EReference" name="type" containment="true"
			        eType="ecore:EClass %s#//EClass"/>
			  </eClassifiers>
			</ecore:EPackage>
			""";

	private GeneratedPackageLocations locations;

	@BeforeEach
	void setUp() {
		locations = new GeneratedPackageLocations();
		locations.add(GeneratedPackageLocations.locationsOf(BSN,
				List.of(Map.of("uri", EcorePackage.eNS_URI, "ecore", "/model/ecore.ecore"))));
	}

	private ResourceSetImpl createResourceSet() {
		ResourceSetImpl resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
		new GeneratedPackageResourceLocator(resourceSet, locations::getNsUri);
		return resourceSet;
	}

	private EReference loadSchemaReference(ResourceSetImpl resourceSet, String typeLocation) throws IOException {
		Resource resource = resourceSet.createResource(URI.createURI("file:/probe/shapes.ecore"));
		try (InputStream in = new ByteArrayInputStream(SCHEMA.formatted(typeLocation).getBytes(StandardCharsets.UTF_8))) {
			resource.load(in, null);
		}
		EPackage ePackage = (EPackage) resource.getContents().get(0);
		return (EReference) ((EClass) ePackage.getEClassifier("Shape")).getEStructuralFeature("type");
	}

	@Test
	void testDeclaredLocationResolvesToRegisteredPackage() {
		ResourceSetImpl resourceSet = createResourceSet();

		assertSame(EcorePackage.eINSTANCE.eResource(), resourceSet.getResource(LOCATION, true));
		assertSame(EcorePackage.Literals.ECLASS, resourceSet.getEObject(LOCATION.appendFragment("//EClass"), true));
		assertTrue(resourceSet.getResources().isEmpty(), "No resource must be created for the location");
	}

	@Test
	void testProxyReferenceInLoadedSchemaResolves() throws IOException {
		ResourceSetImpl resourceSet = createResourceSet();

		EReference reference = loadSchemaReference(resourceSet, LOCATION.toString());

		assertSame(EcorePackage.Literals.ECLASS, reference.getEReferenceType());
		assertEquals(1, resourceSet.getResources().size(), "Only the schema itself may be contained");
	}

	@Test
	void testUriMapNormalizationResolvesToRegisteredPackage() throws IOException {
		ResourceSetImpl resourceSet = createResourceSet();
		URI mapped = URI.createURI("file:/models/ecore.ecore");
		resourceSet.getURIConverter().getURIMap().put(mapped, URI.createURI(EcorePackage.eNS_URI));

		EReference reference = loadSchemaReference(resourceSet, mapped.toString());

		assertSame(EcorePackage.Literals.ECLASS, reference.getEReferenceType());
		assertEquals(1, resourceSet.getResources().size(), "Only the schema itself may be contained");
	}

	@Test
	void testContainedResourceTakesPrecedence() {
		ResourceSetImpl resourceSet = createResourceSet();
		Resource contained = resourceSet.createResource(LOCATION);

		assertSame(contained, resourceSet.getResource(LOCATION, false));
	}

	@Test
	void testUnregisteredPackageIsNotResolved() {
		locations.add(Map.of(URI.createURI("platform:/plugin/org.example.other/model/other.ecore"), "http://example.org/notregistered"));
		ResourceSetImpl resourceSet = createResourceSet();

		assertNull(resourceSet.getResource(URI.createURI("platform:/plugin/org.example.other/model/other.ecore"), false));
		assertNull(resourceSet.getResource(URI.createURI("file:/unknown.ecore"), false));
	}

	@Test
	void testChainsWithAnInstalledResourceLocator() {
		SynchronizedResourceSetImpl resourceSet = new SynchronizedResourceSetImpl();
		new GeneratedPackageResourceLocator(resourceSet, locations::getNsUri);

		assertSame(EcorePackage.Literals.ECLASS, resourceSet.getEObject(LOCATION.appendFragment("//EClass"), true));
		Resource contained = resourceSet.createResource(URI.createURI("file:/contained.xmi"));
		assertSame(contained, resourceSet.getResource(URI.createURI("file:/contained.xmi"), false));
	}

	@Test
	void testLocationsOfCapabilityAttributes() {
		Map<URI, String> result = GeneratedPackageLocations.locationsOf(BSN, List.of(
				Map.of("uri", "http://example.org/a", "ecore", "/model/a.ecore"),
				Map.of("uri", "http://example.org/b", "ecore", "model/b.ecore"),
				Map.of("uri", "http://example.org/c", "ecore", "http://example.org/models/c.ecore"),
				Map.of("uri", "http://example.org/d"),
				Map.of("uri", "http://example.org/e", "ecore", " "),
				Map.of("ecore", "/model/f.ecore")));

		assertEquals(Map.of(
				URI.createURI("platform:/plugin/org.example.model/model/a.ecore"), "http://example.org/a",
				URI.createURI("platform:/plugin/org.example.model/model/b.ecore"), "http://example.org/b",
				URI.createURI("http://example.org/models/c.ecore"), "http://example.org/c"), result);
	}

	@Test
	void testRemoveKeepsTheMappingOfAnotherBundle() {
		Map<URI, String> first = Map.of(LOCATION, "http://example.org/first");
		Map<URI, String> second = Map.of(LOCATION, "http://example.org/second");
		GeneratedPackageLocations table = new GeneratedPackageLocations();
		table.add(first);
		table.add(second);

		table.remove(first);
		assertEquals("http://example.org/second", table.getNsUri(LOCATION).orElseThrow());

		table.remove(second);
		assertFalse(table.getNsUri(LOCATION).isPresent());
	}
}
