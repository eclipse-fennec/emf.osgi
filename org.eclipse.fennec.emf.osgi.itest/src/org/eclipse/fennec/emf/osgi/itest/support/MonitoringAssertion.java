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
package org.eclipse.fennec.emf.osgi.itest.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Lightweight, dependency-free replacement for the fluent service-event
 * monitoring assertion that was previously provided by
 * {@code org.osgi.test.assertj.monitoring}.
 * <p>
 * It installs a plain OSGi {@link ServiceListener}, runs an action while
 * recording all {@link ServiceEvent}s, waits until the event stream settles and
 * then offers assertions over the observed events. Only the subset of the
 * original API that is used by the integration tests is implemented.
 * <p>
 * Typical usage:
 *
 * <pre>
 * MonitoringAssertion.executeAndObserve(() -&gt; {
 *     // trigger service (un)registrations
 * }).untilNoMoreServiceEventWithin(100).assertWithTimeoutThat(1000)
 *         .hasExactlyOneServiceEventRegisteredWith(MyService.class);
 * </pre>
 */
public class MonitoringAssertion {

	/** Poll interval while waiting for the event stream to settle. */
	private static final long POLL_INTERVAL_MS = 20;

	private final BundleContext context;
	private final List<Recorded> events = new CopyOnWriteArrayList<>();
	private final ServiceListener listener = this::onServiceEvent;

	/** Point in time from which the settle-quiet-period is measured. */
	private volatile long observationStart;

	/** Quiet period (ms) with no relevant event that marks the stream settled. */
	private int quietMillis;
	/**
	 * When set, only {@code MODIFIED} events for this type reset the quiet
	 * period; otherwise every service event does.
	 */
	private Class<?> settleModifiedType;

	private boolean timedOut;

	private MonitoringAssertion() {
		this.context = FrameworkUtil.getBundle(MonitoringAssertion.class).getBundleContext();
	}

	/**
	 * An action that is observed for service events and may throw a checked
	 * exception (e.g. {@code IOException} from configuration updates).
	 */
	@FunctionalInterface
	public interface ObservedAction {
		void run() throws Exception;
	}

	/**
	 * Runs the given action while observing all service events.
	 *
	 * @param action the action to execute; must not be {@code null}
	 * @return the monitoring assertion for further configuration
	 */
	public static MonitoringAssertion executeAndObserve(ObservedAction action) {
		Objects.requireNonNull(action, "action");
		MonitoringAssertion monitor = new MonitoringAssertion();
		monitor.context.addServiceListener(monitor.listener);
		monitor.observationStart = System.currentTimeMillis();
		try {
			action.run();
		} catch (RuntimeException e) {
			monitor.context.removeServiceListener(monitor.listener);
			throw e;
		} catch (Exception e) {
			monitor.context.removeServiceListener(monitor.listener);
			throw new IllegalStateException("Observed action failed", e);
		}
		// Keep listening; the stream is settled and the listener removed during
		// assertWithTimeoutThat(int).
		return monitor;
	}

	/**
	 * Marks the stream settled once no service event of any kind was observed for
	 * the given quiet period.
	 *
	 * @param quietMillis the quiet period in milliseconds
	 * @return this
	 */
	public MonitoringAssertion untilNoMoreServiceEventWithin(int quietMillis) {
		this.quietMillis = quietMillis;
		this.settleModifiedType = null;
		return this;
	}

	/**
	 * Marks the stream settled once no {@code MODIFIED} event for the given type
	 * was observed for the given quiet period.
	 *
	 * @param quietMillis the quiet period in milliseconds
	 * @param type        the service type whose modification events are awaited
	 * @return this
	 */
	public MonitoringAssertion untilNoMoreServiceEventModifiedWithin(int quietMillis, Class<?> type) {
		this.quietMillis = quietMillis;
		this.settleModifiedType = Objects.requireNonNull(type, "type");
		return this;
	}

	/**
	 * Waits up to the given timeout for the event stream to settle (see the
	 * {@code untilNoMore...} methods), then stops observing and hands over to the
	 * assertions. If the stream never settles within the timeout,
	 * {@link #isNotTimedOut()} will fail.
	 *
	 * @param timeoutMillis the overall observation timeout in milliseconds
	 * @return this
	 */
	public MonitoringAssertion assertWithTimeoutThat(int timeoutMillis) {
		long deadline = observationStart + timeoutMillis;
		try {
			while (true) {
				long now = System.currentTimeMillis();
				if (now - lastRelevantEventTime() >= quietMillis) {
					timedOut = false;
					break;
				}
				if (now >= deadline) {
					timedOut = true;
					break;
				}
				try {
					Thread.sleep(POLL_INTERVAL_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					timedOut = true;
					break;
				}
			}
		} finally {
			context.removeServiceListener(listener);
		}
		return this;
	}

	/**
	 * Asserts that the event stream settled within the configured timeout.
	 *
	 * @return this
	 */
	public MonitoringAssertion isNotTimedOut() {
		assertFalse(timedOut, "Observation timed out before the service events settled");
		return this;
	}

	/**
	 * Asserts that exactly one {@code REGISTERED} event for the given type was
	 * observed.
	 *
	 * @param type the service type
	 * @return this
	 */
	public MonitoringAssertion hasExactlyOneServiceEventRegisteredWith(Class<?> type) {
		long count = count(type, ServiceEvent.REGISTERED);
		assertEquals(1L, count, () -> "Expected exactly one REGISTERED service event for " + type.getName()
				+ " but observed " + count);
		return this;
	}

	/**
	 * Asserts that exactly one {@code UNREGISTERING} event for the given type was
	 * observed.
	 *
	 * @param type the service type
	 * @return this
	 */
	public MonitoringAssertion hasExactlyOneServiceEventUnregisteringWith(Class<?> type) {
		long count = count(type, ServiceEvent.UNREGISTERING);
		assertEquals(1L, count, () -> "Expected exactly one UNREGISTERING service event for " + type.getName()
				+ " but observed " + count);
		return this;
	}

	/**
	 * Asserts that at least one {@code MODIFIED} (or {@code MODIFIED_ENDMATCH})
	 * event for the given type was observed.
	 *
	 * @param type the service type
	 * @return this
	 */
	public MonitoringAssertion hasAtLeastOneServiceEventModifiedWith(Class<?> type) {
		long count = count(type, ServiceEvent.MODIFIED, ServiceEvent.MODIFIED_ENDMATCH);
		assertTrue(count >= 1L, () -> "Expected at least one MODIFIED service event for " + type.getName()
				+ " but observed none");
		return this;
	}

	private void onServiceEvent(ServiceEvent event) {
		events.add(new Recorded(event, System.currentTimeMillis()));
	}

	/**
	 * @return the timestamp of the most recent event relevant to the configured
	 *         settle strategy, or {@link #observationStart} if none was observed
	 */
	private long lastRelevantEventTime() {
		long last = observationStart;
		for (Recorded recorded : events) {
			if (isRelevantForSettle(recorded)) {
				last = Math.max(last, recorded.time);
			}
		}
		return last;
	}

	private boolean isRelevantForSettle(Recorded recorded) {
		if (settleModifiedType == null) {
			return true;
		}
		return recorded.event.getType() == ServiceEvent.MODIFIED && matches(recorded.event, settleModifiedType);
	}

	private long count(Class<?> type, int... eventTypes) {
		long count = 0;
		for (Recorded recorded : events) {
			if (matches(recorded.event, type) && isOneOf(recorded.event.getType(), eventTypes)) {
				count++;
			}
		}
		return count;
	}

	private static boolean isOneOf(int value, int... candidates) {
		for (int candidate : candidates) {
			if (value == candidate) {
				return true;
			}
		}
		return false;
	}

	private static boolean matches(ServiceEvent event, Class<?> type) {
		ServiceReference<?> reference = event.getServiceReference();
		if (reference == null) {
			return false;
		}
		Object objectClass = reference.getProperty(Constants.OBJECTCLASS);
		if (objectClass instanceof String[]) {
			for (String name : (String[]) objectClass) {
				if (type.getName().equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	/** A recorded service event together with the time it was observed. */
	private static final class Recorded {
		final ServiceEvent event;
		final long time;

		Recorded(ServiceEvent event, long time) {
			this.event = event;
			this.time = time;
		}
	}
}
