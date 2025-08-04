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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.fennec.emf.osgi.RegistryPropertyListener;
import org.eclipse.fennec.emf.osgi.RegistryTrackingService;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Implementation of the RegistryTrackingService that tracks EMF registry services
 * and notifies listeners when their properties change.
 */
@Component(service = RegistryTrackingService.class, immediate = true)
@ProviderType
public class RegistryTrackingServiceComponent implements RegistryTrackingService {

    // Track service properties by service ID
    private final Map<Long, ServiceInfo> trackedServices = new ConcurrentHashMap<>();
    
    // Track listeners and the service IDs they're interested in
    private final Map<RegistryPropertyListener, Set<Long>> listenerServiceIds = new ConcurrentHashMap<>();
    
    // Reverse mapping: service ID to listeners interested in it
    private final Map<Long, Set<RegistryPropertyListener>> serviceListeners = new ConcurrentHashMap<>();

    private static class ServiceInfo {
        final String serviceName;
        final Map<String, Object> properties;
        
        ServiceInfo(String serviceName, Map<String, Object> properties) {
            this.serviceName = serviceName;
            this.properties = Collections.unmodifiableMap(properties);
        }
    }

    @Override
    public void registerListener(RegistryPropertyListener listener, Set<Long> serviceIds) {
        Set<Long> previousServiceIds = listenerServiceIds.put(listener, new CopyOnWriteArraySet<>(serviceIds));
        
        // Remove listener from previous service mappings
        if (previousServiceIds != null) {
            for (Long serviceId : previousServiceIds) {
                Set<RegistryPropertyListener> listeners = serviceListeners.get(serviceId);
                if (listeners != null) {
                    listeners.remove(listener);
                    if (listeners.isEmpty()) {
                        serviceListeners.remove(serviceId);
                    }
                }
            }
        }
        
        // Add listener to new service mappings
        for (Long serviceId : serviceIds) {
            serviceListeners.computeIfAbsent(serviceId, k -> new CopyOnWriteArraySet<>()).add(listener);
        }
    }

    @Override
    public void unregisterListener(RegistryPropertyListener listener) {
        Set<Long> serviceIds = listenerServiceIds.remove(listener);
        if (serviceIds != null) {
            for (Long serviceId : serviceIds) {
                Set<RegistryPropertyListener> listeners = serviceListeners.get(serviceId);
                if (listeners != null) {
                    listeners.remove(listener);
                    if (listeners.isEmpty()) {
                        serviceListeners.remove(serviceId);
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Object> getServiceProperties(long serviceId) {
        ServiceInfo info = trackedServices.get(serviceId);
        return info != null ? info.properties : null;
    }

    @Override
    public Set<Long> getTrackedServiceIds() {
        return Collections.unmodifiableSet(trackedServices.keySet());
    }

    // Track EPackage Registry services
    @Reference(name = "epackageRegistry", 
               service = EPackage.Registry.class,
               cardinality = ReferenceCardinality.MULTIPLE, 
               policy = ReferencePolicy.DYNAMIC,
               unbind = "removeEPackageRegistry",
               updated = "updateEPackageRegistry")
    protected void addEPackageRegistry(ServiceReference<EPackage.Registry> serviceRef) {
        long serviceId = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
        Map<String, Object> properties = FrameworkUtil.asMap(serviceRef.getProperties());
        trackedServices.put(serviceId, new ServiceInfo("EPackage.Registry", properties));
    }

    protected void updateEPackageRegistry(ServiceReference<EPackage.Registry> serviceRef) {
        long serviceId = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
        Map<String, Object> properties = FrameworkUtil.asMap(serviceRef.getProperties());
        trackedServices.put(serviceId, new ServiceInfo("EPackage.Registry", properties));
        
        // Notify interested listeners
        notifyListeners(serviceId, "EPackage.Registry", properties);
    }

    protected void removeEPackageRegistry(ServiceReference<EPackage.Registry> serviceRef) {
        long serviceId = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
        ServiceInfo removed = trackedServices.remove(serviceId);
        if (removed != null) {
            notifyListenersOfRemoval(serviceId, removed.serviceName);
        }
    }

    // Track Resource Factory Registry services
    @Reference(name = "resourceFactoryRegistry",
               service = Resource.Factory.Registry.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "removeResourceFactoryRegistry",
               updated = "updateResourceFactoryRegistry")
    protected void addResourceFactoryRegistry(ServiceReference<Resource.Factory.Registry> serviceRef) {
        long serviceId = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
        Map<String, Object> properties = FrameworkUtil.asMap(serviceRef.getProperties());
        trackedServices.put(serviceId, new ServiceInfo("Resource.Factory.Registry", properties));
    }

    protected void updateResourceFactoryRegistry(ServiceReference<Resource.Factory.Registry> serviceRef) {
        long serviceId = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
        Map<String, Object> properties = FrameworkUtil.asMap(serviceRef.getProperties());
        trackedServices.put(serviceId, new ServiceInfo("Resource.Factory.Registry", properties));
        
        // Notify interested listeners
        notifyListeners(serviceId, "Resource.Factory.Registry", properties);
    }

    protected void removeResourceFactoryRegistry(ServiceReference<Resource.Factory.Registry> serviceRef) {
        long serviceId = (Long) serviceRef.getProperty(Constants.SERVICE_ID);
        ServiceInfo removed = trackedServices.remove(serviceId);
        if (removed != null) {
            notifyListenersOfRemoval(serviceId, removed.serviceName);
        }
    }

    private void notifyListeners(long serviceId, String serviceName, Map<String, Object> properties) {
        Set<RegistryPropertyListener> listeners = serviceListeners.get(serviceId);
        if (listeners != null) {
            for (RegistryPropertyListener listener : listeners) {
                try {
                    listener.onRegistryPropertiesChanged(serviceId, serviceName, properties);
                } catch (Exception e) {
                    // Log error but continue with other listeners
                    System.err.println("Error notifying listener: " + e.getMessage());
                }
            }
        }
    }

    private void notifyListenersOfRemoval(long serviceId, String serviceName) {
        Set<RegistryPropertyListener> listeners = serviceListeners.remove(serviceId);
        if (listeners != null) {
            for (RegistryPropertyListener listener : listeners) {
                try {
                    listener.onRegistryServiceRemoved(serviceId, serviceName);
                } catch (Exception e) {
                    // Log error but continue with other listeners
                    System.err.println("Error notifying listener of removal: " + e.getMessage());
                }
            }
        }
    }
}