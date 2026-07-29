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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.metadata.impl.MetadataServiceImpl;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.junit.jupiter.api.Test;

/**
 * Genericity gate, spike (a): an eorm-style consumer walks the mirror tree and attaches its
 * own mapping model - <b>without inheriting from the metadata model</b> (issue #63).
 * <p>
 * This is the test of the M7 decision. In the donor design a provider had to subclass
 * {@code ClassAspect} to attach anything, which made the metadata model a supertype of
 * every consumer's model and forced them into one release train. Here the provider brings
 * an EPackage the metadata model has never seen - built dynamically, so not even a compile
 * time relationship exists - and drops instances into {@link AspectEntry#getContent()}.
 * <p>
 * Two things have to hold for the gate to pass: the consumer needs nothing but the public
 * API to do its work, and the result must still serialize, because a metadata tree that
 * cannot be written and read back is not a cache.
 */
class EormStyleVisitorSpikeTest {

	private static final String MAPPING_NS_URI = "http://example.org/spike/eorm/1.0";
	private static final String DOMAIN_NS_URI = "http://example.org/spike/domain/1.0";
	private static final String EORM = "eorm";

	/**
	 * The consumer's own model: a table mapping per class. Built at runtime on purpose - if
	 * the mechanism works for a package that did not exist when the metadata bundle was
	 * compiled, the independence claim is not a matter of packaging discipline.
	 */
	private static final EPackage MAPPING_PACKAGE = mappingPackage();

	/**
	 * The consumer: a {@link MetadataHandler} that visits the tree and attaches one mapping
	 * per class. It uses nothing but the API - no cast to an implementation type, no
	 * subclass of anything in the metadata model.
	 */
	private static final class TableMappingVisitor implements MetadataHandler {

		@Override
		public void onPackageRegistered(PackageMetadata packageMetadata) {
			for (ClassMetadata classMetadata : packageMetadata.getClasses()) {
				EObject mapping = EcoreUtil.create((EClass) MAPPING_PACKAGE.getEClassifier("TableMapping"));
				mapping.eSet(feature(mapping, "tableName"), classMetadata.getName().toLowerCase());
				@SuppressWarnings("unchecked")
				List<String> columns = (List<String>) mapping.eGet(feature(mapping, "columns"));
				classMetadata.getFeatures().forEach(featureMetadata -> columns.add(featureMetadata.getName()));

				AspectEntry entry = MetadataFactory.eINSTANCE.createAspectEntry();
				entry.setTypeId(EORM);
				entry.setContent(mapping);
				classMetadata.getAspects().add(entry);
			}
		}
	}

	@Test
	void testConsumerAttachesItsOwnModelWithoutInheritingFromTheMetadataModel() {
		MetadataServiceImpl service = service();
		service.addMetadataHandler(new TableMappingVisitor());
		EPackage domain = domainPackage();

		service.registerPackage(domain);

		EClass person = (EClass) domain.getEClassifier("Person");
		EObject mapping = service.getClassAspect(person, EORM).orElseThrow().getContent();

		assertThat(mapping.eGet(feature(mapping, "tableName"))).isEqualTo("person");
		assertThat(mapping.eGet(feature(mapping, "columns"))).asInstanceOf(
				org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
				.containsExactly("name", "age");
	}

	@Test
	void testTheAttachedModelIsUnrelatedToTheMetadataModel() {
		MetadataServiceImpl service = service();
		service.addMetadataHandler(new TableMappingVisitor());
		EPackage domain = domainPackage();
		service.registerPackage(domain);

		EClass person = (EClass) domain.getEClassifier("Person");
		EClass mappingClass = service.getClassAspect(person, EORM).orElseThrow().getContent().eClass();

		assertThat(mappingClass.getEPackage().getNsURI())
				.as("the content comes from the consumer's own package")
				.isEqualTo(MAPPING_NS_URI);
		assertThat(mappingClass.getEAllSuperTypes())
				.as("nothing in the consumer's model derives from the metadata model")
				.noneMatch(superType -> MetadataPackage.eNS_URI.equals(superType.getEPackage().getNsURI()));
	}

	@Test
	void testTheTreeWithForeignContentSurvivesSerialization() throws Exception {
		MetadataServiceImpl service = service();
		service.addMetadataHandler(new TableMappingVisitor());
		EPackage domain = domainPackage();
		service.registerPackage(domain);

		MetadataRegistry reloaded = roundTrip(service.getRegistry(), domain);

		AspectEntry entry = reloaded.getPackages().get(0).getClasses().get(0).getAspects().get(0);
		assertThat(entry.getTypeId()).isEqualTo(EORM);
		assertThat(entry.getContent().eGet(feature(entry.getContent(), "tableName")))
				.as("the containment slot is what makes a foreign aspect transferable")
				.isEqualTo("person");
	}

	@Test
	void testTransientContentIsDeliberatelyLost() throws Exception {
		MetadataServiceImpl service = service();
		service.addMetadataHandler(metadata -> {
			AspectEntry entry = MetadataFactory.eINSTANCE.createAspectEntry();
			entry.setTypeId("cache");
			// A compiled query, a live connection - something no serializer could carry.
			entry.setTransientContent(new Object());
			metadata.getAspects().add(entry);
		});
		EPackage domain = domainPackage();
		service.registerPackage(domain);

		assertThat(service.getPackageAspect(domain, "cache").orElseThrow().getTransientContent()).isNotNull();

		MetadataRegistry reloaded = roundTrip(service.getRegistry(), domain);

		assertThat(reloaded.getPackages().get(0).getAspects().get(0).getTransientContent())
				.as("the transient slot is the one that must not travel - that is why there are two")
				.isNull();
	}

	private static MetadataServiceImpl service() {
		MetadataServiceImpl service = new MetadataServiceImpl();
		service.setFingerprintService(new DefaultFingerprintService());
		return service;
	}

	/**
	 * Writes the registry and the described model to XMI and reads both back into a fresh
	 * resource set, so nothing but the serialized form can carry information across.
	 */
	private static MetadataRegistry roundTrip(MetadataRegistry registry, EPackage domain) throws Exception {
		ResourceSet writeSet = resourceSet();
		Resource modelResource = writeSet.createResource(URI.createURI("memory:/domain.ecore"));
		modelResource.getContents().add(domain);
		Resource registryResource = writeSet.createResource(URI.createURI("memory:/registry.xmi"));
		registryResource.getContents().add(registry);

		ByteArrayOutputStream model = new ByteArrayOutputStream();
		modelResource.save(model, Map.of());
		ByteArrayOutputStream metadata = new ByteArrayOutputStream();
		registryResource.save(metadata, Map.of());

		ResourceSet readSet = resourceSet();
		Resource reloadedModel = readSet.createResource(URI.createURI("memory:/domain.ecore"));
		reloadedModel.load(new ByteArrayInputStream(model.toByteArray()), Map.of());
		Resource reloadedRegistry = readSet.createResource(URI.createURI("memory:/registry.xmi"));
		reloadedRegistry.load(new ByteArrayInputStream(metadata.toByteArray()), Map.of());
		return (MetadataRegistry) reloadedRegistry.getContents().get(0);
	}

	private static ResourceSet resourceSet() {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		// The reader must know the consumer's model to rebuild its objects - the same
		// condition any foreign aspect content has on the receiving side.
		resourceSet.getPackageRegistry().put(MAPPING_NS_URI, MAPPING_PACKAGE);
		resourceSet.getPackageRegistry().put(MetadataPackage.eNS_URI, MetadataPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		return resourceSet;
	}

	private static EStructuralFeature feature(EObject eObject, String name) {
		return eObject.eClass().getEStructuralFeature(name);
	}

	private static EPackage mappingPackage() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("eorm");
		ePackage.setNsPrefix("eorm");
		ePackage.setNsURI(MAPPING_NS_URI);

		EClass tableMapping = EcoreFactory.eINSTANCE.createEClass();
		tableMapping.setName("TableMapping");
		ePackage.getEClassifiers().add(tableMapping);

		EAttribute tableName = EcoreFactory.eINSTANCE.createEAttribute();
		tableName.setName("tableName");
		tableName.setEType(EcorePackage.Literals.ESTRING);
		tableMapping.getEStructuralFeatures().add(tableName);

		EAttribute columns = EcoreFactory.eINSTANCE.createEAttribute();
		columns.setName("columns");
		columns.setEType(EcorePackage.Literals.ESTRING);
		columns.setUpperBound(-1);
		tableMapping.getEStructuralFeatures().add(columns);

		return ePackage;
	}

	private static EPackage domainPackage() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("domain");
		ePackage.setNsPrefix("domain");
		ePackage.setNsURI(DOMAIN_NS_URI);

		EClass person = EcoreFactory.eINSTANCE.createEClass();
		person.setName("Person");
		ePackage.getEClassifiers().add(person);

		EAttribute name = EcoreFactory.eINSTANCE.createEAttribute();
		name.setName("name");
		name.setEType(EcorePackage.Literals.ESTRING);
		person.getEStructuralFeatures().add(name);

		EAttribute age = EcoreFactory.eINSTANCE.createEAttribute();
		age.setName("age");
		age.setEType(EcorePackage.Literals.EINT);
		person.getEStructuralFeatures().add(age);

		return ePackage;
	}
}
