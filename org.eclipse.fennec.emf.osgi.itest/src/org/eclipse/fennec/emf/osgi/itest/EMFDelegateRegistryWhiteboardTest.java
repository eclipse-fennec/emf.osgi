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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EDataType.Internal.ConversionDelegate;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.example.model.basic.BasicPackage;
import org.eclipse.fennec.emf.osgi.example.model.extended.ExtendedPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
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
	
	/**
	 * 5b) Demonstrates inconsistency in OSGi ResourceSet PackageRegistry:
	 * containsKey() delegates to parent (global) registry and finds packages,
	 * but keySet() only returns local entries and is empty.
	 * This means iteration-based access and key-based lookup behave differently.
	 */
	@Test
	void testPackageRegistryDelegationInconsistency(
	        @InjectService(filter = "(&(emf.name=basic)(emf.name=extended))", timeout = 5000) ResourceSet rs) {

	    EPackage.Registry registry = rs.getPackageRegistry();

	    // --- containsKey: delegates to global parent registry ---
	    boolean containsBasic = registry.containsKey(BasicPackage.eNS_URI);
	    boolean containsEp = registry.containsKey(ExtendedPackage.eNS_URI);

	    // --- get: also delegates ---
	    Object basicViaGet = registry.get(BasicPackage.eNS_URI);
	    Object exViaGet = registry.get(ExtendedPackage.eNS_URI);

	    // --- keySet: only local entries ---
	    boolean keySetContainsBasic = registry.keySet().contains(BasicPackage.eNS_URI);
	    boolean keySetContainsEx = registry.keySet().contains(ExtendedPackage.eNS_URI);

	    // --- entrySet: only local entries ---
	    boolean entrySetHasBasic = registry.entrySet().stream()
	            .anyMatch(e -> BasicPackage.eNS_URI.equals(e.getKey()));
	    boolean entrySetHasEx = registry.entrySet().stream()
	            .anyMatch(e -> ExtendedPackage.eNS_URI.equals(e.getKey()));

	    // --- global registry for comparison ---
	    boolean globalHasBasic = EPackage.Registry.INSTANCE.containsKey(BasicPackage.eNS_URI);
	    boolean globalHasEx = EPackage.Registry.INSTANCE.containsKey(ExtendedPackage.eNS_URI);

	    System.out.println("=== PackageRegistry Delegation Inconsistency ===");
	    System.out.println("  registry class: " + registry.getClass().getName());
	    System.out.println();
	    System.out.println("  Global EPackage.Registry.INSTANCE:");
	    System.out.println("    containsKey Basic:          " + globalHasBasic);
	    System.out.println("    containsKey Extended:   " + globalHasEx);
	    System.out.println();
	    System.out.println("  ResourceSet PackageRegistry (via containsKey - delegates to parent):");
	    System.out.println("    containsKey Basic:          " + containsBasic);
	    System.out.println("    containsKey Extended:   " + containsEp);
	    System.out.println();
	    System.out.println("  ResourceSet PackageRegistry (via get - delegates to parent):");
	    System.out.println("    get Basic:                  " + (basicViaGet != null ? "found" : "NULL"));
	    System.out.println("    get Extended:           " + (exViaGet != null ? "found" : "NULL"));
	    System.out.println();
	    System.out.println("  ResourceSet PackageRegistry (via keySet - NO delegation):");
	    System.out.println("    keySet contains Basic:      " + keySetContainsBasic);
	    System.out.println("    keySet contains Extended: " + keySetContainsEx);
	    System.out.println("    keySet size:               " + registry.keySet().size());
	    System.out.println("    keySet entries:            " + registry.keySet());
	    System.out.println();
	    System.out.println("  ResourceSet PackageRegistry (via entrySet - NO delegation):");
	    System.out.println("    entrySet has Basic:         " + entrySetHasBasic);
	    System.out.println("    entrySet has Extended:  " + entrySetHasEx);

	    // Assertions that show the inconsistency:
	    // containsKey finds packages (via delegation)
	    assertThat(containsBasic).as("containsKey finds Basic (delegates to global)").isTrue();
	    assertThat(containsEp).as("containsKey finds epersistence (delegates to global)").isTrue();
	    // get also finds them
	    assertThat(basicViaGet).as("get finds Basic (delegates to global)").isNotNull();
	    assertThat(exViaGet).as("get finds epersistence (delegates to global)").isNotNull();
	    // BUT keySet does NOT contain them — this is the inconsistency
	    // If this assertion fails, the bug is fixed in fennecEMF
	    assertThat(keySetContainsBasic).as("keySet contains eorm (should be true if local registry is populated)").isTrue();
	    assertThat(keySetContainsEx).as("keySet contains epersistence (should be true if local registry is populated)").isTrue();
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
