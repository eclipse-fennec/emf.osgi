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
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service that tracks EMF registry services and their properties, allowing components
 * to register for notifications when registry properties change. This eliminates the
 * need for duplicate injection points in components that need both early service access
 * and property update notifications.
 * 
 * @author Mark Hoffmann
 * @since 15.01.2025
 */
@ProviderType
public interface RegistryTrackingService {

    /**
     * Register a listener to be notified when properties change on specific registry services.
     * The listener will be called whenever properties are updated on any of the specified service IDs.
     * 
     * @param listener the listener to register
     * @param serviceIds the set of service IDs to track for this listener
     */
    void registerListener(RegistryPropertyListener listener, Set<Long> serviceIds);
    
    /**
     * Unregister a listener from receiving property change notifications.
     * 
     * @param listener the listener to unregister
     */
    void unregisterListener(RegistryPropertyListener listener);
    
    /**
     * Get the current properties for a specific registry service.
     * 
     * @param serviceId the service ID to get properties for
     * @return the current properties, or null if service is not tracked
     */
    Map<String, Object> getServiceProperties(long serviceId);
    
    /**
     * Get all currently tracked registry service IDs.
     * 
     * @return set of service IDs being tracked
     */
    Set<Long> getTrackedServiceIds();
}