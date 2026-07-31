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
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Restoring a saved registry: what was computed once is adopted instead of rebuilt, and
 * what has gone stale is refused.
 * <p>
 * The point of persisting {@code getRegistry()} is to skip an expensive derivation on the
 * next node or the next start. That only pays off if the restored state is reachable
 * through every lookup a freshly built one would answer - and it is only safe if a tree
 * whose model has changed underneath it is left behind rather than served.
 */
class LoadRegistryTest {

	private static final String NS_URI = "http://fennec.eclipse.org/test/metadata/load";

	private MetadataServiceImpl service;

	@BeforeEach
	void setup() {
		service = new MetadataServiceImpl();
		service.setFingerprintService(new DefaultFingerprintService());
	}

	@Test
	void testAdoptedTreesAreReachableThroughEveryLookup() {
		EPackage ePackage = personPackage("name");
		MetadataRegistry saved = registryFor(ePackage);
		String fingerprint = saved.getPackages().get(0).getModelFingerprint();

		List<PackageMetadata> adopted = service.loadRegistry(saved);

		assertThat(adopted).hasSize(1);
		PackageMetadata tree = adopted.get(0);
		assertThat(service.getPackageMetadataByFingerprint(fingerprint)).contains(tree);
		assertThat(service.getPackageMetadata(NS_URI)).contains(tree);
		assertThat(service.getPackageMetadataVersions(NS_URI)).containsExactly(tree);
		assertThat(service.getPackageMetadata(ePackage)).as("the live instance resolves onto the adopted tree")
				.contains(tree);

		EClass person = (EClass) ePackage.getEClassifier("Person");
		assertThat(service.getClassMetadata(person)).isPresent();
		assertThat(service.getFeatureMetadata(person.getEStructuralFeature("name"))).isPresent();
		assertThat(service.getOperationMetadata(person.getEOperations().get(0))).isPresent();
		assertThat(service.getClassMetadataByURI(EcoreUtil.getURI(person).toString()))
				.as("the index is populated from what was adopted").isPresent();
	}

	@Test
	void testContributedContentSurvivesTheRoundTrip() {
		MetadataRegistry saved = registryFor(personPackage("name"));
		AspectEntry entry = MetadataFactory.eINSTANCE.createAspectEntry();
		entry.setTypeId("codec");
		entry.setContent(EcoreFactory.eINSTANCE.createEAnnotation());
		saved.getPackages().get(0).getAspects().add(entry);

		service.loadRegistry(saved);

		assertThat(service.getPackageMetadata(NS_URI).orElseThrow().getAspects())
				.as("the derivation is what was worth saving").hasSize(1);
	}

	@Test
	void testAdoptionIsAMoveOutOfTheSourceRegistry() {
		MetadataRegistry saved = registryFor(personPackage("name"));

		service.loadRegistry(saved);

		assertThat(saved.getPackages()).as("what was adopted is gone from the argument").isEmpty();
		assertThat(service.getRegistry().getPackages()).hasSize(1);
	}

	@Test
	void testATreeWhoseModelMovedOnIsRefused() {
		EPackage ePackage = personPackage("name");
		MetadataRegistry saved = registryFor(ePackage);
		// The very failure fingerprint keying exists for: the saved tree describes "name",
		// the package it points at now says "fullName".
		((EClass) ePackage.getEClassifier("Person")).getEStructuralFeatures().get(0).setName("fullName");

		assertThat(service.loadRegistry(saved)).isEmpty();
		assertThat(service.getPackageMetadata(NS_URI)).isEmpty();
	}

	@Test
	void testALiveTreeWinsOverAStoredOne() {
		EPackage ePackage = personPackage("name");
		PackageMetadata live = service.registerPackage(ePackage).orElseThrow();
		MetadataRegistry saved = registryFor(personPackage("name"));

		assertThat(service.loadRegistry(saved)).as("same model version, already built here").isEmpty();
		assertThat(service.getPackageMetadata(NS_URI)).contains(live);
	}

	@Test
	void testAnUnkeyableTreeIsSkipped() {
		MetadataRegistry saved = MetadataFactory.eINSTANCE.createMetadataRegistry();
		PackageMetadata withoutFingerprint = MetadataFactory.eINSTANCE.createPackageMetadata();
		withoutFingerprint.setNsURI(NS_URI);
		saved.getPackages().add(withoutFingerprint);

		assertThat(service.loadRegistry(saved)).isEmpty();
		assertThat(service.getRegistry().getPackages()).isEmpty();
	}

	@Test
	void testAdoptedTreesTakeNoLivenessCount() {
		EPackage ePackage = personPackage("name");
		service.loadRegistry(registryFor(ePackage));

		service.unregisterPackage(ePackage);

		assertThat(service.getPackageMetadata(NS_URI)).as("an unbind must not evict cached state").isPresent();
	}

	@Test
	void testAdoptionDoesNotReRunHandlers() {
		MetadataRegistry saved = registryFor(personPackage("name"));
		CountingHandler handler = new CountingHandler();
		service.addMetadataHandler(handler);

		service.loadRegistry(saved);

		assertThat(handler.calls).as("the handler's work is already in the saved tree").isZero();
	}

	@Test
	void testConstructingOverARegistryMakesItReachable() {
		MetadataRegistry saved = registryFor(personPackage("name"));

		MetadataServiceImpl restored = new MetadataServiceImpl(saved);

		assertThat(restored.getPackageMetadata(NS_URI)).as("a registry handed to the constructor is adopted too")
				.isPresent();
		assertThat(restored.getRegistry().getPackages()).hasSize(1);
	}

	@Test
	void testNullIsIgnored() {
		assertThat(service.loadRegistry(null)).isEmpty();
	}

	/** A registry as it would come back from storage: built elsewhere, then detached. */
	private static MetadataRegistry registryFor(EPackage ePackage) {
		MetadataServiceImpl builder = new MetadataServiceImpl();
		builder.setFingerprintService(new DefaultFingerprintService());
		builder.registerPackage(ePackage);
		return builder.getRegistry();
	}

	private static final class CountingHandler implements MetadataHandler {

		private int calls;

		@Override
		public void onPackageRegistered(PackageMetadata packageMetadata) {
			calls++;
		}
	}

	private static EPackage personPackage(String attributeName) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("test");
		ePackage.setNsURI(NS_URI);
		ePackage.setNsPrefix("test");

		EClass person = EcoreFactory.eINSTANCE.createEClass();
		person.setName("Person");

		EAttribute name = EcoreFactory.eINSTANCE.createEAttribute();
		name.setName(attributeName);
		name.setEType(EcorePackage.Literals.ESTRING);
		person.getEStructuralFeatures().add(name);

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
