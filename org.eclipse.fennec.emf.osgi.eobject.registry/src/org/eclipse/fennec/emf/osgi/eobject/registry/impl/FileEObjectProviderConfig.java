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
 * Factory configuration of one file provider instance. The single-underscore method
 * names map to dotted configuration keys per the DS property naming rules -
 * {@code emf_eobject_provider_name()} is the {@code emf.eobject.provider.name} property
 * a registry configuration's {@code initialProvider.target} selects.
 */
@ObjectClassDefinition(name = "EObject Registry File Provider", description = "Loads EObjects from local resource files "
		+ "as the initial content of an EObject registry. Selected by a registry configuration via "
		+ "initialProvider.target=(emf.eobject.provider.name=...).")
public @interface FileEObjectProviderConfig {

	@AttributeDefinition(name = "Provider name", description = "The provider's name: its emf.eobject.provider.name "
			+ "service property and the source tag of every entry it writes.")
	String emf_eobject_provider_name();

	@AttributeDefinition(name = "Locations", required = false, description = "Files or directories to load "
			+ "(directories are walked recursively). An empty list is a valid, empty initial state.")
	String[] locations() default {};

	@AttributeDefinition(name = "Key feature", required = false, description = "Optional attribute name whose value "
			+ "becomes the entry key (e.g. an id attribute). Empty: keys are <fileName>#<uriFragment>.")
	String key_feature() default "";

	@AttributeDefinition(name = "File extensions", required = false, description = "Extensions (without the leading "
			+ "dot, matched case-insensitively) a directory walk attempts to load. Files with any other extension "
			+ "are passed over silently, as are dotfiles such as .keep - only a file that was meant to be a model "
			+ "and failed to parse is logged. An EMPTY list attempts every file in the directory. A location that "
			+ "names a file directly is always loaded, whatever its extension.")
	String[] file_extensions() default { "xmi", "ecore", "json", "xml" };
}
