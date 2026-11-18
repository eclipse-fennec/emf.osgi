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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;

/**
 * Unit tests for {@link GeckoServiceProperties}.
 */
class GeckoServicePropertiesTest {

	@Test
	void toFennecDropsFrameworkProperties() {
		Map<String, Object> props = new HashMap<>();
		props.put(Constants.SERVICE_ID, 1L);
		props.put(Constants.OBJECTCLASS, new String[] { "x" });
		props.put(ComponentConstants.COMPONENT_NAME, "c");
		props.put("custom", "value");

		Dictionary<String, Object> result = GeckoServiceProperties.toFennec(props);

		assertNull(result.get(Constants.SERVICE_ID));
		assertNull(result.get(Constants.OBJECTCLASS));
		assertNull(result.get(ComponentConstants.COMPONENT_NAME));
		assertEquals("value", result.get("custom"));
	}

	@Test
	void toFennecAddsFennecNamesForLegacyNames() {
		Map<String, Object> props = new HashMap<>();
		props.put("emf.model.name", "library");
		props.put("emf.model.nsURI", "http://library/1.0");
		props.put("emf.configurator.name", "mongo");

		Dictionary<String, Object> result = GeckoServiceProperties.toFennec(props);

		assertEquals("library", result.get(EMFNamespaces.EMF_NAME));
		assertEquals("http://library/1.0", result.get(EMFNamespaces.EMF_MODEL_NSURI));
		assertEquals("mongo", result.get(EMFNamespaces.EMF_CONFIGURATOR_NAME));
		// legacy names stay, so filters written for them keep working
		assertEquals("library", result.get("emf.model.name"));
		assertEquals("mongo", result.get("emf.configurator.name"));
	}

	@Test
	void toFennecKeepsExplicitFennecValue() {
		Map<String, Object> props = new HashMap<>();
		props.put("emf.model.name", "legacy");
		props.put(EMFNamespaces.EMF_NAME, "fennec");

		Dictionary<String, Object> result = GeckoServiceProperties.toFennec(props);

		assertEquals("fennec", result.get(EMFNamespaces.EMF_NAME));
	}

	@Test
	void toFennecMergesLegacyNamesMappingToTheSameKey() {
		Map<String, Object> props = new HashMap<>();
		props.put("emf.model.fileExtension", new String[] { "xmi", "xml" });
		props.put("emf.resource.configurator.fileExtension", List.of("xml", "json"));

		Dictionary<String, Object> result = GeckoServiceProperties.toFennec(props);

		Object merged = result.get(EMFNamespaces.EMF_MODEL_FILE_EXT);
		assertEquals(Set.of("xmi", "xml", "json"), Set.copyOf((List<?>) merged));
	}

	@Test
	void toLegacyAddsLegacyNamesForFennecNames() {
		Map<String, Object> props = new HashMap<>();
		props.put(Constants.SERVICE_ID, 7L);
		props.put(EMFNamespaces.EMF_NAME, List.of("library"));
		props.put(EMFNamespaces.EMF_MODEL_NSURI, List.of("http://library/1.0"));
		props.put(EMFNamespaces.EMF_CONFIGURATOR_NAME, List.of("mongo", "xmi"));

		Dictionary<String, Object> result = GeckoServiceProperties.toLegacy(props);

		assertNull(result.get(Constants.SERVICE_ID));
		assertEquals(List.of("library"), result.get("emf.model.name"));
		assertEquals(List.of("http://library/1.0"), result.get("emf.model.nsURI"));
		assertEquals(List.of("mongo", "xmi"), result.get("emf.configurator.name"));
		assertEquals(List.of("mongo", "xmi"), result.get("emf.resource.configurator.name"));
		assertEquals(List.of("library"), result.get(EMFNamespaces.EMF_NAME));
	}

	@Test
	void toLegacyWithoutEmfPropertiesAddsNothing() {
		Map<String, Object> props = Map.of("custom", "value");

		Dictionary<String, Object> result = GeckoServiceProperties.toLegacy(props);

		assertEquals(1, result.size());
		assertFalse(result.isEmpty());
	}

	@Test
	void nullPropertiesAreRejected() {
		assertThrows(NullPointerException.class, () -> GeckoServiceProperties.toFennec(null));
		assertThrows(NullPointerException.class, () -> GeckoServiceProperties.toLegacy(null));
	}
}
