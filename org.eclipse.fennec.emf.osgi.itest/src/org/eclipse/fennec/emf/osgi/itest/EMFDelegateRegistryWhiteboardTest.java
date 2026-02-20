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
package org.eclipse.fennec.emf.osgi.itest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.dictionary.Dictionaries;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * OSGi integration tests that verify the whiteboard populator components
 * actually pick up delegate services and register them in EMF's static registries.
 * <p>
 * This tests the full OSGi wiring: service registration with {@code emf.configuratorType}
 * property → DS target filter match → populator calls {@code Registry.INSTANCE.put()}.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class EMFDelegateRegistryWhiteboardTest {

	private static final String DELEGATE_URI = "http://example.org/test/whiteboard";

	@InjectBundleContext
	BundleContext bc;

	@Test
	void invocationDelegateFactoryIsPickedUpByWhiteboard() throws Exception {
		EOperation.Internal.InvocationDelegate.Factory factory = operation ->
				(target, arguments) -> "invoked";

		ServiceRegistration<?> reg = bc.registerService(
				EOperation.Internal.InvocationDelegate.Factory.class,
				factory,
				Dictionaries.dictionaryOf(
						EMFNamespaces.EMF_CONFIGURATOR_TYPE, "OPERATION_INVOCATION_FACTORY",
						EMFNamespaces.EMF_CONFIGURATOR_NAME, DELEGATE_URI));
		try {
			Thread.sleep(200);
			assertSame(factory,
					EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE.get(DELEGATE_URI),
					"Invocation delegate factory should be in Registry.INSTANCE after service registration");
		} finally {
			reg.unregister();
			Thread.sleep(200);
		}
		assertNull(
				EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE.get(DELEGATE_URI),
				"Invocation delegate factory should be removed from Registry.INSTANCE after service unregistration");
	}

	@Test
	void settingDelegateFactoryIsPickedUpByWhiteboard() throws Exception {
		EStructuralFeature.Internal.SettingDelegate.Factory factory = feature ->
				new NoOpSettingDelegate();

		ServiceRegistration<?> reg = bc.registerService(
				EStructuralFeature.Internal.SettingDelegate.Factory.class,
				factory,
				Dictionaries.dictionaryOf(
						EMFNamespaces.EMF_CONFIGURATOR_TYPE, "SETTING_DELEGATE_FACTORY",
						EMFNamespaces.EMF_CONFIGURATOR_NAME, DELEGATE_URI));
		try {
			Thread.sleep(200);
			assertSame(factory,
					EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.get(DELEGATE_URI),
					"Setting delegate factory should be in Registry.INSTANCE after service registration");
		} finally {
			reg.unregister();
			Thread.sleep(200);
		}
		assertNull(
				EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.get(DELEGATE_URI),
				"Setting delegate factory should be removed from Registry.INSTANCE after service unregistration");
	}

	@Test
	void validationDelegateIsPickedUpByWhiteboard() throws Exception {
		EValidator.ValidationDelegate delegate = new NoOpValidationDelegate();

		ServiceRegistration<?> reg = bc.registerService(
				EValidator.ValidationDelegate.class,
				delegate,
				Dictionaries.dictionaryOf(
						EMFNamespaces.EMF_CONFIGURATOR_TYPE, "VALIDATION_DELEGATE",
						EMFNamespaces.EMF_CONFIGURATOR_NAME, DELEGATE_URI));
		try {
			Thread.sleep(200);
			assertSame(delegate,
					EValidator.ValidationDelegate.Registry.INSTANCE.get(DELEGATE_URI),
					"Validation delegate should be in Registry.INSTANCE after service registration");
		} finally {
			reg.unregister();
			Thread.sleep(200);
		}
		assertNull(
				EValidator.ValidationDelegate.Registry.INSTANCE.get(DELEGATE_URI),
				"Validation delegate should be removed from Registry.INSTANCE after service unregistration");
	}

	@Test
	void conversionDelegateFactoryIsPickedUpByWhiteboard() throws Exception {
		EDataType.Internal.ConversionDelegate.Factory factory = dataType ->
				new NoOpConversionDelegate();

		ServiceRegistration<?> reg = bc.registerService(
				EDataType.Internal.ConversionDelegate.Factory.class,
				factory,
				Dictionaries.dictionaryOf(
						EMFNamespaces.EMF_CONFIGURATOR_TYPE, "CONVERSION_DELEGATE_FACTORY",
						EMFNamespaces.EMF_CONFIGURATOR_NAME, DELEGATE_URI));
		try {
			Thread.sleep(200);
			assertSame(factory,
					EDataType.Internal.ConversionDelegate.Factory.Registry.INSTANCE.get(DELEGATE_URI),
					"Conversion delegate factory should be in Registry.INSTANCE after service registration");
		} finally {
			reg.unregister();
			Thread.sleep(200);
		}
		assertNull(
				EDataType.Internal.ConversionDelegate.Factory.Registry.INSTANCE.get(DELEGATE_URI),
				"Conversion delegate factory should be removed from Registry.INSTANCE after service unregistration");
	}

	// --- Minimal delegate stubs (interfaces with many methods need concrete classes) ---

	static class NoOpSettingDelegate implements EStructuralFeature.Internal.SettingDelegate {
		@Override
		public EStructuralFeature.Setting dynamicSetting(InternalEObject owner,
				EStructuralFeature.Internal.DynamicValueHolder settings, int dynamicFeatureID) {
			throw new UnsupportedOperationException();
		}
		@Override
		public Object dynamicGet(InternalEObject owner,
				EStructuralFeature.Internal.DynamicValueHolder settings,
				int dynamicFeatureID, boolean resolve, boolean coreType) {
			return null;
		}
		@Override
		public void dynamicSet(InternalEObject owner,
				EStructuralFeature.Internal.DynamicValueHolder settings,
				int dynamicFeatureID, Object newValue) {
		}
		@Override
		public boolean dynamicIsSet(InternalEObject owner,
				EStructuralFeature.Internal.DynamicValueHolder settings, int dynamicFeatureID) {
			return false;
		}
		@Override
		public void dynamicUnset(InternalEObject owner,
				EStructuralFeature.Internal.DynamicValueHolder settings, int dynamicFeatureID) {
		}
		@Override
		public org.eclipse.emf.common.notify.NotificationChain dynamicInverseAdd(
				InternalEObject owner, EStructuralFeature.Internal.DynamicValueHolder settings,
				int dynamicFeatureID, InternalEObject otherEnd,
				org.eclipse.emf.common.notify.NotificationChain notifications) {
			return notifications;
		}
		@Override
		public org.eclipse.emf.common.notify.NotificationChain dynamicInverseRemove(
				InternalEObject owner, EStructuralFeature.Internal.DynamicValueHolder settings,
				int dynamicFeatureID, InternalEObject otherEnd,
				org.eclipse.emf.common.notify.NotificationChain notifications) {
			return notifications;
		}
	}

	static class NoOpValidationDelegate implements EValidator.ValidationDelegate {
		@Override
		public boolean validate(EClass eClass, EObject eObject,
				Map<Object, Object> context, EOperation invariant, String expression) {
			return true;
		}
		@Override
		public boolean validate(EClass eClass, EObject eObject,
				Map<Object, Object> context, String constraint, String expression) {
			return true;
		}
		@Override
		public boolean validate(EDataType eDataType, Object value,
				Map<Object, Object> context, String constraint, String expression) {
			return true;
		}
	}

	static class NoOpConversionDelegate implements ConversionDelegate {
		@Override
		public String convertToString(Object value) {
			return value != null ? value.toString() : null;
		}
		@Override
		public Object createFromString(String literal) {
			return literal;
		}
	}
}
