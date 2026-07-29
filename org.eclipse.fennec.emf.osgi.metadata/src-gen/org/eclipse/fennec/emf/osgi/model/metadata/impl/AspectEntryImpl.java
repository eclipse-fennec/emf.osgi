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
package org.eclipse.fennec.emf.osgi.model.metadata.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Aspect Entry</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.AspectEntryImpl#getTypeId <em>Type Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.AspectEntryImpl#getContent <em>Content</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.AspectEntryImpl#getTransientContent <em>Transient Content</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.AspectEntryImpl#getDiagnostics <em>Diagnostics</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AspectEntryImpl extends MinimalEObjectImpl.Container implements AspectEntry {
	/**
	 * The default value of the '{@link #getTypeId() <em>Type Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTypeId()
	 * @generated
	 * @ordered
	 */
	protected static final String TYPE_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTypeId() <em>Type Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTypeId()
	 * @generated
	 * @ordered
	 */
	protected String typeId = TYPE_ID_EDEFAULT;

	/**
	 * The cached value of the '{@link #getContent() <em>Content</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getContent()
	 * @generated
	 * @ordered
	 */
	protected EObject content;

	/**
	 * The default value of the '{@link #getTransientContent() <em>Transient Content</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTransientContent()
	 * @generated
	 * @ordered
	 */
	protected static final Object TRANSIENT_CONTENT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTransientContent() <em>Transient Content</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTransientContent()
	 * @generated
	 * @ordered
	 */
	protected Object transientContent = TRANSIENT_CONTENT_EDEFAULT;

	/**
	 * The cached value of the '{@link #getDiagnostics() <em>Diagnostics</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDiagnostics()
	 * @generated
	 * @ordered
	 */
	protected EList<MetadataDiagnostic> diagnostics;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AspectEntryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return MetadataPackage.Literals.ASPECT_ENTRY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTypeId() {
		return typeId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTypeId(String newTypeId) {
		String oldTypeId = typeId;
		typeId = newTypeId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.ASPECT_ENTRY__TYPE_ID, oldTypeId, typeId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject getContent() {
		return content;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetContent(EObject newContent, NotificationChain msgs) {
		EObject oldContent = content;
		content = newContent;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, MetadataPackage.ASPECT_ENTRY__CONTENT, oldContent, newContent);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setContent(EObject newContent) {
		if (newContent != content) {
			NotificationChain msgs = null;
			if (content != null)
				msgs = ((InternalEObject)content).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - MetadataPackage.ASPECT_ENTRY__CONTENT, null, msgs);
			if (newContent != null)
				msgs = ((InternalEObject)newContent).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - MetadataPackage.ASPECT_ENTRY__CONTENT, null, msgs);
			msgs = basicSetContent(newContent, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.ASPECT_ENTRY__CONTENT, newContent, newContent));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getTransientContent() {
		return transientContent;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTransientContent(Object newTransientContent) {
		Object oldTransientContent = transientContent;
		transientContent = newTransientContent;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.ASPECT_ENTRY__TRANSIENT_CONTENT, oldTransientContent, transientContent));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<MetadataDiagnostic> getDiagnostics() {
		if (diagnostics == null) {
			diagnostics = new EObjectContainmentEList<MetadataDiagnostic>(MetadataDiagnostic.class, this, MetadataPackage.ASPECT_ENTRY__DIAGNOSTICS);
		}
		return diagnostics;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case MetadataPackage.ASPECT_ENTRY__CONTENT:
				return basicSetContent(null, msgs);
			case MetadataPackage.ASPECT_ENTRY__DIAGNOSTICS:
				return ((InternalEList<?>)getDiagnostics()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case MetadataPackage.ASPECT_ENTRY__TYPE_ID:
				return getTypeId();
			case MetadataPackage.ASPECT_ENTRY__CONTENT:
				return getContent();
			case MetadataPackage.ASPECT_ENTRY__TRANSIENT_CONTENT:
				return getTransientContent();
			case MetadataPackage.ASPECT_ENTRY__DIAGNOSTICS:
				return getDiagnostics();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case MetadataPackage.ASPECT_ENTRY__TYPE_ID:
				setTypeId((String)newValue);
				return;
			case MetadataPackage.ASPECT_ENTRY__CONTENT:
				setContent((EObject)newValue);
				return;
			case MetadataPackage.ASPECT_ENTRY__TRANSIENT_CONTENT:
				setTransientContent(newValue);
				return;
			case MetadataPackage.ASPECT_ENTRY__DIAGNOSTICS:
				getDiagnostics().clear();
				getDiagnostics().addAll((Collection<? extends MetadataDiagnostic>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case MetadataPackage.ASPECT_ENTRY__TYPE_ID:
				setTypeId(TYPE_ID_EDEFAULT);
				return;
			case MetadataPackage.ASPECT_ENTRY__CONTENT:
				setContent((EObject)null);
				return;
			case MetadataPackage.ASPECT_ENTRY__TRANSIENT_CONTENT:
				setTransientContent(TRANSIENT_CONTENT_EDEFAULT);
				return;
			case MetadataPackage.ASPECT_ENTRY__DIAGNOSTICS:
				getDiagnostics().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case MetadataPackage.ASPECT_ENTRY__TYPE_ID:
				return TYPE_ID_EDEFAULT == null ? typeId != null : !TYPE_ID_EDEFAULT.equals(typeId);
			case MetadataPackage.ASPECT_ENTRY__CONTENT:
				return content != null;
			case MetadataPackage.ASPECT_ENTRY__TRANSIENT_CONTENT:
				return TRANSIENT_CONTENT_EDEFAULT == null ? transientContent != null : !TRANSIENT_CONTENT_EDEFAULT.equals(transientContent);
			case MetadataPackage.ASPECT_ENTRY__DIAGNOSTICS:
				return diagnostics != null && !diagnostics.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (typeId: ");
		result.append(typeId);
		result.append(", transientContent: ");
		result.append(transientContent);
		result.append(')');
		return result.toString();
	}

} //AspectEntryImpl
