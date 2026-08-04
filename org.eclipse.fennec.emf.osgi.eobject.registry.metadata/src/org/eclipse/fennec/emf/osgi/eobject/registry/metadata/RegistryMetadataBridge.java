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
package org.eclipse.fennec.emf.osgi.eobject.registry.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryEntry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryListener;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.metadata.MetadataService;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;

/**
 * Makes the metadata service the uniform, model-anchored read surface for registry
 * content: runtime code holding an {@link EClass} asks
 * {@code MetadataService.getClassAspect(eClass, typeId)} and finds the content of a
 * named EObject registry as an {@link AspectEntry} - one lookup face for mappings, OCL
 * libraries and codec aspects alike.
 * <p>
 * The bridge is <b>both</b> an {@link EObjectRegistryListener} (content changes flow
 * onto the trees of every live model version) and a {@link MetadataHandler} (a newly
 * registered model version - a new fingerprint tree - gets the current content
 * re-contributed; without this, a re-registered or bumped package would silently lose
 * its aspects, because metadata identity is the fingerprint, not the nsURI).
 * <p>
 * <b>Boundaries.</b> The aspect content is a <em>copy</em> of the registry object -
 * {@code AspectEntry#content} is a containment slot, and stealing the live object out
 * of its resource is not an option. The metadata face is therefore snapshot lookup:
 * aspects emit no change events and do not update the metadata index; consumers
 * needing dynamics or id-based lookups ({@code getProfile(id)}-style) use the registry
 * itself. Anchor resolution is domain-specific and pluggable via
 * {@link AspectAnchorResolver}; one anchor class carries at most one aspect per type id
 * (last write wins, logged), so domains with many entries per anchor keep the registry
 * as their query face.
 *
 * @author Data In Motion Consulting
 */
public class RegistryMetadataBridge implements EObjectRegistryListener, MetadataHandler, AutoCloseable {

	private static final Logger logger = Logger.getLogger(RegistryMetadataBridge.class.getName());

	private final MetadataService metadataService;
	private final String aspectTypeId;
	private final AspectAnchorResolver anchorResolver;
	private final Map<String, EObjectRegistryEntry> mirror = new LinkedHashMap<>();
	private final Map<String, List<AspectEntry>> placed = new HashMap<>();
	private final Object lock = new Object();

	/**
	 * Creates a bridge.
	 *
	 * @param metadataService the metadata service to attach to; must not be {@code null}
	 * @param aspectTypeId    the aspect type id this bridge owns; must not be
	 *                        {@code null}
	 * @param anchorResolver  the domain's anchor resolution; must not be {@code null},
	 *                        see {@link AspectAnchorResolver#contentClass()}
	 */
	public RegistryMetadataBridge(MetadataService metadataService, String aspectTypeId,
			AspectAnchorResolver anchorResolver) {
		this.metadataService = Objects.requireNonNull(metadataService, "metadataService");
		this.aspectTypeId = Objects.requireNonNull(aspectTypeId, "aspectTypeId");
		this.anchorResolver = Objects.requireNonNull(anchorResolver, "anchorResolver");
	}

	@Override
	public void entryAdded(EObjectRegistryEntry entry) {
		synchronized (lock) {
			mirror.put(entry.key(), entry);
			attach(entry);
		}
	}

	@Override
	public void entryUpdated(EObjectRegistryEntry entry, EObjectRegistryEntry oldEntry) {
		synchronized (lock) {
			mirror.put(entry.key(), entry);
			detach(entry.key());
			attach(entry);
		}
	}

	@Override
	public void entryRemoved(EObjectRegistryEntry entry) {
		synchronized (lock) {
			mirror.remove(entry.key());
			detach(entry.key());
		}
	}

	/**
	 * A new model version's tree is being built: re-contribute the current content
	 * whose anchors live in that package. This is what keeps aspects alive across model
	 * version bumps and late-starting model bundles.
	 */
	@Override
	public void onPackageRegistered(PackageMetadata packageMetadata) {
		synchronized (lock) {
			for (EObjectRegistryEntry entry : mirror.values()) {
				for (EClass anchor : anchorResolver.anchorsOf(entry)) {
					if (Objects.equals(nsUriOf(anchor), packageMetadata.getNsURI())) {
						findClass(packageMetadata, anchor.getName()).ifPresent(cm -> place(entry, cm));
					}
				}
			}
		}
	}

	@Override
	public void onPackageUnregistered(PackageMetadata packageMetadata) {
		synchronized (lock) {
			// the tree dies with its aspects - just forget the placements that live in it
			placed.values()
					.forEach(list -> list.removeIf(aspect -> EcoreUtil.getRootContainer(aspect) == packageMetadata));
		}
	}

	@Override
	public void clear() {
		synchronized (lock) {
			List.copyOf(placed.keySet()).forEach(this::detach);
			mirror.clear();
		}
	}

	/** Detaches every contributed aspect and drops all state. */
	@Override
	public void close() {
		clear();
	}

	private void attach(EObjectRegistryEntry entry) {
		for (EClass anchor : anchorResolver.anchorsOf(entry)) {
			String nsUri = nsUriOf(anchor);
			if (nsUri == null) {
				continue;
			}
			// every live version tree of the anchor's package gets the aspect - lookups
			// are per fingerprint, and content anchored by class name spans versions
			for (PackageMetadata packageMetadata : metadataService.getPackageMetadataVersions(nsUri)) {
				findClass(packageMetadata, anchor.getName()).ifPresent(cm -> place(entry, cm));
			}
		}
	}

	private void place(EObjectRegistryEntry entry, ClassMetadata classMetadata) {
		List<AspectEntry> stale = classMetadata.getAspects().stream()
				.filter(aspect -> aspectTypeId.equals(aspect.getTypeId())).toList();
		if (!stale.isEmpty()) {
			logger.warning(() -> String.format(
					"Aspect %s on %s is replaced by registry entry %s (one aspect per anchor and type id - last write wins)",
					aspectTypeId, classMetadata.getName(), entry.key()));
			stale.forEach(EcoreUtil::remove);
		}
		AspectEntry aspect = MetadataFactory.eINSTANCE.createAspectEntry();
		aspect.setTypeId(aspectTypeId);
		// a copy: AspectEntry#content is containment, and the live object stays where it is;
		// the metadata face is snapshot lookup by contract
		aspect.setContent(EcoreUtil.copy(entry.object()));
		classMetadata.getAspects().add(aspect);
		placed.computeIfAbsent(entry.key(), key -> new ArrayList<>()).add(aspect);
	}

	private void detach(String key) {
		List<AspectEntry> aspects = placed.remove(key);
		if (aspects != null) {
			aspects.forEach(EcoreUtil::remove);
		}
	}

	private Optional<ClassMetadata> findClass(PackageMetadata packageMetadata, String className) {
		return packageMetadata.getClasses().stream().filter(cm -> className.equals(cm.getName())).findFirst();
	}

	private String nsUriOf(EClass anchor) {
		EPackage ePackage = anchor.getEPackage();
		return ePackage == null ? null : ePackage.getNsURI();
	}
}
