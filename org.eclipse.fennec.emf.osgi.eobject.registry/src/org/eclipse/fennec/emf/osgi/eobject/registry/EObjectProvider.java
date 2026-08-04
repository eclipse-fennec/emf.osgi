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

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The SPI of a <em>pulled initial provider</em> - the one content source a registry
 * loads before it goes public. In OSGi the registry configuration selects it through a
 * reference target on its {@link EObjectRegistryConstants#EMF_EOBJECT_PROVIDER_NAME}
 * service property; the registry component runs {@link #load(EObjectRegistryWriter)} on
 * a private executor and publishes the registry services only when the returned future
 * completes successfully.
 * <p>
 * Dynamic sources do <b>not</b> implement this interface - they are ordinary clients of
 * the {@link EObjectRegistryWriter} service and push. A source whose availability
 * depends on the network must be a dynamic source, never an initial provider: registry
 * publication must not depend on the network.
 *
 * @author Data In Motion Consulting
 */
@ConsumerType
public interface EObjectProvider {

	/**
	 * Loads the provider's content through the given writer. The provider writes
	 * ({@link EObjectRegistryWriter#put} or {@link EObjectRegistryWriter#sync}) and
	 * completes the future when its state is fully written; completing exceptionally
	 * signals a failed load (the registry then stays unpublished, logged).
	 *
	 * @param writer the registry's write face; never {@code null}
	 * @return a future that completes when the initial content is fully written; never
	 *         {@code null}
	 */
	CompletableFuture<Void> load(EObjectRegistryWriter writer);
}
