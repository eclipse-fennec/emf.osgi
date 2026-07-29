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

import org.eclipse.emf.ecore.EOperation;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Operation Metadata</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Pre-computed metadata for an EOperation: cached properties, the resolved return-type metadata, metadata for all EParameters, and the aspect entries contributed by providers. Contained by ClassMetadata.operations.
 * 
 * The operation level is a first-class attachment point (decision M8): it costs one containment feature now and a model version bump later, and it has plausible first users - constraint expressions attached to operations, for instance.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getClassMetadata <em>Class Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getEOperation <em>EOperation</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getOperationID <em>Operation ID</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getReturnTypeMetadata <em>Return Type Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getParameters <em>Parameters</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getAspects <em>Aspects</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getOperationMetadata()
 * @model
 * @generated
 */
@ProviderType
public interface OperationMetadata extends DiagnosticContainer {
	/**
	 * Returns the value of the '<em><b>Class Metadata</b></em>' container reference.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getOperations <em>Operations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The parent ClassMetadata. Bidirectional opposite of ClassMetadata.operations. Navigate to classMetadata.getPackage() to reach the PackageMetadata.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Class Metadata</em>' container reference.
	 * @see #setClassMetadata(ClassMetadata)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getOperationMetadata_ClassMetadata()
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getOperations
	 * @model opposite="operations" transient="false"
	 * @generated
	 */
	ClassMetadata getClassMetadata();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getClassMetadata <em>Class Metadata</em>}' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Class Metadata</em>' container reference.
	 * @see #getClassMetadata()
	 * @generated
	 */
	void setClassMetadata(ClassMetadata value);

	/**
	 * Returns the value of the '<em><b>EOperation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The EOperation this metadata describes.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>EOperation</em>' reference.
	 * @see #setEOperation(EOperation)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getOperationMetadata_EOperation()
	 * @model
	 * @generated
	 */
	EOperation getEOperation();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getEOperation <em>EOperation</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>EOperation</em>' reference.
	 * @see #getEOperation()
	 * @generated
	 */
	void setEOperation(EOperation value);

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Cached operation name. Avoids repeated eOperation.getName() calls.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getOperationMetadata_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Operation ID</b></em>' attribute.
	 * The default value is <code>"-1"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Cached operation ID from the EClass, for fast operation lookup by numeric ID. Value -1 indicates uninitialized.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Operation ID</em>' attribute.
	 * @see #setOperationID(int)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getOperationMetadata_OperationID()
	 * @model default="-1"
	 * @generated
	 */
	int getOperationID();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getOperationID <em>Operation ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Operation ID</em>' attribute.
	 * @see #getOperationID()
	 * @generated
	 */
	void setOperationID(int value);

	/**
	 * Returns the value of the '<em><b>Return Type Metadata</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Pre-resolved metadata for the EOperation return type, when the return EClassifier is an EClass in a registered package. Null for void operations, EDataType returns, or unregistered target packages.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Return Type Metadata</em>' reference.
	 * @see #setReturnTypeMetadata(ClassMetadata)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getOperationMetadata_ReturnTypeMetadata()
	 * @model
	 * @generated
	 */
	ClassMetadata getReturnTypeMetadata();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getReturnTypeMetadata <em>Return Type Metadata</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Return Type Metadata</em>' reference.
	 * @see #getReturnTypeMetadata()
	 * @generated
	 */
	void setReturnTypeMetadata(ClassMetadata value);

	/**
	 * Returns the value of the '<em><b>Parameters</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata}.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getOperationMetadata <em>Operation Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Metadata for all EParameters of this EOperation, in declaration order. Bidirectional: each ParameterMetadata has a back-reference via ParameterMetadata.operationMetadata.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Parameters</em>' containment reference list.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getOperationMetadata_Parameters()
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getOperationMetadata
	 * @model opposite="operationMetadata" containment="true"
	 * @generated
	 */
	EList<ParameterMetadata> getParameters();

	/**
	 * Returns the value of the '<em><b>Aspects</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Aspect entries attached to this operation by providers, at most one per typeId.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Aspects</em>' containment reference list.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getOperationMetadata_Aspects()
	 * @model containment="true"
	 * @generated
	 */
	EList<AspectEntry> getAspects();

} // OperationMetadata
