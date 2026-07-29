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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The identity semantics the metadata service exists for: metadata is keyed by model
 * fingerprint, not by nsURI, and liveness is counted per model version.
 * <p>
 * These are the WP6 invariants in their smallest form. The full acceptance suite follows
 * with issue #62; what is asserted here is what would silently serve one model version's
 * objects with another version's metadata if it broke.
 */
class MetadataServiceImplTest {

	private static final String NS_URI = "http://fennec.eclipse.org/test/metadata";

	private MetadataServiceImpl service;

	@BeforeEach
	void setup() {
		service = new MetadataServiceImpl();
		service.setFingerprintService(new DefaultFingerprintService());
		service.setMetadataIndex(new MapBasedMetadataIndex());
	}

	@Test
	void testRegisteringIdenticalContentTwiceDeduplicates() {
		EPackage first = personPackage("name");
		EPackage second = personPackage("name");

		PackageMetadata a = service.registerPackage(first).orElseThrow();
		PackageMetadata b = service.registerPackage(second).orElseThrow();

		assertThat(b).as("same content is the same model version").isSameAs(a);
		assertThat(service.getRegistry().getPackages()).hasSize(1);
		assertThat(service.getPackageMetadataVersions(NS_URI)).hasSize(1);
	}

	@Test
	void testDivergingContentUnderOneNsUriCoexists() {
		PackageMetadata a = service.registerPackage(personPackage("name")).orElseThrow();
		PackageMetadata b = service.registerPackage(personPackage("fullName")).orElseThrow();

		assertThat(b).isNotSameAs(a);
		assertThat(a.getModelFingerprint()).isNotEqualTo(b.getModelFingerprint());
		assertThat(service.getPackageMetadataVersions(NS_URI))
				.as("both versions stay reachable, in registration order")
				.containsExactly(a, b);
		assertThat(service.getPackageMetadata(NS_URI)).as("nsURI lookup is best effort: newest wins").contains(b);
	}

	@Test
	void testEPackageLookupResolvesTheExactVersion() {
		EPackage older = personPackage("name");
		EPackage newer = personPackage("fullName");
		PackageMetadata a = service.registerPackage(older).orElseThrow();
		PackageMetadata b = service.registerPackage(newer).orElseThrow();

		assertThat(service.getPackageMetadata(older)).contains(a);
		assertThat(service.getPackageMetadata(newer)).contains(b);
		assertThat(service.getPackageMetadataByFingerprint(a.getModelFingerprint())).contains(a);
	}

	@Test
	void testUnregisterOnlyDropsTheLastRegistrationOfThatVersion() {
		EPackage ePackage = personPackage("name");
		service.registerPackage(ePackage);
		service.registerPackage(ePackage);

		service.unregisterPackage(ePackage);
		assertThat(service.getPackageMetadata(NS_URI)).as("one registration is still live").isPresent();

		service.unregisterPackage(ePackage);
		assertThat(service.getPackageMetadata(NS_URI)).isEmpty();
		assertThat(service.getRegistry().getPackages()).isEmpty();
	}

	@Test
	void testUnregisterLeavesTheOtherVersionOfTheSameNsUriAlone() {
		EPackage older = personPackage("name");
		EPackage newer = personPackage("fullName");
		service.registerPackage(older);
		PackageMetadata surviving = service.registerPackage(newer).orElseThrow();

		service.unregisterPackage(older);

		assertThat(service.getPackageMetadataVersions(NS_URI)).containsExactly(surviving);
		assertThat(service.getPackageMetadata(newer)).contains(surviving);
	}

	@Test
	void testPullCreatedMetadataSurvivesUnregister() {
		EPackage ePackage = personPackage("name");

		// Resolve-or-build: a read, not a registration.
		PackageMetadata pulled = service.getPackageMetadata(ePackage).orElseThrow();
		service.unregisterPackage(ePackage);

		assertThat(service.getPackageMetadata(ePackage))
				.as("an unbind must not evict what a read created")
				.contains(pulled);
	}

	@Test
	void testMirrorTreeIsBuiltFromTheModel() {
		EPackage ePackage = personPackage("name");
		PackageMetadata metadata = service.registerPackage(ePackage).orElseThrow();
		EClass person = (EClass) ePackage.getEClassifier("Person");

		ClassMetadata classMetadata = service.getClassMetadata(person).orElseThrow();
		assertThat(classMetadata.getName()).isEqualTo("Person");
		assertThat(classMetadata.getTypeURI()).endsWith("#//Person");
		assertThat(classMetadata.isHasId()).isTrue();
		assertThat(classMetadata.getIdFeatures()).hasSize(1);
		assertThat(classMetadata.getFeatures()).hasSize(2);
		assertThat(classMetadata.getOperations()).hasSize(1);
		assertThat(metadata.getClasses()).contains(classMetadata);

		// The self-reference resolves to the same class metadata.
		ReferenceMetadata friend = (ReferenceMetadata) classMetadata.getFeatures().get(1);
		assertThat(friend.getTargetClassMetadata()).isSameAs(classMetadata);
		assertThat(friend.isContainment()).isFalse();

		assertThat(service.getOperationMetadataFromClass("greet", classMetadata)).isPresent();
		assertThat(service.getFeatureMetadataFromClass("name", classMetadata)).isPresent();
	}

	@Test
	void testIndexFindsWhatWasRegisteredAndForgetsWhatWasNot() {
		EPackage ePackage = personPackage("name");
		service.registerPackage(ePackage);
		EClass person = (EClass) ePackage.getEClassifier("Person");
		String uri = service.getClassMetadata(person).orElseThrow().getTypeURI();

		assertThat(service.getClassMetadataByURI(uri)).isPresent();
		assertThat(service.getClassMetadataByName("Person", NS_URI)).isPresent();

		service.unregisterPackage(ePackage);

		assertThat(service.getClassMetadataByURI(uri)).isEmpty();
		assertThat(service.getClassMetadataByName("Person", NS_URI)).isEmpty();
	}

	@Test
	void testHandlerContributesBeforeTheVersionBecomesVisible() {
		List<String> visibilityDuringCallback = new ArrayList<>();
		service.addMetadataHandler(metadata -> {
			// The tree must not be reachable yet, or a reader could see it without entries.
			visibilityDuringCallback.add(String.valueOf(service.getPackageMetadata(metadata.getNsURI()).isPresent()));
			AspectEntry entry = MetadataFactory.eINSTANCE.createAspectEntry();
			entry.setTypeId("test");
			metadata.getAspects().add(entry);
		});

		EPackage ePackage = personPackage("name");
		service.registerPackage(ePackage);

		assertThat(visibilityDuringCallback).containsExactly("false");
		assertThat(service.getPackageAspect(ePackage, "test")).isPresent();
		assertThat(service.getPackageAspect(ePackage, "other")).isEmpty();
	}

	@Test
	void testLateHandlerSeesEverythingAlreadyRegistered() {
		service.registerPackage(personPackage("name"));
		service.registerPackage(personPackage("fullName"));

		List<PackageMetadata> seen = new ArrayList<>();
		service.addMetadataHandler(seen::add);

		assertThat(seen).as("a contributor arriving late still reaches existing trees").hasSize(2);
	}

	@Test
	void testRemovingAHandlerClearsIt() {
		List<String> calls = new ArrayList<>();
		MetadataHandler handler = new MetadataHandler() {

			@Override
			public void onPackageRegistered(PackageMetadata packageMetadata) {
				calls.add("registered");
			}

			@Override
			public void clear() {
				calls.add("cleared");
			}
		};
		service.addMetadataHandler(handler);
		service.removeMetadataHandler(handler);
		service.registerPackage(personPackage("name"));

		assertThat(calls).containsExactly("cleared");
		assertThat(service.getMetadataHandlers()).isEmpty();
	}

	@Test
	void testServicePropertiesBecomeTransientBuildContext() {
		PackageMetadata metadata = service
				.registerPackage(personPackage("name"), Map.of("emf.name", "person", "ranks", new Object[] { 1, 2 }))
				.orElseThrow();

		assertThat(metadata.getProperties().get("emf.name")).isEqualTo("person");
		assertThat(metadata.getProperties().get("ranks")).isEqualTo("[1, 2]");
	}

	@Test
	void testNullArgumentsAreAnsweredWithEmpty() {
		assertThat(service.registerPackage(null)).isEmpty();
		assertThat(service.getPackageMetadata((EPackage) null)).isEmpty();
		assertThat(service.getPackageMetadata((String) null)).isEmpty();
		assertThat(service.getPackageMetadataByFingerprint(null)).isEmpty();
		assertThat(service.getPackageMetadataVersions(null)).isEmpty();
		assertThat(service.getClassMetadata(null)).isEmpty();
		// must not throw
		service.unregisterPackage(null);
	}

	/**
	 * A one-class package under a fixed nsURI. The name of the second attribute is the
	 * knob: changing it changes the model content and therefore the fingerprint, while the
	 * nsURI stays the same - which is exactly the same-nsURI multi-version case.
	 */
	private static EPackage personPackage(String secondAttributeName) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("test");
		ePackage.setNsURI(NS_URI);
		ePackage.setNsPrefix("test");

		EClass person = EcoreFactory.eINSTANCE.createEClass();
		person.setName("Person");

		EAttribute id = EcoreFactory.eINSTANCE.createEAttribute();
		id.setName(secondAttributeName);
		id.setEType(EcorePackage.Literals.ESTRING);
		id.setID(true);
		person.getEStructuralFeatures().add(id);

		EReference friend = EcoreFactory.eINSTANCE.createEReference();
		friend.setName("friend");
		friend.setEType(person);
		person.getEStructuralFeatures().add(friend);

		EOperation greet = EcoreFactory.eINSTANCE.createEOperation();
		greet.setName("greet");
		greet.setEType(EcorePackage.Literals.ESTRING);
		person.getEOperations().add(greet);

		ePackage.getEClassifiers().add(person);
		return ePackage;
	}
}
