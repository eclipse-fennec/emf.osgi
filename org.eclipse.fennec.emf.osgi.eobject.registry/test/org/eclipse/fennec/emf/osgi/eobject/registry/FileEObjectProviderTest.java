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
package org.eclipse.fennec.emf.osgi.eobject.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The file provider against real XMI fixtures: key strategies, directory walking,
 * broken-file resilience and the sync-based re-load swap.
 */
public class FileEObjectProviderTest {

	private static final String NS_URI = "http://fennec.eclipse.org/test/library/1.0";

	@TempDir
	Path tempDir;

	private EPackage libraryPackage;
	private EClass libraryClass;
	private EAttribute nameAttribute;
	private EObjectRegistryWriter writer;

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

		writer = EObjectRegistries.createRegistry("files");
	}

	private ResourceSet resourceSet() {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		resourceSet.getPackageRegistry().put(NS_URI, libraryPackage);
		return resourceSet;
	}

	private EObject library(String name) {
		EObject created = EcoreUtil.create(libraryClass);
		created.eSet(nameAttribute, name);
		return created;
	}

	private Path writeFixture(String fileName, EObject... roots) throws IOException {
		return writeFixtureAt(tempDir, fileName, roots);
	}

	private Path writeFixtureAt(Path dir, String fileName, EObject... roots) throws IOException {
		Files.createDirectories(dir);
		Path file = dir.resolve(fileName);
		ResourceSet resourceSet = resourceSet();
		Resource resource = resourceSet.createResource(URI.createFileURI(file.toAbsolutePath().toString()));
		resource.getContents().addAll(List.of(roots));
		resource.save(Map.of());
		return file;
	}

	private FileEObjectProvider provider(List<Path> locations) {
		return new FileEObjectProvider("files", resourceSet(), locations, FileEObjectProvider.uriFragmentKeys());
	}

	@Test
	public void testSingleFileWithMultipleRoots() throws Exception {
		Path file = writeFixture("libs.xmi", library("central"), library("annex"));

		provider(List.of(file)).load(writer).join();

		var entries = writer.getRegistry().entries();
		assertThat(entries).hasSize(2);
		assertThat(entries).allSatisfy(entry -> {
			assertThat(entry.key()).startsWith("libs.xmi#");
			assertThat(entry.source()).isEqualTo("files");
			assertThat(entry.properties()).containsEntry(FileEObjectProvider.PROP_NS_URI, NS_URI)
					.containsEntry(FileEObjectProvider.PROP_FILE_LOCATION, file.toAbsolutePath().toString());
		});
	}

	@Test
	public void testDirectoryIsWalkedRecursively() throws Exception {
		writeFixture("a.xmi", library("a"));
		writeFixtureAt(tempDir.resolve("nested"), "b.xmi", library("b"));

		provider(List.of(tempDir)).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(2);
	}

	@Test
	public void testFeatureDerivedKeys() throws Exception {
		Path file = writeFixture("libs.xmi", library("central"), library("annex"));

		new FileEObjectProvider("files", resourceSet(), List.of(file), FileEObjectProvider.featureKeys("name"))
				.load(writer).join();

		assertThat(writer.getRegistry().get("central")).isPresent();
		assertThat(writer.getRegistry().get("annex")).isPresent();
	}

	@Test
	public void testObjectWithoutKeyIsSkippedOthersLoad() throws Exception {
		Path file = writeFixture("libs.xmi", library("central"), library(null));

		new FileEObjectProvider("files", resourceSet(), List.of(file), FileEObjectProvider.featureKeys("name"))
				.load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
		assertThat(writer.getRegistry().get("central")).isPresent();
	}

	@Test
	public void testUnknownKeyFeatureSkipsAllObjectsOfThatClass() throws Exception {
		Path file = writeFixture("libs.xmi", library("central"));

		new FileEObjectProvider("files", resourceSet(), List.of(file), FileEObjectProvider.featureKeys("no-such"))
				.load(writer).join();

		assertThat(writer.getRegistry().entries()).isEmpty();
	}

	@Test
	public void testBrokenFileIsSkippedRestLoads() throws Exception {
		writeFixture("good.xmi", library("good"));
		Files.writeString(tempDir.resolve("broken.xmi"), "this is no xmi");

		provider(List.of(tempDir)).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
	}

	@Test
	public void testMissingLocationIsSkipped() throws Exception {
		Path file = writeFixture("libs.xmi", library("central"));

		provider(List.of(tempDir.resolve("does-not-exist"), file)).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
	}

	@Test
	public void testEmptyLocationsAreAValidEmptyState() {
		provider(List.of()).load(writer).join();
		assertThat(writer.getRegistry().entries()).isEmpty();
	}

	@Test
	public void testReloadSwapsAgainstThePreviousState() throws Exception {
		Path keep = writeFixture("keep.xmi", library("keep"));
		Path gone = writeFixture("gone.xmi", library("gone"));
		provider(List.of(tempDir)).load(writer).join();
		assertThat(writer.getRegistry().entries()).hasSize(2);

		RecordingListener listener = new RecordingListener();
		writer.getRegistry().addListener(listener);
		listener.events.clear();

		Files.delete(gone);
		// a re-load uses a fresh ResourceSet, like the OSGi component does
		provider(List.of(tempDir)).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
		assertThat(writer.getRegistry().getEntry("keep.xmi#/")).isPresent();
		assertThat(listener.removed).extracting(EObjectRegistryEntry::key).containsExactly("gone.xmi#/");
		assertThat(keep).exists();
	}
}
