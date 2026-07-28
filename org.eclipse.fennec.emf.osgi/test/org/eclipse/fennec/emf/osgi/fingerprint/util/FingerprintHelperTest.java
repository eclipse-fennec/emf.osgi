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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.junit.jupiter.api.Test;

/**
 * The static fingerprint entry point (issue #54, decisions M3/M4): value identity with
 * the service facade, cache soundness across equal-content instances, null handling and
 * benign concurrency.
 */
class FingerprintHelperTest {

    private static EPackage samplePackage() {
        EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
        pkg.setName("helper");
        pkg.setNsPrefix("hlp");
        pkg.setNsURI("http://example.org/helper/1.0");

        EClass person = EcoreFactory.eINSTANCE.createEClass();
        person.setName("Person");
        pkg.getEClassifiers().add(person);

        EAttribute name = EcoreFactory.eINSTANCE.createEAttribute();
        name.setName("name");
        name.setEType(EcorePackage.Literals.ESTRING);
        person.getEStructuralFeatures().add(name);
        return pkg;
    }

    // ---- positive ---------------------------------------------------------------

    @Test
    void matchesTheServiceFacadeValue() {
        EPackage pkg = samplePackage();
        assertEquals(new DefaultFingerprintService().fingerprint(pkg), FingerprintHelper.fingerprint(pkg),
                "the static helper and the service facade must be two doors to the same value");
    }

    @Test
    void cachedRepeatCallsReturnTheSameValue() {
        EPackage pkg = samplePackage();
        assertEquals(FingerprintHelper.fingerprint(pkg), FingerprintHelper.fingerprint(pkg));
    }

    @Test
    void equalContentInstancesYieldTheSameValue() {
        // Two distinct instances: the identity-keyed cache must never make equal
        // content diverge — determinism is the property that keeps races benign.
        assertEquals(FingerprintHelper.fingerprint(samplePackage()), FingerprintHelper.fingerprint(samplePackage()));
    }

    @Test
    void derivationInputsMatchTheServiceAndBypassTheCache() {
        EPackage pkg = samplePackage();
        String withInputs = FingerprintHelper.fingerprint(pkg, "eorm=2.1");
        assertEquals(new DefaultFingerprintService().fingerprint(pkg, "eorm=2.1"), withInputs);
        assertNotEquals(FingerprintHelper.fingerprint(pkg), withInputs,
                "derivation inputs must yield a different (artifact) fingerprint");
    }

    @Test
    void concurrentCallsYieldOneValue() throws Exception {
        EPackage pkg = samplePackage();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                calls.add(() -> FingerprintHelper.fingerprint(pkg));
            }
            Set<String> values = new HashSet<>();
            for (Future<String> future : pool.invokeAll(calls)) {
                values.add(future.get());
            }
            assertEquals(1, values.size(), "a race may duplicate work but never produce a second value");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void schemeAccessorsDelegate() {
        assertEquals("fp1", FingerprintHelper.currentScheme());
        assertTrue(FingerprintHelper.supportedSchemes().contains("fp1"));
        EPackage pkg = samplePackage();
        assertEquals(FingerprintHelper.fingerprint(pkg), FingerprintHelper.fingerprintInScheme("fp1", pkg));
    }

    // ---- negative ---------------------------------------------------------------

    @Test
    void nullPackageYieldsNull() {
        assertNull(FingerprintHelper.fingerprint(null));
        assertNull(FingerprintHelper.fingerprintInScheme("fp1", null));
    }

    @Test
    void unknownSchemeIsACallerError() {
        assertThrows(IllegalArgumentException.class,
                () -> FingerprintHelper.fingerprintInScheme("fp0", samplePackage()));
    }

    @Test
    void differentContentYieldsDifferentValues() {
        EPackage changed = samplePackage();
        ((EClass) changed.getEClassifier("Person")).setAbstract(true);
        assertNotEquals(FingerprintHelper.fingerprint(samplePackage()), FingerprintHelper.fingerprint(changed));
    }
}
