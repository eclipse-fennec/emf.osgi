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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.metadata.MetadataIndex;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.osgi.service.component.annotations.Component;

/**
 * In-memory index over hash maps - the default implementation, swappable through the
 * whiteboard.
 * <p>
 * Every key maps to a <b>list</b> of entries, not a single one. That is not defensive
 * coding: with several model versions registered under one nsURI, the same type URI, class
 * name and instance class name genuinely occur more than once. Single-result lookups
 * answer with the most recently indexed entry, list lookups expose the full candidate set.
 * <p>
 * Reads are lock-free. Writes happen under the service's write lock, and the per-key lists
 * are copy-on-write, so a reader always sees a consistent snapshot.
 *
 * @author Data In Motion Consulting
 */
@Component(name = "MapBasedMetadataIndex", service = MetadataIndex.class)
public class MapBasedMetadataIndex implements MetadataIndex {

	private static final String KEY_SEPARATOR = "::";

	private final Map<String, List<ClassMetadata>> classesByURI = new ConcurrentHashMap<>();
	private final Map<String, List<FeatureMetadata>> featuresByURI = new ConcurrentHashMap<>();
	private final Map<String, List<OperationMetadata>> operationsByURI = new ConcurrentHashMap<>();

	private final Map<String, List<ClassMetadata>> classesByNsURIAndName = new ConcurrentHashMap<>();
	private final Map<String, List<ClassMetadata>> classesByNsURIAndInstanceClassName = new ConcurrentHashMap<>();

	private final Map<String, List<ClassMetadata>> classesByName = new ConcurrentHashMap<>();
	private final Map<String, List<ClassMetadata>> classesByInstanceClassName = new ConcurrentHashMap<>();

	@Override
	public void indexPackage(PackageMetadata packageMetadata) {
		if (packageMetadata == null) {
			return;
		}
		packageMetadata.getClasses().forEach(this::indexClass);
	}

	@Override
	public void indexClass(ClassMetadata classMetadata) {
		if (classMetadata == null) {
			return;
		}
		EClass eClass = classMetadata.getEClass();
		String name = classMetadata.getName();
		String nsURI = nsURIOf(eClass);
		String instanceClassName = eClass != null ? eClass.getInstanceClassName() : null;

		putMulti(classesByURI, classMetadata.getTypeURI(), classMetadata);
		putMulti(classesByNsURIAndName, compositeKey(nsURI, name), classMetadata);
		putMulti(classesByNsURIAndInstanceClassName, compositeKey(nsURI, instanceClassName), classMetadata);
		putMulti(classesByName, name, classMetadata);
		putMulti(classesByInstanceClassName, instanceClassName, classMetadata);

		classMetadata.getFeatures().forEach(this::indexFeature);
		classMetadata.getOperations().forEach(this::indexOperation);
	}

	@Override
	public void indexFeature(FeatureMetadata featureMetadata) {
		if (featureMetadata != null) {
			putMulti(featuresByURI, uriOf(featureMetadata.getEFeature()), featureMetadata);
		}
	}

	@Override
	public void indexOperation(OperationMetadata operationMetadata) {
		if (operationMetadata != null) {
			putMulti(operationsByURI, uriOf(operationMetadata.getEOperation()), operationMetadata);
		}
	}

	@Override
	public void removePackage(PackageMetadata packageMetadata) {
		if (packageMetadata == null) {
			return;
		}
		packageMetadata.getClasses().forEach(this::removeClass);
	}

	@Override
	public void removeClass(ClassMetadata classMetadata) {
		if (classMetadata == null) {
			return;
		}
		EClass eClass = classMetadata.getEClass();
		String name = classMetadata.getName();
		String nsURI = nsURIOf(eClass);
		String instanceClassName = eClass != null ? eClass.getInstanceClassName() : null;

		// Only this version's entries go: a surviving same-nsURI version keeps its own,
		// structurally identical, keys.
		removeMulti(classesByURI, classMetadata.getTypeURI(), classMetadata);
		removeMulti(classesByNsURIAndName, compositeKey(nsURI, name), classMetadata);
		removeMulti(classesByNsURIAndInstanceClassName, compositeKey(nsURI, instanceClassName), classMetadata);
		removeMulti(classesByName, name, classMetadata);
		removeMulti(classesByInstanceClassName, instanceClassName, classMetadata);

		classMetadata.getFeatures().forEach(this::removeFeature);
		classMetadata.getOperations().forEach(this::removeOperation);
	}

	@Override
	public void removeFeature(FeatureMetadata featureMetadata) {
		if (featureMetadata != null) {
			removeMulti(featuresByURI, uriOf(featureMetadata.getEFeature()), featureMetadata);
		}
	}

	@Override
	public void removeOperation(OperationMetadata operationMetadata) {
		if (operationMetadata != null) {
			removeMulti(operationsByURI, uriOf(operationMetadata.getEOperation()), operationMetadata);
		}
	}

	@Override
	public void clear() {
		classesByURI.clear();
		featuresByURI.clear();
		operationsByURI.clear();
		classesByNsURIAndName.clear();
		classesByNsURIAndInstanceClassName.clear();
		classesByName.clear();
		classesByInstanceClassName.clear();
	}

	@Override
	public Optional<ClassMetadata> findByInstanceClassName(String nsURI, String instanceClassName) {
		return newest(classesByNsURIAndInstanceClassName, compositeKey(nsURI, instanceClassName));
	}

	@Override
	public List<ClassMetadata> findAllByInstanceClassName(String instanceClassName) {
		return snapshot(classesByInstanceClassName, instanceClassName);
	}

	@Override
	public Optional<ClassMetadata> findByClassName(String nsURI, String className) {
		return newest(classesByNsURIAndName, compositeKey(nsURI, className));
	}

	@Override
	public List<ClassMetadata> findAllByClassName(String className) {
		return snapshot(classesByName, className);
	}

	@Override
	public Optional<ClassMetadata> findClassByURI(String uri) {
		return newest(classesByURI, uri);
	}

	@Override
	public Optional<FeatureMetadata> findFeatureByURI(String uri) {
		return newest(featuresByURI, uri);
	}

	@Override
	public List<ClassMetadata> findClassesByAnnotation(String annotationSource, String key, String value) {
		return byAnnotation(classesByURI, ClassMetadata::getEClass, annotationSource, key, value);
	}

	@Override
	public List<FeatureMetadata> findFeaturesByAnnotation(String annotationSource, String key, String value) {
		return byAnnotation(featuresByURI, FeatureMetadata::getEFeature, annotationSource, key, value);
	}

	@Override
	public Optional<OperationMetadata> findOperationByURI(String uri) {
		return newest(operationsByURI, uri);
	}

	@Override
	public List<OperationMetadata> findOperationsByAnnotation(String annotationSource, String key, String value) {
		return byAnnotation(operationsByURI, OperationMetadata::getEOperation, annotationSource, key, value);
	}

	private static String compositeKey(String nsURI, String name) {
		return nsURI != null && name != null ? nsURI + KEY_SEPARATOR + name : null;
	}

	private static String nsURIOf(EClass eClass) {
		return eClass != null && eClass.getEPackage() != null ? eClass.getEPackage().getNsURI() : null;
	}

	private static String uriOf(EObject eObject) {
		return eObject != null ? EcoreUtil.getURI(eObject).toString() : null;
	}

	/**
	 * Appends to the per-key list, creating it on first use. A {@code null} key is a
	 * missing coordinate, not an error - the entry is simply not reachable that way.
	 */
	private static <T> void putMulti(Map<String, List<T>> map, String key, T value) {
		if (key != null) {
			map.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(value);
		}
	}

	/**
	 * Removes one entry, dropping the key when its last version is gone, so a surviving
	 * same-key version is never affected.
	 */
	private static <T> void removeMulti(Map<String, List<T>> map, String key, T value) {
		if (key == null) {
			return;
		}
		List<T> versions = map.get(key);
		if (versions != null) {
			versions.remove(value);
			if (versions.isEmpty()) {
				map.remove(key);
			}
		}
	}

	/**
	 * The most recently indexed entry for a key. Iterates rather than indexing the tail, so
	 * a concurrent removal cannot race into an index out of bounds.
	 */
	private static <T> Optional<T> newest(Map<String, List<T>> map, String key) {
		if (key == null) {
			return Optional.empty();
		}
		List<T> versions = map.get(key);
		if (versions == null) {
			return Optional.empty();
		}
		T last = null;
		for (T value : versions) {
			last = value;
		}
		return Optional.ofNullable(last);
	}

	private static <T> List<T> snapshot(Map<String, List<T>> map, String key) {
		if (key == null) {
			return Collections.emptyList();
		}
		List<T> versions = map.get(key);
		return versions != null ? List.copyOf(versions) : Collections.emptyList();
	}

	/**
	 * Scans one index for entries whose Ecore element carries a matching annotation. A scan
	 * rather than a lookup: annotation queries are rare and building an index per
	 * source/key pair would cost more than it saves.
	 */
	private static <T, E extends EModelElement> List<T> byAnnotation(Map<String, List<T>> map,
			Function<T, E> elementOf, String annotationSource, String key, String value) {
		if (annotationSource == null || key == null) {
			return Collections.emptyList();
		}
		List<T> results = new ArrayList<>();
		for (List<T> versions : map.values()) {
			for (T candidate : versions) {
				E element = elementOf.apply(candidate);
				if (element != null && matches(element.getEAnnotation(annotationSource), key, value)) {
					results.add(candidate);
				}
			}
		}
		return results;
	}

	/**
	 * Whether an annotation carries the key, and the value if one was asked for. A
	 * {@code null} value matches any value the key happens to have.
	 */
	private static boolean matches(EAnnotation annotation, String key, String value) {
		if (annotation == null) {
			return false;
		}
		String actual = annotation.getDetails().get(key);
		return actual != null && (value == null || value.equals(actual));
	}
}
