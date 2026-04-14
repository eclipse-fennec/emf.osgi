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

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.gecko.emf.osgi.configurator.ResourceSetConfigurator;
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
 * Compatibility component that tracks new Fennec {@link org.eclipse.fennec.emf.osgi.ResourceSetFactory}
 * services and re-registers them as legacy {@link org.gecko.emf.osgi.ResourceSetFactory}
 * services with converted properties.
 * <p>
 * This wrapper goes in the reverse direction compared to the other wrappers:
 * it exposes new Fennec services under the old Gecko interface so that existing
 * consumers using the old API continue to work.
 *
 * @author Data In Motion
 * @since 1.0
 */
@Component(name = "GeckoResourceSetFactoryWrapper")
@SuppressWarnings("deprecation")
@ServiceCapability(org.gecko.emf.osgi.ResourceSetFactory.class)
public class GeckoResourceSetFactoryWrapperComponent {

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
	private final Map<ResourceSetFactory, ServiceRegistration<org.gecko.emf.osgi.ResourceSetFactory>> registrations = new ConcurrentHashMap<>();

	@Activate
	public GeckoResourceSetFactoryWrapperComponent(BundleContext ctx) {
		this.ctx = ctx;
	}

	@Deactivate
	public void deactivate() {
		registrations.values().forEach(ServiceRegistration::unregister);
		registrations.clear();
	}

	@Reference(name = "fennecResourceSetFactory", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeFennecResourceSetFactory", updated = "updatedFennecResourceSetFactory")
	public void addFennecResourceSetFactory(ResourceSetFactory factory, Map<String, Object> properties) {
		org.gecko.emf.osgi.ResourceSetFactory wrapper = new org.gecko.emf.osgi.ResourceSetFactory() {
			@Override
			public ResourceSet createResourceSet() {
				return factory.createResourceSet();
			}

			@Override
			public Collection<ResourceSetConfigurator> getResourceSetConfigurators() {
				return Collections.emptyList();
			}
		};
		Dictionary<String, Object> converted = convertProperties(properties);
		ServiceRegistration<org.gecko.emf.osgi.ResourceSetFactory> reg = ctx.registerService(org.gecko.emf.osgi.ResourceSetFactory.class, wrapper, converted);
		registrations.put(factory, reg);
	}

	public void updatedFennecResourceSetFactory(ResourceSetFactory factory, Map<String, Object> properties) {
		ServiceRegistration<org.gecko.emf.osgi.ResourceSetFactory> reg = registrations.get(factory);
		if (reg != null) {
			reg.setProperties(convertProperties(properties));
		}
	}

	public void removeFennecResourceSetFactory(ResourceSetFactory factory) {
		ServiceRegistration<org.gecko.emf.osgi.ResourceSetFactory> reg = registrations.remove(factory);
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
