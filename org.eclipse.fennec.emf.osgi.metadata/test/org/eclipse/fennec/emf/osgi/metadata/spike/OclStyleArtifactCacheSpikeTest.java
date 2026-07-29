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
package org.eclipse.fennec.emf.osgi.metadata.spike;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.artifact.ArtifactStore;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.eclipse.fennec.emf.osgi.components.fingerprint.InMemoryArtifactStore;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.metadata.impl.MetadataServiceImpl;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.junit.jupiter.api.Test;

/**
 * Genericity gate, spike (b): a derived artifact - constraint expressions in the style of
 * an OCL cache - built once per model version, keyed by fingerprint in an
 * {@link ArtifactStore}, and carried in the aspect content slot (issue #63).
 * <p>
 * Where spike (a) shows that a foreign model can be <em>attached</em>, this one shows that
 * the expensive part can be <em>skipped</em>. Parsing expressions is the kind of work
 * nobody wants to repeat on every node and every restart; keying the result by the model
 * fingerprint is what makes reuse safe, because the key changes exactly when the model
 * content changes.
 * <p>
 * Again without inheriting from the metadata model: the artifact is an EObject of the
 * consumer's own, runtime-built package.
 */
class OclStyleArtifactCacheSpikeTest {

	private static final String EXPRESSION_NS_URI = "http://example.org/spike/ocl/1.0";
	private static final String DOMAIN_NS_URI = "http://example.org/spike/domain/1.0";
	private static final String OCL = "ocl";
	private static final String CONSTRAINT_SOURCE = "http://example.org/ocl";

	private static final EPackage EXPRESSION_PACKAGE = expressionPackage();

	/**
	 * The consumer: resolve-or-build against the store. On a hit nothing is parsed; on a
	 * miss the expressions are collected from the model and the result is stored under the
	 * model's fingerprint.
	 */
	private static final class OclCacheProvider implements MetadataHandler {

		private final ArtifactStore store;
		private final AtomicInteger builds = new AtomicInteger();

		OclCacheProvider(ArtifactStore store) {
			this.store = store;
		}

		@Override
		public void onPackageRegistered(PackageMetadata packageMetadata) {
			String fingerprint = packageMetadata.getModelFingerprint();
			EObject constraints = store.resolve(fingerprint, OCL).orElseGet(() -> {
				EObject built = parseConstraints(packageMetadata);
				store.put(fingerprint, OCL, built);
				builds.incrementAndGet();
				return built;
			});

			AspectEntry entry = MetadataFactory.eINSTANCE.createAspectEntry();
			entry.setTypeId(OCL);
			entry.setContent(constraints);
			packageMetadata.getAspects().add(entry);
		}

		/** Stands in for the expensive part: reading and compiling expressions. */
		private static EObject parseConstraints(PackageMetadata packageMetadata) {
			EObject set = EcoreUtil.create((EClass) EXPRESSION_PACKAGE.getEClassifier("ConstraintSet"));
			@SuppressWarnings("unchecked")
			List<EObject> constraints = (List<EObject>) set.eGet(feature(set, "constraints"));
			for (ClassMetadata classMetadata : packageMetadata.getClasses()) {
				for (OperationMetadata operationMetadata : classMetadata.getOperations()) {
					EAnnotation annotation = operationMetadata.getEOperation().getEAnnotation(CONSTRAINT_SOURCE);
					if (annotation != null) {
						EObject constraint = EcoreUtil
								.create((EClass) EXPRESSION_PACKAGE.getEClassifier("Constraint"));
						constraint.eSet(feature(constraint, "context"),
								classMetadata.getName() + "::" + operationMetadata.getName());
						constraint.eSet(feature(constraint, "body"), annotation.getDetails().get("body"));
						constraints.add(constraint);
					}
				}
			}
			return set;
		}
	}

	@Test
	void testTheArtifactIsBuiltOnceAndKeyedByTheModelFingerprint() {
		ArtifactStore store = new InMemoryArtifactStore();
		OclCacheProvider provider = new OclCacheProvider(store);
		MetadataServiceImpl service = service();
		service.addMetadataHandler(provider);
		EPackage domain = domainPackage("self.age > 0");

		PackageMetadata metadata = service.registerPackage(domain).orElseThrow();

		assertThat(provider.builds).hasValue(1);
		assertThat(store.resolve(metadata.getModelFingerprint(), OCL))
				.as("the store key is the model version, not the nsURI")
				.isPresent();
		assertThat(constraintBodies(service.getPackageAspect(domain, OCL).orElseThrow()))
				.containsExactly("self.age > 0");
	}

	@Test
	void testASecondNodeReusesTheStoredArtifactInsteadOfRebuilding() {
		ArtifactStore sharedStore = new InMemoryArtifactStore();

		OclCacheProvider first = new OclCacheProvider(sharedStore);
		MetadataServiceImpl firstNode = service();
		firstNode.addMetadataHandler(first);
		firstNode.registerPackage(domainPackage("self.age > 0"));

		// A second service with its own metadata state but the same store - a restart, or
		// another node in the cluster.
		OclCacheProvider second = new OclCacheProvider(sharedStore);
		MetadataServiceImpl secondNode = service();
		secondNode.addMetadataHandler(second);
		EPackage sameModel = domainPackage("self.age > 0");
		secondNode.registerPackage(sameModel);

		assertThat(first.builds).hasValue(1);
		assertThat(second.builds).as("the second node must not repeat the expensive part").hasValue(0);
		assertThat(constraintBodies(secondNode.getPackageAspect(sameModel, OCL).orElseThrow()))
				.as("and it must end up with the same content")
				.containsExactly("self.age > 0");
	}

	@Test
	void testDivergingVersionsOfOneNsUriGetTheirOwnArtifact() {
		ArtifactStore store = new InMemoryArtifactStore();
		OclCacheProvider provider = new OclCacheProvider(store);
		MetadataServiceImpl service = service();
		service.addMetadataHandler(provider);

		EPackage draft = domainPackage("self.age > 0");
		EPackage approved = domainPackage("self.age >= 18");
		PackageMetadata draftMetadata = service.registerPackage(draft).orElseThrow();
		PackageMetadata approvedMetadata = service.registerPackage(approved).orElseThrow();

		assertThat(provider.builds).as("different content is different work").hasValue(2);
		assertThat(draftMetadata.getModelFingerprint()).isNotEqualTo(approvedMetadata.getModelFingerprint());
		assertThat(constraintBodies(service.getPackageAspect(draft, OCL).orElseThrow()))
				.containsExactly("self.age > 0");
		assertThat(constraintBodies(service.getPackageAspect(approved, OCL).orElseThrow()))
				.as("an artifact must never be served to the version it was not derived from")
				.containsExactly("self.age >= 18");
	}

	@Test
	void testTheArtifactModelIsUnrelatedToTheMetadataModel() {
		ArtifactStore store = new InMemoryArtifactStore();
		MetadataServiceImpl service = service();
		service.addMetadataHandler(new OclCacheProvider(store));
		EPackage domain = domainPackage("self.age > 0");
		service.registerPackage(domain);

		EClass artifactClass = service.getPackageAspect(domain, OCL).orElseThrow().getContent().eClass();

		assertThat(artifactClass.getEPackage().getNsURI()).isEqualTo(EXPRESSION_NS_URI);
		assertThat(artifactClass.getEAllSuperTypes())
				.as("the consumer's artifact model stands on its own")
				.isEmpty();
	}

	private static MetadataServiceImpl service() {
		MetadataServiceImpl service = new MetadataServiceImpl();
		service.setFingerprintService(new DefaultFingerprintService());
		return service;
	}

	private static List<String> constraintBodies(AspectEntry entry) {
		EObject set = entry.getContent();
		@SuppressWarnings("unchecked")
		List<EObject> constraints = (List<EObject>) set.eGet(feature(set, "constraints"));
		return constraints.stream().map(constraint -> (String) constraint.eGet(feature(constraint, "body"))).toList();
	}

	private static EStructuralFeature feature(EObject eObject, String name) {
		return eObject.eClass().getEStructuralFeature(name);
	}

	private static EPackage expressionPackage() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("ocl");
		ePackage.setNsPrefix("ocl");
		ePackage.setNsURI(EXPRESSION_NS_URI);

		EClass constraint = EcoreFactory.eINSTANCE.createEClass();
		constraint.setName("Constraint");
		ePackage.getEClassifiers().add(constraint);
		constraint.getEStructuralFeatures().add(attribute("context", EcorePackage.Literals.ESTRING));
		constraint.getEStructuralFeatures().add(attribute("body", EcorePackage.Literals.ESTRING));

		EClass constraintSet = EcoreFactory.eINSTANCE.createEClass();
		constraintSet.setName("ConstraintSet");
		ePackage.getEClassifiers().add(constraintSet);
		EReference constraints = EcoreFactory.eINSTANCE.createEReference();
		constraints.setName("constraints");
		constraints.setEType(constraint);
		constraints.setContainment(true);
		constraints.setUpperBound(-1);
		constraintSet.getEStructuralFeatures().add(constraints);

		return ePackage;
	}

	/**
	 * A domain model whose operation carries a constraint expression as an annotation - the
	 * input the consumer parses.
	 */
	private static EPackage domainPackage(String constraintBody) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("domain");
		ePackage.setNsPrefix("domain");
		ePackage.setNsURI(DOMAIN_NS_URI);

		EClass person = EcoreFactory.eINSTANCE.createEClass();
		person.setName("Person");
		ePackage.getEClassifiers().add(person);
		person.getEStructuralFeatures().add(attribute("age", EcorePackage.Literals.EINT));

		EOperation validate = EcoreFactory.eINSTANCE.createEOperation();
		validate.setName("validate");
		validate.setEType(EcorePackage.Literals.EBOOLEAN);
		EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
		annotation.setSource(CONSTRAINT_SOURCE);
		annotation.getDetails().put("body", constraintBody);
		validate.getEAnnotations().add(annotation);
		person.getEOperations().add(validate);

		return ePackage;
	}

	private static EAttribute attribute(String name, EClassifier type) {
		EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();
		attribute.setName(name);
		attribute.setEType(type);
		return attribute;
	}
}
