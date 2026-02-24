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
package org.eclipse.fennec.emf.osgi.extender.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * OSGi integration tests for the EMF model extender bundle lifecycle behavior.
 * <p>
 * Verifies that stopping and restarting a model bundle correctly unregisters
 * and re-registers its EMF model services ({@link EPackage}, {@link EPackageConfigurator},
 * {@link ResourceSet}).
 *
 * @author Mark Hoffmann
 * @since 14.10.2022
 */
@ExtendWith(ServiceExtension.class)
@ExtendWith(BundleContextExtension.class)
public class EMFModelExtenderRestartTest {

	private static final String EXTENDER_TEST_MODEL_BSN = "org.eclipse.fennec.emf.osgi.example.model.extender";
	private static final String MANUAL_MODEL_NSURI = "http://fennec.eclipse.org/example/model/manual/1.0";
	private static final String MANUAL_FILTER = "(" + EMFNamespaces.EMF_NAME + "=manual)";
	private static final long BUNDLE_SETTLE_MS = 1000L;

	private BundleContext ctx;

	@BeforeEach
	public void before(@InjectBundleContext BundleContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * Ensures the test model bundle is active after each test so other tests
	 * are not affected by stop/restart operations.
	 */
	@AfterEach
	public void after() throws BundleException, InterruptedException {
		for (Bundle b : ctx.getBundles()) {
			if (EXTENDER_TEST_MODEL_BSN.equals(b.getSymbolicName()) && b.getState() != Bundle.ACTIVE) {
				b.start();
				Thread.sleep(BUNDLE_SETTLE_MS);
			}
		}
	}

	/**
	 * Verifies that stopping the model bundle causes all its EMF services
	 * to be unregistered from the service registry.
	 */
	@Test
	public void stopBundleTest(
			@InjectService(filter = MANUAL_FILTER) ServiceAware<ResourceSet> rsAware,
			@InjectService(filter = MANUAL_FILTER) ServiceAware<EPackage> ePackageAware)
			throws BundleException, InterruptedException, InvalidSyntaxException {

		assertModelServicesAvailable(rsAware, ePackageAware);

		Bundle origin = findModelBundle();
		origin.stop();
		Thread.sleep(BUNDLE_SETTLE_MS);

		assertTrue(rsAware.isEmpty(), "ResourceSet service should be unregistered after bundle stop");
		assertTrue(ePackageAware.isEmpty(), "EPackage service should be unregistered after bundle stop");
	}

	/**
	 * Verifies that restarting the model bundle re-registers all EMF services
	 * so they become available again in the service registry.
	 */
	@Test
	public void restartBundleTest(
			@InjectService(filter = MANUAL_FILTER) ServiceAware<ResourceSet> rsAware,
			@InjectService(filter = MANUAL_FILTER) ServiceAware<EPackage> ePackageAware)
			throws BundleException, InterruptedException, InvalidSyntaxException {

		assertModelServicesAvailable(rsAware, ePackageAware);

		Bundle origin = findModelBundle();

		// Stop the bundle and verify services disappear
		origin.stop();
		Thread.sleep(BUNDLE_SETTLE_MS);

		assertTrue(rsAware.isEmpty(), "ResourceSet service should be unregistered after bundle stop");
		assertTrue(ePackageAware.isEmpty(), "EPackage service should be unregistered after bundle stop");

		// Restart the bundle and verify services reappear
		origin.start();
		Thread.sleep(BUNDLE_SETTLE_MS);

		assertFalse(rsAware.isEmpty(), "ResourceSet service should be available after bundle restart");
		assertFalse(ePackageAware.isEmpty(), "EPackage service should be available after bundle restart");

		// Verify the configurator is re-registered from the correct bundle
		Collection<ServiceReference<EPackageConfigurator>> configurators =
				ctx.getServiceReferences(EPackageConfigurator.class, MANUAL_FILTER);
		assertEquals(1, configurators.size(), "Exactly one configurator should be registered after restart");

		ServiceReference<EPackageConfigurator> ref = configurators.iterator().next();
		assertNotNull(ref.getBundle());
		assertEquals(EXTENDER_TEST_MODEL_BSN, ref.getBundle().getSymbolicName());
	}

	/**
	 * Asserts that the model services are initially available and the model
	 * content is correct (Foo classifier exists, Bar does not).
	 */
	private void assertModelServicesAvailable(ServiceAware<ResourceSet> rsAware, ServiceAware<EPackage> ePackageAware) {
		ResourceSet rs = rsAware.getService();
		assertNotNull(rs);

		EPackage ePackageService = ePackageAware.getService();
		assertNotNull(ePackageService);

		EFactory eFactory = rs.getPackageRegistry().getEFactory(MANUAL_MODEL_NSURI);
		assertNotNull(eFactory);

		EPackage ePackage = eFactory.getEPackage();
		assertNotNull(ePackage);
		assertEquals(ePackage, ePackageService);

		EClass foo = (EClass) ePackage.getEClassifier("Foo");
		assertNotNull(foo, "Foo classifier should exist in manual model");

		EClass bar = (EClass) ePackage.getEClassifier("Bar");
		assertNull(bar, "Bar classifier should not exist in manual model");
	}

	/**
	 * Finds the model bundle by its symbolic name via the configurator service reference.
	 *
	 * @return the model bundle
	 * @throws InvalidSyntaxException if the service filter is invalid
	 */
	private Bundle findModelBundle() throws InvalidSyntaxException {
		Collection<ServiceReference<EPackageConfigurator>> configurators =
				ctx.getServiceReferences(EPackageConfigurator.class, MANUAL_FILTER);
		assertFalse(configurators.isEmpty(), "Manual configurator should be registered");
		assertEquals(1, configurators.size(), "Exactly one manual configurator expected");

		ServiceReference<EPackageConfigurator> ref = configurators.iterator().next();
		Bundle origin = ref.getBundle();
		assertNotNull(origin);
		assertEquals(EXTENDER_TEST_MODEL_BSN, origin.getSymbolicName());
		return origin;
	}
}
