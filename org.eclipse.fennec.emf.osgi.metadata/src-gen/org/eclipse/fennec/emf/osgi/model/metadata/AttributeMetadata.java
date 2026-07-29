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

import org.eclipse.emf.ecore.EAttribute;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Attribute Metadata</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Pre-computed metadata for an EAttribute. Extends FeatureMetadata with attribute-specific properties such as eID status and the cached default value.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#getEAttribute <em>EAttribute</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#isIsId <em>Is Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#getDefaultValue <em>Default Value</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAttributeMetadata()
 * @model
 * @generated
 */
@ProviderType
public interface AttributeMetadata extends FeatureMetadata {
	/**
	 * Returns the value of the '<em><b>EAttribute</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The EAttribute this metadata describes. Typed convenience reference; the base class eFeature holds the same value as EStructuralFeature.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>EAttribute</em>' reference.
	 * @see #setEAttribute(EAttribute)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAttributeMetadata_EAttribute()
	 * @model
	 * @generated
	 */
	EAttribute getEAttribute();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#getEAttribute <em>EAttribute</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>EAttribute</em>' reference.
	 * @see #getEAttribute()
	 * @generated
	 */
	void setEAttribute(EAttribute value);

	/**
	 * Returns the value of the '<em><b>Is Id</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Whether this EAttribute is marked as eID in the Ecore model. Cached for fast ID feature resolution.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Is Id</em>' attribute.
	 * @see #setIsId(boolean)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAttributeMetadata_IsId()
	 * @model default="false"
	 * @generated
	 */
	boolean isIsId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#isIsId <em>Is Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Is Id</em>' attribute.
	 * @see #isIsId()
	 * @generated
	 */
	void setIsId(boolean value);

	/**
	 * Returns the value of the '<em><b>Default Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Cached default value of the EAttribute, so consumers can compare against it without recomputing it on every access.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Default Value</em>' attribute.
	 * @see #setDefaultValue(Object)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAttributeMetadata_DefaultValue()
	 * @model
	 * @generated
	 */
	Object getDefaultValue();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#getDefaultValue <em>Default Value</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Default Value</em>' attribute.
	 * @see #getDefaultValue()
	 * @generated
	 */
	void setDefaultValue(Object value);

} // AttributeMetadata
