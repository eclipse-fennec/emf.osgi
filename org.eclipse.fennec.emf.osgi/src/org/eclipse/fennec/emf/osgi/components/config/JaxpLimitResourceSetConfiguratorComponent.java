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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition;
import org.osgi.service.metatype.annotations.Designate;

/**
 * A {@link ResourceSetConfigurator} that applies configurable JAXP XML
 * processing limits ({@code jdk.xml.*}) to every {@link ResourceSet} created by
 * the EMF ResourceSetFactory, by setting them as
 * {@link XMLResource#OPTION_PARSER_PROPERTIES} in the resource set's load
 * options.
 * <p>
 * The component is gated to Java 24+ through a mandatory reference to the
 * {@link EMFNamespaces#RUNTIME_JAVA_24_PLUS_CONDITION_ID} condition, which only
 * exists on such a runtime. On older JDKs the reference stays
 * unsatisfied and the component never activates - matching the fact that the
 * tightened {@code jdk.xml.*} defaults only exist on JDK 24+. Activation also
 * requires a Config Admin configuration for {@value #PID}, so the limits are
 * only ever touched when an administrator opts in.
 * <p>
 * Note: the configured properties are applied to {@link ResourceSet#getLoadOptions()},
 * so they take effect for resources demand-loaded through the resource set (and
 * for callers that pass these load options to {@code Resource.load}).
 */
@Component(
		name = "JaxpLimitResourceSetConfigurator",
		service = ResourceSetConfigurator.class,
		configurationPid = JaxpLimitResourceSetConfiguratorComponent.PID,
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = JaxpLimitConfig.class)
@ProviderType
public class JaxpLimitResourceSetConfiguratorComponent implements ResourceSetConfigurator {

	/** Configuration PID for the JAXP processing-limit configuration. */
	public static final String PID = "org.eclipse.fennec.emf.osgi.jaxp.limits";

	private static final String JDK_XML_PREFIX = "jdk.xml.";

	/** Immutable map of {@code jdk.xml.*} parser properties derived from the configuration. */
	private final Map<String, Object> parserProperties;

	@Activate
	public JaxpLimitResourceSetConfiguratorComponent(
			JaxpLimitConfig config,
			@Reference(target = "(" + Condition.CONDITION_ID + "=" + EMFNamespaces.RUNTIME_JAVA_24_PLUS_CONDITION_ID + ")")
			Condition java24Plus) {
		this.parserProperties = buildParserProperties(config);
	}

	@Override
	public void configureResourceSet(ResourceSet resourceSet) {
		if (parserProperties.isEmpty()) {
			return;
		}
		Map<Object, Object> loadOptions = resourceSet.getLoadOptions();

		// Merge with any parser properties a previous configurator may have set,
		// rather than replacing them outright.
		Map<String, Object> merged = new LinkedHashMap<>();
		Object existing = loadOptions.get(XMLResource.OPTION_PARSER_PROPERTIES);
		if (existing instanceof Map<?, ?> existingMap) {
			existingMap.forEach((k, v) -> merged.put(String.valueOf(k), v));
		}
		merged.putAll(parserProperties);

		loadOptions.put(XMLResource.OPTION_PARSER_PROPERTIES, merged);
	}

	private static Map<String, Object> buildParserProperties(JaxpLimitConfig config) {
		Map<String, Object> properties = new LinkedHashMap<>();
		putIfSet(properties, "elementAttributeLimit", config.elementAttributeLimit());
		putIfSet(properties, "maxElementDepth", config.maxElementDepth());
		putIfSet(properties, "maxOccurLimit", config.maxOccurLimit());
		putIfSet(properties, "entityExpansionLimit", config.entityExpansionLimit());
		putIfSet(properties, "totalEntitySizeLimit", config.totalEntitySizeLimit());
		putIfSet(properties, "maxGeneralEntitySizeLimit", config.maxGeneralEntitySizeLimit());
		putIfSet(properties, "maxParameterEntitySizeLimit", config.maxParameterEntitySizeLimit());
		putIfSet(properties, "maxXMLNameLimit", config.maxXMLNameLimit());
		putIfSet(properties, "entityReplacementLimit", config.entityReplacementLimit());
		return Map.copyOf(properties);
	}

	private static void putIfSet(Map<String, Object> properties, String limitName, String value) {
		if (value != null && !value.isBlank()) {
			properties.put(JDK_XML_PREFIX + limitName, value.trim());
		}
	}
}
