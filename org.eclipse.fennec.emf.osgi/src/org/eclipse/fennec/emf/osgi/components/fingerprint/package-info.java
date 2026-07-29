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
/**
 * Internal fingerprint implementation: the tag-addressed canonicalization schemes, the
 * default {@link org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService} facade and
 * the in-memory {@link org.eclipse.fennec.emf.osgi.artifact.ArtifactStore}.
 * <p>
 * Deliberately <b>not exported</b> — the canonicalization seam stays internal. Ported verbatim from
 * {@code org.eclipse.fennec.model.metadata.service}.
 */
@org.osgi.annotation.versioning.Version("1.0.0")
package org.eclipse.fennec.emf.osgi.components.fingerprint;
