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
package org.eclipse.fennec.emf.osgi.metadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Lifecycle side of the metadata system. The implementation registers itself under both
 * this interface and {@link MetadataService}, so consumers can inject read-only access
 * while whiteboard components take the full handle.
 * <p>
 * <b>Registration is per model version.</b> Registering an {@link EPackage} whose content
 * is already known deduplicates onto the existing tree; registering diverging content
 * under a known nsURI creates a second, coexisting tree. Unregistration is a per-version
 * refcount: only the last registration of <em>that</em> fingerprint drops it, so another
 * live version of the same nsURI is never collaterally removed - and neither is a tree
 * that {@link MetadataService#getPackageMetadata(EPackage)} created as a cached read,
 * since that path takes no registration.
 *
 * @author Data In Motion Consulting
 */
@ProviderType
public interface MetadataWhiteboard extends MetadataService {

	/**
	 * Registers a package and builds its metadata.
	 *
	 * @param ePackage the package to register
	 * @return the new or existing tree, empty only if {@code ePackage} is {@code null}
	 */
	Optional<PackageMetadata> registerPackage(EPackage ePackage);

	/**
	 * Registers a package, taking the OSGi service properties of its EPackage service as
	 * transient build context. They reach handlers through
	 * {@link PackageMetadata#getProperties()} - stringified and never serialized, because
	 * service properties are node-specific and volatile.
	 * <p>
	 * A fingerprint among those properties is build context, not truth: the fingerprint
	 * this service keys by is always computed locally.
	 *
	 * @param ePackage the package to register
	 * @param properties the EPackage service properties, or {@code null}
	 * @return the new or existing tree, empty only if {@code ePackage} is {@code null}
	 */
	Optional<PackageMetadata> registerPackage(EPackage ePackage, Map<String, Object> properties);

	/**
	 * Withdraws one registration of this package's model version. Removes the tree only
	 * when it was the last one. No-op for an unknown package.
	 *
	 * @param ePackage the package to unregister
	 */
	void unregisterPackage(EPackage ePackage);

	/**
	 * Adopts model versions from a registry read back from storage - the counterpart of
	 * saving {@link MetadataService#getRegistry()}. What was computed once, possibly at
	 * build time or on another node, is taken over instead of rebuilt.
	 * <p>
	 * <b>Adopted, not trusted.</b> A tree is taken over only if it can be keyed and is not
	 * stale. An entry is skipped when it carries no fingerprint or nsURI, when its
	 * fingerprint is already live here (the in-memory tree wins), or when its
	 * {@link PackageMetadata#getEPackage()} resolves and recomputing that package's
	 * fingerprint contradicts the stored one - the saved tree then describes a model version
	 * that no longer exists. Verification needs both a resolvable package and a bound
	 * {@link FingerprintService}; where either is missing the stored key is taken as stated,
	 * which is the offline case and the reason the return value lists what was actually
	 * adopted.
	 * <p>
	 * <b>Adoption is a move.</b> Trees are containment-transferred into this whiteboard's
	 * registry, so the argument is left holding only what was skipped. Adopted versions take
	 * no liveness count - like a tree built by
	 * {@link MetadataService#getPackageMetadata(EPackage)}, they are cached state, and no
	 * unbind evicts them.
	 * <p>
	 * <b>Handlers are not re-run.</b> Their contributions are part of what was saved, so
	 * replaying them would duplicate entries. A handler added <em>after</em> a load is
	 * replayed over the adopted trees like over any other, so a contributor that cannot
	 * tolerate a tree already carrying its entries should check for its own type id first.
	 *
	 * @param registry the registry to adopt from; {@code null} is ignored
	 * @return the adopted trees, in encounter order; empty if nothing qualified
	 */
	List<PackageMetadata> loadRegistry(MetadataRegistry registry);

	/**
	 * Binds the service that computes model identity, replacing any previous one.
	 * <p>
	 * Mandatory collaborator: without it no package can be registered. In OSGi DS binds it
	 * before the whiteboard becomes available, and
	 * {@code MetadataServices.createWhiteboard(MetadataHandler...)} sets the default outside
	 * OSGi - this method exists for the caller that wants a different one afterwards. A
	 * replacement discards fingerprints memoized from the previous service, which may have
	 * used a different scheme; already built trees keep the identity they were registered
	 * under.
	 * <p>
	 * There is no unset counterpart, unlike {@link #unsetMetadataIndex(MetadataIndex)}: the
	 * whiteboard is not usable without a service, so it is replaced, never withdrawn. A
	 * {@code null} argument is ignored.
	 *
	 * @param fingerprintService the service to bind; {@code null} is ignored
	 */
	void setFingerprintService(FingerprintService fingerprintService);

	/**
	 * The currently bound index.
	 *
	 * @return the index, or empty if none is bound
	 */
	Optional<MetadataIndex> getMetadataIndex();

	/**
	 * Binds an index and populates it with everything already registered, replacing any
	 * previous one.
	 *
	 * @param index the index to bind
	 */
	void setMetadataIndex(MetadataIndex index);

	/**
	 * Unbinds the given index, clearing it first. Ignored unless it is the one currently
	 * bound - which guards against out-of-order lifecycle events.
	 *
	 * @param index the index to unbind
	 */
	void unsetMetadataIndex(MetadataIndex index);

	/**
	 * Adds a handler. Late arrivals see everything already registered:
	 * {@link MetadataHandler#onPackageRegistered(PackageMetadata)} is replayed for each
	 * known model version, so a contributor's entries end up on trees built before it
	 * appeared.
	 *
	 * @param handler the handler to add
	 */
	void addMetadataHandler(MetadataHandler handler);

	/**
	 * Removes a handler and calls {@link MetadataHandler#clear()} on it.
	 *
	 * @param handler the handler to remove
	 */
	void removeMetadataHandler(MetadataHandler handler);

	/**
	 * All currently registered handlers.
	 *
	 * @return the handlers, never {@code null}
	 */
	List<MetadataHandler> getMetadataHandlers();
}
