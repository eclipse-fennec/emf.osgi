/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 *  
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *  
 * Contributors:
 *       Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.emf.osgi.example.model.manual.configuration;

import static org.eclipse.fennec.emf.osgi.constants.EMFNamespaces.EMF_MODEL_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.example.model.manual.util.ManualResourceFactoryImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class ManualPackageConfigurator implements EPackageConfigurator {

	public static EPackage eINSTANCE = null;
	
	public static final String eNS_URI = "http://fennec.eclipse.org/example/model/manual/1.0";
	public static final String eNAME = "manual";

	public static ServiceRegistration<?>  registerManualPackage(BundleContext bc, Dictionary<String, Object> properties) throws IOException {
		Map<String, Object> propertiesToUse =  new HashMap<String, Object>();
		propertiesToUse.put(EMF_MODEL_NAME, ManualPackageConfigurator.eNAME);
		propertiesToUse.put(EMFNamespaces.EMF_MODEL_SCOPE, EMFNamespaces.EMF_MODEL_SCOPE_RESOURCE_SET);
		if(properties != null) {
			propertiesToUse.putAll(FrameworkUtil.asMap(properties));
		}
		ServiceRegistration<?> packageRegistration = bc.registerService(
				new String[] { EPackageConfigurator.class.getName()},
				new ManualPackageConfigurator(), FrameworkUtil.asDictionary(propertiesToUse));
		Dictionary<String, String> props = new Hashtable<>();
		props.put(EMFNamespaces.EMF_MODEL_FILE_EXT, "manual");
		props.put(EMFNamespaces.EMF_MODEL_CONTENT_TYPE, "manual#1.0");
		bc.registerService(
				new String[] { Resource.Factory.class.getName()},
				new ManualResourceFactoryImpl(), props);
		return packageRegistration;
	}
	
	/**
	 * @param string
	 * @param manualPackage
	 * @return
	 */
	public static EObject createFoo(String name, EPackage manualPackage) {
		EClass eClass = (EClass) manualPackage.getEClassifier("Foo");
		EObject eObject = EcoreUtil.create(eClass);
		eObject.eSet(eClass.getEStructuralFeature("value"), name);
		return eObject;
	}
	
	public static EObject createFoo(String name) {
		return createFoo(name, eINSTANCE);
	}

	public static InputStream readFooXMI() throws IOException {
		Bundle bundle = FrameworkUtil.getBundle(ManualPackageConfigurator.class);
		InputStream inputStream = bundle.getEntry("model/Foo.xmi").openStream();
		return inputStream;
	}
	
	public static String getValue(EObject eObject) {
		EClass eClass = (EClass) eObject.eClass();
		return (String) eObject.eGet(eClass.getEStructuralFeature("value"));
	}
	
	public ManualPackageConfigurator() throws IOException {
		EPackage ePackage = loadEPackage();
		eINSTANCE = ePackage; 
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public static EPackage loadEPackage() throws IOException {
		Resource resource = new XMIResourceImpl(URI.createURI("http://something"));
		Bundle bundle = FrameworkUtil.getBundle(ManualPackageConfigurator.class);
		InputStream inputStream = bundle.getEntry("model/manual.ecore").openStream();
		resource.load(inputStream, null);
		EPackage ePackage = (EPackage) resource.getContents().get(0);
		resource.setURI(URI.createURI(ePackage.getNsURI()));
		return ePackage;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.emf.osgi.osgi.EPackageRegistryConfigurator#configureEPackage(org.eclipse.emf.ecore.EPackage.Registry)
	 */
	@Override
	public void configureEPackage(org.eclipse.emf.ecore.EPackage.Registry registry) {
		registry.put(eINSTANCE.getNsURI(), eINSTANCE);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.emf.osgi.osgi.EPackageRegistryConfigurator#unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry)
	 */
	@Override
	public void unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry registry) {
		registry.remove(eINSTANCE.getNsURI());
	}
}
