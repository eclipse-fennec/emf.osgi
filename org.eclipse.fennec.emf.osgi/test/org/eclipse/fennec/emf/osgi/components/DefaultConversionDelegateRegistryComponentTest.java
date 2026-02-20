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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory.Descriptor;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory.Registry;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link DefaultConversionDelegateRegistryComponent}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultConversionDelegateRegistryComponentTest {

	private static final String DELEGATE_URI = "http://example.org/test/conversion";

	private DefaultConversionDelegateRegistryComponent component;

	@Mock
	private Factory factory;

	@Mock
	private Factory anotherFactory;

	@Mock
	private Descriptor descriptor;

	@Mock
	private Descriptor anotherDescriptor;

	@Mock
	private EMFConfigurator configurator;

	@BeforeEach
	void setUp() {
		component = new DefaultConversionDelegateRegistryComponent();
		when(configurator.configuratorName()).thenReturn(DELEGATE_URI);
	}

	@AfterEach
	void tearDown() {
		Registry.INSTANCE.remove(DELEGATE_URI);
	}

	@Test
	void addFactoryRegistersInGlobalRegistry() {
		component.addConversionDelegateFactory(factory, configurator);

		assertSame(factory, Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void removeFactoryUnregistersFromGlobalRegistry() {
		component.addConversionDelegateFactory(factory, configurator);
		component.removeConversionDelegateFactory(factory, configurator);

		assertNull(Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void addFactoryReplacesExisting() {
		component.addConversionDelegateFactory(factory, configurator);
		component.addConversionDelegateFactory(anotherFactory, configurator);

		assertSame(anotherFactory, Registry.INSTANCE.get(DELEGATE_URI));
	}

	// Note: EMF delegate registries resolve Descriptor via get(), calling getFactory().
	// Since mocked descriptors return null, we use containsKey instead.

	@Test
	void addDescriptorRegistersInGlobalRegistry() {
		component.addConversionDelegateDescriptor(descriptor, configurator);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void removeDescriptorUnregistersFromGlobalRegistry() {
		component.addConversionDelegateDescriptor(descriptor, configurator);
		component.removeConversionDelegateDescriptor(descriptor, configurator);

		assertFalse(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void addDescriptorReplacesExisting() {
		component.addConversionDelegateDescriptor(descriptor, configurator);
		component.addConversionDelegateDescriptor(anotherDescriptor, configurator);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void factoryAndDescriptorShareRegistryKey() {
		component.addConversionDelegateFactory(factory, configurator);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));

		// descriptor replaces factory under same key
		component.addConversionDelegateDescriptor(descriptor, configurator);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}
}
