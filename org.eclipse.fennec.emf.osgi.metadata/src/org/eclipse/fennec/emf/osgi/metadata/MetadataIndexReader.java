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

import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Read-only query interface for metadata lookups. Consumers reach it through
 * {@link MetadataService#getIndexReader()}.
 * <p>
 * The scoped lookups ({@code nsURI} plus a name) can still be ambiguous: several model
 * versions may share one nsURI, and the index keeps them all. Where that happens the
 * scoped variants answer with one match and the {@code findAll…} variants expose the
 * full candidate set - selection is the caller's decision, never the index's.
 *
 * @author Data In Motion Consulting
 */
@ProviderType
public interface MetadataIndexReader {

	/**
	 * Finds class metadata by Java instance class name within one package.
	 *
	 * @param nsURI the namespace URI of the package to search
	 * @param instanceClassName the Java instance class name
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<ClassMetadata> findByInstanceClassName(String nsURI, String instanceClassName);

	/**
	 * Finds all class metadata with a matching Java instance class name, across every
	 * indexed package. More than one match is normal - the same Java type can back
	 * classes in several models.
	 *
	 * @param instanceClassName the Java instance class name
	 * @return all matches, never {@code null}
	 */
	List<ClassMetadata> findAllByInstanceClassName(String instanceClassName);

	/**
	 * Finds class metadata by EClass name within one package.
	 *
	 * @param nsURI the namespace URI of the package to search
	 * @param className the EClass name
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<ClassMetadata> findByClassName(String nsURI, String className);

	/**
	 * Finds all class metadata with a matching EClass name across every indexed package.
	 *
	 * @param className the EClass name
	 * @return all matches, never {@code null}
	 */
	List<ClassMetadata> findAllByClassName(String className);

	/**
	 * Finds class metadata by its full EMF URI, e.g.
	 * {@code http://example.org/model#//Person}.
	 *
	 * @param uri the EClass URI
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<ClassMetadata> findClassByURI(String uri);

	/**
	 * Finds feature metadata by its full EMF URI, e.g.
	 * {@code http://example.org/model#//Person/name}.
	 *
	 * @param uri the feature URI
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<FeatureMetadata> findFeatureByURI(String uri);

	/**
	 * Finds all classes whose EClass carries an annotation with the given source and key.
	 *
	 * @param annotationSource the annotation source
	 * @param key the detail key
	 * @param value the detail value, or {@code null} to match any value for the key
	 * @return all matches, never {@code null}
	 */
	List<ClassMetadata> findClassesByAnnotation(String annotationSource, String key, String value);

	/**
	 * Finds all features whose EStructuralFeature carries an annotation with the given
	 * source and key.
	 *
	 * @param annotationSource the annotation source
	 * @param key the detail key
	 * @param value the detail value, or {@code null} to match any value for the key
	 * @return all matches, never {@code null}
	 */
	List<FeatureMetadata> findFeaturesByAnnotation(String annotationSource, String key, String value);

	/**
	 * Finds operation metadata by its full EMF URI, e.g.
	 * {@code http://example.org/model#//Person/greet}.
	 *
	 * @param uri the operation URI
	 * @return the metadata, or empty if nothing matches
	 */
	Optional<OperationMetadata> findOperationByURI(String uri);

	/**
	 * Finds all operations whose EOperation carries an annotation with the given source
	 * and key - the lookup for constraint expressions attached to operations, for
	 * instance.
	 *
	 * @param annotationSource the annotation source
	 * @param key the detail key
	 * @param value the detail value, or {@code null} to match any value for the key
	 * @return all matches, never {@code null}
	 */
	List<OperationMetadata> findOperationsByAnnotation(String annotationSource, String key, String value);
}
