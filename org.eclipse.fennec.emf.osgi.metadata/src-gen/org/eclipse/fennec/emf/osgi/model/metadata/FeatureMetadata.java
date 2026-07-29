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

import org.eclipse.emf.ecore.EStructuralFeature;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Feature Metadata</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Abstract base for pre-computed metadata about an EStructuralFeature: cached properties and the aspect entries contributed by providers. Concrete subclasses: AttributeMetadata for EAttribute, ReferenceMetadata for EReference. Contained by ClassMetadata.features.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getClassMetadata <em>Class Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getEFeature <em>EFeature</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getExtendedMetaDataName <em>Extended Meta Data Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getFeatureID <em>Feature ID</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getAspects <em>Aspects</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getFeatureMetadata()
 * @model abstract="true"
 * @generated
 */
@ProviderType
public interface FeatureMetadata extends DiagnosticContainer {
	/**
	 * Returns the value of the '<em><b>Class Metadata</b></em>' container reference.
	 * It is bidirectional and its opposite is '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getFeatures <em>Features</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The parent ClassMetadata. Bidirectional opposite of ClassMetadata.features. Navigate to classMetadata.getPackage() to reach the PackageMetadata.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Class Metadata</em>' container reference.
	 * @see #setClassMetadata(ClassMetadata)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getFeatureMetadata_ClassMetadata()
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getFeatures
	 * @model opposite="features" transient="false"
	 * @generated
	 */
	ClassMetadata getClassMetadata();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getClassMetadata <em>Class Metadata</em>}' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Class Metadata</em>' container reference.
	 * @see #getClassMetadata()
	 * @generated
	 */
	void setClassMetadata(ClassMetadata value);

	/**
	 * Returns the value of the '<em><b>EFeature</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The EStructuralFeature this metadata describes.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>EFeature</em>' reference.
	 * @see #setEFeature(EStructuralFeature)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getFeatureMetadata_EFeature()
	 * @model
	 * @generated
	 */
	EStructuralFeature getEFeature();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getEFeature <em>EFeature</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>EFeature</em>' reference.
	 * @see #getEFeature()
	 * @generated
	 */
	void setEFeature(EStructuralFeature value);

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Cached feature name. Avoids repeated eFeature.getName() calls.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getFeatureMetadata_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Extended Meta Data Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Alternative name from the EMF ExtendedMetaData annotation (source: http://www.eclipse.org/emf/2002/Ecore). Present in XSD-generated models where the XML element/attribute name differs from the Java-friendly EMF feature name. Null if no ExtendedMetaData 'name' annotation exists.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Extended Meta Data Name</em>' attribute.
	 * @see #setExtendedMetaDataName(String)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getFeatureMetadata_ExtendedMetaDataName()
	 * @model
	 * @generated
	 */
	String getExtendedMetaDataName();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getExtendedMetaDataName <em>Extended Meta Data Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Extended Meta Data Name</em>' attribute.
	 * @see #getExtendedMetaDataName()
	 * @generated
	 */
	void setExtendedMetaDataName(String value);

	/**
	 * Returns the value of the '<em><b>Feature ID</b></em>' attribute.
	 * The default value is <code>"-1"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Cached feature ID from the EClass, for fast feature lookup by numeric ID. Value -1 indicates uninitialized.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Feature ID</em>' attribute.
	 * @see #setFeatureID(int)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getFeatureMetadata_FeatureID()
	 * @model default="-1"
	 * @generated
	 */
	int getFeatureID();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getFeatureID <em>Feature ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Feature ID</em>' attribute.
	 * @see #getFeatureID()
	 * @generated
	 */
	void setFeatureID(int value);

	/**
	 * Returns the value of the '<em><b>Aspects</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Aspect entries attached to this feature by providers, at most one per typeId.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Aspects</em>' containment reference list.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getFeatureMetadata_Aspects()
	 * @model containment="true"
	 * @generated
	 */
	EList<AspectEntry> getAspects();

} // FeatureMetadata
