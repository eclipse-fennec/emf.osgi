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
package org.eclipse.fennec.emf.osgi.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.jupiter.api.Test;

/**
 * Plain tests of the nsURI index (issue #113).
 * @author Mark Hoffmann
 * @since 16 Oct 2026
 */
class NsUriIndexTest {

	private static final String NESTED_ECORE = """
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="root" nsURI="http://test/root" nsPrefix="root">
			  <eSubpackages name="sub" nsURI="http://test/root/sub" nsPrefix="sub"/>
			</ecore:EPackage>
			""";

	@Test
	void testEcoreDeclaresRootAndSubpackages() throws Exception {
		NsUriIndex index = new NsUriIndex();
		try (InputStream in = new ByteArrayInputStream(NESTED_ECORE.getBytes(StandardCharsets.UTF_8))) {
			index.addEcore("org.test.model", "model/root.ecore", in);
		}
		assertThat(index.locations()).containsOnlyKeys("http://test/root", "http://test/root/sub");
		assertThat(index.locations().get("http://test/root/sub").path()).isEqualTo("model/root.ecore");
	}

	@Test
	void testFirstProviderWins() {
		NsUriIndex index = new NsUriIndex();
		index.add("org.test.first", "http://test/model", "/model/model.ecore");
		index.add("org.test.second", "http://test/model", "/model/model.ecore");
		assertThat(index.locations().get("http://test/model").bsn()).isEqualTo("org.test.first");
		assertThat(index.locations().get("http://test/model").path()).isEqualTo("model/model.ecore");
	}

	@Test
	void testEmfCorePackagesAreNotShadowed() {
		// org.eclipse.emf.ecore ships model/Ecore.ecore, the jar scan finds it
		NsUriIndex index = new NsUriIndex();
		index.add("org.eclipse.emf.ecore", EcorePackage.eNS_URI, "model/Ecore.ecore");
		index.add("org.test.model", "http://test/model", "model/model.ecore");
		ResourceSet resourceSet = new ResourceSetImpl();
		index.register(resourceSet, "org.test.consumer");
		// containsKey of the registry asks the global delegate as well, so check the local entries
		assertThat(resourceSet.getPackageRegistry().keySet()).containsExactly("http://test/model");
		assertThat(resourceSet.getPackageRegistry().getEPackage(EcorePackage.eNS_URI)).isSameAs(EcorePackage.eINSTANCE);
	}

}
