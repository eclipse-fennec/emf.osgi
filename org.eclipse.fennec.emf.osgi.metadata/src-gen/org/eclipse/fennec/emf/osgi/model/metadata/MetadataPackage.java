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
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.fennec.emf.osgi.annotation.provide.EPackage;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * Pre-computed metadata mirror tree for EPackages. The tree mirrors the Ecore structure (package, class, feature, operation, parameter) with the values a consumer would otherwise recompute on every access, and carries cross-cutting content through generic aspect entries.
 * 
 * Cut from the model.metadata donor model for the migration into emf.osgi (issue #60): the codec vocabulary (Base*Config, SerializationFormat, TypeStrategy, IdStrategy, IdKeyMode, SuperTypeSelection, EnumSerializationStrategy) and the *Profile hierarchy return to the codec repository, and the typed aspect hierarchy (Aspect/PackageAspect/ClassAspect/FeatureAspect/OperationAspect) is replaced by composition through AspectEntry.
 * <!-- end-model-doc -->
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory
 * @model kind="package"
 *        annotation="Version value='1.0'"
 * @generated
 */
@ProviderType
@EPackage(uri = MetadataPackage.eNS_URI, fingerprint = "fp1:01c813556f7191a1720500470a133c6500dd4934395213e12a8d75e01a319473", genModel = "/model/metadata.genmodel", genModelSourceLocations = {"model/metadata.genmodel","org.eclipse.fennec.emf.osgi.metadata/model/metadata.genmodel"}, ecore = "/model/metadata.ecore", ecoreSourceLocations = "/model/metadata.ecore")
public interface MetadataPackage extends org.eclipse.emf.ecore.EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "metadata";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "https://eclipse.org/fennec/emf/osgi/metadata/1.0.0";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "metadata";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	MetadataPackage eINSTANCE = org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl.init();

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataDiagnosticImpl <em>Diagnostic</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataDiagnosticImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getMetadataDiagnostic()
	 * @generated
	 */
	int METADATA_DIAGNOSTIC = 0;

	/**
	 * The feature id for the '<em><b>Severity</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int METADATA_DIAGNOSTIC__SEVERITY = 0;

	/**
	 * The feature id for the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int METADATA_DIAGNOSTIC__MESSAGE = 1;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int METADATA_DIAGNOSTIC__KEY = 2;

	/**
	 * The number of structural features of the '<em>Diagnostic</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int METADATA_DIAGNOSTIC_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Diagnostic</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int METADATA_DIAGNOSTIC_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer <em>Diagnostic Container</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getDiagnosticContainer()
	 * @generated
	 */
	int DIAGNOSTIC_CONTAINER = 1;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC_CONTAINER__DIAGNOSTICS = 0;

	/**
	 * The feature id for the '<em><b>All Diagnostics</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC_CONTAINER__ALL_DIAGNOSTICS = 1;

	/**
	 * The number of structural features of the '<em>Diagnostic Container</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC_CONTAINER_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Diagnostic Container</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DIAGNOSTIC_CONTAINER_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.AspectEntryImpl <em>Aspect Entry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.AspectEntryImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getAspectEntry()
	 * @generated
	 */
	int ASPECT_ENTRY = 2;

	/**
	 * The feature id for the '<em><b>Type Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASPECT_ENTRY__TYPE_ID = 0;

	/**
	 * The feature id for the '<em><b>Content</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASPECT_ENTRY__CONTENT = 1;

	/**
	 * The feature id for the '<em><b>Transient Content</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASPECT_ENTRY__TRANSIENT_CONTENT = 2;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASPECT_ENTRY__DIAGNOSTICS = 3;

	/**
	 * The number of structural features of the '<em>Aspect Entry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASPECT_ENTRY_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Aspect Entry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASPECT_ENTRY_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.PackageMetadataImpl <em>Package Metadata</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.PackageMetadataImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getPackageMetadata()
	 * @generated
	 */
	int PACKAGE_METADATA = 3;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA__DIAGNOSTICS = DIAGNOSTIC_CONTAINER__DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>All Diagnostics</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA__ALL_DIAGNOSTICS = DIAGNOSTIC_CONTAINER__ALL_DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>EPackage</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA__EPACKAGE = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Ns URI</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA__NS_URI = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Model Fingerprint</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA__MODEL_FINGERPRINT = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Classes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA__CLASSES = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Aspects</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA__ASPECTS = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA__PROPERTIES = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 5;

	/**
	 * The number of structural features of the '<em>Package Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA_FEATURE_COUNT = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 6;

	/**
	 * The number of operations of the '<em>Package Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PACKAGE_METADATA_OPERATION_COUNT = DIAGNOSTIC_CONTAINER_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl <em>Class Metadata</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getClassMetadata()
	 * @generated
	 */
	int CLASS_METADATA = 4;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__DIAGNOSTICS = DIAGNOSTIC_CONTAINER__DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>All Diagnostics</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__ALL_DIAGNOSTICS = DIAGNOSTIC_CONTAINER__ALL_DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>Package</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__PACKAGE = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>EClass</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__ECLASS = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__NAME = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Classifier ID</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__CLASSIFIER_ID = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Type URI</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__TYPE_URI = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Features</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__FEATURES = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Operations</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__OPERATIONS = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 6;

	/**
	 * The feature id for the '<em><b>Super Types</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__SUPER_TYPES = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 7;

	/**
	 * The feature id for the '<em><b>All Super Types</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__ALL_SUPER_TYPES = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 8;

	/**
	 * The feature id for the '<em><b>Id Features</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__ID_FEATURES = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 9;

	/**
	 * The feature id for the '<em><b>Has Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__HAS_ID = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 10;

	/**
	 * The feature id for the '<em><b>Aspects</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA__ASPECTS = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 11;

	/**
	 * The number of structural features of the '<em>Class Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA_FEATURE_COUNT = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 12;

	/**
	 * The number of operations of the '<em>Class Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CLASS_METADATA_OPERATION_COUNT = DIAGNOSTIC_CONTAINER_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl <em>Feature Metadata</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getFeatureMetadata()
	 * @generated
	 */
	int FEATURE_METADATA = 5;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA__DIAGNOSTICS = DIAGNOSTIC_CONTAINER__DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>All Diagnostics</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA__ALL_DIAGNOSTICS = DIAGNOSTIC_CONTAINER__ALL_DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>Class Metadata</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA__CLASS_METADATA = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>EFeature</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA__EFEATURE = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA__NAME = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Extended Meta Data Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA__EXTENDED_META_DATA_NAME = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Feature ID</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA__FEATURE_ID = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Aspects</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA__ASPECTS = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 5;

	/**
	 * The number of structural features of the '<em>Feature Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA_FEATURE_COUNT = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 6;

	/**
	 * The number of operations of the '<em>Feature Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FEATURE_METADATA_OPERATION_COUNT = DIAGNOSTIC_CONTAINER_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.AttributeMetadataImpl <em>Attribute Metadata</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.AttributeMetadataImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getAttributeMetadata()
	 * @generated
	 */
	int ATTRIBUTE_METADATA = 6;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__DIAGNOSTICS = FEATURE_METADATA__DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>All Diagnostics</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__ALL_DIAGNOSTICS = FEATURE_METADATA__ALL_DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>Class Metadata</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__CLASS_METADATA = FEATURE_METADATA__CLASS_METADATA;

	/**
	 * The feature id for the '<em><b>EFeature</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__EFEATURE = FEATURE_METADATA__EFEATURE;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__NAME = FEATURE_METADATA__NAME;

	/**
	 * The feature id for the '<em><b>Extended Meta Data Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__EXTENDED_META_DATA_NAME = FEATURE_METADATA__EXTENDED_META_DATA_NAME;

	/**
	 * The feature id for the '<em><b>Feature ID</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__FEATURE_ID = FEATURE_METADATA__FEATURE_ID;

	/**
	 * The feature id for the '<em><b>Aspects</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__ASPECTS = FEATURE_METADATA__ASPECTS;

	/**
	 * The feature id for the '<em><b>EAttribute</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__EATTRIBUTE = FEATURE_METADATA_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Is Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__IS_ID = FEATURE_METADATA_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Default Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA__DEFAULT_VALUE = FEATURE_METADATA_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Attribute Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA_FEATURE_COUNT = FEATURE_METADATA_FEATURE_COUNT + 3;

	/**
	 * The number of operations of the '<em>Attribute Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ATTRIBUTE_METADATA_OPERATION_COUNT = FEATURE_METADATA_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl <em>Reference Metadata</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getReferenceMetadata()
	 * @generated
	 */
	int REFERENCE_METADATA = 7;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__DIAGNOSTICS = FEATURE_METADATA__DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>All Diagnostics</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__ALL_DIAGNOSTICS = FEATURE_METADATA__ALL_DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>Class Metadata</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__CLASS_METADATA = FEATURE_METADATA__CLASS_METADATA;

	/**
	 * The feature id for the '<em><b>EFeature</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__EFEATURE = FEATURE_METADATA__EFEATURE;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__NAME = FEATURE_METADATA__NAME;

	/**
	 * The feature id for the '<em><b>Extended Meta Data Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__EXTENDED_META_DATA_NAME = FEATURE_METADATA__EXTENDED_META_DATA_NAME;

	/**
	 * The feature id for the '<em><b>Feature ID</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__FEATURE_ID = FEATURE_METADATA__FEATURE_ID;

	/**
	 * The feature id for the '<em><b>Aspects</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__ASPECTS = FEATURE_METADATA__ASPECTS;

	/**
	 * The feature id for the '<em><b>EReference</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__EREFERENCE = FEATURE_METADATA_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Containment</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__CONTAINMENT = FEATURE_METADATA_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Target Class Metadata</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__TARGET_CLASS_METADATA = FEATURE_METADATA_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Opposite Metadata</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__OPPOSITE_METADATA = FEATURE_METADATA_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Has Bidirectional</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA__HAS_BIDIRECTIONAL = FEATURE_METADATA_FEATURE_COUNT + 4;

	/**
	 * The number of structural features of the '<em>Reference Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA_FEATURE_COUNT = FEATURE_METADATA_FEATURE_COUNT + 5;

	/**
	 * The number of operations of the '<em>Reference Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REFERENCE_METADATA_OPERATION_COUNT = FEATURE_METADATA_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl <em>Operation Metadata</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getOperationMetadata()
	 * @generated
	 */
	int OPERATION_METADATA = 8;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__DIAGNOSTICS = DIAGNOSTIC_CONTAINER__DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>All Diagnostics</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__ALL_DIAGNOSTICS = DIAGNOSTIC_CONTAINER__ALL_DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>Class Metadata</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__CLASS_METADATA = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>EOperation</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__EOPERATION = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__NAME = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Operation ID</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__OPERATION_ID = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 3;

	/**
	 * The feature id for the '<em><b>Return Type Metadata</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__RETURN_TYPE_METADATA = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 4;

	/**
	 * The feature id for the '<em><b>Parameters</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__PARAMETERS = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 5;

	/**
	 * The feature id for the '<em><b>Aspects</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA__ASPECTS = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 6;

	/**
	 * The number of structural features of the '<em>Operation Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA_FEATURE_COUNT = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 7;

	/**
	 * The number of operations of the '<em>Operation Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OPERATION_METADATA_OPERATION_COUNT = DIAGNOSTIC_CONTAINER_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl <em>Parameter Metadata</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getParameterMetadata()
	 * @generated
	 */
	int PARAMETER_METADATA = 9;

	/**
	 * The feature id for the '<em><b>Diagnostics</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_METADATA__DIAGNOSTICS = DIAGNOSTIC_CONTAINER__DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>All Diagnostics</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_METADATA__ALL_DIAGNOSTICS = DIAGNOSTIC_CONTAINER__ALL_DIAGNOSTICS;

	/**
	 * The feature id for the '<em><b>Operation Metadata</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_METADATA__OPERATION_METADATA = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>EParameter</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_METADATA__EPARAMETER = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_METADATA__NAME = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 2;

	/**
	 * The feature id for the '<em><b>Type Metadata</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_METADATA__TYPE_METADATA = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 3;

	/**
	 * The number of structural features of the '<em>Parameter Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_METADATA_FEATURE_COUNT = DIAGNOSTIC_CONTAINER_FEATURE_COUNT + 4;

	/**
	 * The number of operations of the '<em>Parameter Metadata</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PARAMETER_METADATA_OPERATION_COUNT = DIAGNOSTIC_CONTAINER_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataRegistryImpl <em>Registry</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataRegistryImpl
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getMetadataRegistry()
	 * @generated
	 */
	int METADATA_REGISTRY = 10;

	/**
	 * The feature id for the '<em><b>Packages</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int METADATA_REGISTRY__PACKAGES = 0;

	/**
	 * The number of structural features of the '<em>Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int METADATA_REGISTRY_FEATURE_COUNT = 1;

	/**
	 * The number of operations of the '<em>Registry</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int METADATA_REGISTRY_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity <em>Diagnostic Severity</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getDiagnosticSeverity()
	 * @generated
	 */
	int DIAGNOSTIC_SEVERITY = 11;


	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic <em>Diagnostic</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Diagnostic</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic
	 * @generated
	 */
	EClass getMetadataDiagnostic();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getSeverity <em>Severity</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Severity</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getSeverity()
	 * @see #getMetadataDiagnostic()
	 * @generated
	 */
	EAttribute getMetadataDiagnostic_Severity();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getMessage <em>Message</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Message</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getMessage()
	 * @see #getMetadataDiagnostic()
	 * @generated
	 */
	EAttribute getMetadataDiagnostic_Message();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getKey <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getKey()
	 * @see #getMetadataDiagnostic()
	 * @generated
	 */
	EAttribute getMetadataDiagnostic_Key();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer <em>Diagnostic Container</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Diagnostic Container</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer
	 * @generated
	 */
	EClass getDiagnosticContainer();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer#getDiagnostics <em>Diagnostics</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Diagnostics</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer#getDiagnostics()
	 * @see #getDiagnosticContainer()
	 * @generated
	 */
	EReference getDiagnosticContainer_Diagnostics();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer#getAllDiagnostics <em>All Diagnostics</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>All Diagnostics</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer#getAllDiagnostics()
	 * @see #getDiagnosticContainer()
	 * @generated
	 */
	EReference getDiagnosticContainer_AllDiagnostics();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry <em>Aspect Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Aspect Entry</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry
	 * @generated
	 */
	EClass getAspectEntry();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getTypeId <em>Type Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type Id</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getTypeId()
	 * @see #getAspectEntry()
	 * @generated
	 */
	EAttribute getAspectEntry_TypeId();

	/**
	 * Returns the meta object for the containment reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getContent <em>Content</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Content</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getContent()
	 * @see #getAspectEntry()
	 * @generated
	 */
	EReference getAspectEntry_Content();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getTransientContent <em>Transient Content</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Transient Content</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getTransientContent()
	 * @see #getAspectEntry()
	 * @generated
	 */
	EAttribute getAspectEntry_TransientContent();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getDiagnostics <em>Diagnostics</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Diagnostics</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getDiagnostics()
	 * @see #getAspectEntry()
	 * @generated
	 */
	EReference getAspectEntry_Diagnostics();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata <em>Package Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Package Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata
	 * @generated
	 */
	EClass getPackageMetadata();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getEPackage <em>EPackage</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>EPackage</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getEPackage()
	 * @see #getPackageMetadata()
	 * @generated
	 */
	EReference getPackageMetadata_EPackage();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getNsURI <em>Ns URI</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ns URI</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getNsURI()
	 * @see #getPackageMetadata()
	 * @generated
	 */
	EAttribute getPackageMetadata_NsURI();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getModelFingerprint <em>Model Fingerprint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Model Fingerprint</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getModelFingerprint()
	 * @see #getPackageMetadata()
	 * @generated
	 */
	EAttribute getPackageMetadata_ModelFingerprint();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getClasses <em>Classes</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Classes</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getClasses()
	 * @see #getPackageMetadata()
	 * @generated
	 */
	EReference getPackageMetadata_Classes();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getAspects <em>Aspects</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Aspects</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getAspects()
	 * @see #getPackageMetadata()
	 * @generated
	 */
	EReference getPackageMetadata_Aspects();

	/**
	 * Returns the meta object for the map '{@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>Properties</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata#getProperties()
	 * @see #getPackageMetadata()
	 * @generated
	 */
	EReference getPackageMetadata_Properties();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata <em>Class Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Class Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata
	 * @generated
	 */
	EClass getClassMetadata();

	/**
	 * Returns the meta object for the container reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getPackage <em>Package</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Package</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getPackage()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EReference getClassMetadata_Package();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getEClass <em>EClass</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>EClass</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getEClass()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EReference getClassMetadata_EClass();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getName()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EAttribute getClassMetadata_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getClassifierID <em>Classifier ID</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Classifier ID</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getClassifierID()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EAttribute getClassMetadata_ClassifierID();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getTypeURI <em>Type URI</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type URI</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getTypeURI()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EAttribute getClassMetadata_TypeURI();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getFeatures <em>Features</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Features</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getFeatures()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EReference getClassMetadata_Features();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getOperations <em>Operations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Operations</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getOperations()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EReference getClassMetadata_Operations();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getSuperTypes <em>Super Types</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Super Types</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getSuperTypes()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EReference getClassMetadata_SuperTypes();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getAllSuperTypes <em>All Super Types</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>All Super Types</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getAllSuperTypes()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EReference getClassMetadata_AllSuperTypes();

	/**
	 * Returns the meta object for the reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getIdFeatures <em>Id Features</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Id Features</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getIdFeatures()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EReference getClassMetadata_IdFeatures();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#isHasId <em>Has Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Has Id</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#isHasId()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EAttribute getClassMetadata_HasId();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getAspects <em>Aspects</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Aspects</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata#getAspects()
	 * @see #getClassMetadata()
	 * @generated
	 */
	EReference getClassMetadata_Aspects();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata <em>Feature Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Feature Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata
	 * @generated
	 */
	EClass getFeatureMetadata();

	/**
	 * Returns the meta object for the container reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getClassMetadata <em>Class Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Class Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getClassMetadata()
	 * @see #getFeatureMetadata()
	 * @generated
	 */
	EReference getFeatureMetadata_ClassMetadata();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getEFeature <em>EFeature</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>EFeature</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getEFeature()
	 * @see #getFeatureMetadata()
	 * @generated
	 */
	EReference getFeatureMetadata_EFeature();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getName()
	 * @see #getFeatureMetadata()
	 * @generated
	 */
	EAttribute getFeatureMetadata_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getExtendedMetaDataName <em>Extended Meta Data Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Extended Meta Data Name</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getExtendedMetaDataName()
	 * @see #getFeatureMetadata()
	 * @generated
	 */
	EAttribute getFeatureMetadata_ExtendedMetaDataName();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getFeatureID <em>Feature ID</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Feature ID</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getFeatureID()
	 * @see #getFeatureMetadata()
	 * @generated
	 */
	EAttribute getFeatureMetadata_FeatureID();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getAspects <em>Aspects</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Aspects</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata#getAspects()
	 * @see #getFeatureMetadata()
	 * @generated
	 */
	EReference getFeatureMetadata_Aspects();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata <em>Attribute Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Attribute Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata
	 * @generated
	 */
	EClass getAttributeMetadata();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#getEAttribute <em>EAttribute</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>EAttribute</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#getEAttribute()
	 * @see #getAttributeMetadata()
	 * @generated
	 */
	EReference getAttributeMetadata_EAttribute();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#isIsId <em>Is Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Is Id</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#isIsId()
	 * @see #getAttributeMetadata()
	 * @generated
	 */
	EAttribute getAttributeMetadata_IsId();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#getDefaultValue <em>Default Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Default Value</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata#getDefaultValue()
	 * @see #getAttributeMetadata()
	 * @generated
	 */
	EAttribute getAttributeMetadata_DefaultValue();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata <em>Reference Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Reference Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata
	 * @generated
	 */
	EClass getReferenceMetadata();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getEReference <em>EReference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>EReference</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getEReference()
	 * @see #getReferenceMetadata()
	 * @generated
	 */
	EReference getReferenceMetadata_EReference();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#isContainment <em>Containment</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Containment</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#isContainment()
	 * @see #getReferenceMetadata()
	 * @generated
	 */
	EAttribute getReferenceMetadata_Containment();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getTargetClassMetadata <em>Target Class Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Target Class Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getTargetClassMetadata()
	 * @see #getReferenceMetadata()
	 * @generated
	 */
	EReference getReferenceMetadata_TargetClassMetadata();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getOppositeMetadata <em>Opposite Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Opposite Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#getOppositeMetadata()
	 * @see #getReferenceMetadata()
	 * @generated
	 */
	EReference getReferenceMetadata_OppositeMetadata();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#isHasBidirectional <em>Has Bidirectional</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Has Bidirectional</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata#isHasBidirectional()
	 * @see #getReferenceMetadata()
	 * @generated
	 */
	EAttribute getReferenceMetadata_HasBidirectional();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata <em>Operation Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Operation Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata
	 * @generated
	 */
	EClass getOperationMetadata();

	/**
	 * Returns the meta object for the container reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getClassMetadata <em>Class Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Class Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getClassMetadata()
	 * @see #getOperationMetadata()
	 * @generated
	 */
	EReference getOperationMetadata_ClassMetadata();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getEOperation <em>EOperation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>EOperation</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getEOperation()
	 * @see #getOperationMetadata()
	 * @generated
	 */
	EReference getOperationMetadata_EOperation();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getName()
	 * @see #getOperationMetadata()
	 * @generated
	 */
	EAttribute getOperationMetadata_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getOperationID <em>Operation ID</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Operation ID</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getOperationID()
	 * @see #getOperationMetadata()
	 * @generated
	 */
	EAttribute getOperationMetadata_OperationID();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getReturnTypeMetadata <em>Return Type Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Return Type Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getReturnTypeMetadata()
	 * @see #getOperationMetadata()
	 * @generated
	 */
	EReference getOperationMetadata_ReturnTypeMetadata();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getParameters <em>Parameters</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Parameters</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getParameters()
	 * @see #getOperationMetadata()
	 * @generated
	 */
	EReference getOperationMetadata_Parameters();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getAspects <em>Aspects</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Aspects</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata#getAspects()
	 * @see #getOperationMetadata()
	 * @generated
	 */
	EReference getOperationMetadata_Aspects();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata <em>Parameter Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Parameter Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata
	 * @generated
	 */
	EClass getParameterMetadata();

	/**
	 * Returns the meta object for the container reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getOperationMetadata <em>Operation Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Operation Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getOperationMetadata()
	 * @see #getParameterMetadata()
	 * @generated
	 */
	EReference getParameterMetadata_OperationMetadata();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getEParameter <em>EParameter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>EParameter</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getEParameter()
	 * @see #getParameterMetadata()
	 * @generated
	 */
	EReference getParameterMetadata_EParameter();

	/**
	 * Returns the meta object for the attribute '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getName()
	 * @see #getParameterMetadata()
	 * @generated
	 */
	EAttribute getParameterMetadata_Name();

	/**
	 * Returns the meta object for the reference '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getTypeMetadata <em>Type Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Type Metadata</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata#getTypeMetadata()
	 * @see #getParameterMetadata()
	 * @generated
	 */
	EReference getParameterMetadata_TypeMetadata();

	/**
	 * Returns the meta object for class '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry <em>Registry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Registry</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry
	 * @generated
	 */
	EClass getMetadataRegistry();

	/**
	 * Returns the meta object for the containment reference list '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry#getPackages <em>Packages</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Packages</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry#getPackages()
	 * @see #getMetadataRegistry()
	 * @generated
	 */
	EReference getMetadataRegistry_Packages();

	/**
	 * Returns the meta object for enum '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity <em>Diagnostic Severity</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Diagnostic Severity</em>'.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity
	 * @generated
	 */
	EEnum getDiagnosticSeverity();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	MetadataFactory getMetadataFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataDiagnosticImpl <em>Diagnostic</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataDiagnosticImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getMetadataDiagnostic()
		 * @generated
		 */
		EClass METADATA_DIAGNOSTIC = eINSTANCE.getMetadataDiagnostic();

		/**
		 * The meta object literal for the '<em><b>Severity</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute METADATA_DIAGNOSTIC__SEVERITY = eINSTANCE.getMetadataDiagnostic_Severity();

		/**
		 * The meta object literal for the '<em><b>Message</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute METADATA_DIAGNOSTIC__MESSAGE = eINSTANCE.getMetadataDiagnostic_Message();

		/**
		 * The meta object literal for the '<em><b>Key</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute METADATA_DIAGNOSTIC__KEY = eINSTANCE.getMetadataDiagnostic_Key();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer <em>Diagnostic Container</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getDiagnosticContainer()
		 * @generated
		 */
		EClass DIAGNOSTIC_CONTAINER = eINSTANCE.getDiagnosticContainer();

		/**
		 * The meta object literal for the '<em><b>Diagnostics</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DIAGNOSTIC_CONTAINER__DIAGNOSTICS = eINSTANCE.getDiagnosticContainer_Diagnostics();

		/**
		 * The meta object literal for the '<em><b>All Diagnostics</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference DIAGNOSTIC_CONTAINER__ALL_DIAGNOSTICS = eINSTANCE.getDiagnosticContainer_AllDiagnostics();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.AspectEntryImpl <em>Aspect Entry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.AspectEntryImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getAspectEntry()
		 * @generated
		 */
		EClass ASPECT_ENTRY = eINSTANCE.getAspectEntry();

		/**
		 * The meta object literal for the '<em><b>Type Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ASPECT_ENTRY__TYPE_ID = eINSTANCE.getAspectEntry_TypeId();

		/**
		 * The meta object literal for the '<em><b>Content</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ASPECT_ENTRY__CONTENT = eINSTANCE.getAspectEntry_Content();

		/**
		 * The meta object literal for the '<em><b>Transient Content</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ASPECT_ENTRY__TRANSIENT_CONTENT = eINSTANCE.getAspectEntry_TransientContent();

		/**
		 * The meta object literal for the '<em><b>Diagnostics</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ASPECT_ENTRY__DIAGNOSTICS = eINSTANCE.getAspectEntry_Diagnostics();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.PackageMetadataImpl <em>Package Metadata</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.PackageMetadataImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getPackageMetadata()
		 * @generated
		 */
		EClass PACKAGE_METADATA = eINSTANCE.getPackageMetadata();

		/**
		 * The meta object literal for the '<em><b>EPackage</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference PACKAGE_METADATA__EPACKAGE = eINSTANCE.getPackageMetadata_EPackage();

		/**
		 * The meta object literal for the '<em><b>Ns URI</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PACKAGE_METADATA__NS_URI = eINSTANCE.getPackageMetadata_NsURI();

		/**
		 * The meta object literal for the '<em><b>Model Fingerprint</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PACKAGE_METADATA__MODEL_FINGERPRINT = eINSTANCE.getPackageMetadata_ModelFingerprint();

		/**
		 * The meta object literal for the '<em><b>Classes</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference PACKAGE_METADATA__CLASSES = eINSTANCE.getPackageMetadata_Classes();

		/**
		 * The meta object literal for the '<em><b>Aspects</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference PACKAGE_METADATA__ASPECTS = eINSTANCE.getPackageMetadata_Aspects();

		/**
		 * The meta object literal for the '<em><b>Properties</b></em>' map feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference PACKAGE_METADATA__PROPERTIES = eINSTANCE.getPackageMetadata_Properties();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl <em>Class Metadata</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getClassMetadata()
		 * @generated
		 */
		EClass CLASS_METADATA = eINSTANCE.getClassMetadata();

		/**
		 * The meta object literal for the '<em><b>Package</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CLASS_METADATA__PACKAGE = eINSTANCE.getClassMetadata_Package();

		/**
		 * The meta object literal for the '<em><b>EClass</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CLASS_METADATA__ECLASS = eINSTANCE.getClassMetadata_EClass();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CLASS_METADATA__NAME = eINSTANCE.getClassMetadata_Name();

		/**
		 * The meta object literal for the '<em><b>Classifier ID</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CLASS_METADATA__CLASSIFIER_ID = eINSTANCE.getClassMetadata_ClassifierID();

		/**
		 * The meta object literal for the '<em><b>Type URI</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CLASS_METADATA__TYPE_URI = eINSTANCE.getClassMetadata_TypeURI();

		/**
		 * The meta object literal for the '<em><b>Features</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CLASS_METADATA__FEATURES = eINSTANCE.getClassMetadata_Features();

		/**
		 * The meta object literal for the '<em><b>Operations</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CLASS_METADATA__OPERATIONS = eINSTANCE.getClassMetadata_Operations();

		/**
		 * The meta object literal for the '<em><b>Super Types</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CLASS_METADATA__SUPER_TYPES = eINSTANCE.getClassMetadata_SuperTypes();

		/**
		 * The meta object literal for the '<em><b>All Super Types</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CLASS_METADATA__ALL_SUPER_TYPES = eINSTANCE.getClassMetadata_AllSuperTypes();

		/**
		 * The meta object literal for the '<em><b>Id Features</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CLASS_METADATA__ID_FEATURES = eINSTANCE.getClassMetadata_IdFeatures();

		/**
		 * The meta object literal for the '<em><b>Has Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CLASS_METADATA__HAS_ID = eINSTANCE.getClassMetadata_HasId();

		/**
		 * The meta object literal for the '<em><b>Aspects</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CLASS_METADATA__ASPECTS = eINSTANCE.getClassMetadata_Aspects();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl <em>Feature Metadata</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getFeatureMetadata()
		 * @generated
		 */
		EClass FEATURE_METADATA = eINSTANCE.getFeatureMetadata();

		/**
		 * The meta object literal for the '<em><b>Class Metadata</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FEATURE_METADATA__CLASS_METADATA = eINSTANCE.getFeatureMetadata_ClassMetadata();

		/**
		 * The meta object literal for the '<em><b>EFeature</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FEATURE_METADATA__EFEATURE = eINSTANCE.getFeatureMetadata_EFeature();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FEATURE_METADATA__NAME = eINSTANCE.getFeatureMetadata_Name();

		/**
		 * The meta object literal for the '<em><b>Extended Meta Data Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FEATURE_METADATA__EXTENDED_META_DATA_NAME = eINSTANCE.getFeatureMetadata_ExtendedMetaDataName();

		/**
		 * The meta object literal for the '<em><b>Feature ID</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FEATURE_METADATA__FEATURE_ID = eINSTANCE.getFeatureMetadata_FeatureID();

		/**
		 * The meta object literal for the '<em><b>Aspects</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FEATURE_METADATA__ASPECTS = eINSTANCE.getFeatureMetadata_Aspects();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.AttributeMetadataImpl <em>Attribute Metadata</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.AttributeMetadataImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getAttributeMetadata()
		 * @generated
		 */
		EClass ATTRIBUTE_METADATA = eINSTANCE.getAttributeMetadata();

		/**
		 * The meta object literal for the '<em><b>EAttribute</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ATTRIBUTE_METADATA__EATTRIBUTE = eINSTANCE.getAttributeMetadata_EAttribute();

		/**
		 * The meta object literal for the '<em><b>Is Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ATTRIBUTE_METADATA__IS_ID = eINSTANCE.getAttributeMetadata_IsId();

		/**
		 * The meta object literal for the '<em><b>Default Value</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ATTRIBUTE_METADATA__DEFAULT_VALUE = eINSTANCE.getAttributeMetadata_DefaultValue();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl <em>Reference Metadata</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getReferenceMetadata()
		 * @generated
		 */
		EClass REFERENCE_METADATA = eINSTANCE.getReferenceMetadata();

		/**
		 * The meta object literal for the '<em><b>EReference</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REFERENCE_METADATA__EREFERENCE = eINSTANCE.getReferenceMetadata_EReference();

		/**
		 * The meta object literal for the '<em><b>Containment</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REFERENCE_METADATA__CONTAINMENT = eINSTANCE.getReferenceMetadata_Containment();

		/**
		 * The meta object literal for the '<em><b>Target Class Metadata</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REFERENCE_METADATA__TARGET_CLASS_METADATA = eINSTANCE.getReferenceMetadata_TargetClassMetadata();

		/**
		 * The meta object literal for the '<em><b>Opposite Metadata</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference REFERENCE_METADATA__OPPOSITE_METADATA = eINSTANCE.getReferenceMetadata_OppositeMetadata();

		/**
		 * The meta object literal for the '<em><b>Has Bidirectional</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute REFERENCE_METADATA__HAS_BIDIRECTIONAL = eINSTANCE.getReferenceMetadata_HasBidirectional();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl <em>Operation Metadata</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getOperationMetadata()
		 * @generated
		 */
		EClass OPERATION_METADATA = eINSTANCE.getOperationMetadata();

		/**
		 * The meta object literal for the '<em><b>Class Metadata</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference OPERATION_METADATA__CLASS_METADATA = eINSTANCE.getOperationMetadata_ClassMetadata();

		/**
		 * The meta object literal for the '<em><b>EOperation</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference OPERATION_METADATA__EOPERATION = eINSTANCE.getOperationMetadata_EOperation();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute OPERATION_METADATA__NAME = eINSTANCE.getOperationMetadata_Name();

		/**
		 * The meta object literal for the '<em><b>Operation ID</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute OPERATION_METADATA__OPERATION_ID = eINSTANCE.getOperationMetadata_OperationID();

		/**
		 * The meta object literal for the '<em><b>Return Type Metadata</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference OPERATION_METADATA__RETURN_TYPE_METADATA = eINSTANCE.getOperationMetadata_ReturnTypeMetadata();

		/**
		 * The meta object literal for the '<em><b>Parameters</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference OPERATION_METADATA__PARAMETERS = eINSTANCE.getOperationMetadata_Parameters();

		/**
		 * The meta object literal for the '<em><b>Aspects</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference OPERATION_METADATA__ASPECTS = eINSTANCE.getOperationMetadata_Aspects();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl <em>Parameter Metadata</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getParameterMetadata()
		 * @generated
		 */
		EClass PARAMETER_METADATA = eINSTANCE.getParameterMetadata();

		/**
		 * The meta object literal for the '<em><b>Operation Metadata</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference PARAMETER_METADATA__OPERATION_METADATA = eINSTANCE.getParameterMetadata_OperationMetadata();

		/**
		 * The meta object literal for the '<em><b>EParameter</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference PARAMETER_METADATA__EPARAMETER = eINSTANCE.getParameterMetadata_EParameter();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PARAMETER_METADATA__NAME = eINSTANCE.getParameterMetadata_Name();

		/**
		 * The meta object literal for the '<em><b>Type Metadata</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference PARAMETER_METADATA__TYPE_METADATA = eINSTANCE.getParameterMetadata_TypeMetadata();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataRegistryImpl <em>Registry</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataRegistryImpl
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getMetadataRegistry()
		 * @generated
		 */
		EClass METADATA_REGISTRY = eINSTANCE.getMetadataRegistry();

		/**
		 * The meta object literal for the '<em><b>Packages</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference METADATA_REGISTRY__PACKAGES = eINSTANCE.getMetadataRegistry_Packages();

		/**
		 * The meta object literal for the '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity <em>Diagnostic Severity</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity
		 * @see org.eclipse.fennec.emf.osgi.model.metadata.impl.MetadataPackageImpl#getDiagnosticSeverity()
		 * @generated
		 */
		EEnum DIAGNOSTIC_SEVERITY = eINSTANCE.getDiagnosticSeverity();

	}

} //MetadataPackage
