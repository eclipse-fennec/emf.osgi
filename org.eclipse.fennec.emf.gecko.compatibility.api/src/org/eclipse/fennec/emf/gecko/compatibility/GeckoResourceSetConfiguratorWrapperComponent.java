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

import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
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
 * Compatibility component that tracks legacy {@link org.gecko.emf.osgi.configurator.ResourceSetConfigurator}
 * services and re-registers them as {@link org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator}
 * services with converted properties.
 *
 * @author Data In Motion
 * @since 1.0
 */
@Component(name = "GeckoResourceSetConfiguratorWrapper")
@SuppressWarnings("deprecation")
public class GeckoResourceSetConfiguratorWrapperComponent {

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
	private final Map<org.gecko.emf.osgi.configurator.ResourceSetConfigurator, ServiceRegistration<ResourceSetConfigurator>> registrations = new ConcurrentHashMap<>();

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
		ResourceSetConfigurator wrapper = configurator::configureResourceSet;
		Dictionary<String, Object> converted = convertProperties(properties);
		ServiceRegistration<ResourceSetConfigurator> reg = ctx.registerService(ResourceSetConfigurator.class, wrapper, converted);
		registrations.put(configurator, reg);
	}

	public void updatedGeckoResourceSetConfigurator(org.gecko.emf.osgi.configurator.ResourceSetConfigurator configurator, Map<String, Object> properties) {
		ServiceRegistration<ResourceSetConfigurator> reg = registrations.get(configurator);
		if (reg != null) {
			reg.setProperties(convertProperties(properties));
		}
	}

	public void removeGeckoResourceSetConfigurator(org.gecko.emf.osgi.configurator.ResourceSetConfigurator configurator) {
		ServiceRegistration<ResourceSetConfigurator> reg = registrations.remove(configurator);
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
