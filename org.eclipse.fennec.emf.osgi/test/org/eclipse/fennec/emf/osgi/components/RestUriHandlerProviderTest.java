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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.urihandler.HostAllowList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.sun.net.httpserver.HttpServer;

/**
 * Plain unit tests for {@link RestUriHandlerProvider}, covering the lifecycle that issue #100
 * exposed: a {@code ResourceSet} - and with it a URI handler - is usually created <em>before</em>
 * Config Admin delivers the {@code org.eclipse.fennec.emf.osgi.urihandler.http} configuration. The
 * handler therefore must read the allow-list live from the provider instead of snapshotting it at
 * construction, while the normalization of the configured patterns still happens only once per
 * configuration.
 * <p>
 * The allowed path is exercised against a loopback {@link HttpServer}, so the tests never leave the
 * machine; the blocked path never opens a connection at all.
 */
public class RestUriHandlerProviderTest {

	private static final String BODY = "ok";

	private HttpServer server;
	private BundleContext bundleContext;
	@SuppressWarnings("unchecked")
	private final ServiceRegistration<ResourceSetConfigurator> capabilityRegistration = mock(
			ServiceRegistration.class);

	private List<LogRecord> logRecords;
	private Handler logCollector;
	private Logger allowListLogger;

	@BeforeEach
	public void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/model", exchange -> {
			byte[] payload = BODY.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, payload.length);
			exchange.getResponseBody().write(payload);
			exchange.close();
		});
		server.start();

		bundleContext = mock(BundleContext.class);
		when(bundleContext.registerService(eq(ResourceSetConfigurator.class), any(ResourceSetConfigurator.class),
				any())).thenReturn(capabilityRegistration);

		logRecords = new ArrayList<>();
		logCollector = new Handler() {

			@Override
			public void publish(LogRecord record) {
				logRecords.add(record);
			}

			@Override
			public void flush() {
				// nothing to do
			}

			@Override
			public void close() {
				// nothing to do
			}
		};
		allowListLogger = Logger.getLogger(HostAllowList.class.getName());
		allowListLogger.addHandler(logCollector);
	}

	@AfterEach
	public void tearDown() {
		allowListLogger.removeHandler(logCollector);
		server.stop(0);
	}

	/** Builds a {@link RestUriHandlerConfig} with the given allowed hosts. */
	private static RestUriHandlerConfig config(String... allowedHosts) {
		return (RestUriHandlerConfig) Proxy.newProxyInstance(RestUriHandlerConfig.class.getClassLoader(),
				new Class<?>[] { RestUriHandlerConfig.class },
				(proxy, method, args) -> "allowedHosts".equals(method.getName()) ? allowedHosts : null);
	}

	private URI uri() {
		return URI.createURI("http://localhost:" + server.getAddress().getPort() + "/model");
	}

	private static String read(URIHandler handler, URI uri) throws IOException {
		try (InputStream in = handler.createInputStream(uri, Map.of())) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static void assertBlocked(URIHandler handler, URI uri) {
		IOException failure = assertThrows(IOException.class, () -> handler.createInputStream(uri, Map.of()),
				"resolution must be refused before a connection is opened");
		assertTrue(failure.getMessage().contains("Blocked outbound"),
				"the failure must come from the access guard: " + failure.getMessage());
	}

	@Test
	public void handlerCreatedBeforeTheConfigurationArrivesPicksItUp() throws IOException {
		RestUriHandlerProvider provider = new RestUriHandlerProvider(bundleContext, config());
		// the consumer obtained its ResourceSet while the provider was still unconfigured
		URIHandler handler = provider.getURIHandler();
		assertBlocked(handler, uri());

		provider.modified(config("localhost"));

		assertEquals(BODY, read(handler, uri()),
				"the handler created before the configuration must resolve the allow-listed host");
	}

	@Test
	public void handlerCreatedAfterTheConfigurationKeepsWorking() throws IOException {
		RestUriHandlerProvider provider = new RestUriHandlerProvider(bundleContext, config("localhost"));

		assertEquals(BODY, read(provider.getURIHandler(), uri()),
				"a handler created after the configuration must resolve the allow-listed host");
	}

	@Test
	public void withdrawingTheConfigurationBlocksExistingHandlers() throws IOException {
		RestUriHandlerProvider provider = new RestUriHandlerProvider(bundleContext, config("localhost"));
		URIHandler handler = provider.getURIHandler();
		assertEquals(BODY, read(handler, uri()), "the configured host resolves");

		// an emptied configuration is delivered as a modification ...
		provider.modified(config());
		assertBlocked(handler, uri());

		provider.modified(config("localhost"));
		assertEquals(BODY, read(handler, uri()), "the host is allowed again");

		// ... a deleted one deactivates the component, which must close the allow-list as well
		provider.deactivate();
		assertBlocked(handler, uri());
	}

	@Test
	public void capabilityMarkerFollowsTheConfiguration() {
		RestUriHandlerProvider provider = new RestUriHandlerProvider(bundleContext, config());
		verify(bundleContext, times(0)).registerService(eq(ResourceSetConfigurator.class),
				any(ResourceSetConfigurator.class), any());

		provider.modified(config("localhost"));
		verify(bundleContext, times(1)).registerService(eq(ResourceSetConfigurator.class),
				any(ResourceSetConfigurator.class), any());

		provider.modified(config("localhost", "models.example.test"));
		verify(bundleContext, times(1)).registerService(eq(ResourceSetConfigurator.class),
				any(ResourceSetConfigurator.class), any());
		verify(capabilityRegistration, times(0)).unregister();

		provider.modified(config());
		verify(capabilityRegistration, times(1)).unregister();
	}

	@Test
	public void blankOnlyConfigurationStaysBlockedAndUnadvertised() {
		RestUriHandlerProvider provider = new RestUriHandlerProvider(bundleContext, config("  ", ""));

		assertBlocked(provider.getURIHandler(), uri());
		verify(bundleContext, times(0)).registerService(eq(ResourceSetConfigurator.class),
				any(ResourceSetConfigurator.class), any());
	}

	@Test
	public void hostPatternsAreNormalizedPerConfigurationNotPerResolution() throws IOException {
		RestUriHandlerProvider provider = new RestUriHandlerProvider(bundleContext, config("*"));

		// three handlers, several resolutions - the wildcard warning is emitted by the normalization,
		// so it must appear exactly once: when the configuration was applied
		for (int i = 0; i < 3; i++) {
			assertEquals(BODY, read(provider.getURIHandler(), uri()), "the bare '*' permits every host");
		}

		assertEquals(1, warnings().size(),
				"normalization must run once per configuration, not per handler or per resolution");

		provider.modified(config("*"));

		assertEquals(2, warnings().size(), "a new configuration normalizes again");
	}

	private List<LogRecord> warnings() {
		return logRecords.stream().filter(record -> record.getLevel() == Level.WARNING).toList();
	}

	@Test
	public void deactivationUnregistersTheCapabilityMarker() {
		RestUriHandlerProvider provider = new RestUriHandlerProvider(bundleContext, config("localhost"));

		provider.deactivate();

		verify(capabilityRegistration, times(1)).unregister();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void capabilityCarriesTheHttpPropertyButNotTheHostList() {
		ArgumentCaptor<Dictionary> properties = ArgumentCaptor.forClass(Dictionary.class);

		new RestUriHandlerProvider(bundleContext, config("localhost"));

		verify(bundleContext).registerService(eq(ResourceSetConfigurator.class),
				any(ResourceSetConfigurator.class), properties.capture());
		assertEquals(Boolean.TRUE, properties.getValue().get(EMFNamespaces.PROP_URI_HANDLER_HTTP),
				"the marker must advertise the http capability");
		assertNull(properties.getValue().get("allowedHosts"),
				"the configured host list must never become a service property");
	}
}
