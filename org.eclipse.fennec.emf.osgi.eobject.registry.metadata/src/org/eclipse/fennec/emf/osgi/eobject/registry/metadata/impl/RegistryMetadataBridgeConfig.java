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
package org.eclipse.fennec.emf.osgi.eobject.registry.metadata.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Factory configuration of one bridge instance. {@code emf_eobject_registry_name()}
 * maps to the {@code emf.eobject.registry.name} service property, which is what the
 * registry component's listener whiteboard matches on. A custom anchor resolver is
 * selected via the standard DS reference target
 * {@code anchorResolver.target=(...)} - not an OCD attribute.
 */
@ObjectClassDefinition(name = "EObject Registry Metadata Bridge", description = "Attaches the content of one named "
		+ "EObject registry as AspectEntry to the ClassMetadata of every live model version, under a domain aspect type id.")
public @interface RegistryMetadataBridgeConfig {

	@AttributeDefinition(name = "Registry name", description = "The emf.eobject.registry.name of the registry to observe.")
	String emf_eobject_registry_name();

	@AttributeDefinition(name = "Aspect type id", description = "The aspect type id this bridge owns, "
			+ "e.g. sensinact.mapping - the id consumers pass to MetadataService.getClassAspect(eClass, typeId).")
	String aspect_type_id();
}
