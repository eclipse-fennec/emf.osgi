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
package org.eclipse.fennec.emf.osgi.annotation.provide;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Capability;

/**
 * Marker annotation that the bundle has the capability to provide a certain
 * model and allows the ecore editor to find this in a repository.
 * 
 * @author Juergen Albert
 * @since 9 Feb 2018
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.PACKAGE })
@Capability(namespace = EPackage.NAMESPACE,
	attribute = {
		"class=\"${#value}\"", //
		"uri=${#uri}",
		"genModel=${#genModel}",
		"genModelSourceLocations:List<String>=\"${#genModelSourceLocations}\"",
		"ecore=${#ecore}",
		"ecoreSourceLocations:List<String>=\"${#ecoreSourceLocations}\"",
		"${if;${#fingerprint};emf.fingerprint=${#fingerprint}}"
	})
public @interface EPackage {

	/** ORG_ECLIPSE_EMF_ECORE_GENERATED_PACKAGE */
	public static final String NAMESPACE = "org.eclipse.emf.ecore.generated_package";

	/** The capability attribute carrying the model fingerprint. */
	public static final String FINGERPRINT_ATTRIBUTE = "emf.fingerprint";

	String uri();

	Class<?> value() default Target.class;

	String genModel() default "";

	String[] genModelSourceLocations() default "";

	String ecore() default "";

	String[] ecoreSourceLocations() default "";

	/**
	 * The fingerprint of the model version this bundle provides, in the current
	 * canonicalisation scheme (e.g. {@code fp1:…}), as it is also advertised in the
	 * {@code emf.fingerprint} service property. Emitted as a capability attribute so the
	 * model version can be matched at resolve time and read from the JAR without a running
	 * framework - see decision M13c in {@code docs/metadata-migration.md}.
	 * <p>
	 * Empty when the provider cannot state it reliably; consumers must treat an absent
	 * fingerprint as unknown, never as a mismatch.
	 */
	String fingerprint() default "";
}
