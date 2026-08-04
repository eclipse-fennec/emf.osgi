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
package org.eclipse.fennec.emf.osgi.eobject.registry.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Factory configuration of one EObject registry instance. The initial provider is
 * selected through the standard DS reference target, e.g.
 * {@code initialProvider.target=(emf.eobject.provider.name=my-files)} - it is not an
 * OCD attribute on purpose (SCR reads it directly from the configuration).
 */
@ObjectClassDefinition(name = "EObject Registry", description = "One named EObject registry instance per factory configuration. "
		+ "The registry services are published only after the initial provider completed its load; "
		+ "select the initial provider via initialProvider.target=(emf.eobject.provider.name=...).")
public @interface EObjectRegistryConfig {

	@AttributeDefinition(name = "Name", description = "Stable, human-chosen registry instance name; "
			+ "propagated as the emf.eobject.registry.name service property. Changing it means a new registry.")
	String name();

	@AttributeDefinition(name = "Content types", required = false, description = "Optional declared content types "
			+ "(nsURIs or EClass URIs), propagated as the emf.eobject.registry.content.types service property for discovery.")
	String[] content_types() default {};
}
