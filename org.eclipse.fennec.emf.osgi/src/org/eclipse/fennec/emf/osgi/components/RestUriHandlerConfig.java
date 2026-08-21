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
 * Configuration for the REST ({@code http}/{@code https}) URI handler that Fennec attaches
 * to every {@code ResourceSet} produced by the EMF ResourceSetFactory.
 * <p>
 * By default (no configuration, or an empty {@link #allowedHosts()} list) the handler
 * <strong>blocks</strong> all demand-loading of {@code http}/{@code https} URIs. This prevents
 * server-side request forgery (SSRF, CWE-918) via attacker-supplied proxy references that would
 * otherwise be fetched during deserialization. Listing hosts here opts them back in for outbound
 * resolution; trusted code driving a {@code ResourceSet} manually can additionally allow a single
 * URI per call through the
 * {@code org.eclipse.fennec.emf.osgi.constants.EMFUriHandlerConstants#OPTION_ALLOW_URI_RESOLUTION}
 * load/save option.
 */
@ObjectClassDefinition(
		name = "EMF REST URI Handler Access Policy",
		description = "Controls outbound http/https proxy resolution performed by the Fennec REST URI "
				+ "handler. With no allowed hosts (the default) all http(s) resolution is blocked to "
				+ "prevent SSRF; list host names to permit resolution against them.")
public @interface RestUriHandlerConfig {

	@AttributeDefinition(
			name = "Allowed Hosts",
			description = "Host names permitted for outbound http/https proxy resolution (matched "
					+ "case-insensitively, host only - no port). Empty (the default) blocks all http(s) "
					+ "resolution.",
			required = false)
	String[] allowedHosts() default {};
}
