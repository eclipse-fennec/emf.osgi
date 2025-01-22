/**
 * Copyright (c) 2012 - 2023 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.emf.ecore.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.fennec.emf.osgi.example.model.manual.ManualPackage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;

/**
 * See documentation here: 
 * 	https://github.com/osgi/osgi-test
 * 	https://github.com/osgi/osgi-test/wiki
 * Examples: https://github.com/osgi/osgi-test/tree/main/examples
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class GlobalEPackageServiceRegistryTest {
	
	
	@BeforeAll
	public static void before(@InjectBundleContext BundleContext bundleContext) throws BundleException {
		Bundle bundle = Arrays.stream(bundleContext.getBundles()).filter(b -> b.getSymbolicName().equals("org.eclipse.emf.ecore")).findFirst().get();
		bundle.start();
	}

	@AfterEach
	public void after() {
		EPackage.Registry.INSTANCE.remove(ManualPackage.eNS_URI);
	}
	
	/*
	 * Tests that the Registered Registry is picked up and that changes to the static Registry is picked up.
	 */
	@Test
	public void testBasicRegistration(@InjectBundleContext BundleContext context) {
		Registry staticRegistry = EPackage.Registry.INSTANCE;
		
		assertThat(staticRegistry).isNotNull();
		
		EPackage manualPackage = staticRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNull();
		
		staticRegistry.put(ManualPackage.eNS_URI, ManualPackage.eINSTANCE);
		
		manualPackage = staticRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNotNull();
		
		EPackageRegistryImpl registryService = new EPackageRegistryImpl();
	
		ServiceRegistration<Registry> registerService = context.registerService(Registry.class, registryService, FrameworkUtil.asDictionary(Collections.singletonMap("emf.default.epackage.registry", true)));
		
		manualPackage = registryService.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNotNull();
		
		staticRegistry.remove(ManualPackage.eNS_URI);
		manualPackage = registryService.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNull();
		
		staticRegistry.put(ManualPackage.eNS_URI, ManualPackage.eINSTANCE);
		registerService.unregister();
		
		manualPackage = staticRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNotNull();
	}
	
	/*
	 * The expectation is that a second Registry will be ignored 
	 */
	@Test
	public void testDoubleRegbistration(@InjectBundleContext BundleContext context) {
		Registry staticRegistry = EPackage.Registry.INSTANCE;
		
		assertThat(staticRegistry).isNotNull();
		
		EPackage manualPackage = staticRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNull();
		
		staticRegistry.put(ManualPackage.eNS_URI, ManualPackage.eINSTANCE);
		
		manualPackage = staticRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNotNull();
		
		EPackageRegistryImpl mainRegistry = new EPackageRegistryImpl();
		
		ServiceRegistration<Registry> registration1 = context.registerService(Registry.class, mainRegistry, FrameworkUtil.asDictionary(Collections.singletonMap("emf.default.epackage.registry", true)));
		
		manualPackage = mainRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNotNull();

		EPackageRegistryImpl toIgnoreRegistry = new EPackageRegistryImpl();
		
		ServiceRegistration<Registry> registration2 = context.registerService(Registry.class, mainRegistry, FrameworkUtil.asDictionary(Collections.singletonMap("emf.default.epackage.registry", true)));
		
		//we have to use something random in order to avoid interference between tests, because of the static nature of the registry
		String random = UUID.randomUUID().toString();
		
		//just to make sure nothing lands there
		staticRegistry.put(random, ManualPackage.eINSTANCE);
		
		manualPackage = toIgnoreRegistry.getEPackage(random);
		assertThat(manualPackage).isNull();
	}

	/*
	 * The expectation is that a second Registry will be ignored 
	 */
	@Test
	public void testDoubleRegistrationWithServiceRank(@InjectBundleContext BundleContext context) {
		Registry staticRegistry = EPackage.Registry.INSTANCE;
		
		assertThat(staticRegistry).isNotNull();
		
		EPackage manualPackage = staticRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNull();
		
		staticRegistry.put(ManualPackage.eNS_URI, ManualPackage.eINSTANCE);
		
		manualPackage = staticRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNotNull();
		
		EPackageRegistryImpl mainRegistry = new EPackageRegistryImpl();
		
		ServiceRegistration<Registry> registration1 = context.registerService(Registry.class, mainRegistry, FrameworkUtil.asDictionary(Collections.singletonMap("emf.default.epackage.registry", true)));
		
		manualPackage = mainRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNotNull();
		
		EPackageRegistryImpl secondRegistry = new EPackageRegistryImpl();
		
		Map<String, Object> props = new HashMap<>(); 
		props.put("emf.default.epackage.registry", true);
		props.put(Constants.SERVICE_RANKING, 100);
		ServiceRegistration<Registry> registration2 = context.registerService(Registry.class, secondRegistry, FrameworkUtil.asDictionary(props));
		
		
		manualPackage = secondRegistry.getEPackage(ManualPackage.eNS_URI);
		assertThat(manualPackage).isNotNull();
		
		//we have to use something random in order to avoid interference between tests, because of the static nature of the registry
		String random = UUID.randomUUID().toString();
		
		staticRegistry.put(random, ManualPackage.eINSTANCE);
		
		manualPackage = secondRegistry.getEPackage(random);
		assertThat(manualPackage).isNotNull();

		//Nothing must be added to the old registry
		manualPackage = mainRegistry.getEPackage(random);
		assertThat(manualPackage).isNull();

		//now we unregister the higher ranked registry and the test2 must be part of the old one, as it becomes active again
		registration2.unregister();
		
		manualPackage = mainRegistry.getEPackage(random);
		assertThat(manualPackage).isNotNull();
	}

}
