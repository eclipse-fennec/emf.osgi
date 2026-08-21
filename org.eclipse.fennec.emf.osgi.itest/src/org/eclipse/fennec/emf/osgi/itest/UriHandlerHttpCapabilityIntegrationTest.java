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
package org.eclipse.fennec.emf.osgi.itest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * Integration test for the {@code emf.uri.handler.http} capability property. It verifies that a
 * {@link ResourceSet}/{@link ResourceSetFactory} produced by the Fennec factory only advertises
 * the property - and is therefore selectable via {@code (emf.uri.handler.http=true)} - once the
 * REST URI handler is configured with a non-empty host whitelist (PID
 * {@code org.eclipse.fennec.emf.osgi.urihandler.http}). The host list itself is never exposed as a
 * service property.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@RequireConfigurationAdmin
public class UriHandlerHttpCapabilityIntegrationTest {

	private static final String HTTP_URI_HANDLER_PID = "org.eclipse.fennec.emf.osgi.urihandler.http";
	private static final String HTTP_CAPABILITY_FILTER = "(" + EMFNamespaces.PROP_URI_HANDLER_HTTP + "=true)";

	@InjectService
	ConfigurationAdmin ca;

	@Test
	public void httpCapabilityAdvertisedAndFilterableOnlyWhenWhitelistConfigured(
			@InjectService(filter = HTTP_CAPABILITY_FILTER, cardinality = 0) ServiceAware<ResourceSetFactory> factoryAware,
			@InjectService(filter = HTTP_CAPABILITY_FILTER, cardinality = 0) ServiceAware<ResourceSet> resourceSetAware)
			throws Exception {

		// Default: no configuration -> nothing advertises http capability.
		assertTrue(factoryAware.getServices().isEmpty(),
				"no ResourceSetFactory should match " + HTTP_CAPABILITY_FILTER + " before configuration");
		assertTrue(resourceSetAware.getServices().isEmpty(),
				"no ResourceSet should match " + HTTP_CAPABILITY_FILTER + " before configuration");

		Configuration config = ca.getConfiguration(HTTP_URI_HANDLER_PID, "?");
		try {
			Dictionary<String, Object> props = new Hashtable<>();
			props.put("allowedHosts", new String[] { "models.example.com" });
			config.update(props);

			// A whitelist is configured -> the factory and the ResourceSet become selectable by the filter.
			ResourceSetFactory factory = factoryAware.waitForService(5000);
			assertNotNull(factory, "a ResourceSetFactory must be selectable via " + HTTP_CAPABILITY_FILTER
					+ " once a whitelist is configured");
			assertTrue(
					advertisesTrue(
							factoryAware.getServiceReference().getProperty(EMFNamespaces.PROP_URI_HANDLER_HTTP)),
					"the factory must advertise the http capability property as true");

			ResourceSet resourceSet = resourceSetAware.waitForService(5000);
			assertNotNull(resourceSet, "a ResourceSet must be selectable via " + HTTP_CAPABILITY_FILTER
					+ " once a whitelist is configured");

			// The host list must not leak into the service properties.
			assertTrue(resourceSetAware.getServiceReference().getProperty("allowedHosts") == null,
					"the configured host list must not be exposed as a service property");
		} finally {
			config.delete();
		}

		// Configuration withdrawn -> the capability disappears again.
		awaitEmpty(factoryAware, 5000);
		awaitEmpty(resourceSetAware, 5000);
	}

	/**
	 * Exercises the {@code @Modified} path: an existing configuration whose {@code allowedHosts} is
	 * changed at runtime must toggle the {@code emf.uri.handler.http} capability accordingly -
	 * emptying the list withdraws it, refilling it re-advertises it - without deleting the
	 * configuration.
	 */
	@Test
	public void httpCapabilityTracksConfigurationChangesAtRuntime(
			@InjectService(filter = HTTP_CAPABILITY_FILTER, cardinality = 0) ServiceAware<ResourceSetFactory> factoryAware)
			throws Exception {

		assertTrue(factoryAware.getServices().isEmpty(),
				"no ResourceSetFactory should match " + HTTP_CAPABILITY_FILTER + " before configuration");

		Configuration config = ca.getConfiguration(HTTP_URI_HANDLER_PID, "?");
		try {
			// 1) initial non-empty whitelist -> capability advertised
			config.update(hosts("a.example.com"));
			assertNotNull(factoryAware.waitForService(5000),
					"capability must appear once a non-empty whitelist is configured");

			// 2) modify the SAME configuration to an empty list -> capability withdrawn
			config.update(hosts());
			awaitEmpty(factoryAware, 5000);

			// 3) modify again to a different non-empty whitelist -> capability re-advertised
			config.update(hosts("b.example.com"));
			assertNotNull(factoryAware.waitForService(5000),
					"capability must reappear when the whitelist is refilled at runtime");
		} finally {
			config.delete();
		}

		awaitEmpty(factoryAware, 5000);
	}

	private static Dictionary<String, Object> hosts(String... allowedHosts) {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("allowedHosts", allowedHosts);
		return props;
	}

	/**
	 * The capability property is aggregated by the ResourceSetFactory's ServicePropertyContext and
	 * therefore surfaces as a {@code String[]} (e.g. {@code ["true"]}), not a raw {@code Boolean};
	 * accept either shape as long as it carries {@code "true"}.
	 */
	private static boolean advertisesTrue(Object value) {
		if (value instanceof String[] array) {
			return Arrays.asList(array).contains("true");
		}
		return "true".equals(String.valueOf(value));
	}

	private static void awaitEmpty(ServiceAware<?> aware, long timeoutMillis) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!aware.getServices().isEmpty() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertTrue(aware.getServices().isEmpty(),
				"the http capability must be withdrawn once the whitelist configuration is deleted");
	}
}
