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
import java.util.Optional;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Read access to pre-computed model metadata. Lifecycle lives in
 * {@link MetadataWhiteboard}, so a consumer that only reads never gets a handle that
 * could register or unregister anything.
 * <p>
 * <b>Identity is the model fingerprint, not the nsURI.</b> Two packages published under
 * one nsURI with diverging content are two model versions and get two metadata trees.
 * Every lookup that starts from an {@link EPackage} instance therefore resolves through
 * that instance's fingerprint and is exact.
 * {@link #getPackageMetadata(String)} cannot be exact - it takes an nsURI, which does not
 * identify a version - and answers best effort with the most recently registered one; use
 * {@link #getPackageMetadataVersions(String)} when the ambiguity matters.
 * <p>
 * One exception to strict read-only semantics: {@link #getPackageMetadata(EPackage)}
 * builds and caches on a miss. That is a memoized read, not lifecycle - deterministic,
 * idempotent, and invisible to unregistration - so stateless consumers holding only an
 * EPackage need no admin interface.
 *
 * @author Data In Motion Consulting
 */
@ProviderType
public interface MetadataService {

	/**
	 * The index for fast lookups, kept in sync as packages come and go.
	 *
	 * @return the reader, or empty while no index is bound
	 */
	Optional<MetadataIndexReader> getIndexReader();

	/**
	 * Metadata for a namespace URI. Best effort: an nsURI does not identify a model
	 * version, so with several versions registered this answers with the most recent one.
	 *
	 * @param nsURI the namespace URI
	 * @return the metadata, or empty if the nsURI is unknown
	 * @see #getPackageMetadataVersions(String)
	 */
	Optional<PackageMetadata> getPackageMetadata(String nsURI);

	/**
	 * Metadata for exactly this instance's model version, resolved through its fingerprint
	 * and built on a miss (resolve-or-build). No prior registration required.
	 *
	 * @param ePackage the package
	 * @return the metadata, empty only if {@code ePackage} is {@code null}
	 */
	Optional<PackageMetadata> getPackageMetadata(EPackage ePackage);

	/**
	 * Metadata for a model version by fingerprint. Pure lookup - never builds, because an
	 * externally supplied fingerprint is a key, not a licence to create state.
	 *
	 * @param fingerprint the model fingerprint
	 * @return the metadata, or empty if no such model version is known
	 */
	Optional<PackageMetadata> getPackageMetadataByFingerprint(String fingerprint);

	/**
	 * Every known model version for a namespace URI, oldest first. The tail is what
	 * {@link #getPackageMetadata(String)} serves.
	 * <p>
	 * This exposes facts, not policy: which version a consumer wants - newest, pinned, or
	 * tried in order against a stored document - stays the consumer's decision.
	 *
	 * @param nsURI the namespace URI
	 * @return the versions in registration order, empty if the nsURI is unknown
	 */
	List<PackageMetadata> getPackageMetadataVersions(String nsURI);

	/**
	 * Metadata for an EClass, resolved by instance identity.
	 *
	 * @param eClass the class
	 * @return the metadata, or empty if its package is not registered
	 */
	Optional<ClassMetadata> getClassMetadata(EClass eClass);

	/**
	 * Metadata for an EClass by its full EMF URI.
	 *
	 * @param uri the EClass URI
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<ClassMetadata> getClassMetadataByURI(String uri);

	/**
	 * Metadata for an EClass by name within a package.
	 *
	 * @param className the EClass name
	 * @param nsURI the namespace URI
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<ClassMetadata> getClassMetadataByName(String className, String nsURI);

	/**
	 * Metadata for a structural feature, resolved by instance identity.
	 *
	 * @param feature the feature
	 * @return the metadata, or empty if its package is not registered
	 */
	Optional<FeatureMetadata> getFeatureMetadata(EStructuralFeature feature);

	/**
	 * Metadata for a structural feature by its full EMF URI.
	 *
	 * @param uri the feature URI
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<FeatureMetadata> getFeatureMetadataByURI(String uri);

	/**
	 * Metadata for a structural feature by feature name, class name and namespace URI.
	 *
	 * @param featureName the feature name
	 * @param className the owning EClass name
	 * @param nsURI the namespace URI
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<FeatureMetadata> getFeatureMetadataByName(String featureName, String className, String nsURI);

	/**
	 * Metadata for a structural feature by name within a known class - cheaper than
	 * {@link #getFeatureMetadataByName(String, String, String)} when the class metadata is
	 * already at hand.
	 *
	 * @param featureName the feature name
	 * @param classMetadata the owning class metadata
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<FeatureMetadata> getFeatureMetadataFromClass(String featureName, ClassMetadata classMetadata);

	/**
	 * Metadata for an EOperation, resolved by instance identity.
	 *
	 * @param operation the operation
	 * @return the metadata, or empty if its package is not registered
	 */
	Optional<OperationMetadata> getOperationMetadata(EOperation operation);

	/**
	 * Metadata for an EOperation by its full EMF URI.
	 *
	 * @param uri the operation URI
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<OperationMetadata> getOperationMetadataByURI(String uri);

	/**
	 * Metadata for an operation by name within a known class. Operation names are not
	 * unique under overloading - this answers with the first match.
	 *
	 * @param operationName the operation name
	 * @param classMetadata the owning class metadata
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<OperationMetadata> getOperationMetadataFromClass(String operationName, ClassMetadata classMetadata);

	/**
	 * A package's aspect entry for one contributor.
	 *
	 * @param ePackage the package
	 * @param aspectTypeId the contributor's type id, e.g. {@code codec}
	 * @return the entry, or empty if the package is unknown or carries no such entry
	 */
	Optional<AspectEntry> getPackageAspect(EPackage ePackage, String aspectTypeId);

	/**
	 * A class's aspect entry for one contributor.
	 *
	 * @param eClass the class
	 * @param aspectTypeId the contributor's type id
	 * @return the entry, or empty if the class is unknown or carries no such entry
	 */
	Optional<AspectEntry> getClassAspect(EClass eClass, String aspectTypeId);

	/**
	 * A feature's aspect entry for one contributor.
	 *
	 * @param feature the feature
	 * @param aspectTypeId the contributor's type id
	 * @return the entry, or empty if the feature is unknown or carries no such entry
	 */
	Optional<AspectEntry> getFeatureAspect(EStructuralFeature feature, String aspectTypeId);

	/**
	 * An operation's aspect entry for one contributor.
	 *
	 * @param operation the operation
	 * @param aspectTypeId the contributor's type id
	 * @return the entry, or empty if the operation is unknown or carries no such entry
	 */
	Optional<AspectEntry> getOperationAspect(EOperation operation, String aspectTypeId);

	/**
	 * The registry holding every known metadata tree - the serializable root, for
	 * persisting or replicating the whole state.
	 *
	 * @return the registry, never {@code null}
	 */
	MetadataRegistry getRegistry();
}
