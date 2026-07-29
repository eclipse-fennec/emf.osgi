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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;

/**
 * Builds the common core of the model service properties — the one place every
 * emission site calls, so no site can silently register a model without the
 * fingerprint. Computing the
 * properties <em>before</em> {@code registerService} is what guarantees the property
 * is present from the first instant the service is visible.
 * <p>
 * Sites add their specific properties (registration kind, scope, file extensions,
 * features, …) on top.
 *
 * @author Mark Hoffmann
 */
public final class ModelPropertiesHelper {

	private ModelPropertiesHelper() {
	}

	/**
	 * The common model property core: {@code emf.name}, {@code emf.nsURI} and
	 * {@code emf.fingerprint}. A {@code null} name or nsURI is skipped — callers that
	 * care report their own diagnostics; the fingerprint is always present.
	 *
	 * @param ePackage the registered model version; must not be {@code null}
	 * @return a mutable map with the core properties
	 */
	public static Map<String, Object> modelProperties(EPackage ePackage) {
		Objects.requireNonNull(ePackage, "ePackage");
		Map<String, Object> properties = new HashMap<>();
		String name = ePackage.getName();
		if (name != null) {
			properties.put(EMFNamespaces.EMF_NAME, name);
		}
		String nsURI = ePackage.getNsURI();
		if (nsURI != null) {
			properties.put(EMFNamespaces.EMF_MODEL_NSURI, nsURI);
		}
		properties.put(EMFNamespaces.EMF_MODEL_FINGERPRINT, FingerprintHelper.fingerprint(ePackage));
		return properties;
	}
}
