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
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
		tmp.delete();
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
