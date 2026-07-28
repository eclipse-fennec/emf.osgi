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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.osgi.service.component.annotations.Component;

/**
 * Default {@link FingerprintService}: a registry of tag-addressed
 * {@link CanonicalizationScheme}s.
 * <p>
 * This class holds no canonicalization logic of its own — it resolves a scheme by tag,
 * delegates, and prefixes the tag onto the digest. That split is what keeps several
 * schemes computable in parallel (issue #17, §2): bumping the scheme means registering
 * another implementation and moving {@link #currentScheme()}, not editing an algorithm
 * whose values are already in circulation.
 * <p>
 * Registered schemes:
 * <ul>
 *   <li>{@link Fp1CanonicalizationScheme} — {@code fp1}, the current scheme.</li>
 * </ul>
 * Stateless and thread-safe: the registry is built once at construction and never mutated,
 * and the schemes themselves hold no state across calls.
 *
 * @author Mark Hoffmann
 */
@Component
public class DefaultFingerprintService implements FingerprintService {

    private final Map<String, CanonicalizationScheme> schemesByTag;
    private final CanonicalizationScheme current;

    /**
     * Creates the service with all built-in schemes registered and {@code fp1} current.
     */
    public DefaultFingerprintService() {
        Map<String, CanonicalizationScheme> schemes = new LinkedHashMap<>();
        register(schemes, new Fp1CanonicalizationScheme());
        this.schemesByTag = Collections.unmodifiableMap(schemes);
        this.current = schemes.get(Fp1CanonicalizationScheme.TAG);
    }

    private static void register(Map<String, CanonicalizationScheme> schemes, CanonicalizationScheme scheme) {
        CanonicalizationScheme clash = schemes.put(scheme.tag(), scheme);
        if (clash != null) {
            // Two schemes under one tag would make values ambiguous — fail at construction,
            // not at the first fingerprint that silently used the wrong algorithm.
            throw new IllegalStateException("Duplicate canonicalization scheme tag: " + scheme.tag());
        }
    }

    @Override
    public String fingerprint(EPackage ePackage, String... derivationInputs) {
        return compute(current, ePackage, derivationInputs);
    }

    @Override
    public String currentScheme() {
        return current.tag();
    }

    @Override
    public Set<String> supportedSchemes() {
        return schemesByTag.keySet();
    }

    @Override
    public String fingerprintInScheme(String scheme, EPackage ePackage, String... derivationInputs) {
        CanonicalizationScheme canonicalizationScheme = schemesByTag.get(scheme);
        if (canonicalizationScheme == null) {
            throw new IllegalArgumentException("Unsupported canonicalization scheme: " + scheme
                    + " (supported: " + supportedSchemes() + ")");
        }
        return compute(canonicalizationScheme, ePackage, derivationInputs);
    }

    /**
     * Returns the canonical form a scheme hashes, for diagnostics: when two model versions
     * hash differently, diffing their canonical forms shows exactly why.
     * <p>
     * Package-private on purpose: the diagnostic is for tests and in-package callers,
     * not a published API promise.
     *
     * @param scheme the scheme tag to canonicalize in
     * @param ePackage the model version; may be {@code null}
     * @param derivationInputs optional canonical input tokens
     * @return the canonical text, or {@code null} if {@code ePackage} is {@code null}
     * @throws IllegalArgumentException if {@code scheme} is not supported
     */
    String canonicalForm(String scheme, EPackage ePackage, String... derivationInputs) {
        CanonicalizationScheme canonicalizationScheme = schemesByTag.get(scheme);
        if (canonicalizationScheme == null) {
            throw new IllegalArgumentException("Unsupported canonicalization scheme: " + scheme
                    + " (supported: " + supportedSchemes() + ")");
        }
        return ePackage != null ? canonicalizationScheme.canonicalForm(ePackage, derivationInputs) : null;
    }

    private static String compute(CanonicalizationScheme scheme, EPackage ePackage, String... derivationInputs) {
        if (ePackage == null) {
            return null;
        }
        return scheme.tag() + ":" + scheme.digest(ePackage, derivationInputs);
    }
}
