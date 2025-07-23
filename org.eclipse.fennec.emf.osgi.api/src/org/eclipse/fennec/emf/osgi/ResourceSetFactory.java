/**
 * Copyright (c) 2012 - 2024 Data In Motion and others.
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
package org.eclipse.fennec.emf.osgi;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This OSGi service builds an EMF ResourceSet.
 * @author bhunt
 * @author Mark Hoffmann
 */
@ProviderType
public interface ResourceSetFactory
{
	public static final String EMF_CAPABILITY_NAME = "osgi";
	public static final String CONDITION_ID = "org.eclipse.fennec.emf.osgi.ResourceSetFactory";
	
	/**
	 * Returns a new instance of a resource set
	 * @return the newly created ResourceSet
	 */
	ResourceSet createResourceSet();

}
