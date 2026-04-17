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

import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
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

import aQute.bnd.annotation.service.ServiceCapability;

/**
 * Compatibility component that tracks legacy {@link org.gecko.emf.osgi.configurator.EPackageConfigurator}
 * services and re-registers them as {@link org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator}
 * services with converted properties.
 *
 * @author Data In Motion
 * @since 1.0
 */
@Component(name = "GeckoEPackageConfiguratorWrapper")
@SuppressWarnings("deprecation")
@ServiceCapability(EPackageConfigurator.class)
public class GeckoEPackageConfiguratorWrapperComponent {

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
	private final Map<org.gecko.emf.osgi.configurator.EPackageConfigurator, ServiceRegistration<EPackageConfigurator>> registrations = new ConcurrentHashMap<>();

	@Activate
	public GeckoEPackageConfiguratorWrapperComponent(BundleContext ctx) {
		this.ctx = ctx;
	}

	@Deactivate
	public void deactivate() {
		registrations.values().forEach(ServiceRegistration::unregister);
		registrations.clear();
	}

	@Reference(name = "geckoEPackageConfigurator", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeGeckoEPackageConfigurator", updated = "updatedGeckoEPackageConfigurator")
	public void addGeckoEPackageConfigurator(org.gecko.emf.osgi.configurator.EPackageConfigurator configurator, Map<String, Object> properties) {
		EPackageConfigurator wrapper = new EPackageConfigurator() {
			@Override
			public void configureEPackage(org.eclipse.emf.ecore.EPackage.Registry registry) {
				configurator.configureEPackage(registry);
			}

			@Override
			public void unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry registry) {
				configurator.unconfigureEPackage(registry);
			}
		};
		Dictionary<String, Object> converted = convertProperties(properties);
		ServiceRegistration<EPackageConfigurator> reg = ctx.registerService(EPackageConfigurator.class, wrapper, converted);
		registrations.put(configurator, reg);
	}

	public void updatedGeckoEPackageConfigurator(org.gecko.emf.osgi.configurator.EPackageConfigurator configurator, Map<String, Object> properties) {
		ServiceRegistration<EPackageConfigurator> reg = registrations.get(configurator);
		if (reg != null) {
			reg.setProperties(convertProperties(properties));
		}
	}

	public void removeGeckoEPackageConfigurator(org.gecko.emf.osgi.configurator.EPackageConfigurator configurator) {
		ServiceRegistration<EPackageConfigurator> reg = registrations.remove(configurator);
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
