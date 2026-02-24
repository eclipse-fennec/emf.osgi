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
package org.eclipse.fennec.emf.gecko.compatibility;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.fennec.emf.osgi.UriHandlerProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Compatibility component that tracks legacy {@link org.gecko.emf.osgi.UriHandlerProvider}
 * services and re-registers them as {@link org.eclipse.fennec.emf.osgi.UriHandlerProvider}
 * services with converted properties.
 *
 * @author Data In Motion
 * @since 1.0
 */
@Component(name = "GeckoUriHandlerProviderWrapper")
@SuppressWarnings("deprecation")
public class GeckoUriHandlerProviderWrapperComponent {

	private static final Set<String> EXCLUDED_PROPERTIES = Set.of(
			Constants.SERVICE_ID,
			Constants.SERVICE_BUNDLEID,
			Constants.SERVICE_SCOPE,
			Constants.OBJECTCLASS,
			Constants.SERVICE_PID,
			ComponentConstants.COMPONENT_NAME,
			ComponentConstants.COMPONENT_ID
	);

	private final BundleContext ctx;
	
	private final Map<org.gecko.emf.osgi.UriHandlerProvider, ServiceRegistration<UriHandlerProvider>> registrations = new ConcurrentHashMap<>();

	@Activate
	public GeckoUriHandlerProviderWrapperComponent(BundleContext ctx) {
		this.ctx = ctx;
	}

	@Deactivate
	public void deactivate() {
		registrations.values().forEach(ServiceRegistration::unregister);
		registrations.clear();
	}

	@Reference(name = "geckoUriHandlerProvider", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeGeckoUriHandlerProvider", updated = "updatedGeckoUriHandlerProvider")
	public void addGeckoUriHandlerProvider(org.gecko.emf.osgi.UriHandlerProvider provider, Map<String, Object> properties) {
		UriHandlerProvider wrapper = provider::getURIHandler;
		Dictionary<String, Object> converted = convertProperties(properties);
		ServiceRegistration<UriHandlerProvider> reg = ctx.registerService(UriHandlerProvider.class, wrapper, converted);
		registrations.put(provider, reg);
	}

	public void updatedGeckoUriHandlerProvider(org.gecko.emf.osgi.UriHandlerProvider provider, Map<String, Object> properties) {
		ServiceRegistration<UriHandlerProvider> reg = registrations.get(provider);
		if (reg != null) {
			reg.setProperties(convertProperties(properties));
		}
	}

	public void removeGeckoUriHandlerProvider(org.gecko.emf.osgi.UriHandlerProvider provider) {
		ServiceRegistration<UriHandlerProvider> reg = registrations.remove(provider);
		if (reg != null) {
			reg.unregister();
		}
	}

	private Dictionary<String, Object> convertProperties(Map<String, Object> properties) {
		Hashtable<String, Object> converted = new Hashtable<>();
		properties.forEach((key, value) -> {
			if (value != null && !EXCLUDED_PROPERTIES.contains(key)) {
				converted.put(key, value);
			}
		});
		return converted;
	}
}
