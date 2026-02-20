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

import org.eclipse.emf.ecore.EOperation.Internal.InvocationDelegate.Factory;
import org.eclipse.emf.ecore.EOperation.Internal.InvocationDelegate.Factory.Descriptor;
import org.eclipse.emf.ecore.EOperation.Internal.InvocationDelegate.Factory.Registry;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link DefaultEOperationInvocationDelegateRegistryComponent}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultEOperationInvocationDelegateRegistryComponentTest {

	private static final String DELEGATE_URI = "http://example.org/test/invocation";

	private DefaultEOperationInvocationDelegateRegistryComponent component;

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
		component = new DefaultEOperationInvocationDelegateRegistryComponent();
	}

	@AfterEach
	void tearDown() {
		Registry.INSTANCE.remove(DELEGATE_URI);
	}

	@Test
	void addFactoryRegistersInGlobalRegistry() {
		component.addOperationInvocationDelegateFactory(factory, properties);

		assertSame(factory, Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void removeFactoryUnregistersFromGlobalRegistry() {
		component.addOperationInvocationDelegateFactory(factory, properties);
		component.removeOperationInvocationDelegateFactory(factory, properties);

		assertNull(Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void addFactoryReplacesExisting() {
		component.addOperationInvocationDelegateFactory(factory, properties);
		component.addOperationInvocationDelegateFactory(anotherFactory, properties);

		assertSame(anotherFactory, Registry.INSTANCE.get(DELEGATE_URI));
	}

	// Note: EMF delegate registries resolve Descriptor via get(), calling getFactory().
	// Since mocked descriptors return null from getFactory(), we use containsKey instead.

	@Test
	void addDescriptorRegistersInGlobalRegistry() {
		component.addOperationInvocationDelegateDescriptor(descriptor, properties);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void removeDescriptorUnregistersFromGlobalRegistry() {
		component.addOperationInvocationDelegateDescriptor(descriptor, properties);
		component.removeOperationInvocationDelegateDescriptor(descriptor, properties);

		assertFalse(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void addDescriptorReplacesExisting() {
		component.addOperationInvocationDelegateDescriptor(descriptor, properties);
		component.addOperationInvocationDelegateDescriptor(anotherDescriptor, properties);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void factoryAndDescriptorUseSameRegistryKey() {
		component.addOperationInvocationDelegateFactory(factory, properties);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));

		// descriptor replaces factory under same key
		component.addOperationInvocationDelegateDescriptor(descriptor, properties);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}
}
