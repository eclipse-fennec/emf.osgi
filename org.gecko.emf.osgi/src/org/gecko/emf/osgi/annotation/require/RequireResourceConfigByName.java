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
 *     Data In Motion - initial API and implementation
 */
package org.gecko.emf.osgi.annotation.require;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.gecko.emf.osgi.EMFNamespaces;
import org.gecko.emf.osgi.ResourceSetConfigurator;
import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Requirement;

/**
 * Marks the requirement of a specific {@link ResourceSetConfigurator}
 * @author Juergen Albert
 * @since 9 Feb 2018
 * @deprecated Clarifiy Talk with Jürgen
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.TYPE, ElementType.PACKAGE
})
@Requirement(namespace = EMFNamespaces.EMF_CONFIGURATOR_NAMESPACE, //
		name = ResourceSetConfigurator.EMF_CONFIGURATOR_NAME,
		filter = "(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=${#name})" 
		)
public @interface RequireResourceConfigByName {
	
	/**
	 * the Name of the {@link ResourceSetConfigurator}
	 */
	@Attribute()
	String name(); 
}
