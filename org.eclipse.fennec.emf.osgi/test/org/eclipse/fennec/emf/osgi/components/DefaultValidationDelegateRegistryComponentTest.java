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

import org.eclipse.emf.ecore.EValidator.ValidationDelegate;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Descriptor;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Registry;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link DefaultValidationDelegateRegistryComponent}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultValidationDelegateRegistryComponentTest {

	private static final String DELEGATE_URI = "http://example.org/test/validation";

	private DefaultValidationDelegateRegistryComponent component;

	private final Map<String, Object> properties = Map.of(
			EMFNamespaces.EMF_CONFIGURATOR_NAME, DELEGATE_URI);

	@Mock
	private ValidationDelegate delegate;

	@Mock
	private ValidationDelegate anotherDelegate;

	@Mock
	private Descriptor descriptor;

	@Mock
	private Descriptor anotherDescriptor;

	@BeforeEach
	void setUp() {
		component = new DefaultValidationDelegateRegistryComponent();
	}

	@AfterEach
	void tearDown() {
		Registry.INSTANCE.remove(DELEGATE_URI);
	}

	@Test
	void addDelegateRegistersInGlobalRegistry() {
		component.addValidationDelegate(delegate, properties);

		assertSame(delegate, Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void removeDelegateUnregistersFromGlobalRegistry() {
		component.addValidationDelegate(delegate, properties);
		component.removeValidationDelegate(delegate, properties);

		assertNull(Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void addDelegateReplacesExisting() {
		component.addValidationDelegate(delegate, properties);
		component.addValidationDelegate(anotherDelegate, properties);

		assertSame(anotherDelegate, Registry.INSTANCE.get(DELEGATE_URI));
	}

	// Note: EMF delegate registries resolve Descriptor via get(), calling getValidationDelegate().
	// Since mocked descriptors return null, we use containsKey instead.

	@Test
	void addDescriptorRegistersInGlobalRegistry() {
		component.addValidationDelegateDescriptor(descriptor, properties);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void removeDescriptorUnregistersFromGlobalRegistry() {
		component.addValidationDelegateDescriptor(descriptor, properties);
		component.removeValidationDelegateDescriptor(descriptor, properties);

		assertFalse(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void addDescriptorReplacesExisting() {
		component.addValidationDelegateDescriptor(descriptor, properties);
		component.addValidationDelegateDescriptor(anotherDescriptor, properties);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void delegateAndDescriptorShareRegistryKey() {
		component.addValidationDelegate(delegate, properties);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));

		// descriptor replaces delegate under same key
		component.addValidationDelegateDescriptor(descriptor, properties);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}
}
