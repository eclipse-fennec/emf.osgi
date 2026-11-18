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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.fennec.emf.osgi.UriHandlerProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import aQute.bnd.annotation.service.ServiceCapability;

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
@ServiceCapability(org.gecko.emf.osgi.UriHandlerProvider.class)
public class GeckoUriHandlerProviderWrapperComponent {

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
		Dictionary<String, Object> converted = GeckoServiceProperties.toFennec(properties);
		ServiceRegistration<UriHandlerProvider> reg = ctx.registerService(UriHandlerProvider.class, wrapper, converted);
		registrations.put(provider, reg);
	}

	public void updatedGeckoUriHandlerProvider(org.gecko.emf.osgi.UriHandlerProvider provider, Map<String, Object> properties) {
		ServiceRegistration<UriHandlerProvider> reg = registrations.get(provider);
		if (reg != null) {
			reg.setProperties(GeckoServiceProperties.toFennec(properties));
		}
	}

	public void removeGeckoUriHandlerProvider(org.gecko.emf.osgi.UriHandlerProvider provider) {
		ServiceRegistration<UriHandlerProvider> reg = registrations.remove(provider);
		if (reg != null) {
			reg.unregister();
		}
	}

}
