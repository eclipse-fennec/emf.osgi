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
package org.eclipse.fennec.emf.osgi.components;

import static java.util.Objects.nonNull;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory;
import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory.Descriptor;
import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory.Registry;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Whiteboard populator that registers {@link Factory} and {@link Descriptor}
 * services into the global {@link Registry#INSTANCE} for setting delegates.
 * <p>
 * Services must carry service properties
 * {@code emf.configuratorType = SETTING_DELEGATE_FACTORY} and an {@code emf.configuratorName}
 * matching the delegate URI (e.g. {@code "http://www.eclipse.org/emf/2002/Ecore/OCL"}).
 *
 * @author Mark Hoffmann
 * @since 20.02.2026
 * @see org.eclipse.fennec.emf.osgi.annotation.ConfiguratorType#SETTING_DELEGATE_FACTORY
 */
@Component(name = DefaultSettingDelegateRegistryComponent.NAME)
@ProviderType
public class DefaultSettingDelegateRegistryComponent {

	private static final Logger LOG = Logger.getLogger(DefaultSettingDelegateRegistryComponent.class.getName());

	public static final String NAME = "DefaultSettingDelegateRegistry";
	public static final String TARGET = "(" + EMFNamespaces.EMF_CONFIGURATOR_TYPE + "=SETTING_DELEGATE_FACTORY)";

	private final Registry registry = Registry.INSTANCE;

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
	public void addSettingDelegateFactory(Factory factory, Map<String, Object> properties) {
		String name = (String) properties.get(EMFNamespaces.EMF_CONFIGURATOR_NAME);
		Object recent = registry.put(name, factory);
		if (nonNull(recent)) {
			LOG.info(() -> String.format(
					"A setting delegate factory '%s' for '%s' was already registered and is now replaced by a new one",
					recent, name));
		}
	}

	public void removeSettingDelegateFactory(Factory factory, Map<String, Object> properties) {
		registry.remove(properties.get(EMFNamespaces.EMF_CONFIGURATOR_NAME));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
	public void addSettingDelegateDescriptor(Descriptor descriptor, Map<String, Object> properties) {
		String name = (String) properties.get(EMFNamespaces.EMF_CONFIGURATOR_NAME);
		Object recent = registry.put(name, descriptor);
		if (nonNull(recent)) {
			LOG.info(() -> String.format(
					"A setting delegate factory descriptor '%s' for '%s' was already registered and is now replaced by a new one",
					recent, name));
		}
	}

	public void removeSettingDelegateDescriptor(Descriptor descriptor, Map<String, Object> properties) {
		registry.remove(properties.get(EMFNamespaces.EMF_CONFIGURATOR_NAME));
	}
}
