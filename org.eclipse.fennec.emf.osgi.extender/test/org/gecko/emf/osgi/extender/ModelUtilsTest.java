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
package org.gecko.emf.osgi.extender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.fennec.emf.osgi.extender.ModelHelper;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ModelHelper#extractProperties(String, Map)}.
 * <p>
 * Verifies path/property extraction from semicolon-separated path strings
 * such as {@code "/model;foo=bar;test=me"}.
 *
 * @author Mark Hoffmann
 * @since 17.10.2022
 */
class ModelUtilsTest {

	/**
	 * Tests that null and simple paths without properties are handled correctly,
	 * including when the properties map is null.
	 */
	@Test
	void testExtractPropertiesNoProperties() {
		assertNull(ModelHelper.extractProperties(null, null));
		assertEquals("/test", ModelHelper.extractProperties("/test", null));
		assertEquals("/test", ModelHelper.extractProperties("/test;blub", null));
	}

	/**
	 * Tests that plain paths (without semicolons or with trailing semicolons)
	 * return the path unchanged and do not add entries to the properties map.
	 */
	@Test
	void testExtractPropertiesPath() {
		Map<String, String> props = new HashMap<>();
		assertEquals("/test", ModelHelper.extractProperties("/test", props));
		assertTrue(props.isEmpty());
		props.clear();

		assertEquals("", ModelHelper.extractProperties("", props));
		assertTrue(props.isEmpty());
		props.clear();

		assertEquals("/test", ModelHelper.extractProperties("/test;", props));
		assertTrue(props.isEmpty());
		props.clear();
	}

	/**
	 * Tests various valid property extraction scenarios:
	 * <ul>
	 *   <li>Key without value ({@code ;blub})</li>
	 *   <li>Key with value ({@code ;blub=bla})</li>
	 *   <li>Duplicate keys (last value wins)</li>
	 *   <li>Multiple key-value pairs</li>
	 *   <li>Mixed keys with and without values</li>
	 *   <li>Empty path with properties ({@code ;blub=bla})</li>
	 * </ul>
	 */
	@Test
	void testExtractPropertiesWorking() {
		Map<String, String> props = new HashMap<>();

		// Key without value
		assertEquals("/test", ModelHelper.extractProperties("/test;blub", props));
		assertFalse(props.isEmpty());
		assertTrue(props.containsKey("blub"));
		assertNull(props.get("blub"));
		props.clear();

		// Key with value
		assertEquals("/test", ModelHelper.extractProperties("/test;blub=bla", props));
		assertFalse(props.isEmpty());
		assertTrue(props.containsKey("blub"));
		assertEquals("bla", props.get("blub"));
		props.clear();

		// Duplicate key - last value wins
		assertEquals("/test", ModelHelper.extractProperties("/test;blub=bla;blub=blubber", props));
		assertFalse(props.isEmpty());
		assertEquals(1, props.size());
		assertTrue(props.containsKey("blub"));
		assertEquals("blubber", props.get("blub"));
		props.clear();

		// Multiple distinct key-value pairs
		assertEquals("/test", ModelHelper.extractProperties("/test;blub=bla;foo=bar", props));
		assertFalse(props.isEmpty());
		assertEquals(2, props.size());
		assertTrue(props.containsKey("blub"));
		assertEquals("bla", props.get("blub"));
		assertTrue(props.containsKey("foo"));
		assertEquals("bar", props.get("foo"));
		props.clear();

		// Mixed: key without value and key with value
		assertEquals("/test", ModelHelper.extractProperties("/test;blub;foo=bar", props));
		assertFalse(props.isEmpty());
		assertEquals(2, props.size());
		assertTrue(props.containsKey("blub"));
		assertNull(props.get("blub"));
		assertTrue(props.containsKey("foo"));
		assertEquals("bar", props.get("foo"));
		props.clear();

		// Empty path with properties
		assertEquals("", ModelHelper.extractProperties(";blub=bla", props));
		assertFalse(props.isEmpty());
		assertTrue(props.containsKey("blub"));
		assertEquals("bla", props.get("blub"));
		props.clear();
	}
}
