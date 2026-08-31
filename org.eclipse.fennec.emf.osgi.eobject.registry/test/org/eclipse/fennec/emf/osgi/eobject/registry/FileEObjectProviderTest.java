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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The file provider against real XMI fixtures: key strategies, directory walking, the extension
 * allow-list that keeps placeholder files out of the load, broken-file resilience and the
 * sync-based re-load swap.
 */
public class FileEObjectProviderTest {

	private static final String NS_URI = "http://fennec.eclipse.org/test/library/1.0";

	@TempDir
	Path tempDir;

	private EPackage libraryPackage;
	private EClass libraryClass;
	private EAttribute nameAttribute;
	private EObjectRegistryWriter writer;
	private List<LogRecord> logRecords;
	private Handler logCollector;
	private Logger providerLogger;

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

		logRecords = new ArrayList<>();
		logCollector = new Handler() {

			@Override
			public void publish(LogRecord record) {
				logRecords.add(record);
			}

			@Override
			public void flush() {
				// nothing to do
			}

			@Override
			public void close() {
				// nothing to do
			}
		};
		providerLogger = Logger.getLogger(FileEObjectProvider.class.getName());
		providerLogger.addHandler(logCollector);
	}

	@AfterEach
	public void tearDown() {
		providerLogger.removeHandler(logCollector);
	}

	private List<LogRecord> warnings() {
		return logRecords.stream().filter(record -> record.getLevel().intValue() >= Level.WARNING.intValue()).toList();
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

	private FileEObjectProvider provider(List<Path> locations, List<String> fileExtensions) {
		return new FileEObjectProvider("files", resourceSet(), locations, FileEObjectProvider.uriFragmentKeys(),
				fileExtensions);
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
		assertThat(warnings()).as("a file with a model extension that fails to parse must stay visible").hasSize(1);
		assertThat(warnings().get(0).getMessage()).contains("broken.xmi");
	}

	@Test
	public void testPlaceholderAndHousekeepingFilesAreSkippedSilently() throws Exception {
		writeFixture("libs.xmi", library("central"));
		Files.writeString(tempDir.resolve(".keep"), "");
		Files.writeString(tempDir.resolve(".gitignore"), "*.tmp");
		Files.writeString(tempDir.resolve(".DS_Store"), "binary junk");
		Files.writeString(tempDir.resolve(".libs.xmi.swp"), "vim swap");
		Files.writeString(tempDir.resolve("README.md"), "# how these models are maintained");
		Files.writeString(tempDir.resolve("notes.txt"), "not a model");

		provider(List.of(tempDir)).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
		assertThat(warnings()).as("placeholder and housekeeping files must not be reported as broken models")
				.isEmpty();
	}

	@Test
	public void testExtensionsAreMatchedCaseInsensitively() throws Exception {
		writeFixture("LIBS.XMI", library("central"));

		provider(List.of(tempDir)).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
	}

	@Test
	public void testConfiguredExtensionsReplaceTheDefaults() throws Exception {
		writeFixture("mapping.conf", library("configured"));
		writeFixture("libs.xmi", library("central"));

		// a leading dot in the configured entry is tolerated
		provider(List.of(tempDir), List.of(".conf")).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
		assertThat(writer.getRegistry().getEntry("mapping.conf#/")).isPresent();
		assertThat(warnings()).isEmpty();
	}

	@Test
	public void testEmptyExtensionListAttemptsEveryFileButStillSkipsDotfiles() throws Exception {
		writeFixture("mapping.conf", library("configured"));
		Files.writeString(tempDir.resolve(".keep"), "");

		provider(List.of(tempDir), List.of()).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
		assertThat(writer.getRegistry().getEntry("mapping.conf#/")).isPresent();
		assertThat(warnings()).as("a dotfile is never model content, whatever the allow-list says").isEmpty();
	}

	@Test
	public void testDirectlyNamedFileBypassesTheExtensionFilter() throws Exception {
		Path file = writeFixture("mapping.conf", library("configured"));

		provider(List.of(file)).load(writer).join();

		assertThat(writer.getRegistry().entries()).hasSize(1);
		assertThat(writer.getRegistry().getEntry("mapping.conf#/")).isPresent();
	}

	@Test
	public void testDirectlyNamedDotfileIsStillLoaded() throws Exception {
		Path file = writeFixture(".hidden.xmi", library("hidden"));

		provider(List.of(file)).load(writer).join();

		assertThat(writer.getRegistry().entries())
				.as("naming a file explicitly is deliberate and must not be second-guessed").hasSize(1);
	}

	@Test
	public void testFileWithoutExtensionIsSkippedByTheAllowList() throws Exception {
		writeFixture("Makefile", library("nope"));

		provider(List.of(tempDir)).load(writer).join();

		assertThat(writer.getRegistry().entries()).isEmpty();
		assertThat(warnings()).isEmpty();
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
