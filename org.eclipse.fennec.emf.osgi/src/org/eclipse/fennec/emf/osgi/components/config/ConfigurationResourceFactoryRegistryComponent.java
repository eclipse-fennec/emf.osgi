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

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

/**
 * Component for the {@link ResourceFactoryRegistryImpl}
 * @author Mark Hoffmann
 * @since 25.07.2017
 */
@Component(configurationPid=EMFNamespaces.RESOURCE_FACTORY_CONFIG_NAME, service=Resource.Factory.Registry.class, configurationPolicy=ConfigurationPolicy.REQUIRE)
@Designate(ocd = ResourceFactoryRegistryConfig.class, factory = true)
@ProviderType
public class ConfigurationResourceFactoryRegistryComponent extends ResourceFactoryRegistryImpl {

}
