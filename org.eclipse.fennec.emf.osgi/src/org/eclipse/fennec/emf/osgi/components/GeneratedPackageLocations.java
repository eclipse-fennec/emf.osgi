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
package org.eclipse.fennec.emf.osgi.components;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.common.util.URI;

/**
 * The declared locations of the generated packages, mapped to their namespace URIs.
 * <p>
 * A bundle that contains a generated model declares a
 * {@code org.eclipse.emf.ecore.generated_package} capability. Its {@code ecore} attribute is the
 * location of the model file inside the bundle, its {@code uri} attribute the namespace URI of the
 * package. The location is turned into the {@code platform:/plugin/<bsn>/<ecore>} URI that other
 * models use to reference the model, the same way EMF resolves the relative {@code genModel}
 * location of the {@code generated_package} extension.
 * <p>
 * The table is thread safe.
 */
final class GeneratedPackageLocations {

	/** The namespace of the capability a bundle declares for each generated package */
	static final String GENERATED_PACKAGE_NAMESPACE = "org.eclipse.emf.ecore.generated_package";
	/** The capability attribute with the namespace URI of the package */
	static final String URI_ATTRIBUTE = "uri";
	/** The capability attribute with the location of the model file in the bundle */
	static final String ECORE_ATTRIBUTE = "ecore";

	private final Map<URI, String> nsUris = new ConcurrentHashMap<>();

	/**
	 * Computes the locations one bundle declares for its generated packages.
	 * @param symbolicName the symbolic name of the bundle, must not be <code>null</code>
	 * @param capabilityAttributes the attributes of each {@code generated_package} capability of the bundle
	 * @return the location of each package mapped to its namespace URI, never <code>null</code>
	 */
	static Map<URI, String> locationsOf(String symbolicName, Collection<Map<String, Object>> capabilityAttributes) {
		requireNonNull(symbolicName);
		requireNonNull(capabilityAttributes);
		Map<URI, String> result = new HashMap<>();
		for (Map<String, Object> attributes : capabilityAttributes) {
			if (attributes.get(ECORE_ATTRIBUTE) instanceof String ecore && !ecore.isBlank()
					&& attributes.get(URI_ATTRIBUTE) instanceof String nsUri && !nsUri.isBlank()) {
				result.put(toLocation(symbolicName, ecore.trim()), nsUri.trim());
			}
		}
		return result;
	}

	/**
	 * Creates the URI of a model file location inside a bundle.
	 * @param symbolicName the symbolic name of the bundle
	 * @param ecore the location of the model file, relative to the bundle root or an absolute URI
	 * @return the {@code platform:/plugin} URI of a relative location, the absolute URI unchanged
	 */
	private static URI toLocation(String symbolicName, String ecore) {
		URI ecoreUri = URI.createURI(ecore);
		if (!ecoreUri.isRelative()) {
			return ecoreUri;
		}
		String path = ecore.startsWith("/") ? ecore.substring(1) : ecore;
		return URI.createPlatformPluginURI(symbolicName + "/" + path, true);
	}

	/**
	 * Returns the namespace URI of the generated package declared at the given location.
	 * @param location the location URI without a fragment
	 * @return the namespace URI, or an empty {@link Optional} if no generated package is declared there
	 */
	Optional<String> getNsUri(URI location) {
		return location == null ? Optional.empty() : Optional.ofNullable(nsUris.get(location));
	}

	/**
	 * Adds the given locations.
	 * @param locations the locations mapped to their namespace URIs
	 */
	void add(Map<URI, String> locations) {
		nsUris.putAll(locations);
	}

	/**
	 * Removes the given locations, unless another bundle has meanwhile mapped one of them to a different
	 * namespace URI.
	 * @param locations the locations mapped to their namespace URIs, as given to {@link #add(Map)}
	 */
	void remove(Map<URI, String> locations) {
		locations.forEach(nsUris::remove);
	}

	/**
	 * Removes all locations.
	 */
	void clear() {
		nsUris.clear();
	}
}
