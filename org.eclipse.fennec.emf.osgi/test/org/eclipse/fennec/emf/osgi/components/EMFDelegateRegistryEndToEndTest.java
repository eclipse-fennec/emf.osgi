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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.fennec.emf.osgi.helper.EcoreHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for EMF delegate registries (Invocation, Setting, Validation, Conversion).
 *
 * These tests load a real ecore model with delegate annotations and verify that EMF
 * resolves delegates from the static registries and invokes them correctly.
 */
class EMFDelegateRegistryEndToEndTest {

	private static final String DELEGATE_URI = "http://example.org/test/delegates";

	private EcoreHelper ecoreHelper;

	@BeforeEach
	void setUp() {
		ecoreHelper = new EcoreHelper();
	}

	@AfterEach
	void tearDown() {
		EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE.remove(DELEGATE_URI);
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.remove(DELEGATE_URI);
		EValidator.ValidationDelegate.Registry.INSTANCE.remove(DELEGATE_URI);
		EDataType.Internal.ConversionDelegate.Factory.Registry.INSTANCE.remove(DELEGATE_URI);
	}

	private EPackage loadFreshPackage() {
		try {
			return ecoreHelper.loadEcoreDetached("delegates-test.ecore", getClass());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private EClass getPersonClass(EPackage ePackage) {
		return (EClass) ePackage.getEClassifier("Person");
	}

	private EDataType getUpperStringType(EPackage ePackage) {
		return (EDataType) ePackage.getEClassifier("UpperString");
	}

	private EObject createPerson(EPackage ePackage) {
		EClass personClass = getPersonClass(ePackage);
		return ePackage.getEFactoryInstance().create(personClass);
	}

	// --- Invocation Delegate ---

	@Test
	void invocationDelegateIsResolvedAndInvoked() throws Exception {
		EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE
				.put(DELEGATE_URI, new TestInvocationDelegateFactory());

		EPackage ePackage = loadFreshPackage();
		EClass personClass = getPersonClass(ePackage);
		EObject person = createPerson(ePackage);

		EOperation greetOp = personClass.getEOperations().get(0);
		assertEquals("greet", greetOp.getName());

		EList<Object> args = new BasicEList<>();
		Object result = ((InternalEObject) person).eInvoke(greetOp, args);
		assertEquals("Hello from delegate", result);
	}

	// --- Setting Delegate ---

	@Test
	void settingDelegateIsResolvedForDerivedAttribute() {
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE
				.put(DELEGATE_URI, new TestSettingDelegateFactory());

		EPackage ePackage = loadFreshPackage();
		EClass personClass = getPersonClass(ePackage);
		EObject person = createPerson(ePackage);

		EAttribute upperName = (EAttribute) personClass.getEStructuralFeature("upperName");
		assertNotNull(upperName);

		Object value = person.eGet(upperName);
		assertEquals("DERIVED_VALUE", value);
	}

	@Test
	void settingDelegateSetThrowsForDerived() {
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE
				.put(DELEGATE_URI, new TestSettingDelegateFactory());

		EPackage ePackage = loadFreshPackage();
		EClass personClass = getPersonClass(ePackage);
		EObject person = createPerson(ePackage);

		EAttribute upperName = (EAttribute) personClass.getEStructuralFeature("upperName");
		// EMF rejects eSet on non-changeable features with IllegalArgumentException
		// before the delegate is even reached
		assertThrows(IllegalArgumentException.class, () -> person.eSet(upperName, "x"));
	}

	// --- Validation Delegate ---

	@Test
	void validationDelegateIsInvokedForConstraint() {
		TestValidationDelegate validationDelegate = new TestValidationDelegate(true);
		EValidator.ValidationDelegate.Registry.INSTANCE.put(DELEGATE_URI, validationDelegate);

		EPackage ePackage = loadFreshPackage();
		EObject person = createPerson(ePackage);
		person.eSet(person.eClass().getEStructuralFeature("name"), "Alice");

		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(person);
		assertTrue(validationDelegate.validateCalled, "Validation delegate should have been invoked");
		assertEquals(Diagnostic.OK, diagnostic.getSeverity());
	}

	// --- Conversion Delegate ---

	@Test
	void conversionDelegateConvertToString() {
		EDataType.Internal.ConversionDelegate.Factory.Registry.INSTANCE
				.put(DELEGATE_URI, new TestConversionDelegateFactory());

		EPackage ePackage = loadFreshPackage();
		EDataType upperStringType = getUpperStringType(ePackage);
		EFactory eFactory = ePackage.getEFactoryInstance();

		String result = eFactory.convertToString(upperStringType, "hello");
		assertEquals("HELLO", result);
	}

	@Test
	void conversionDelegateCreateFromString() {
		EDataType.Internal.ConversionDelegate.Factory.Registry.INSTANCE
				.put(DELEGATE_URI, new TestConversionDelegateFactory());

		EPackage ePackage = loadFreshPackage();
		EDataType upperStringType = getUpperStringType(ePackage);
		EFactory eFactory = ePackage.getEFactoryInstance();

		Object result = eFactory.createFromString(upperStringType, "hello");
		assertEquals("hello", result);
	}

	// --- Combined ---

	@Test
	void allFourDelegatesWorkTogether() throws Exception {
		TestInvocationDelegateFactory invocationFactory = new TestInvocationDelegateFactory();
		TestSettingDelegateFactory settingFactory = new TestSettingDelegateFactory();
		TestValidationDelegate validationDelegate = new TestValidationDelegate(true);
		TestConversionDelegateFactory conversionFactory = new TestConversionDelegateFactory();

		EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE
				.put(DELEGATE_URI, invocationFactory);
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE
				.put(DELEGATE_URI, settingFactory);
		EValidator.ValidationDelegate.Registry.INSTANCE
				.put(DELEGATE_URI, validationDelegate);
		EDataType.Internal.ConversionDelegate.Factory.Registry.INSTANCE
				.put(DELEGATE_URI, conversionFactory);

		EPackage ePackage = loadFreshPackage();
		EClass personClass = getPersonClass(ePackage);
		EObject person = createPerson(ePackage);
		person.eSet(personClass.getEStructuralFeature("name"), "Alice");

		// Invocation
		EOperation greetOp = personClass.getEOperations().get(0);
		Object greetResult = ((InternalEObject) person).eInvoke(greetOp, new BasicEList<>());
		assertEquals("Hello from delegate", greetResult);

		// Setting
		EAttribute upperName = (EAttribute) personClass.getEStructuralFeature("upperName");
		assertEquals("DERIVED_VALUE", person.eGet(upperName));

		// Validation
		Diagnostic diagnostic = Diagnostician.INSTANCE.validate(person);
		assertTrue(validationDelegate.validateCalled);
		assertEquals(Diagnostic.OK, diagnostic.getSeverity());

		// Conversion
		EDataType upperStringType = getUpperStringType(ePackage);
		EFactory eFactory = ePackage.getEFactoryInstance();
		assertEquals("HELLO", eFactory.convertToString(upperStringType, "hello"));
		assertEquals("hello", eFactory.createFromString(upperStringType, "hello"));
	}

	// ========== Dummy Delegate Implementations ==========

	static class TestInvocationDelegateFactory implements EOperation.Internal.InvocationDelegate.Factory {
		@Override
		public EOperation.Internal.InvocationDelegate createInvocationDelegate(EOperation operation) {
			return new TestInvocationDelegate();
		}
	}

	static class TestInvocationDelegate implements EOperation.Internal.InvocationDelegate {
		@Override
		public Object dynamicInvoke(InternalEObject target, EList<?> arguments)
				throws java.lang.reflect.InvocationTargetException {
			return "Hello from delegate";
		}
	}

	static class TestSettingDelegateFactory implements EStructuralFeature.Internal.SettingDelegate.Factory {
		@Override
		public EStructuralFeature.Internal.SettingDelegate createSettingDelegate(EStructuralFeature eStructuralFeature) {
			return new TestSettingDelegate();
		}
	}

	static class TestSettingDelegate implements EStructuralFeature.Internal.SettingDelegate {
		@Override
		public EStructuralFeature.Setting dynamicSetting(InternalEObject owner,
				EStructuralFeature.Internal.DynamicValueHolder settings, int dynamicFeatureID) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object dynamicGet(InternalEObject owner, EStructuralFeature.Internal.DynamicValueHolder settings,
				int index, boolean resolve, boolean coreType) {
			return "DERIVED_VALUE";
		}

		@Override
		public void dynamicSet(InternalEObject owner, EStructuralFeature.Internal.DynamicValueHolder settings,
				int index, Object newValue) {
			throw new UnsupportedOperationException("Derived attribute is read-only");
		}

		@Override
		public boolean dynamicIsSet(InternalEObject owner, EStructuralFeature.Internal.DynamicValueHolder settings,
				int index) {
			return true;
		}

		@Override
		public void dynamicUnset(InternalEObject owner, EStructuralFeature.Internal.DynamicValueHolder settings,
				int index) {
			throw new UnsupportedOperationException("Derived attribute is read-only");
		}

		@Override
		public NotificationChain dynamicInverseAdd(InternalEObject owner,
				EStructuralFeature.Internal.DynamicValueHolder settings, int dynamicFeatureID,
				InternalEObject otherEnd, NotificationChain notifications) {
			throw new UnsupportedOperationException();
		}

		@Override
		public NotificationChain dynamicInverseRemove(InternalEObject owner,
				EStructuralFeature.Internal.DynamicValueHolder settings, int dynamicFeatureID,
				InternalEObject otherEnd, NotificationChain notifications) {
			throw new UnsupportedOperationException();
		}
	}

	static class TestValidationDelegate implements EValidator.ValidationDelegate {

		boolean validateCalled = false;
		private final boolean result;

		TestValidationDelegate(boolean result) {
			this.result = result;
		}

		@Override
		public boolean validate(EClass eClass, EObject eObject,
				java.util.Map<Object, Object> context, EOperation invariant, String expression) {
			validateCalled = true;
			return result;
		}

		@Override
		public boolean validate(EClass eClass, EObject eObject,
				java.util.Map<Object, Object> context, String constraint, String expression) {
			validateCalled = true;
			return result;
		}

		@Override
		public boolean validate(EDataType eDataType, Object value,
				java.util.Map<Object, Object> context, String constraint, String expression) {
			validateCalled = true;
			return result;
		}
	}

	static class TestConversionDelegateFactory implements EDataType.Internal.ConversionDelegate.Factory {
		@Override
		public EDataType.Internal.ConversionDelegate createConversionDelegate(EDataType eDataType) {
			return new TestConversionDelegate();
		}
	}

	static class TestConversionDelegate implements EDataType.Internal.ConversionDelegate {
		@Override
		public String convertToString(Object value) {
			return value.toString().toUpperCase();
		}

		@Override
		public Object createFromString(String literal) {
			return literal;
		}
	}
}
