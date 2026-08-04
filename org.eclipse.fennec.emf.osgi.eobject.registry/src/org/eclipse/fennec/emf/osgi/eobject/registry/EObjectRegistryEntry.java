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

import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

/**
 * One piece of content in an {@link EObjectRegistry}.
 * <p>
 * The {@code key} is unique per registry; its convention is per content domain - an
 * EClass URI for JPA/eORM content, a mapping id ({@code mid}) for sensinact provider
 * mappings, a library name for OCL libraries. The registry is a
 * {@code Map<String, EObject>} at heart.
 * <p>
 * <b>{@code source} is the technical origin</b> - the input channel that wrote the
 * entry, i.e. the provider instance name (e.g. {@code my-files}, {@code atlas-jena}).
 * It scopes {@link EObjectRegistryWriter#sync(String, java.util.Collection)}. It is
 * <b>not</b> the model origin: where the content's <em>model</em> comes from belongs
 * into the entry {@code properties}, under the existing conventions {@code emf.nsURI}
 * and {@code emf.fingerprint}. Example - a sensinact mapping fetched from a model
 * atlas: {@code key} = the mapping's {@code mid}, {@code source} = {@code atlas-jena},
 * {@code properties} = {@code emf.nsURI}, {@code emf.fingerprint},
 * {@code atlas.scope}, {@code atlas.registry}, {@code atlas.object.id}.
 *
 * @param key        unique key of the entry within its registry; never {@code null}
 * @param object     the content; never {@code null}
 * @param source     the input channel that wrote the entry; never {@code null}
 * @param properties transient context of the entry (model anchoring, provenance);
 *                   never {@code null}, immutable
 * @author Data In Motion Consulting
 */
public record EObjectRegistryEntry(String key, EObject object, String source, Map<String, Object> properties) {

	public EObjectRegistryEntry {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(object, "object");
		Objects.requireNonNull(source, "source");
		properties = properties == null ? Map.of() : Map.copyOf(properties);
	}

	/**
	 * Creates an entry without properties.
	 *
	 * @param key    unique key of the entry within its registry
	 * @param object the content
	 * @param source the input channel that writes the entry
	 * @return the entry
	 */
	public static EObjectRegistryEntry of(String key, EObject object, String source) {
		return new EObjectRegistryEntry(key, object, source, Map.of());
	}
}
