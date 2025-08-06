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

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.fennec.emf.osgi.configurator.EPackageConfigurator;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.helper.DelegatingHashMap;
import org.eclipse.fennec.emf.osgi.helper.MapChangeListener;
import org.eclipse.fennec.emf.osgi.helper.ServicePropertiesHelper;
import org.eclipse.fennec.emf.osgi.helper.ServicePropertyContext;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * An implementation of the Main  {@link EPackage} registry that replaces the static registry.
 * Uses {@link DelegatingHashMap} with change listeners to automatically update service properties
 * when EPackages are added or removed.
 */
@Component(name = StaticEPackageRegistryComponent.NAME, service = {})
@ProviderType
public class StaticEPackageRegistryComponent implements EPackage.Registry {
	
	/** DEFAULT_E_PACKAGE_REGISTRY */
	public static final String NAME = "StaticEPackageRegistryComponent";

	final DelegatingHashMap<String, Object> registry;
	private long serviceChangeCount = 0;

	private final ServiceRegistration<Registry> serviceRegistration;
	
	@Activate
	public StaticEPackageRegistryComponent(BundleContext ctx) {
		// Create registry with EMF's static registry as delegate
		registry = new DelegatingHashMap<>();
		
		// Add change listener to automatically update service properties
		registry.addMapChangeListener(new MapChangeListener<String, Object>() {
			@Override
			public void entryAdded(String key, Object value) {
				updateProperties();
			}

			@Override
			public void entryRemoved(String key, Object value) {
				updateProperties();
			}

			@Override
			public void entryUpdated(String key, Object oldValue, Object newValue) {
				updateProperties();
			}

			@Override
			public void mapCleared() {
				updateProperties();
			}
		});
		
		serviceRegistration = ctx.registerService(EPackage.Registry.class, this, getDictionary());
	}
	
	@Deactivate
	public void deactivate() {
		serviceRegistration.unregister();
	}
	
	/**
	 * Creates a dictionary for the stored properties
	 * @return a dictionary for the stored properties
	 */
	protected Dictionary<String, Object> getDictionary() {
		ServicePropertyContext propertyContext = ServicePropertyContext.create();
		values().stream().filter(EPackage.class::isInstance).map(EPackage.class::cast).map(this::getProperties).forEach(propertyContext::addSubContext);
		Dictionary<String, Object> properties = propertyContext.getDictionary(true);
		Map<String, Object> features = ServicePropertiesHelper.normalizeProperties(EMFNamespaces.EMF_MODEL_FEATURE + ".", FrameworkUtil.asMap(properties));
		features.forEach(properties::put);
		properties.put("emf.default.epackage.registry", "true");
		properties.put(ComponentConstants.COMPONENT_NAME, "StaticEPackageRegistry");
		properties.put(Constants.SERVICE_CHANGECOUNT, serviceChangeCount++);
		return properties;
	}
	
	private Map<String, Object> getProperties(EPackage ePackage) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(Constants.SERVICE_ID, Long.valueOf(ePackage.hashCode()));
		properties.put(EMFNamespaces.EMF_NAME, ePackage.getName());
		properties.put(EMFNamespaces.EMF_MODEL_NSURI, ePackage.getNsURI());
		return properties;
	}
	
	
	/**
	 * 
	 */
	private void updateProperties() {
		if(serviceRegistration != null) {
			serviceRegistration.setProperties(getDictionary());
		}
	}

	/**
	 * Adds {@link EPackageConfigurator}, to register a new {@link EPackage}
	 * @param configurator the {@link EPackageConfigurator} to be registered
	 * @param properties the service properties
	 */
	@Reference(name="ePackageConfigurator", policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.MULTIPLE, target="(" + EMFNamespaces.EMF_MODEL_SCOPE + "=" + EMFNamespaces.EMF_MODEL_SCOPE_STATIC + ")", unbind = "removeEPackageConfigurator")
	protected void addEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		configurator.configureEPackage(this);
	}

	/**
	 * Removes a {@link EPackageConfigurator} from the registry and unconfigures it
	 * @param configurator the configurator to be removed
	 * @param modelInfo the model information
	 * @param properties the service properties
	 */
	protected void removeEPackageConfigurator(EPackageConfigurator configurator, Map<String, Object> properties) {
		configurator.unconfigureEPackage(this);
	}
	
	
	/*
	 * Javadoc copied from interface.
	 */
	public EPackage getEPackage(String nsURI) {
		Object ePackage = get(nsURI);
		if (ePackage instanceof EPackage) {
			EPackage result = (EPackage) ePackage;
			return result;
		} else if (ePackage instanceof EPackage.Descriptor) {
			EPackage.Descriptor ePackageDescriptor = (EPackage.Descriptor) ePackage;
			EPackage result = ePackageDescriptor.getEPackage();
			if (result != null) {
				put(nsURI, result);
			}
			return result;
		} else {
			return null;
		}
	}

	/*
	 * Javadoc copied from interface.
	 */
	public EFactory getEFactory(String nsURI) {
		Object ePackage = get(nsURI);
		if (ePackage instanceof EPackage) {
			EPackage result = (EPackage) ePackage;
			return result.getEFactoryInstance();
		} else if (ePackage instanceof EPackage.Descriptor) {
			EPackage.Descriptor ePackageDescriptor = (EPackage.Descriptor) ePackage;
			return ePackageDescriptor.getEFactory();
		} else {
			return null;
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {
		return registry.size();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return registry.isEmpty();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {
		return registry.containsKey(key);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		return registry.containsValue(value);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	public Object get(Object key) {
		return registry.get(key);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object put(String key, Object value) {
		return registry.put(key, value);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public Object remove(Object key) {
		return registry.remove(key);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		registry.putAll(m);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		registry.clear();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(registry.keySet());
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<Object> values() {
		return Collections.unmodifiableCollection(registry.values());
	}

	/* 
	 * (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<Entry<String, Object>> entrySet() {
		return Collections.unmodifiableSet(new LinkedHashMap<>(registry).entrySet());
	}
	
	
}
