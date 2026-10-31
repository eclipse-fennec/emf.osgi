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
package org.eclipse.fennec.emf.osgi.eobject.registry.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistries;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.emf.osgi.metadata.MetadataServices;
import org.eclipse.fennec.emf.osgi.metadata.MetadataWhiteboard;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The bridge places and removes aspects while other threads read them (issue #129).
 * Readers iterate the aspect lists under the service's read lock; the bridge's registry
 * callbacks write under its write lock, taken before the bridge's own lock.
 */
public class RegistryMetadataBridgeConcurrencyTest {

	private static final String TYPE_ID = "test.aspect";
	private static final int ROUNDS = 2_000;

	private EPackage domainPackage;
	private EClass contentClass;
	private EAttribute nameAttribute;

	private MetadataWhiteboard whiteboard;
	private EObjectRegistryWriter writer;
	private RegistryMetadataBridge bridge;
	private ExecutorService executor;

	@BeforeEach
	public void setUp() {
		domainPackage = EcoreFactory.eINSTANCE.createEPackage();
		domainPackage.setName("content");
		domainPackage.setNsPrefix("content");
		domainPackage.setNsURI("http://fennec.eclipse.org/test/concurrency/1.0");
		contentClass = EcoreFactory.eINSTANCE.createEClass();
		contentClass.setName("Content");
		nameAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		nameAttribute.setName("name");
		nameAttribute.setEType(EcorePackage.Literals.ESTRING);
		contentClass.getEStructuralFeatures().add(nameAttribute);
		domainPackage.getEClassifiers().add(contentClass);

		whiteboard = MetadataServices.createWhiteboard();
		writer = EObjectRegistries.createRegistry("concurrency-test");
		bridge = new RegistryMetadataBridge(whiteboard, TYPE_ID, AspectAnchorResolver.contentClass());
		whiteboard.addMetadataHandler(bridge);
		writer.getRegistry().addListener(bridge);
		whiteboard.registerPackage(domainPackage);
		executor = Executors.newFixedThreadPool(2);
	}

	@AfterEach
	public void tearDown() {
		executor.shutdownNow();
	}

	private EObject content(String name) {
		EObject content = EcoreUtil.create(contentClass);
		content.eSet(nameAttribute, name);
		return content;
	}

	@Test
	public void testReadersNeverSeeAConcurrentModification() throws Exception {
		// Foreign aspects make each lookup walk a long list, which widens the window a
		// placement has to hit - added through update, as any non-handler writer must.
		ClassMetadata classMetadata = whiteboard.getClassMetadata(contentClass).orElseThrow();
		whiteboard.update(() -> {
			for (int i = 0; i < 200; i++) {
				AspectEntry foreign = MetadataFactory.eINSTANCE.createAspectEntry();
				foreign.setTypeId("foreign." + i);
				classMetadata.getAspects().add(foreign);
			}
		});

		AtomicBoolean done = new AtomicBoolean();
		CountDownLatch start = new CountDownLatch(1);
		Future<?> writes = executor.submit(() -> {
			start.await();
			try {
				for (int i = 0; i < ROUNDS; i++) {
					writer.put("src", "a", content("v" + i), null);
					if (i % 2 == 0) {
						writer.remove("src", "a");
					}
				}
			} finally {
				done.set(true);
			}
			return null;
		});
		Future<?> reads = executor.submit(() -> {
			start.await();
			while (!done.get()) {
				// a type id nobody carries forces a walk over the whole list
				whiteboard.getClassAspect(contentClass, "absent");
				whiteboard.getClassAspect(contentClass, TYPE_ID);
			}
			return null;
		});
		start.countDown();

		writes.get(60, TimeUnit.SECONDS);
		reads.get(60, TimeUnit.SECONDS);
		assertThat(whiteboard.getClassAspect(contentClass, TYPE_ID)).isPresent();
	}

	@Test
	public void testRegistryCallbacksAndPackageRegistrationDoNotDeadlock() throws Exception {
		// Package registration enters the bridge with the service's write lock held; a
		// registry callback takes the same two locks. Opposite orders would deadlock here.
		List<EPackage> packages = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			EPackage ePackage = EcoreUtil.copy(domainPackage);
			ePackage.setNsURI("http://fennec.eclipse.org/test/concurrency/other/" + i);
			packages.add(ePackage);
		}

		CountDownLatch start = new CountDownLatch(1);
		Future<?> registrations = executor.submit(() -> {
			start.await();
			for (EPackage ePackage : packages) {
				whiteboard.registerPackage(ePackage);
				whiteboard.unregisterPackage(ePackage);
			}
			return null;
		});
		Future<?> writes = executor.submit(() -> {
			start.await();
			for (int i = 0; i < ROUNDS; i++) {
				writer.put("src", "k" + (i % 10), content("v" + i), null);
			}
			return null;
		});
		start.countDown();

		registrations.get(60, TimeUnit.SECONDS);
		writes.get(60, TimeUnit.SECONDS);
		assertThat(whiteboard.getClassAspect(contentClass, TYPE_ID)).isPresent();
	}
}
