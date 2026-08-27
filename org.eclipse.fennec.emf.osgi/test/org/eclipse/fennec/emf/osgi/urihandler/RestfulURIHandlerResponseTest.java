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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.fennec.emf.osgi.constants.EMFUriHandlerConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Tests that {@link RestfulURIHandlerImpl} fills the
 * {@link URIConverter#OPTION_RESPONSE response map} correctly and consistently:
 * the {@code Last-Modified} HTTP-date is parsed as a date (issue #93), the HTTP
 * status is published on the read and delete paths too (issue #94), and the keys
 * involved are part of the public {@link EMFUriHandlerConstants} API (issue #95)
 *
 * @author Data In Motion
 */
public class RestfulURIHandlerResponseTest {

	/** A spec-compliant HTTP-date as defined by RFC 7231 */
	private static final String LAST_MODIFIED_HTTP_DATE = "Thu, 27 Aug 2026 07:32:33 GMT";
	private static final long LAST_MODIFIED_EPOCH_MILLIS = ZonedDateTime
			.of(2026, 8, 27, 7, 32, 33, 0, ZoneOffset.UTC).toInstant().toEpochMilli();

	private HttpServer server;
	private RestfulURIHandlerImpl handler;
	private Map<Object, Object> response;
	private List<LogRecord> logRecords;
	private Handler logCollector;
	private Logger handlerLogger;

	@BeforeEach
	public void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.start();
		handler = new RestfulURIHandlerImpl(Set.of("localhost"));
		response = new HashMap<>();
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
		handlerLogger = Logger.getLogger(RestfulURIHandlerImpl.class.getName());
		handlerLogger.addHandler(logCollector);
	}

	@AfterEach
	public void tearDown() {
		handlerLogger.removeHandler(logCollector);
		server.stop(0);
	}

	private URI uri(String path) {
		return URI.createURI("http://localhost:" + server.getAddress().getPort() + path);
	}

	private Map<?, ?> options() {
		return Map.of(URIConverter.OPTION_RESPONSE, response);
	}

	private void respond(String path, int status, String body, Map<String, String> headers) {
		server.createContext(path, exchange -> {
			exchange.getRequestBody().readAllBytes();
			headers.forEach(exchange.getResponseHeaders()::set);
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
	}

	private Object headerValues(String name) {
		return response.entrySet().stream().filter(e -> e.getKey() instanceof String key && key.equalsIgnoreCase(name))
				.map(Entry::getValue).findFirst().orElse(null);
	}

	private void assertNoSevereLog() {
		assertTrue(logRecords.stream().noneMatch(r -> r.getLevel().intValue() >= Level.SEVERE.intValue()),
				() -> "unexpected SEVERE log: " + logRecords.stream().map(LogRecord::getMessage).toList());
	}

	// issue #93 - Last-Modified is an HTTP-date, not a number

	@Test
	public void testLastModifiedHttpDateIsParsedOnRead() throws IOException {
		respond("/ok", 200, "content", Map.of("Last-Modified", LAST_MODIFIED_HTTP_DATE));
		try (InputStream in = handler.createInputStream(uri("/ok"), options())) {
			in.readAllBytes();
		}
		assertEquals(LAST_MODIFIED_EPOCH_MILLIS, response.get(URIConverter.RESPONSE_TIME_STAMP_PROPERTY));
		assertNoSevereLog();
	}

	@Test
	public void testLastModifiedHttpDateIsParsedOnWrite() throws IOException {
		respond("/put", 200, "", Map.of("Last-Modified", LAST_MODIFIED_HTTP_DATE));
		OutputStream out = handler.createOutputStream(uri("/put"), options());
		out.write("payload".getBytes(StandardCharsets.UTF_8));
		out.close();
		assertEquals(LAST_MODIFIED_EPOCH_MILLIS, response.get(URIConverter.RESPONSE_TIME_STAMP_PROPERTY));
		assertNoSevereLog();
	}

	@Test
	public void testMissingLastModifiedLeavesPropertyUnset() throws IOException {
		respond("/nodate", 200, "content", Map.of());
		try (InputStream in = handler.createInputStream(uri("/nodate"), options())) {
			in.readAllBytes();
		}
		assertFalse(response.containsKey(URIConverter.RESPONSE_TIME_STAMP_PROPERTY),
				"timestamp must not be set without a Last-Modified header");
		assertNoSevereLog();
	}

	@Test
	public void testMalformedLastModifiedIsIgnoredSilently() throws IOException {
		respond("/broken", 200, "content", Map.of("Last-Modified", "not-a-date"));
		try (InputStream in = handler.createInputStream(uri("/broken"), options())) {
			in.readAllBytes();
		}
		assertFalse(response.containsKey(URIConverter.RESPONSE_TIME_STAMP_PROPERTY),
				"timestamp must not be set for an unparseable Last-Modified header");
		assertNoSevereLog();
	}

	// issue #94 - the HTTP status must be published on every path

	@Test
	public void testReadPublishesHttpStatus() throws IOException {
		respond("/ok", 200, "content", Map.of());
		try (InputStream in = handler.createInputStream(uri("/ok"), options())) {
			in.readAllBytes();
		}
		assertEquals(200, response.get(EMFUriHandlerConstants.RESPONSE_HTTP_STATUS));
	}

	@Test
	public void testReadPublishesHttpStatusOnFailure() throws IOException {
		respond("/missing", 404, "gone", Map.of());
		InputStream in = handler.createInputStream(uri("/missing"), options());
		assertThrows(IOException.class, in::close);
		assertEquals(404, response.get(EMFUriHandlerConstants.RESPONSE_HTTP_STATUS));
	}

	@Test
	public void testReadCopiesResponseHeaders() throws IOException {
		respond("/headers", 200, "content", Map.of("X-Fennec-Test", "value"));
		try (InputStream in = handler.createInputStream(uri("/headers"), options())) {
			in.readAllBytes();
		}
		// header names are case-insensitive and the key is whatever the server sent on the wire
		assertEquals(List.of("value"), headerValues("X-Fennec-Test"));
		assertFalse(response.containsKey(null), "the status line's null header key must not leak into the map");
	}

	@Test
	public void testExistsPublishesHttpStatus() {
		respond("/missing", 404, "", Map.of());
		assertFalse(handler.exists(uri("/missing"), options()));
		assertEquals(404, response.get(EMFUriHandlerConstants.RESPONSE_HTTP_STATUS));
	}

	@Test
	public void testExistsPublishesHttpStatusAndTimestampOnSuccess() {
		respond("/ok", 200, "", Map.of("Last-Modified", LAST_MODIFIED_HTTP_DATE));
		assertTrue(handler.exists(uri("/ok"), options()));
		assertEquals(200, response.get(EMFUriHandlerConstants.RESPONSE_HTTP_STATUS));
		assertEquals(LAST_MODIFIED_EPOCH_MILLIS, response.get(URIConverter.RESPONSE_TIME_STAMP_PROPERTY));
	}

	@Test
	public void testDeletePublishesHttpStatusOnFailure() {
		respond("/forbidden", 403, "nope", Map.of());
		assertThrows(IOException.class, () -> handler.delete(uri("/forbidden"), options()));
		assertEquals(403, response.get(EMFUriHandlerConstants.RESPONSE_HTTP_STATUS));
	}

	@Test
	public void testDeletePublishesHttpStatusOnSuccess() throws IOException {
		respond("/gone", 204, "", Map.of());
		handler.delete(uri("/gone"), options());
		assertEquals(204, response.get(EMFUriHandlerConstants.RESPONSE_HTTP_STATUS));
	}

	@Test
	public void testMissingResponseMapIsTolerated() throws IOException {
		respond("/ok", 200, "content", Map.of("Last-Modified", LAST_MODIFIED_HTTP_DATE));
		try (InputStream in = handler.createInputStream(uri("/ok"), Map.of())) {
			assertEquals("content", new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
		respond("/gone", 204, "", Map.of());
		handler.delete(uri("/gone"), Map.of());
		assertTrue(handler.exists(uri("/ok"), Map.of()));
	}

	// issue #95 - the request keys must be public API too

	@Test
	public void testEClassOptionSetsContentClassHeader() throws IOException {
		AtomicReference<String> contentClass = new AtomicReference<>();
		server.createContext("/typed", exchange -> {
			contentClass.set(exchange.getRequestHeaders().getFirst(EMFUriHandlerConstants.HEADER_CONTENT_CLASS));
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(204, -1);
			exchange.close();
		});
		OutputStream out = handler.createOutputStream(uri("/typed"),
				Map.of(EMFUriHandlerConstants.OPTION_ECLASS, "http://example.org/model#//Person"));
		out.write("payload".getBytes(StandardCharsets.UTF_8));
		out.close();
		assertEquals("http://example.org/model#//Person", contentClass.get());
	}

	@Test
	public void testPublicConstantValuesArePreserved() {
		assertEquals("HTTPResponseCode", EMFUriHandlerConstants.RESPONSE_HTTP_STATUS);
		assertEquals("EClass", EMFUriHandlerConstants.OPTION_ECLASS);
		assertEquals("Content-Class", EMFUriHandlerConstants.HEADER_CONTENT_CLASS);
	}
}
