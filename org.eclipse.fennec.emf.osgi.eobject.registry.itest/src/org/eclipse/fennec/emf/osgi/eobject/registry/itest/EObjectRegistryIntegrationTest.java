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
package org.eclipse.fennec.emf.osgi.eobject.registry.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryConstants;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryEntry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryListener;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.emf.osgi.example.model.basic.BasicFactory;
import org.eclipse.fennec.emf.osgi.example.model.basic.BasicPackage;
import org.eclipse.fennec.emf.osgi.example.model.basic.Person;
import org.eclipse.fennec.emf.osgi.metadata.MetadataService;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;
import org.osgi.util.tracker.ServiceTracker;

/**
 * End-to-end proof in Felix: factory configurations wire file provider, registry and
 * metadata bridge; the registry service appears only after the initial load; listeners
 * get whiteboard replay; instances are isolated by name; the bridge answers from the
 * {@link MetadataService}.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class EObjectRegistryIntegrationTest {

	@InjectBundleContext
	BundleContext ctx;

	@InjectService
	ConfigurationAdmin configurationAdmin;

	@InjectService
	ResourceSetFactory resourceSetFactory;

	private final List<Configuration> configurations = new ArrayList<>();
	private final List<ServiceRegistration<?>> registrations = new ArrayList<>();

	@AfterEach
	public void cleanUp() throws Exception {
		for (ServiceRegistration<?> registration : registrations) {
			try {
				registration.unregister();
			} catch (IllegalStateException e) {
				// already gone
			}
		}
		for (Configuration configuration : configurations) {
			configuration.delete();
		}
		Thread.sleep(200);
	}

	private Path writePersonFixture(Path dir, String fileName, String firstName) throws Exception {
		Files.createDirectories(dir);
		Path file = dir.resolve(fileName);
		Person person = BasicFactory.eINSTANCE.createPerson();
		person.setFirstName(firstName);
		person.setLastName("Tester");
		ResourceSet resourceSet = resourceSetFactory.createResourceSet();
		Resource resource = resourceSet.createResource(URI.createFileURI(file.toAbsolutePath().toString()));
		resource.getContents().add(person);
		resource.save(Map.of());
		return file;
	}

	private void createProviderConfig(String providerName, Path location) throws Exception {
		Configuration configuration = configurationAdmin.createFactoryConfiguration("FileEObjectProvider", "?");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(EObjectRegistryConstants.EMF_EOBJECT_PROVIDER_NAME, providerName);
		props.put("locations", new String[] { location.toAbsolutePath().toString() });
		props.put("key.feature", "firstName");
		configuration.update(props);
		configurations.add(configuration);
	}

	private void createRegistryConfig(String registryName, String providerName) throws Exception {
		Configuration configuration = configurationAdmin.createFactoryConfiguration("EObjectRegistry", "?");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("name", registryName);
		props.put("initialProvider.target",
				"(" + EObjectRegistryConstants.EMF_EOBJECT_PROVIDER_NAME + "=" + providerName + ")");
		configuration.update(props);
		configurations.add(configuration);
	}

	private void createBridgeConfig(String registryName, String typeId) throws Exception {
		Configuration configuration = configurationAdmin
				.createFactoryConfiguration("EObjectRegistryMetadataBridge", "?");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME, registryName);
		props.put("aspect.type.id", typeId);
		configuration.update(props);
		configurations.add(configuration);
	}

	private <T> ServiceTracker<T, T> tracker(Class<T> type, String registryName) throws Exception {
		String filter = String.format("(&(objectClass=%s)(%s=%s))", type.getName(),
				EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME, registryName);
		ServiceTracker<T, T> tracker = new ServiceTracker<>(ctx, ctx.createFilter(filter), null);
		tracker.open();
		return tracker;
	}

	@Test
	public void testRegistryAppearsOnlyAfterTheInitialLoad() throws Exception {
		Path dir = Files.createTempDirectory("eobject-registry-itest");
		writePersonFixture(dir, "persons.basic", "Emil");

		// registry config first - its initial provider does not exist yet
		createRegistryConfig("persons-gating", "persons-gating-files");
		ServiceTracker<EObjectRegistry, EObjectRegistry> registryTracker = tracker(EObjectRegistry.class,
				"persons-gating");
		try {
			assertThat(registryTracker.waitForService(500))
					.as("no registry service while the initial provider is missing").isNull();

			createProviderConfig("persons-gating-files", dir);

			EObjectRegistry registry = registryTracker.waitForService(10_000);
			assertThat(registry).as("registry appears once the provider is bound and loaded").isNotNull();
			assertThat(registry.getName()).isEqualTo("persons-gating");
			assertThat(registry.get("Emil")).isPresent();
			assertThat(registry.getEntry("Emil").orElseThrow().source()).isEqualTo("persons-gating-files");
		} finally {
			registryTracker.close();
		}
	}

	@Test
	public void testWriterPushAndListenerWhiteboard() throws Exception {
		Path dir = Files.createTempDirectory("eobject-registry-itest");
		writePersonFixture(dir, "persons.basic", "Emil");
		createProviderConfig("persons-wb-files", dir);
		createRegistryConfig("persons-wb", "persons-wb-files");

		ServiceTracker<EObjectRegistryWriter, EObjectRegistryWriter> writerTracker = tracker(
				EObjectRegistryWriter.class, "persons-wb");
		try {
			EObjectRegistryWriter writer = writerTracker.waitForService(10_000);
			assertThat(writer).isNotNull();

			List<String> events = new CopyOnWriteArrayList<>();
			EObjectRegistryListener listener = new EObjectRegistryListener() {
				@Override
				public void entryAdded(EObjectRegistryEntry entry) {
					events.add("added:" + entry.key());
				}

				@Override
				public void entryRemoved(EObjectRegistryEntry entry) {
					events.add("removed:" + entry.key());
				}
			};
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME, "persons-wb");
			registrations.add(ctx.registerService(EObjectRegistryListener.class, listener, props));

			awaitTrue(() -> events.contains("added:Emil"), "whiteboard listener gets the replay");

			Person pushed = BasicFactory.eINSTANCE.createPerson();
			pushed.setFirstName("Dynamic");
			writer.put("push-source", "Dynamic", pushed, null);
			awaitTrue(() -> events.contains("added:Dynamic"), "whiteboard listener sees live pushes");
		} finally {
			writerTracker.close();
		}
	}

	@Test
	public void testRegistriesAreIsolatedByName() throws Exception {
		Path dirA = Files.createTempDirectory("eobject-registry-itest-a");
		Path dirB = Files.createTempDirectory("eobject-registry-itest-b");
		writePersonFixture(dirA, "a.basic", "Anna");
		writePersonFixture(dirB, "b.basic", "Ben");
		createProviderConfig("files-a", dirA);
		createProviderConfig("files-b", dirB);
		createRegistryConfig("registry-a", "files-a");
		createRegistryConfig("registry-b", "files-b");

		ServiceTracker<EObjectRegistry, EObjectRegistry> trackerA = tracker(EObjectRegistry.class, "registry-a");
		ServiceTracker<EObjectRegistry, EObjectRegistry> trackerB = tracker(EObjectRegistry.class, "registry-b");
		try {
			EObjectRegistry registryA = trackerA.waitForService(10_000);
			EObjectRegistry registryB = trackerB.waitForService(10_000);
			assertThat(registryA).isNotNull();
			assertThat(registryB).isNotNull();

			assertThat(registryA.get("Anna")).isPresent();
			assertThat(registryA.get("Ben")).isEmpty();
			assertThat(registryB.get("Ben")).isPresent();
			assertThat(registryB.get("Anna")).isEmpty();
		} finally {
			trackerA.close();
			trackerB.close();
		}
	}

	@Test
	public void testBridgeAnswersFromTheMetadataService(@InjectService MetadataService metadataService)
			throws Exception {
		Path dir = Files.createTempDirectory("eobject-registry-itest");
		writePersonFixture(dir, "persons.basic", "Emil");
		createProviderConfig("persons-bridge-files", dir);
		createRegistryConfig("persons-bridge", "persons-bridge-files");
		createBridgeConfig("persons-bridge", "itest.person.aspect");

		awaitTrue(() -> metadataService.getClassAspect(BasicPackage.Literals.PERSON, "itest.person.aspect")
				.isPresent(), "the registry content is answerable from the EClass via the bridge");

		Optional<AspectEntry> aspect = metadataService.getClassAspect(BasicPackage.Literals.PERSON,
				"itest.person.aspect");
		Person snapshot = (Person) aspect.orElseThrow().getContent();
		assertThat(snapshot.getFirstName()).isEqualTo("Emil");
	}

	/**
	 * The fingerprint guard against the fingerprints the running framework really computes
	 * (issue #81): an entry naming the live model version is placed, an entry naming any
	 * other version is not - it stays in the registry, pending, and must not displace the
	 * aspect that legitimately sits on the anchor.
	 */
	@Test
	public void testPinnedEntryReachesOnlyTheLiveModelVersion(@InjectService MetadataService metadataService)
			throws Exception {
		Path dir = Files.createTempDirectory("eobject-registry-itest");
		writePersonFixture(dir, "persons.basic", "Emil");
		createProviderConfig("persons-pinned-files", dir);
		createRegistryConfig("persons-pinned", "persons-pinned-files");
		createBridgeConfig("persons-pinned", "itest.pinned.aspect");

		awaitTrue(() -> metadataService.getClassAspect(BasicPackage.Literals.PERSON, "itest.pinned.aspect").isPresent(),
				"the bridge is up on the authored content");
		String liveFingerprint = metadataService.getPackageMetadata(BasicPackage.eINSTANCE).orElseThrow()
				.getModelFingerprint();

		ServiceTracker<EObjectRegistryWriter, EObjectRegistryWriter> writerTracker = tracker(
				EObjectRegistryWriter.class, "persons-pinned");
		try {
			EObjectRegistryWriter writer = writerTracker.waitForService(10_000);
			assertThat(writer).isNotNull();

			Person derived = BasicFactory.eINSTANCE.createPerson();
			derived.setFirstName("Pinned");
			writer.put("compiler", "Pinned", derived, Map.of(EMFNamespaces.EMF_MODEL_FINGERPRINT, liveFingerprint));
			awaitTrue(() -> "Pinned".equals(aspectFirstName(metadataService, "itest.pinned.aspect")),
					"an entry naming the live version is placed");

			Person foreign = BasicFactory.eINSTANCE.createPerson();
			foreign.setFirstName("ForeignVersion");
			writer.put("compiler", "Foreign", foreign, Map.of(EMFNamespaces.EMF_MODEL_FINGERPRINT,
					"fp1:0000000000000000000000000000000000000000000000000000000000000000"));

			assertThat(writer.getRegistry().get("Foreign")).as("kept in the registry, pending its version").isPresent();
			assertThat(aspectFirstName(metadataService, "itest.pinned.aspect"))
					.as("an entry naming another version never reaches this tree").isEqualTo("Pinned");
		} finally {
			writerTracker.close();
		}
	}

	private String aspectFirstName(MetadataService metadataService, String typeId) {
		return metadataService.getClassAspect(BasicPackage.Literals.PERSON, typeId)
				.map(aspect -> ((Person) aspect.getContent()).getFirstName()).orElse(null);
	}

	private void awaitTrue(BooleanSupplier condition, String description) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			if (condition.getAsBoolean()) {
				return;
			}
			Thread.sleep(100);
		}
		assertThat(condition.getAsBoolean()).as(description).isTrue();
	}
}
