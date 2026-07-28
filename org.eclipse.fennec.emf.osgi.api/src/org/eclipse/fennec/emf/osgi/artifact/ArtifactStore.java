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
package org.eclipse.fennec.emf.osgi.artifact;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;

/**
 * Durable storage for derived artifacts (aspects/profiles), keyed by the fingerprint
 * of the model version they were derived from (see {@link FingerprintService}) plus an
 * artifact type id.
 * <p>
 * This is the storage-delegate the metadata service uses to persist a derived artifact
 * once and resolve it again on package re-registration, instead of rebuilding it. An
 * in-memory implementation is the default; a Model-Atlas-backed implementation stores
 * the artifacts in an EObject registry.
 * <p>
 * Implementations are <b>copy-isolating</b>: {@link #put} stores an independent copy and
 * {@link #resolve} returns an independent copy, so mutating an artifact handed to or
 * returned from the store never corrupts the stored state.
 *
 * @author Mark Hoffmann
 */
public interface ArtifactStore {

    /**
     * Resolves the artifact stored under the given fingerprint and type id.
     *
     * @param fingerprint the model-version fingerprint (see {@link FingerprintService})
     * @param typeId the artifact type id (e.g. the {@code AspectProvider} type id)
     * @return a copy of the stored artifact, or {@link Optional#empty()} if none is stored
     *         under exactly this {@code (fingerprint, typeId)} pair, or if either argument
     *         is {@code null}
     */
    Optional<EObject> resolve(String fingerprint, String typeId);

    /**
     * Stores (a copy of) the artifact under the given fingerprint and type id, replacing
     * any artifact previously stored under the same {@code (fingerprint, typeId)} pair.
     * A {@code null} argument is ignored (no-op).
     *
     * @param fingerprint the model-version fingerprint
     * @param typeId the artifact type id
     * @param artifact the artifact to store
     */
    void put(String fingerprint, String typeId, EObject artifact);
}
