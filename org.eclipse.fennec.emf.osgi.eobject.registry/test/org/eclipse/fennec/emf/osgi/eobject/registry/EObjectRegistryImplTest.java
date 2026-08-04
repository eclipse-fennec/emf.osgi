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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.fennec.emf.osgi.eobject.registry.impl.EObjectRegistryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The registry core: lookup, per-source swap semantics, listener contract. These
 * semantics carry the resilience story of the whole design - they were previously only
 * covered indirectly through the sensinact Felix IT.
 */
public class EObjectRegistryImplTest {

	private EObjectRegistryImpl registry;
	private RecordingListener listener;

	private EObject objectA;
	private EObject objectB;
	private EObject objectC;

	@BeforeEach
	public void setUp() {
		registry = new EObjectRegistryImpl("test-registry");
		listener = new RecordingListener();
		objectA = EcoreFactory.eINSTANCE.createEClass();
		objectB = EcoreFactory.eINSTANCE.createEClass();
		objectC = EcoreFactory.eINSTANCE.createEClass();
	}

	@Nested
	class ReadFace {

		@Test
		public void testNameIsExposed() {
			assertThat(registry.getName()).isEqualTo("test-registry");
			assertThat(registry.getRegistry()).isSameAs(registry);
		}

		@Test
		public void testEmptyRegistryAnswersEmpty() {
			assertThat(registry.get("unknown")).isEmpty();
			assertThat(registry.getEntry("unknown")).isEmpty();
			assertThat(registry.entries()).isEmpty();
		}

		@Test
		public void testNullKeyLookupsAnswerEmpty() {
			assertThat(registry.get(null)).isEmpty();
			assertThat(registry.getEntry(null)).isEmpty();
		}

		@Test
		public void testLookupAfterPut() {
			registry.put("src", "a", objectA, Map.of("emf.nsURI", "http://example/1.0"));

			assertThat(registry.get("a")).contains(objectA);
			EObjectRegistryEntry entry = registry.getEntry("a").orElseThrow();
			assertThat(entry.key()).isEqualTo("a");
			assertThat(entry.source()).isEqualTo("src");
			assertThat(entry.properties()).containsEntry("emf.nsURI", "http://example/1.0");
		}

		@Test
		public void testEntriesSnapshotKeepsInsertionOrderAcrossUpdates() {
			registry.put("src", "a", objectA, null);
			registry.put("src", "b", objectB, null);
			registry.put("src", "a", objectC, null);

			assertThat(registry.entries()).extracting(EObjectRegistryEntry::key).containsExactly("a", "b");
		}

		@Test
		public void testEntriesSnapshotIsDetached() {
			registry.put("src", "a", objectA, null);
			var snapshot = registry.entries();
			registry.put("src", "b", objectB, null);

			assertThat(snapshot).hasSize(1);
		}
	}

	@Nested
	class PutAndRemove {

		@Test
		public void testPutValidatesArguments() {
			assertThatNullPointerException().isThrownBy(() -> registry.put(null, "a", objectA, null));
			assertThatNullPointerException().isThrownBy(() -> registry.put("src", null, objectA, null));
			assertThatNullPointerException().isThrownBy(() -> registry.put("src", "a", null, null));
			assertThatNullPointerException().isThrownBy(() -> registry.remove(null, "a"));
			assertThatNullPointerException().isThrownBy(() -> registry.remove("src", null));
		}

		@Test
		public void testPutFiresAddedThenUpdated() {
			registry.addListener(listener);
			registry.put("src", "a", objectA, null);
			registry.put("src", "a", objectB, null);

			assertThat(listener.events).containsExactly("added:a", "updated:a");
			assertThat(listener.updated.get(0).object()).isSameAs(objectB);
			assertThat(listener.updatedOld.get(0).object()).isSameAs(objectA);
		}

		@Test
		public void testIdenticalPutIsSilentNoOp() {
			registry.put("src", "a", objectA, Map.of("p", "v"));
			registry.addListener(listener);
			listener.events.clear();

			registry.put("src", "a", objectA, Map.of("p", "v"));

			assertThat(listener.events).isEmpty();
		}

		@Test
		public void testSameObjectWithChangedPropertiesIsAnUpdate() {
			registry.put("src", "a", objectA, Map.of("p", "v1"));
			registry.addListener(listener);
			listener.events.clear();

			registry.put("src", "a", objectA, Map.of("p", "v2"));

			assertThat(listener.events).containsExactly("updated:a");
			assertThat(registry.getEntry("a").orElseThrow().properties()).containsEntry("p", "v2");
		}

		@Test
		public void testCrossSourcePutAdoptsTheNewSource() {
			registry.put("file", "a", objectA, null);
			registry.put("atlas", "a", objectB, null);

			EObjectRegistryEntry entry = registry.getEntry("a").orElseThrow();
			assertThat(entry.source()).isEqualTo("atlas");
			assertThat(entry.object()).isSameAs(objectB);
		}

		@Test
		public void testRemoveOwnEntry() {
			registry.put("src", "a", objectA, null);
			registry.addListener(listener);
			listener.events.clear();

			registry.remove("src", "a");

			assertThat(registry.get("a")).isEmpty();
			assertThat(listener.events).containsExactly("removed:a");
		}

		@Test
		public void testRemoveForeignEntryIsIgnored() {
			registry.put("file", "a", objectA, null);
			registry.addListener(listener);
			listener.events.clear();

			registry.remove("atlas", "a");

			assertThat(registry.get("a")).contains(objectA);
			assertThat(listener.events).isEmpty();
		}

		@Test
		public void testRemoveUnknownKeyIsNoOp() {
			registry.addListener(listener);
			registry.remove("src", "unknown");
			assertThat(listener.events).isEmpty();
		}
	}

	@Nested
	class Sync {

		@Test
		public void testSyncValidatesArguments() {
			assertThatNullPointerException().isThrownBy(() -> registry.sync(null, List.of()));
			assertThatNullPointerException().isThrownBy(() -> registry.sync("src", null));
		}

		@Test
		public void testSyncRejectsForeignSourceEntriesUntouched() {
			registry.put("src", "existing", objectA, null);

			assertThatIllegalArgumentException().isThrownBy(() -> registry.sync("src",
					List.of(EObjectRegistryEntry.of("a", objectA, "src"),
							EObjectRegistryEntry.of("b", objectB, "other"))));

			// validation happens before any mutation
			assertThat(registry.entries()).hasSize(1);
			assertThat(registry.get("a")).isEmpty();
		}

		@Test
		public void testInitialSyncAddsEverything() {
			registry.addListener(listener);
			registry.sync("src", List.of(EObjectRegistryEntry.of("a", objectA, "src"),
					EObjectRegistryEntry.of("b", objectB, "src")));

			assertThat(listener.events).containsExactly("added:a", "added:b");
			assertThat(registry.entries()).hasSize(2);
		}

		@Test
		public void testResyncWithIdenticalInstancesIsEventSilent() {
			List<EObjectRegistryEntry> state = List.of(EObjectRegistryEntry.of("a", objectA, "src"),
					EObjectRegistryEntry.of("b", objectB, "src"));
			registry.sync("src", state);
			registry.addListener(listener);
			listener.events.clear();

			registry.sync("src", state);

			assertThat(listener.events).isEmpty();
		}

		@Test
		public void testChangedInstanceIsAnUpdateGoneKeyIsRemoved() {
			registry.sync("src", List.of(EObjectRegistryEntry.of("a", objectA, "src"),
					EObjectRegistryEntry.of("b", objectB, "src")));
			registry.addListener(listener);
			listener.events.clear();

			registry.sync("src", List.of(EObjectRegistryEntry.of("a", objectC, "src")));

			assertThat(listener.events).containsExactly("updated:a", "removed:b");
			assertThat(registry.get("a")).contains(objectC);
			assertThat(registry.get("b")).isEmpty();
		}

		@Test
		public void testUpdatesAreDeliveredBeforeRemovals() {
			registry.sync("src", List.of(EObjectRegistryEntry.of("gone", objectA, "src")));
			registry.addListener(listener);
			listener.events.clear();

			registry.sync("src", List.of(EObjectRegistryEntry.of("new", objectB, "src")));

			assertThat(listener.events).containsExactly("added:new", "removed:gone");
		}

		@Test
		public void testSyncNeverTouchesOtherSources() {
			registry.put("file", "file-entry", objectA, null);
			registry.sync("atlas", List.of(EObjectRegistryEntry.of("atlas-entry", objectB, "atlas")));
			registry.addListener(listener);
			listener.events.clear();

			// atlas now reports empty state - only its own entry disappears
			registry.sync("atlas", List.of());

			assertThat(listener.events).containsExactly("removed:atlas-entry");
			assertThat(registry.get("file-entry")).contains(objectA);
		}

		@Test
		public void testEmptySyncOnEmptyRegistryIsValid() {
			registry.addListener(listener);
			registry.sync("src", List.of());
			assertThat(listener.events).isEmpty();
		}

		@Test
		public void testKeyTakenOverByAnotherSourceIsNoLongerRemovedByTheOldOne() {
			registry.sync("file", List.of(EObjectRegistryEntry.of("a", objectA, "file")));
			registry.put("atlas", "a", objectB, null);
			registry.addListener(listener);
			listener.events.clear();

			// file syncs empty - but "a" is owned by atlas now
			registry.sync("file", List.of());

			assertThat(listener.events).isEmpty();
			assertThat(registry.get("a")).contains(objectB);
		}
	}

	@Nested
	class Listeners {

		@Test
		public void testNullListenerIsRejected() {
			assertThatNullPointerException().isThrownBy(() -> registry.addListener(null));
			assertThatNullPointerException().isThrownBy(() -> registry.removeListener(null));
		}

		@Test
		public void testLateListenerGetsReplayInInsertionOrder() {
			registry.put("src", "a", objectA, null);
			registry.put("src", "b", objectB, null);

			registry.addListener(listener);

			assertThat(listener.events).containsExactly("added:a", "added:b");
		}

		@Test
		public void testReplayThenLiveEventsWithoutDuplicates() {
			registry.put("src", "a", objectA, null);
			registry.addListener(listener);
			registry.put("src", "b", objectB, null);

			assertThat(listener.events).containsExactly("added:a", "added:b");
		}

		@Test
		public void testDoubleAddDoesNotDoubleDeliver() {
			registry.put("src", "a", objectA, null);
			registry.addListener(listener);
			registry.addListener(listener);
			registry.put("src", "b", objectB, null);

			assertThat(listener.events).containsExactly("added:a", "added:b");
		}

		@Test
		public void testRemovedListenerReceivesNothing() {
			registry.addListener(listener);
			registry.removeListener(listener);
			registry.put("src", "a", objectA, null);

			assertThat(listener.events).isEmpty();
		}

		@Test
		public void testBrokenListenerDoesNotBreakOthersOrTheWriter() {
			EObjectRegistryListener broken = new EObjectRegistryListener() {
				@Override
				public void entryAdded(EObjectRegistryEntry entry) {
					throw new IllegalStateException("boom");
				}
			};
			registry.addListener(broken);
			registry.addListener(listener);

			registry.put("src", "a", objectA, null);

			assertThat(listener.events).containsExactly("added:a");
			assertThat(registry.get("a")).contains(objectA);
		}

		@Test
		public void testListenerMayRemoveItselfDuringCallback() {
			EObjectRegistryListener selfRemoving = new EObjectRegistryListener() {
				@Override
				public void entryAdded(EObjectRegistryEntry entry) {
					registry.removeListener(this);
				}
			};
			registry.addListener(selfRemoving);
			registry.addListener(listener);

			registry.put("src", "a", objectA, null);
			registry.put("src", "b", objectB, null);

			assertThat(listener.events).containsExactly("added:a", "added:b");
		}
	}
}
