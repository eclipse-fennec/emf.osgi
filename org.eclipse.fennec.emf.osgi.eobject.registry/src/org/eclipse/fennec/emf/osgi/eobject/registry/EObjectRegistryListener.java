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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Change notification of an {@link EObjectRegistry}. A newly added listener is replayed
 * the current content first ({@link #entryAdded(EObjectRegistryEntry)} per existing
 * entry), then receives live events - no gap, no duplicates.
 * <p>
 * In OSGi, listeners can additionally be whiteboard services carrying the
 * {@link EObjectRegistryConstants#EMF_EOBJECT_REGISTRY_NAME} property of the registry
 * they want to observe; the registry component binds and replays them (analogous to
 * {@code MetadataWhiteboard.addMetadataHandler} late-arrival replay).
 * <p>
 * Callbacks are delivered on the writing thread while the registry holds its internal
 * lock: implementations must be fast, must not block, and must not call back into the
 * writer.
 *
 * @author Data In Motion Consulting
 */
@ConsumerType
public interface EObjectRegistryListener {

	/**
	 * A new entry appeared (also fired per existing entry when the listener is added).
	 *
	 * @param entry the entry; never {@code null}
	 */
	default void entryAdded(EObjectRegistryEntry entry) {
	}

	/**
	 * An entry was replaced.
	 *
	 * @param entry    the new entry; never {@code null}
	 * @param oldEntry the replaced entry; never {@code null}
	 */
	default void entryUpdated(EObjectRegistryEntry entry, EObjectRegistryEntry oldEntry) {
	}

	/**
	 * An entry was removed.
	 *
	 * @param entry the removed entry; never {@code null}
	 */
	default void entryRemoved(EObjectRegistryEntry entry) {
	}
}
