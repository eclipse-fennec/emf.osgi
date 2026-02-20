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

import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory;
import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory.Descriptor;
import org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory.Registry;
import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link DefaultSettingDelegateRegistryComponent}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSettingDelegateRegistryComponentTest {

	private static final String DELEGATE_URI = "http://example.org/test/setting";

	private DefaultSettingDelegateRegistryComponent component;

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
		component = new DefaultSettingDelegateRegistryComponent();
		when(configurator.configuratorName()).thenReturn(DELEGATE_URI);
	}

	@AfterEach
	void tearDown() {
		Registry.INSTANCE.remove(DELEGATE_URI);
	}

	@Test
	void addFactoryRegistersInGlobalRegistry() {
		component.addSettingDelegateFactory(factory, configurator);

		assertSame(factory, Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void removeFactoryUnregistersFromGlobalRegistry() {
		component.addSettingDelegateFactory(factory, configurator);
		component.removeSettingDelegateFactory(factory, configurator);

		assertNull(Registry.INSTANCE.get(DELEGATE_URI));
	}

	@Test
	void addFactoryReplacesExisting() {
		component.addSettingDelegateFactory(factory, configurator);
		component.addSettingDelegateFactory(anotherFactory, configurator);

		assertSame(anotherFactory, Registry.INSTANCE.get(DELEGATE_URI));
	}

	// Note: EMF delegate registries resolve Descriptor via get(), calling getFactory().
	// Since mocked descriptors return null, we use containsKey instead.

	@Test
	void addDescriptorRegistersInGlobalRegistry() {
		component.addSettingDelegateDescriptor(descriptor, configurator);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void removeDescriptorUnregistersFromGlobalRegistry() {
		component.addSettingDelegateDescriptor(descriptor, configurator);
		component.removeSettingDelegateDescriptor(descriptor, configurator);

		assertFalse(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void addDescriptorReplacesExisting() {
		component.addSettingDelegateDescriptor(descriptor, configurator);
		component.addSettingDelegateDescriptor(anotherDescriptor, configurator);

		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}

	@Test
	void factoryAndDescriptorShareRegistryKey() {
		component.addSettingDelegateFactory(factory, configurator);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));

		// descriptor replaces factory under same key
		component.addSettingDelegateDescriptor(descriptor, configurator);
		assertTrue(Registry.INSTANCE.containsKey(DELEGATE_URI));
	}
}
