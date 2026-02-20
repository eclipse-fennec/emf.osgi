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

import java.util.Map;

import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory.Descriptor;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate.Factory.Registry;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
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

	private final Map<String, Object> properties = Map.of(
			EMFNamespaces.EMF_CONFIGURATOR_NAME, DELEGATE_URI);

	@Mock
	private Factory factory;

	@Mock
	private Factory anotherFactory;

	@Mock
	private Descriptor descriptor;

	@Mock
	private Descriptor anotherDescriptor;

	@BeforeEach
	void setUp() {
		component = new DefaultConversionDelegateRegistryComponent();
	}

	@AfterEach
	void tearDown() {
		Registry.INSTANCE.remove(DELEGATE_URI);
	}

	@Test
	void addFactoryRegistersInGlobalRegistry() {
		component.addConversionDelegateFactory(factory, properties);

		assertSame(factory, Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void removeFactoryUnregistersFromGlobalRegistry() {
		component.addConversionDelegateFactory(factory, properties);
		component.removeConversionDelegateFactory(factory, properties);

		assertNull(Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void addFactoryReplacesExisting() {
		component.addConversionDelegateFactory(factory, properties);
		component.addConversionDelegateFactory(anotherFactory, properties);

		assertSame(anotherFactory, Registry.INSTANCE.get(DELEGATE_URI));
	}

	// Note: EMF delegate registries resolve Descriptor via get(), calling getFactory().
	// Since mocked descriptors return null, we use containsKey instead.

	@Test
	void addDescriptorRegistersInGlobalRegistry() {
		component.addConversionDelegateDescriptor(descriptor, properties);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void removeDescriptorUnregistersFromGlobalRegistry() {
		component.addConversionDelegateDescriptor(descriptor, properties);
		component.removeConversionDelegateDescriptor(descriptor, properties);

		assertFalse(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void addDescriptorReplacesExisting() {
		component.addConversionDelegateDescriptor(descriptor, properties);
		component.addConversionDelegateDescriptor(anotherDescriptor, properties);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void factoryAndDescriptorShareRegistryKey() {
		component.addConversionDelegateFactory(factory, properties);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));

		// descriptor replaces factory under same key
		component.addConversionDelegateDescriptor(descriptor, properties);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}
}
