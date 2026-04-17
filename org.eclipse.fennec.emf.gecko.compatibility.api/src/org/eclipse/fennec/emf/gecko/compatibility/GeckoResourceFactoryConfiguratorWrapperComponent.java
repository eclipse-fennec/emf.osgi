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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator;
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
 * Compatibility component that tracks legacy {@link ResourceFactoryConfigurator}
 * services. Since Fennec no longer has a ResourceFactoryConfigurator equivalent,
 * this wrapper collects the configurator, gives it an empty
 * {@link Resource.Factory.Registry}, and then registers each
 * {@link Resource.Factory} found in the registry as an OSGi service with the
 * correctly translated properties.
 *
 * @author Data In Motion
 * @since 1.0
 */
@Component(name = "GeckoResourceFactoryConfiguratorWrapper")
@SuppressWarnings("deprecation")
@ServiceCapability(Resource.Factory.class)
public class GeckoResourceFactoryConfiguratorWrapperComponent {

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
	private final Map<ResourceFactoryConfigurator, List<ServiceRegistration<Resource.Factory>>> registrations = new ConcurrentHashMap<>();

	@Activate
	public GeckoResourceFactoryConfiguratorWrapperComponent(BundleContext ctx) {
		this.ctx = ctx;
	}

	@Deactivate
	public void deactivate() {
		registrations.values().stream()
				.flatMap(List::stream)
				.forEach(ServiceRegistration::unregister);
		registrations.clear();
	}

	@Reference(name = "geckoResourceFactoryConfigurator", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeGeckoResourceFactoryConfigurator", updated = "updatedGeckoResourceFactoryConfigurator")
	public void addGeckoResourceFactoryConfigurator(ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		List<ServiceRegistration<Resource.Factory>> regs = registerFactories(configurator, properties);
		registrations.put(configurator, regs);
	}

	public void updatedGeckoResourceFactoryConfigurator(ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		removeGeckoResourceFactoryConfigurator(configurator);
		List<ServiceRegistration<Resource.Factory>> regs = registerFactories(configurator, properties);
		registrations.put(configurator, regs);
	}

	public void removeGeckoResourceFactoryConfigurator(ResourceFactoryConfigurator configurator) {
		List<ServiceRegistration<Resource.Factory>> regs = registrations.remove(configurator);
		if (regs != null) {
			regs.forEach(ServiceRegistration::unregister);
		}
	}

	private List<ServiceRegistration<Resource.Factory>> registerFactories(ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		Resource.Factory.Registry registry = new ResourceFactoryRegistryImpl();
		configurator.configureResourceFactory(registry);

		Map<Resource.Factory, FactoryRegistrationInfo> factoryInfos = new ConcurrentHashMap<>();

		registry.getExtensionToFactoryMap().forEach((extension, factory) -> {
			if (factory instanceof Resource.Factory f) {
				factoryInfos.computeIfAbsent(f, k -> new FactoryRegistrationInfo())
						.fileExtensions.add((String) extension);
			}
		});

		registry.getContentTypeToFactoryMap().forEach((contentType, factory) -> {
			if (factory instanceof Resource.Factory f) {
				factoryInfos.computeIfAbsent(f, k -> new FactoryRegistrationInfo())
						.contentTypes.add((String) contentType);
			}
		});

		registry.getProtocolToFactoryMap().forEach((protocol, factory) -> {
			if (factory instanceof Resource.Factory f) {
				factoryInfos.computeIfAbsent(f, k -> new FactoryRegistrationInfo())
						.protocols.add((String) protocol);
			}
		});

		List<ServiceRegistration<Resource.Factory>> regs = new ArrayList<>();
		for (Map.Entry<Resource.Factory, FactoryRegistrationInfo> entry : factoryInfos.entrySet()) {
			Dictionary<String, Object> serviceProps = buildServiceProperties(entry.getValue(), properties);
			ServiceRegistration<Resource.Factory> reg = ctx.registerService(Resource.Factory.class, entry.getKey(), serviceProps);
			regs.add(reg);
		}
		return regs;
	}

	private Dictionary<String, Object> buildServiceProperties(FactoryRegistrationInfo info, Map<String, Object> originalProperties) {
		Hashtable<String, Object> props = new Hashtable<>();

		// Copy non-excluded original properties
		originalProperties.forEach((key, value) -> {
			if (value != null && !EXCLUDED_PROPERTIES.contains(key)) {
				props.put(key, value);
			}
		});

		// Set Resource.Factory specific properties from registry entries
		if (!info.fileExtensions.isEmpty()) {
			props.put(EMFNamespaces.EMF_MODEL_FILE_EXT, info.fileExtensions);
		}
		if (!info.contentTypes.isEmpty()) {
			props.put(EMFNamespaces.EMF_MODEL_CONTENT_TYPE, info.contentTypes);
		}
		if (!info.protocols.isEmpty()) {
			props.put(EMFNamespaces.EMF_MODEL_PROTOCOL, info.protocols);
		}

		return props;
	}

	private static class FactoryRegistrationInfo {
		final List<String> fileExtensions = new ArrayList<>();
		final List<String> contentTypes = new ArrayList<>();
		final List<String> protocols = new ArrayList<>();
	}
}
