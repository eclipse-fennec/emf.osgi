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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;

/**
 * The non-OSGi bootstrap path: the repo credo "works without OSGi, OSGi sits on top",
 * same split as {@code MetadataServices}.
 */
public class EObjectRegistriesTest {

	private final EObject object = EcoreFactory.eINSTANCE.createEClass();

	@Test
	public void testCreateRegistryValidatesName() {
		assertThatNullPointerException().isThrownBy(() -> EObjectRegistries.createRegistry(null));
	}

	@Test
	public void testCreateEmptyRegistry() {
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("flat");

		assertThat(writer.getRegistry().getName()).isEqualTo("flat");
		assertThat(writer.getRegistry().entries()).isEmpty();
	}

	@Test
	public void testCreateWithInitialProviderLoadsSynchronously() {
		EObjectProvider provider = writer -> {
			writer.sync("init", List.of(EObjectRegistryEntry.of("a", object, "init")));
			return CompletableFuture.completedFuture(null);
		};

		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("flat", provider);

		assertThat(writer.getRegistry().get("a")).contains(object);
	}

	@Test
	public void testFailingInitialProviderSurfaces() {
		EObjectProvider provider = writer -> CompletableFuture.failedFuture(new IOException("no files"));

		assertThatThrownBy(() -> EObjectRegistries.createRegistry("flat", provider))
				.hasCauseInstanceOf(IOException.class);
	}

	@Test
	public void testNullInitialProviderIsRejected() {
		assertThatNullPointerException().isThrownBy(() -> EObjectRegistries.createRegistry("flat", null));
	}
}
