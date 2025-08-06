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
package org.eclipse.fennec.emf.osgi.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link Map} implementation that holds an internal {@link HashMap} to store its data together with a delegate.
 * All reading operations will delegate on the delegate instance if no results are found internally.
 * <p>
 * This implementation supports change listeners that are notified when modifications are made to the map.
 * Listeners are called for add, update, remove, and clear operations. Only changes to the internal map
 * trigger notifications - changes to the delegate map do not generate events.
 * <p>
 * The listener mechanism is thread-safe and robust - if one listener throws an exception, other listeners
 * will still be notified of the change.
 * 
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Juergen Albert
 * @since 25 Nov 2022
 * @see MapChangeListener
 */
public class DelegatingHashMap<K, V> implements Map<K, V> {

	private Map<K, V> delegate;
	private Map<K, V> main;
	private final List<MapChangeListener<K, V>> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Creates a new instance.
	 */
	public DelegatingHashMap() {
		this(new HashMap<>());
		
	}
	/**
	 * Creates a new instance.
	 */
	public DelegatingHashMap(Map<K,V> delegate) {
		this.main = new HashMap<>();
		this.delegate = delegate;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.util.HashMap#size()
	 */
	
	@Override
	public int size() {
		return main.size() + delegate.size();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.util.HashMap#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return main.isEmpty() && delegate.isEmpty();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.util.HashMap#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {
		return main.containsKey(key) || delegate.containsKey(key) ;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.util.HashMap#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		return main.containsValue(value) || delegate.containsValue(value);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.util.HashMap#entrySet()
	 */
	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> set = new HashSet<>(delegate.entrySet());
		set.addAll(main.entrySet());
		return set;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.util.HashMap#get(java.lang.Object)
	 */
	@Override
	public V get(Object key) {
		V result = main.get(key);
		if(result == null) {
			return delegate.get(key);
		}
		return result;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.util.HashMap#values()
	 */
	@Override
	public Collection<V> values() {
		List<V> values = new ArrayList<>(delegate.values());
		values.addAll(main.values());
		return values;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.util.HashMap#keySet()
	 */
	@Override
	public Set<K> keySet() {
		Set<K> keys = new HashSet<>(delegate.keySet());
		keys.addAll(main.keySet());
		return keys;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V put(K key, V value) {
		V oldValue = main.put(key, value);
		if (oldValue == null) {
			notifyEntryAdded(key, value);
		} else {
			notifyEntryUpdated(key, oldValue, value);
		}
		return oldValue;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public V remove(Object key) {
		V removedValue = main.remove(key);
		if (removedValue != null) {
			@SuppressWarnings("unchecked")
			K typedKey = (K) key;
			notifyEntryRemoved(typedKey, removedValue);
		}
		return removedValue;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		if (!main.isEmpty()) {
			main.clear();
			notifyMapCleared();
		}
	}

	/**
	 * Adds a listener to be notified of map changes.
	 * 
	 * @param listener the listener to add
	 */
	public void addMapChangeListener(MapChangeListener<K, V> listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes a listener from being notified of map changes.
	 * 
	 * @param listener the listener to remove
	 */
	public void removeMapChangeListener(MapChangeListener<K, V> listener) {
		listeners.remove(listener);
	}

	private void notifyEntryAdded(K key, V value) {
		for (MapChangeListener<K, V> listener : listeners) {
			try {
				listener.entryAdded(key, value);
			} catch (Exception e) {
				// Continue with other listeners even if one fails
			}
		}
	}

	private void notifyEntryRemoved(K key, V value) {
		for (MapChangeListener<K, V> listener : listeners) {
			try {
				listener.entryRemoved(key, value);
			} catch (Exception e) {
				// Continue with other listeners even if one fails
			}
		}
	}

	private void notifyEntryUpdated(K key, V oldValue, V newValue) {
		for (MapChangeListener<K, V> listener : listeners) {
			try {
				listener.entryUpdated(key, oldValue, newValue);
			} catch (Exception e) {
				// Continue with other listeners even if one fails
			}
		}
	}

	private void notifyMapCleared() {
		for (MapChangeListener<K, V> listener : listeners) {
			try {
				listener.mapCleared();
			} catch (Exception e) {
				// Continue with other listeners even if one fails
			}
		}
	}
}
