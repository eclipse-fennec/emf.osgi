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
package org.eclipse.fennec.emf.osgi.components.fingerprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemoryArtifactStore} — acceptance criteria (positive) and
 * non-criteria (negative) from the WP2 work package.
 */
class InMemoryArtifactStoreTest {

    private InMemoryArtifactStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryArtifactStore();
    }

    /** A concrete, self-contained EObject to use as an artifact. */
    private static EAnnotation artifact(String source, String key, String value) {
        EAnnotation a = EcoreFactory.eINSTANCE.createEAnnotation();
        a.setSource(source);
        a.getDetails().put(key, value);
        return a;
    }

    // ---- acceptance criteria (positive) --------------------------------------

    @Test
    void roundTrip() {
        EAnnotation a = artifact("s", "k", "v");
        store.put("fp1", "eorm", a);

        Optional<EObject> resolved = store.resolve("fp1", "eorm");
        assertTrue(resolved.isPresent());
        assertTrue(EcoreUtil.equals(a, resolved.get()));
    }

    @Test
    void missReturnsEmpty() {
        assertFalse(store.resolve("nope", "eorm").isPresent());
    }

    @Test
    void typeIdsAreIsolatedUnderSameFingerprint() {
        store.put("fp1", "eorm", artifact("s", "k", "eorm-value"));
        store.put("fp1", "ocl", artifact("s", "k", "ocl-value"));

        EAnnotation eorm = (EAnnotation) store.resolve("fp1", "eorm").orElseThrow();
        EAnnotation ocl = (EAnnotation) store.resolve("fp1", "ocl").orElseThrow();
        assertEquals("eorm-value", eorm.getDetails().get("k"));
        assertEquals("ocl-value", ocl.getDetails().get("k"));
        assertEquals(2, store.size());
    }

    @Test
    void idempotentPutKeepsSingleEntry() {
        store.put("fp1", "eorm", artifact("s", "k", "v"));
        store.put("fp1", "eorm", artifact("s", "k", "v"));
        assertEquals(1, store.size());
    }

    @Test
    void putReplacesExistingContent() {
        store.put("fp1", "eorm", artifact("s", "k", "old"));
        store.put("fp1", "eorm", artifact("s", "k", "new"));
        assertEquals(1, store.size());
        EAnnotation resolved = (EAnnotation) store.resolve("fp1", "eorm").orElseThrow();
        assertEquals("new", resolved.getDetails().get("k"));
    }

    @Test
    void storeIsIsolatedFromCallerMutationOnPut() {
        EAnnotation a = artifact("s", "k", "v1");
        store.put("fp1", "eorm", a);
        a.getDetails().put("k", "MUTATED"); // mutate the original after storing

        EAnnotation resolved = (EAnnotation) store.resolve("fp1", "eorm").orElseThrow();
        assertEquals("v1", resolved.getDetails().get("k"), "stored copy must not reflect caller mutation");
    }

    @Test
    void storeIsIsolatedFromCallerMutationOnResolve() {
        store.put("fp1", "eorm", artifact("s", "k", "v1"));

        EAnnotation first = (EAnnotation) store.resolve("fp1", "eorm").orElseThrow();
        first.getDetails().put("k", "MUTATED"); // mutate the returned copy
        assertNotSame(first, store.resolve("fp1", "eorm").orElseThrow());

        EAnnotation second = (EAnnotation) store.resolve("fp1", "eorm").orElseThrow();
        assertEquals("v1", second.getDetails().get("k"), "store must not reflect mutation of a resolved copy");
    }

    // ---- non-criteria (negative) ---------------------------------------------

    @Test
    void resolveWithWrongTypeIdReturnsEmpty() {
        store.put("fp1", "eorm", artifact("s", "k", "v"));
        assertFalse(store.resolve("fp1", "ocl").isPresent(),
                "must not return an artifact stored under a different typeId");
    }

    @Test
    void resolveWithDifferentFingerprintReturnsEmpty() {
        store.put("fp1", "eorm", artifact("s", "k", "v"));
        assertFalse(store.resolve("fp2", "eorm").isPresent(),
                "must not match by typeId alone when the fingerprint differs");
    }

    @Test
    void nullArgumentsAreTolerated() {
        // resolve with null -> empty, no exception
        assertFalse(store.resolve(null, "eorm").isPresent());
        assertFalse(store.resolve("fp1", null).isPresent());
        // put with null -> no-op, no exception, nothing stored
        store.put(null, "eorm", artifact("s", "k", "v"));
        store.put("fp1", null, artifact("s", "k", "v"));
        store.put("fp1", "eorm", null);
        assertEquals(0, store.size());
        assertFalse(store.resolve("fp1", "eorm").isPresent());
    }

    // ---- thread-safety --------------------------------------------------------

    @Test
    void concurrentPutAndResolve() throws InterruptedException {
        int threads = 8;
        int perThread = 100;
        int total = threads * perThread;

        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier gate = new CyclicBarrier(threads);
        try {
            for (int t = 0; t < threads; t++) {
                int base = t * perThread;
                pool.submit(() -> {
                    try {
                        gate.await();
                        for (int i = 0; i < perThread; i++) {
                            String fp = "fp-" + (base + i);
                            store.put(fp, "eorm", artifact("s", "k", "v" + (base + i)));
                            store.resolve(fp, "eorm");
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "workers did not finish");
        } finally {
            pool.shutdownNow();
        }

        assertTrue(errors.isEmpty(), "concurrency errors: " + new ArrayList<>(errors));
        assertEquals(total, store.size(), "every distinct entry must be stored exactly once");
        for (int i = 0; i < total; i++) {
            assertTrue(store.resolve("fp-" + i, "eorm").isPresent(), "missing entry " + i);
        }
    }
}
