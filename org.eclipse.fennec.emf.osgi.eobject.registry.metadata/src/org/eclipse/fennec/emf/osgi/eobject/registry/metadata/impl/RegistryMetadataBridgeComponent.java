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
package org.eclipse.fennec.emf.osgi.eobject.registry.metadata.impl;

import java.util.Objects;

import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryEntry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryListener;
import org.eclipse.fennec.emf.osgi.eobject.registry.metadata.AspectAnchorResolver;
import org.eclipse.fennec.emf.osgi.eobject.registry.metadata.RegistryMetadataBridge;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.metadata.MetadataService;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

/**
 * OSGi wiring of the {@link RegistryMetadataBridge}: one bridge per factory
 * configuration, registered under both whiteboard faces - as
 * {@link EObjectRegistryListener} it carries the configured
 * {@code emf.eobject.registry.name} (DS propagates the configuration), so the registry
 * component binds and replays it; as {@link MetadataHandler} the metadata whiteboard
 * tracks it for new model versions. Deactivation detaches every contributed aspect.
 * <p>
 * The anchor resolver is mandatory: by default the {@link ContentClassAnchorResolverComponent},
 * otherwise the one {@code anchorResolver.target} selects. A bridge whose resolver is
 * missing stays unsatisfied instead of anchoring with the wrong resolver.
 *
 * @author Data In Motion Consulting
 */
@Component(name = "EObjectRegistryMetadataBridge", configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = RegistryMetadataBridgeConfig.class, factory = true)
public class RegistryMetadataBridgeComponent implements EObjectRegistryListener, MetadataHandler {

	static final String DEFAULT_ANCHOR_RESOLVER = "(" + AspectAnchorResolver.ANCHOR_RESOLVER + "="
			+ AspectAnchorResolver.CONTENT_CLASS + ")";

	private final RegistryMetadataBridge bridge;

	@Activate
	public RegistryMetadataBridgeComponent(@Reference MetadataService metadataService,
			@Reference(name = "anchorResolver", target = DEFAULT_ANCHOR_RESOLVER) AspectAnchorResolver anchorResolver,
			RegistryMetadataBridgeConfig config) {
		Objects.requireNonNull(config, "config");
		if (config.aspect_type_id() == null || config.aspect_type_id().isBlank()) {
			throw new IllegalArgumentException("Bridge configuration requires a non-blank aspect.type.id");
		}
		if (config.emf_eobject_registry_name() == null || config.emf_eobject_registry_name().isBlank()) {
			throw new IllegalArgumentException("Bridge configuration requires a non-blank emf.eobject.registry.name");
		}
		bridge = new RegistryMetadataBridge(metadataService, config.aspect_type_id(), anchorResolver);
	}

	@Deactivate
	public void deactivate() {
		bridge.close();
	}

	@Override
	public void entryAdded(EObjectRegistryEntry entry) {
		bridge.entryAdded(entry);
	}

	@Override
	public void entryUpdated(EObjectRegistryEntry entry, EObjectRegistryEntry oldEntry) {
		bridge.entryUpdated(entry, oldEntry);
	}

	@Override
	public void entryRemoved(EObjectRegistryEntry entry) {
		bridge.entryRemoved(entry);
	}

	@Override
	public void onPackageRegistered(PackageMetadata packageMetadata) {
		bridge.onPackageRegistered(packageMetadata);
	}

	@Override
	public void onPackageUnregistered(PackageMetadata packageMetadata) {
		bridge.onPackageUnregistered(packageMetadata);
	}

	@Override
	public void clear() {
		bridge.clear();
	}
}
