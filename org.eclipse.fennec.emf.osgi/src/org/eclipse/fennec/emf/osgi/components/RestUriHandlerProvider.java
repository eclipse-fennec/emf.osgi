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

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.fennec.emf.osgi.UriHandlerProvider;
import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.urihandler.RestfulURIHandlerImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

/**
 * Provider for the REST-ful URI handler that resolves {@code http}/{@code https} URIs.
 * <p>
 * The handler is <strong>secure by default</strong>: with no configuration (or an empty host
 * whitelist) it refuses to demand-load http(s) URIs, closing the SSRF vector where an
 * attacker-supplied proxy reference would be fetched during deserialization. An administrator opts
 * hosts back in through the {@value #PID} Config Admin configuration
 * ({@link RestUriHandlerConfig#allowedHosts()}); trusted code driving a {@code ResourceSet}
 * manually can also allow a single URI per call via
 * {@code EMFUriHandlerConstants#OPTION_ALLOW_URI_RESOLUTION}.
 * <p>
 * While a non-empty whitelist is configured, this component additionally registers a marker
 * {@link ResourceSetConfigurator} carrying only the {@link EMFNamespaces#PROP_URI_HANDLER_HTTP}
 * service property. Because the ResourceSetFactory propagates {@code ResourceSetConfigurator}
 * service properties (but not {@code UriHandlerProvider} ones), this makes
 * {@code emf.uri.handler.http=true} visible on the produced ResourceSetFactory/ResourceSet
 * services - so consumers can filter for an http-capable ResourceSet - without ever exposing the
 * host list itself as a service property.
 *
 * @author Mark Hoffmann
 * @since 27.07.2017
 */
@Component(
		name = "RestUriHandlerProvider",
		service = UriHandlerProvider.class,
		configurationPid = RestUriHandlerProvider.PID,
		configurationPolicy = ConfigurationPolicy.OPTIONAL,
		immediate = true)
@Designate(ocd = RestUriHandlerConfig.class)
public class RestUriHandlerProvider implements UriHandlerProvider {

	/** Configuration PID for the REST URI handler access policy. */
	public static final String PID = "org.eclipse.fennec.emf.osgi.urihandler.http";

	/** Marker configurator that carries only the http capability property; performs no configuration. */
	private static final ResourceSetConfigurator HTTP_CAPABILITY_MARKER = resourceSet -> {
		/* no-op: the actual handler is attached by ResourceSetUriHandlerConfiguratorComponent */
	};

	private final BundleContext bundleContext;

	/** Host names allowed for outbound http(s) resolution. Empty = block all (secure default). */
	private volatile Set<String> allowedHosts = Set.of();

	/** Registration of the http-capability marker; non-null only while a whitelist is configured. */
	private ServiceRegistration<ResourceSetConfigurator> capabilityRegistration;

	@Activate
	public RestUriHandlerProvider(BundleContext bundleContext, RestUriHandlerConfig config) {
		this.bundleContext = bundleContext;
		applyConfig(config);
	}

	@Modified
	void modified(RestUriHandlerConfig config) {
		applyConfig(config);
	}

	@Deactivate
	void deactivate() {
		unregisterCapability();
	}

	@Override
	public URIHandler getURIHandler() {
		return new RestfulURIHandlerImpl(allowedHosts);
	}

	private void applyConfig(RestUriHandlerConfig config) {
		Set<String> hosts = new HashSet<>();
		for (String host : config.allowedHosts()) {
			if (host != null && !host.isBlank()) {
				hosts.add(host.trim());
			}
		}
		this.allowedHosts = Set.copyOf(hosts);
		if (allowedHosts.isEmpty()) {
			unregisterCapability();
		} else {
			registerCapability();
		}
	}

	private void registerCapability() {
		if (capabilityRegistration == null) {
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put(EMFNamespaces.PROP_URI_HANDLER_HTTP, Boolean.TRUE);
			capabilityRegistration = bundleContext.registerService(ResourceSetConfigurator.class,
					HTTP_CAPABILITY_MARKER, properties);
		}
	}

	private void unregisterCapability() {
		if (capabilityRegistration != null) {
			capabilityRegistration.unregister();
			capabilityRegistration = null;
		}
	}
}
