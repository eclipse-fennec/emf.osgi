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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.fennec.emf.osgi.RegistryPropertyListener;
import org.eclipse.fennec.emf.osgi.RegistryTrackingService;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Test for {@link RegistryTrackingServiceComponent}
 * 
 * @author Mark Hoffmann
 * @since 15.01.2025
 */
@ExtendWith(MockitoExtension.class)
public class RegistryTrackingServiceComponentTest {

    private RegistryTrackingServiceComponent trackingService;
    
    @Mock
    private RegistryPropertyListener mockListener;
    
    @Mock
    private ServiceReference<EPackage.Registry> mockEPackageRegistryRef;
    
    @Mock 
    private ServiceReference<Resource.Factory.Registry> mockResourceFactoryRegistryRef;

    @BeforeEach
    void setUp() {
        trackingService = new RegistryTrackingServiceComponent();
    }

    @Test
    void testRegisterListener() {
        // Given
        Long serviceId = 123L;
        Set<Long> serviceIds = Collections.singleton(serviceId);
        
        // When
        trackingService.registerListener(mockListener, serviceIds);
        
        // Then
        Set<Long> trackedIds = trackingService.getTrackedServiceIds();
        // Note: Service won't be tracked until a service is actually added
        assertNotNull(trackedIds);
    }

    @Test
    void testUnregisterListener() {
        // Given
        Long serviceId = 123L;
        Set<Long> serviceIds = Collections.singleton(serviceId);
        trackingService.registerListener(mockListener, serviceIds);
        
        // When
        trackingService.unregisterListener(mockListener);
        
        // Then - should not fail
        assertTrue(true, "Unregister should complete without error");
    }

    @Test
    void testAddEPackageRegistry() {
        // Given
        Long serviceId = 100L;
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.SERVICE_ID, serviceId);
        properties.put(EMFNamespaces.EMF_MODEL_NAME, "testModel");
        properties.put(EMFNamespaces.EMF_MODEL_NSURI, "http://test.example/model");
        
        when(mockEPackageRegistryRef.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        when(mockEPackageRegistryRef.getProperties()).thenReturn(createDictionary(properties));
        
        // When
        trackingService.addEPackageRegistry(mockEPackageRegistryRef);
        
        // Then
        Set<Long> trackedIds = trackingService.getTrackedServiceIds();
        assertTrue(trackedIds.contains(serviceId));
        
        Map<String, Object> retrievedProperties = trackingService.getServiceProperties(serviceId);
        assertNotNull(retrievedProperties);
        assertEquals("testModel", retrievedProperties.get(EMFNamespaces.EMF_MODEL_NAME));
        assertEquals("http://test.example/model", retrievedProperties.get(EMFNamespaces.EMF_MODEL_NSURI));
    }

    @Test
    void testUpdateEPackageRegistryTriggersListener() {
        // Given
        Long serviceId = 200L;
        Set<Long> serviceIds = Collections.singleton(serviceId);
        trackingService.registerListener(mockListener, serviceIds);
        
        Map<String, Object> initialProperties = new HashMap<>();
        initialProperties.put(Constants.SERVICE_ID, serviceId);
        initialProperties.put(EMFNamespaces.EMF_MODEL_NAME, "initialModel");
        
        Map<String, Object> updatedProperties = new HashMap<>();
        updatedProperties.put(Constants.SERVICE_ID, serviceId);
        updatedProperties.put(EMFNamespaces.EMF_MODEL_NAME, "updatedModel");
        updatedProperties.put(EMFNamespaces.EMF_MODEL_NSURI, "http://updated.example/model");
        
        when(mockEPackageRegistryRef.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        when(mockEPackageRegistryRef.getProperties())
            .thenReturn(createDictionary(initialProperties))
            .thenReturn(createDictionary(updatedProperties));
        
        // Add the registry first
        trackingService.addEPackageRegistry(mockEPackageRegistryRef);
        
        // When - update the registry
        trackingService.updateEPackageRegistry(mockEPackageRegistryRef);
        
        // Then - verify listener was called
        ArgumentCaptor<Map<String, Object>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockListener, times(1)).onRegistryPropertiesChanged(
            eq(serviceId), 
            eq("EPackage.Registry"), 
            propertiesCaptor.capture()
        );
        
        Map<String, Object> capturedProperties = propertiesCaptor.getValue();
        assertEquals("updatedModel", capturedProperties.get(EMFNamespaces.EMF_MODEL_NAME));
        assertEquals("http://updated.example/model", capturedProperties.get(EMFNamespaces.EMF_MODEL_NSURI));
    }

    @Test 
    void testRemoveEPackageRegistryTriggersListener() {
        // Given
        Long serviceId = 300L;
        Set<Long> serviceIds = Collections.singleton(serviceId);
        trackingService.registerListener(mockListener, serviceIds);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.SERVICE_ID, serviceId);
        properties.put(EMFNamespaces.EMF_MODEL_NAME, "testModel");
        
        when(mockEPackageRegistryRef.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        when(mockEPackageRegistryRef.getProperties()).thenReturn(createDictionary(properties));
        
        // Add the registry first
        trackingService.addEPackageRegistry(mockEPackageRegistryRef);
        
        // When - remove the registry
        trackingService.removeEPackageRegistry(mockEPackageRegistryRef);
        
        // Then - verify listener was called and service is no longer tracked
        verify(mockListener, times(1)).onRegistryServiceRemoved(serviceId, "EPackage.Registry");
        assertFalse(trackingService.getTrackedServiceIds().contains(serviceId));
    }

    @Test
    void testAddResourceFactoryRegistry() {
        // Given
        Long serviceId = 400L;
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.SERVICE_ID, serviceId);
        properties.put("resource.factory.extension", "xml");
        
        when(mockResourceFactoryRegistryRef.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        when(mockResourceFactoryRegistryRef.getProperties()).thenReturn(createDictionary(properties));
        
        // When
        trackingService.addResourceFactoryRegistry(mockResourceFactoryRegistryRef);
        
        // Then
        Set<Long> trackedIds = trackingService.getTrackedServiceIds();
        assertTrue(trackedIds.contains(serviceId));
        
        Map<String, Object> retrievedProperties = trackingService.getServiceProperties(serviceId);
        assertNotNull(retrievedProperties);
        assertEquals("xml", retrievedProperties.get("resource.factory.extension"));
    }

    @Test
    void testUpdateResourceFactoryRegistryTriggersListener() {
        // Given
        Long serviceId = 500L;
        Set<Long> serviceIds = Collections.singleton(serviceId);
        trackingService.registerListener(mockListener, serviceIds);
        
        Map<String, Object> updatedProperties = new HashMap<>();
        updatedProperties.put(Constants.SERVICE_ID, serviceId);
        updatedProperties.put("resource.factory.extension", "xmi");
        
        when(mockResourceFactoryRegistryRef.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        when(mockResourceFactoryRegistryRef.getProperties()).thenReturn(createDictionary(updatedProperties));
        
        // When
        trackingService.updateResourceFactoryRegistry(mockResourceFactoryRegistryRef);
        
        // Then - verify listener was called
        verify(mockListener, times(1)).onRegistryPropertiesChanged(
            eq(serviceId), 
            eq("Resource.Factory.Registry"), 
            any(Map.class)
        );
    }

    @Test
    void testListenerNotCalledForUnregisteredServices() {
        // Given
        Long trackedServiceId = 600L;
        Long untrackedServiceId = 700L;
        Set<Long> serviceIds = Collections.singleton(trackedServiceId); // Only track one service
        trackingService.registerListener(mockListener, serviceIds);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.SERVICE_ID, untrackedServiceId);
        
        when(mockEPackageRegistryRef.getProperty(Constants.SERVICE_ID)).thenReturn(untrackedServiceId);
        when(mockEPackageRegistryRef.getProperties()).thenReturn(createDictionary(properties));
        
        // When - update a service that is not tracked by this listener
        trackingService.updateEPackageRegistry(mockEPackageRegistryRef);
        
        // Then - verify listener was NOT called
        verify(mockListener, never()).onRegistryPropertiesChanged(any(Long.class), any(String.class), any(Map.class));
    }

    // Helper method to create Dictionary from Map
    private Dictionary<String, Object> createDictionary(Map<String, Object> properties) {
        Dictionary<String, Object> dict = new Hashtable<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            dict.put(entry.getKey(), entry.getValue());
        }
        return dict;
    }
}