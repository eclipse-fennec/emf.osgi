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

import java.util.Set;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;

/**
 * Static entry point to the fingerprint computation — the primary access for the
 * emission sites inside this bundle (decision M3 in {@code docs/metadata-migration.md}):
 * service properties are fixed at registration time, so the computation must not depend
 * on a dynamic service reference. The {@link DefaultFingerprintService} component remains
 * registered as a facade for external consumers.
 * <p>
 * Stateless delegation to a shared {@link DefaultFingerprintService}; thread-safe because
 * the service is.
 *
 * @author Mark Hoffmann
 */
public final class FingerprintHelper {

	private static final DefaultFingerprintService SERVICE = new DefaultFingerprintService();

	private FingerprintHelper() {
	}

	/**
	 * Computes the canonical fingerprint in the current scheme.
	 *
	 * @param ePackage the model version to fingerprint; may be {@code null}
	 * @param derivationInputs optional canonical input tokens
	 * @return the fingerprint, or {@code null} if {@code ePackage} is {@code null}
	 * @see FingerprintService#fingerprint(EPackage, String...)
	 */
	public static String fingerprint(EPackage ePackage, String... derivationInputs) {
		return SERVICE.fingerprint(ePackage, derivationInputs);
	}

	/**
	 * Computes the fingerprint in an explicitly named scheme.
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
