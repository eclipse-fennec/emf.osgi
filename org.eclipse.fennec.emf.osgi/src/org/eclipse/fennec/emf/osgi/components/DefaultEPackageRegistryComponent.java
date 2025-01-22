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
package org.eclipse.fennec.emf.osgi.components;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * An implementation of a package registry that can delegate failed lookup to
 * another registry. This implementation is derived from the default
 * {@link DefaultEPackageRegistryComponent} to be enabled as OSGi component
 */
@Component(name = DefaultEPackageRegistryComponent.NAME, service = EPackage.Registry.class)
@ProviderType
public class DefaultEPackageRegistryComponent extends HashMap<String, Object> implements EPackage.Registry {
	
	/** DEFAULT_E_PACKAGE_REGISTRY */
	public static final String NAME = "DefaultEPackageRegistry";

	private static final long serialVersionUID = 1L;

	/**
	 * The delegate registry.
	 */
	protected transient EPackage.Registry delegateRegistry;

	/**
	 * Creates a non-delegating instance.
	 */
	public DefaultEPackageRegistryComponent() {
		super();
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetDelegateRegistry", target = "(!(component.name=DefaultEPackageRegistry))")
	public void setDelegateRegistry(EPackage.Registry delegateRegistry) {
		if (delegateRegistry.equals(this)) {
			return;
		}
		this.delegateRegistry = delegateRegistry;
	}

	public void unsetDelegateRegistry(EPackage.Registry delegateRegistry) {
		this.delegateRegistry = null;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public EPackage getEPackage(String nsURI) {
		Object ePackage = get(nsURI);
		if (ePackage instanceof EPackage) {
			EPackage result = (EPackage) ePackage;
			if (result.getNsURI() == null) {
				initialize(result);
			}
			return result;
		} else if (ePackage instanceof EPackage.Descriptor) {
			EPackage.Descriptor ePackageDescriptor = (EPackage.Descriptor) ePackage;
			EPackage result = ePackageDescriptor.getEPackage();
			if (result != null) {
				if (result.getNsURI() == null) {
					initialize(result);
				} else {
					put(nsURI, result);
				}
			}
			return result;
		} else {
			return delegatedGetEPackage(nsURI);
		}
	}

	/*
	 * Javadoc copied from interface.
	 */
	public EFactory getEFactory(String nsURI) {
		Object ePackage = get(nsURI);
		if (ePackage instanceof EPackage) {
			EPackage result = (EPackage) ePackage;
			if (result.getNsURI() == null) {
				initialize(result);
			}
			return result.getEFactoryInstance();
		} else if (ePackage instanceof EPackage.Descriptor) {
			EPackage.Descriptor ePackageDescriptor = (EPackage.Descriptor) ePackage;
			return ePackageDescriptor.getEFactory();
		} else {
			return delegatedGetEFactory(nsURI);
		}
	}

	/**
	 * Creates a delegating instance.
	 */
	protected void initialize(EPackage ePackage) {
		// Nothing to do here
	}

	/**
	 * Returns the package from the delegate registry, if there is one.
	 * 
	 * @return the package from the delegate registry.
	 */
	protected EPackage delegatedGetEPackage(String nsURI) {
		if (delegateRegistry != null) {
			return delegateRegistry.getEPackage(nsURI);
		}

		return null;
	}

	/**
	 * Returns the factory from the delegate registry, if there is one.
	 * 
	 * @return the factory from the delegate registry.
	 */
	protected EFactory delegatedGetEFactory(String nsURI) {
		if (delegateRegistry != null) {
			return delegateRegistry.getEFactory(nsURI);
		}

		return null;
	}

	/**
	 * Returns whether this map or the delegate map contains this key. Note that if
	 * there is a delegate map, the result of this method may <em><b>not</b></em> be
	 * the same as <code>keySet().contains(key)</code>.
	 * 
	 * @param key the key whose presence in this map is to be tested.
	 * @return whether this map or the delegate map contains this key.
	 */
	@Override
	public boolean containsKey(Object key) {
		return super.containsKey(key) || delegateRegistry != null && delegateRegistry.containsKey(key);
	}
}
