/**
 * Copyright (c) 2012 - 2025 Data In Motion and others.
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

import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;

import org.eclipse.fennec.emf.osgi.helper.ServicePropertyContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;

/**
 * 
 * @author Juergen Albert
 * @since 26 Jun 2025
 */
public abstract class SelfRegisteringServiceComponent {

	private final ServicePropertyContext propertyContext;
	private ServiceRegistration<?> serviceRegistration;
	private long serviceChangeCount = 0;
	final String componentName;
	private Map<String, Object> defaultProperties;

	/**
	 * Creates a new instance.
	 */
	public SelfRegisteringServiceComponent(BundleContext ctx, String componentName, Map<String, Object> defaultProperties) {
		super();
		checkEMFEcore(ctx);
		this.componentName = componentName;
		Objects.requireNonNull(defaultProperties);
		this.defaultProperties = defaultProperties;
		this.propertyContext = ServicePropertyContext.create();
	}
	
	/**
	 * We have to make sure that org.eclipse.emf.ecore is started, so we don't run 
	 * into start order issues due to the use of static access in EMF 
	 * @param ctx the {@link BundleContext} to use
	 */
	private void checkEMFEcore(BundleContext ctx) {
		Bundle[] bundles = ctx.getBundles();
		
		for(Bundle bundle : bundles) {
			if("org.eclipse.emf.ecore".equals(bundle.getSymbolicName())) {
				try {
					bundle.start();
				} catch (BundleException e) {
					System.err.println("Could not start Bundle org.eclipse.emf.ecore, something seems seriously wrong: " + e.getMessage());
					e.printStackTrace();
				}
				break;
			}
		}
	}

	protected <T> void registerService(BundleContext ctx, Class<T> clazz, T service) {
		serviceRegistration = ctx.registerService(clazz, service, getDictionary());
	}

	protected void doDeactivate() {
		serviceRegistration.unregister();
		serviceRegistration = null;
	}

	/**
	 * Updates the registry's properties
	 */
	protected void updateRegistrationProperties() {
		if (serviceRegistration != null) {
			serviceChangeCount++;
			serviceRegistration.setProperties(getDictionary());
		}
	}

	/**
	 * Creates a dictionary for the stored properties
	 * @return a dictionary for the stored properties
	 */
	protected Dictionary<String, Object> getDictionary() {
		Dictionary<String, Object> properties = propertyContext.getDictionary(true);
		defaultProperties.forEach(properties::put);
		properties.put(ComponentConstants.COMPONENT_NAME, componentName);
		properties.put(Constants.SERVICE_CHANGECOUNT, serviceChangeCount++);
		return properties;
	}

	/**
	 * Returns the propertyContext.
	 * @return the propertyContext
	 */
	protected ServicePropertyContext getPropertyContext() {
		return propertyContext;
	}
}