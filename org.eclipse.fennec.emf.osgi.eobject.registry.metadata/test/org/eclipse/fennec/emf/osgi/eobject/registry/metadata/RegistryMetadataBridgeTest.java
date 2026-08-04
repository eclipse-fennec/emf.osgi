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
package org.eclipse.fennec.emf.osgi.eobject.registry.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistries;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.emf.osgi.metadata.MetadataServices;
import org.eclipse.fennec.emf.osgi.metadata.MetadataWhiteboard;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Bridge semantics in isolation: snapshot copies, one-aspect-per-anchor-and-type,
 * detach on removal and teardown.
 */
public class RegistryMetadataBridgeTest {

	private static final String TYPE_ID = "test.aspect";

	private EPackage domainPackage;
	private EClass contentClass;
	private EAttribute nameAttribute;

	private MetadataWhiteboard whiteboard;
	private EObjectRegistryWriter writer;
	private RegistryMetadataBridge bridge;

	@BeforeEach
	public void setUp() {
		domainPackage = EcoreFactory.eINSTANCE.createEPackage();
		domainPackage.setName("content");
		domainPackage.setNsPrefix("content");
		domainPackage.setNsURI("http://fennec.eclipse.org/test/content/1.0");
		contentClass = EcoreFactory.eINSTANCE.createEClass();
		contentClass.setName("Content");
		nameAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		nameAttribute.setName("name");
		nameAttribute.setEType(EcorePackage.Literals.ESTRING);
		contentClass.getEStructuralFeatures().add(nameAttribute);
		domainPackage.getEClassifiers().add(contentClass);

		whiteboard = MetadataServices.createWhiteboard();
		writer = EObjectRegistries.createRegistry("bridge-test");
		bridge = new RegistryMetadataBridge(whiteboard, TYPE_ID, AspectAnchorResolver.contentClass());
		whiteboard.addMetadataHandler(bridge);
		writer.getRegistry().addListener(bridge);
		whiteboard.registerPackage(domainPackage);
	}

	private EObject content(String name) {
		EObject content = EcoreUtil.create(contentClass);
		content.eSet(nameAttribute, name);
		return content;
	}

	@Test
	public void testAspectContentIsASnapshotCopy() {
		EObject live = content("original");
		writer.put("src", "a", live, null);

		AspectEntry aspect = whiteboard.getClassAspect(contentClass, TYPE_ID).orElseThrow();
		assertThat(aspect.getContent()).isNotSameAs(live);
		assertThat(aspect.getContent().eGet(nameAttribute)).isEqualTo("original");

		// mutating the live object never changes the published snapshot
		live.eSet(nameAttribute, "mutated");
		assertThat(aspect.getContent().eGet(nameAttribute)).isEqualTo("original");
		// the live object stays where it is - it was not stolen into the containment slot
		assertThat(live.eContainer()).isNull();
	}

	@Test
	public void testUpdateReplacesTheSnapshot() {
		writer.put("src", "a", content("v1"), null);
		writer.put("src", "a", content("v2"), null);

		AspectEntry aspect = whiteboard.getClassAspect(contentClass, TYPE_ID).orElseThrow();
		assertThat(aspect.getContent().eGet(nameAttribute)).isEqualTo("v2");
	}

	@Test
	public void testRemovalDetachesTheAspect() {
		writer.put("src", "a", content("gone"), null);
		writer.remove("src", "a");

		assertThat(whiteboard.getClassAspect(contentClass, TYPE_ID)).isEmpty();
	}

	@Test
	public void testOneAspectPerAnchorAndTypeLastWriteWins() {
		writer.put("src", "first", content("first"), null);
		writer.put("src", "second", content("second"), null);

		AspectEntry aspect = whiteboard.getClassAspect(contentClass, TYPE_ID).orElseThrow();
		assertThat(aspect.getContent().eGet(nameAttribute)).isEqualTo("second");

		// removing the loser must not tear down the winner's aspect
		writer.remove("src", "first");
		assertThat(whiteboard.getClassAspect(contentClass, TYPE_ID)).isPresent();

		writer.remove("src", "second");
		assertThat(whiteboard.getClassAspect(contentClass, TYPE_ID)).isEmpty();
	}

	@Test
	public void testCloseDetachesEverythingButKeepsTheRegistry() {
		writer.put("src", "a", content("kept"), null);

		bridge.close();

		assertThat(whiteboard.getClassAspect(contentClass, TYPE_ID)).isEmpty();
		assertThat(writer.getRegistry().get("a")).isPresent();
	}

	@Test
	public void testMultiAnchorContent() {
		EClass secondAnchor = EcoreFactory.eINSTANCE.createEClass();
		secondAnchor.setName("SecondAnchor");
		domainPackage.getEClassifiers().add(secondAnchor);
		whiteboard.unregisterPackage(domainPackage);
		whiteboard.registerPackage(domainPackage);
		AspectAnchorResolver bothAnchors = entry -> List.of(contentClass, secondAnchor);
		RegistryMetadataBridge multiBridge = new RegistryMetadataBridge(whiteboard, "multi.aspect", bothAnchors);
		writer.getRegistry().addListener(multiBridge);
		whiteboard.addMetadataHandler(multiBridge);

		writer.put("src", "a", content("both"), null);
		assertThat(whiteboard.getClassAspect(contentClass, "multi.aspect")).isPresent();
		assertThat(whiteboard.getClassAspect(secondAnchor, "multi.aspect")).isPresent();

		writer.remove("src", "a");
		assertThat(whiteboard.getClassAspect(contentClass, "multi.aspect")).isEmpty();
		assertThat(whiteboard.getClassAspect(secondAnchor, "multi.aspect")).isEmpty();
	}

	@Test
	public void testContentForUnregisteredModelIsSilentlyPending() {
		EPackage otherPackage = EcoreFactory.eINSTANCE.createEPackage();
		otherPackage.setName("other");
		otherPackage.setNsPrefix("other");
		otherPackage.setNsURI("http://fennec.eclipse.org/test/other/1.0");
		EClass otherClass = EcoreFactory.eINSTANCE.createEClass();
		otherClass.setName("Other");
		otherPackage.getEClassifiers().add(otherClass);

		writer.put("src", "pending", EcoreUtil.create(otherClass), null);

		assertThat(whiteboard.getClassAspect(otherClass, TYPE_ID)).isEmpty();
	}
}
