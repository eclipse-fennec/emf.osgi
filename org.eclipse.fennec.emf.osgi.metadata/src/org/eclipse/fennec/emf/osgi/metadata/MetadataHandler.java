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
package org.eclipse.fennec.emf.osgi.metadata;

import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The single extension point of the metadata system: register one as an OSGi service and
 * it is called for every model version the whiteboard sees.
 * <p>
 * It serves both roles the donor design split across two interfaces. A <b>contributor</b>
 * attaches its own content as {@link AspectEntry} instances in
 * {@link #onPackageRegistered(PackageMetadata)}; an <b>observer</b> uses the same callback
 * to update state of its own. One interface is enough because with
 * {@code AspectEntry} a contributor no longer returns typed objects the service would
 * have to understand - it writes into the tree it is handed.
 * <p>
 * <b>Timing.</b> {@code onPackageRegistered} runs while the tree is being built, before it
 * is published to lookups, the index and other readers. That ordering is what makes
 * contribution safe: no consumer can observe a model version whose entries are still
 * missing. The price is that the callback must not call back into
 * {@link MetadataService} for the package being registered - it is not visible yet, and
 * the passed tree already holds everything.
 * <p>
 * Implementations should be quick and must not block: they run inside the registration of
 * an {@link org.eclipse.emf.ecore.EPackage} service.
 *
 * @author Data In Motion Consulting
 */
@ConsumerType
public interface MetadataHandler {

	/**
	 * Called while a model version's metadata is being built, before it becomes visible.
	 * Contributors add their {@link AspectEntry} instances here; observers read.
	 * <p>
	 * Called once per model version, not once per nsURI: a second, diverging version of a
	 * package already seen is a separate call with a separate tree.
	 *
	 * @param packageMetadata the tree being built, never {@code null}
	 */
	void onPackageRegistered(PackageMetadata packageMetadata);

	/**
	 * Called before a model version is dropped, i.e. when its last registration goes away.
	 * Drop any state kept for it.
	 *
	 * @param packageMetadata the tree about to be removed, never {@code null}
	 */
	default void onPackageUnregistered(PackageMetadata packageMetadata) {
		// nothing to release by default
	}

	/**
	 * Called when this handler is removed from the whiteboard. Drop all state.
	 */
	default void clear() {
		// nothing to release by default
	}
}
