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
package org.eclipse.fennec.emf.osgi.eobject.registry.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryEntry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryListener;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;

/**
 * Plain-Java registry core: a string-keyed, insertion-ordered store with listener
 * notification and the per-source swap semantics on its write face.
 * <p>
 * All mutations, listener (de)registration and snapshot reads synchronize on one
 * internal lock; events are delivered while holding it, so a listener added mid-write
 * sees replay-then-events with no gap and no duplicate. The reference for the swap
 * semantics is the sensinact {@code AtlasMappingSourceComponent}, whose sync/swap
 * machinery moved here.
 *
 * @author Data In Motion Consulting
 */
public class EObjectRegistryImpl implements EObjectRegistry, EObjectRegistryWriter {

	private static final Logger logger = Logger.getLogger(EObjectRegistryImpl.class.getName());

	private final String name;
	private final Map<String, EObjectRegistryEntry> entriesByKey = new LinkedHashMap<>();
	private final List<EObjectRegistryListener> listeners = new ArrayList<>();
	private final Object lock = new Object();

	/**
	 * Creates an empty registry.
	 *
	 * @param name the registry name; must not be {@code null}
	 */
	public EObjectRegistryImpl(String name) {
		this.name = Objects.requireNonNull(name, "name");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public EObjectRegistry getRegistry() {
		return this;
	}

	@Override
	public Optional<EObject> get(String key) {
		return getEntry(key).map(EObjectRegistryEntry::object);
	}

	@Override
	public Optional<EObjectRegistryEntry> getEntry(String key) {
		if (key == null) {
			return Optional.empty();
		}
		synchronized (lock) {
			return Optional.ofNullable(entriesByKey.get(key));
		}
	}

	@Override
	public Collection<EObjectRegistryEntry> entries() {
		synchronized (lock) {
			return List.copyOf(entriesByKey.values());
		}
	}

	@Override
	public void addListener(EObjectRegistryListener listener) {
		Objects.requireNonNull(listener, "listener");
		synchronized (lock) {
			if (listeners.contains(listener)) {
				return;
			}
			listeners.add(listener);
			for (EObjectRegistryEntry entry : entriesByKey.values()) {
				deliver(listener, l -> l.entryAdded(entry));
			}
		}
	}

	@Override
	public void removeListener(EObjectRegistryListener listener) {
		Objects.requireNonNull(listener, "listener");
		synchronized (lock) {
			listeners.remove(listener);
		}
	}

	@Override
	public void put(String source, String key, EObject object, Map<String, Object> properties) {
		EObjectRegistryEntry entry = new EObjectRegistryEntry(key, object, source, properties);
		synchronized (lock) {
			putEntry(entry);
		}
	}

	@Override
	public void remove(String source, String key) {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(key, "key");
		synchronized (lock) {
			EObjectRegistryEntry current = entriesByKey.get(key);
			if (current == null) {
				return;
			}
			if (!current.source().equals(source)) {
				logger.warning(() -> String.format(
						"Registry %s: source %s tried to remove key %s owned by source %s - ignored", name, source,
						key, current.source()));
				return;
			}
			entriesByKey.remove(key);
			notifyListeners(l -> l.entryRemoved(current));
		}
	}

	@Override
	public void sync(String source, Collection<EObjectRegistryEntry> entries) {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(entries, "entries");
		for (EObjectRegistryEntry entry : entries) {
			if (!source.equals(entry.source())) {
				throw new IllegalArgumentException(String.format(
						"sync(%s): entry %s carries source %s - a sync pass only takes entries of the syncing source",
						source, entry.key(), entry.source()));
			}
		}
		synchronized (lock) {
			// update before remove: whiteboard-style consumers see the new state of a key
			// before anything of this pass disappears
			Set<String> synced = new HashSet<>();
			for (EObjectRegistryEntry entry : entries) {
				putEntry(entry);
				synced.add(entry.key());
			}
			Iterator<EObjectRegistryEntry> it = entriesByKey.values().iterator();
			List<EObjectRegistryEntry> removed = new ArrayList<>();
			while (it.hasNext()) {
				EObjectRegistryEntry existing = it.next();
				if (existing.source().equals(source) && !synced.contains(existing.key())) {
					it.remove();
					removed.add(existing);
				}
			}
			for (EObjectRegistryEntry gone : removed) {
				notifyListeners(l -> l.entryRemoved(gone));
			}
		}
	}

	/**
	 * Insert or replace under the lock. Identity compare short-circuit: the identical
	 * object instance with equal properties and the same source is a no-op - a remote
	 * client's ETag cache returns the identical instance while the content is unchanged,
	 * so this is what makes periodic re-syncs cheap and event-silent.
	 */
	private void putEntry(EObjectRegistryEntry entry) {
		EObjectRegistryEntry old = entriesByKey.get(entry.key());
		if (old != null && old.object() == entry.object() && old.source().equals(entry.source())
				&& old.properties().equals(entry.properties())) {
			return;
		}
		if (old != null && !old.source().equals(entry.source())) {
			logger.warning(() -> String.format("Registry %s: key %s changes source %s -> %s (last write wins)", name,
					entry.key(), old.source(), entry.source()));
		}
		entriesByKey.put(entry.key(), entry);
		if (old == null) {
			notifyListeners(l -> l.entryAdded(entry));
		} else {
			notifyListeners(l -> l.entryUpdated(entry, old));
		}
	}

	private void notifyListeners(Consumer<EObjectRegistryListener> event) {
		// snapshot: a listener may remove itself from within its callback
		for (EObjectRegistryListener listener : List.copyOf(listeners)) {
			deliver(listener, event);
		}
	}

	/** A broken listener never breaks the writer or the other listeners. */
	private void deliver(EObjectRegistryListener listener, Consumer<EObjectRegistryListener> event) {
		try {
			event.accept(listener);
		} catch (RuntimeException e) {
			logger.log(Level.WARNING,
					String.format("Registry %s: listener %s failed - continuing", name, listener), e);
		}
	}
}
