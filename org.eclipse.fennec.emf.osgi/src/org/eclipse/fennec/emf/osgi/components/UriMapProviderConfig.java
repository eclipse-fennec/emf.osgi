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
 * Configuration for a {@link UriMapProviderComponent}.
 * Defines URI mappings from source URIs to destination URIs for EMF resource resolution.
 */
@ObjectClassDefinition(
		name = "EMF URI Map Provider",
		description = "Provides URI mappings for EMF resource resolution. Source and destination URI lists must have equal length."
)
public @interface UriMapProviderConfig {

	/**
	 * Comma-separated list of source URIs.
	 * Must have the same number of entries as the destination URIs.
	 * @return the source URIs
	 */
	@AttributeDefinition(
			name = "Source URIs",
			description = "Comma-separated list of source URIs to map from."
	)
	String uri_map_src();

	/**
	 * Comma-separated list of destination URIs.
	 * Must have the same number of entries as the source URIs.
	 * @return the destination URIs
	 */
	@AttributeDefinition(
			name = "Destination URIs",
			description = "Comma-separated list of destination URIs to map to."
	)
	String uri_map_dest();

}
