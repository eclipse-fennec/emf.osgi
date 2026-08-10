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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
import org.junit.jupiter.api.AfterEach;
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

	/** Whether narrowing stayed silent or said so is part of the contract - so capture it. */
	private final List<String> warnings = new CopyOnWriteArrayList<>();
	private Logger bridgeLogger;
	private Handler logHandler;

	@BeforeEach
	public void captureBridgeWarnings() {
		bridgeLogger = Logger.getLogger(RegistryMetadataBridge.class.getName());
		logHandler = new Handler() {

			@Override
			public void publish(LogRecord record) {
				warnings.add(record.getMessage());
			}

			@Override
			public void flush() {
				// nothing buffered
			}

			@Override
			public void close() {
				// nothing to release
			}
		};
		bridgeLogger.addHandler(logHandler);
	}

	@AfterEach
	public void releaseLogHandler() {
		bridgeLogger.removeHandler(logHandler);
	}

	/** The warnings about content that reached no tree, ignoring last-write-wins notices. */
	private List<String> unplacedWarnings() {
		List<String> matching = new ArrayList<>();
		for (String warning : warnings) {
			if (warning != null && warning.contains("no aspect placed")) {
				matching.add(warning);
			}
		}
		return matching;
	}

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

	// ---------------------------------------------------------------- cardinalities

	/** The degenerate case the guard must not break: one live version, pinned to it. */
	@Test
	public void testPinnedContentWithASingleLiveVersion() {
		EPackage only = domainVersion();
		String fingerprint = whiteboard.registerPackage(only).orElseThrow().getModelFingerprint();

		EObjectRegistryWriter writer = registryWithBridge(only);
		writer.put("compiler", "expr", expression("only-body"), pinnedTo(fingerprint));

		assertThat(whiteboard.getPackageMetadataVersions(NS_URI)).hasSize(1);
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(only), TYPE_ID).orElseThrow())).isEqualTo("only-body");
		assertThat(unplacedWarnings()).isEmpty();
	}

	/**
	 * No live version at all - a freshly started gateway whose content sources are up
	 * before any model bundle. Pending is the normal state here, so it must stay
	 * <em>silent</em>: a warning per entry on every cold start would be noise, and the
	 * replay places the content the moment the model arrives.
	 */
	@Test
	public void testPinnedContentWithNoLiveVersionIsSilentlyPending() {
		EPackage v1 = domainVersion();
		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr", expression("v1-body"), pinnedTo(FingerprintHelper.fingerprint(v1)));

		assertThat(whiteboard.getPackageMetadataVersions(NS_URI)).isEmpty();
		assertThat(unplacedWarnings()).as("nothing is wrong yet - no model is deployed").isEmpty();

		whiteboard.registerPackage(v1);
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID).orElseThrow())).isEqualTo("v1-body");
	}

	/**
	 * Five diverging versions of one nsURI: nothing in the placement path is tuned to
	 * "two" or "three". Every version answers with its own artifact, and no tree carries
	 * more than the one aspect that belongs to it.
	 */
	@Test
	public void testFiveVersionsEachAnswerWithTheirOwnDerivedContent() {
		List<EPackage> versions = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			String[] extraFeatures = new String[i];
			for (int j = 0; j < i; j++) {
				extraFeatures[j] = "v" + i + "feature" + j;
			}
			versions.add(domainVersion(extraFeatures));
		}
		List<String> fingerprints = new ArrayList<>();
		for (EPackage version : versions) {
			fingerprints.add(whiteboard.registerPackage(version).orElseThrow().getModelFingerprint());
		}
		assertThat(fingerprints).doesNotHaveDuplicates();
		assertThat(whiteboard.getPackageMetadataVersions(NS_URI)).hasSize(5);

		EObjectRegistryWriter writer = registryWithBridge(versions.get(0));
		for (int i = 0; i < versions.size(); i++) {
			writer.put("compiler", "expr-" + i, expression("body-" + i), pinnedTo(fingerprints.get(i)));
		}

		for (int i = 0; i < versions.size(); i++) {
			EPackage version = versions.get(i);
			assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(version), TYPE_ID).orElseThrow()))
					.isEqualTo("body-" + i);
			assertThat(anchorMetadata(whiteboard.getPackageMetadata(version).orElseThrow()).getAspects()).hasSize(1);
		}
		assertThat(unplacedWarnings()).isEmpty();
	}

	// ------------------------------------------------------- malformed property values

	/**
	 * An <b>empty</b> fingerprint means "the provider cannot state it reliably" and must be
	 * treated as unknown, <em>never</em> as a mismatch - the contract is spelled out on
	 * {@code EPackage#fingerprint()} in the api bundle. Dropping such content would be the
	 * worst of both worlds: a silent loss caused by a property that was meant to be
	 * optional.
	 */
	@Test
	public void testBlankFingerprintIsTreatedAsUnknownNotAsMismatch() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		whiteboard.registerPackage(v1);
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr", expression("stated-nothing"), pinnedTo(""));

		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).as("empty is unknown, not a mismatch")
				.isPresent();
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).isPresent();

		// whitespace is no more of a statement than the empty string
		writer.put("compiler", "expr", expression("still-nothing"), pinnedTo("   "));
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID).orElseThrow())).isEqualTo("still-nothing");
	}

	/**
	 * Entry properties are frequently copied wholesale from OSGi service properties or
	 * Configurator JSON - where a single value legitimately arrives as a one-element array.
	 * Comparing {@code String[].toString()} against a fingerprint would never match and
	 * would drop the content.
	 */
	@Test
	public void testFingerprintGivenAsSingleElementArrayIsHonoured() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr", expression("v1-body"),
				Map.of(FINGERPRINT_ATTRIBUTE, new String[] { fingerprintV1 }));

		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID).orElseThrow())).isEqualTo("v1-body");
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).as("still pinned").isEmpty();
	}

	/**
	 * A multi-valued fingerprint states no single version. It is a producer bug, and the
	 * safe reading is the documented one - unknown rather than a mismatch - so the content
	 * behaves like version-independent content instead of vanishing.
	 */
	@Test
	public void testMultiValuedFingerprintIsTreatedAsUnknown() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		String fingerprintV2 = whiteboard.registerPackage(v2).orElseThrow().getModelFingerprint();

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr", expression("ambiguous"),
				Map.of(FINGERPRINT_ATTRIBUTE, List.of(fingerprintV1, fingerprintV2)));

		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).isPresent();
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).isPresent();
	}

	// ------------------------------------------------------------ state transitions

	/**
	 * A source learns the version later - the atlas first hands out a mapping, then the
	 * fingerprint it was derived against. The update must <b>shrink</b> the placement from
	 * every version to the named one.
	 */
	@Test
	public void testEntryBecomingPinnedShrinksToItsVersion() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		EPackage v3 = domainVersion("accuracy", "precision");
		String fingerprintV2 = whiteboard.registerPackage(v2).orElseThrow().getModelFingerprint();
		whiteboard.registerPackage(v1);
		whiteboard.registerPackage(v3);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("atlas", "expr", expression("unpinned"), null);
		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).isPresent();
		assertThat(whiteboard.getClassAspect(anchorOf(v3), TYPE_ID)).isPresent();

		writer.put("atlas", "expr", expression("now-pinned"), pinnedTo(fingerprintV2));

		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID).orElseThrow())).isEqualTo("now-pinned");
		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).as("dropped from the other versions").isEmpty();
		assertThat(whiteboard.getClassAspect(anchorOf(v3), TYPE_ID)).isEmpty();
	}

	/** And the reverse: dropping the pin re-opens the entry to every live version. */
	@Test
	public void testEntryLosingItsPinSpansAllVersionsAgain() {
		EPackage v1 = domainVersion();
		EPackage v2 = domainVersion("accuracy");
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("atlas", "expr", expression("pinned"), pinnedTo(fingerprintV1));
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).isEmpty();

		writer.put("atlas", "expr", expression("unpinned"), null);

		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID).orElseThrow())).isEqualTo("unpinned");
		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID).orElseThrow())).isEqualTo("unpinned");
	}

	// ----------------------------------------------------------------- anchor edges

	/**
	 * The named version exists but does not carry the anchor class - it was renamed or
	 * dropped in that version. Nothing can be placed, and unlike the cold-start case this
	 * <em>is</em> worth saying out loud: an entry names a deployed version and still reaches
	 * nothing.
	 */
	@Test
	public void testAnchorMissingInTheNamedVersionPlacesNothingAndWarns() {
		EPackage renamed = domainVersion("accuracy");
		anchorOf(renamed).setName("RenamedSensor");
		String fingerprint = whiteboard.registerPackage(renamed).orElseThrow().getModelFingerprint();
		EPackage v1 = domainVersion();

		EObjectRegistryWriter writer = registryWithBridge(v1);
		writer.put("compiler", "expr", expression("orphan"), pinnedTo(fingerprint));

		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).isEmpty();
		assertThat(unplacedWarnings()).as("a named, deployed version that reaches nothing is reported").hasSize(1);
		assertThat(unplacedWarnings().get(0)).contains("expr");
	}

	/** A pinned entry with several anchors places all of them - on its version only. */
	@Test
	public void testPinnedMultiAnchorContentPlacesEveryAnchorOnItsVersionOnly() {
		EPackage v1 = domainVersion();
		EClass secondAnchor = EcoreFactory.eINSTANCE.createEClass();
		secondAnchor.setName("HumiditySensor");
		v1.getEClassifiers().add(secondAnchor);
		EPackage v2 = EcoreUtil.copy(v1);
		EAttribute extra = EcoreFactory.eINSTANCE.createEAttribute();
		extra.setName("accuracy");
		extra.setEType(EcorePackage.Literals.ESTRING);
		((EClass) v2.getEClassifier(ANCHOR_NAME)).getEStructuralFeatures().add(extra);
		String fingerprintV1 = whiteboard.registerPackage(v1).orElseThrow().getModelFingerprint();
		whiteboard.registerPackage(v2);

		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("expressions");
		RegistryMetadataBridge bridge = new RegistryMetadataBridge(whiteboard, TYPE_ID,
				entry -> List.of(anchorOf(v1), secondAnchor));
		whiteboard.addMetadataHandler(bridge);
		writer.getRegistry().addListener(bridge);

		writer.put("compiler", "expr", expression("v1-body"), pinnedTo(fingerprintV1));

		assertThat(whiteboard.getClassAspect(anchorOf(v1), TYPE_ID)).isPresent();
		assertThat(whiteboard.getClassAspect(secondAnchor, TYPE_ID)).isPresent();
		assertThat(whiteboard.getClassAspect(anchorOf(v2), TYPE_ID)).as("neither anchor on the foreign version")
				.isEmpty();
		assertThat(whiteboard.getClassAspect((EClass) v2.getEClassifier("HumiditySensor"), TYPE_ID)).isEmpty();
	}

	// ---------------------------------------------------------------------- legacy

	/**
	 * A legacy model advertises no {@code emf.fingerprint} - generated code from before the
	 * scheme existed, or a dynamically loaded ecore. Metadata identity does not depend on
	 * that: the service <b>computes</b> the fingerprint for every version it registers
	 * ({@code MetadataServiceImpl} treats a supplied value as context, never as truth). A
	 * producer can therefore pin derived content against a legacy model just as well - it
	 * reads the computed value instead of a bundle property.
	 */
	@Test
	public void testLegacyModelWithoutAdvertisedFingerprintCanStillBePinned() {
		EPackage legacy = domainVersion();
		EPackage other = domainVersion("accuracy");
		PackageMetadata legacyMetadata = whiteboard.registerPackage(legacy).orElseThrow();
		whiteboard.registerPackage(other);

		assertThat(legacyMetadata.getModelFingerprint()).as("identity is computed, not advertised").isNotBlank()
				.startsWith(FingerprintHelper.currentScheme());
		assertThat(legacyMetadata.getModelFingerprint()).isEqualTo(FingerprintHelper.fingerprint(legacy));

		EObjectRegistryWriter writer = registryWithBridge(legacy);
		writer.put("compiler", "expr", expression("legacy-body"),
				pinnedTo(legacyMetadata.getModelFingerprint()));

		assertThat(bodyOf(whiteboard.getClassAspect(anchorOf(legacy), TYPE_ID).orElseThrow()))
				.isEqualTo("legacy-body");
		assertThat(whiteboard.getClassAspect(anchorOf(other), TYPE_ID)).isEmpty();
	}
}
