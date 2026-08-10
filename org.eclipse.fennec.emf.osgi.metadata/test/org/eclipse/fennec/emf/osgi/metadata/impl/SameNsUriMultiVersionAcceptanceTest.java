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
package org.eclipse.fennec.emf.osgi.metadata.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The acceptance suite for same-nsURI multi-version support (WP6), ported from the
 * production finding in {@code eclipse-fennec/model.atlas#156}: two live {@link EPackage}
 * services under one nsURI with diverging content - a draft and an approved stage of the
 * same model - must coexist, and unregistering one must never take away what the other
 * still needs.
 * <p>
 * These are the invariants the whole migration exists for. They assert observable service
 * behaviour only, never a mechanism, so they stay valid across changes in how identity is
 * keyed - which is what made them survive the move from the donor repository unchanged in
 * substance.
 * <p>
 * Ported for issue #62. Only the shape changed: {@code Optional} instead of {@code null},
 * and the fingerprint service is injected rather than defaulted.
 */
class SameNsUriMultiVersionAcceptanceTest {

	private static final String NS_URI = "http://example.org/person/1.0";

	private MetadataServiceImpl service;

	@BeforeEach
	void setup() {
		service = new MetadataServiceImpl();
		service.setFingerprintService(new DefaultFingerprintService());
	}

	// ---- the atlas#156 repro: unregister one stage, the survivor must be served ----

	@Test
	void testUnregisterOneVersionKeepsServingTheSurvivor() {
		EPackage draft = draftPackage();
		EPackage approved = approvedPackage();

		service.registerPackage(draft);
		String approvedFingerprint = service.registerPackage(approved).orElseThrow().getModelFingerprint();

		service.unregisterPackage(approved);

		assertThat(service.getPackageMetadata(NS_URI))
				.as("draft must still be served after approved's unregister")
				.get()
				.extracting(PackageMetadata::getEPackage)
				.isSameAs(draft);
		assertThat(service.getPackageMetadata(draft)).isPresent();
		assertThat(service.getPackageMetadataByFingerprint(approvedFingerprint))
				.as("the unregistered version must be gone - not the survivor")
				.isEmpty();
	}

	// ---- the atlas#156 signature at index level: class-URI lookup must survive -----

	@Test
	void testUnregisterOneVersionKeepsClassUriLookupForSurvivor() {
		EPackage draft = draftPackage();
		EPackage approved = approvedPackage();
		service.registerPackage(draft);
		service.registerPackage(approved);

		// Both versions carry a "Person" EClass under the same nsURI, so both resolve to the
		// same structural typeURI. The destructive-remove bug: unregistering either version
		// deleted the shared URI entry and broke the lookup for the survivor.
		String personURI = EcoreUtil.getURI((EClass) approved.getEClassifier("Person")).toString();
		assertThat(service.getClassMetadataByURI(personURI))
				.as("class-URI lookup must resolve while both versions are live")
				.isPresent();

		service.unregisterPackage(approved);

		assertThat(service.getClassMetadataByURI(personURI))
				.as("class-URI lookup must survive one same-nsURI version's unregister")
				.get()
				.extracting(classMetadata -> classMetadata.getEClass())
				.as("the surviving version's class must be served, not the removed one's")
				.isSameAs(draft.getEClassifier("Person"));
	}

	@Test
	void testUnregisteringTheOtherVersionAlsoKeepsTheSurvivor() {
		EPackage draft = draftPackage();
		EPackage approved = approvedPackage();
		service.registerPackage(draft);
		service.registerPackage(approved);

		// Symmetric case: remove the first-registered version, the newer must remain.
		String personURI = EcoreUtil.getURI((EClass) draft.getEClassifier("Person")).toString();

		service.unregisterPackage(draft);

		assertThat(service.getClassMetadataByURI(personURI))
				.as("removing the older version must not drop the URI entry")
				.get()
				.extracting(classMetadata -> classMetadata.getEClass())
				.isSameAs(approved.getEClassifier("Person"));
	}

	// ---- coexistence: two diverging versions, each with its own metadata ----------

	@Test
	void testDivergingVersionsOfSameNsUriCoexist() {
		EPackage draft = draftPackage();
		EPackage approved = approvedPackage();

		PackageMetadata draftMetadata = service.registerPackage(draft).orElseThrow();
		PackageMetadata approvedMetadata = service.registerPackage(approved).orElseThrow();

		assertThat(approvedMetadata)
				.as("diverging content must never silently reuse the other version's metadata")
				.isNotSameAs(draftMetadata);
		assertThat(approvedMetadata.getModelFingerprint()).isNotEqualTo(draftMetadata.getModelFingerprint());
		assertThat(draftMetadata.getEPackage()).isSameAs(draft);
		assertThat(approvedMetadata.getEPackage()).isSameAs(approved);
	}

	@Test
	void testEachVersionResolvesItsOwnMetadataByInstance() {
		EPackage draft = draftPackage();
		EPackage approved = approvedPackage();
		service.registerPackage(draft);
		service.registerPackage(approved);

		assertThat(firstFeatureName(service.getPackageMetadata(draft).orElseThrow())).isEqualTo("name");
		assertThat(firstFeatureName(service.getPackageMetadata(approved).orElseThrow())).isEqualTo("fullName");
	}

	// ---- dedupe: identical content on two instances is one model version ----------

	@Test
	void testIdenticalContentDedupesOntoOneEntry() {
		PackageMetadata a = service.registerPackage(draftPackage()).orElseThrow();
		PackageMetadata b = service.registerPackage(draftPackage()).orElseThrow();

		assertThat(b).as("identical content must dedupe onto one model-version entry").isSameAs(a);
	}

	@Test
	void testDedupedEntrySurvivesUntilLastRegistrationIsGone() {
		EPackage branchA = draftPackage();
		EPackage branchB = draftPackage();
		service.registerPackage(branchA);
		service.registerPackage(branchB);

		service.unregisterPackage(branchA);
		assertThat(service.getPackageMetadata(NS_URI))
				.as("one of two live registrations unbinding must not drop the entry")
				.isPresent();

		service.unregisterPackage(branchB);
		assertThat(service.getPackageMetadata(NS_URI))
				.as("the last registration unbinding removes the entry")
				.isEmpty();
	}

	// ---- fingerprint lookup -------------------------------------------------------

	@Test
	void testFingerprintLookupResolvesTheExactVersion() {
		PackageMetadata draftMetadata = service.registerPackage(draftPackage()).orElseThrow();
		PackageMetadata approvedMetadata = service.registerPackage(approvedPackage()).orElseThrow();

		assertThat(service.getPackageMetadataByFingerprint(draftMetadata.getModelFingerprint()))
				.containsSame(draftMetadata);
		assertThat(service.getPackageMetadataByFingerprint(approvedMetadata.getModelFingerprint()))
				.containsSame(approvedMetadata);
		assertThat(service.getPackageMetadataByFingerprint("fp1:unknown")).isEmpty();
	}

	// ---- stateless pull: no prior registration required ---------------------------

	@Test
	void testPullResolvesWithoutPriorRegistration() {
		EPackage draft = draftPackage();

		PackageMetadata pulled = service.getPackageMetadata(draft)
				.orElseThrow(() -> new AssertionError("the pull path must build on miss (resolve-or-build)"));

		assertThat(service.getPackageMetadata(draft)).as("second pull is a cache hit").containsSame(pulled);
		assertThat(service.getPackageMetadataByFingerprint(pulled.getModelFingerprint())).containsSame(pulled);
	}

	@Test
	void testUnregisterNeverEvictsPullCreatedEntries() {
		EPackage draft = draftPackage();
		PackageMetadata pulled = service.getPackageMetadata(draft).orElseThrow();

		service.unregisterPackage(draftPackage()); // equal content, was never registered

		assertThat(service.getPackageMetadata(draft))
				.as("a pull-created cache entry carries no liveness and must not be evicted")
				.containsSame(pulled);
	}

	@Test
	void testPullAfterRegistrationReturnsTheRegisteredEntry() {
		PackageMetadata registered = service.registerPackage(draftPackage()).orElseThrow();

		assertThat(service.getPackageMetadata(draftPackage()))
				.as("an equal-content instance must resolve to the same model-version entry")
				.containsSame(registered);
	}

	// ---- versions getter: enumerate all versions for an nsURI ----------------------

	@Test
	void testVersionsIsEmptyForUnknownOrNullNsUri() {
		assertThat(service.getPackageMetadataVersions("http://example.org/nope"))
				.as("an unknown nsURI yields no versions")
				.isEmpty();
		assertThat(service.getPackageMetadataVersions(null))
				.as("a null nsURI yields no versions, never null")
				.isEmpty();
	}

	@Test
	void testVersionsReturnsTheSingleRegisteredVersion() {
		PackageMetadata draftMetadata = service.registerPackage(draftPackage()).orElseThrow();

		assertThat(service.getPackageMetadataVersions(NS_URI)).containsExactly(draftMetadata);
	}

	@Test
	void testVersionsReturnsAllDivergingVersionsNewestLast() {
		PackageMetadata draftMetadata = service.registerPackage(draftPackage()).orElseThrow();
		PackageMetadata approvedMetadata = service.registerPackage(approvedPackage()).orElseThrow();

		List<PackageMetadata> versions = service.getPackageMetadataVersions(NS_URI);
		assertThat(versions)
				.as("both diverging same-nsURI versions must be enumerated, oldest first")
				.containsExactly(draftMetadata, approvedMetadata);
		assertThat(service.getPackageMetadata(NS_URI))
				.as("the tail must equal the best-effort newest served by the nsURI lookup")
				.containsSame(versions.get(versions.size() - 1));
	}

	@Test
	void testVersionsShrinksWhenAVersionIsUnregistered() {
		service.registerPackage(draftPackage());
		EPackage approved = approvedPackage();
		PackageMetadata approvedMetadata = service.registerPackage(approved).orElseThrow();

		assertThat(service.getPackageMetadataVersions(NS_URI)).hasSize(2);

		service.unregisterPackage(approved);

		assertThat(service.getPackageMetadataVersions(NS_URI))
				.as("the unregistered version must drop out of the set")
				.hasSize(1)
				.doesNotContain(approvedMetadata);
	}

	@Test
	void testVersionsIsEmptyAfterLastVersionUnregistered() {
		EPackage draft = draftPackage();
		service.registerPackage(draft);

		service.unregisterPackage(draft);

		assertThat(service.getPackageMetadataVersions(NS_URI))
				.as("once the last version is gone the nsURI has no versions")
				.isEmpty();
	}

	@Test
	void testVersionsSnapshotIsDefensive() {
		service.registerPackage(draftPackage());

		List<PackageMetadata> snapshot = service.getPackageMetadataVersions(NS_URI);

		assertThat(snapshot)
				.as("the getter must hand out a snapshot, not the live backing list")
				.isUnmodifiable();
		assertThat(service.getPackageMetadataVersions(NS_URI)).hasSize(1);
	}

	// ---- divergence that is visible only in generics -------------------------------

	@Test
	void testVersionsDivergingOnlyInTypeArgumentsCoexist() {
		PackageMetadata stringMetadata = service.registerPackage(personHoldingBoxOf(EcorePackage.Literals.ESTRING))
				.orElseThrow();
		PackageMetadata intMetadata = service.registerPackage(personHoldingBoxOf(EcorePackage.Literals.EINT))
				.orElseThrow();

		assertThat(intMetadata.getModelFingerprint())
				.as("Box<EString> and Box<EInt> are different model versions")
				.isNotEqualTo(stringMetadata.getModelFingerprint());
		assertThat(intMetadata).as("the second version must not be discarded onto the first entry")
				.isNotSameAs(stringMetadata);
		assertThat(service.getPackageMetadataVersions(NS_URI))
				.as("both generics variants must be enumerated as separate versions")
				.hasSize(2);
	}

	@Test
	void testEachGenericsVariantResolvesItsOwnMetadataByInstance() {
		EPackage stringBoxed = personHoldingBoxOf(EcorePackage.Literals.ESTRING);
		EPackage intBoxed = personHoldingBoxOf(EcorePackage.Literals.EINT);
		service.registerPackage(stringBoxed);
		service.registerPackage(intBoxed);

		// The invariant registration documents as "never": an instance must not be served
		// another version's metadata.
		assertThat(service.getPackageMetadata(stringBoxed).orElseThrow().getEPackage()).isSameAs(stringBoxed);
		assertThat(service.getPackageMetadata(intBoxed).orElseThrow().getEPackage()).isSameAs(intBoxed);
	}

	@Test
	void testUnregisterOneGenericsVariantKeepsTheOther() {
		EPackage stringBoxed = personHoldingBoxOf(EcorePackage.Literals.ESTRING);
		EPackage intBoxed = personHoldingBoxOf(EcorePackage.Literals.EINT);
		service.registerPackage(stringBoxed);
		service.registerPackage(intBoxed);

		service.unregisterPackage(intBoxed);

		assertThat(service.getPackageMetadata(NS_URI))
				.as("unregistering one generics variant must not remove the other")
				.get()
				.extracting(PackageMetadata::getEPackage)
				.isSameAs(stringBoxed);
	}

	// ---- representation independence: two instances, one model version --------------

	/**
	 * A generated {@code EPackage} and the same model loaded from its {@code .ecore} are two
	 * Java instances of <b>one</b> model version - that is a guaranteed property, asserted by
	 * the equivalence gate (issue #57) - and the dedupe above puts them on one tree. Element
	 * lookups are keyed by instance for speed, but instance identity is a cache key, not the
	 * identity of the model: an EClass of the second instance must resolve to the shared
	 * metadata rather than to nothing.
	 */
	@Test
	void testDedupedSecondInstanceResolvesTheSharedClassMetadata() {
		EPackage generated = draftPackage();
		EPackage loaded = draftPackage();
		service.registerPackage(generated);
		service.registerPackage(loaded);

		assertThat(service.getClassMetadata(personOf(loaded)))
				.as("the second instance's EClass must resolve to the one tree both share")
				.containsSame(service.getClassMetadata(personOf(generated)).orElseThrow());
	}

	@Test
	void testDedupedSecondInstanceResolvesTheSharedFeatureMetadata() {
		EPackage generated = draftPackage();
		EPackage loaded = draftPackage();
		service.registerPackage(generated);
		service.registerPackage(loaded);

		assertThat(service.getFeatureMetadata(personOf(loaded).getEStructuralFeature("name")))
				.containsSame(service.getFeatureMetadata(personOf(generated).getEStructuralFeature("name"))
						.orElseThrow());
	}

	/**
	 * Operation names are not unique under overloading, so the correspondence between the two
	 * instances must be positional - which is sound exactly because equal fingerprints imply
	 * an equal declared order.
	 */
	@Test
	void testDedupedSecondInstanceResolvesOverloadedOperationsByPosition() {
		EPackage generated = personPackageWithOverloads();
		EPackage loaded = personPackageWithOverloads();
		service.registerPackage(generated);
		service.registerPackage(loaded);

		List<EOperation> loadedOperations = personOf(loaded).getEOperations();
		assertThat(loadedOperations).hasSize(2);
		assertThat(service.getOperationMetadata(loadedOperations.get(0)).orElseThrow().getParameters()).isEmpty();
		assertThat(service.getOperationMetadata(loadedOperations.get(1)).orElseThrow().getParameters()).hasSize(1);
	}

	/**
	 * The case this exists for: content anchored on the model version - an
	 * {@code AspectEntry} contributed when the first instance registered - must be answerable
	 * through the second instance's EClass. Otherwise a consumer holding an EObject loaded
	 * through a ResourceSet whose package registry carries the other instance finds nothing,
	 * although the content sits on the very tree that describes its model.
	 */
	@Test
	void testDedupedSecondInstanceFindsAspectsOfTheModelVersion() {
		service.addMetadataHandler(new MetadataHandler() {

			@Override
			public void onPackageRegistered(PackageMetadata packageMetadata) {
				AspectEntry aspect = MetadataFactory.eINSTANCE.createAspectEntry();
				aspect.setTypeId("mapping");
				packageMetadata.getClasses().get(0).getAspects().add(aspect);
			}
		});
		service.registerPackage(draftPackage());
		EPackage loaded = draftPackage();
		service.registerPackage(loaded);

		assertThat(service.getClassAspect(personOf(loaded), "mapping"))
				.as("the aspect of the shared model version must answer for either instance")
				.isPresent();
	}

	/** The pull path dedupes as well, so it must resolve elements just the same. */
	@Test
	void testPullCreatedVersionResolvesElementsOfAnEqualInstance() {
		service.getPackageMetadata(draftPackage());
		EPackage loaded = draftPackage();

		assertThat(service.getClassMetadata(personOf(loaded))).isPresent();
	}

	/**
	 * The fallback must not become a name lookup: two <em>diverging</em> instances are two
	 * model versions and each EClass keeps resolving to its own.
	 */
	@Test
	void testDivergingInstancesStillResolveTheirOwnClassMetadata() {
		EPackage draft = draftPackage();
		EPackage approved = approvedPackage();
		service.registerPackage(draft);
		service.registerPackage(approved);

		ClassMetadata draftClass = service.getClassMetadata(personOf(draft)).orElseThrow();
		ClassMetadata approvedClass = service.getClassMetadata(personOf(approved)).orElseThrow();
		assertThat(draftClass).isNotSameAs(approvedClass);
		assertThat(draftClass.getFeatures().get(0).getName()).isEqualTo("name");
		assertThat(approvedClass.getFeatures().get(0).getName()).isEqualTo("fullName");
	}

	@Test
	void testUnknownModelVersionResolvesToNothing() {
		service.registerPackage(draftPackage());

		assertThat(service.getClassMetadata(personOf(approvedPackage())))
				.as("a version nobody registered has no metadata - the fallback is per version, not per name")
				.isEmpty();
	}

	/**
	 * No stale answers: once the last registration of a version is gone, neither instance
	 * resolves any longer. A memoized fallback would leak metadata of a withdrawn tree here.
	 */
	@Test
	void testWithdrawnVersionResolvesForNeitherInstance() {
		EPackage generated = draftPackage();
		EPackage loaded = draftPackage();
		service.registerPackage(generated);
		service.registerPackage(loaded);
		assertThat(service.getClassMetadata(personOf(loaded))).isPresent();

		service.unregisterPackage(generated);
		service.unregisterPackage(loaded);

		assertThat(service.getClassMetadata(personOf(loaded))).isEmpty();
		assertThat(service.getClassMetadata(personOf(generated))).isEmpty();
	}

	private static EClass personOf(EPackage ePackage) {
		return (EClass) ePackage.getEClassifier("Person");
	}

	/** {@code Person{name, greet(), greet(EString)}} - the overload case. */
	private static EPackage personPackageWithOverloads() {
		EPackage ePackage = personPackage("name");
		EClass person = personOf(ePackage);

		EOperation greet = EcoreFactory.eINSTANCE.createEOperation();
		greet.setName("greet");
		person.getEOperations().add(greet);

		EOperation greetSomeone = EcoreFactory.eINSTANCE.createEOperation();
		greetSomeone.setName("greet");
		EParameter who = EcoreFactory.eINSTANCE.createEParameter();
		who.setName("who");
		who.setEType(EcorePackage.Literals.ESTRING);
		greetSomeone.getEParameters().add(who);
		person.getEOperations().add(greetSomeone);
		return ePackage;
	}

	private static String firstFeatureName(PackageMetadata metadata) {
		return metadata.getClasses().get(0).getFeatures().get(0).getName();
	}

	/** The draft variant: {@code Person{name}}. */
	private static EPackage draftPackage() {
		return personPackage("name");
	}

	/** The approved variant: {@code Person{fullName}} - same nsURI, diverging content. */
	private static EPackage approvedPackage() {
		return personPackage("fullName");
	}

	private static EPackage personPackage(String attributeName) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("person");
		ePackage.setNsPrefix("person");
		ePackage.setNsURI(NS_URI);

		EClass person = EcoreFactory.eINSTANCE.createEClass();
		person.setName("Person");
		ePackage.getEClassifiers().add(person);

		EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();
		attribute.setName(attributeName);
		attribute.setEType(EcorePackage.Literals.ESTRING);
		person.getEStructuralFeatures().add(attribute);
		return ePackage;
	}

	/**
	 * {@code class Box<T> { T value }  class Person { Box<argument> box }} - two variants
	 * that differ <em>only</em> in the type argument. Both references have
	 * {@code eType == Box}, so a canonical form blind to {@link EGenericType} hashes them
	 * identically and the dedupe then discards the second version. This is the case that
	 * makes "same nsURI, same structure, different model" concrete.
	 */
	private static EPackage personHoldingBoxOf(EClassifier argument) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("person");
		ePackage.setNsPrefix("person");
		ePackage.setNsURI(NS_URI);

		EClass box = EcoreFactory.eINSTANCE.createEClass();
		box.setName("Box");
		ePackage.getEClassifiers().add(box);
		ETypeParameter typeParameter = EcoreFactory.eINSTANCE.createETypeParameter();
		typeParameter.setName("T");
		box.getETypeParameters().add(typeParameter);
		EAttribute value = EcoreFactory.eINSTANCE.createEAttribute();
		value.setName("value");
		EGenericType valueType = EcoreFactory.eINSTANCE.createEGenericType();
		valueType.setETypeParameter(typeParameter);
		value.setEGenericType(valueType);
		box.getEStructuralFeatures().add(value);

		EClass person = EcoreFactory.eINSTANCE.createEClass();
		person.setName("Person");
		ePackage.getEClassifiers().add(person);
		EGenericType boxOfArgument = EcoreFactory.eINSTANCE.createEGenericType();
		boxOfArgument.setEClassifier(box);
		EGenericType typeArgument = EcoreFactory.eINSTANCE.createEGenericType();
		typeArgument.setEClassifier(argument);
		boxOfArgument.getETypeArguments().add(typeArgument);
		EReference boxReference = EcoreFactory.eINSTANCE.createEReference();
		boxReference.setName("box");
		boxReference.setContainment(true);
		boxReference.setEGenericType(boxOfArgument);
		person.getEStructuralFeatures().add(boxReference);

		return ePackage;
	}
}
