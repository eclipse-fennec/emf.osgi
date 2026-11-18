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
package org.eclipse.fennec.emf.gecko.compatibility;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;

/**
 * Converts service properties between the GeckoEMF and the Fennec naming scheme.
 * <p>
 * GeckoEMF 3.x - 5.x used prefixed property names ({@code emf.model.name},
 * {@code emf.configurator.name}, {@code emf.resource.configurator.name}, ...).
 * GeckoEMF 6 and Fennec use the flat names ({@code emf.name},
 * {@code emf.configuratorName}, ...). The bridges copy all properties and add the
 * counterpart names, so filters written for either naming scheme match.
 * </p>
 */
final class GeckoServiceProperties {

	/** Framework and DS properties that belong to the original registration only. */
	private static final Set<String> EXCLUDED_PROPERTIES = Set.of(
			Constants.SERVICE_ID,
			Constants.SERVICE_BUNDLEID,
			Constants.SERVICE_SCOPE,
			Constants.OBJECTCLASS,
			Constants.SERVICE_PID,
			ComponentConstants.COMPONENT_NAME,
			ComponentConstants.COMPONENT_ID
	);

	/** GeckoEMF 3.x - 5.x property name to Fennec property name. */
	private static final Map<String, String> LEGACY_TO_FENNEC = new LinkedHashMap<>();
	/** Fennec property name to the GeckoEMF 3.x - 5.x names a legacy ResourceSetFactory advertised. */
	private static final Map<String, List<String>> FENNEC_TO_LEGACY = new LinkedHashMap<>();

	static {
		LEGACY_TO_FENNEC.put("emf.model.name", EMFNamespaces.EMF_NAME);
		LEGACY_TO_FENNEC.put("emf.model.nsURI", EMFNamespaces.EMF_MODEL_NSURI);
		LEGACY_TO_FENNEC.put("emf.model.contentType", EMFNamespaces.EMF_MODEL_CONTENT_TYPE);
		LEGACY_TO_FENNEC.put("emf.model.fileExtension", EMFNamespaces.EMF_MODEL_FILE_EXT);
		LEGACY_TO_FENNEC.put("emf.model.version", EMFNamespaces.EMF_MODEL_VERSION);
		LEGACY_TO_FENNEC.put("emf.configurator.name", EMFNamespaces.EMF_CONFIGURATOR_NAME);
		LEGACY_TO_FENNEC.put("emf.configurator.contentType", EMFNamespaces.EMF_MODEL_CONTENT_TYPE);
		LEGACY_TO_FENNEC.put("emf.configurator.fileExtension", EMFNamespaces.EMF_MODEL_FILE_EXT);
		LEGACY_TO_FENNEC.put("emf.configurator.feature", EMFNamespaces.EMF_MODEL_FEATURE);
		LEGACY_TO_FENNEC.put("emf.configurator.version", EMFNamespaces.EMF_MODEL_VERSION);
		LEGACY_TO_FENNEC.put("emf.resource.configurator.name", EMFNamespaces.EMF_CONFIGURATOR_NAME);
		LEGACY_TO_FENNEC.put("emf.resource.configurator.contentType", EMFNamespaces.EMF_MODEL_CONTENT_TYPE);
		LEGACY_TO_FENNEC.put("emf.resource.configurator.fileExtension", EMFNamespaces.EMF_MODEL_FILE_EXT);
		LEGACY_TO_FENNEC.put("emf.resource.configurator.protocol", EMFNamespaces.EMF_MODEL_PROTOCOL);
		LEGACY_TO_FENNEC.put("emf.resource.configurator.feature", EMFNamespaces.EMF_MODEL_FEATURE);

		FENNEC_TO_LEGACY.put(EMFNamespaces.EMF_NAME, List.of("emf.model.name"));
		FENNEC_TO_LEGACY.put(EMFNamespaces.EMF_MODEL_NSURI, List.of("emf.model.nsURI"));
		FENNEC_TO_LEGACY.put(EMFNamespaces.EMF_CONFIGURATOR_NAME,
				List.of("emf.configurator.name", "emf.resource.configurator.name"));
	}

	private GeckoServiceProperties() {
		// utility class
	}

	/**
	 * Converts the properties of a legacy GeckoEMF service for its Fennec re-registration.
	 * All properties are copied; legacy names additionally get their Fennec counterpart.
	 * @param properties the properties of the legacy service, must not be <code>null</code>
	 * @return the properties for the Fennec registration
	 */
	static Dictionary<String, Object> toFennec(Map<String, Object> properties) {
		requireNonNull(properties);
		Hashtable<String, Object> result = copy(properties);
		Map<String, Set<Object>> added = new LinkedHashMap<>();
		LEGACY_TO_FENNEC.forEach((legacyKey, fennecKey) -> {
			Object value = properties.get(legacyKey);
			if (value != null && !properties.containsKey(fennecKey)) {
				added.computeIfAbsent(fennecKey, k -> new LinkedHashSet<>()).addAll(flatten(value));
			}
		});
		added.forEach((key, values) -> result.put(key, toPropertyValue(values)));
		return result;
	}

	/**
	 * Converts the properties of a Fennec service for its legacy GeckoEMF re-registration.
	 * All properties are copied; the Fennec names a pre-6 GeckoEMF consumer may filter on
	 * additionally get their legacy counterparts.
	 * @param properties the properties of the Fennec service, must not be <code>null</code>
	 * @return the properties for the legacy registration
	 */
	static Dictionary<String, Object> toLegacy(Map<String, Object> properties) {
		requireNonNull(properties);
		Hashtable<String, Object> result = copy(properties);
		FENNEC_TO_LEGACY.forEach((fennecKey, legacyKeys) -> {
			Object value = properties.get(fennecKey);
			if (value != null) {
				legacyKeys.stream()
						.filter(legacyKey -> !properties.containsKey(legacyKey))
						.forEach(legacyKey -> result.put(legacyKey, value));
			}
		});
		return result;
	}

	private static Hashtable<String, Object> copy(Map<String, Object> properties) {
		Hashtable<String, Object> result = new Hashtable<>();
		properties.forEach((key, value) -> {
			if (value != null && !EXCLUDED_PROPERTIES.contains(key)) {
				result.put(key, value);
			}
		});
		return result;
	}

	private static List<Object> flatten(Object value) {
		List<Object> values = new ArrayList<>();
		if (value instanceof Collection<?> collection) {
			values.addAll(collection);
		} else if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				values.add(Array.get(value, i));
			}
		} else {
			values.add(value);
		}
		return values;
	}

	private static Object toPropertyValue(Set<Object> values) {
		if (values.size() == 1) {
			return values.iterator().next();
		}
		return List.copyOf(values);
	}
}
