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
package org.eclipse.fennec.emf.osgi.itest.configurator;

import org.eclipse.fennec.emf.osgi.annotation.provide.EMFConfigurator;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.osgi.service.component.annotations.Component;

/**
 * 
 * @author mark
 * @since 17.12.2023
 */
@EMFConfigurator(
		configuratorName = "testPackage", 
		contentType = {"testPackage", "testPackage2"}, 
		feature = {"testPackageFeature", "testPF"},
		protocol = {"tp1", "tp2"},
		fileExtension = {"fetp1", "fetp2"})
@Component(name = "TestEPackageConfigurator", enabled = false, property =  EMFNamespaces.EMF_MODEL_SCOPE + "=" + EMFNamespaces.EMF_MODEL_SCOPE_RESOURCE_SET)
public class TestEPackageConfigurator implements EPackageConfigurator {

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator#configureEPackage(org.eclipse.emf.ecore.EPackage.Registry)
	 */
	@Override
	public void configureEPackage(org.eclipse.emf.ecore.EPackage.Registry arg0) {
		// TODO Auto-generated method stub
		
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator#unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry)
	 */
	@Override
	public void unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry arg0) {
		// TODO Auto-generated method stub
		
	}

}
