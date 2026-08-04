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

import java.util.Collection;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The read face of a named EObject registry: a local, string-keyed store of authored
 * EObject content (provider mappings, mapping profiles, OCL libraries, ...) that stays
 * available even when the content's remote source is not.
 * <p>
 * In OSGi one registry instance exists per content domain, created from a factory
 * configuration and published under this interface with the
 * {@link EObjectRegistryConstants#EMF_EOBJECT_REGISTRY_NAME} service property.
 * Consumers reference the registry by name; the service only appears after the
 * registry's initial content is loaded, so a consumer never observes a half-loaded
 * registry. Content sources use the {@link EObjectRegistryWriter} face instead.
 * <p>
 * Content changes are delivered through {@link EObjectRegistryListener}s - there are
 * no per-object OSGi services and therefore no ServiceTracker to lean on.
 *
 * @author Data In Motion Consulting
 */
@ProviderType
public interface EObjectRegistry {

	/**
	 * Returns the stable, human-chosen name of this registry instance.
	 *
	 * @return the registry name; never {@code null}
	 */
	String getName();

	/**
	 * Returns the content for the given key.
	 *
	 * @param key the entry key; {@code null} yields an empty result
	 * @return the object, or empty if the key is unknown
	 */
	Optional<EObject> get(String key);

	/**
	 * Returns the entry for the given key, including its source and properties.
	 *
	 * @param key the entry key; {@code null} yields an empty result
	 * @return the entry, or empty if the key is unknown
	 */
	Optional<EObjectRegistryEntry> getEntry(String key);

	/**
	 * Returns a snapshot of all entries, in insertion order.
	 *
	 * @return an immutable snapshot; never {@code null}
	 */
	Collection<EObjectRegistryEntry> entries();

	/**
	 * Adds a listener. The current content is replayed to the listener first - it sees
	 * every existing entry as {@link EObjectRegistryListener#entryAdded(EObjectRegistryEntry)}
	 * before any live event, with no gap in between.
	 *
	 * @param listener the listener; must not be {@code null}
	 */
	void addListener(EObjectRegistryListener listener);

	/**
	 * Removes a listener. Unknown listeners are ignored.
	 *
	 * @param listener the listener; must not be {@code null}
	 */
	void removeListener(EObjectRegistryListener listener);
}
