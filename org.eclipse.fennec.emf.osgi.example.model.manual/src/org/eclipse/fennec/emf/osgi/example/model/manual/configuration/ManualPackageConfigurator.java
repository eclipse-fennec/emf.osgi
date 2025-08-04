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

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.example.model.manual.ManualPackage;
import org.eclipse.fennec.emf.osgi.example.model.manual.util.ManualResourceFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ManualPackageConfigurator implements EPackageConfigurator {

	public static ServiceRegistration<?>  registerManualPackage(BundleContext bc, Dictionary<String, Object> properties) {
		Dictionary<String, Object> propertiesToUse = properties == null ? new Hashtable<String, Object>() : properties;
		propertiesToUse.put(EMF_MODEL_NAME, "manual");
		ServiceRegistration<?> packageRegistration = bc.registerService(
				new String[] { EPackageConfigurator.class.getName()},
				new ManualPackageConfigurator(), propertiesToUse);
		Dictionary<String, String> props = new Hashtable<>();
		props.put(EMFNamespaces.EMF_MODEL_FILE_EXT, "manual");
		props.put(EMFNamespaces.EMF_MODEL_CONTENT_TYPE, "manual#1.0");
		bc.registerService(
				new String[] { Resource.Factory.class.getName()},
				new ManualResourceFactoryImpl(), props);
		return packageRegistration;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.emf.osgi.osgi.EPackageRegistryConfigurator#configureEPackage(org.eclipse.emf.ecore.EPackage.Registry)
	 */
	@Override
	public void configureEPackage(org.eclipse.emf.ecore.EPackage.Registry registry) {
		registry.put(ManualPackage.eNS_URI, ManualPackage.eINSTANCE);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.emf.osgi.osgi.EPackageRegistryConfigurator#unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry)
	 */
	@Override
	public void unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry registry) {
		registry.remove(ManualPackage.eNS_URI);
	}

}
