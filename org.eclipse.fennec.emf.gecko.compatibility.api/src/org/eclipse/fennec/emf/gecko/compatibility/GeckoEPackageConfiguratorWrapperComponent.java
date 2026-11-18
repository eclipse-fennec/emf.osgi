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
import java.util.function.Consumer;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
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
 * Compatibility component that tracks legacy GeckoEMF EPackageConfigurator services and
 * re-registers them as {@link org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator}
 * services with converted properties.
 * <p>
 * Both legacy locations are tracked: {@link org.gecko.emf.osgi.configurator.EPackageConfigurator}
 * (GeckoEMF 6) and {@link org.gecko.emf.osgi.EPackageConfigurator} (GeckoEMF 3.x - 5.x).
 * </p>
 *
 * @author Data In Motion
 * @since 1.0
 */
@Component(name = "GeckoEPackageConfiguratorWrapper")
@SuppressWarnings("deprecation")
@ServiceCapability(EPackageConfigurator.class)
public class GeckoEPackageConfiguratorWrapperComponent {

	private final BundleContext ctx;
	private final Map<Object, ServiceRegistration<EPackageConfigurator>> registrations = new ConcurrentHashMap<>();

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
		register(configurator, configurator::configureEPackage, configurator::unconfigureEPackage, properties);
	}

	public void updatedGeckoEPackageConfigurator(org.gecko.emf.osgi.configurator.EPackageConfigurator configurator, Map<String, Object> properties) {
		update(configurator, properties);
	}

	public void removeGeckoEPackageConfigurator(org.gecko.emf.osgi.configurator.EPackageConfigurator configurator) {
		unregister(configurator);
	}

	@Reference(name = "legacyGeckoEPackageConfigurator", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeLegacyGeckoEPackageConfigurator", updated = "updatedLegacyGeckoEPackageConfigurator")
	public void addLegacyGeckoEPackageConfigurator(org.gecko.emf.osgi.EPackageConfigurator configurator, Map<String, Object> properties) {
		register(configurator, configurator::configureEPackage, configurator::unconfigureEPackage, properties);
	}

	public void updatedLegacyGeckoEPackageConfigurator(org.gecko.emf.osgi.EPackageConfigurator configurator, Map<String, Object> properties) {
		update(configurator, properties);
	}

	public void removeLegacyGeckoEPackageConfigurator(org.gecko.emf.osgi.EPackageConfigurator configurator) {
		unregister(configurator);
	}

	private void register(Object configurator, Consumer<EPackage.Registry> configure, Consumer<EPackage.Registry> unconfigure, Map<String, Object> properties) {
		EPackageConfigurator wrapper = new EPackageConfigurator() {
			@Override
			public void configureEPackage(EPackage.Registry registry) {
				configure.accept(registry);
			}

			@Override
			public void unconfigureEPackage(EPackage.Registry registry) {
				unconfigure.accept(registry);
			}
		};
		ServiceRegistration<EPackageConfigurator> reg = ctx.registerService(EPackageConfigurator.class, wrapper, GeckoServiceProperties.toFennec(properties));
		registrations.put(configurator, reg);
	}

	private void update(Object configurator, Map<String, Object> properties) {
		ServiceRegistration<EPackageConfigurator> reg = registrations.get(configurator);
		if (reg != null) {
			reg.setProperties(GeckoServiceProperties.toFennec(properties));
		}
	}

	private void unregister(Object configurator) {
		ServiceRegistration<EPackageConfigurator> reg = registrations.remove(configurator);
		if (reg != null) {
			reg.unregister();
		}
	}
}
