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

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Configurator for the {@link Registry} registry
 * <p>
 * Legacy location used by GeckoEMF 3.x - 5.x (package {@code org.gecko.emf.osgi} 3.x).
 * GeckoEMF 6 moved this interface to {@code org.gecko.emf.osgi.configurator}. It is kept
 * here so that bundles built against older GeckoEMF versions resolve and their services are
 * picked up by the compatibility bridges.
 * </p>
 * @author Mark Hoffmann
 * @since 25.07.2017
 */
@ProviderType
@Deprecated
public interface ResourceFactoryConfigurator {
	
	/**
	 * Configures the {@link Registry} registry
	 * @param registry the registry to be configured
	 */
	public void configureResourceFactory(Resource.Factory.Registry registry);
	
	/**
	 * Unconfigure {@link Registry} registry
	 * @param registry the registry to unconfigure
	 */
	public void unconfigureResourceFactory(Resource.Factory.Registry registry);

}
