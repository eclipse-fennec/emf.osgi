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
package org.eclipse.fennec.emf.osgi.fingerprint;

import java.util.Set;

import org.eclipse.emf.ecore.EPackage;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Computes a canonical, content-derived fingerprint of a model version.
 * <p>
 * The fingerprint keys derived artifacts (aspects/profiles) so they can be stored
 * once, reused on re-registration, and replicated across systems. It has three
 * guaranteed properties:
 * <ul>
 *   <li><b>Reproducible</b> — the same {@link EPackage} content plus the same
 *       derivation inputs yields the same value on every node, without coordination
 *       and independent of object identity, {@code hashCode}, registration order,
 *       serialization order/whitespace or wall-clock time.</li>
 *   <li><b>Identifying</b> — structurally different content (added/removed/renamed
 *       classifiers, features, operations; changed types; changed log-relevant
 *       annotations) yields a different value; the nsURI alone is never the key.</li>
 *   <li><b>Canonical</b> — semantically irrelevant differences (e.g. the order in
 *       which classifiers appear in the package, or GenModel {@code documentation}
 *       annotations) do not affect the value.</li>
 * </ul>
 *
 * <h2>Schemes</h2>
 * A value is {@code <scheme>:<digest>}, e.g. {@code fp1:9f86d0…}. The scheme tag versions
 * the <em>canonicalization algorithm</em>, not the model — two values with different tags
 * are not comparable, even for the same model. An implementation may keep several schemes
 * computable at the same time so that a scheme bump does not invalidate what is already
 * computable: {@link #currentScheme()} names the one used for new values,
 * {@link #supportedSchemes()} lists all of them, and
 * {@link #fingerprintInScheme(String, EPackage, String...)} addresses one explicitly.
 * <p>
 * Consumers that merely resolve a value they read elsewhere need none of this — they pass
 * it to {@code MetadataService.getPackageMetadataByFingerprint} and react to the result.
 *
 * @author Mark Hoffmann
 */
@ProviderType
public interface FingerprintService {

    /**
     * Computes the canonical fingerprint of the given {@link EPackage} together with
     * the (optional) declared derivation inputs.
     * <p>
     * {@code derivationInputs} are opaque, already-canonical tokens (for example
     * {@code "oclEngine=1.2.0"} or {@code "eormConfig=<hash>"}) that are folded into
     * the fingerprint so that a change of a derivation input yields a different
     * fingerprint even for identical Ecore. The order of the tokens is not
     * significant; {@code null} tokens are ignored.
     *
     * @param ePackage the model version to fingerprint; may be {@code null}
     * @param derivationInputs optional canonical input tokens (may be empty or {@code null})
     * @return the fingerprint in the {@linkplain #currentScheme() current scheme}, or
     *         {@code null} if {@code ePackage} is {@code null}
     */
    String fingerprint(EPackage ePackage, String... derivationInputs);

    /**
     * The scheme tag {@link #fingerprint(EPackage, String...)} produces values in.
     *
     * @return the current scheme tag, e.g. {@code "fp1"}; never {@code null}
     */
    String currentScheme();

    /**
     * All scheme tags this implementation can compute, including the current one.
     * <p>
     * Use this to decide whether a value read from elsewhere can be recomputed at all:
     * a tag that is absent here is not computable, and no amount of retrying helps.
     *
     * @return the supported scheme tags; never {@code null}, never empty
     */
    Set<String> supportedSchemes();

    /**
     * Computes the fingerprint in an explicitly named scheme instead of the current one.
     * <p>
     * Deliberately <em>not</em> an overload of {@link #fingerprint(EPackage, String...)}:
     * with the scheme in second position, {@code fingerprint(pkg, "oclEngine=1.2.0")}
     * would silently bind to the scheme overload and read the derivation input as a scheme
     * tag. The scheme comes first so that no existing call can drift.
     *
     * @param scheme the scheme tag to compute in; must be one of {@link #supportedSchemes()}
     * @param ePackage the model version to fingerprint; may be {@code null}
     * @param derivationInputs optional canonical input tokens (may be empty or {@code null})
     * @return the fingerprint in {@code scheme}, or {@code null} if {@code ePackage} is
     *         {@code null}
     * @throws IllegalArgumentException if {@code scheme} is {@code null} or not supported —
     *         an unknown scheme is a caller error, not a data condition; guard with
     *         {@link #supportedSchemes()} when the tag comes from data
     */
    String fingerprintInScheme(String scheme, EPackage ePackage, String... derivationInputs);
}
