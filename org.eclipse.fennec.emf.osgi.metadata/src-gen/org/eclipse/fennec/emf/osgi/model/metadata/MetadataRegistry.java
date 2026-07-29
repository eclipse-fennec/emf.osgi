/*
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
 */
package org.eclipse.fennec.emf.osgi.model.metadata;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Registry</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Root container for pre-computed package metadata. The serializable root for persisting or caching metadata state; contains PackageMetadata instances keyed by their fingerprint, so several versions of the same nsURI coexist.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry#getPackages <em>Packages</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getMetadataRegistry()
 * @model
 * @generated
 */
@ProviderType
public interface MetadataRegistry extends EObject {
	/**
	 * Returns the value of the '<em><b>Packages</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * All contained PackageMetadata instances. Each holds the complete metadata tree for one model version (classes, features, operations, aspect entries).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Packages</em>' containment reference list.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getMetadataRegistry_Packages()
	 * @model containment="true"
	 * @generated
	 */
	EList<PackageMetadata> getPackages();

} // MetadataRegistry
