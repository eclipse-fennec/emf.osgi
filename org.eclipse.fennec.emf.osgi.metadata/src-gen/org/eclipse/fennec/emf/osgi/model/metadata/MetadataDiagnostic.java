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

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Diagnostic</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Diagnostic message for issues detected during metadata construction, annotation parsing, or configuration validation. Contained by the metadata element or aspect entry where the issue was detected.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getSeverity <em>Severity</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getMessage <em>Message</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getKey <em>Key</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getMetadataDiagnostic()
 * @model
 * @generated
 */
@ProviderType
public interface MetadataDiagnostic extends EObject {
	/**
	 * Returns the value of the '<em><b>Severity</b></em>' attribute.
	 * The default value is <code>"WARNING"</code>.
	 * The literals are from the enumeration {@link org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Severity level of this diagnostic (WARNING or ERROR).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Severity</em>' attribute.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity
	 * @see #setSeverity(DiagnosticSeverity)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getMetadataDiagnostic_Severity()
	 * @model default="WARNING"
	 * @generated
	 */
	DiagnosticSeverity getSeverity();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getSeverity <em>Severity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Severity</em>' attribute.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity
	 * @see #getSeverity()
	 * @generated
	 */
	void setSeverity(DiagnosticSeverity value);

	/**
	 * Returns the value of the '<em><b>Message</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Human-readable description of the issue.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Message</em>' attribute.
	 * @see #setMessage(String)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getMetadataDiagnostic_Message()
	 * @model
	 * @generated
	 */
	String getMessage();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getMessage <em>Message</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Message</em>' attribute.
	 * @see #getMessage()
	 * @generated
	 */
	void setMessage(String value);

	/**
	 * Returns the value of the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The annotation key that caused the issue, if applicable. Null for diagnostics not related to a specific annotation key.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Key</em>' attribute.
	 * @see #setKey(String)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getMetadataDiagnostic_Key()
	 * @model
	 * @generated
	 */
	String getKey();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic#getKey <em>Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Key</em>' attribute.
	 * @see #getKey()
	 * @generated
	 */
	void setKey(String value);

} // MetadataDiagnostic
