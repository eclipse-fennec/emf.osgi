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
 * Configuration for a dedicated EPackage registry instance.
 * Used by {@link ConfigurationEPackageRegistryComponent} to create
 * isolated EPackage registries that can be targeted by specific ResourceSetFactory instances.
 */
@ObjectClassDefinition(
		name = "EMF EPackage Registry",
		description = "Configuration for a dedicated EPackage registry instance that tracks EPackageConfigurator services and delegates to a parent registry."
)
public @interface EPackageRegistryConfig {

	/**
	 * The name of the resource set factory this registry belongs to.
	 * Used to associate this registry with a specific ResourceSetFactory via target filters.
	 * @return the factory name
	 */
	@AttributeDefinition(
			name = "Factory Name",
			description = "Name of the resource set factory this registry belongs to. Used for target filter matching (rsf.name=<name>)."
	)
	String rsf_name();

	/**
	 * Target filter for the EPackageConfigurator services to track.
	 * @return the LDAP target filter
	 */
	@AttributeDefinition(
			name = "EPackage Configurator Target",
			description = "LDAP target filter for the EPackageConfigurator services to track.",
			required = false
	)
	String ePackageConfigurator_target() default "(emf.model.scope=resourceset)";

	/**
	 * Target filter for the parent EPackage.Registry service.
	 * The parent registry provides the delegate for failed lookups.
	 * @return the LDAP target filter
	 */
	@AttributeDefinition(
			name = "Parent Registry Target",
			description = "LDAP target filter for the parent EPackage.Registry service used as delegate for failed lookups.",
			required = false
	)
	String parentRegistry_target() default "(default.resourceset.epackage.registry=true)";

}
