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

import java.util.ArrayList;
import java.util.List;

/**
 * Test listener recording every event in delivery order as {@code "<kind>:<key>"}.
 */
public class RecordingListener implements EObjectRegistryListener {

	public final List<String> events = new ArrayList<>();
	public final List<EObjectRegistryEntry> added = new ArrayList<>();
	public final List<EObjectRegistryEntry> updated = new ArrayList<>();
	public final List<EObjectRegistryEntry> updatedOld = new ArrayList<>();
	public final List<EObjectRegistryEntry> removed = new ArrayList<>();

	@Override
	public void entryAdded(EObjectRegistryEntry entry) {
		events.add("added:" + entry.key());
		added.add(entry);
	}

	@Override
	public void entryUpdated(EObjectRegistryEntry entry, EObjectRegistryEntry oldEntry) {
		events.add("updated:" + entry.key());
		updated.add(entry);
		updatedOld.add(oldEntry);
	}

	@Override
	public void entryRemoved(EObjectRegistryEntry entry) {
		events.add("removed:" + entry.key());
		removed.add(entry);
	}
}
