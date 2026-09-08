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
package org.eclipse.fennec.emf.osgi.components;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EPackage;

/**
 * A live view onto the {@link EPackage.Registry} a registry component currently delegates its
 * failed lookups to. The registry components hand this view to their delegating registry once, at
 * construction time, and re-target it whenever their parent registry service comes, goes or is
 * replaced. While no parent is bound, lookups fall back to {@link EPackage.Registry#INSTANCE}, the
 * registry the generated model code registers itself in.
 *
 * @author Data In Motion Consulting
 */
public class SwitchableEPackageRegistry implements EPackage.Registry {

	private final AtomicReference<EPackage.Registry> target = new AtomicReference<>();

	/**
	 * Targets the given registry. A <code>null</code> registry falls back to
	 * {@link EPackage.Registry#INSTANCE}.
	 * @param registry the registry to delegate to, may be <code>null</code>
	 */
	public void setTarget(EPackage.Registry registry) {
		target.set(registry);
	}

	@Override
	public EPackage getEPackage(String nsURI) {
		return delegate().getEPackage(nsURI);
	}

	@Override
	public EFactory getEFactory(String nsURI) {
		return delegate().getEFactory(nsURI);
	}

	@Override
	public int size() {
		return delegate().size();
	}

	@Override
	public boolean isEmpty() {
		return delegate().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate().containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return delegate().get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return delegate().put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return delegate().remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		delegate().putAll(map);
	}

	@Override
	public void clear() {
		delegate().clear();
	}

	@Override
	public Set<String> keySet() {
		return delegate().keySet();
	}

	@Override
	public Collection<Object> values() {
		return delegate().values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return delegate().entrySet();
	}

	private EPackage.Registry delegate() {
		EPackage.Registry registry = target.get();
		return registry != null ? registry : EPackage.Registry.INSTANCE;
	}
}
