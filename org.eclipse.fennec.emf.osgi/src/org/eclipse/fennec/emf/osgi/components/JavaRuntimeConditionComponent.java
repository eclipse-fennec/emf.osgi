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
package org.eclipse.fennec.emf.osgi.components;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.condition.Condition;

/**
 * Publishes {@link Condition} services that describe the JDK the framework is
 * running on, so that other components can declaratively require a minimum Java
 * feature version.
 * <p>
 * The {@value #JAVA_24_PLUS_CONDITION_ID} condition is only registered when the
 * runtime is Java 24 or higher. Components that should only exist on such a JDK
 * can therefore declare a mandatory reference with the target filter
 * <code>(osgi.condition.id={@value #JAVA_24_PLUS_CONDITION_ID})</code> and will
 * stay unsatisfied (and hence inactive) on older JDKs.
 * <p>
 * This is used to gate the JAXP processing-limit configuration support, which is
 * only meaningful on JDK 24+ where the relevant {@code jdk.xml.*} defaults were
 * tightened (see <a href="https://bugs.openjdk.org/browse/JDK-8343006">JDK-8343006</a>).
 */
@Component(name = "JavaRuntimeConditionComponent", immediate = true, service = {})
public class JavaRuntimeConditionComponent {

	/** Condition id present only when running on Java 24 or higher. */
	public static final String JAVA_24_PLUS_CONDITION_ID = EMFNamespaces.RUNTIME_JAVA_24_PLUS_CONDITION_ID;

	/** The JDK feature version in which the JAXP processing-limit defaults were tightened. */
	private static final int LIMITS_TIGHTENED_IN = 24;

	private ServiceRegistration<Condition> java24PlusRegistration;

	@Activate
	public JavaRuntimeConditionComponent(BundleContext bundleContext) {
		if (Runtime.version().feature() >= LIMITS_TIGHTENED_IN) {
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put(Condition.CONDITION_ID, JAVA_24_PLUS_CONDITION_ID);
			java24PlusRegistration = bundleContext.registerService(Condition.class, Condition.INSTANCE, properties);
		}
	}

	@Deactivate
	void deactivate() {
		if (java24PlusRegistration != null) {
			java24PlusRegistration.unregister();
			java24PlusRegistration = null;
		}
	}
}
