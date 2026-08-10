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
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistries;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.eclipse.fennec.emf.osgi.metadata.MetadataServices;
import org.eclipse.fennec.emf.osgi.metadata.MetadataWhiteboard;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Placement across several live versions of one nsURI (issue #81).
 * <p>
 * The bridge copies entry content with {@code EcoreUtil.copy}, which keeps
 * non-containment references pointing at the <em>originals</em>. For version-independent
 * content - a mapping, a profile - that is harmless and spanning every live version is
 * the feature. For a <b>derived</b> artifact - compiled OCL holding the
 * {@code EStructuralFeature} instances it resolved against - a copy on a foreign version's
 * tree navigates the wrong package: with dynamic EMF it fails at {@code eGet}, with
 * generated code it may resolve by name and quietly answer from the wrong model. That is
 * the failure fingerprint identity exists to prevent.
 * <p>
 * The contract these tests pin down: <b>an entry that names a version through
 * {@code emf.fingerprint} belongs on that version and no other; an entry that names none
 * is version-independent and goes onto every live version of its anchor's nsURI.</b> The
 * placement is therefore also the provenance - the fingerprint of the
 * {@link PackageMetadata} containing an aspect is the fingerprint of the package its
 * content was built from, which is exactly what the unguarded copy made untrue.
 */
public class FingerprintScopedPlacementTest {

	private static final String NS_URI = "http://fennec.eclipse.org/test/sensors";
	private static final String TYPE_ID = "compiled.ocl";
	private static final String ANCHOR_NAME = "TemperatureSensor";

	/** The content model: what a derived artifact looks like, reduced to a label. */
	private EPackage contentPackage;
	private EClass expressionClass;
	private EAttribute bodyAttribute;

	private MetadataWhiteboard whiteboard;
	private AspectAnchorResolver anchorResolver;

	@BeforeEach
	public void setUp() {
		contentPackage = EcoreFactory.eINSTANCE.createEPackage();
		contentPackage.setName("expressions");
		contentPackage.setNsPrefix("expressions");
		contentPackage.setNsURI("http://fennec.eclipse.org/test/expressions");
		expressionClass = EcoreFactory.eINSTANCE.createEClass();
		expressionClass.setName("CompiledExpression");
		bodyAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		bodyAttribute.setName("body");
		bodyAttribute.setEType(EcorePackage.Literals.ESTRING);
		expressionClass.getEStructuralFeatures().add(bodyAttribute);
		contentPackage.getEClassifiers().add(expressionClass);

		whiteboard = MetadataServices.createWhiteboard();
	}

	/**
	 * A domain model version. Every version carries the anchor class under the same name -
	 * that is what lets content span versions - and differs structurally, so the
	 * fingerprints differ.
	 *
	 * @param extraFeatures feature names distinguishing this version from the others
	 * @return the package, not yet registered
	 */
	private EPackage domainVersion(String... extraFeatures) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("sensors");
		ePackage.setNsPrefix("sensors");
		ePackage.setNsURI(NS_URI);
		EClass anchor = EcoreFactory.eINSTANCE.createEClass();
		anchor.setName(ANCHOR_NAME);
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

	private EObject expression(String body) {
		EObject expression = EcoreUtil.create(expressionClass);
		expression.eSet(bodyAttribute, body);
		return expression;
	}

	private String bodyOf(AspectEntry aspect) {
		return (String) aspect.getContent().eGet(bodyAttribute);
	}

	/**
	 * Registry plus bridge, wired the way the OSGi components do. The anchor is resolved
	 * against the version handed in here; placement itself is by class name, so content
	 * resolved against one version's EClass can still land on another version's tree -
	 * which is the whole point of the guard under test.
	 */
	private EObjectRegistryWriter registryWithBridge(EPackage resolveAnchorAgainst) {
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("expressions");
		anchorResolver = entry -> List.of(anchorOf(resolveAnchorAgainst));
		RegistryMetadataBridge bridge = new RegistryMetadataBridge(whiteboard, TYPE_ID, anchorResolver);
		whiteboard.addMetadataHandler(bridge);
		writer.getRegistry().addListener(bridge);
		return writer;
	}

	private Map<String, Object> pinnedTo(String fingerprint) {
		return Map.of(FINGERPRINT_ATTRIBUTE, fingerprint);
	}

	private ClassMetadata anchorMetadata(PackageMetadata packageMetadata) {
		return packageMetadata.getClasses().stream().filter(cm -> ANCHOR_NAME.equals(cm.getName())).findFirst()
				.orElseThrow();
	}

	/**
	 * The reported bug, minimal: content pinned to v1 must not be copied onto v2's tree
	 * when the version bump arrives. Both versions stay live, only v1 answers.
	 */
	@Test
	public void testPinnedContentStaysOnItsVersionWhenAnotherRegisters() {
		EPackage v1 = domainVersion();
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr-1", expression("v1-body"), pinnedTo(fingerprintV1));

		EPackage v2 = domainVersion("accuracy");
		whiteboard.registerPackage(v2);

		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).as("its own version answers").isPresent();
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).as("the foreign version must not").isEmpty();
	}

	/**
	 * The same, with the entry arriving first: the guard sits in the handler replay path
	 * too, and must narrow it without breaking it. v2 is live from the start, so a missing
	 * guard would place on v2 and the later v1 registration would look correct anyway -
	 * this is the test that fails for the right reason.
	 */
	@Test
	public void testPinnedContentReachesItsVersionRegisteringLater() {
		EPackage v2 = domainVersion("accuracy");
		whiteboard.registerPackage(v2);
		EPackage v1 = domainVersion();
		// the fingerprint is stated before the model is there - the derived artifact was
		// built elsewhere (a compiler, a model atlas) and names the version it targets
		String fingerprintV1 = FingerprintHelper.fingerprint(v1);

		EObjectRegistryWriter writer = registryWithBridge(v2);
		writer.put("compiler", "expr-1", expression("v1-body"), pinnedTo(fingerprintV1));
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).as("not this version's content").isEmpty();

		whiteboard.registerPackage(v1);

		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID).orElseThrow())).isEqualTo("v1-body");
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).as("still not v2's").isEmpty();
	}

	/**
	 * Three live versions of one nsURI, three fingerprints, one derived entry per version:
	 * every version answers with its own content and nothing else. Without the guard all
	 * three entries land on all three trees, one aspect per anchor and type id wins, and
	 * two of the three versions answer with a foreign artifact.
	 */
	@Test
	public void testThreeVersionsEachAnswerWithTheirOwnDerivedContent() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		EPackage v3 = domainVersion("accuracy", "precision");
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		String fingerprintV2 = whiteboard.registerPackage(v2).orElseThrow().getModelFingerprint();
		String fingerprintV3 = whiteboard.registerPackage(v3).orElseThrow().getModelFingerprint();
		assertThat(List.of(fingerprintV1, fingerprintV2, fingerprintV3)).doesNotHaveDuplicates();
		assertThat(whiteboard.getPackageMetadataVersions(NS_URI)).hasSize(3);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr-v1", expression("v1-body"), pinnedTo(fingerprintV1));
		writer.put("compiler", "expr-v2", expression("v2-body"), pinnedTo(fingerprintV2));
		writer.put("compiler", "expr-v3", expression("v3-body"), pinnedTo(fingerprintV3));

		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID).orElseThrow())).isEqualTo("v1-body");
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID).orElseThrow())).isEqualTo("v2-body");
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v3), TYPE_ID).orElseThrow())).isEqualTo("v3-body");
		// one aspect per tree, not three placements fighting over one slot
		assertThat(anchorMetadata(whiteboard.getPackageMetadata(v2).orElseThrow()).getAspects()).hasSize(1);
	}

	/**
	 * The same three versions in the other order: the entries exist before any model does,
	 * and each registration picks up exactly the one entry that names it. This is the
	 * start-ordering guarantee of the registry (no DS ordering, cf. use case 2) applied per
	 * version.
	 */
	@Test
	public void testThreeVersionsRegisteringAfterTheContentEachPickUpTheirOwn() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		EPackage v3 = domainVersion("accuracy", "precision");

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr-v1", expression("v1-body"), pinnedTo(FingerprintHelper.fingerprint(v1)));
		writer.put("compiler", "expr-v2", expression("v2-body"), pinnedTo(FingerprintHelper.fingerprint(v2)));
		writer.put("compiler", "expr-v3", expression("v3-body"), pinnedTo(FingerprintHelper.fingerprint(v3)));

		whiteboard.registerPackage(v3);
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v3), TYPE_ID).orElseThrow())).isEqualTo("v3-body");

		whiteboard.registerPackage(v1);
		whiteboard.registerPackage(v2);

		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID).orElseThrow())).isEqualTo("v1-body");
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID).orElseThrow())).isEqualTo("v2-body");
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v3), TYPE_ID).orElseThrow())).isEqualTo("v3-body");
	}

	/**
	 * The compatibility half of the contract: an entry that names no version keeps spanning
	 * every live version, across all three trees. This is what makes a version bump cost
	 * nothing for mappings and profiles, and it must stay untouched by the guard.
	 */
	@Test
	public void testVersionIndependentContentStillSpansAllThreeVersions() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		EPackage v3 = domainVersion("accuracy", "precision");
		whiteboard.registerPackage(v1);
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		// properties without a fingerprint: provenance is stated, the version is not
		writer.put("files", "mapping-1", expression("any-version"), Map.of("emf.nsURI", NS_URI));

		whiteboard.registerPackage(v3);

		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID).orElseThrow())).isEqualTo("any-version");
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID).orElseThrow())).isEqualTo("any-version");
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v3), TYPE_ID).orElseThrow())).isEqualTo("any-version");
	}

	/**
	 * An entry pinned to a version nobody provides lands nowhere - not on a plausible
	 * neighbour. Narrowing turns a silent misplacement into a silent absence, which is the
	 * safe direction but still needs to be a deliberate, logged decision rather than an
	 * accident.
	 */
	@Test
	public void testPinnedContentForAnUnknownVersionLandsNowhere() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		whiteboard.registerPackage(v1);
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr-stale", expression("built-against-a-gone-version"),
				pinnedTo("fp1:0000000000000000000000000000000000000000000000000000000000000000"));

		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).isEmpty();
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).isEmpty();
		// the registry keeps it: the model may still arrive, and the replay will place it
		assertThat(writer.getRegistry().get("expr-stale")).isPresent();
	}

	/**
	 * Cross-version updates and removals stay scoped. A pinned entry is replaced and
	 * removed without ever touching the other versions' aspects - the bookkeeping is per
	 * placement, not per nsURI.
	 */
	@Test
	public void testUpdateAndRemovalOfOnePinnedEntryLeaveTheOtherVersionsAlone() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		String fingerprintV2 = whiteboard.registerPackage(v2).orElseThrow().getModelFingerprint();

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr-v1", expression("v1-body"), pinnedTo(fingerprintV1));
		writer.put("compiler", "expr-v2", expression("v2-body"), pinnedTo(fingerprintV2));

		writer.put("compiler", "expr-v1", expression("v1-recompiled"), pinnedTo(fingerprintV1));
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID).orElseThrow())).isEqualTo("v1-recompiled");
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID).orElseThrow())).isEqualTo("v2-body");

		writer.remove("compiler", "expr-v1");
		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).isEmpty();
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID).orElseThrow())).as("untouched")
				.isEqualTo("v2-body");
	}

	/**
	 * A withdrawn version's placements are forgotten, as
	 * {@code onPackageUnregistered} documents: the tree dies with its aspects. Observable
	 * because a later removal of the entry must then no longer reach into the dead tree -
	 * if the bridge still tracked those aspects it would keep whole content copies alive
	 * for every version that ever came and went.
	 */
	@Test
	public void testWithdrawnVersionPlacementsAreForgotten() {
		EPackage v1 = domainVersion();
		PackageMetadata metadataV1 = whiteboard.registerPackage(v1).orElseThrow();
		String fingerprintV1 = metadataV1.getModelFingerprint();
		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr-v1", expression("v1-body"), pinnedTo(fingerprintV1));
		ClassMetadata deadAnchor = anchorMetadata(metadataV1);
		assertThat(deadAnchor.getAspects()).hasSize(1);

		whiteboard.unregisterPackage(v1);
		writer.remove("compiler", "expr-v1");

		assertThat(deadAnchor.getAspects()).as("the withdrawn tree is nobody's business anymore").hasSize(1);
	}
}
