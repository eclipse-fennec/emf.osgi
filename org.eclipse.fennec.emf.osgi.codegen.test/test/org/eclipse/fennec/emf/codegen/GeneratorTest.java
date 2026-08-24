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
package org.eclipse.fennec.emf.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.fennec.emf.osgi.codegen.FennecEmfGenerator;
import org.eclipse.fennec.emf.osgi.codegen.FennecEmfGenerator.GeneratorOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.generate.BuildContext;
import aQute.lib.io.IO;


/**
 * 
 * @author Jürgen Albert
 * @since 14 Jan 2021
 */
class GeneratorTest {
	
	File tmp;
	
	@BeforeEach
	public void beforeEach(TestInfo testInfo) {
		tmp = new File("generated/test/" + testInfo.getDisplayName());
		IO.delete(tmp);
		tmp.mkdirs();
	}
	
	@Test
	void testGeneratorUml() throws Exception {
		try (Workspace workspace = getWorkspace("test-resources/ws-1")) {
			Project project = workspace.getProject("org.w3.rdf.model");
			assertThat(project).isNotNull();
			project.verifyDependencies(false);
			assertThat(project.getErrors()).isEmpty();
			Map<String, String> attrs = new HashMap<>();
			attrs.put("generate", "fennecEMF");
			attrs.put("genmodel", "model/rdf.genmodel");
			attrs.put("output", "src");
			BuildContext bc = new BuildContext(project, attrs, Collections.emptyList(), System.in, System.out, System.err);
			FennecEmfGenerator generator = new FennecEmfGenerator();
			generator.generate(bc, new GeneratorOptions() {
				
				@Override
				public Map<String, String> _properties() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public List<String> _arguments() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public Optional<File> output() {
					// TODO Auto-generated method stub
					return Optional.empty();
				}
			});
			File file = project.getFile("src/org/w3/rdfs/RdfsPackage.java");
			assertThat(file).exists();
		};
	}

	@Test
	void testGeneratorBasic() throws Exception {
		try (Workspace workspace = getWorkspace("test-resources/ws-2")) {
			Project project = workspace.getProject("org.eclipse.fennec.emf.osgi.example.model.basic");
			assertThat(project).isNotNull();
			project.verifyDependencies(false);
			assertThat(project.getErrors()).isEmpty();
			Map<String, String> attrs = new HashMap<>();
			attrs.put("generate", "fennecEMF");
			attrs.put("genmodel", "other/main/resources/model/basic.genmodel");
			attrs.put("output", "src-gen");
			BuildContext bc = new BuildContext(project, attrs, Collections.emptyList(), System.in, System.out, System.err);
			FennecEmfGenerator generator = new FennecEmfGenerator();
			generator.generate(bc, new GeneratorOptions() {
				
				@Override
				public Map<String, String> _properties() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public List<String> _arguments() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public Optional<File> output() {
					// TODO Auto-generated method stub
					return Optional.empty();
				}
			});
			File file = project.getFile("src-gen/org/gecko/emf/osgi/example/model/basic/util/BasicResourceImpl.java");
			assertThat(file).exists();
		};
	}
	
	@Test
	void testGeneratorLineEndingsLf() throws Exception {
		String content = generateBasicModelWithLineEndings("lf", null);
		assertThat(content).contains("\n").doesNotContain("\r");
	}

	@Test
	void testGeneratorLineEndingsCrLf() throws Exception {
		String content = generateBasicModelWithLineEndings("crlf", null);
		assertThat(content).contains("\r\n");
		assertThat(content.replace("\r\n", "")).doesNotContain("\n").doesNotContain("\r");
	}

	@Test
	void testGeneratorLineEndingsFromEclipsePrefs() throws Exception {
		String content = generateBasicModelWithLineEndings(null, "\\n");
		assertThat(content).contains("\n").doesNotContain("\r");
	}

	@Test
	void testGeneratorLineEndingsAttributeWinsOverEclipsePrefs() throws Exception {
		String content = generateBasicModelWithLineEndings("crlf", "\\n");
		assertThat(content).contains("\r\n");
		assertThat(content.replace("\r\n", "")).doesNotContain("\n").doesNotContain("\r");
	}

	@Test
	void testGeneratorLineEndingsInvalid() throws Exception {
		try (Workspace workspace = getWorkspace("test-resources/ws-2")) {
			Project project = workspace.getProject("org.eclipse.fennec.emf.osgi.example.model.basic");
			assertThat(project).isNotNull();
			project.verifyDependencies(false);
			assertThat(project.getErrors()).isEmpty();
			Map<String, String> attrs = new HashMap<>();
			attrs.put("generate", "fennecEMF");
			attrs.put("genmodel", "other/main/resources/model/basic.genmodel");
			attrs.put("output", "src-gen");
			attrs.put("lineEndings", "mixed");
			BuildContext bc = new BuildContext(project, attrs, Collections.emptyList(), System.in, System.out, System.err);
			Optional<String> result = new FennecEmfGenerator().generate(bc, emptyOptions());
			assertThat(result).isPresent();
			assertThat(result.get()).contains("lineEndings");
		}
	}

	@Test
	void testGeneratorUsedGenPackagesOfMultiPackageBundle() throws Exception {
		IO.copy(new File("test-resources/ws-3"), tmp);
		createMultiPackageModelJar(new File(tmp, "org.fennec.test.consumer/lib/org.fennec.test.multi.model.jar"));
		try (Workspace workspace = new Workspace(tmp)) {
			Project project = workspace.getProject("org.fennec.test.consumer");
			assertThat(project).isNotNull();
			project.verifyDependencies(false);
			assertThat(project.getErrors()).isEmpty();
			Map<String, String> attrs = new HashMap<>();
			attrs.put("generate", "fennecEMF");
			attrs.put("genmodel", "model/consumer.genmodel");
			attrs.put("output", "src-gen");
			BuildContext bc = new BuildContext(project, attrs, Collections.emptyList(), System.in, System.out, System.err);
			Optional<String> result = new FennecEmfGenerator().generate(bc, emptyOptions());
			assertThat(result).isEmpty();
			File file = project.getFile("src-gen/org/fennec/test/consumer/ConsumerObject.java");
			assertThat(file).exists();
			assertThat(Files.readString(file.toPath())).contains("org.fennec.test.multi.packb.BObject");
		}
	}

	@Test
	void testGeneratorFailsOnInvalidGenmodel() throws Exception {
		IO.copy(new File("test-resources/ws-3"), tmp);
		createMultiPackageModelJar(new File(tmp, "org.fennec.test.consumer/lib/org.fennec.test.multi.model.jar"));
		try (Workspace workspace = new Workspace(tmp)) {
			Project project = workspace.getProject("org.fennec.test.consumer");
			assertThat(project).isNotNull();
			project.verifyDependencies(false);
			assertThat(project.getErrors()).isEmpty();
			Map<String, String> attrs = new HashMap<>();
			attrs.put("generate", "fennecEMF");
			attrs.put("genmodel", "model/broken.genmodel");
			attrs.put("output", "src-gen");
			BuildContext bc = new BuildContext(project, attrs, Collections.emptyList(), System.in, System.out, System.err);
			Optional<String> result = new FennecEmfGenerator().generate(bc, emptyOptions());
			assertThat(result).isPresent();
			assertThat(result.get()).contains("model/broken.genmodel").contains("invalid");
		}
	}

	/**
	 * Builds the multi-package model bundle referenced from the ws-3 consumer's buildpath:
	 * one jar carrying two EPackages, providing one generated_package capability per EPackage,
	 * where only the second EPackage is referenced via usedGenPackages (issue #87).
	 */
	private void createMultiPackageModelJar(File target) throws Exception {
		Manifest manifest = new Manifest();
		Attributes main = manifest.getMainAttributes();
		main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		main.putValue("Bundle-ManifestVersion", "2");
		main.putValue("Bundle-SymbolicName", "org.fennec.test.multi.model");
		main.putValue("Bundle-Version", "1.0.0");
		main.putValue("Provide-Capability",
				"org.eclipse.emf.ecore.generated_package;"
						+ "uri=\"http://fennec.eclipse.org/test/multi/a\";"
						+ "class=\"org.fennec.test.multi.packa.PackaPackage\";"
						+ "genModel=\"/model/multi.genmodel\";ecore=\"/model/a.ecore\","
						+ "org.eclipse.emf.ecore.generated_package;"
						+ "uri=\"http://fennec.eclipse.org/test/multi/b\";"
						+ "class=\"org.fennec.test.multi.packb.PackbPackage\";"
						+ "genModel=\"/model/multi.genmodel\";ecore=\"/model/b.ecore\"");
		target.getParentFile().mkdirs();
		File modelDir = new File("test-resources/multi-model");
		try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(target), manifest)) {
			for (String name : List.of("a.ecore", "b.ecore", "multi.genmodel")) {
				jar.putNextEntry(new ZipEntry("model/" + name));
				jar.write(Files.readAllBytes(new File(modelDir, name).toPath()));
				jar.closeEntry();
			}
		}
	}

	private String generateBasicModelWithLineEndings(String lineEndings, String prefsLineSeparator) throws Exception {
		try (Workspace workspace = getWorkspace("test-resources/ws-2")) {
			Project project = workspace.getProject("org.eclipse.fennec.emf.osgi.example.model.basic");
			assertThat(project).isNotNull();
			project.verifyDependencies(false);
			assertThat(project.getErrors()).isEmpty();
			if (prefsLineSeparator != null) {
				File prefsFile = project.getFile(".settings/org.eclipse.core.runtime.prefs");
				prefsFile.getParentFile().mkdirs();
				Files.writeString(prefsFile.toPath(),
						"eclipse.preferences.version=1\nline.separator=" + prefsLineSeparator + "\n");
			}
			Map<String, String> attrs = new HashMap<>();
			attrs.put("generate", "fennecEMF");
			attrs.put("genmodel", "other/main/resources/model/basic.genmodel");
			attrs.put("output", "src-gen");
			if (lineEndings != null) {
				attrs.put("lineEndings", lineEndings);
			}
			BuildContext bc = new BuildContext(project, attrs, Collections.emptyList(), System.in, System.out, System.err);
			Optional<String> result = new FennecEmfGenerator().generate(bc, emptyOptions());
			assertThat(result).isEmpty();
			File file = project.getFile("src-gen/org/gecko/emf/osgi/example/model/basic/util/BasicResourceImpl.java");
			assertThat(file).exists();
			return Files.readString(file.toPath());
		}
	}

	private GeneratorOptions emptyOptions() {
		return new GeneratorOptions() {

			@Override
			public Map<String, String> _properties() {
				return null;
			}

			@Override
			public List<String> _arguments() {
				return null;
			}

			@Override
			public Optional<File> output() {
				return Optional.empty();
			}
		};
	}

	private Workspace getWorkspace(File file) throws Exception {
		IO.copy(file, tmp);
		return new Workspace(tmp);
	}

	private Workspace getWorkspace(String dir) throws Exception {
		return getWorkspace(new File(dir));
	}
}
