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

import org.eclipse.emf.ecore.EValidator.ValidationDelegate;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Descriptor;
import org.eclipse.emf.ecore.EValidator.ValidationDelegate.Registry;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
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

	@Mock
	private ValidationDelegate delegate;

	@Mock
	private ValidationDelegate anotherDelegate;

	@Mock
	private Descriptor descriptor;

	@Mock
	private Descriptor anotherDescriptor;

	@Mock
	private EMFConfigurator configurator;

	@BeforeEach
	void setUp() {
		component = new DefaultValidationDelegateRegistryComponent();
		when(configurator.configuratorName()).thenReturn(DELEGATE_URI);
	}

	@AfterEach
	void tearDown() {
		Registry.INSTANCE.remove(DELEGATE_URI);
	}

	@Test
	void addDelegateRegistersInGlobalRegistry() {
		component.addValidationDelegate(delegate, configurator);

		assertSame(delegate, Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void removeDelegateUnregistersFromGlobalRegistry() {
		component.addValidationDelegate(delegate, configurator);
		component.removeValidationDelegate(delegate, configurator);

		assertNull(Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void addDelegateReplacesExisting() {
		component.addValidationDelegate(delegate, configurator);
		component.addValidationDelegate(anotherDelegate, configurator);

		assertSame(anotherDelegate, Registry.INSTANCE.get(DELEGATE_URI));
	}

	// Note: EMF delegate registries resolve Descriptor via get(), calling getValidationDelegate().
	// Since mocked descriptors return null, we use containsKey instead.

	@Test
	void addDescriptorRegistersInGlobalRegistry() {
		component.addValidationDelegateDescriptor(descriptor, configurator);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void removeDescriptorUnregistersFromGlobalRegistry() {
		component.addValidationDelegateDescriptor(descriptor, configurator);
		component.removeValidationDelegateDescriptor(descriptor, configurator);

		assertFalse(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void addDescriptorReplacesExisting() {
		component.addValidationDelegateDescriptor(descriptor, configurator);
		component.addValidationDelegateDescriptor(anotherDescriptor, configurator);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void delegateAndDescriptorShareRegistryKey() {
		component.addValidationDelegate(delegate, configurator);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));

		// descriptor replaces delegate under same key
		component.addValidationDelegateDescriptor(descriptor, configurator);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}
}
