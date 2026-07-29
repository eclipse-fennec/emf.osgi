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

import java.util.Arrays;
import java.util.Locale;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Bind-cost measurement for issue #56 (Phase 1 exit criterion). Measures the <b>uncached</b> canonicalization +
 * SHA-256 per package — the cost every registration pays exactly once — and the cached
 * lookup that every later property re-aggregation pays.
 * <p>
 * {@code @Disabled} by default: timing assertions have no place in CI. Run manually and
 * record the numbers in the migration document:
 * {@code ./gradlew :org.eclipse.fennec.emf.osgi:test --tests '*FingerprintCostMeasurement*'}
 * with the {@code @Disabled} line commented out.
 */
@Disabled("manual measurement, numbers recorded in docs/model-fingerprint-guide.md")
class FingerprintCostMeasurement {

    private static final int WARMUP = 100;
    private static final int RUNS = 500;

    @Test
    void measure() {
        DefaultFingerprintService service = new DefaultFingerprintService();
        measure("Ecore (EcorePackage, ~20 classifiers + generics)", service, EcorePackage.eINSTANCE);
        measure("XMLType (XMLTypePackage, ~55 data types)", service, XMLTypePackage.eINSTANCE);
        measure("synthetic 200 classes x 10 attributes", service, synthetic(200, 10));
        measureCachedLookup();
    }

    private static void measure(String label, DefaultFingerprintService service, EPackage ePackage) {
        for (int i = 0; i < WARMUP; i++) {
            service.fingerprint(ePackage);
        }
        long[] nanos = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            service.fingerprint(ePackage);
            nanos[i] = System.nanoTime() - start;
        }
        report(label, nanos);
    }

    private static void measureCachedLookup() {
        EPackage ePackage = synthetic(200, 10);
        FingerprintHelper.fingerprint(ePackage); // populate the cache
        long[] nanos = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            FingerprintHelper.fingerprint(ePackage);
            nanos[i] = System.nanoTime() - start;
        }
        report("cached lookup (FingerprintHelper, synthetic 200x10)", nanos);
    }

    private static void report(String label, long[] nanos) {
        Arrays.sort(nanos);
        long median = nanos[nanos.length / 2];
        long p95 = nanos[(int) (nanos.length * 0.95)];
        System.out.printf(Locale.ROOT, "%-55s median %8.1f us   p95 %8.1f us%n",
                label, median / 1000.0, p95 / 1000.0);
    }

    private static EPackage synthetic(int classes, int attributesPerClass) {
        EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
        pkg.setName("synthetic");
        pkg.setNsPrefix("syn");
        pkg.setNsURI("http://example.org/synthetic/1.0");
        for (int c = 0; c < classes; c++) {
            EClass eClass = EcoreFactory.eINSTANCE.createEClass();
            eClass.setName("Clazz" + c);
            pkg.getEClassifiers().add(eClass);
            for (int a = 0; a < attributesPerClass; a++) {
                EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();
                attribute.setName("attr" + a);
                attribute.setEType(EcorePackage.Literals.ESTRING);
                eClass.getEStructuralFeatures().add(attribute);
            }
        }
        return pkg;
    }
}
