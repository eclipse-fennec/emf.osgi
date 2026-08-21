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
package org.eclipse.fennec.emf.osgi.constants;

import java.util.Map;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * A few Constants to enable customized UriHandler behavior
 * @author Juergen Albert
 * @since 27.07.2017
 */
public interface EMFUriHandlerConstants {

	/**
	 * The Request Methode to be used. Only applies for Outputstream to determin if out or post is used
	 */
	String OPTION_HTTP_METHOD = "method";
	
	/**
	 * Must provide a {@link Map}p of additional Headers that will be set to the request. They will be added last and thus may overwrite existing headers
	 */
	String OPTION_HTTP_HEADERS = "headers";
	
	/**
	 * If a complex Object in response is expected the Reponse will be feed as input stream to the given {@link Resource}
	 */
	String OPTIONS_EXPECTED_RESPONSE_RESOURCE = "expected.response.resource";
	
	/**
	 * A {@link Map} of Resource options that will be feed to the given response {@link Resource} as well
	 */
	String OPTIONS_EXPECTED_RESPONSE_RESOURCE_OPTIONS = "expected.response.options";
	
	/**
	 * if this option is set to <code>true</code> the response will be logged by the Urihandlers logger
	 */
	String OPTIONS_LOG_RESPONSE = "log.response";

	/**
	 * If this option is set to {@link Boolean#TRUE} in the load/save options {@link Map}, the
	 * REST URI handler permits outbound resolution of the URI for the <em>current</em> operation,
	 * regardless of the configured host whitelist.
	 * <p>
	 * By default the handler blocks demand-loading of {@code http}/{@code https} URIs (to prevent
	 * SSRF via attacker-supplied proxy references); an administrator may opt individual hosts back
	 * in through Config Admin, and trusted code that manually drives a {@code ResourceSet} may use
	 * this per-call option to allow a specific, known URI. Attacker-driven proxy demand-loads use
	 * the ResourceSet's own load options and never carry this key, so they remain blocked.
	 */
	String OPTION_ALLOW_URI_RESOLUTION = "allow.uri.resolution";
	
	/**
	 * They was used for basic authentication, which is not recommended anymore. 
	 * If you need basic Auth, please handle it manually and set the Header via the 
	 * {@link EMFUriHandlerConstants#OPTION_HTTP_HEADERS}
	 * @deprecated will not be replaced
	 */
	@Deprecated
	String OPTIONS_AUTH_USER = "user";
	
	/**
	 * They was used for basic authentication, which is not recommended anymore. 
	 * If you need basic Auth, please handle it manually and set the Header via the 
	 * {@link EMFUriHandlerConstants#OPTION_HTTP_HEADERS}
	 * @deprecated will not be replaced
	 */
	@Deprecated
	String OPTIONS_AUTH_MANDANT = "mandant";
	
	/**
	 * They was used for basic authentication, which is not recommended anymore. 
	 * If you need basic Auth, please handle it manually and set the Header via the 
	 * {@link EMFUriHandlerConstants#OPTION_HTTP_HEADERS}
	 * @deprecated will not be replaced
	 */
	@Deprecated
	String OPTIONS_AUTH_PASSWORD = "password";

}
