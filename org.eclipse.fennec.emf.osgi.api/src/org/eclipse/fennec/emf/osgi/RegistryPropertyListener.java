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
package org.eclipse.fennec.emf.osgi;

import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Interface for components that want to be notified when registry service properties change.
 * This allows components to react to dynamic changes in EMF registry capabilities without
 * requiring duplicate injection points.
 * 
 * @author Mark Hoffmann
 * @since 15.01.2025
 */
@ConsumerType
public interface RegistryPropertyListener {

    /**
     * Called when properties of a tracked registry service have been updated.
     * 
     * @param serviceId the service ID of the registry that changed
     * @param serviceName the name/type of the registry service 
     * @param newProperties the updated service properties
     */
    void onRegistryPropertiesChanged(long serviceId, String serviceName, Map<String, Object> newProperties);
    
    /**
     * Called when a tracked registry service becomes unavailable.
     * 
     * @param serviceId the service ID of the registry that was removed
     * @param serviceName the name/type of the registry service
     */
    void onRegistryServiceRemoved(long serviceId, String serviceName);
}