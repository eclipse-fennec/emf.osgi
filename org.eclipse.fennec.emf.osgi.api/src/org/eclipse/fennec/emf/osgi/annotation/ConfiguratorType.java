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
package org.eclipse.fennec.emf.osgi.annotation;

/**
 * Type of Fennec EMF OSGi configurators.
 * <p>
 * Each value corresponds to a specific EMF registry that can be populated
 * via the OSGi whiteboard pattern using {@code @EMFConfigurator}.
 *
 * @author Mark Hoffmann
 * @since 15.12.2023
 */
public enum ConfiguratorType {

	EPACKAGE,
	RESOURCE_SET,
	RESOURCE_FACTORY,
	OPERATION_INVOCATION_FACTORY,
	VALIDATION_DELEGATE,
	SETTING_DELEGATE_FACTORY,
	CONVERSION_DELEGATE_FACTORY

}
