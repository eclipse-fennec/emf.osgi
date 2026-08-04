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
package org.eclipse.fennec.emf.osgi.metadata.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.fennec.emf.osgi.metadata.MetadataService;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * Regression test for issue #71: an {@link EPackage} service that is already registered
 * when {@code MetadataServiceComponent} activates must end up in the metadata registry.
 * <p>
 * Before the fix, SCR bound the dynamic {@code EPackage} reference before the
 * {@code FingerprintService} bind method (descriptor order), so every pre-existing
 * package threw and was permanently lost. The fingerprint service is now injected via
 * the component constructor, which always precedes any bind method.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class MetadataServiceStartOrderIntegrationTest {

	private static final String METADATA_BSN = "org.eclipse.fennec.emf.osgi.metadata";
	private static final String NS_URI = "http://fennec.eclipse.org/itest/startorder/1.0";

	@Test
	public void testEPackageRegisteredBeforeActivationIsRetained(@InjectBundleContext BundleContext ctx,
			@InjectService(cardinality = 0) ServiceAware<MetadataService> metadataAware) throws Exception {
		Bundle metadataBundle = Arrays.stream(ctx.getBundles())
				.filter(b -> METADATA_BSN.equals(b.getSymbolicName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("metadata bundle not installed"));

		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("startorder");
		ePackage.setNsPrefix("startorder");
		ePackage.setNsURI(NS_URI);

		metadataBundle.stop();
		ServiceRegistration<EPackage> registration = ctx.registerService(EPackage.class, ePackage, null);
		try {
			metadataBundle.start();

			MetadataService metadataService = metadataAware.waitForService(5000);
			assertThat(metadataService).isNotNull();
			assertThat(metadataService.getPackageMetadata(NS_URI))
					.as("EPackage registered before component activation must be in the metadata registry")
					.isPresent()
					.map(PackageMetadata::getNsURI)
					.hasValue(NS_URI);
		} finally {
			registration.unregister();
			metadataBundle.start();
		}
	}
}
