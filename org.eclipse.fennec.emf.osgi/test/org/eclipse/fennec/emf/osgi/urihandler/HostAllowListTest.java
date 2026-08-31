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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for {@link HostAllowList}, the normalized form of the configured host patterns.
 * The normalization itself happens exactly once, in {@link HostAllowList#of(java.util.Collection)},
 * so these tests cover what {@link RestfulURIHandlerImpl} then only has to look up.
 */
public class HostAllowListTest {

	@Test
	public void emptyInputYieldsTheBlockAllInstance() {
		assertSame(HostAllowList.BLOCK_ALL, HostAllowList.of(null), "null must fall back to block-all");
		assertSame(HostAllowList.BLOCK_ALL, HostAllowList.of(List.of()), "an empty list blocks everything");
		assertSame(HostAllowList.BLOCK_ALL, HostAllowList.of(Arrays.asList("  ", null)),
				"blank and null entries must not create an allow-list");

		assertTrue(HostAllowList.BLOCK_ALL.isEmpty(), "the block-all instance reports itself as empty");
		assertFalse(HostAllowList.BLOCK_ALL.isAllowed("example.test"), "block-all permits no host");
	}

	@Test
	public void normalizesCaseAndSurroundingWhitespace() {
		HostAllowList allowList = HostAllowList.of(List.of("  Example.Test  "));

		assertTrue(allowList.isAllowed("example.test"), "entries are trimmed and lower-cased");
		assertTrue(allowList.isAllowed("EXAMPLE.TEST"), "the host is matched case-insensitively");
		assertFalse(allowList.isEmpty(), "a configured host makes the allow-list non-empty");
	}

	@Test
	public void subdomainWildcardMatchesSubdomainsButNotApexOrSiblings() {
		HostAllowList allowList = HostAllowList.of(Set.of("*.mydomain.com"));

		assertTrue(allowList.isAllowed("a.mydomain.com"), "a direct subdomain must match");
		assertTrue(allowList.isAllowed("a.b.mydomain.com"), "a nested subdomain must match");
		assertFalse(allowList.isAllowed("mydomain.com"), "the apex must NOT match a *. wildcard");
		assertFalse(allowList.isAllowed("evilmydomain.com"),
				"a look-alike host must not match (the suffix is anchored on the dot)");
	}

	@Test
	public void bareStarAllowsEveryHostIncludingAnUnknownOne() {
		HostAllowList allowList = HostAllowList.of(List.of("*"));

		assertTrue(allowList.isAllowed("169.254.169.254"), "a bare '*' must permit every host");
		assertTrue(allowList.isAllowed("anything.example"), "a bare '*' must permit every host");
		assertFalse(allowList.isEmpty(), "a bare '*' is a configured policy, not an empty one");
	}

	@Test
	public void nullHostIsNeverAllowedUnlessEverythingIs() {
		assertFalse(HostAllowList.of(List.of("example.test")).isAllowed(null),
				"a URI without a host must not match an allow-list entry");
		assertTrue(HostAllowList.of(List.of("*")).isAllowed(null),
				"a bare '*' disables the guard entirely, host or not");
	}
}
