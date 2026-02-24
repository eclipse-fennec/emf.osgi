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
 * Configuration for a dedicated ResourceSetFactory instance.
 * Used by {@link ConfigurationResourceSetFactoryComponent} to create
 * isolated ResourceSetFactory instances that are wired to specific registries.
 */
@ObjectClassDefinition(
		name = "EMF ResourceSet Factory",
		description = "Configuration for a dedicated ResourceSetFactory instance. Allows targeting specific EPackage and Resource.Factory registries."
)
public @interface ResourceSetFactoryConfig {

	/**
	 * Target filter for the EPackage.Registry service to use.
	 * Determines which EPackage registry provides models for this factory's ResourceSets.
	 * @return the LDAP target filter
	 */
	@AttributeDefinition(
			name = "EPackage Registry Target",
			description = "LDAP target filter for the EPackage.Registry service to use for ResourceSet creation.",
			required = false
	)
	String ePackageRegistry_target() default "(emf.model.scope=resourceset)";

	/**
	 * Target filter for the Resource.Factory.Registry service to use.
	 * Determines which Resource.Factory registry provides resource factories for this factory's ResourceSets.
	 * @return the LDAP target filter
	 */
	@AttributeDefinition(
			name = "Resource Factory Registry Target",
			description = "LDAP target filter for the Resource.Factory.Registry service to use for ResourceSet creation.",
			required = false
	)
	String resourceFactoryRegistry_target() default "";

}
