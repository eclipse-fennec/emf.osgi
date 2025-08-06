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

/**
 * Listener interface for receiving notifications about changes to a map.
 * 
 * @param <K> the type of keys maintained by the map
 * @param <V> the type of mapped values
 * @author Juergen Albert
 */
public interface MapChangeListener<K, V> {

	/**
	 * Called when a key-value pair is added to the map.
	 * 
	 * @param key the key that was added
	 * @param value the value that was added
	 */
	void entryAdded(K key, V value);

	/**
	 * Called when a key-value pair is removed from the map.
	 * 
	 * @param key the key that was removed
	 * @param value the value that was removed
	 */
	void entryRemoved(K key, V value);

	/**
	 * Called when a key-value pair is updated in the map.
	 * 
	 * @param key the key that was updated
	 * @param oldValue the previous value
	 * @param newValue the new value
	 */
	void entryUpdated(K key, V oldValue, V newValue);

	/**
	 * Called when the map is cleared.
	 */
	void mapCleared();
}