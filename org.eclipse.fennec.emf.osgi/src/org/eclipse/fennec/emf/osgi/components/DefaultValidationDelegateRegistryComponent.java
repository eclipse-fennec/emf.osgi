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

import org.eclipse.emf.ecore.EValidator.ValidationDelegate;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Descriptor;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Registry;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Whiteboard populator that registers {@link ValidationDelegate} and
 * {@link Descriptor} services into the global {@link Registry#INSTANCE}.
 * <p>
 * Services must carry service properties
 * {@code emf.configuratorType = VALIDATION_DELEGATE} and an {@code emf.configuratorName}
 * matching the delegate URI (e.g. {@code "http://www.eclipse.org/emf/2002/Ecore/OCL"}).
 *
 * @author Mark Hoffmann
 * @since 20.02.2026
 * @see org.eclipse.fennec.emf.osgi.annotation.ConfiguratorType#VALIDATION_DELEGATE
 */
@Component(name = DefaultValidationDelegateRegistryComponent.NAME)
@ProviderType
public class DefaultValidationDelegateRegistryComponent {

	private static final Logger LOG = Logger.getLogger(DefaultValidationDelegateRegistryComponent.class.getName());

	public static final String NAME = "DefaultValidationDelegateRegistry";
	public static final String TARGET = "(" + EMFNamespaces.EMF_CONFIGURATOR_TYPE + "=VALIDATION_DELEGATE)";

	private final Registry registry = Registry.INSTANCE;

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
	public void addValidationDelegate(ValidationDelegate delegate, Map<String, Object> properties) {
		String name = (String) properties.get(EMFNamespaces.EMF_CONFIGURATOR_NAME);
		Object recent = registry.put(name, delegate);
		if (nonNull(recent)) {
			LOG.info(() -> String.format(
					"A validation delegate '%s' for '%s' was already registered and is now replaced by a new one",
					recent, name));
		}
	}

	public void removeValidationDelegate(ValidationDelegate delegate, Map<String, Object> properties) {
		registry.remove(properties.get(EMFNamespaces.EMF_CONFIGURATOR_NAME));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
	public void addValidationDelegateDescriptor(Descriptor descriptor, Map<String, Object> properties) {
		String name = (String) properties.get(EMFNamespaces.EMF_CONFIGURATOR_NAME);
		Object recent = registry.put(name, descriptor);
		if (nonNull(recent)) {
			LOG.info(() -> String.format(
					"A validation delegate descriptor '%s' for '%s' was already registered and is now replaced by a new one",
					recent, name));
		}
	}

	public void removeValidationDelegateDescriptor(Descriptor descriptor, Map<String, Object> properties) {
		registry.remove(properties.get(EMFNamespaces.EMF_CONFIGURATOR_NAME));
	}
}
