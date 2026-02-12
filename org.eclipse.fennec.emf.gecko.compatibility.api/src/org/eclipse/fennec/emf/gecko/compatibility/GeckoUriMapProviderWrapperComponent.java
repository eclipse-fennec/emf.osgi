/**
 * Copyright (c) 2012 - 2025 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.emf.gecko.compatibility;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.fennec.emf.osgi.UriMapProvider;
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
 * Compatibility component that tracks legacy {@link org.gecko.emf.osgi.UriMapProvider}
 * services and re-registers them as {@link org.eclipse.fennec.emf.osgi.UriMapProvider}
 * services with converted properties.
 *
 * @author Data In Motion
 * @since 1.0
 */
@Component(name = "GeckoUriMapProviderWrapper")
@SuppressWarnings("deprecation")
public class GeckoUriMapProviderWrapperComponent {

	private static final Set<String> EXCLUDED_PROPERTIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			Constants.SERVICE_ID,
			Constants.SERVICE_BUNDLEID,
			Constants.SERVICE_SCOPE,
			Constants.OBJECTCLASS,
			Constants.SERVICE_PID,
			ComponentConstants.COMPONENT_NAME,
			ComponentConstants.COMPONENT_ID
	)));

	private final BundleContext ctx;
	private final Map<org.gecko.emf.osgi.UriMapProvider, ServiceRegistration<UriMapProvider>> registrations = new ConcurrentHashMap<>();

	@Activate
	public GeckoUriMapProviderWrapperComponent(BundleContext ctx) {
		this.ctx = ctx;
	}

	@Deactivate
	public void deactivate() {
		registrations.values().forEach(ServiceRegistration::unregister);
		registrations.clear();
	}

	@Reference(name = "geckoUriMapProvider", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeGeckoUriMapProvider", updated = "updatedGeckoUriMapProvider")
	public void addGeckoUriMapProvider(org.gecko.emf.osgi.UriMapProvider provider, Map<String, Object> properties) {
		UriMapProvider wrapper = provider::getUriMap;
		Dictionary<String, Object> converted = convertProperties(properties);
		ServiceRegistration<UriMapProvider> reg = ctx.registerService(UriMapProvider.class, wrapper, converted);
		registrations.put(provider, reg);
	}

	public void updatedGeckoUriMapProvider(org.gecko.emf.osgi.UriMapProvider provider, Map<String, Object> properties) {
		ServiceRegistration<UriMapProvider> reg = registrations.get(provider);
		if (reg != null) {
			reg.setProperties(convertProperties(properties));
		}
	}

	public void removeGeckoUriMapProvider(org.gecko.emf.osgi.UriMapProvider provider) {
		ServiceRegistration<UriMapProvider> reg = registrations.remove(provider);
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
