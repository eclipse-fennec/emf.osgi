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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
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
 * Compatibility component that tracks legacy GeckoEMF ResourceFactoryConfigurator
 * services, both {@link org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator}
 * (GeckoEMF 6) and {@link org.gecko.emf.osgi.ResourceFactoryConfigurator} (GeckoEMF 3.x - 5.x). Since Fennec no longer has a ResourceFactoryConfigurator equivalent,
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

	private final BundleContext ctx;
	private final Map<Object, List<ServiceRegistration<Resource.Factory>>> registrations = new ConcurrentHashMap<>();

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
	public void addGeckoResourceFactoryConfigurator(org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		registrations.put(configurator, registerFactories(configurator::configureResourceFactory, properties));
	}

	public void updatedGeckoResourceFactoryConfigurator(org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		unregister(configurator);
		registrations.put(configurator, registerFactories(configurator::configureResourceFactory, properties));
	}

	public void removeGeckoResourceFactoryConfigurator(org.gecko.emf.osgi.configurator.ResourceFactoryConfigurator configurator) {
		unregister(configurator);
	}

	@Reference(name = "legacyGeckoResourceFactoryConfigurator", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeLegacyGeckoResourceFactoryConfigurator", updated = "updatedLegacyGeckoResourceFactoryConfigurator")
	public void addLegacyGeckoResourceFactoryConfigurator(org.gecko.emf.osgi.ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		registrations.put(configurator, registerFactories(configurator::configureResourceFactory, properties));
	}

	public void updatedLegacyGeckoResourceFactoryConfigurator(org.gecko.emf.osgi.ResourceFactoryConfigurator configurator, Map<String, Object> properties) {
		unregister(configurator);
		registrations.put(configurator, registerFactories(configurator::configureResourceFactory, properties));
	}

	public void removeLegacyGeckoResourceFactoryConfigurator(org.gecko.emf.osgi.ResourceFactoryConfigurator configurator) {
		unregister(configurator);
	}

	private void unregister(Object configurator) {
		List<ServiceRegistration<Resource.Factory>> regs = registrations.remove(configurator);
		if (regs != null) {
			regs.forEach(ServiceRegistration::unregister);
		}
	}

	private List<ServiceRegistration<Resource.Factory>> registerFactories(Consumer<Resource.Factory.Registry> configure, Map<String, Object> properties) {
		Resource.Factory.Registry registry = new ResourceFactoryRegistryImpl();
		configure.accept(registry);

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
		// Copy the original properties, legacy names get their Fennec counterpart
		Dictionary<String, Object> props = GeckoServiceProperties.toFennec(originalProperties);

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
