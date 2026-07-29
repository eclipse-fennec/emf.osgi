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

import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Maintenance side of the index. Driven by {@link MetadataWhiteboard} when packages come
 * and go; consumers query through {@link MetadataIndexReader} instead.
 * <p>
 * The index is derived state, never a source of truth: everything it holds can be rebuilt
 * from the {@link PackageMetadata} trees the service owns. That is what makes the index
 * swappable at runtime.
 *
 * @author Data In Motion Consulting
 */
@ProviderType
public interface MetadataIndexWriter {

	/**
	 * Indexes a package and everything below it - classes, features, operations.
	 *
	 * @param packageMetadata the package to index; {@code null} is ignored
	 */
	void indexPackage(PackageMetadata packageMetadata);

	/**
	 * Indexes a single class with its features and operations.
	 *
	 * @param classMetadata the class to index; {@code null} is ignored
	 */
	void indexClass(ClassMetadata classMetadata);

	/**
	 * Indexes a single feature.
	 *
	 * @param featureMetadata the feature to index; {@code null} is ignored
	 */
	void indexFeature(FeatureMetadata featureMetadata);

	/**
	 * Indexes a single operation.
	 *
	 * @param operationMetadata the operation to index; {@code null} is ignored
	 */
	void indexOperation(OperationMetadata operationMetadata);

	/**
	 * Removes a package and everything below it from the index.
	 *
	 * @param packageMetadata the package to remove; {@code null} is ignored
	 */
	void removePackage(PackageMetadata packageMetadata);

	/**
	 * Removes a class with its features and operations from the index.
	 *
	 * @param classMetadata the class to remove; {@code null} is ignored
	 */
	void removeClass(ClassMetadata classMetadata);

	/**
	 * Removes a feature from the index.
	 *
	 * @param featureMetadata the feature to remove; {@code null} is ignored
	 */
	void removeFeature(FeatureMetadata featureMetadata);

	/**
	 * Removes an operation from the index.
	 *
	 * @param operationMetadata the operation to remove; {@code null} is ignored
	 */
	void removeOperation(OperationMetadata operationMetadata);

	/**
	 * Drops every entry. Called when the index is replaced or the service shuts down.
	 */
	void clear();
}
