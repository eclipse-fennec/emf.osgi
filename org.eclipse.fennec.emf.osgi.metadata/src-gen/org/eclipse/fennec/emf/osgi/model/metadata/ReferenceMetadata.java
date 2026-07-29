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

import org.eclipse.emf.ecore.EReference;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Reference Metadata</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Pre-computed metadata for an EReference. Extends FeatureMetadata with reference-specific properties: containment status, resolved target class and resolved bidirectional opposite.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getEReference <em>EReference</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#isContainment <em>Containment</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getTargetClassMetadata <em>Target Class Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getOppositeMetadata <em>Opposite Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#isHasBidirectional <em>Has Bidirectional</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getReferenceMetadata()
 * @model
 * @generated
 */
@ProviderType
public interface ReferenceMetadata extends FeatureMetadata {
	/**
	 * Returns the value of the '<em><b>EReference</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The EReference this metadata describes. Typed convenience reference; the base class eFeature holds the same value as EStructuralFeature.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>EReference</em>' reference.
	 * @see #setEReference(EReference)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getReferenceMetadata_EReference()
	 * @model
	 * @generated
	 */
	EReference getEReference();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getEReference <em>EReference</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>EReference</em>' reference.
	 * @see #getEReference()
	 * @generated
	 */
	void setEReference(EReference value);

	/**
	 * Returns the value of the '<em><b>Containment</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Whether this is a containment reference. Containment references own the target object, non-containment references point to objects owned elsewhere.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Containment</em>' attribute.
	 * @see #setContainment(boolean)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getReferenceMetadata_Containment()
	 * @model default="false"
	 * @generated
	 */
	boolean isContainment();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#isContainment <em>Containment</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Containment</em>' attribute.
	 * @see #isContainment()
	 * @generated
	 */
	void setContainment(boolean value);

	/**
	 * Returns the value of the '<em><b>Target Class Metadata</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Pre-resolved metadata for the target EClass of this reference. Resolved during package registration after all ClassMetadata are created. Null if the target class is in an unregistered package.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Target Class Metadata</em>' reference.
	 * @see #setTargetClassMetadata(ClassMetadata)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getReferenceMetadata_TargetClassMetadata()
	 * @model
	 * @generated
	 */
	ClassMetadata getTargetClassMetadata();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getTargetClassMetadata <em>Target Class Metadata</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target Class Metadata</em>' reference.
	 * @see #getTargetClassMetadata()
	 * @generated
	 */
	void setTargetClassMetadata(ClassMetadata value);

	/**
	 * Returns the value of the '<em><b>Opposite Metadata</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Pre-resolved metadata for the opposite EReference in a bidirectional association. Null if this reference has no opposite. Resolved during package registration.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Opposite Metadata</em>' reference.
	 * @see #setOppositeMetadata(ReferenceMetadata)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getReferenceMetadata_OppositeMetadata()
	 * @model
	 * @generated
	 */
	ReferenceMetadata getOppositeMetadata();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getOppositeMetadata <em>Opposite Metadata</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Opposite Metadata</em>' reference.
	 * @see #getOppositeMetadata()
	 * @generated
	 */
	void setOppositeMetadata(ReferenceMetadata value);

	/**
	 * Returns the value of the '<em><b>Has Bidirectional</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Whether this reference has an opposite reference. Quick check to avoid null-checking oppositeMetadata.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Has Bidirectional</em>' attribute.
	 * @see #setHasBidirectional(boolean)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getReferenceMetadata_HasBidirectional()
	 * @model default="false"
	 * @generated
	 */
	boolean isHasBidirectional();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#isHasBidirectional <em>Has Bidirectional</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Has Bidirectional</em>' attribute.
	 * @see #isHasBidirectional()
	 * @generated
	 */
	void setHasBidirectional(boolean value);

} // ReferenceMetadata
