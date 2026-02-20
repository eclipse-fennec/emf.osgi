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

import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory.Descriptor;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory.Registry;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Whiteboard populator that registers {@link Factory} and {@link Descriptor}
 * services into the global {@link Registry#INSTANCE} for conversion delegates.
 * <p>
 * Services must carry an {@link EMFConfigurator} annotation with
 * {@code configuratorType = CONVERSION_DELEGATE_FACTORY} and a {@code configuratorName}
 * matching the delegate URI (e.g. {@code "http://www.eclipse.org/emf/2002/Ecore/OCL"}).
 *
 * @author Mark Hoffmann
 * @since 20.02.2026
 * @see org.eclipse.fennec.emf.osgi.annotation.ConfiguratorType#CONVERSION_DELEGATE_FACTORY
 */
@Component(name = DefaultConversionDelegateRegistryComponent.NAME)
@ProviderType
public class DefaultConversionDelegateRegistryComponent {

	private static final Logger LOG = Logger.getLogger(DefaultConversionDelegateRegistryComponent.class.getName());

	public static final String NAME = "DefaultConversionDelegateRegistry";
	public static final String TARGET = "(configuratorType=CONVERSION_DELEGATE_FACTORY)";

	private final Registry registry = Registry.INSTANCE;

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
	public void addConversionDelegateFactory(Factory factory, final EMFConfigurator properties) {
		Object recent = registry.put(properties.configuratorName(), factory);
		if (recent != null) {
			LOG.info(() -> String.format(
					"A conversion delegate factory '%s' for '%s' was already registered and is now replaced by a new one",
					recent, properties.configuratorName()));
		}
	}

	public void removeConversionDelegateFactory(Factory factory, EMFConfigurator properties) {
		registry.remove(properties.configuratorName());
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = TARGET)
	public void addConversionDelegateDescriptor(Descriptor descriptor, final EMFConfigurator properties) {
		Object recent = registry.put(properties.configuratorName(), descriptor);
		if (recent != null) {
			LOG.info(() -> String.format(
					"A conversion delegate factory descriptor '%s' for '%s' was already registered and is now replaced by a new one",
					recent, properties.configuratorName()));
		}
	}

	public void removeConversionDelegateDescriptor(Descriptor descriptor, EMFConfigurator properties) {
		registry.remove(properties.configuratorName());
	}
}
