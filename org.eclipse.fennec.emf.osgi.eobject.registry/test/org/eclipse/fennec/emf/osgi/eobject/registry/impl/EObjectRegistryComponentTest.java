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
package org.eclipse.fennec.emf.osgi.eobject.registry.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectProvider;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryConstants;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryEntry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.emf.osgi.eobject.registry.RecordingListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The gated publication contract of the factory component: the registry services
 * appear only after the initial provider's future completed successfully, never on a
 * deactivated component, and the listener whiteboard matches by registry name.
 */
public class EObjectRegistryComponentTest {

	private final BundleContext ctx = mock(BundleContext.class);
	@SuppressWarnings("rawtypes")
	private final ServiceRegistration registration = mock(ServiceRegistration.class);
	private final EObject object = EcoreFactory.eINSTANCE.createEClass();

	private EObjectRegistryComponent component;

	private EObjectRegistryConfig config(String name, String... contentTypes) {
		return new EObjectRegistryConfig() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return EObjectRegistryConfig.class;
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public String[] content_types() {
				return contentTypes;
			}
		};
	}

	private EObjectProvider providerWriting(String key) {
		return writer -> {
			writer.put("init", key, object, null);
			return CompletableFuture.completedFuture(null);
		};
	}

	@SuppressWarnings("unchecked")
	private void mockRegisterService() {
		when(ctx.registerService(any(String[].class), any(), any(Dictionary.class))).thenReturn(registration);
	}

	@AfterEach
	public void tearDown() {
		if (component != null) {
			component.deactivate();
		}
	}

	@Test
	public void testBlankNameIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new EObjectRegistryComponent(ctx, providerWriting("a"), config(" ")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPublishesBothFacesAfterSuccessfulLoad() {
		mockRegisterService();
		component = new EObjectRegistryComponent(ctx, providerWriting("a"), config("reg-a", "http://example/1.0"));

		ArgumentCaptor<String[]> classes = ArgumentCaptor.forClass(String[].class);
		ArgumentCaptor<Object> service = ArgumentCaptor.forClass(Object.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<Dictionary> props = ArgumentCaptor.forClass(Dictionary.class);
		verify(ctx, timeout(5000)).registerService(classes.capture(), service.capture(), props.capture());

		assertThat(classes.getValue()).containsExactlyInAnyOrder(EObjectRegistry.class.getName(),
				EObjectRegistryWriter.class.getName());
		assertThat(props.getValue().get(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME)).isEqualTo("reg-a");
		assertThat((String[]) props.getValue().get(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_CONTENT_TYPES))
				.containsExactly("http://example/1.0");

		EObjectRegistry registry = ((EObjectRegistryWriter) service.getValue()).getRegistry();
		assertThat(registry.getName()).isEqualTo("reg-a");
		assertThat(registry.get("a")).contains(object);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPublicationWaitsForTheProviderFuture() {
		mockRegisterService();
		CompletableFuture<Void> gate = new CompletableFuture<>();
		EObjectProvider provider = writer -> {
			writer.put("init", "a", object, null);
			return gate;
		};
		component = new EObjectRegistryComponent(ctx, provider, config("reg-a"));

		verify(ctx, after(300).never()).registerService(any(String[].class), any(), any(Dictionary.class));

		gate.complete(null);
		verify(ctx, timeout(5000)).registerService(any(String[].class), any(), any(Dictionary.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFailedLoadStaysUnpublished() {
		EObjectProvider provider = writer -> CompletableFuture.failedFuture(new IllegalStateException("no content"));
		component = new EObjectRegistryComponent(ctx, provider, config("reg-a"));

		verify(ctx, after(500).never()).registerService(any(String[].class), any(), any(Dictionary.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testProviderThrowingDirectlyStaysUnpublished() {
		EObjectProvider provider = writer -> {
			throw new IllegalStateException("boom");
		};
		component = new EObjectRegistryComponent(ctx, provider, config("reg-a"));

		verify(ctx, after(500).never()).registerService(any(String[].class), any(), any(Dictionary.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeactivateBeforeCompletionSuppressesPublication() {
		mockRegisterService();
		CompletableFuture<Void> gate = new CompletableFuture<>();
		component = new EObjectRegistryComponent(ctx, writer -> gate, config("reg-a"));

		component.deactivate();
		component = null;
		gate.complete(null);

		verify(ctx, after(500).never()).registerService(any(String[].class), any(), any(Dictionary.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeactivateUnregistersThePublishedService() {
		mockRegisterService();
		component = new EObjectRegistryComponent(ctx, providerWriting("a"), config("reg-a"));
		verify(ctx, timeout(5000)).registerService(any(String[].class), any(), any(Dictionary.class));

		component.deactivate();
		component = null;

		verify(registration).unregister();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testListenerWhiteboardMatchesByName() {
		mockRegisterService();
		component = new EObjectRegistryComponent(ctx, providerWriting("a"), config("reg-a"));
		ArgumentCaptor<Object> service = ArgumentCaptor.forClass(Object.class);
		verify(ctx, timeout(5000)).registerService(any(String[].class), service.capture(), any(Dictionary.class));

		RecordingListener matching = new RecordingListener();
		RecordingListener foreign = new RecordingListener();
		RecordingListener multiValue = new RecordingListener();
		component.addListener(matching, Map.of(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME, "reg-a"));
		component.addListener(foreign, Map.of(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME, "reg-b"));
		component.addListener(multiValue,
				Map.of(EObjectRegistryConstants.EMF_EOBJECT_REGISTRY_NAME, new String[] { "reg-b", "reg-a" }));

		assertThat(matching.events).containsExactly("added:a");
		assertThat(multiValue.events).containsExactly("added:a");
		assertThat(foreign.events).isEmpty();

		EObjectRegistryWriter writer = (EObjectRegistryWriter) service.getValue();
		writer.sync("dyn", List.of(EObjectRegistryEntry.of("b", object, "dyn")));
		assertThat(matching.events).containsExactly("added:a", "added:b");
		assertThat(foreign.events).isEmpty();

		component.removeListener(matching);
		writer.put("dyn", "c", object, null);
		assertThat(matching.events).containsExactly("added:a", "added:b");
		assertThat(multiValue.events).containsExactly("added:a", "added:b", "added:c");
	}

	@Test
	public void testListenerWithoutNamePropertyIsIgnored() {
		mockRegisterService();
		component = new EObjectRegistryComponent(ctx, providerWriting("a"), config("reg-a"));

		RecordingListener listener = new RecordingListener();
		component.addListener(listener, Map.of());

		assertThat(listener.events).isEmpty();
	}
}
