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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for a {@link ResourceSetCacheComponent}.
 * Enables caching of a single ResourceSet instance created by a specific ResourceSetFactory.
 */
@ObjectClassDefinition(
		name = "EMF ResourceSet Cache",
		description = "Caches a single ResourceSet instance created by a targeted ResourceSetFactory service."
)
public @interface ResourceSetCacheConfig {

	/**
	 * Target filter for the ResourceSetFactory service to use.
	 * Allows selecting a specific ResourceSetFactory for the cached ResourceSet.
	 * @return the LDAP target filter
	 */
	@AttributeDefinition(
			name = "ResourceSet Factory Target",
			description = "LDAP target filter for the ResourceSetFactory service to use for creating the cached ResourceSet.",
			required = false
	)
	String resourceSetFactory_target() default "";

}
