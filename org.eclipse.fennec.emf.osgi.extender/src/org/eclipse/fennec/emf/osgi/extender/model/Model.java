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
package org.eclipse.fennec.emf.osgi.extender.model;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.osgi.framework.FrameworkUtil;

/**
 * Immutable data holder for a discovered EMF model.
 * <p>
 * Encapsulates an {@link EPackage} loaded from a bundle together with the OSGi
 * service properties to be used when registering the model as a service, and
 * the originating bundle ID for proper service lifecycle management.
 *
 * @author Mark Hoffmann
 * @since 17.10.2022
 */
public class Model {

	private final EPackage ePackage;
	private final Map<String, Object> properties;
	private final long bundleId;

	/**
	 * Creates a new model instance.
	 *
	 * @param ePackage   the loaded EMF package, must not be {@code null}
	 * @param properties the OSGi service properties for registration
	 * @param bundleId   the ID of the bundle that provides this model
	 */
	public Model(EPackage ePackage, Dictionary<String, Object> properties, long bundleId) {
		this.ePackage = ePackage;
		this.bundleId = bundleId;
		this.properties = new HashMap<>(FrameworkUtil.asMap(properties));
	}

	/**
	 * Returns the loaded EMF package.
	 *
	 * @return the EPackage, never {@code null}
	 */
	public EPackage getEPackage() {
		return ePackage;
	}

	/**
	 * Returns a copy of the service properties as a {@link Dictionary}.
	 * <p>
	 * A new {@link Hashtable} is created on each call so callers cannot
	 * mutate the internal state.
	 *
	 * @return service properties for OSGi registration
	 */
	public Dictionary<String, Object> getProperties() {
		return new Hashtable<>(properties);
	}

	/**
	 * Returns the ID of the bundle that provides this model.
	 *
	 * @return the originating bundle ID, or {@code -1} if not associated with a specific bundle
	 */
	public long getBundleId() {
		return bundleId;
	}

	@Override
	public String toString() {
		return "Model [namespace=" + ePackage.getNsURI()
				+ ", bundleId=" + bundleId
				+ ", properties=" + properties + "]";
	}
}
