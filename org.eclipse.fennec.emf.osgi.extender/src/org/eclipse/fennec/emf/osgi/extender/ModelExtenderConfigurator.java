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
package org.eclipse.fennec.emf.osgi.extender;

import static java.util.Objects.requireNonNull;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;

/**
 * {@link EPackageConfigurator} implementation used by the EMF model extender.
 * <p>
 * Wraps a single {@link EPackage} and registers/unregisters it in an
 * {@link EPackage.Registry} by its namespace URI. This allows the extender
 * to integrate discovered ecore models into the EMF package registry
 * through the OSGi whiteboard pattern.
 *
 * @author Mark Hoffmann
 * @since 13.10.2022
 * @see EMFModelExtender
 */
public class ModelExtenderConfigurator implements EPackageConfigurator {

	private final EPackage ePackage;
	private final String nsURI;

	/**
	 * Creates a new configurator for the given package.
	 *
	 * @param ePackage the EMF package to register, must not be {@code null} and must have a non-null nsURI
	 * @throws NullPointerException if {@code ePackage} or its nsURI is {@code null}
	 */
	public ModelExtenderConfigurator(EPackage ePackage) {
		this.ePackage = requireNonNull(ePackage, "EPackage must not be null");
		this.nsURI = requireNonNull(ePackage.getNsURI(), "EPackage nsURI must not be null");
	}

	@Override
	public void configureEPackage(EPackage.Registry registry) {
		registry.put(nsURI, ePackage);
	}

	@Override
	public void unconfigureEPackage(EPackage.Registry registry) {
		registry.remove(nsURI);
	}

}
