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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.fennec.emf.osgi.constants.EMFUriHandlerConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Tests that {@link RestfulURIHandlerImpl} surfaces the HTTP error response
 * body in the thrown {@link IOException} (see issue #48)
 *
 * @author Data In Motion
 */
public class RestfulURIHandlerImplTest {

	private static final String ERROR_BODY = "{\"error\":\"validation failed\",\"field\":\"name\"}";

	private HttpServer server;
	private RestfulURIHandlerImpl handler;

	@BeforeEach
	public void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.start();
		// The read path is blocked by default (SSRF guard); allow the test's loopback host so these
		// tests keep exercising the handler's HTTP error-body behavior.
		handler = new RestfulURIHandlerImpl(Set.of("localhost"));
	}

	@AfterEach
	public void tearDown() {
		server.stop(0);
	}

	private URI uri(String path) {
		return URI.createURI("http://localhost:" + server.getAddress().getPort() + path);
	}

	private void respond(String path, int status, String body) {
		server.createContext(path, exchange -> {
			// drain the request body, otherwise the client may see a broken pipe
			exchange.getRequestBody().readAllBytes();
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
	}

	@Test
	public void testCreateOutputStreamErrorBodyInException() throws IOException {
		respond("/reject", 400, ERROR_BODY);
		OutputStream out = handler.createOutputStream(uri("/reject"),
				Map.of(EMFUriHandlerConstants.OPTION_HTTP_METHOD, "POST"));
		out.write("payload".getBytes(StandardCharsets.UTF_8));
		IOException exception = assertThrows(IOException.class, out::close);
		assertTrue(exception.getMessage().contains("POST failed with HTTP response code 400"),
				"unexpected message: " + exception.getMessage());
		assertTrue(exception.getMessage().contains(ERROR_BODY), "error body missing: " + exception.getMessage());
	}

	@Test
	public void testCreateInputStreamErrorBodyInException() throws IOException {
		respond("/reject", 404, ERROR_BODY);
		InputStream in = handler.createInputStream(uri("/reject"), Map.of());
		IOException exception = assertThrows(IOException.class, in::close);
		assertTrue(exception.getMessage().contains("GET failed with HTTP response code 404"),
				"unexpected message: " + exception.getMessage());
		assertTrue(exception.getMessage().contains(ERROR_BODY), "error body missing: " + exception.getMessage());
	}

	@Test
	public void testDeleteErrorBodyInException() {
		respond("/reject", 409, ERROR_BODY);
		IOException exception = assertThrows(IOException.class, () -> handler.delete(uri("/reject"), Map.of()));
		assertTrue(exception.getMessage().contains("DELETE failed with HTTP response code 409"),
				"unexpected message: " + exception.getMessage());
		assertTrue(exception.getMessage().contains(ERROR_BODY), "error body missing: " + exception.getMessage());
	}

	@Test
	public void testEmptyErrorBodyKeepsPlainMessage() throws IOException {
		respond("/empty", 500, "");
		InputStream in = handler.createInputStream(uri("/empty"), Map.of());
		IOException exception = assertThrows(IOException.class, in::close);
		assertEquals("GET failed with HTTP response code 500", exception.getMessage());
	}

	@Test
	public void testErrorBodyIsCapped() throws IOException {
		respond("/huge", 400, "x".repeat(64 * 1024));
		InputStream in = handler.createInputStream(uri("/huge"), Map.of());
		IOException exception = assertThrows(IOException.class, in::close);
		assertTrue(exception.getMessage().length() <= 8 * 1024 + 100,
				"message not capped, length: " + exception.getMessage().length());
	}

	@Test
	public void testSuccessfulRequestsStillWork() throws IOException {
		respond("/ok", 200, "content");
		try (InputStream in = handler.createInputStream(uri("/ok"), Map.of())) {
			assertEquals("content", new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}
}
