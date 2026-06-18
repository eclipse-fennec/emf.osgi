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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the JAXP XML processing limits that EMF's XMI parser should
 * use when loading resources.
 * <p>
 * Each attribute maps to the matching {@code jdk.xml.*} SAX parser property. An
 * empty value leaves the JDK default in place; a value of {@code "0"} (or any
 * value &le; 0) disables the corresponding limit. These limits were tightened
 * significantly in JDK 24
 * (<a href="https://bugs.openjdk.org/browse/JDK-8343006">JDK-8343006</a>); the
 * ones reachable by ordinary, entity-free XMI are {@code elementAttributeLimit}
 * (10000 &rarr; 200) and {@code maxElementDepth} (unlimited &rarr; 100).
 */
@ObjectClassDefinition(
		name = "EMF JAXP Processing Limits",
		description = "Configures the jdk.xml.* JAXP processing limits applied by EMF's XMI parser when loading "
				+ "resources. Only active on Java 24+, where these limits were tightened. Empty = keep JDK default, "
				+ "0 = unlimited.")
public @interface JaxpLimitConfig {

	@AttributeDefinition(
			name = "Element Attribute Limit",
			description = "jdk.xml.elementAttributeLimit: maximum number of attributes on a single element "
					+ "(JDK 24+ default 200). Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String elementAttributeLimit() default "";

	@AttributeDefinition(
			name = "Max Element Depth",
			description = "jdk.xml.maxElementDepth: maximum element nesting depth (JDK 24+ default 100). "
					+ "Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String maxElementDepth() default "";

	@AttributeDefinition(
			name = "Max Occur Limit",
			description = "jdk.xml.maxOccurLimit: maximum number of content-model nodes for maxOccurs. "
					+ "Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String maxOccurLimit() default "";

	@AttributeDefinition(
			name = "Entity Expansion Limit",
			description = "jdk.xml.entityExpansionLimit: maximum number of entity expansions. "
					+ "Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String entityExpansionLimit() default "";

	@AttributeDefinition(
			name = "Total Entity Size Limit",
			description = "jdk.xml.totalEntitySizeLimit: maximum total size of all entities. "
					+ "Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String totalEntitySizeLimit() default "";

	@AttributeDefinition(
			name = "Max General Entity Size Limit",
			description = "jdk.xml.maxGeneralEntitySizeLimit: maximum size of any single general entity. "
					+ "Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String maxGeneralEntitySizeLimit() default "";

	@AttributeDefinition(
			name = "Max Parameter Entity Size Limit",
			description = "jdk.xml.maxParameterEntitySizeLimit: maximum size of any single parameter entity. "
					+ "Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String maxParameterEntitySizeLimit() default "";

	@AttributeDefinition(
			name = "Max XML Name Limit",
			description = "jdk.xml.maxXMLNameLimit: maximum length of XML names. "
					+ "Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String maxXMLNameLimit() default "";

	@AttributeDefinition(
			name = "Entity Replacement Limit",
			description = "jdk.xml.entityReplacementLimit: maximum number of nodes from entity references. "
					+ "Empty keeps the JDK default, 0 disables the limit.",
			required = false)
	String entityReplacementLimit() default "";
}
