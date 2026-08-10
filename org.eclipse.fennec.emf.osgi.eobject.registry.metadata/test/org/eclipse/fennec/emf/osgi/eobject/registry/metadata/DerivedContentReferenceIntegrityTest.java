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
import static org.eclipse.fennec.emf.osgi.annotation.provide.EPackage.FINGERPRINT_ATTRIBUTE;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
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
 * <b>Why</b> derived content must not be copied across model versions (issue #81), pinned
 * down at the level the bug actually lives on: the references inside the copy.
 * <p>
 * The placement tests distinguish versions by a string attribute; that proves the routing
 * but not the reason for it. These tests assert the object identities: the copy in an
 * {@link AspectEntry} keeps its <b>non-containment</b> references pointing at the very
 * {@code EStructuralFeature} and {@code EClassifier} instances the artifact was derived
 * against - which is correct exactly as long as the aspect sits on that version's tree,
 * and wrong on any other. They are the regression guard for the copy mechanism itself: a
 * future change to how content is snapshotted has to keep these identities intact, or the
 * cross-version mix-up returns without any routing test noticing.
 * <p>
 * Also documented here, deliberately, is the <b>limit</b> of the guard: content that names
 * no version still spans every live version, references and all. That is why the guide
 * states carrying {@code emf.fingerprint} as an obligation for content holding model
 * references rather than as an option.
 */
public class DerivedContentReferenceIntegrityTest {

	private static final String NS_URI = "http://fennec.eclipse.org/test/sensors";
	private static final String TYPE_ID = "compiled.ocl";
	private static final String ANCHOR_NAME = "TemperatureSensor";
	private static final String MEASURED_FEATURE = "measured";

	/**
	 * The content model, shaped like a compiled expression: it holds the feature it
	 * resolved against, a containment list of steps, and each step holds a classifier plus
	 * a reference to a sibling step - the three reference cases that must behave
	 * differently under a copy.
	 */
	private EPackage contentPackage;
	private EClass expressionClass;
	private EAttribute bodyAttribute;
	private EReference resolvedFeature;
	private EReference steps;
	private EClass stepClass;
	private EReference stepClassifier;
	private EReference stepSibling;

	private MetadataWhiteboard whiteboard;

	@BeforeEach
	public void setUp() {
		contentPackage = EcoreFactory.eINSTANCE.createEPackage();
		contentPackage.setName("expressions");
		contentPackage.setNsPrefix("expressions");
		contentPackage.setNsURI("http://fennec.eclipse.org/test/expressions");

		stepClass = EcoreFactory.eINSTANCE.createEClass();
		stepClass.setName("Step");
		stepClassifier = reference(stepClass, "classifier", EcorePackage.Literals.ECLASSIFIER, false);
		stepSibling = reference(stepClass, "sibling", stepClass, false);

		expressionClass = EcoreFactory.eINSTANCE.createEClass();
		expressionClass.setName("CompiledExpression");
		bodyAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		bodyAttribute.setName("body");
		bodyAttribute.setEType(EcorePackage.Literals.ESTRING);
		expressionClass.getEStructuralFeatures().add(bodyAttribute);
		resolvedFeature = reference(expressionClass, "resolvedFeature", EcorePackage.Literals.ESTRUCTURAL_FEATURE,
				false);
		steps = reference(expressionClass, "steps", stepClass, true);
		steps.setUpperBound(-1);

		contentPackage.getEClassifiers().add(stepClass);
		contentPackage.getEClassifiers().add(expressionClass);

		whiteboard = MetadataServices.createWhiteboard();
	}

	private EReference reference(EClass owner, String name, EClassifier type, boolean containment) {
		EReference eReference = EcoreFactory.eINSTANCE.createEReference();
		eReference.setName(name);
		eReference.setEType(type);
		eReference.setContainment(containment);
		owner.getEStructuralFeatures().add(eReference);
		return eReference;
	}

	/** A domain model version; every version carries the anchor and a measured feature. */
	private EPackage domainVersion(String... extraFeatures) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("sensors");
		ePackage.setNsPrefix("sensors");
		ePackage.setNsURI(NS_URI);
		EClass anchor = EcoreFactory.eINSTANCE.createEClass();
		anchor.setName(ANCHOR_NAME);
		EAttribute measured = EcoreFactory.eINSTANCE.createEAttribute();
		measured.setName(MEASURED_FEATURE);
		measured.setEType(EcorePackage.Literals.EDOUBLE);
		anchor.getEStructuralFeatures().add(measured);
		for (String featureName : extraFeatures) {
			EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();
			attribute.setName(featureName);
			attribute.setEType(EcorePackage.Literals.ESTRING);
			anchor.getEStructuralFeatures().add(attribute);
		}
		ePackage.getEClassifiers().add(anchor);
		return ePackage;
	}

	private EClass anchorOf(EPackage domainVersion) {
		return (EClass) domainVersion.getEClassifier(ANCHOR_NAME);
	}

	private EStructuralFeature measuredOf(EPackage domainVersion) {
		return anchorOf(domainVersion).getEStructuralFeature(MEASURED_FEATURE);
	}

	/**
	 * An artifact "compiled" against one model version: it resolves the measured feature
	 * and two steps, the second referring back to the first.
	 */
	private EObject compiledAgainst(EPackage domainVersion, String body) {
		EObject expression = EcoreUtil.create(expressionClass);
		expression.eSet(bodyAttribute, body);
		expression.eSet(resolvedFeature, measuredOf(domainVersion));

		EObject first = EcoreUtil.create(stepClass);
		first.eSet(stepClassifier, anchorOf(domainVersion));
		EObject second = EcoreUtil.create(stepClass);
		second.eSet(stepClassifier, anchorOf(domainVersion));
		second.eSet(stepSibling, first);
		@SuppressWarnings("unchecked")
		List<EObject> stepList = (List<EObject>) expression.eGet(steps);
		stepList.add(first);
		stepList.add(second);
		return expression;
	}

	private EObjectRegistryWriter registryWithBridge(EPackage resolveAnchorAgainst) {
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("expressions");
		RegistryMetadataBridge bridge = new RegistryMetadataBridge(whiteboard, TYPE_ID,
				entry -> List.of(anchorOf(resolveAnchorAgainst)));
		whiteboard.addMetadataHandler(bridge);
		writer.getRegistry().addListener(bridge);
		return writer;
	}

	private EObject aspectContent(EPackage domainVersion) {
		return whiteboard.getClassAspect(anchorOf(domainVersion), TYPE_ID).orElseThrow().getContent();
	}

	/**
	 * The core identity claim: the snapshot on v1's tree navigates <b>v1's</b> feature
	 * instance, not an equally named one from another version. Two live versions with the
	 * same feature names is exactly the constellation in which name equality would hide a
	 * wrong answer.
	 */
	@Test
	public void testSnapshotOnItsVersionReferencesThatVersionsFeatureInstance() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr", compiledAgainst(v1, "v1-body"), Map.of(FINGERPRINT_ATTRIBUTE, fingerprintV1));

		EObject snapshot = aspectContent(v1);
		assertThat(snapshot.eGet(resolvedFeature)).as("the feature instance of its own version")
				.isSameAs(measuredOf(v1));
		assertThat(snapshot.eGet(resolvedFeature)).isNotSameAs(measuredOf(v2));
		// name equality across versions is real - which is why identity is what matters
		assertThat(measuredOf(v1).getName()).isEqualTo(measuredOf(v2).getName());
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).as("no snapshot on the foreign tree").isEmpty();
	}

	/**
	 * References out of a <b>contained child</b> of the content must survive the copy the
	 * same way - the copy is deep, and a step that resolved v1's anchor still points at
	 * v1's anchor.
	 */
	@Test
	public void testNestedContainmentKeepsItsModelReferences() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr", compiledAgainst(v1, "v1-body"), Map.of(FINGERPRINT_ATTRIBUTE, fingerprintV1));

		EObject snapshot = aspectContent(v1);
		@SuppressWarnings("unchecked")
		List<EObject> copiedSteps = (List<EObject>) snapshot.eGet(steps);
		assertThat(copiedSteps).hasSize(2);
		assertThat(copiedSteps.get(0).eGet(stepClassifier)).isSameAs(anchorOf(v1));
		assertThat(copiedSteps.get(1).eGet(stepClassifier)).isSameAs(anchorOf(v1));
		assertThat(copiedSteps.get(0).eGet(stepClassifier)).isNotSameAs(anchorOf(v2));
	}

	/**
	 * The complementary half of the copy contract: a reference whose target lies
	 * <b>inside</b> the copied tree is redirected to the copy, so the snapshot is
	 * internally consistent and never reaches back into the live registry object. Together
	 * with the two tests above this is the full rule - intra-tree references move, model
	 * references stay.
	 */
	@Test
	public void testIntraContentReferencesAreRedirectedToTheCopy() {
		EPackage v1 = domainVersion();
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();

		EObjectRegistryWriter writer = registryWithBridge(v1);
		EObject live = compiledAgainst(v1, "v1-body");
		writer.put("compiler", "expr", live, Map.of(FINGERPRINT_ATTRIBUTE, fingerprintV1));

		EObject snapshot = aspectContent(v1);
		@SuppressWarnings("unchecked")
		List<EObject> copiedSteps = (List<EObject>) snapshot.eGet(steps);
		@SuppressWarnings("unchecked")
		List<EObject> liveSteps = (List<EObject>) live.eGet(steps);

		assertThat(snapshot).isNotSameAs(live);
		assertThat(copiedSteps.get(1).eGet(stepSibling)).as("redirected to the copied sibling")
				.isSameAs(copiedSteps.get(0));
		assertThat(copiedSteps.get(1).eGet(stepSibling)).isNotSameAs(liveSteps.get(0));
		// and the live object was not stolen into the containment slot
		assertThat(live.eContainer()).isNull();
	}

	/**
	 * The documented <b>limit</b>: content that names no version keeps spanning every live
	 * version - and a copy sitting on v2's tree then still references v1's feature. Nothing
	 * detects that, which is precisely why
	 * {@code docs/eobject-registry-guide.md} makes {@code emf.fingerprint} mandatory for
	 * content holding model references. This test exists so the limitation is visible and
	 * cannot be mistaken for something the guard already handles.
	 */
	@Test
	public void testUnpinnedContentSpansVersionsAndKeepsItsOriginReferences() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		whiteboard.registerPackage(v1);
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("files", "expr", compiledAgainst(v1, "unpinned"), null);

		assertThat(aspectContent(v1).eGet(resolvedFeature)).isSameAs(measuredOf(v1));
		assertThat(aspectContent(v2).eGet(resolvedFeature))
				.as("on the foreign tree, still pointing at the origin version - the reason for the MUST rule")
				.isSameAs(measuredOf(v1));
	}

	/**
	 * Two versions, one derived artifact each: both trees answer, and each answer navigates
	 * its own version's features. This is the constellation the whole issue is about, at
	 * reference level.
	 */
	@Test
	public void testTwoDerivedArtifactsEachNavigateTheirOwnVersion() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		String fingerprintV2 = whiteboard.registerPackage(v2).orElseThrow().getModelFingerprint();

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr-v1", compiledAgainst(v1, "v1-body"),
				Map.of(FINGERPRINT_ATTRIBUTE, fingerprintV1));
		writer.put("compiler", "expr-v2", compiledAgainst(v2, "v2-body"),
				Map.of(FINGERPRINT_ATTRIBUTE, fingerprintV2));

		assertThat(aspectContent(v1).eGet(bodyAttribute)).isEqualTo("v1-body");
		assertThat(aspectContent(v1).eGet(resolvedFeature)).isSameAs(measuredOf(v1));
		assertThat(aspectContent(v2).eGet(bodyAttribute)).isEqualTo("v2-body");
		assertThat(aspectContent(v2).eGet(resolvedFeature)).isSameAs(measuredOf(v2));
	}
}
