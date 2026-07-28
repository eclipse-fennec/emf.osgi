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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.emf.ecore.EPackage;

/**
 * One versioned canonicalization algorithm behind {@link DefaultFingerprintService}.
 * <p>
 * A fingerprint value is {@code <tag>:<digest>}; the tag versions the <em>algorithm</em>,
 * not the model. This seam is what makes several tags computable at the same time — the
 * service addresses schemes by tag, so a scheme bump adds an implementation instead of
 * editing one.
 * <p>
 * <b>A published scheme is frozen.</b> Each implementation owns its algorithm end to end
 * and must never share mutable canonicalization logic with another scheme: a helper
 * refactored for a newer scheme would silently change the older scheme's values. Later
 * schemes copy what they need rather than reuse it.
 * <p>
 * Deliberately package-private. The exported surface is the tag-addressed entry point on
 * {@link org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService}; which implementations
 * sit behind it is an internal matter.
 *
 * @author Mark Hoffmann
 */
interface CanonicalizationScheme {

    /**
     * The scheme tag that prefixes every value this scheme produces, e.g. {@code "fp1"}.
     *
     * @return the stable, non-{@code null} tag
     */
    String tag();

    /**
     * Builds the canonical textual form that {@link #digest(EPackage, String...)} hashes.
     * <p>
     * Exposed because it is the diagnostic that answers "why do these two models hash
     * differently?" — diffing the canonical forms shows it directly.
     *
     * @param ePackage the model version to canonicalize; never {@code null}
     * @param derivationInputs optional canonical input tokens (may be empty or {@code null})
     * @return the canonical form, never {@code null}
     */
    String canonicalForm(EPackage ePackage, String... derivationInputs);

    /**
     * The digest part of the fingerprint value, without the tag or its separator.
     * <p>
     * Defaults to SHA-256 over the canonical form. A future scheme that also changes the
     * hash function overrides this — the tag versions the whole algorithm, digest included.
     *
     * @param ePackage the model version to fingerprint; never {@code null}
     * @param derivationInputs optional canonical input tokens (may be empty or {@code null})
     * @return the digest, never {@code null}
     */
    default String digest(EPackage ePackage, String... derivationInputs) {
        return sha256Hex(canonicalForm(ePackage, derivationInputs));
    }

    /**
     * SHA-256 of the UTF-8 bytes of {@code content}, as lowercase hex.
     *
     * @param content the text to hash
     * @return the lowercase hex digest
     */
    static String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandated algorithm on every JVM.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
