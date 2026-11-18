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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
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
 * Compatibility component that tracks legacy GeckoEMF ResourceSetConfigurator services and
 * re-registers them as {@link org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator}
 * services with converted properties.
 * <p>
 * Both legacy locations are tracked: {@link org.gecko.emf.osgi.configurator.ResourceSetConfigurator}
 * (GeckoEMF 6) and {@link org.gecko.emf.osgi.ResourceSetConfigurator} (GeckoEMF 3.x - 5.x).
 * </p>
 *
 * @author Data In Motion
 * @since 1.0
 */
@Component(name = "GeckoResourceSetConfiguratorWrapper")
@SuppressWarnings("deprecation")
@ServiceCapability(ResourceSetConfigurator.class)
public class GeckoResourceSetConfiguratorWrapperComponent {

	private final BundleContext ctx;
	private final Map<Object, ServiceRegistration<ResourceSetConfigurator>> registrations = new ConcurrentHashMap<>();

	@Activate
	public GeckoResourceSetConfiguratorWrapperComponent(BundleContext ctx) {
		this.ctx = ctx;
	}

	@Deactivate
	public void deactivate() {
		registrations.values().forEach(ServiceRegistration::unregister);
		registrations.clear();
	}

	@Reference(name = "geckoResourceSetConfigurator", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeGeckoResourceSetConfigurator", updated = "updatedGeckoResourceSetConfigurator")
	public void addGeckoResourceSetConfigurator(org.gecko.emf.osgi.configurator.ResourceSetConfigurator configurator, Map<String, Object> properties) {
		register(configurator, configurator::configureResourceSet, properties);
	}

	public void updatedGeckoResourceSetConfigurator(org.gecko.emf.osgi.configurator.ResourceSetConfigurator configurator, Map<String, Object> properties) {
		update(configurator, properties);
	}

	public void removeGeckoResourceSetConfigurator(org.gecko.emf.osgi.configurator.ResourceSetConfigurator configurator) {
		unregister(configurator);
	}

	@Reference(name = "legacyGeckoResourceSetConfigurator", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeLegacyGeckoResourceSetConfigurator", updated = "updatedLegacyGeckoResourceSetConfigurator")
	public void addLegacyGeckoResourceSetConfigurator(org.gecko.emf.osgi.ResourceSetConfigurator configurator, Map<String, Object> properties) {
		register(configurator, configurator::configureResourceSet, properties);
	}

	public void updatedLegacyGeckoResourceSetConfigurator(org.gecko.emf.osgi.ResourceSetConfigurator configurator, Map<String, Object> properties) {
		update(configurator, properties);
	}

	public void removeLegacyGeckoResourceSetConfigurator(org.gecko.emf.osgi.ResourceSetConfigurator configurator) {
		unregister(configurator);
	}

	private void register(Object configurator, ResourceSetConfigurator wrapper, Map<String, Object> properties) {
		ServiceRegistration<ResourceSetConfigurator> reg = ctx.registerService(ResourceSetConfigurator.class, wrapper, GeckoServiceProperties.toFennec(properties));
		registrations.put(configurator, reg);
	}

	private void update(Object configurator, Map<String, Object> properties) {
		ServiceRegistration<ResourceSetConfigurator> reg = registrations.get(configurator);
		if (reg != null) {
			reg.setProperties(GeckoServiceProperties.toFennec(properties));
		}
	}

	private void unregister(Object configurator) {
		ServiceRegistration<ResourceSetConfigurator> reg = registrations.remove(configurator);
		if (reg != null) {
			reg.unregister();
		}
	}
}
