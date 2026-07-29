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
package org.eclipse.fennec.emf.osgi.model.metadata.util;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.fennec.emf.osgi.model.metadata.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage
 * @generated
 */
public class MetadataAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static MetadataPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MetadataAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = MetadataPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch that delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MetadataSwitch<Adapter> modelSwitch =
		new MetadataSwitch<Adapter>() {
			@Override
			public Adapter caseMetadataDiagnostic(MetadataDiagnostic object) {
				return createMetadataDiagnosticAdapter();
			}
			@Override
			public Adapter caseDiagnosticContainer(DiagnosticContainer object) {
				return createDiagnosticContainerAdapter();
			}
			@Override
			public Adapter caseAspectEntry(AspectEntry object) {
				return createAspectEntryAdapter();
			}
			@Override
			public Adapter casePackageMetadata(PackageMetadata object) {
				return createPackageMetadataAdapter();
			}
			@Override
			public Adapter caseClassMetadata(ClassMetadata object) {
				return createClassMetadataAdapter();
			}
			@Override
			public Adapter caseFeatureMetadata(FeatureMetadata object) {
				return createFeatureMetadataAdapter();
			}
			@Override
			public Adapter caseAttributeMetadata(AttributeMetadata object) {
				return createAttributeMetadataAdapter();
			}
			@Override
			public Adapter caseReferenceMetadata(ReferenceMetadata object) {
				return createReferenceMetadataAdapter();
			}
			@Override
			public Adapter caseOperationMetadata(OperationMetadata object) {
				return createOperationMetadataAdapter();
			}
			@Override
			public Adapter caseParameterMetadata(ParameterMetadata object) {
				return createParameterMetadataAdapter();
			}
			@Override
			public Adapter caseMetadataRegistry(MetadataRegistry object) {
				return createMetadataRegistryAdapter();
			}
			@Override
			public Adapter defaultCase(EObject object) {
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target) {
		return modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic <em>Diagnostic</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic
	 * @generated
	 */
	public Adapter createMetadataDiagnosticAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer <em>Diagnostic Container</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticContainer
	 * @generated
	 */
	public Adapter createDiagnosticContainerAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry <em>Aspect Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry
	 * @generated
	 */
	public Adapter createAspectEntryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata <em>Package Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata
	 * @generated
	 */
	public Adapter createPackageMetadataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata <em>Class Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata
	 * @generated
	 */
	public Adapter createClassMetadataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata <em>Feature Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata
	 * @generated
	 */
	public Adapter createFeatureMetadataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata <em>Attribute Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata
	 * @generated
	 */
	public Adapter createAttributeMetadataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata <em>Reference Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata
	 * @generated
	 */
	public Adapter createReferenceMetadataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata <em>Operation Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata
	 * @generated
	 */
	public Adapter createOperationMetadataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata <em>Parameter Metadata</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata
	 * @generated
	 */
	public Adapter createParameterMetadataAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry <em>Registry</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataRegistry
	 * @generated
	 */
	public Adapter createMetadataRegistryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //MetadataAdapterFactory
