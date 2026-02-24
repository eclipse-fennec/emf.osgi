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
 * Configuration for a dedicated Resource.Factory registry instance.
 * Used by {@link ConfigurationResourceFactoryRegistryComponent} to create
 * isolated Resource.Factory registries.
 */
@ObjectClassDefinition(
		name = "EMF Resource Factory Registry",
		description = "Configuration for a dedicated Resource.Factory registry instance."
)
public @interface ResourceFactoryRegistryConfig {

	/**
	 * The name of the resource set factory this registry belongs to.
	 * Used to associate this registry with a specific ResourceSetFactory via target filters.
	 * @return the factory name
	 */
	@AttributeDefinition(
			name = "Factory Name",
			description = "Name of the resource set factory this registry belongs to. Used for target filter matching (rsf.name=<name>).",
			required = false
	)
	String rsf_name() default "";

}
