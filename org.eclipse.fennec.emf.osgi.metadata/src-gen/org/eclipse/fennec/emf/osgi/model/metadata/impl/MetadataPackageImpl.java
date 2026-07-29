/**
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
package org.eclipse.fennec.emf.osgi.model.metadata.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.impl.EPackageImpl;

import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer;
import org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity;
import org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class MetadataPackageImpl extends EPackageImpl implements MetadataPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass metadataDiagnosticEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass diagnosticContainerEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass aspectEntryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass packageMetadataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass classMetadataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass featureMetadataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass attributeMetadataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass referenceMetadataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass operationMetadataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass parameterMetadataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass metadataRegistryEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum diagnosticSeverityEEnum = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private MetadataPackageImpl() {
		super(eNS_URI, MetadataFactory.eINSTANCE);
	}
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 *
	 * <p>This method is used to initialize {@link MetadataPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static MetadataPackage init() {
		if (isInited) return (MetadataPackage)EPackage.Registry.INSTANCE.getEPackage(MetadataPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredMetadataPackage = EPackage.Registry.INSTANCE.get(eNS_URI);
		MetadataPackageImpl theMetadataPackage = registeredMetadataPackage instanceof MetadataPackageImpl ? (MetadataPackageImpl)registeredMetadataPackage : new MetadataPackageImpl();

		isInited = true;

		// Create package meta-data objects
		theMetadataPackage.createPackageContents();

		// Initialize created meta-data
		theMetadataPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theMetadataPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(MetadataPackage.eNS_URI, theMetadataPackage);
		return theMetadataPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getMetadataDiagnostic() {
		return metadataDiagnosticEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMetadataDiagnostic_Severity() {
		return (EAttribute)metadataDiagnosticEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMetadataDiagnostic_Message() {
		return (EAttribute)metadataDiagnosticEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMetadataDiagnostic_Key() {
		return (EAttribute)metadataDiagnosticEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getDiagnosticContainer() {
		return diagnosticContainerEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getDiagnosticContainer_Diagnostics() {
		return (EReference)diagnosticContainerEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getDiagnosticContainer_AllDiagnostics() {
		return (EReference)diagnosticContainerEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getAspectEntry() {
		return aspectEntryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAspectEntry_TypeId() {
		return (EAttribute)aspectEntryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getAspectEntry_Content() {
		return (EReference)aspectEntryEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAspectEntry_TransientContent() {
		return (EAttribute)aspectEntryEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getAspectEntry_Diagnostics() {
		return (EReference)aspectEntryEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getPackageMetadata() {
		return packageMetadataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getPackageMetadata_EPackage() {
		return (EReference)packageMetadataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPackageMetadata_NsURI() {
		return (EAttribute)packageMetadataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPackageMetadata_ModelFingerprint() {
		return (EAttribute)packageMetadataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getPackageMetadata_Classes() {
		return (EReference)packageMetadataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getPackageMetadata_Aspects() {
		return (EReference)packageMetadataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getPackageMetadata_Properties() {
		return (EReference)packageMetadataEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getClassMetadata() {
		return classMetadataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getClassMetadata_Package() {
		return (EReference)classMetadataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getClassMetadata_EClass() {
		return (EReference)classMetadataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getClassMetadata_Name() {
		return (EAttribute)classMetadataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getClassMetadata_ClassifierID() {
		return (EAttribute)classMetadataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getClassMetadata_TypeURI() {
		return (EAttribute)classMetadataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getClassMetadata_Features() {
		return (EReference)classMetadataEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getClassMetadata_Operations() {
		return (EReference)classMetadataEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getClassMetadata_SuperTypes() {
		return (EReference)classMetadataEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getClassMetadata_AllSuperTypes() {
		return (EReference)classMetadataEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getClassMetadata_IdFeatures() {
		return (EReference)classMetadataEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getClassMetadata_HasId() {
		return (EAttribute)classMetadataEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getClassMetadata_Aspects() {
		return (EReference)classMetadataEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFeatureMetadata() {
		return featureMetadataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getFeatureMetadata_ClassMetadata() {
		return (EReference)featureMetadataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getFeatureMetadata_EFeature() {
		return (EReference)featureMetadataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFeatureMetadata_Name() {
		return (EAttribute)featureMetadataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFeatureMetadata_ExtendedMetaDataName() {
		return (EAttribute)featureMetadataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFeatureMetadata_FeatureID() {
		return (EAttribute)featureMetadataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getFeatureMetadata_Aspects() {
		return (EReference)featureMetadataEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getAttributeMetadata() {
		return attributeMetadataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getAttributeMetadata_EAttribute() {
		return (EReference)attributeMetadataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAttributeMetadata_IsId() {
		return (EAttribute)attributeMetadataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAttributeMetadata_DefaultValue() {
		return (EAttribute)attributeMetadataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getReferenceMetadata() {
		return referenceMetadataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getReferenceMetadata_EReference() {
		return (EReference)referenceMetadataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getReferenceMetadata_Containment() {
		return (EAttribute)referenceMetadataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getReferenceMetadata_TargetClassMetadata() {
		return (EReference)referenceMetadataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getReferenceMetadata_OppositeMetadata() {
		return (EReference)referenceMetadataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getReferenceMetadata_HasBidirectional() {
		return (EAttribute)referenceMetadataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getOperationMetadata() {
		return operationMetadataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOperationMetadata_ClassMetadata() {
		return (EReference)operationMetadataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOperationMetadata_EOperation() {
		return (EReference)operationMetadataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOperationMetadata_Name() {
		return (EAttribute)operationMetadataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOperationMetadata_OperationID() {
		return (EAttribute)operationMetadataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOperationMetadata_ReturnTypeMetadata() {
		return (EReference)operationMetadataEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOperationMetadata_Parameters() {
		return (EReference)operationMetadataEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOperationMetadata_Aspects() {
		return (EReference)operationMetadataEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getParameterMetadata() {
		return parameterMetadataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getParameterMetadata_OperationMetadata() {
		return (EReference)parameterMetadataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getParameterMetadata_EParameter() {
		return (EReference)parameterMetadataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getParameterMetadata_Name() {
		return (EAttribute)parameterMetadataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getParameterMetadata_TypeMetadata() {
		return (EReference)parameterMetadataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getMetadataRegistry() {
		return metadataRegistryEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getMetadataRegistry_Packages() {
		return (EReference)metadataRegistryEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getDiagnosticSeverity() {
		return diagnosticSeverityEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public MetadataFactory getMetadataFactory() {
		return (MetadataFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		metadataDiagnosticEClass = createEClass(METADATA_DIAGNOSTIC);
		createEAttribute(metadataDiagnosticEClass, METADATA_DIAGNOSTIC__SEVERITY);
		createEAttribute(metadataDiagnosticEClass, METADATA_DIAGNOSTIC__MESSAGE);
		createEAttribute(metadataDiagnosticEClass, METADATA_DIAGNOSTIC__KEY);

		diagnosticContainerEClass = createEClass(DIAGNOSTIC_CONTAINER);
		createEReference(diagnosticContainerEClass, DIAGNOSTIC_CONTAINER__DIAGNOSTICS);
		createEReference(diagnosticContainerEClass, DIAGNOSTIC_CONTAINER__ALL_DIAGNOSTICS);

		aspectEntryEClass = createEClass(ASPECT_ENTRY);
		createEAttribute(aspectEntryEClass, ASPECT_ENTRY__TYPE_ID);
		createEReference(aspectEntryEClass, ASPECT_ENTRY__CONTENT);
		createEAttribute(aspectEntryEClass, ASPECT_ENTRY__TRANSIENT_CONTENT);
		createEReference(aspectEntryEClass, ASPECT_ENTRY__DIAGNOSTICS);

		packageMetadataEClass = createEClass(PACKAGE_METADATA);
		createEReference(packageMetadataEClass, PACKAGE_METADATA__EPACKAGE);
		createEAttribute(packageMetadataEClass, PACKAGE_METADATA__NS_URI);
		createEAttribute(packageMetadataEClass, PACKAGE_METADATA__MODEL_FINGERPRINT);
		createEReference(packageMetadataEClass, PACKAGE_METADATA__CLASSES);
		createEReference(packageMetadataEClass, PACKAGE_METADATA__ASPECTS);
		createEReference(packageMetadataEClass, PACKAGE_METADATA__PROPERTIES);

		classMetadataEClass = createEClass(CLASS_METADATA);
		createEReference(classMetadataEClass, CLASS_METADATA__PACKAGE);
		createEReference(classMetadataEClass, CLASS_METADATA__ECLASS);
		createEAttribute(classMetadataEClass, CLASS_METADATA__NAME);
		createEAttribute(classMetadataEClass, CLASS_METADATA__CLASSIFIER_ID);
		createEAttribute(classMetadataEClass, CLASS_METADATA__TYPE_URI);
		createEReference(classMetadataEClass, CLASS_METADATA__FEATURES);
		createEReference(classMetadataEClass, CLASS_METADATA__OPERATIONS);
		createEReference(classMetadataEClass, CLASS_METADATA__SUPER_TYPES);
		createEReference(classMetadataEClass, CLASS_METADATA__ALL_SUPER_TYPES);
		createEReference(classMetadataEClass, CLASS_METADATA__ID_FEATURES);
		createEAttribute(classMetadataEClass, CLASS_METADATA__HAS_ID);
		createEReference(classMetadataEClass, CLASS_METADATA__ASPECTS);

		featureMetadataEClass = createEClass(FEATURE_METADATA);
		createEReference(featureMetadataEClass, FEATURE_METADATA__CLASS_METADATA);
		createEReference(featureMetadataEClass, FEATURE_METADATA__EFEATURE);
		createEAttribute(featureMetadataEClass, FEATURE_METADATA__NAME);
		createEAttribute(featureMetadataEClass, FEATURE_METADATA__EXTENDED_META_DATA_NAME);
		createEAttribute(featureMetadataEClass, FEATURE_METADATA__FEATURE_ID);
		createEReference(featureMetadataEClass, FEATURE_METADATA__ASPECTS);

		attributeMetadataEClass = createEClass(ATTRIBUTE_METADATA);
		createEReference(attributeMetadataEClass, ATTRIBUTE_METADATA__EATTRIBUTE);
		createEAttribute(attributeMetadataEClass, ATTRIBUTE_METADATA__IS_ID);
		createEAttribute(attributeMetadataEClass, ATTRIBUTE_METADATA__DEFAULT_VALUE);

		referenceMetadataEClass = createEClass(REFERENCE_METADATA);
		createEReference(referenceMetadataEClass, REFERENCE_METADATA__EREFERENCE);
		createEAttribute(referenceMetadataEClass, REFERENCE_METADATA__CONTAINMENT);
		createEReference(referenceMetadataEClass, REFERENCE_METADATA__TARGET_CLASS_METADATA);
		createEReference(referenceMetadataEClass, REFERENCE_METADATA__OPPOSITE_METADATA);
		createEAttribute(referenceMetadataEClass, REFERENCE_METADATA__HAS_BIDIRECTIONAL);

		operationMetadataEClass = createEClass(OPERATION_METADATA);
		createEReference(operationMetadataEClass, OPERATION_METADATA__CLASS_METADATA);
		createEReference(operationMetadataEClass, OPERATION_METADATA__EOPERATION);
		createEAttribute(operationMetadataEClass, OPERATION_METADATA__NAME);
		createEAttribute(operationMetadataEClass, OPERATION_METADATA__OPERATION_ID);
		createEReference(operationMetadataEClass, OPERATION_METADATA__RETURN_TYPE_METADATA);
		createEReference(operationMetadataEClass, OPERATION_METADATA__PARAMETERS);
		createEReference(operationMetadataEClass, OPERATION_METADATA__ASPECTS);

		parameterMetadataEClass = createEClass(PARAMETER_METADATA);
		createEReference(parameterMetadataEClass, PARAMETER_METADATA__OPERATION_METADATA);
		createEReference(parameterMetadataEClass, PARAMETER_METADATA__EPARAMETER);
		createEAttribute(parameterMetadataEClass, PARAMETER_METADATA__NAME);
		createEReference(parameterMetadataEClass, PARAMETER_METADATA__TYPE_METADATA);

		metadataRegistryEClass = createEClass(METADATA_REGISTRY);
		createEReference(metadataRegistryEClass, METADATA_REGISTRY__PACKAGES);

		// Create enums
		diagnosticSeverityEEnum = createEEnum(DIAGNOSTIC_SEVERITY);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		packageMetadataEClass.getESuperTypes().add(this.getDiagnosticContainer());
		classMetadataEClass.getESuperTypes().add(this.getDiagnosticContainer());
		featureMetadataEClass.getESuperTypes().add(this.getDiagnosticContainer());
		attributeMetadataEClass.getESuperTypes().add(this.getFeatureMetadata());
		referenceMetadataEClass.getESuperTypes().add(this.getFeatureMetadata());
		operationMetadataEClass.getESuperTypes().add(this.getDiagnosticContainer());
		parameterMetadataEClass.getESuperTypes().add(this.getDiagnosticContainer());

		// Initialize classes, features, and operations; add parameters
		initEClass(metadataDiagnosticEClass, MetadataDiagnostic.class, "MetadataDiagnostic", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMetadataDiagnostic_Severity(), this.getDiagnosticSeverity(), "severity", "WARNING", 0, 1, MetadataDiagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMetadataDiagnostic_Message(), ecorePackage.getEString(), "message", null, 0, 1, MetadataDiagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMetadataDiagnostic_Key(), ecorePackage.getEString(), "key", null, 0, 1, MetadataDiagnostic.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(diagnosticContainerEClass, DiagnosticContainer.class, "DiagnosticContainer", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getDiagnosticContainer_Diagnostics(), this.getMetadataDiagnostic(), null, "diagnostics", null, 0, -1, DiagnosticContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getDiagnosticContainer_AllDiagnostics(), this.getMetadataDiagnostic(), null, "allDiagnostics", null, 0, -1, DiagnosticContainer.class, IS_TRANSIENT, IS_VOLATILE, !IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(aspectEntryEClass, AspectEntry.class, "AspectEntry", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAspectEntry_TypeId(), ecorePackage.getEString(), "typeId", null, 0, 1, AspectEntry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getAspectEntry_Content(), ecorePackage.getEObject(), null, "content", null, 0, 1, AspectEntry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAspectEntry_TransientContent(), ecorePackage.getEJavaObject(), "transientContent", null, 0, 1, AspectEntry.class, IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getAspectEntry_Diagnostics(), this.getMetadataDiagnostic(), null, "diagnostics", null, 0, -1, AspectEntry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(packageMetadataEClass, PackageMetadata.class, "PackageMetadata", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getPackageMetadata_EPackage(), ecorePackage.getEPackage(), null, "ePackage", null, 0, 1, PackageMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPackageMetadata_NsURI(), ecorePackage.getEString(), "nsURI", null, 0, 1, PackageMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPackageMetadata_ModelFingerprint(), ecorePackage.getEString(), "modelFingerprint", null, 0, 1, PackageMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getPackageMetadata_Classes(), this.getClassMetadata(), this.getClassMetadata_Package(), "classes", null, 0, -1, PackageMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getPackageMetadata_Aspects(), this.getAspectEntry(), null, "aspects", null, 0, -1, PackageMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getPackageMetadata_Properties(), ecorePackage.getEStringToStringMapEntry(), null, "properties", null, 0, -1, PackageMetadata.class, IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(classMetadataEClass, ClassMetadata.class, "ClassMetadata", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getClassMetadata_Package(), this.getPackageMetadata(), this.getPackageMetadata_Classes(), "package", null, 0, 1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getClassMetadata_EClass(), ecorePackage.getEClass(), null, "eClass", null, 0, 1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getClassMetadata_Name(), ecorePackage.getEString(), "name", null, 0, 1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getClassMetadata_ClassifierID(), ecorePackage.getEInt(), "classifierID", "-1", 0, 1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getClassMetadata_TypeURI(), ecorePackage.getEString(), "typeURI", null, 0, 1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getClassMetadata_Features(), this.getFeatureMetadata(), this.getFeatureMetadata_ClassMetadata(), "features", null, 0, -1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getClassMetadata_Operations(), this.getOperationMetadata(), this.getOperationMetadata_ClassMetadata(), "operations", null, 0, -1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getClassMetadata_SuperTypes(), this.getClassMetadata(), null, "superTypes", null, 0, -1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getClassMetadata_AllSuperTypes(), this.getClassMetadata(), null, "allSuperTypes", null, 0, -1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getClassMetadata_IdFeatures(), this.getFeatureMetadata(), null, "idFeatures", null, 0, -1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getClassMetadata_HasId(), ecorePackage.getEBoolean(), "hasId", "false", 0, 1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getClassMetadata_Aspects(), this.getAspectEntry(), null, "aspects", null, 0, -1, ClassMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(featureMetadataEClass, FeatureMetadata.class, "FeatureMetadata", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getFeatureMetadata_ClassMetadata(), this.getClassMetadata(), this.getClassMetadata_Features(), "classMetadata", null, 0, 1, FeatureMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFeatureMetadata_EFeature(), ecorePackage.getEStructuralFeature(), null, "eFeature", null, 0, 1, FeatureMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFeatureMetadata_Name(), ecorePackage.getEString(), "name", null, 0, 1, FeatureMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFeatureMetadata_ExtendedMetaDataName(), ecorePackage.getEString(), "extendedMetaDataName", null, 0, 1, FeatureMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFeatureMetadata_FeatureID(), ecorePackage.getEInt(), "featureID", "-1", 0, 1, FeatureMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFeatureMetadata_Aspects(), this.getAspectEntry(), null, "aspects", null, 0, -1, FeatureMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(attributeMetadataEClass, AttributeMetadata.class, "AttributeMetadata", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getAttributeMetadata_EAttribute(), ecorePackage.getEAttribute(), null, "eAttribute", null, 0, 1, AttributeMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAttributeMetadata_IsId(), ecorePackage.getEBoolean(), "isId", "false", 0, 1, AttributeMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAttributeMetadata_DefaultValue(), ecorePackage.getEJavaObject(), "defaultValue", null, 0, 1, AttributeMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(referenceMetadataEClass, ReferenceMetadata.class, "ReferenceMetadata", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getReferenceMetadata_EReference(), ecorePackage.getEReference(), null, "eReference", null, 0, 1, ReferenceMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getReferenceMetadata_Containment(), ecorePackage.getEBoolean(), "containment", "false", 0, 1, ReferenceMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getReferenceMetadata_TargetClassMetadata(), this.getClassMetadata(), null, "targetClassMetadata", null, 0, 1, ReferenceMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getReferenceMetadata_OppositeMetadata(), this.getReferenceMetadata(), null, "oppositeMetadata", null, 0, 1, ReferenceMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getReferenceMetadata_HasBidirectional(), ecorePackage.getEBoolean(), "hasBidirectional", "false", 0, 1, ReferenceMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(operationMetadataEClass, OperationMetadata.class, "OperationMetadata", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getOperationMetadata_ClassMetadata(), this.getClassMetadata(), this.getClassMetadata_Operations(), "classMetadata", null, 0, 1, OperationMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOperationMetadata_EOperation(), ecorePackage.getEOperation(), null, "eOperation", null, 0, 1, OperationMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOperationMetadata_Name(), ecorePackage.getEString(), "name", null, 0, 1, OperationMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOperationMetadata_OperationID(), ecorePackage.getEInt(), "operationID", "-1", 0, 1, OperationMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOperationMetadata_ReturnTypeMetadata(), this.getClassMetadata(), null, "returnTypeMetadata", null, 0, 1, OperationMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOperationMetadata_Parameters(), this.getParameterMetadata(), this.getParameterMetadata_OperationMetadata(), "parameters", null, 0, -1, OperationMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOperationMetadata_Aspects(), this.getAspectEntry(), null, "aspects", null, 0, -1, OperationMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(parameterMetadataEClass, ParameterMetadata.class, "ParameterMetadata", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getParameterMetadata_OperationMetadata(), this.getOperationMetadata(), this.getOperationMetadata_Parameters(), "operationMetadata", null, 0, 1, ParameterMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getParameterMetadata_EParameter(), ecorePackage.getEParameter(), null, "eParameter", null, 0, 1, ParameterMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getParameterMetadata_Name(), ecorePackage.getEString(), "name", null, 0, 1, ParameterMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getParameterMetadata_TypeMetadata(), this.getClassMetadata(), null, "typeMetadata", null, 0, 1, ParameterMetadata.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(metadataRegistryEClass, MetadataRegistry.class, "MetadataRegistry", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getMetadataRegistry_Packages(), this.getPackageMetadata(), null, "packages", null, 0, -1, MetadataRegistry.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Initialize enums and add enum literals
		initEEnum(diagnosticSeverityEEnum, DiagnosticSeverity.class, "DiagnosticSeverity");
		addEEnumLiteral(diagnosticSeverityEEnum, DiagnosticSeverity.WARNING);
		addEEnumLiteral(diagnosticSeverityEEnum, DiagnosticSeverity.ERROR);

		// Create resource
		createResource(eNS_URI);

		// Create annotations
		// Version
		createVersionAnnotations();
	}

	/**
	 * Initializes the annotations for <b>Version</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void createVersionAnnotations() {
		String source = "Version";
		addAnnotation
		  (this,
		   source,
		   new String[] {
			   "value", "1.0"
		   });
	}

} //MetadataPackageImpl
