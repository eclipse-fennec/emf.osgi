/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.emf.osgi.extender;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;

/**
 * Implementation for Gecko EMF configurators, used by the extender.
 * @author Mark Hoffmann
 * @since 13.10.2022
 */
public class ModelExtenderConfigurator implements EPackageConfigurator {
	
	private static Logger logger = Logger.getLogger(ModelExtenderConfigurator.class.getName());
	private final EPackage ePackage;

	/**
	 * Creates a new instance.
	 */
	public ModelExtenderConfigurator(EPackage ePackage) {
		this.ePackage = ePackage;
	}


	/* 
	 * (non-Javadoc)
	 * @see org.gecko.emf.osgi.EPackageConfigurator#configureEPackage(org.eclipse.emf.ecore.EPackage.Registry)
	 */
	@Override
	public void configureEPackage(org.eclipse.emf.ecore.EPackage.Registry registry) {
		if (ePackage != null) {
			registry.put(ePackage.getNsURI(), ePackage);
		} else {
			logger.log(Level.SEVERE, ()->"Error registering a NULL package, that should never happen");
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.emf.osgi.EPackageConfigurator#unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry)
	 */
	@Override
	public void unconfigureEPackage(org.eclipse.emf.ecore.EPackage.Registry registry) {
		if (ePackage != null) {
			registry.remove(ePackage.getNsURI());
		} else {
			logger.log(Level.SEVERE, ()->"Error un-registering a NULL package, that should never happen");
		}
	}

}
