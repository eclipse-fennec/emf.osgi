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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * The {@code emf.fingerprint} property on every model registration path (issue #55,
 * decision M5 in {@code docs/metadata-migration.md}): each emitting site registers the
 * {@link EPackage} service with the fingerprint already present, and two packages
 * sharing one nsURI carry distinct values — the property the whole mechanism exists for
 * (the nsURI alone is never the key).
 * <p>
 * The extender path is asserted in the extender itest module; the generated-code path
 * follows with the build-time constant (#58).
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class FingerprintPropertyIntegrationTest {

	private static final String VERSIONED_NS_URI = "http://fennec.eclipse.org/itest/versioned/1.0";

	@InjectService
	ConfigurationAdmin ca;

	/**
	 * Ecore path ({@code EcorePackagesRegistrator}): the statically registered Ecore
	 * package carries the fingerprint, and it is the value the static helper computes.
	 */
	@Test
	public void testEcorePathCarriesTheFingerprint(@InjectService ServiceAware<EcorePackage> ecoreAware) {
		assertThat(ecoreAware).isNotNull();
		Object fingerprint = ecoreAware.getServiceReference().getProperty(EMFNamespaces.EMF_MODEL_FINGERPRINT);
		assertThat(fingerprint).isInstanceOf(String.class);
		assertThat((String) fingerprint)
				.startsWith("fp1:")
				.isEqualTo(FingerprintHelper.fingerprint(EcorePackage.eINSTANCE));
	}

	/**
	 * Static registry path ({@code StaticEPackageRegistryComponent}): the aggregating
	 * registry service exposes the fingerprints of the packages it tracks.
	 */
	@Test
	public void testStaticRegistryAggregatesFingerprints(
			@InjectService(filter = "(emf.default.epackage.registry=true)") ServiceAware<EPackage.Registry> registryAware) {
		assertThat(registryAware).isNotNull();
		Object fingerprints = registryAware.getServiceReference().getProperty(EMFNamespaces.EMF_MODEL_FINGERPRINT);
		assertNotNull(fingerprints, "the aggregated registry properties must expose the tracked fingerprints");
		assertThat(flatten(fingerprints)).isNotEmpty().allSatisfy(value -> assertThat(value).startsWith("fp1:"));
	}

	/**
	 * Dynamic path ({@code DynamicPackageLoader}): a dynamically loaded model is
	 * registered with the fingerprint present from the first instant.
	 */
	@Test
	public void testDynamicPathCarriesTheFingerprint(
			@InjectService(cardinality = 0, filter = "(" + EMFNamespaces.EMF_MODEL_NSURI + "=" + VERSIONED_NS_URI
					+ ")") ServiceAware<EPackage> versionedAware)
			throws Exception {
		assertThat(versionedAware.isEmpty()).isTrue();
		Configuration config = dynamicModel("versioned-a.ecore");
		try {
			EPackage ePackage = versionedAware.waitForService(10_000L);
			assertNotNull(ePackage);
			Object fingerprint = versionedAware.getServiceReference().getProperty(EMFNamespaces.EMF_MODEL_FINGERPRINT);
			assertThat(fingerprint).isInstanceOf(String.class);
			assertThat((String) fingerprint)
					.startsWith("fp1:")
					.isEqualTo(FingerprintHelper.fingerprint(ePackage));
		} finally {
			config.delete();
		}
	}

	/**
	 * The multi-version assertion: two models under the <b>same nsURI</b> but with
	 * different content are registered with <b>distinct</b> fingerprints.
	 */
	@Test
	public void testSameNsUriTwoVersionsCarryDistinctFingerprints(
			@InjectService(cardinality = 0, filter = "(" + EMFNamespaces.EMF_MODEL_NSURI + "=" + VERSIONED_NS_URI
					+ ")") ServiceAware<EPackage> versionedAware)
			throws Exception {
		assertThat(versionedAware.isEmpty()).isTrue();
		Configuration configA = dynamicModel("versioned-a.ecore");
		Configuration configB = dynamicModel("versioned-b.ecore");
		try {
			versionedAware.waitForService(10_000L);
			waitForServiceCount(versionedAware, 2, 10_000L);

			Set<String> fingerprints = new HashSet<>();
			for (ServiceReference<EPackage> reference : versionedAware.getServiceReferences()) {
				Object fingerprint = reference.getProperty(EMFNamespaces.EMF_MODEL_FINGERPRINT);
				assertThat(fingerprint).isInstanceOf(String.class);
				assertThat((String) fingerprint).startsWith("fp1:");
				fingerprints.add((String) fingerprint);
			}
			assertThat(fingerprints).as("same nsURI, different content => different identities").hasSize(2);
		} finally {
			configA.delete();
			configB.delete();
		}
	}

	private Configuration dynamicModel(String ecoreFile) throws Exception {
		String ecoreBase = System.getProperty("ecoreBase");
		assertNotNull(ecoreBase);
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(EMFNamespaces.EMF_MODEL_DYNAMIC_URI, "file:///" + ecoreBase + "/data/" + ecoreFile);
		Configuration configuration = ca
				.createFactoryConfiguration(EMFNamespaces.DYNAMIC_MODEL_CONFIGURATOR_CONFIG_NAME, "?");
		configuration.update(properties);
		return configuration;
	}

	private static void waitForServiceCount(ServiceAware<EPackage> serviceAware, int expected, long timeoutMs)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (serviceAware.getServiceReferences().size() < expected) {
			if (System.currentTimeMillis() > deadline) {
				throw new AssertionError("expected " + expected + " services with nsURI " + VERSIONED_NS_URI + ", got "
						+ serviceAware.getServiceReferences().size());
			}
			Thread.sleep(100L);
		}
	}

	private static Set<String> flatten(Object propertyValue) {
		Set<String> values = new HashSet<>();
		if (propertyValue instanceof String[] array) {
			for (String value : array) {
				values.add(value);
			}
		} else if (propertyValue instanceof String value) {
			values.add(value);
		}
		return values;
	}
}
