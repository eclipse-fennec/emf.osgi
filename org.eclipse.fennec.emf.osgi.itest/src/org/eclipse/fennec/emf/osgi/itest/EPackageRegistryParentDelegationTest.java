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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * Tests that the {@link EPackage.Registry} services delegate failed lookups along their registry
 * hierarchy: a resource set scoped registry asks the static registry, an isolated registry asks the
 * default resource set registry. See issue #105.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@RequireConfigurationAdmin
public class EPackageRegistryParentDelegationTest {

	private static final String STATIC_NS_URI = "http://example.org/fennec/itest/staticscope";
	private static final String PARENT_NS_URI = "http://example.org/fennec/itest/parentscope";
	private static final String FACTORY_NAME = "parentdelegation";

	@InjectBundleContext
	BundleContext bc;

	@InjectService
	ConfigurationAdmin ca;

	/**
	 * A package that only the static registry tracks must be resolvable through the default resource
	 * set registry, which has the static registry as its parent. Note that this holds even without
	 * the parent delegation, because in a framework EMF makes {@link EPackage.Registry#INSTANCE}
	 * itself delegate to the static registry service - this test guards the resulting invariant, the
	 * delegation as such is covered by {@link #testIsolatedRegistryResolvesFromItsParent}.
	 */
	@Test
	public void testResourceSetRegistryResolvesFromStaticRegistry(
			@InjectService(filter = "(default.resourceset.epackage.registry=true)") EPackage.Registry resourceSetRegistry,
			@InjectService(filter = "(emf.default.epackage.registry=true)") EPackage.Registry staticRegistry) {

		EPackage ePackage = createPackage("staticscope", STATIC_NS_URI);
		assertNull(resourceSetRegistry.getEPackage(STATIC_NS_URI));

		ServiceRegistration<EPackageConfigurator> registration = registerConfigurator(ePackage,
				EMFNamespaces.EMF_MODEL_SCOPE_STATIC, null);
		try {
			assertSame(ePackage, staticRegistry.getEPackage(STATIC_NS_URI));
			assertSame(ePackage, resourceSetRegistry.getEPackage(STATIC_NS_URI));
		} finally {
			registration.unregister();
		}

		assertNull(resourceSetRegistry.getEPackage(STATIC_NS_URI));
	}

	/**
	 * An isolated registry that does not track a model itself must still resolve it through its
	 * parent, the default resource set registry.
	 */
	@Test
	public void testIsolatedRegistryResolvesFromItsParent(
			@InjectService(filter = "(default.resourceset.epackage.registry=true)") EPackage.Registry resourceSetRegistry,
			@InjectService(filter = "(rsf.name=" + FACTORY_NAME + ")", cardinality = 0) ServiceAware<EPackage.Registry> isolatedAware)
			throws IOException, InterruptedException {

		EPackage ePackage = createPackage("parentscope", PARENT_NS_URI);

		Dictionary<String, Object> factoryProperties = new Hashtable<>();
		factoryProperties.put(EMFNamespaces.PROP_RESOURCE_SET_FACTORY_NAME, FACTORY_NAME);
		// the isolated registry tracks no model of its own, everything has to come from the parent
		factoryProperties.put(EMFNamespaces.PROP_MODEL_TARGET_FILTER,
				"(" + EMFNamespaces.EMF_NAME + "=nothingtotrack)");

		Configuration configuration = ca.getConfiguration(EMFNamespaces.ISOLATED_RESOURCE_SET_FACTORY_CONFIG_NAME,
				"?");
		try {
			configuration.update(factoryProperties);
			EPackage.Registry isolatedRegistry = isolatedAware.waitForService(2000);
			assertNotNull(isolatedRegistry, "The isolated EPackage registry must become available");
			assertNull(isolatedRegistry.getEPackage(PARENT_NS_URI));

			ServiceRegistration<EPackageConfigurator> registration = registerConfigurator(ePackage,
					EMFNamespaces.EMF_MODEL_SCOPE_RESOURCE_SET, "parentscope");
			try {
				assertSame(ePackage, resourceSetRegistry.getEPackage(PARENT_NS_URI));
				assertSame(ePackage, isolatedRegistry.getEPackage(PARENT_NS_URI));
			} finally {
				registration.unregister();
			}

			assertNull(isolatedRegistry.getEPackage(PARENT_NS_URI));
		} finally {
			configuration.delete();
		}
	}

	private ServiceRegistration<EPackageConfigurator> registerConfigurator(EPackage ePackage, String scope,
			String modelName) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(EMFNamespaces.EMF_MODEL_SCOPE, scope);
		properties.put(EMFNamespaces.EMF_MODEL_NSURI, ePackage.getNsURI());
		if (modelName != null) {
			properties.put(EMFNamespaces.EMF_NAME, modelName);
		}
		return bc.registerService(EPackageConfigurator.class, new EPackageConfigurator() {

			@Override
			public void configureEPackage(EPackage.Registry registry) {
				registry.put(ePackage.getNsURI(), ePackage);
			}

			@Override
			public void unconfigureEPackage(EPackage.Registry registry) {
				registry.remove(ePackage.getNsURI());
			}
		}, properties);
	}

	private static EPackage createPackage(String name, String nsURI) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName(name);
		ePackage.setNsPrefix(name);
		ePackage.setNsURI(nsURI);
		return ePackage;
	}
}
