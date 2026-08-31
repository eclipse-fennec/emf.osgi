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
package org.eclipse.fennec.emf.osgi.urihandler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.common.util.URI;
import org.eclipse.fennec.emf.osgi.constants.EMFUriHandlerConstants;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for the SSRF access policy of {@link RestfulURIHandlerImpl}: outbound
 * http(s) resolution is blocked by default, permitted only for whitelisted hosts, and can be
 * opted in per call via {@link EMFUriHandlerConstants#OPTION_ALLOW_URI_RESOLUTION}. The block
 * path is asserted through {@code createInputStream}, which refuses before opening any
 * connection, so the tests never touch the network.
 */
public class RestfulURIHandlerAccessTest {

	private static final URI METADATA_URI = URI.createURI("http://169.254.169.254/latest/meta-data");

	@Test
	public void blocksResolutionByDefault() {
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl();

		assertFalse(handler.isResolutionAllowed(METADATA_URI, Map.of()),
				"empty whitelist must block every http(s) host");

		IOException ex = assertThrows(IOException.class,
				() -> handler.createInputStream(METADATA_URI, Map.of()),
				"createInputStream must refuse the blocked URI before connecting");
		assertTrue(ex.getMessage().contains("Blocked outbound"),
				"the failure must come from the access guard, not from the network");
	}

	@Test
	public void allowsWhitelistedHostCaseInsensitively() {
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl(Set.of("Example.Test"));

		assertTrue(handler.isResolutionAllowed(URI.createURI("http://example.test/model.json"), Map.of()),
				"whitelisted host must be allowed regardless of case");
		assertFalse(handler.isResolutionAllowed(URI.createURI("http://evil.test/model.json"), Map.of()),
				"non-whitelisted host must stay blocked");
	}

	@Test
	public void perCallOptionOverridesEvenWithEmptyWhitelist() {
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl();

		assertTrue(
				handler.isResolutionAllowed(METADATA_URI,
						Map.of(EMFUriHandlerConstants.OPTION_ALLOW_URI_RESOLUTION, Boolean.TRUE)),
				"the per-call allow option must permit the current URI even without a whitelist");
	}

	@Test
	public void perCallOptionMustBeBooleanTrue() {
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl();

		assertFalse(
				handler.isResolutionAllowed(METADATA_URI,
						Map.of(EMFUriHandlerConstants.OPTION_ALLOW_URI_RESOLUTION, "true")),
				"a non-Boolean.TRUE value must not unlock resolution");
	}

	@Test
	public void subdomainWildcardMatchesSubdomainsButNotApexOrSiblings() {
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl(Set.of("*.mydomain.com"));

		assertTrue(handler.isResolutionAllowed(URI.createURI("http://a.mydomain.com/x"), Map.of()),
				"a direct subdomain must match");
		assertTrue(handler.isResolutionAllowed(URI.createURI("https://a.b.mydomain.com/x"), Map.of()),
				"a nested subdomain must match");
		assertFalse(handler.isResolutionAllowed(URI.createURI("http://mydomain.com/x"), Map.of()),
				"the apex must NOT match a *. wildcard");
		assertFalse(handler.isResolutionAllowed(URI.createURI("http://evilmydomain.com/x"), Map.of()),
				"a look-alike host must not match (suffix is anchored on the dot)");
	}

	@Test
	public void bareStarAllowsEveryHost() {
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl(Set.of("*"));

		assertTrue(handler.isResolutionAllowed(METADATA_URI, Map.of()),
				"a bare '*' must permit every host");
		assertTrue(handler.isResolutionAllowed(URI.createURI("http://anything.example/x"), Map.of()),
				"a bare '*' must permit every host");
	}

	@Test
	public void liveAllowListPicksUpAConfigurationThatArrivesLater() {
		// the ResourceSet - and with it the handler - is created before Config Admin delivers the
		// configuration, which is the ordering that made the snapshot handler block forever (issue #100)
		AtomicReference<HostAllowList> allowList = new AtomicReference<>(HostAllowList.BLOCK_ALL);
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl(allowList::get);
		URI uri = URI.createURI("https://api.example.test/v1/models");

		assertFalse(handler.isResolutionAllowed(uri, Map.of()),
				"before the configuration arrives the handler must block");

		allowList.set(HostAllowList.of(Set.of("api.example.test")));

		assertTrue(handler.isResolutionAllowed(uri, Map.of()),
				"the already-created handler must see the configuration that arrived afterwards");
	}

	@Test
	public void liveAllowListBlocksAgainWhenTheConfigurationIsWithdrawn() {
		AtomicReference<HostAllowList> allowList = new AtomicReference<>(
				HostAllowList.of(Set.of("api.example.test")));
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl(allowList::get);
		URI uri = URI.createURI("https://api.example.test/v1/models");

		assertTrue(handler.isResolutionAllowed(uri, Map.of()), "the configured host is allowed");

		allowList.set(HostAllowList.BLOCK_ALL);

		assertFalse(handler.isResolutionAllowed(uri, Map.of()),
				"removing the configuration must block resolutions through existing ResourceSets");
	}

	@Test
	public void supplierReturningNullBlocksInsteadOfFailing() {
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl(() -> null);

		assertFalse(handler.isResolutionAllowed(METADATA_URI, Map.of()),
				"a supplier without an allow-list must close the guard, not open it");
	}

	@Test
	public void perCallOptionStillOverridesALiveAllowList() {
		RestfulURIHandlerImpl handler = new RestfulURIHandlerImpl(() -> HostAllowList.BLOCK_ALL);

		assertTrue(
				handler.isResolutionAllowed(METADATA_URI,
						Map.of(EMFUriHandlerConstants.OPTION_ALLOW_URI_RESOLUTION, Boolean.TRUE)),
				"the escape hatch must keep working with a live allow-list");
	}
}
