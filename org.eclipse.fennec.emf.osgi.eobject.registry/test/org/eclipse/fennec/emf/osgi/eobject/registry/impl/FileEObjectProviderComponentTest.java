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
package org.eclipse.fennec.emf.osgi.eobject.registry.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistries;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The DS wiring of the file provider: ResourceSet per load from the factory, key
 * strategy switching via configuration.
 */
public class FileEObjectProviderComponentTest {

	private static final String NS_URI = "http://fennec.eclipse.org/test/library/1.0";

	@TempDir
	Path tempDir;

	private EPackage libraryPackage;
	private EClass libraryClass;
	private EAttribute nameAttribute;
	private final ResourceSetFactory resourceSetFactory = this::resourceSet;
	private int createdResourceSets;

	@BeforeEach
	public void setUp() {
		libraryPackage = EcoreFactory.eINSTANCE.createEPackage();
		libraryPackage.setName("library");
		libraryPackage.setNsPrefix("lib");
		libraryPackage.setNsURI(NS_URI);
		libraryClass = EcoreFactory.eINSTANCE.createEClass();
		libraryClass.setName("Library");
		nameAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		nameAttribute.setName("name");
		nameAttribute.setEType(EcorePackage.Literals.ESTRING);
		libraryClass.getEStructuralFeatures().add(nameAttribute);
		libraryPackage.getEClassifiers().add(libraryClass);
	}

	private ResourceSet resourceSet() {
		createdResourceSets++;
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		resourceSet.getPackageRegistry().put(NS_URI, libraryPackage);
		return resourceSet;
	}

	private FileEObjectProviderConfig config(String name, String keyFeature, String... locations) {
		return config(name, keyFeature, DEFAULT_EXTENSIONS, locations);
	}

	/** The annotation default of {@code file.extensions}, which DS applies when the property is absent. */
	private static final String[] DEFAULT_EXTENSIONS = { "xmi", "ecore", "json", "xml" };

	private FileEObjectProviderConfig config(String name, String keyFeature, String[] fileExtensions,
			String... locations) {
		return new FileEObjectProviderConfig() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return FileEObjectProviderConfig.class;
			}

			@Override
			public String emf_eobject_provider_name() {
				return name;
			}

			@Override
			public String[] locations() {
				return locations;
			}

			@Override
			public String key_feature() {
				return keyFeature;
			}

			@Override
			public String[] file_extensions() {
				return fileExtensions;
			}
		};
	}

	private Path writeFixture(String fileName, String libraryName) throws Exception {
		Path file = tempDir.resolve(fileName);
		Resource resource = resourceSet().createResource(URI.createFileURI(file.toAbsolutePath().toString()));
		EObject library = EcoreUtil.create(libraryClass);
		library.eSet(nameAttribute, libraryName);
		resource.getContents().add(library);
		resource.save(Map.of());
		return file;
	}

	@Test
	public void testBlankProviderNameIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileEObjectProviderComponent(resourceSetFactory, config("", "")));
	}

	@Test
	public void testLoadsThroughTheResourceSetFactoryWithFeatureKeys() throws Exception {
		writeFixture("libs.xmi", "central");
		FileEObjectProviderComponent component = new FileEObjectProviderComponent(resourceSetFactory,
				config("my-files", "name", tempDir.toString()));
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("test");

		component.load(writer).join();

		assertThat(writer.getRegistry().get("central")).isPresent();
		assertThat(writer.getRegistry().getEntry("central").orElseThrow().source()).isEqualTo("my-files");
	}

	@Test
	public void testDefaultKeysAndFreshResourceSetPerLoad() throws Exception {
		writeFixture("libs.xmi", "central");
		FileEObjectProviderComponent component = new FileEObjectProviderComponent(resourceSetFactory,
				config("my-files", "", tempDir.toString()));
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("test");
		int before = createdResourceSets;

		component.load(writer).join();
		component.load(writer).join();

		assertThat(writer.getRegistry().getEntry("libs.xmi#/")).isPresent();
		assertThat(createdResourceSets - before)
				.as("each load pass must use a fresh ResourceSet - a cached one would never re-read changed files")
				.isEqualTo(2);
	}

	@Test
	public void testDefaultExtensionsKeepPlaceholderFilesOutOfTheLoad() throws Exception {
		writeFixture("libs.xmi", "central");
		// loadable content behind a non-model extension: only the allow-list keeps it out, so this
		// asserts the default really reaches the walk instead of the walk taking everything
		writeFixture("README.md", "readme");
		Files.writeString(tempDir.resolve(".keep"), "");
		FileEObjectProviderComponent component = new FileEObjectProviderComponent(resourceSetFactory,
				config("my-files", "name", tempDir.toString()));
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("test");

		component.load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
		assertThat(writer.getRegistry().get("central")).isPresent();
		assertThat(writer.getRegistry().get("readme")).isEmpty();
	}

	@Test
	public void testConfiguredExtensionsAreApplied() throws Exception {
		writeFixture("mapping.conf", "configured");
		writeFixture("libs.xmi", "central");
		FileEObjectProviderComponent component = new FileEObjectProviderComponent(resourceSetFactory,
				config("my-files", "name", new String[] { "conf" }, tempDir.toString()));
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("test");

		component.load(writer).join();

		assertThat(writer.getRegistry().get("configured")).isPresent();
		assertThat(writer.getRegistry().get("central")).isEmpty();
	}

	@Test
	public void testEmptyExtensionsConfigurationAttemptsEveryFile() throws Exception {
		writeFixture("mapping.conf", "configured");
		writeFixture("libs.xmi", "central");
		FileEObjectProviderComponent component = new FileEObjectProviderComponent(resourceSetFactory,
				config("my-files", "name", new String[0], tempDir.toString()));
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("test");

		component.load(writer).join();

		assertThat(writer.getRegistry().get("configured")).isPresent();
		assertThat(writer.getRegistry().get("central")).isPresent();
	}

	@Test
	public void testReloadAfterFileRemovalDropsTheEntry() throws Exception {
		Path file = writeFixture("libs.xmi", "central");
		FileEObjectProviderComponent component = new FileEObjectProviderComponent(resourceSetFactory,
				config("my-files", "name", tempDir.toString()));
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("test");
		component.load(writer).join();
		assertThat(writer.getRegistry().get("central")).isPresent();

		Files.delete(file);
		component.load(writer).join();

		assertThat(writer.getRegistry().get("central")).isEmpty();
	}
}
