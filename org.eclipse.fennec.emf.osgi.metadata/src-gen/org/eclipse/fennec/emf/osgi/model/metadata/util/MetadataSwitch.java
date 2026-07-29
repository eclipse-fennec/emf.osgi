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

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.Switch;

import org.eclipse.fennec.emf.osgi.model.metadata.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage
 * @generated
 */
public class MetadataSwitch<T> extends Switch<T> {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static MetadataPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MetadataSwitch() {
		if (modelPackage == null) {
			modelPackage = MetadataPackage.eINSTANCE;
		}
	}

	/**
	 * Checks whether this is a switch for the given package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param ePackage the package in question.
	 * @return whether this is a switch for the given package.
	 * @generated
	 */
	@Override
	protected boolean isSwitchFor(EPackage ePackage) {
		return ePackage == modelPackage;
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	@Override
	protected T doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case MetadataPackage.METADATA_DIAGNOSTIC: {
				MetadataDiagnostic metadataDiagnostic = (MetadataDiagnostic)theEObject;
				T result = caseMetadataDiagnostic(metadataDiagnostic);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.DIAGNOSTIC_CONTAINER: {
				DiagnosticContainer diagnosticContainer = (DiagnosticContainer)theEObject;
				T result = caseDiagnosticContainer(diagnosticContainer);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.ASPECT_ENTRY: {
				AspectEntry aspectEntry = (AspectEntry)theEObject;
				T result = caseAspectEntry(aspectEntry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.PACKAGE_METADATA: {
				PackageMetadata packageMetadata = (PackageMetadata)theEObject;
				T result = casePackageMetadata(packageMetadata);
				if (result == null) result = caseDiagnosticContainer(packageMetadata);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.CLASS_METADATA: {
				ClassMetadata classMetadata = (ClassMetadata)theEObject;
				T result = caseClassMetadata(classMetadata);
				if (result == null) result = caseDiagnosticContainer(classMetadata);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.FEATURE_METADATA: {
				FeatureMetadata featureMetadata = (FeatureMetadata)theEObject;
				T result = caseFeatureMetadata(featureMetadata);
				if (result == null) result = caseDiagnosticContainer(featureMetadata);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.ATTRIBUTE_METADATA: {
				AttributeMetadata attributeMetadata = (AttributeMetadata)theEObject;
				T result = caseAttributeMetadata(attributeMetadata);
				if (result == null) result = caseFeatureMetadata(attributeMetadata);
				if (result == null) result = caseDiagnosticContainer(attributeMetadata);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.REFERENCE_METADATA: {
				ReferenceMetadata referenceMetadata = (ReferenceMetadata)theEObject;
				T result = caseReferenceMetadata(referenceMetadata);
				if (result == null) result = caseFeatureMetadata(referenceMetadata);
				if (result == null) result = caseDiagnosticContainer(referenceMetadata);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.OPERATION_METADATA: {
				OperationMetadata operationMetadata = (OperationMetadata)theEObject;
				T result = caseOperationMetadata(operationMetadata);
				if (result == null) result = caseDiagnosticContainer(operationMetadata);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.PARAMETER_METADATA: {
				ParameterMetadata parameterMetadata = (ParameterMetadata)theEObject;
				T result = caseParameterMetadata(parameterMetadata);
				if (result == null) result = caseDiagnosticContainer(parameterMetadata);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MetadataPackage.METADATA_REGISTRY: {
				MetadataRegistry metadataRegistry = (MetadataRegistry)theEObject;
				T result = caseMetadataRegistry(metadataRegistry);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Diagnostic</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Diagnostic</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseMetadataDiagnostic(MetadataDiagnostic object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Diagnostic Container</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Diagnostic Container</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDiagnosticContainer(DiagnosticContainer object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Aspect Entry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Aspect Entry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAspectEntry(AspectEntry object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Package Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Package Metadata</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T casePackageMetadata(PackageMetadata object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Class Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Class Metadata</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseClassMetadata(ClassMetadata object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Feature Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Feature Metadata</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseFeatureMetadata(FeatureMetadata object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Attribute Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Attribute Metadata</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAttributeMetadata(AttributeMetadata object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Reference Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Reference Metadata</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseReferenceMetadata(ReferenceMetadata object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Operation Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Operation Metadata</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseOperationMetadata(OperationMetadata object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Parameter Metadata</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Parameter Metadata</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseParameterMetadata(ParameterMetadata object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Registry</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Registry</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseMetadataRegistry(MetadataRegistry object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	@Override
	public T defaultCase(EObject object) {
		return null;
	}

} //MetadataSwitch
