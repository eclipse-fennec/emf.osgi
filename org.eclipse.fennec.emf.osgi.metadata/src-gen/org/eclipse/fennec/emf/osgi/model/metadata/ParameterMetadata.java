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

import org.eclipse.emf.ecore.EParameter;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Parameter Metadata</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Pre-computed metadata for an EParameter of an EOperation: cached properties and the resolved parameter-type metadata. Contained by OperationMetadata.parameters.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getOperationMetadata <em>Operation Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getEParameter <em>EParameter</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getTypeMetadata <em>Type Metadata</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getParameterMetadata()
 * @model
 * @generated
 */
@ProviderType
public interface ParameterMetadata extends DiagnosticContainer {
	/**
	 * Returns the value of the '<em><b>Operation Metadata</b></em>' container reference.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getParameters <em>Parameters</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The parent OperationMetadata. Bidirectional opposite of OperationMetadata.parameters.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Operation Metadata</em>' container reference.
	 * @see #setOperationMetadata(OperationMetadata)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getParameterMetadata_OperationMetadata()
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getParameters
	 * @model opposite="parameters" transient="false"
	 * @generated
	 */
	OperationMetadata getOperationMetadata();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getOperationMetadata <em>Operation Metadata</em>}' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Operation Metadata</em>' container reference.
	 * @see #getOperationMetadata()
	 * @generated
	 */
	void setOperationMetadata(OperationMetadata value);

	/**
	 * Returns the value of the '<em><b>EParameter</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The EParameter this metadata describes.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>EParameter</em>' reference.
	 * @see #setEParameter(EParameter)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getParameterMetadata_EParameter()
	 * @model
	 * @generated
	 */
	EParameter getEParameter();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getEParameter <em>EParameter</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>EParameter</em>' reference.
	 * @see #getEParameter()
	 * @generated
	 */
	void setEParameter(EParameter value);

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Cached parameter name. Avoids repeated eParameter.getName() calls.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getParameterMetadata_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Type Metadata</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Pre-resolved metadata for the parameter type, when the parameter EClassifier is an EClass in a registered package. Null for EDataType parameters or unregistered target packages.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Type Metadata</em>' reference.
	 * @see #setTypeMetadata(ClassMetadata)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getParameterMetadata_TypeMetadata()
	 * @model
	 * @generated
	 */
	ClassMetadata getTypeMetadata();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getTypeMetadata <em>Type Metadata</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type Metadata</em>' reference.
	 * @see #getTypeMetadata()
	 * @generated
	 */
	void setTypeMetadata(ClassMetadata value);

} // ParameterMetadata
