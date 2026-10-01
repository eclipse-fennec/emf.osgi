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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.fennec.emf.osgi.helper.XMLResourceIDs.IdentifiedCopy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link XMLResourceIDs}.
 */
class XMLResourceIDsTest {

	private EClass nodeClass;
	private EReference childrenRef;
	private EReference linkRef;

	private EObject root;
	private EObject childA;
	private EObject childB;

	@BeforeEach
	void setUp() {
		EcoreFactory factory = EcoreFactory.eINSTANCE;
		EPackage ePackage = factory.createEPackage();
		ePackage.setName("test");
		ePackage.setNsURI("http://test/xmlresourceids");
		ePackage.setNsPrefix("test");

		nodeClass = factory.createEClass();
		nodeClass.setName("Node");
		childrenRef = factory.createEReference();
		childrenRef.setName("children");
		childrenRef.setEType(nodeClass);
		childrenRef.setContainment(true);
		childrenRef.setUpperBound(-1);
		linkRef = factory.createEReference();
		linkRef.setName("link");
		linkRef.setEType(nodeClass);
		nodeClass.getEStructuralFeatures().add(childrenRef);
		nodeClass.getEStructuralFeatures().add(linkRef);
		ePackage.getEClassifiers().add(nodeClass);

		root = EcoreUtil.create(nodeClass);
		childA = EcoreUtil.create(nodeClass);
		childB = EcoreUtil.create(nodeClass);
		children(root).add(childA);
		children(root).add(childB);
		childA.eSet(linkRef, childB);
	}

	@SuppressWarnings("unchecked")
	private EList<EObject> children(EObject node) {
		return (EList<EObject>) node.eGet(childrenRef);
	}

	private XMLResource sourceWithIDs() {
		XMLResource source = new XMIResourceImpl(URI.createURI("source.xmi"));
		source.getContents().add(root);
		source.setID(root, "_root");
		source.setID(childA, "_a");
		source.setID(childB, "_b");
		return source;
	}

	private static String save(Resource resource) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		resource.save(out, null);
		return out.toString(StandardCharsets.UTF_8);
	}

	// ===== collectIDs =====

	@Test
	void collectIDsReturnsRootAndContainedIDsInContainmentOrder() {
		sourceWithIDs();

		Map<EObject, String> ids = XMLResourceIDs.collectIDs(root);

		assertEquals(List.of(root, childA, childB), List.copyOf(ids.keySet()));
		assertEquals(List.of("_root", "_a", "_b"), List.copyOf(ids.values()));
	}

	@Test
	void collectIDsSkipsObjectsWithoutID() {
		XMLResource source = new XMIResourceImpl(URI.createURI("source.xmi"));
		source.getContents().add(root);
		source.setID(childA, "_a");

		assertEquals(Map.of(childA, "_a"), XMLResourceIDs.collectIDs(root));
	}

	@Test
	void collectIDsOfDetachedObjectIsEmpty() {
		assertTrue(XMLResourceIDs.collectIDs(root).isEmpty());
	}

	@Test
	void collectIDsFromNonXMLResourceIsEmpty() {
		new ResourceImpl(URI.createURI("plain")).getContents().add(root);

		assertTrue(XMLResourceIDs.collectIDs(root).isEmpty());
	}

	// ===== moveWithIDs =====

	@Test
	void moveWithIDsKeepsIDsAndIntraDocumentReferences() throws IOException {
		XMLResource source = sourceWithIDs();
		XMLResource target = new XMIResourceImpl(URI.createURI("target.xmi"));

		XMLResourceIDs.moveWithIDs(root, target);

		assertTrue(source.getContents().isEmpty());
		assertSame(target, root.eResource());
		assertEquals("_root", target.getID(root));
		assertEquals("_a", target.getID(childA));
		assertEquals("_b", target.getID(childB));
		assertTrue(save(target).contains("link=\"_b\""), "the reference must serialize by id");
	}

	@Test
	void plainMoveLosesIDs() {
		sourceWithIDs();
		XMLResource target = new XMIResourceImpl(URI.createURI("target.xmi"));

		target.getContents().add(root);

		assertNull(target.getID(childA), "documents the EMF behaviour the helper exists for");
	}

	@Test
	void moveWithIDsFromSourceWithoutIDsInventsNone() {
		new XMIResourceImpl(URI.createURI("source.xmi")).getContents().add(root);
		XMLResource target = new XMIResourceImpl(URI.createURI("target.xmi"));

		XMLResourceIDs.moveWithIDs(root, target);

		assertSame(target, root.eResource());
		assertTrue(target.getEObjectToIDMap().isEmpty());
	}

	@Test
	void moveWithIDsOfDetachedObjectIntoXMLResource() {
		XMLResource target = new XMIResourceImpl(URI.createURI("target.xmi"));

		XMLResourceIDs.moveWithIDs(root, target);

		assertSame(target, root.eResource());
		assertTrue(target.getEObjectToIDMap().isEmpty());
	}

	@Test
	void moveWithIDsIntoNonXMLResource() {
		sourceWithIDs();
		Resource target = new ResourceImpl(URI.createURI("plain"));

		XMLResourceIDs.moveWithIDs(root, target);

		assertSame(target, root.eResource());
	}

	// ===== copyWithIDs =====

	@Test
	void copyWithIDsIntoTargetCarriesTheSameIDs() throws IOException {
		XMLResource source = sourceWithIDs();
		XMLResource target = new XMIResourceImpl(URI.createURI("target.xmi"));

		EObject copy = XMLResourceIDs.copyWithIDs(root, target);

		assertNotSame(root, copy);
		assertSame(target, copy.eResource());
		EObject copyA = children(copy).get(0);
		EObject copyB = children(copy).get(1);
		assertEquals("_root", target.getID(copy));
		assertEquals("_a", target.getID(copyA));
		assertEquals("_b", target.getID(copyB));
		assertSame(copyB, copyA.eGet(linkRef), "references must point into the copy");
		assertTrue(save(target).contains("link=\"_b\""), "the reference must serialize by id");
		// the original keeps its ids
		assertSame(source, root.eResource());
		assertEquals("_a", source.getID(childA));
	}

	@Test
	void copyWithIDsReturnsDetachedCopyWithIDsKeyedByCopies() {
		sourceWithIDs();

		IdentifiedCopy<EObject> identifiedCopy = XMLResourceIDs.copyWithIDs(root);

		EObject copy = identifiedCopy.copy();
		assertNull(copy.eResource());
		assertEquals(Map.of(copy, "_root", children(copy).get(0), "_a", children(copy).get(1), "_b"),
				identifiedCopy.ids());

		XMLResource target = new XMIResourceImpl(URI.createURI("target.xmi"));
		target.getContents().add(copy);
		XMLResourceIDs.applyIDs(identifiedCopy.ids(), target);
		assertEquals("_b", target.getID(children(copy).get(1)));
	}

	@Test
	void copyWithIDsFromSourceWithoutIDsInventsNone() {
		new XMIResourceImpl(URI.createURI("source.xmi")).getContents().add(root);
		XMLResource target = new XMIResourceImpl(URI.createURI("target.xmi"));

		XMLResourceIDs.copyWithIDs(root, target);

		assertTrue(target.getEObjectToIDMap().isEmpty());
	}

	// ===== applyIDs =====

	@Test
	void applyIDsToNonXMLResourceIsNoOp() {
		Resource target = new ResourceImpl(URI.createURI("plain"));
		target.getContents().add(root);

		XMLResourceIDs.applyIDs(Map.of(root, "_root"), target);

		assertSame(target, root.eResource());
	}
}
