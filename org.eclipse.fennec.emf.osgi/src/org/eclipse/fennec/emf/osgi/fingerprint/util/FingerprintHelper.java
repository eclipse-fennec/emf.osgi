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
package org.eclipse.fennec.emf.osgi.fingerprint.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;

/**
 * Static entry point to the fingerprint computation — the primary access for the
 * emission sites (decisions M3/M4 in {@code docs/metadata-migration.md}): service
 * properties are fixed at registration time, so the computation must not depend on a
 * dynamic service reference. The {@code DefaultFingerprintService} component remains
 * registered as a facade for external consumers.
 * <p>
 * The no-derivation-input value is cached per {@link EPackage} <b>instance</b> in a
 * weak identity map, scoped to the JVM run: registration paths re-emit service
 * properties on every whiteboard change, and without the cache every bind would re-hash
 * every registered model. <b>Mutating a package after registration is out of
 * contract</b> — the fingerprint identifies a model version, a mutated package is a
 * different version; the cache is deliberately never invalidated.
 * <p>
 * Thread-safe: the computation is deterministic, so a race costs duplicate work, never
 * a wrong value.
 *
 * @author Mark Hoffmann
 */
public final class FingerprintHelper {

	private static final FingerprintService SERVICE = new DefaultFingerprintService();

	/**
	 * No-input fingerprints keyed by package instance. {@link WeakHashMap} compares by
	 * {@code equals}, which is identity for {@link EPackage}; the value never references
	 * the key, so entries die with their package.
	 */
	private static final Map<EPackage, String> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

	private FingerprintHelper() {
	}

	/**
	 * Computes the canonical fingerprint in the current scheme, cached per package
	 * instance when no derivation inputs are given.
	 *
	 * @param ePackage the model version to fingerprint; may be {@code null}
	 * @param derivationInputs optional canonical input tokens
	 * @return the fingerprint, or {@code null} if {@code ePackage} is {@code null}
	 * @see FingerprintService#fingerprint(EPackage, String...)
	 */
	public static String fingerprint(EPackage ePackage, String... derivationInputs) {
		if (ePackage == null) {
			return null;
		}
		if (derivationInputs == null || derivationInputs.length == 0) {
			return CACHE.computeIfAbsent(ePackage, SERVICE::fingerprint);
		}
		return SERVICE.fingerprint(ePackage, derivationInputs);
	}

	/**
	 * Computes the fingerprint in an explicitly named scheme. Never cached — scheme
	 * variants are diagnostic/migration calls, not the emission hot path.
	 *
	 * @param scheme the scheme tag; must be one of {@link #supportedSchemes()}
	 * @param ePackage the model version to fingerprint; may be {@code null}
	 * @param derivationInputs optional canonical input tokens
	 * @return the fingerprint, or {@code null} if {@code ePackage} is {@code null}
	 * @see FingerprintService#fingerprintInScheme(String, EPackage, String...)
	 */
	public static String fingerprintInScheme(String scheme, EPackage ePackage, String... derivationInputs) {
		return SERVICE.fingerprintInScheme(scheme, ePackage, derivationInputs);
	}

	/**
	 * The scheme tag new values are produced in.
	 *
	 * @return the current scheme tag, e.g. {@code "fp1"}
	 */
	public static String currentScheme() {
		return SERVICE.currentScheme();
	}

	/**
	 * All computable scheme tags.
	 *
	 * @return the supported scheme tags
	 */
	public static Set<String> supportedSchemes() {
		return SERVICE.supportedSchemes();
	}
}
