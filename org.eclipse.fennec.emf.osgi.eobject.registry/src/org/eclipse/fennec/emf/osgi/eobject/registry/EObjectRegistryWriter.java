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
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The write face of a named EObject registry. Content sources use it; consumers never
 * see it (the read/write split follows the {@code MetadataService}/
 * {@code MetadataWhiteboard} precedent).
 * <p>
 * In OSGi the writer is published alongside the {@link EObjectRegistry} on the same
 * instance, carrying the same
 * {@link EObjectRegistryConstants#EMF_EOBJECT_REGISTRY_NAME} property. Dynamic sources
 * reference it by registry name and push - and because the service only exists after
 * the registry's initial load, a source can never write into a half-initialized
 * registry. A source that dies simply leaves its content in place.
 * <p>
 * <b>Source discipline:</b> every write carries the source's name (its input channel,
 * e.g. {@code my-files}, {@code atlas-jena}). {@link #sync(String, Collection)} and
 * {@link #remove(String, String)} only ever remove entries <em>owned by that source</em>;
 * a {@link #put(String, String, EObject, Map)} on a key owned by another source
 * replaces the entry and adopts the new source (last write wins, logged).
 *
 * @author Data In Motion Consulting
 */
@ProviderType
public interface EObjectRegistryWriter {

	/**
	 * Returns the read face of this registry.
	 *
	 * @return the registry; never {@code null}
	 */
	EObjectRegistry getRegistry();

	/**
	 * Puts one entry. An existing entry under the same key is replaced; if the new
	 * object is the identical instance with equal properties and the same source, the
	 * call is a no-op and no event fires.
	 *
	 * @param source     the writing source's name; must not be {@code null}
	 * @param key        the entry key, unique per registry; must not be {@code null}
	 * @param object     the content; must not be {@code null}
	 * @param properties transient entry context (model anchoring, provenance); may be
	 *                   {@code null} for none
	 */
	void put(String source, String key, EObject object, Map<String, Object> properties);

	/**
	 * Removes the entry under the given key, if it is owned by the given source. A key
	 * owned by a different source is left untouched (logged); an unknown key is a no-op.
	 *
	 * @param source the removing source's name; must not be {@code null}
	 * @param key    the entry key; must not be {@code null}
	 */
	void remove(String source, String key);

	/**
	 * Bulk synchronization: "this is my complete current state". The swap semantics
	 * live here, once, so no source ever reimplements them:
	 * <ul>
	 * <li><b>identity compare</b> - an incoming entry whose object is the identical
	 * instance with equal properties is a no-op (a remote client's ETag cache returns
	 * the identical instance while the content is unchanged);</li>
	 * <li><b>update before remove</b> - additions and updates are applied and delivered
	 * before any removal of this pass;</li>
	 * <li><b>per-source removal</b> - entries owned by this source that are not part of
	 * the passed state are removed; entries of other sources are never touched.</li>
	 * </ul>
	 * A source that fails transiently simply does not call {@code sync} - the existing
	 * content stays.
	 *
	 * @param source  the syncing source's name; must not be {@code null}
	 * @param entries the source's complete current state; every entry's
	 *                {@link EObjectRegistryEntry#source()} must equal {@code source}
	 * @throws IllegalArgumentException if an entry carries a different source
	 */
	void sync(String source, Collection<EObjectRegistryEntry> entries);
}
