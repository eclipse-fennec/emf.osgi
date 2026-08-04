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
package org.eclipse.fennec.emf.osgi.eobject.registry;

/**
 * Service property conventions of the EObject registry.
 * <p>
 * These constants live here and not in {@code EMFNamespaces}, so the registry API does
 * not force a dependency direction onto the core API bundle.
 *
 * @author Data In Motion Consulting
 */
public final class EObjectRegistryConstants {

	/**
	 * The stable, human-chosen name of a registry instance. Set on the
	 * {@link EObjectRegistry} and {@link EObjectRegistryWriter} services; listeners and
	 * writer clients target a registry through it.
	 */
	public static final String EMF_EOBJECT_REGISTRY_NAME = "emf.eobject.registry.name";

	/**
	 * The name of an {@link EObjectProvider} service. The registry configuration selects
	 * its initial provider through this property, e.g.
	 * {@code initialProvider.target=(emf.eobject.provider.name=my-files)}.
	 */
	public static final String EMF_EOBJECT_PROVIDER_NAME = "emf.eobject.provider.name";

	/**
	 * Optional declared content types of a registry instance (nsURIs or EClass URIs),
	 * set as a service property for discovery.
	 */
	public static final String EMF_EOBJECT_REGISTRY_CONTENT_TYPES = "emf.eobject.registry.content.types";

	private EObjectRegistryConstants() {
	}
}
