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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.example.model.basic.BasicPackage;
import org.eclipse.fennec.emf.osgi.example.model.extended.ExtendedPackage;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * The pin test for the build-time fingerprint constant (issue #58): generated bundles
 * carry the fingerprint as a literal that the generator computed from the {@code .ecore}, so nothing recomputes it
 * at bind time and nothing would notice if it went stale.
 * <p>
 * This test is that notice. It compares the registered property against the value the
 * runtime computes from the loaded {@link EPackage}, which fails on exactly the drift
 * cases the constant introduces: {@code src-gen} not regenerated after a model change, a
 * hand-edited constant, or a residual defect in the equivalence the constant rests on
 * (guarded from the other side by {@code FingerprintEquivalenceGateTest}).
 * <p>
 * It runs over <em>every</em> registered package rather than a generated list per model:
 * no wiring in the model bundles, and every model added to the runtime later is covered
 * the moment it is registered.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class FingerprintConstantDriftTest {

	/**
	 * The published capability contract (#59): namespace and attribute names are asserted
	 * as literals on purpose. They are what foreign tooling filters on, so a rename must
	 * fail here rather than silently break every consumer — the same rule the fp1 golden
	 * values follow. Mirrors {@code EPackage.NAMESPACE} and
	 * {@code EPackage.FINGERPRINT_ATTRIBUTE} in the api bundle.
	 */
	private static final String MODEL_CAPABILITY_NAMESPACE = "org.eclipse.emf.ecore.generated_package";
	private static final String FINGERPRINT_ATTRIBUTE = "emf.fingerprint";
	private static final String URI_ATTRIBUTE = "uri";

	/**
	 * The generated path (#58): both generated example models carry the property, and the
	 * build-time literal is the value the runtime computes for the same model.
	 */
	@Test
	public void testGeneratedModelsCarryTheBuildTimeConstant(@InjectBundleContext BundleContext context)
			throws Exception {
		assertPinned(context, BasicPackage.eNS_URI, BasicPackage.eINSTANCE);
		// extended cross-references basic and Ecore — the case where an unresolved proxy
		// at generation time would have burned a degraded value into the constant.
		assertPinned(context, ExtendedPackage.eNS_URI, ExtendedPackage.eINSTANCE);
	}

	/**
	 * The sweep: no registered package anywhere in the runtime advertises a fingerprint
	 * that differs from its computed one, whatever path registered it.
	 */
	@Test
	public void testNoRegisteredPackageDriftsFromItsComputedFingerprint(@InjectBundleContext BundleContext context)
			throws Exception {
		Collection<ServiceReference<EPackage>> references = context.getServiceReferences(EPackage.class, null);
		assertThat(references).as("the runtime must have registered models to check").isNotEmpty();

		List<String> drifted = new ArrayList<>();
		int checked = 0;
		for (ServiceReference<EPackage> reference : references) {
			Object advertised = reference.getProperty(EMFNamespaces.EMF_MODEL_FINGERPRINT);
			if (!(advertised instanceof String value)) {
				continue;
			}
			EPackage ePackage = context.getService(reference);
			if (ePackage == null) {
				continue;
			}
			try {
				checked++;
				String computed = FingerprintHelper.fingerprint(ePackage);
				if (!value.equals(computed)) {
					drifted.add(ePackage.getNsURI() + ": advertised " + value + ", computed " + computed);
				}
			} finally {
				context.ungetService(reference);
			}
		}
		assertThat(checked).as("no registered package carried a fingerprint at all").isPositive();
		assertThat(drifted).as("registered fingerprints must match the computed ones").isEmpty();
	}

	/**
	 * The manifest capability (#59): the fingerprint written into
	 * {@code Provide-Capability} is the same value the runtime computes. This is the
	 * assertion that makes the manifest usable without a framework — atlas ingestion, OBR
	 * indexing and {@code Require-Capability} matching all read this value and never see
	 * the service property.
	 */
	@Test
	public void testManifestCapabilityCarriesTheSameFingerprint(@InjectBundleContext BundleContext context) {
		List<String> mismatches = new ArrayList<>();
		List<String> seen = new ArrayList<>();
		for (Bundle bundle : context.getBundles()) {
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			if (wiring == null) {
				continue;
			}
			for (BundleCapability capability : wiring.getCapabilities(MODEL_CAPABILITY_NAMESPACE)) {
				Object advertised = capability.getAttributes().get(FINGERPRINT_ATTRIBUTE);
				if (advertised == null) {
					// A model that cannot state its fingerprint omits the attribute
					// entirely rather than advertising an empty one.
					continue;
				}
				String nsURI = String.valueOf(capability.getAttributes().get(URI_ATTRIBUTE));
				seen.add(nsURI);
				String computed = computedFor(context, nsURI);
				if (computed == null) {
					mismatches.add(nsURI + ": capability declares " + advertised + ", but no package is registered");
				} else if (!computed.equals(advertised)) {
					mismatches.add(nsURI + ": capability declares " + advertised + ", computed " + computed);
				}
			}
		}
		assertThat(seen).as("the generated example models must declare the capability attribute")
				.contains(BasicPackage.eNS_URI, ExtendedPackage.eNS_URI);
		assertThat(mismatches).as("manifest fingerprints must match the computed ones").isEmpty();
	}

	private static String computedFor(BundleContext context, String nsURI) {
		try {
			Collection<ServiceReference<EPackage>> references = context.getServiceReferences(EPackage.class,
					"(" + EMFNamespaces.EMF_MODEL_NSURI + "=" + nsURI + ")");
			for (ServiceReference<EPackage> reference : references) {
				EPackage ePackage = context.getService(reference);
				if (ePackage == null) {
					continue;
				}
				try {
					return FingerprintHelper.fingerprint(ePackage);
				} finally {
					context.ungetService(reference);
				}
			}
			return null;
		} catch (InvalidSyntaxException e) {
			throw new IllegalStateException("cannot filter for " + nsURI, e);
		}
	}

	private static void assertPinned(BundleContext context, String nsURI, EPackage ePackage) throws Exception {
		Collection<ServiceReference<EPackage>> references = context.getServiceReferences(EPackage.class,
				"(" + EMFNamespaces.EMF_MODEL_NSURI + "=" + nsURI + ")");
		assertThat(references).as("no EPackage service registered for " + nsURI).isNotEmpty();
		for (ServiceReference<EPackage> reference : references) {
			Object advertised = reference.getProperty(EMFNamespaces.EMF_MODEL_FINGERPRINT);
			assertThat(advertised).as("generated model " + nsURI + " must advertise its build-time fingerprint")
					.isInstanceOf(String.class);
			assertThat((String) advertised)
					.startsWith("fp1:")
					.as("the generated constant for " + nsURI + " is stale or was hand-edited")
					.isEqualTo(FingerprintHelper.fingerprint(ePackage));
		}
	}
}
