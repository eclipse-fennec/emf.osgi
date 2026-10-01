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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.eclipse.fennec.emf.osgi.UriMapProvider;
import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
import org.eclipse.fennec.emf.osgi.example.model.basic.BasicPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * Tests that a reference to a generated, deployed model by its location resolves to the registered
 * generated package, see issue #114.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@RequireConfigurationAdmin
public class GeneratedPackageLocationIntegrationTest {

	private static final String CONFIGURATOR_FILTER = "(component.name=GeneratedPackageLocationConfigurator)";
	private static final String SCHEMA = """
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
			    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="probe" nsURI="http://example.org/probe" nsPrefix="probe">
			  <eClassifiers xsi:type="ecore:EClass" name="Holder">
			    <eStructuralFeatures xsi:type="ecore:EReference" name="person" containment="true"
			        eType="ecore:EClass %s#//Person"/>
			  </eClassifiers>
			</ecore:EPackage>
			""";

	@InjectService
	ConfigurationAdmin ca;

	private EReference loadReference(ResourceSet resourceSet, String typeLocation) throws IOException {
		Resource resource = new EcoreResourceFactoryImpl().createResource(URI.createURI("file:/probe/probe.ecore"));
		resourceSet.getResources().add(resource);
		try (InputStream in = new ByteArrayInputStream(SCHEMA.formatted(typeLocation).getBytes(StandardCharsets.UTF_8))) {
			resource.load(in, null);
		}
		EPackage ePackage = (EPackage) resource.getContents().get(0);
		return (EReference) ((EClass) ePackage.getEClassifier("Holder")).getEStructuralFeature("person");
	}

	@Test
	public void testPlatformPluginLocationResolvesToGeneratedPackage(
			@InjectService(filter = CONFIGURATOR_FILTER) ResourceSetConfigurator configurator,
			@InjectService ResourceSetFactory factory) throws IOException {
		ResourceSet resourceSet = factory.createResourceSet();

		EReference reference = loadReference(resourceSet,
				"platform:/plugin/org.eclipse.fennec.emf.osgi.example.model.basic/model/basic.ecore");

		assertFalse(reference.getEReferenceType().eIsProxy());
		assertSame(BasicPackage.Literals.PERSON, reference.getEReferenceType());
		assertEquals(1, resourceSet.getResources().size(), "No resource must be created for the location");
	}

	@Test
	public void testNsUriReferenceStillResolves(
			@InjectService(filter = CONFIGURATOR_FILTER) ResourceSetConfigurator configurator,
			@InjectService ResourceSetFactory factory) throws IOException {
		ResourceSet resourceSet = factory.createResourceSet();

		EReference reference = loadReference(resourceSet, BasicPackage.eNS_URI);

		assertSame(BasicPackage.Literals.PERSON, reference.getEReferenceType());
	}

	@Test
	public void testUriMapProviderMapsALocationToTheNsUri(
			@InjectService(filter = CONFIGURATOR_FILTER) ResourceSetConfigurator configurator,
			@InjectService ResourceSetFactory factory,
			@InjectService(cardinality = 0) ServiceAware<UriMapProvider> mapProviders) throws Exception {
		String location = "file:/models/basic.ecore";
		Configuration configuration = ca.createFactoryConfiguration("DefaultUriMapProvider", "?");
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(UriMapProvider.URI_MAP_SOURE, location);
		properties.put(UriMapProvider.URI_MAP_DESTINATION, BasicPackage.eNS_URI);
		configuration.update(properties);
		try {
			assertNotNull(mapProviders.waitForService(5000L));
			// the URI handler configurator binds the provider dynamically, wait until it applies the map
			ResourceSet resourceSet = factory.createResourceSet();
			for (int i = 0; i < 50 && !resourceSet.getURIConverter().getURIMap().containsKey(URI.createURI(location)); i++) {
				Thread.sleep(100);
				resourceSet = factory.createResourceSet();
			}
			assertEquals(URI.createURI(BasicPackage.eNS_URI), resourceSet.getURIConverter().getURIMap().get(URI.createURI(location)));

			EReference reference = loadReference(resourceSet, location);

			assertSame(BasicPackage.Literals.PERSON, reference.getEReferenceType());
			assertEquals(1, resourceSet.getResources().size(), "No resource must be created for the location");
		} finally {
			configuration.delete();
		}
	}
}
