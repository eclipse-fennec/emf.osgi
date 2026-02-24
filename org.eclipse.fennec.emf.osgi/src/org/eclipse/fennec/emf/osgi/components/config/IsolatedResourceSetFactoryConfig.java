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
package org.eclipse.fennec.emf.osgi.components.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for creating an isolated ResourceSetFactory with its own
 * dedicated EPackage registry and Resource.Factory registry.
 * <p>
 * This configuration creates three sub-configurations:
 * <ul>
 *   <li>{@code EPackageRegistry} - a dedicated EPackage registry filtered by the model target</li>
 *   <li>{@code ResourceFactoryRegistry} - a dedicated Resource.Factory registry</li>
 *   <li>{@code ResourceSetFactory} - a ResourceSetFactory wired to the above registries</li>
 * </ul>
 * All three are linked by the factory name via target filters.
 */
@ObjectClassDefinition(
		name = "EMF Isolated ResourceSet Factory",
		description = "Creates an isolated ResourceSetFactory with dedicated EPackage and Resource.Factory registries. "
				+ "All three components are linked via the factory name."
)
public @interface IsolatedResourceSetFactoryConfig {

	/**
	 * The name of the isolated resource set factory.
	 * Used to link the created EPackageRegistry, ResourceFactoryRegistry, and ResourceSetFactory
	 * instances together via target filters.
	 * @return the factory name
	 */
	@AttributeDefinition(
			name = "Factory Name",
			description = "Name of the isolated resource set factory. Links the EPackageRegistry, ResourceFactoryRegistry, and ResourceSetFactory together."
	)
	String rsf_name();

	/**
	 * Optional LDAP filter to select which EPackageConfigurator services are tracked
	 * by the isolated EPackage registry. Defaults to matching all models except ecore.
	 * @return the LDAP target filter for model selection
	 */
	@AttributeDefinition(
			name = "Model Target Filter",
			description = "LDAP filter to select which models are tracked by the isolated EPackage registry. "
					+ "Ecore is always excluded automatically. Default: (emf.name=*)",
			required = false
	)
	String rsf_model_target_filter() default "(emf.name=*)";

}
