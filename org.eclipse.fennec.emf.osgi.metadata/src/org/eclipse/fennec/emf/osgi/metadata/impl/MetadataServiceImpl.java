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
package org.eclipse.fennec.emf.osgi.metadata.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.metadata.MetadataIndex;
import org.eclipse.fennec.emf.osgi.metadata.MetadataIndexReader;
import org.eclipse.fennec.emf.osgi.metadata.MetadataWhiteboard;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata;

/**
 * The metadata whiteboard, keyed by model fingerprint.
 * <p>
 * <b>Why fingerprint and not nsURI.</b> An nsURI names a model, not a model version. Two
 * bundles can publish the same nsURI with diverging content, and stored documents outlive
 * the version that wrote them. Keying by nsURI would silently serve one version's objects
 * with another version's metadata; keying by the canonical fingerprint makes identical
 * content deduplicate and diverging content coexist. The nsURI index is kept as a
 * secondary, best-effort lookup and nothing more.
 * <p>
 * <b>Liveness is per version.</b> {@code unregisterPackage} decrements a refcount for that
 * fingerprint, so unbinding one of two live versions of an nsURI leaves the other
 * untouched. Trees created by the pull path
 * ({@link #getPackageMetadata(EPackage)}) carry no count and are never evicted by an
 * unbind - they are cached reads, not registrations.
 * <p>
 * <b>Publication order.</b> A tree is built, handed to the handlers, and only then
 * published to the lookup maps, the registry and the index. Handlers contribute
 * {@link AspectEntry} content, so publishing earlier would let a reader observe a model
 * version whose entries are still missing.
 *
 * @author Data In Motion Consulting
 */
public class MetadataServiceImpl implements MetadataWhiteboard {

	private static final String EXTENDED_META_DATA = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData";

	private final MetadataRegistry registry;
	private final List<MetadataHandler> handlers = new CopyOnWriteArrayList<>();

	private volatile MetadataIndex index;
	private volatile FingerprintService fingerprintService;

	/** Model versions by fingerprint - the primary key. */
	private final Map<String, PackageMetadata> packagesByFingerprint = new ConcurrentHashMap<>();

	/** Secondary index: all versions of an nsURI, in registration order. */
	private final Map<String, List<PackageMetadata>> packagesByNsURI = new ConcurrentHashMap<>();

	/** Whiteboard registrations per fingerprint; absent for pull-created trees. */
	private final Map<String, Integer> livenessByFingerprint = new ConcurrentHashMap<>();

	/**
	 * Fingerprint memo per EPackage instance for hot read paths. Weak, because the memo
	 * must not keep a package alive; write paths always recompute, since an EPackage is
	 * mutable and a memo may predate a content change.
	 */
	private final Map<EPackage, String> fingerprintByInstance = Collections.synchronizedMap(new WeakHashMap<>());

	private final Map<EClass, ClassMetadata> classesByEClass = new ConcurrentHashMap<>();
	private final Map<EStructuralFeature, FeatureMetadata> featuresByEFeature = new ConcurrentHashMap<>();
	private final Map<EOperation, OperationMetadata> operationsByEOperation = new ConcurrentHashMap<>();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Creates a service with an empty registry.
	 */
	public MetadataServiceImpl() {
		this(MetadataFactory.eINSTANCE.createMetadataRegistry());
	}

	/**
	 * Creates a service over an existing registry, e.g. one read back from an index.
	 * <p>
	 * Starts with an in-memory index already in place. Without one, every URI and name
	 * lookup would answer empty until something binds an index - a silent, hard-to-place
	 * gap. A bound index replaces this default.
	 *
	 * @param registry the registry to use; must not be {@code null}
	 */
	public MetadataServiceImpl(MetadataRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
		MetadataIndex defaultIndex = new MapBasedMetadataIndex();
		this.registry.getPackages().forEach(defaultIndex::indexPackage);
		this.index = defaultIndex;
	}

	/**
	 * Sets the fingerprint service that computes model identity. Must be set before any
	 * package is registered.
	 *
	 * @param fingerprintService the service; {@code null} is ignored
	 */
	public void setFingerprintService(FingerprintService fingerprintService) {
		if (fingerprintService != null) {
			this.fingerprintService = fingerprintService;
			// Memoized values came from the previous service, possibly a different scheme.
			fingerprintByInstance.clear();
		}
	}

	@Override
	public Optional<MetadataIndexReader> getIndexReader() {
		return Optional.ofNullable(index);
	}

	@Override
	public MetadataRegistry getRegistry() {
		return registry;
	}

	@Override
	public Optional<PackageMetadata> getPackageMetadata(String nsURI) {
		if (nsURI == null) {
			return Optional.empty();
		}
		return newest(packagesByNsURI.get(nsURI));
	}

	@Override
	public Optional<PackageMetadata> getPackageMetadata(EPackage ePackage) {
		if (ePackage == null) {
			return Optional.empty();
		}
		// Lock-free fast path for content already known.
		PackageMetadata known = packagesByFingerprint.get(memoizedFingerprint(ePackage));
		if (known != null) {
			return Optional.of(known);
		}
		lock.writeLock().lock();
		try {
			// Recompute under the lock - the memo may predate a content change.
			String fingerprint = fingerprintFor(ePackage);
			PackageMetadata existing = packagesByFingerprint.get(fingerprint);
			if (existing != null) {
				return Optional.of(existing);
			}
			// Pull path: build and cache without a liveness count. A memoized read, not a
			// registration, so no unbind will ever evict it.
			return Optional.of(buildAndPublish(ePackage, fingerprint, null));
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Optional<PackageMetadata> getPackageMetadataByFingerprint(String fingerprint) {
		return fingerprint != null ? Optional.ofNullable(packagesByFingerprint.get(fingerprint)) : Optional.empty();
	}

	@Override
	public List<PackageMetadata> getPackageMetadataVersions(String nsURI) {
		if (nsURI == null) {
			return Collections.emptyList();
		}
		List<PackageMetadata> versions = packagesByNsURI.get(nsURI);
		return versions != null ? List.copyOf(versions) : Collections.emptyList();
	}

	@Override
	public Optional<PackageMetadata> registerPackage(EPackage ePackage) {
		return registerPackage(ePackage, null);
	}

	@Override
	public Optional<PackageMetadata> registerPackage(EPackage ePackage, Map<String, Object> properties) {
		if (ePackage == null) {
			return Optional.empty();
		}
		lock.writeLock().lock();
		try {
			// The fingerprint is computed before any existence check: registration is keyed
			// by model version, not by nsURI. Identical content deduplicates onto the
			// existing tree, diverging content under a known nsURI gets its own.
			String fingerprint = fingerprintFor(ePackage);
			PackageMetadata packageMetadata = packagesByFingerprint.get(fingerprint);
			if (packageMetadata == null) {
				packageMetadata = buildAndPublish(ePackage, fingerprint, properties);
			}
			livenessByFingerprint.merge(fingerprint, 1, Integer::sum);
			return Optional.of(packageMetadata);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void unregisterPackage(EPackage ePackage) {
		if (ePackage == null) {
			return;
		}
		lock.writeLock().lock();
		try {
			String fingerprint = fingerprintFor(ePackage);
			PackageMetadata packageMetadata = packagesByFingerprint.get(fingerprint);
			if (packageMetadata == null) {
				return;
			}
			Integer count = livenessByFingerprint.get(fingerprint);
			if (count == null) {
				return; // pull-created tree - there is no registration to undo
			}
			if (count > 1) {
				livenessByFingerprint.put(fingerprint, count - 1);
				return;
			}
			livenessByFingerprint.remove(fingerprint);
			withdraw(fingerprint, packageMetadata);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Optional<ClassMetadata> getClassMetadata(EClass eClass) {
		return eClass != null ? Optional.ofNullable(classesByEClass.get(eClass)) : Optional.empty();
	}

	@Override
	public Optional<ClassMetadata> getClassMetadataByURI(String uri) {
		return getIndexReader().flatMap(reader -> reader.findClassByURI(uri));
	}

	@Override
	public Optional<ClassMetadata> getClassMetadataByName(String className, String nsURI) {
		return getIndexReader().flatMap(reader -> reader.findByClassName(nsURI, className));
	}

	@Override
	public Optional<FeatureMetadata> getFeatureMetadata(EStructuralFeature feature) {
		return feature != null ? Optional.ofNullable(featuresByEFeature.get(feature)) : Optional.empty();
	}

	@Override
	public Optional<FeatureMetadata> getFeatureMetadataByURI(String uri) {
		return getIndexReader().flatMap(reader -> reader.findFeatureByURI(uri));
	}

	@Override
	public Optional<FeatureMetadata> getFeatureMetadataByName(String featureName, String className, String nsURI) {
		return getClassMetadataByName(className, nsURI)
				.flatMap(classMetadata -> getFeatureMetadataFromClass(featureName, classMetadata));
	}

	@Override
	public Optional<FeatureMetadata> getFeatureMetadataFromClass(String featureName, ClassMetadata classMetadata) {
		if (featureName == null || classMetadata == null) {
			return Optional.empty();
		}
		return classMetadata.getFeatures().stream().filter(feature -> featureName.equals(feature.getName())).findFirst();
	}

	@Override
	public Optional<OperationMetadata> getOperationMetadata(EOperation operation) {
		return operation != null ? Optional.ofNullable(operationsByEOperation.get(operation)) : Optional.empty();
	}

	@Override
	public Optional<OperationMetadata> getOperationMetadataByURI(String uri) {
		return getIndexReader().flatMap(reader -> reader.findOperationByURI(uri));
	}

	@Override
	public Optional<OperationMetadata> getOperationMetadataFromClass(String operationName,
			ClassMetadata classMetadata) {
		if (operationName == null || classMetadata == null) {
			return Optional.empty();
		}
		// Operation names are not unique under overloading - first match wins.
		return classMetadata.getOperations().stream().filter(operation -> operationName.equals(operation.getName()))
				.findFirst();
	}

	@Override
	public Optional<AspectEntry> getPackageAspect(EPackage ePackage, String aspectTypeId) {
		return getPackageMetadata(ePackage).flatMap(metadata -> aspect(metadata.getAspects(), aspectTypeId));
	}

	@Override
	public Optional<AspectEntry> getClassAspect(EClass eClass, String aspectTypeId) {
		return getClassMetadata(eClass).flatMap(metadata -> aspect(metadata.getAspects(), aspectTypeId));
	}

	@Override
	public Optional<AspectEntry> getFeatureAspect(EStructuralFeature feature, String aspectTypeId) {
		return getFeatureMetadata(feature).flatMap(metadata -> aspect(metadata.getAspects(), aspectTypeId));
	}

	@Override
	public Optional<AspectEntry> getOperationAspect(EOperation operation, String aspectTypeId) {
		return getOperationMetadata(operation).flatMap(metadata -> aspect(metadata.getAspects(), aspectTypeId));
	}

	@Override
	public Optional<MetadataIndex> getMetadataIndex() {
		return Optional.ofNullable(index);
	}

	@Override
	public void setMetadataIndex(MetadataIndex index) {
		if (index == null) {
			return;
		}
		lock.writeLock().lock();
		try {
			this.index = index;
			// A fresh index starts empty; everything it should hold is already in the
			// registry, so it can be filled from there.
			registry.getPackages().forEach(index::indexPackage);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void unsetMetadataIndex(MetadataIndex index) {
		lock.writeLock().lock();
		try {
			// Ignore anything but the index actually in use - guards against out-of-order
			// lifecycle events replacing a newer index with the removal of an older one.
			if (index != null && index == this.index) {
				index.clear();
				this.index = null;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void addMetadataHandler(MetadataHandler handler) {
		if (handler == null) {
			return;
		}
		lock.writeLock().lock();
		try {
			if (!handlers.contains(handler)) {
				handlers.add(handler);
				// Late arrival: replay everything already known, so a contributor's entries
				// reach trees that were built before it appeared.
				registry.getPackages().forEach(handler::onPackageRegistered);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removeMetadataHandler(MetadataHandler handler) {
		if (handler == null) {
			return;
		}
		lock.writeLock().lock();
		try {
			if (handlers.remove(handler)) {
				handler.clear();
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public List<MetadataHandler> getMetadataHandlers() {
		return List.copyOf(handlers);
	}

	/**
	 * Builds the tree for a model version and publishes it. Must be called under the write
	 * lock with a freshly computed fingerprint. Does not touch liveness - the caller
	 * decides whether this counts as a registration or as a cached read.
	 */
	private PackageMetadata buildAndPublish(EPackage ePackage, String fingerprint, Map<String, Object> properties) {
		String nsURI = ePackage.getNsURI();

		PackageMetadata packageMetadata = MetadataFactory.eINSTANCE.createPackageMetadata();
		packageMetadata.setEPackage(ePackage);
		packageMetadata.setNsURI(nsURI);
		packageMetadata.setModelFingerprint(fingerprint);

		// Transient build context. A fingerprint among these properties is context, never
		// truth: the key above was computed locally.
		if (properties != null) {
			properties.forEach((key, value) -> {
				if (key != null && value != null) {
					packageMetadata.getProperties().put(key, stringify(value));
				}
			});
		}

		for (EClassifier classifier : ePackage.getEClassifiers()) {
			if (classifier instanceof EClass eClass) {
				packageMetadata.getClasses().add(buildClassMetadata(eClass));
			}
		}
		resolveReferences(packageMetadata);

		// Handlers run before publication: they may attach aspect entries, and no reader
		// should be able to see a version whose entries are still missing.
		handlers.forEach(handler -> handler.onPackageRegistered(packageMetadata));

		registry.getPackages().add(packageMetadata);
		packagesByFingerprint.put(fingerprint, packageMetadata);
		packagesByNsURI.computeIfAbsent(nsURI, key -> new CopyOnWriteArrayList<>()).add(packageMetadata);

		MetadataIndex currentIndex = this.index;
		if (currentIndex != null) {
			currentIndex.indexPackage(packageMetadata);
		}
		return packageMetadata;
	}

	/**
	 * Drops a model version: handlers first, then every structure that refers to it. Must
	 * be called under the write lock.
	 */
	private void withdraw(String fingerprint, PackageMetadata packageMetadata) {
		handlers.forEach(handler -> handler.onPackageUnregistered(packageMetadata));

		packagesByFingerprint.remove(fingerprint);
		List<PackageMetadata> versions = packagesByNsURI.get(packageMetadata.getNsURI());
		if (versions != null) {
			versions.remove(packageMetadata);
			if (versions.isEmpty()) {
				packagesByNsURI.remove(packageMetadata.getNsURI(), versions);
			}
		}

		MetadataIndex currentIndex = this.index;
		if (currentIndex != null) {
			currentIndex.removePackage(packageMetadata);
		}

		for (ClassMetadata classMetadata : packageMetadata.getClasses()) {
			classesByEClass.remove(classMetadata.getEClass());
			classMetadata.getFeatures().forEach(feature -> featuresByEFeature.remove(feature.getEFeature()));
			classMetadata.getOperations().forEach(operation -> operationsByEOperation.remove(operation.getEOperation()));
		}
		registry.getPackages().remove(packageMetadata);
	}

	private ClassMetadata buildClassMetadata(EClass eClass) {
		ClassMetadata classMetadata = MetadataFactory.eINSTANCE.createClassMetadata();
		classMetadata.setEClass(eClass);
		classMetadata.setName(eClass.getName());
		classMetadata.setClassifierID(eClass.getClassifierID());
		classMetadata.setTypeURI(EcoreUtil.getURI(eClass).toString());
		classMetadata.setHasId(eClass.getEIDAttribute() != null);

		for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
			FeatureMetadata featureMetadata = buildFeatureMetadata(feature);
			if (featureMetadata != null) {
				classMetadata.getFeatures().add(featureMetadata);
				if (feature instanceof EAttribute attribute && attribute.isID()) {
					classMetadata.getIdFeatures().add(featureMetadata);
				}
			}
		}
		for (EOperation operation : eClass.getEOperations()) {
			classMetadata.getOperations().add(buildOperationMetadata(operation));
		}

		classesByEClass.put(eClass, classMetadata);
		return classMetadata;
	}

	private FeatureMetadata buildFeatureMetadata(EStructuralFeature feature) {
		FeatureMetadata featureMetadata;
		if (feature instanceof EAttribute attribute) {
			AttributeMetadata attributeMetadata = MetadataFactory.eINSTANCE.createAttributeMetadata();
			attributeMetadata.setEAttribute(attribute);
			attributeMetadata.setIsId(attribute.isID());
			attributeMetadata.setDefaultValue(attribute.getDefaultValue());
			featureMetadata = attributeMetadata;
		} else if (feature instanceof EReference reference) {
			ReferenceMetadata referenceMetadata = MetadataFactory.eINSTANCE.createReferenceMetadata();
			referenceMetadata.setEReference(reference);
			referenceMetadata.setContainment(reference.isContainment());
			referenceMetadata.setHasBidirectional(reference.getEOpposite() != null);
			featureMetadata = referenceMetadata;
		} else {
			// Ecore knows no third kind of feature; a future one is skipped rather than
			// mirrored as something it is not.
			return null;
		}

		featureMetadata.setEFeature(feature);
		featureMetadata.setName(feature.getName());
		featureMetadata.setFeatureID(feature.getFeatureID());
		featureMetadata.setExtendedMetaDataName(extendedMetaDataName(feature));

		featuresByEFeature.put(feature, featureMetadata);
		return featureMetadata;
	}

	private OperationMetadata buildOperationMetadata(EOperation operation) {
		OperationMetadata operationMetadata = MetadataFactory.eINSTANCE.createOperationMetadata();
		operationMetadata.setEOperation(operation);
		operationMetadata.setName(operation.getName());
		operationMetadata.setOperationID(operation.getOperationID());

		for (EParameter parameter : operation.getEParameters()) {
			ParameterMetadata parameterMetadata = MetadataFactory.eINSTANCE.createParameterMetadata();
			parameterMetadata.setEParameter(parameter);
			parameterMetadata.setName(parameter.getName());
			operationMetadata.getParameters().add(parameterMetadata);
		}

		operationsByEOperation.put(operation, operationMetadata);
		return operationMetadata;
	}

	/**
	 * Resolves everything that points at another class: supertypes, the transitive closure,
	 * reference targets and opposites, operation return and parameter types. Runs after all
	 * classes of the package exist, because these are cross-references within the tree.
	 * Targets outside the registered set stay unresolved, which is the honest answer.
	 */
	private void resolveReferences(PackageMetadata packageMetadata) {
		for (ClassMetadata classMetadata : packageMetadata.getClasses()) {
			EClass eClass = classMetadata.getEClass();

			for (EClass superType : eClass.getESuperTypes()) {
				ClassMetadata superMetadata = classesByEClass.get(superType);
				if (superMetadata != null) {
					classMetadata.getSuperTypes().add(superMetadata);
				}
			}
			for (EClass superType : eClass.getEAllSuperTypes()) {
				ClassMetadata superMetadata = classesByEClass.get(superType);
				if (superMetadata != null) {
					classMetadata.getAllSuperTypes().add(superMetadata);
				}
			}

			for (FeatureMetadata featureMetadata : classMetadata.getFeatures()) {
				if (featureMetadata instanceof ReferenceMetadata referenceMetadata) {
					EReference reference = referenceMetadata.getEReference();
					referenceMetadata.setTargetClassMetadata(classesByEClass.get(reference.getEReferenceType()));
					EReference opposite = reference.getEOpposite();
					if (opposite != null
							&& featuresByEFeature.get(opposite) instanceof ReferenceMetadata oppositeMetadata) {
						referenceMetadata.setOppositeMetadata(oppositeMetadata);
					}
				}
			}

			for (OperationMetadata operationMetadata : classMetadata.getOperations()) {
				if (operationMetadata.getEOperation().getEType() instanceof EClass returnType) {
					operationMetadata.setReturnTypeMetadata(classesByEClass.get(returnType));
				}
				for (ParameterMetadata parameterMetadata : operationMetadata.getParameters()) {
					if (parameterMetadata.getEParameter().getEType() instanceof EClass parameterType) {
						parameterMetadata.setTypeMetadata(classesByEClass.get(parameterType));
					}
				}
			}
		}
	}

	/**
	 * Computes the fingerprint freshly and refreshes the memo. Write paths must use this:
	 * an EPackage is mutable, and a memoized value may predate a content change.
	 */
	private String fingerprintFor(EPackage ePackage) {
		FingerprintService service = this.fingerprintService;
		if (service == null) {
			throw new IllegalStateException(
					"No FingerprintService set - model identity cannot be computed. Call setFingerprintService first.");
		}
		String fingerprint = service.fingerprint(ePackage);
		fingerprintByInstance.put(ePackage, fingerprint);
		return fingerprint;
	}

	/** Memoized fingerprint for read paths; computes and memoizes on first sight. */
	private String memoizedFingerprint(EPackage ePackage) {
		String fingerprint = fingerprintByInstance.get(ePackage);
		return fingerprint != null ? fingerprint : fingerprintFor(ePackage);
	}

	private static Optional<AspectEntry> aspect(List<AspectEntry> aspects, String aspectTypeId) {
		if (aspectTypeId == null) {
			return Optional.empty();
		}
		return aspects.stream().filter(entry -> aspectTypeId.equals(entry.getTypeId())).findFirst();
	}

	private static Optional<PackageMetadata> newest(List<PackageMetadata> versions) {
		if (versions == null) {
			return Optional.empty();
		}
		// Best effort under multi-version ambiguity: the most recently registered version.
		PackageMetadata last = null;
		for (PackageMetadata version : versions) {
			last = version;
		}
		return Optional.ofNullable(last);
	}

	private static String extendedMetaDataName(EStructuralFeature feature) {
		EAnnotation annotation = feature.getEAnnotation(EXTENDED_META_DATA);
		if (annotation != null) {
			String name = annotation.getDetails().get("name");
			if (name != null && !name.isEmpty()) {
				return name;
			}
		}
		return null;
	}

	private static String stringify(Object value) {
		return value instanceof Object[] array ? Arrays.toString(array) : String.valueOf(value);
	}
}
