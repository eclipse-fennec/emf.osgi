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
package org.gecko.emf.osgi;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This OSGi service configures a resource set during construction. It is
 * expected that a system may define more than one configurator and that the
 * ResourceSet factory will apply all configurators.
 * 
 * <p>
 * Legacy location used by GeckoEMF 3.x - 5.x (package {@code org.gecko.emf.osgi} 3.x).
 * GeckoEMF 6 moved this interface to {@code org.gecko.emf.osgi.configurator}. It is kept
 * here so that bundles built against older GeckoEMF versions resolve and their services are
 * picked up by the compatibility bridges.
 * </p>
 * @author bhunt, Mark Hoffmann
 */
@ProviderType
@Deprecated
public interface ResourceSetConfigurator {
	
	public static final String EMF_CONFIGURATOR_NAME = "resourceSet";
	
	/**
	 * Configure a newly constructed {@link ResourceSet}. 
	 * @param resourceSet the {@link ResourceSet} to configure
	 */
	void configureResourceSet(ResourceSet resourceSet);
}
