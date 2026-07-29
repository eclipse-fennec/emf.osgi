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
package org.eclipse.fennec.emf.osgi.fingerprint.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.junit.jupiter.api.Test;

/**
 * The shared model property core (issue #54): every emission site builds
 * name/nsURI/fingerprint through this one helper, so no site can register a model
 * without the fingerprint.
 */
class ModelPropertiesHelperTest {

    private static EPackage pkg(String name, String nsURI) {
        EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
        pkg.setName(name);
        pkg.setNsPrefix("p");
        pkg.setNsURI(nsURI);
        return pkg;
    }

    // ---- positive ---------------------------------------------------------------

    @Test
    void containsTheCoreTrio() {
        Map<String, Object> properties = ModelPropertiesHelper.modelProperties(pkg("core", "http://example.org/core/1.0"));
        assertEquals("core", properties.get(EMFNamespaces.EMF_NAME));
        assertEquals("http://example.org/core/1.0", properties.get(EMFNamespaces.EMF_MODEL_NSURI));
        assertEquals(FingerprintHelper.fingerprint(pkg("core", "http://example.org/core/1.0")),
                properties.get(EMFNamespaces.EMF_MODEL_FINGERPRINT));
    }

    @Test
    void fingerprintCarriesTheSchemeTag() {
        Object fingerprint = ModelPropertiesHelper.modelProperties(pkg("core", "http://example.org/core/1.0"))
                .get(EMFNamespaces.EMF_MODEL_FINGERPRINT);
        assertTrue(String.valueOf(fingerprint).startsWith("fp1:"));
    }

    @Test
    void theMapIsMutableForSiteSpecificAdditions() {
        Map<String, Object> properties = ModelPropertiesHelper.modelProperties(pkg("core", "http://example.org/core/1.0"));
        properties.put(EMFNamespaces.EMF_MODEL_REGISTRATION, EMFNamespaces.MODEL_REGISTRATION_PROVIDED);
        assertEquals(EMFNamespaces.MODEL_REGISTRATION_PROVIDED, properties.get(EMFNamespaces.EMF_MODEL_REGISTRATION));
    }

    // ---- negative ---------------------------------------------------------------

    @Test
    void nullNameIsSkippedButTheFingerprintIsPresent() {
        Map<String, Object> properties = ModelPropertiesHelper.modelProperties(pkg(null, "http://example.org/core/1.0"));
        assertFalse(properties.containsKey(EMFNamespaces.EMF_NAME));
        assertTrue(properties.containsKey(EMFNamespaces.EMF_MODEL_FINGERPRINT));
    }

    @Test
    void nullNsUriIsSkippedButTheFingerprintIsPresent() {
        Map<String, Object> properties = ModelPropertiesHelper.modelProperties(pkg("core", null));
        assertFalse(properties.containsKey(EMFNamespaces.EMF_MODEL_NSURI));
        assertTrue(properties.containsKey(EMFNamespaces.EMF_MODEL_FINGERPRINT));
    }

    @Test
    void nullPackageIsRejected() {
        assertThrows(NullPointerException.class, () -> ModelPropertiesHelper.modelProperties(null));
    }
}
