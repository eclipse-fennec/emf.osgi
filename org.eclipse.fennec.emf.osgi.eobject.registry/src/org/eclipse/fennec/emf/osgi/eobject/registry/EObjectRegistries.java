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

import java.util.Objects;
import java.util.concurrent.CompletionException;

import org.eclipse.fennec.emf.osgi.eobject.registry.impl.EObjectRegistryImpl;

/**
 * Bootstrap for using EObject registries without OSGi, analogous to
 * {@code MetadataServices}: the core works on a flat classpath, the OSGi layer (factory
 * component, listener whiteboard, gated service publication) only adds wiring on top.
 * <p>
 * The factory hands out the {@link EObjectRegistryWriter}; the read face is reached via
 * {@link EObjectRegistryWriter#getRegistry()}. On a flat classpath the caller owns both
 * faces anyway - the read/write discipline matters where the faces are handed to
 * different parties, which is what the OSGi layer does.
 *
 * @author Data In Motion Consulting
 */
public final class EObjectRegistries {

	/**
	 * Creates an empty registry.
	 *
	 * @param name the registry name; must not be {@code null}
	 * @return the registry's write face; the read face via
	 *         {@link EObjectRegistryWriter#getRegistry()}
	 */
	public static EObjectRegistryWriter createRegistry(String name) {
		Objects.requireNonNull(name, "name");
		return new EObjectRegistryImpl(name);
	}

	/**
	 * Creates a registry and runs the given initial provider synchronously - the
	 * non-OSGi equivalent of the gated service publication: when this method returns,
	 * the registry holds the provider's complete initial content.
	 *
	 * @param name            the registry name; must not be {@code null}
	 * @param initialProvider the provider to load; must not be {@code null}
	 * @return the registry's write face
	 * @throws CompletionException if the initial load fails
	 */
	public static EObjectRegistryWriter createRegistry(String name, EObjectProvider initialProvider) {
		Objects.requireNonNull(initialProvider, "initialProvider");
		EObjectRegistryWriter writer = createRegistry(name);
		initialProvider.load(writer).join();
		return writer;
	}

	private EObjectRegistries() {
	}
}
