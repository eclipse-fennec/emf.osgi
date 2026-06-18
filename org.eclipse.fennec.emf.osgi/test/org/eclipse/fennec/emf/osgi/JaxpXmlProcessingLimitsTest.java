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
package org.eclipse.fennec.emf.osgi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies how EMF's XMI loading behaves with respect to the JDK's JAXP secure
 * processing limits, using a dynamically generated EMF model and a real,
 * EMF-serialized instance document.
 * <p>
 * Starting with JDK 24 (<a href="https://bugs.openjdk.org/browse/JDK-8343006">JDK-8343006</a>)
 * a number of {@code jdk.xml.*} default limits were tightened. Two of them are
 * reachable by ordinary, entity-free XMI:
 * <ul>
 * <li>{@code jdk.xml.elementAttributeLimit}: {@code 10000 -> 200}. An EObject
 * with more than 200 single-valued attributes serializes to one element with
 * more than 200 XML attributes.</li>
 * <li>{@code jdk.xml.maxElementDepth}: {@code 0 (unlimited) -> 100}. A deep
 * chain of containment references serializes to deeply nested elements.</li>
 * </ul>
 * The entity-size limits ({@code maxGeneralEntitySizeLimit},
 * {@code totalEntitySizeLimit}, ... now {@code 100000}) only apply to documents
 * using XML entities and are therefore never exercised by normal XMI.
 * <p>
 * EMF loads XMI through the JDK SAX parser and does <em>not</em> relax these
 * limits, so on JDK 24+ such documents fail to load with a
 * {@link org.xml.sax.SAXParseException} (JDK error codes {@code JAXP00010002}
 * for attributes, {@code JAXP00010006} for depth), wrapped in a
 * {@link org.eclipse.emf.ecore.resource.Resource.IOWrappedException}.
 */
public class JaxpXmlProcessingLimitsTest {

	/** JDK feature version in which the JAXP default limits were tightened. */
	private static final int LIMITS_TIGHTENED_IN = 24;

	private static final String ATTRIBUTE_LIMIT_PROPERTY = "jdk.xml.elementAttributeLimit";
	private static final String ELEMENT_DEPTH_PROPERTY = "jdk.xml.maxElementDepth";

	/** JDK error code emitted when the element attribute limit is exceeded. */
	private static final String JAXP_ATTRIBUTE_LIMIT_CODE = "JAXP00010002";
	/** JDK error code emitted when the element depth limit is exceeded. */
	private static final String JAXP_ELEMENT_DEPTH_CODE = "JAXP00010006";

	private static final String NS_URI = "http://example.org/jaxptest";

	/** Above the JDK 24+ attribute default of 200, below the pre-24 default of 10000. */
	private static final int ATTRIBUTE_COUNT = 250;
	/** Above the JDK 24+ element-depth default of 100. */
	private static final int NEST_DEPTH = 130;

	private final boolean limitsTightenedByDefault = Runtime.version().feature() >= LIMITS_TIGHTENED_IN;

	@AfterEach
	public void clearLimitProperties() {
		// The limits are read by the SAX parser factory from system properties,
		// so make sure a test that pins them does not leak into the next one.
		System.clearProperty(ATTRIBUTE_LIMIT_PROPERTY);
		System.clearProperty(ELEMENT_DEPTH_PROPERTY);
	}

	// ---------------------------------------------------------------------
	// Default-limit behaviour: this is what our CI hits on the Java 25 rail.
	// ---------------------------------------------------------------------

	@Test
	public void wideInstanceHitsAttributeLimitOnJdk24Plus() throws IOException {
		EPackage model = buildModel();
		byte[] xmi = saveWideInstance(model);

		Throwable error = loadAndCaptureError(model, xmi);

		if (limitsTightenedByDefault) {
			assertNotNull(error, "JDK 24+ enforces the 200 attribute default, but the document loaded");
			assertTrue(messageChain(error).contains(JAXP_ATTRIBUTE_LIMIT_CODE),
					() -> "expected " + JAXP_ATTRIBUTE_LIMIT_CODE + " but got: " + messageChain(error));
		} else {
			assertNull(error, () -> "pre-24 JDKs should load a 250-attribute document, but got: " + messageChain(error));
		}
	}

	@Test
	public void deeplyNestedInstanceHitsElementDepthLimitOnJdk24Plus() throws IOException {
		EPackage model = buildModel();
		byte[] xmi = saveDeepInstance(model, NEST_DEPTH);

		Throwable error = loadAndCaptureError(model, xmi);

		if (limitsTightenedByDefault) {
			assertNotNull(error, "JDK 24+ enforces the 100 element-depth default, but the document loaded");
			assertTrue(messageChain(error).contains(JAXP_ELEMENT_DEPTH_CODE),
					() -> "expected " + JAXP_ELEMENT_DEPTH_CODE + " but got: " + messageChain(error));
		} else {
			assertNull(error, () -> "pre-24 JDKs should load a 130-deep document, but got: " + messageChain(error));
		}
	}

	// ---------------------------------------------------------------------
	// Deterministic guards that run identically on every JDK by pinning the
	// limits via system properties.
	// ---------------------------------------------------------------------

	@Test
	public void wideInstanceFailsWhenAttributeLimitPinnedLow() throws IOException {
		EPackage model = buildModel();
		byte[] xmi = saveWideInstance(model);

		System.setProperty(ATTRIBUTE_LIMIT_PROPERTY, "200");
		Throwable error = loadAndCaptureError(model, xmi);

		assertNotNull(error, "a 250-attribute document must fail when the limit is pinned to 200");
		assertTrue(messageChain(error).contains(JAXP_ATTRIBUTE_LIMIT_CODE),
				() -> "expected " + JAXP_ATTRIBUTE_LIMIT_CODE + " but got: " + messageChain(error));
	}

	@Test
	public void instancesRoundTripWhenLimitsRelaxed() throws IOException {
		EPackage model = buildModel();

		// 0 (or any value <= 0) disables the limit - this is the documented
		// escape hatch for applications that legitimately produce such XMI.
		System.setProperty(ATTRIBUTE_LIMIT_PROPERTY, "0");
		System.setProperty(ELEMENT_DEPTH_PROPERTY, "0");

		Resource wide = load(model, saveWideInstance(model));
		assertEquals(1, wide.getContents().size(), "wide instance should round-trip when limits are relaxed");
		assertTrue(wide.getErrors().isEmpty(), () -> "unexpected load errors: " + wide.getErrors());

		Resource deep = load(model, saveDeepInstance(model, NEST_DEPTH));
		assertEquals(1, deep.getContents().size(), "deep instance should round-trip when limits are relaxed");
		assertTrue(deep.getErrors().isEmpty(), () -> "unexpected load errors: " + deep.getErrors());
	}

	// ---------------------------------------------------------------------
	// Model + document construction.
	// ---------------------------------------------------------------------

	/**
	 * Builds a dynamic EMF model with a single {@code Node} EClass that has
	 * {@value #ATTRIBUTE_COUNT} single-valued string attributes and a
	 * self-containment reference (so instances can be nested arbitrarily deep).
	 */
	private static EPackage buildModel() {
		EcoreFactory ef = EcoreFactory.eINSTANCE;
		EPackage pkg = ef.createEPackage();
		pkg.setName("jaxptest");
		pkg.setNsPrefix("jaxptest");
		pkg.setNsURI(NS_URI);

		EClass node = ef.createEClass();
		node.setName("Node");
		pkg.getEClassifiers().add(node);

		for (int i = 0; i < ATTRIBUTE_COUNT; i++) {
			EAttribute attribute = ef.createEAttribute();
			attribute.setName("attr" + i);
			attribute.setEType(EcorePackage.Literals.ESTRING);
			node.getEStructuralFeatures().add(attribute);
		}

		EReference child = ef.createEReference();
		child.setName("child");
		child.setEType(node);
		child.setContainment(true);
		child.setLowerBound(0);
		child.setUpperBound(1);
		node.getEStructuralFeatures().add(child);

		return pkg;
	}

	/** Creates a {@code Node} with all attributes set and serializes it to XMI bytes. */
	private static byte[] saveWideInstance(EPackage model) throws IOException {
		EClass node = (EClass) model.getEClassifier("Node");
		EObject root = model.getEFactoryInstance().create(node);
		for (int i = 0; i < ATTRIBUTE_COUNT; i++) {
			root.eSet(node.getEStructuralFeature("attr" + i), "value" + i);
		}
		return save(model, root);
	}

	/** Creates a chain of {@code depth} nested {@code Node}s and serializes it to XMI bytes. */
	private static byte[] saveDeepInstance(EPackage model, int depth) throws IOException {
		EClass node = (EClass) model.getEClassifier("Node");
		EReference child = (EReference) node.getEStructuralFeature("child");
		EObject root = model.getEFactoryInstance().create(node);
		EObject current = root;
		for (int i = 1; i < depth; i++) {
			EObject next = model.getEFactoryInstance().create(node);
			current.eSet(child, next);
			current = next;
		}
		return save(model, root);
	}

	private static byte[] save(EPackage model, EObject root) throws IOException {
		ResourceSet rs = newResourceSet(model);
		Resource resource = rs.createResource(URI.createFileURI("jaxp-limit-instance.xmi"));
		resource.getContents().add(root);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		resource.save(out, Map.of());
		return out.toByteArray();
	}

	// ---------------------------------------------------------------------
	// Loading helpers.
	// ---------------------------------------------------------------------

	private static Resource load(EPackage model, byte[] xmi) throws IOException {
		ResourceSet rs = newResourceSet(model);
		Resource resource = rs.createResource(URI.createFileURI("jaxp-limit-reload.xmi"));
		resource.load(new ByteArrayInputStream(xmi), Map.of());
		return resource;
	}

	private static Throwable loadAndCaptureError(EPackage model, byte[] xmi) {
		try {
			load(model, xmi);
			return null;
		} catch (Exception e) {
			return e;
		}
	}

	private static ResourceSet newResourceSet(EPackage model) {
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getPackageRegistry().put(model.getNsURI(), model);
		return rs;
	}

	/** Flattens an exception and its cause chain into a single searchable string. */
	private static String messageChain(Throwable t) {
		StringBuilder sb = new StringBuilder();
		for (Throwable current = t; current != null; current = current.getCause()) {
			if (sb.length() > 0) {
				sb.append(" | ");
			}
			sb.append(current.getClass().getName()).append(": ").append(current.getMessage());
			if (current.getCause() == current) {
				break;
			}
		}
		return sb.toString();
	}
}
