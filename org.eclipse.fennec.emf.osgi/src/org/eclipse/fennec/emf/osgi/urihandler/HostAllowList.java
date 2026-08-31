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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An immutable, already normalized host allow-list for outbound {@code http}/{@code https}
 * resolution.
 * <p>
 * Instances are built <em>once per configuration change</em> through {@link #of(Collection)}, which
 * performs all trimming, lower-casing and pattern classification. {@link #isAllowed(String)} is
 * therefore a pure lookup and can be called on every resolution without repeating the parsing work.
 * Because the instance is immutable it can be handed to any number of
 * {@link RestfulURIHandlerImpl} instances and safely published across threads.
 *
 * @author Mark Hoffmann
 * @since 31.08.2026
 */
public final class HostAllowList {

	private static final Logger LOG = Logger.getLogger(HostAllowList.class.getName());

	/** The secure default: no host may be resolved unless a call opts in explicitly. */
	public static final HostAllowList BLOCK_ALL = new HostAllowList(false, Set.of(), List.of());

	/** {@code true} if a bare {@code "*"} was configured - permits every host (SSRF guard disabled). */
	private final boolean allowAllHosts;
	/** Exact host names (lower-cased) permitted for outbound http/https resolution. */
	private final Set<String> exactHosts;
	/** Suffixes (lower-cased, including the leading dot, e.g. {@code ".mydomain.com"}) from {@code *.} entries. */
	private final List<String> suffixes;

	private HostAllowList(boolean allowAllHosts, Set<String> exactHosts, List<String> suffixes) {
		this.allowAllHosts = allowAllHosts;
		this.exactHosts = exactHosts;
		this.suffixes = suffixes;
	}

	/**
	 * Normalizes the given host patterns into an allow-list. {@code null} entries and blank entries
	 * are ignored; every remaining entry is trimmed and lower-cased. Each entry may be:
	 * <ul>
	 * <li>an exact host name, e.g. {@code models.example.com};</li>
	 * <li>a subdomain wildcard {@code *.mydomain.com}, matching any host that has at least one label
	 * before {@code .mydomain.com} (the apex {@code mydomain.com} is <em>not</em> matched - list it
	 * explicitly if needed);</li>
	 * <li>a bare {@code *}, which permits <strong>every</strong> host. This disables the SSRF
	 * protection entirely and is logged as a warning; use it only for trusted, closed
	 * environments.</li>
	 * </ul>
	 *
	 * @param hostPatterns the configured host patterns, may be {@code null} or empty - both yield
	 *                     {@link #BLOCK_ALL}
	 * @return the normalized allow-list, never {@code null}
	 */
	public static HostAllowList of(Collection<String> hostPatterns) {
		if (hostPatterns == null || hostPatterns.isEmpty()) {
			return BLOCK_ALL;
		}
		boolean allowAll = false;
		Set<String> exact = new HashSet<>();
		List<String> wildcardSuffixes = new ArrayList<>();
		for (String host : hostPatterns) {
			if (host == null || host.isBlank()) {
				continue;
			}
			String normalized = host.trim().toLowerCase(Locale.ROOT);
			if (normalized.equals("*")) {
				allowAll = true;
				LOG.warning(
						"REST URI handler configured with wildcard host '*': ALL outbound http(s) resolution "
								+ "is permitted, which disables SSRF protection. Use an explicit host allow-list instead.");
			} else if (normalized.startsWith("*.")) {
				wildcardSuffixes.add(normalized.substring(1)); // keep the leading dot: ".mydomain.com"
			} else {
				exact.add(normalized);
			}
		}
		if (!allowAll && exact.isEmpty() && wildcardSuffixes.isEmpty()) {
			return BLOCK_ALL;
		}
		return new HostAllowList(allowAll, Set.copyOf(exact), List.copyOf(wildcardSuffixes));
	}

	/**
	 * Returns whether the given host is permitted for outbound resolution. The comparison is
	 * case-insensitive; the host must not carry a port.
	 *
	 * @param host the host name of the URI about to be resolved, may be {@code null}
	 * @return {@code true} if the host matches a bare {@code *}, an exact entry or a {@code *.suffix}
	 *         entry
	 */
	public boolean isAllowed(String host) {
		if (allowAllHosts) {
			return true;
		}
		if (host == null) {
			return false;
		}
		String normalizedHost = host.toLowerCase(Locale.ROOT);
		if (exactHosts.contains(normalizedHost)) {
			return true;
		}
		for (String suffix : suffixes) {
			// endsWith(".mydomain.com") already requires a label before the dot, so the apex does not match
			if (normalizedHost.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether this allow-list permits nothing at all, i.e. whether outbound resolution is
	 * blocked unless a call opts in via
	 * {@link org.eclipse.fennec.emf.osgi.constants.EMFUriHandlerConstants#OPTION_ALLOW_URI_RESOLUTION}.
	 *
	 * @return {@code true} if no host pattern is configured
	 */
	public boolean isEmpty() {
		return !allowAllHosts && exactHosts.isEmpty() && suffixes.isEmpty();
	}
}
