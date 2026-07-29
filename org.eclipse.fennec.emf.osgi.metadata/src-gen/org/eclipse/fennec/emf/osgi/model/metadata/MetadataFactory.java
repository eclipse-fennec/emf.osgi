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

import org.eclipse.emf.ecore.EFactory;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage
 * @generated
 */
@ProviderType
public interface MetadataFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	MetadataFactory eINSTANCE = org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Diagnostic</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Diagnostic</em>'.
	 * @generated
	 */
	MetadataDiagnostic createMetadataDiagnostic();

	/**
	 * Returns a new object of class '<em>Aspect Entry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Aspect Entry</em>'.
	 * @generated
	 */
	AspectEntry createAspectEntry();

	/**
	 * Returns a new object of class '<em>Package Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Package Metadata</em>'.
	 * @generated
	 */
	PackageMetadata createPackageMetadata();

	/**
	 * Returns a new object of class '<em>Class Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Class Metadata</em>'.
	 * @generated
	 */
	ClassMetadata createClassMetadata();

	/**
	 * Returns a new object of class '<em>Attribute Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Attribute Metadata</em>'.
	 * @generated
	 */
	AttributeMetadata createAttributeMetadata();

	/**
	 * Returns a new object of class '<em>Reference Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Reference Metadata</em>'.
	 * @generated
	 */
	ReferenceMetadata createReferenceMetadata();

	/**
	 * Returns a new object of class '<em>Operation Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Operation Metadata</em>'.
	 * @generated
	 */
	OperationMetadata createOperationMetadata();

	/**
	 * Returns a new object of class '<em>Parameter Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Parameter Metadata</em>'.
	 * @generated
	 */
	ParameterMetadata createParameterMetadata();

	/**
	 * Returns a new object of class '<em>Registry</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Registry</em>'.
	 * @generated
	 */
	MetadataRegistry createMetadataRegistry();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	MetadataPackage getMetadataPackage();

} //MetadataFactory
