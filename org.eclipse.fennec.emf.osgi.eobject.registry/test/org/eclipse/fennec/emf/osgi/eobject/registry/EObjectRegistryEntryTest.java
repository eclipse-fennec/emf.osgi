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
package org.eclipse.fennec.emf.osgi.eobject.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;

public class EObjectRegistryEntryTest {

	private final EObject object = EcoreFactory.eINSTANCE.createEClass();

	@Test
	public void testNullArgumentsAreRejected() {
		assertThatNullPointerException().isThrownBy(() -> new EObjectRegistryEntry(null, object, "src", Map.of()));
		assertThatNullPointerException().isThrownBy(() -> new EObjectRegistryEntry("key", null, "src", Map.of()));
		assertThatNullPointerException().isThrownBy(() -> new EObjectRegistryEntry("key", object, null, Map.of()));
	}

	@Test
	public void testNullPropertiesBecomeEmpty() {
		EObjectRegistryEntry entry = new EObjectRegistryEntry("key", object, "src", null);
		assertThat(entry.properties()).isNotNull().isEmpty();
	}

	@Test
	public void testPropertiesAreCopiedAndImmutable() {
		Map<String, Object> props = new HashMap<>();
		props.put("emf.nsURI", "http://example/1.0");
		EObjectRegistryEntry entry = new EObjectRegistryEntry("key", object, "src", props);

		props.put("late", "mutation");
		assertThat(entry.properties()).containsOnlyKeys("emf.nsURI");
		assertThatThrownBy(() -> entry.properties().put("x", "y"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void testOfCreatesEntryWithoutProperties() {
		EObjectRegistryEntry entry = EObjectRegistryEntry.of("key", object, "src");
		assertThat(entry.key()).isEqualTo("key");
		assertThat(entry.object()).isSameAs(object);
		assertThat(entry.source()).isEqualTo("src");
		assertThat(entry.properties()).isEmpty();
	}
}
