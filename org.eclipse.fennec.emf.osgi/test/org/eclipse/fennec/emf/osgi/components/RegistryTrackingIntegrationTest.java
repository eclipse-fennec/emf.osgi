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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.RegistryPropertyListener;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Integration test for RegistryTrackingServiceComponent focusing on real-world scenarios
 * like manual EPackageConfigurator registration triggering listeners.
 * 
 * @author Mark Hoffmann
 * @since 15.01.2025
 */
public class RegistryTrackingIntegrationTest {

    private RegistryTrackingServiceComponent trackingService;

    @BeforeEach
    void setUp() {
        trackingService = new RegistryTrackingServiceComponent();
    }

    @Test
    @Timeout(5)
    void testManualEPackageConfiguratorRegistrationTriggersListener() throws InterruptedException {
        // Given - Simulate the scenario described: manual EPackageConfigurator registration
        Long ePackageRegistryServiceId = 1000L;
        String modelName = "manual";
        String nsUri = "http://manual.example/model";
        
        // Setup a listener that will capture the property changes
        CountDownLatch propertyChangeLatch = new CountDownLatch(1);
        AtomicReference<Map<String, Object>> capturedProperties = new AtomicReference<>();
        
        RegistryPropertyListener testListener = new RegistryPropertyListener() {
            @Override
            public void onRegistryPropertiesChanged(long serviceId, String serviceName, Map<String, Object> newProperties) {
                if (serviceId == ePackageRegistryServiceId) {
                    capturedProperties.set(newProperties);
                    propertyChangeLatch.countDown();
                }
            }
            
            @Override
            public void onRegistryServiceRemoved(long serviceId, String serviceName) {
                // Not needed for this test
            }
        };
        
        // Register the listener for the EPackage registry service
        Set<Long> trackedServiceIds = Collections.singleton(ePackageRegistryServiceId);
        trackingService.registerListener(testListener, trackedServiceIds);
        
        // When - Simulate EPackage registry service being registered
        ServiceReference<EPackage.Registry> mockRegistryRef = createMockEPackageRegistryReference(
            ePackageRegistryServiceId, modelName, nsUri);
        trackingService.addEPackageRegistry(mockRegistryRef);
        
        // Then - Simulate a manual EPackageConfigurator being registered, which updates the registry properties
        Dictionary<String, Object> updatedProperties = new Hashtable<>();
        updatedProperties.put(Constants.SERVICE_ID, ePackageRegistryServiceId);
        updatedProperties.put(EMFNamespaces.EMF_MODEL_NAME, modelName);
        updatedProperties.put(EMFNamespaces.EMF_MODEL_NSURI, nsUri);
        updatedProperties.put("emf.model.scope", "resourceset");
        updatedProperties.put("manual.configurator.registered", "true");
        
        // Simulate the registry service properties being updated
        ServiceReference<EPackage.Registry> updatedRegistryRef = mock(ServiceReference.class);
        when(updatedRegistryRef.getProperty(Constants.SERVICE_ID)).thenReturn(ePackageRegistryServiceId);
        when(updatedRegistryRef.getProperties()).thenReturn(updatedProperties);
        
        trackingService.updateEPackageRegistry(updatedRegistryRef);
        
        // Verify the listener was triggered within reasonable time
        assertTrue(propertyChangeLatch.await(2, TimeUnit.SECONDS), 
            "Property change listener should have been triggered");
        
        // Verify the captured properties contain the expected manual configurator information
        Map<String, Object> receivedProperties = capturedProperties.get();
        assertNotNull(receivedProperties, "Should have received property change notification");
        assertEquals(modelName, receivedProperties.get(EMFNamespaces.EMF_MODEL_NAME));
        assertEquals(nsUri, receivedProperties.get(EMFNamespaces.EMF_MODEL_NSURI));
        assertEquals("resourceset", receivedProperties.get("emf.model.scope"));
        assertEquals("true", receivedProperties.get("manual.configurator.registered"));
    }

    @Test
    void testMultipleListenersReceiveNotifications() {
        // Given - Multiple listeners interested in the same service
        Long serviceId = 2000L;
        RegistryPropertyListener listener1 = mock(RegistryPropertyListener.class);
        RegistryPropertyListener listener2 = mock(RegistryPropertyListener.class);
        
        Set<Long> trackedServiceIds = Collections.singleton(serviceId);
        trackingService.registerListener(listener1, trackedServiceIds);
        trackingService.registerListener(listener2, trackedServiceIds);
        
        // When - Service properties are updated
        ServiceReference<EPackage.Registry> mockRegistryRef = createMockEPackageRegistryReference(
            serviceId, "testModel", "http://test.example/model");
        trackingService.updateEPackageRegistry(mockRegistryRef);
        
        // Then - Both listeners should be notified
        verify(listener1, times(1)).onRegistryPropertiesChanged(eq(serviceId), eq("EPackage.Registry"), any(Map.class));
        verify(listener2, times(1)).onRegistryPropertiesChanged(eq(serviceId), eq("EPackage.Registry"), any(Map.class));
    }

    @Test
    void testServiceRemovalNotification() {
        // Given - A listener tracking a service
        Long serviceId = 3000L;
        RegistryPropertyListener mockListener = mock(RegistryPropertyListener.class);
        
        Set<Long> trackedServiceIds = Collections.singleton(serviceId);
        trackingService.registerListener(mockListener, trackedServiceIds);
        
        // Add the service first
        ServiceReference<EPackage.Registry> mockRegistryRef = createMockEPackageRegistryReference(
            serviceId, "testModel", "http://test.example/model");
        trackingService.addEPackageRegistry(mockRegistryRef);
        
        // When - Service is removed
        trackingService.removeEPackageRegistry(mockRegistryRef);
        
        // Then - Listener should be notified of removal
        verify(mockListener, times(1)).onRegistryServiceRemoved(serviceId, "EPackage.Registry");
    }

    private ServiceReference<EPackage.Registry> createMockEPackageRegistryReference(
            Long serviceId, String modelName, String nsUri) {
        
        @SuppressWarnings("unchecked")
        ServiceReference<EPackage.Registry> mockRef = mock(ServiceReference.class);
        
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_ID, serviceId);
        properties.put(EMFNamespaces.EMF_MODEL_NAME, modelName);
        properties.put(EMFNamespaces.EMF_MODEL_NSURI, nsUri);
        
        when(mockRef.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        when(mockRef.getProperties()).thenReturn(properties);
        
        return mockRef;
    }

    // Helper method for mocking (missing import)
    private static <T> org.mockito.stubbing.OngoingStubbing<T> when(T methodCall) {
        return org.mockito.Mockito.when(methodCall);
    }
}