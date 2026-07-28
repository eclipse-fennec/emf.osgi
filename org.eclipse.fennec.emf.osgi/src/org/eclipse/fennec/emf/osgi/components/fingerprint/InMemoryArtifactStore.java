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
package org.eclipse.fennec.emf.osgi.components.fingerprint;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.artifact.ArtifactStore;
import org.osgi.service.component.annotations.Component;

/**
 * In-memory {@link ArtifactStore} — the default when no external (e.g. Model-Atlas-backed)
 * store is present.
 * <p>
 * Backed by a {@link ConcurrentHashMap} keyed by an unambiguous, length-prefixed
 * combination of fingerprint and type id. Copy-isolating: {@link #put} stores
 * {@link EcoreUtil#copy(EObject) a copy} and {@link #resolve} returns a copy, so the
 * stored state is immune to caller mutation.
 *
 * @author Mark Hoffmann
 */
@Component
public class InMemoryArtifactStore implements ArtifactStore {

    private final Map<String, EObject> artifacts = new ConcurrentHashMap<>();

    @Override
    public Optional<EObject> resolve(String fingerprint, String typeId) {
        if (fingerprint == null || typeId == null) {
            return Optional.empty();
        }
        EObject stored = artifacts.get(key(fingerprint, typeId));
        return stored == null ? Optional.empty() : Optional.of(EcoreUtil.copy(stored));
    }

    @Override
    public void put(String fingerprint, String typeId, EObject artifact) {
        if (fingerprint == null || typeId == null || artifact == null) {
            return; // null arguments are ignored (defined no-op)
        }
        artifacts.put(key(fingerprint, typeId), EcoreUtil.copy(artifact));
    }

    /** Number of stored artifacts. Package-visible for tests. */
    int size() {
        return artifacts.size();
    }

    /**
     * Length-prefixed composite key: the fingerprint length disambiguates the boundary
     * between fingerprint and type id, so no two distinct pairs can collide regardless of
     * their contents.
     */
    private static String key(String fingerprint, String typeId) {
        return fingerprint.length() + ":" + fingerprint + typeId;
    }
}
