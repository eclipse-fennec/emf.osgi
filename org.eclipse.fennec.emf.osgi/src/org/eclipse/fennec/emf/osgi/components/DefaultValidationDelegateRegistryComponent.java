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

import java.util.logging.Logger;

import org.eclipse.emf.ecore.EValidator.ValidationDelegate;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Descriptor;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Registry;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Whiteboard populator that registers {@link ValidationDelegate} and
 * {@link Descriptor} services into the global {@link Registry#INSTANCE}.
 * <p>
 * Services must carry an {@link EMFConfigurator} annotation with
 * {@code configuratorType = VALIDATION_DELEGATE} and a {@code configuratorName}
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
	public static final String TARGET = "(configuratorType=VALIDATION_DELEGATE)";

	private final Registry registry = Registry.INSTANCE;

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
	public void addValidationDelegate(ValidationDelegate delegate, final EMFConfigurator properties) {
		Object recent = registry.put(properties.configuratorName(), delegate);
		if (recent != null) {
			LOG.info(() -> String.format(
					"A validation delegate '%s' for '%s' was already registered and is now replaced by a new one",
					recent, properties.configuratorName()));
		}
	}

	public void removeValidationDelegate(ValidationDelegate delegate, EMFConfigurator properties) {
		registry.remove(properties.configuratorName());
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
	public void addValidationDelegateDescriptor(Descriptor descriptor, final EMFConfigurator properties) {
		Object recent = registry.put(properties.configuratorName(), descriptor);
		if (recent != null) {
			LOG.info(() -> String.format(
					"A validation delegate descriptor '%s' for '%s' was already registered and is now replaced by a new one",
					recent, properties.configuratorName()));
		}
	}

	public void removeValidationDelegateDescriptor(Descriptor descriptor, EMFConfigurator properties) {
		registry.remove(properties.configuratorName());
	}
}
