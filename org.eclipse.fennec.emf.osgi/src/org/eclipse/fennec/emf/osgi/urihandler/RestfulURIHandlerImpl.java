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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;
import org.eclipse.fennec.emf.osgi.constants.EMFUriHandlerConstants;

/**
 * URI Handler with basic authentication
 * 
 * @author Juergen Albert
 * @since 17.12.2012
 */
public class RestfulURIHandlerImpl extends URIHandlerImpl {

	/** ERROR_WITH_RESPONSE_CODE */
	private static final String ERROR_WITH_RESPONSE_CODE = " failed with HTTP response code ";
	/** ERROR_LAST_MODIFIED */
	private static final String ERROR_LAST_MODIFIED = "Error reading last modified header from the response";
	/** SCHEMA_HTTPS */
	private static final String SCHEMA_HTTPS = "https";
	/** SCHEMA_HTTP */
	private static final String SCHEMA_HTTP = "http";
	/** HEADER_CONTENT_LENGTH */
	private static final String HEADER_CONTENT_LENGTH = "Content-Length";
	/** HTTP_HEAD */
	private static final String HTTP_HEAD = "HEAD";
	/** HEADER_ALLOW */
	private static final String HEADER_ALLOW = "Allow";
	/** HTTP_OPTIONS */
	private static final String HTTP_OPTIONS = "OPTIONS";
	/** HTTP_DELETE */
	private static final String HTTP_DELETE = "DELETE";
	/** HEADER_LAST_MODIFIED */
	private static final String HEADER_LAST_MODIFIED = "Last-Modified";
	/** HTTP_PUT */
	private static final String HTTP_PUT = "PUT";
	/** Upper bound for the error body appended to exception messages */
	private static final int MAX_ERROR_BODY_BYTES = 8 * 1024;
	private static final Logger LOG = Logger.getLogger(RestfulURIHandlerImpl.class.getName());

	/** {@code true} if a bare {@code "*"} was configured - permits every host (SSRF guard disabled). */
	private final boolean allowAllHosts;
	/** Exact host names (lower-cased) permitted for outbound http/https resolution. */
	private final Set<String> allowedHosts;
	/** Suffixes (lower-cased, including the leading dot, e.g. {@code ".mydomain.com"}) from {@code *.} entries. */
	private final List<String> allowedSuffixes;

	/**
	 * Creates a handler that blocks all outbound http(s) resolution by default. Individual, trusted
	 * operations can still be permitted per call via
	 * {@link EMFUriHandlerConstants#OPTION_ALLOW_URI_RESOLUTION}.
	 */
	public RestfulURIHandlerImpl() {
		this(Set.of());
	}

	/**
	 * Creates a handler that permits outbound http(s) resolution only for the given host patterns
	 * (matched case-insensitively). An empty set blocks all resolution unless a call opts in via
	 * {@link EMFUriHandlerConstants#OPTION_ALLOW_URI_RESOLUTION} - the secure default that prevents
	 * SSRF via attacker-supplied proxy references.
	 * <p>
	 * Each entry may be:
	 * <ul>
	 * <li>an exact host name, e.g. {@code models.example.com};</li>
	 * <li>a subdomain wildcard {@code *.mydomain.com}, matching any host that has at least one label
	 * before {@code .mydomain.com} (the apex {@code mydomain.com} is <em>not</em> matched - list it
	 * explicitly if needed);</li>
	 * <li>a bare {@code *}, which permits <strong>every</strong> host. This disables the SSRF
	 * protection entirely and is logged as a warning; use it only for trusted, closed environments.</li>
	 * </ul>
	 *
	 * @param allowedHosts the host patterns allowed for outbound resolution; must not be {@code null}
	 */
	public RestfulURIHandlerImpl(Set<String> allowedHosts) {
		requireNonNull(allowedHosts, "allowedHosts must not be null");
		boolean allowAll = false;
		Set<String> exact = new HashSet<>();
		List<String> suffixes = new ArrayList<>();
		for (String host : allowedHosts) {
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
				suffixes.add(normalized.substring(1)); // keep the leading dot: ".mydomain.com"
			} else {
				exact.add(normalized);
			}
		}
		this.allowAllHosts = allowAll;
		this.allowedHosts = Set.copyOf(exact);
		this.allowedSuffixes = List.copyOf(suffixes);
	}

	/**
	 * Decides whether outbound resolution of the given URI is permitted. Resolution is allowed when
	 * the per-call {@link EMFUriHandlerConstants#OPTION_ALLOW_URI_RESOLUTION} option is set to
	 * {@link Boolean#TRUE}, when a bare {@code "*"} was configured, or when the URI's host matches an
	 * exact entry or a {@code *.suffix} entry (all case-insensitive). With an empty whitelist and no
	 * per-call override every http(s) URI is blocked.
	 *
	 * @param uri     the URI about to be resolved
	 * @param options the load/save options of the current operation
	 * @return {@code true} if resolution is permitted, {@code false} otherwise
	 */
	boolean isResolutionAllowed(URI uri, Map<?, ?> options) {
		if (options != null
				&& Boolean.TRUE.equals(options.get(EMFUriHandlerConstants.OPTION_ALLOW_URI_RESOLUTION))) {
			return true;
		}
		if (allowAllHosts) {
			return true;
		}
		String host = uri.host();
		if (host == null) {
			return false;
		}
		String normalizedHost = host.toLowerCase(Locale.ROOT);
		if (allowedHosts.contains(normalizedHost)) {
			return true;
		}
		for (String suffix : allowedSuffixes) {
			// endsWith(".mydomain.com") already requires a label before the dot, so the apex does not match
			if (normalizedHost.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.URIHandlerImpl#createOutputStream(org.
	 * eclipse.emf.common.util.URI, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public OutputStream createOutputStream(URI uri, final Map<?, ?> options) throws IOException {
		java.net.URI netUri = java.net.URI.create(uri.toString());
		final HttpURLConnection httpURLConnection = (HttpURLConnection) netUri.toURL().openConnection();
		String method = HTTP_PUT;
		if (options.containsKey(EMFUriHandlerConstants.OPTION_HTTP_METHOD)) {
			method = options.get(EMFUriHandlerConstants.OPTION_HTTP_METHOD).toString().toUpperCase();
		}
		httpURLConnection.setRequestMethod(method);
		setTimeout(httpURLConnection, options);
		httpURLConnection.setDoOutput(true);
		setRequestHeaders(httpURLConnection,
				(Map<String, String>) options.get(EMFUriHandlerConstants.OPTION_HTTP_HEADERS));
		if (options.containsKey(EMFUriHandlerConstants.OPTION_ECLASS)) {
			httpURLConnection.setRequestProperty(EMFUriHandlerConstants.HEADER_CONTENT_CLASS,
					options.get(EMFUriHandlerConstants.OPTION_ECLASS).toString());
		}
		return new FilterOutputStream(httpURLConnection.getOutputStream()) {
			@Override
			public void close() throws IOException {
				super.close();
				try {
					int responseCode = httpURLConnection.getResponseCode();
					Map<Object, Object> response = getResponse(options);
					if (response != null) {
						setLastModified(httpURLConnection, response);
						response.put(EMFUriHandlerConstants.RESPONSE_HTTP_STATUS, responseCode);
						response.putAll(httpURLConnection.getHeaderFields());
					}
					InputStream in = extractStreamAndLogResponse(options, httpURLConnection);
					switch (responseCode) {
					case HttpURLConnection.HTTP_OK:
					case HttpURLConnection.HTTP_CREATED:
						Resource responseResource = (Resource) options
								.get(EMFUriHandlerConstants.OPTIONS_EXPECTED_RESPONSE_RESOURCE);
						if (responseResource != null) {
							responseResource.load(in, (Map<?, ?>) options
									.get(EMFUriHandlerConstants.OPTIONS_EXPECTED_RESPONSE_RESOURCE_OPTIONS));
						}
						break;
					case HttpURLConnection.HTTP_NO_CONTENT: {
						break;
					}
					default: {
						throw httpError(httpURLConnection, responseCode, readErrorBody(in));
					}
					}
				} finally {
					httpURLConnection.disconnect();
				}
			}
		};
	}

	/**
	 * Sets the given headers to the url connection
	 * 
	 * @param httpURLConnection
	 * @param headers
	 */
	private void setRequestHeaders(HttpURLConnection httpURLConnection, Map<String, String> headers) {
		if (headers != null) {
			for (Entry<String, String> entry : headers.entrySet()) {
				httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.URIHandlerImpl#createInputStream(org.
	 * eclipse.emf.common.util.URI, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
		if (!isResolutionAllowed(uri, options)) {
			throw new IOException("Blocked outbound http(s) resolution of URI '" + uri + "' (host '"
					+ uri.host() + "' is not in the configured allow-list). Add the host to the REST URI "
					+ "handler configuration, or set option '" + EMFUriHandlerConstants.OPTION_ALLOW_URI_RESOLUTION
					+ "'=Boolean.TRUE for a trusted, manual load of this URI.");
		}
		try {
			java.net.URI netUri = java.net.URI.create(uri.toString());
			final HttpURLConnection httpURLConnection = (HttpURLConnection) netUri.toURL().openConnection();
			setTimeout(httpURLConnection, options);
			setRequestHeaders(httpURLConnection,
					(Map<String, String>) options.get(EMFUriHandlerConstants.OPTION_HTTP_HEADERS));
			final int responseCode = httpURLConnection.getResponseCode();
			Map<Object, Object> response = getResponse(options);
			if (response != null) {
				setLastModified(httpURLConnection, response);
			}
			InputStream result = extractStreamAndLogResponse(options, httpURLConnection);
			return new FilterInputStream(result) {

				/*
				 * (non-Javadoc)
				 * 
				 * @see java.io.FilterInputStream#read()
				 */
				@Override
				public int read() throws IOException {
					if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
						return -1;
					}
					return super.read();
				}

				@Override
				public void close() throws IOException {
					int responseCode = httpURLConnection.getResponseCode();
					IOException failure = null;
					switch (responseCode) {
					case HttpURLConnection.HTTP_OK:
					case HttpURLConnection.HTTP_CREATED:
					case HttpURLConnection.HTTP_NO_CONTENT: {
						break;
					}
					default: {
						// the wrapped stream is the error stream here; drain what the
						// caller has not consumed before it is closed
						failure = httpError(httpURLConnection, responseCode, readErrorBody(in));
					}
					}
					super.close();
					httpURLConnection.disconnect();
					if (failure != null) {
						throw failure;
					}
				}

			};
		} catch (RuntimeException exception) {
			throw new Resource.IOWrappedException(exception);
		}
	}

	/**
	 * @param httpURLConnection
	 * @param response
	 */
	private void setLastModified(final HttpURLConnection httpURLConnection, Map<Object, Object> response) {
		try {
			String lastModified = httpURLConnection.getHeaderField(HEADER_LAST_MODIFIED);
			if (lastModified != null) {
				Long lm = Long.parseLong(lastModified);
				response.put(URIConverter.RESPONSE_TIME_STAMP_PROPERTY, lm);
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, ERROR_LAST_MODIFIED, e);
		}
	}

	/**
	 * @param options
	 * @param httpURLConnection
	 * @return
	 * @throws IOException
	 */
	private InputStream extractStreamAndLogResponse(Map<?, ?> options, final HttpURLConnection httpURLConnection)
			throws IOException {
		InputStream result = httpURLConnection.getErrorStream();
		if (result == null) {
			if (httpURLConnection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
				// error response without a body (e.g. with output streaming);
				// getInputStream() would throw the raw JDK exception here, so let
				// the callers report the failure from the status code instead
				result = InputStream.nullInputStream();
			} else {
				result = httpURLConnection.getInputStream();
			}
		}
		if (Boolean.TRUE.equals(options.get(EMFUriHandlerConstants.OPTIONS_LOG_RESPONSE))) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read = result.read();
			while (read != -1) {
				baos.write(read);
				read = result.read();
			}
			byte[] responseArray = baos.toByteArray();
			baos.close();
			result = new ByteArrayInputStream(responseArray);
		}
		return result;
	}

	/**
	 * Reads the HTTP error body from the given stream, capped at
	 * {@link #MAX_ERROR_BODY_BYTES}. Never throws: the response body is
	 * best-effort context and must not mask the status code.
	 *
	 * @param in the error stream, may be <code>null</code> or already partly
	 *            consumed
	 * @return the remaining body content, or an empty string
	 */
	private String readErrorBody(InputStream in) {
		if (in == null) {
			return "";
		}
		try {
			return new String(in.readNBytes(MAX_ERROR_BODY_BYTES), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "";
		}
	}

	/**
	 * Creates the {@link IOException} for a non-2xx response, appending the error
	 * body to the message if one is available
	 *
	 * @param httpURLConnection the connection that returned the error
	 * @param responseCode      the HTTP status code
	 * @param errorBody         the response body, may be empty
	 * @return the exception to throw
	 */
	private IOException httpError(HttpURLConnection httpURLConnection, int responseCode, String errorBody) {
		String message = httpURLConnection.getRequestMethod() + ERROR_WITH_RESPONSE_CODE + responseCode;
		if (!errorBody.isBlank()) {
			message += ": " + errorBody;
		}
		return new IOException(message);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.URIHandlerImpl#delete(org.eclipse.emf.
	 * common.util.URI, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void delete(URI uri, Map<?, ?> options) throws IOException {
		try {
			java.net.URI netUri = java.net.URI.create(uri.toString());
			final HttpURLConnection httpURLConnection = (HttpURLConnection) netUri.toURL().openConnection();
			setTimeout(httpURLConnection, options);
			httpURLConnection.setDoOutput(true);
			setRequestHeaders(httpURLConnection,
					(Map<String, String>) options.get(EMFUriHandlerConstants.OPTION_HTTP_HEADERS));
			httpURLConnection.setRequestMethod(HTTP_DELETE);
			int responseCode = httpURLConnection.getResponseCode();
			try {
				switch (responseCode) {
				case HttpURLConnection.HTTP_OK:
				case HttpURLConnection.HTTP_ACCEPTED:
				case HttpURLConnection.HTTP_NO_CONTENT: {
					break;
				}
				default: {
					throw httpError(httpURLConnection, responseCode,
							readErrorBody(httpURLConnection.getErrorStream()));
				}
				}
			} finally {
				httpURLConnection.disconnect();
			}
		} catch (RuntimeException exception) {
			throw new Resource.IOWrappedException(exception);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.URIHandlerImpl#getAttributes(org.eclipse.
	 * emf.common.util.URI, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, ?> getAttributes(URI uri, Map<?, ?> options) {
		Map<String, Object> result = new HashMap<>();
		Set<String> requestedAttributes = getRequestedAttributes(options);
		try {
			java.net.URI netUri = java.net.URI.create(uri.toString());
			URLConnection urlConnection = null;
			if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_READ_ONLY)) {

				urlConnection = netUri.toURL().openConnection();
				setTimeout(urlConnection, options);
				if (urlConnection instanceof HttpURLConnection httpURLConnection) {
					httpURLConnection.setRequestMethod(HTTP_OPTIONS);
					setRequestHeaders(httpURLConnection,
							(Map<String, String>) options.get(EMFUriHandlerConstants.OPTION_HTTP_HEADERS));
					int responseCode = httpURLConnection.getResponseCode();
					if (responseCode == HttpURLConnection.HTTP_OK) {
						String allow = httpURLConnection.getHeaderField(HEADER_ALLOW);
						result.put(URIConverter.ATTRIBUTE_READ_ONLY, allow == null || !allow.contains(HTTP_PUT));
					}
					urlConnection = null;
				} else {
					result.put(URIConverter.ATTRIBUTE_READ_ONLY, true);
				}
			}

			if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_TIME_STAMP)) {
				if (urlConnection == null) {
					urlConnection = netUri.toURL().openConnection();
					setTimeout(urlConnection, options);
					if (urlConnection instanceof HttpURLConnection httpURLConnection) {
						setRequestHeaders(httpURLConnection,
								(Map<String, String>) options.get(EMFUriHandlerConstants.OPTION_HTTP_HEADERS));
						httpURLConnection.setRequestMethod(HTTP_HEAD);
						httpURLConnection.getResponseCode();
					}
				}
				if (urlConnection.getHeaderField(HEADER_LAST_MODIFIED) != null) {
					result.put(URIConverter.ATTRIBUTE_TIME_STAMP, urlConnection.getLastModified());
				}
			}

			if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_LENGTH)) {
				if (urlConnection == null) {
					urlConnection = netUri.toURL().openConnection();
					setTimeout(urlConnection, options);
					if (urlConnection instanceof HttpURLConnection httpURLConnection) {
						setRequestHeaders(httpURLConnection,
								(Map<String, String>) options.get(EMFUriHandlerConstants.OPTION_HTTP_HEADERS));
						httpURLConnection.setRequestMethod(HTTP_HEAD);
						httpURLConnection.getResponseCode();
					}
				}
				if (urlConnection.getHeaderField(HEADER_CONTENT_LENGTH) != null) {
					result.put(URIConverter.ATTRIBUTE_LENGTH, urlConnection.getContentLength());
				}
			}
		} catch (IOException exception) {
			// Ignore exceptions.
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.URIHandlerImpl#exists(org.eclipse.emf.
	 * common.util.URI, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean exists(URI uri, Map<?, ?> options) {
		try {
			java.net.URI netUri = java.net.URI.create(uri.toString());
			HttpURLConnection httpURLConnection = (HttpURLConnection) netUri.toURL().openConnection();
			setTimeout(httpURLConnection, options);
			httpURLConnection.setRequestMethod(HTTP_HEAD);
			setRequestHeaders(httpURLConnection,
					(Map<String, String>) options.get(EMFUriHandlerConstants.OPTION_HTTP_HEADERS));
			int responseCode = httpURLConnection.getResponseCode();
			Map<Object, Object> response = getResponse(options);
			if (response != null) {
				setLastModified(httpURLConnection, response);
			}
			httpURLConnection.disconnect();
			return responseCode == HttpURLConnection.HTTP_OK;
		} catch (Exception exception) {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.ecore.resource.impl.URIHandlerImpl#canHandle(org.eclipse.emf.
	 * common.util.URI)
	 */
	@Override
	public boolean canHandle(URI uri) {
		return SCHEMA_HTTP.equalsIgnoreCase(uri.scheme()) || SCHEMA_HTTPS.equalsIgnoreCase(uri.scheme());
	}

	/**
	 * Returns the value of the {@link URIConverter#OPTION_TIMEOUT timeout option}.
	 * 
	 * @param options the options in which to look for the timeout option.
	 * @return the value of the timeout option, or <code>3000</code> if not present.
	 */
	@Override
	protected int getTimeout(Map<?, ?> options) {
		Integer timeout = (Integer) options.get(URIConverter.OPTION_TIMEOUT);
		return timeout == null ? 3000 : timeout.intValue();
	}

	protected void setTimeout(URLConnection connection, Map<?, ?> options) {
		int timeout = getTimeout(options);
		if (timeout != 0) {
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
		}
	}
	
}