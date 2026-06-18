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
package org.eclipse.fennec.emf.osgi.components.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.junit.jupiter.api.Test;
import org.osgi.service.condition.Condition;

/**
 * Plain unit tests for {@link JaxpLimitResourceSetConfiguratorComponent}: they
 * verify the mapping from configuration to {@code jdk.xml.*} parser properties
 * and that those are applied to a {@link ResourceSet}'s load options. The Java
 * 24+ gating itself is enforced by Declarative Services (a mandatory
 * {@link Condition} reference) and is therefore not exercised here.
 */
public class JaxpLimitResourceSetConfiguratorComponentTest {

	/** Builds a {@link JaxpLimitConfig} instance, defaulting unset attributes to "". */
	private static JaxpLimitConfig config(Map<String, String> values) {
		return (JaxpLimitConfig) Proxy.newProxyInstance(
				JaxpLimitConfig.class.getClassLoader(),
				new Class<?>[] { JaxpLimitConfig.class },
				(proxy, method, args) -> values.getOrDefault(method.getName(), ""));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parserProperties(ResourceSet rs) {
		Object value = rs.getLoadOptions().get(XMLResource.OPTION_PARSER_PROPERTIES);
		assertInstanceOf(Map.class, value, "OPTION_PARSER_PROPERTIES should be a Map");
		return (Map<String, Object>) value;
	}

	private static JaxpLimitResourceSetConfiguratorComponent newComponent(Map<String, String> values) {
		return new JaxpLimitResourceSetConfiguratorComponent(config(values), Condition.INSTANCE);
	}

	@Test
	public void mapsConfiguredLimitsToPrefixedParserProperties() {
		ResourceSet rs = new ResourceSetImpl();
		newComponent(Map.of("elementAttributeLimit", "0", "maxElementDepth", "500")).configureResourceSet(rs);

		Map<String, Object> props = parserProperties(rs);
		assertEquals("0", props.get("jdk.xml.elementAttributeLimit"));
		assertEquals("500", props.get("jdk.xml.maxElementDepth"));
	}

	@Test
	public void ignoresBlankValuesAndTrims() {
		ResourceSet rs = new ResourceSetImpl();
		newComponent(Map.of("elementAttributeLimit", "  0  ", "maxOccurLimit", "   ")).configureResourceSet(rs);

		Map<String, Object> props = parserProperties(rs);
		assertEquals("0", props.get("jdk.xml.elementAttributeLimit"), "value should be trimmed");
		assertFalse(props.containsKey("jdk.xml.maxOccurLimit"), "blank value must not be set");
	}

	@Test
	public void doesNothingWhenNoLimitsConfigured() {
		ResourceSet rs = new ResourceSetImpl();
		newComponent(Map.of()).configureResourceSet(rs);

		assertFalse(rs.getLoadOptions().containsKey(XMLResource.OPTION_PARSER_PROPERTIES),
				"no parser properties should be added when nothing is configured");
	}

	@Test
	public void mergesWithExistingParserProperties() {
		ResourceSet rs = new ResourceSetImpl();
		rs.getLoadOptions().put(XMLResource.OPTION_PARSER_PROPERTIES,
				new java.util.HashMap<>(Map.of("http://example/feature", "keep")));

		newComponent(Map.of("elementAttributeLimit", "0")).configureResourceSet(rs);

		Map<String, Object> props = parserProperties(rs);
		assertEquals("keep", props.get("http://example/feature"), "pre-existing property must be preserved");
		assertEquals("0", props.get("jdk.xml.elementAttributeLimit"), "configured property must be added");
		assertTrue(props.size() >= 2);
	}
}
