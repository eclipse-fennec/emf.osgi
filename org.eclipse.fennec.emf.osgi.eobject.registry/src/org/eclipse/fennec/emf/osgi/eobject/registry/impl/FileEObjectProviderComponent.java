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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectProvider;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.emf.osgi.eobject.registry.FileEObjectProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

/**
 * OSGi wiring of the {@link FileEObjectProvider}: one provider service per factory
 * configuration, published under {@link EObjectProvider} with the configuration's
 * {@code emf.eobject.provider.name} as service property (DS propagates the
 * configuration automatically).
 * <p>
 * The {@link ResourceSet} comes from the whiteboard-configured
 * {@link ResourceSetFactory} service - never {@code new ResourceSetImpl()} - so files
 * load against the registered models and materialize typed instead of as dynamic
 * EObjects. Every {@link #load(EObjectRegistryWriter)} uses a fresh {@link ResourceSet},
 * so a re-load re-reads the files instead of answering from the resource cache.
 *
 * @author Data In Motion Consulting
 */
@Component(name = "FileEObjectProvider", configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = FileEObjectProviderConfig.class, factory = true)
public class FileEObjectProviderComponent implements EObjectProvider {

	private final ResourceSetFactory resourceSetFactory;
	private final String providerName;
	private final List<Path> locations;
	private final BiFunction<Resource, EObject, String> keyFunction;

	@Activate
	public FileEObjectProviderComponent(@Reference ResourceSetFactory resourceSetFactory,
			FileEObjectProviderConfig config) {
		this.resourceSetFactory = Objects.requireNonNull(resourceSetFactory, "resourceSetFactory");
		Objects.requireNonNull(config, "config");
		if (config.emf_eobject_provider_name() == null || config.emf_eobject_provider_name().isBlank()) {
			throw new IllegalArgumentException("FileEObjectProvider configuration requires a non-blank provider name");
		}
		providerName = config.emf_eobject_provider_name();
		locations = config.locations() == null ? List.of()
				: Arrays.stream(config.locations()).map(Path::of).toList();
		keyFunction = config.key_feature() == null || config.key_feature().isBlank()
				? FileEObjectProvider.uriFragmentKeys()
				: FileEObjectProvider.featureKeys(config.key_feature());
	}

	@Override
	public CompletableFuture<Void> load(EObjectRegistryWriter writer) {
		return new FileEObjectProvider(providerName, resourceSetFactory.createResourceSet(), locations, keyFunction)
				.load(writer);
	}
}
