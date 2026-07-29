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

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage;
import org.eclipse.fennec.emf.osgi.model.metadata.ReferenceMetadata;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Reference Metadata</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl#getEReference <em>EReference</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl#isContainment <em>Containment</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl#getTargetClassMetadata <em>Target Class Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl#getOppositeMetadata <em>Opposite Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ReferenceMetadataImpl#isHasBidirectional <em>Has Bidirectional</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ReferenceMetadataImpl extends FeatureMetadataImpl implements ReferenceMetadata {
	/**
	 * The cached value of the '{@link #getEReference() <em>EReference</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEReference()
	 * @generated
	 * @ordered
	 */
	protected EReference eReference;

	/**
	 * The default value of the '{@link #isContainment() <em>Containment</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isContainment()
	 * @generated
	 * @ordered
	 */
	protected static final boolean CONTAINMENT_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isContainment() <em>Containment</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isContainment()
	 * @generated
	 * @ordered
	 */
	protected boolean containment = CONTAINMENT_EDEFAULT;

	/**
	 * The cached value of the '{@link #getTargetClassMetadata() <em>Target Class Metadata</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetClassMetadata()
	 * @generated
	 * @ordered
	 */
	protected ClassMetadata targetClassMetadata;

	/**
	 * The cached value of the '{@link #getOppositeMetadata() <em>Opposite Metadata</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOppositeMetadata()
	 * @generated
	 * @ordered
	 */
	protected ReferenceMetadata oppositeMetadata;

	/**
	 * The default value of the '{@link #isHasBidirectional() <em>Has Bidirectional</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isHasBidirectional()
	 * @generated
	 * @ordered
	 */
	protected static final boolean HAS_BIDIRECTIONAL_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isHasBidirectional() <em>Has Bidirectional</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isHasBidirectional()
	 * @generated
	 * @ordered
	 */
	protected boolean hasBidirectional = HAS_BIDIRECTIONAL_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ReferenceMetadataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return MetadataPackage.Literals.REFERENCE_METADATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getEReference() {
		if (eReference != null && eReference.eIsProxy()) {
			InternalEObject oldEReference = (InternalEObject)eReference;
			eReference = (EReference)eResolveProxy(oldEReference);
			if (eReference != oldEReference) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.REFERENCE_METADATA__EREFERENCE, oldEReference, eReference));
			}
		}
		return eReference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference basicGetEReference() {
		return eReference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEReference(EReference newEReference) {
		EReference oldEReference = eReference;
		eReference = newEReference;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.REFERENCE_METADATA__EREFERENCE, oldEReference, eReference));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isContainment() {
		return containment;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setContainment(boolean newContainment) {
		boolean oldContainment = containment;
		containment = newContainment;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.REFERENCE_METADATA__CONTAINMENT, oldContainment, containment));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ClassMetadata getTargetClassMetadata() {
		if (targetClassMetadata != null && targetClassMetadata.eIsProxy()) {
			InternalEObject oldTargetClassMetadata = (InternalEObject)targetClassMetadata;
			targetClassMetadata = (ClassMetadata)eResolveProxy(oldTargetClassMetadata);
			if (targetClassMetadata != oldTargetClassMetadata) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.REFERENCE_METADATA__TARGET_CLASS_METADATA, oldTargetClassMetadata, targetClassMetadata));
			}
		}
		return targetClassMetadata;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ClassMetadata basicGetTargetClassMetadata() {
		return targetClassMetadata;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTargetClassMetadata(ClassMetadata newTargetClassMetadata) {
		ClassMetadata oldTargetClassMetadata = targetClassMetadata;
		targetClassMetadata = newTargetClassMetadata;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.REFERENCE_METADATA__TARGET_CLASS_METADATA, oldTargetClassMetadata, targetClassMetadata));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ReferenceMetadata getOppositeMetadata() {
		if (oppositeMetadata != null && oppositeMetadata.eIsProxy()) {
			InternalEObject oldOppositeMetadata = (InternalEObject)oppositeMetadata;
			oppositeMetadata = (ReferenceMetadata)eResolveProxy(oldOppositeMetadata);
			if (oppositeMetadata != oldOppositeMetadata) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.REFERENCE_METADATA__OPPOSITE_METADATA, oldOppositeMetadata, oppositeMetadata));
			}
		}
		return oppositeMetadata;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ReferenceMetadata basicGetOppositeMetadata() {
		return oppositeMetadata;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOppositeMetadata(ReferenceMetadata newOppositeMetadata) {
		ReferenceMetadata oldOppositeMetadata = oppositeMetadata;
		oppositeMetadata = newOppositeMetadata;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.REFERENCE_METADATA__OPPOSITE_METADATA, oldOppositeMetadata, oppositeMetadata));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isHasBidirectional() {
		return hasBidirectional;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setHasBidirectional(boolean newHasBidirectional) {
		boolean oldHasBidirectional = hasBidirectional;
		hasBidirectional = newHasBidirectional;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.REFERENCE_METADATA__HAS_BIDIRECTIONAL, oldHasBidirectional, hasBidirectional));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case MetadataPackage.REFERENCE_METADATA__EREFERENCE:
				if (resolve) return getEReference();
				return basicGetEReference();
			case MetadataPackage.REFERENCE_METADATA__CONTAINMENT:
				return isContainment();
			case MetadataPackage.REFERENCE_METADATA__TARGET_CLASS_METADATA:
				if (resolve) return getTargetClassMetadata();
				return basicGetTargetClassMetadata();
			case MetadataPackage.REFERENCE_METADATA__OPPOSITE_METADATA:
				if (resolve) return getOppositeMetadata();
				return basicGetOppositeMetadata();
			case MetadataPackage.REFERENCE_METADATA__HAS_BIDIRECTIONAL:
				return isHasBidirectional();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case MetadataPackage.REFERENCE_METADATA__EREFERENCE:
				setEReference((EReference)newValue);
				return;
			case MetadataPackage.REFERENCE_METADATA__CONTAINMENT:
				setContainment((Boolean)newValue);
				return;
			case MetadataPackage.REFERENCE_METADATA__TARGET_CLASS_METADATA:
				setTargetClassMetadata((ClassMetadata)newValue);
				return;
			case MetadataPackage.REFERENCE_METADATA__OPPOSITE_METADATA:
				setOppositeMetadata((ReferenceMetadata)newValue);
				return;
			case MetadataPackage.REFERENCE_METADATA__HAS_BIDIRECTIONAL:
				setHasBidirectional((Boolean)newValue);
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
			case MetadataPackage.REFERENCE_METADATA__EREFERENCE:
				setEReference((EReference)null);
				return;
			case MetadataPackage.REFERENCE_METADATA__CONTAINMENT:
				setContainment(CONTAINMENT_EDEFAULT);
				return;
			case MetadataPackage.REFERENCE_METADATA__TARGET_CLASS_METADATA:
				setTargetClassMetadata((ClassMetadata)null);
				return;
			case MetadataPackage.REFERENCE_METADATA__OPPOSITE_METADATA:
				setOppositeMetadata((ReferenceMetadata)null);
				return;
			case MetadataPackage.REFERENCE_METADATA__HAS_BIDIRECTIONAL:
				setHasBidirectional(HAS_BIDIRECTIONAL_EDEFAULT);
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
			case MetadataPackage.REFERENCE_METADATA__EREFERENCE:
				return eReference != null;
			case MetadataPackage.REFERENCE_METADATA__CONTAINMENT:
				return containment != CONTAINMENT_EDEFAULT;
			case MetadataPackage.REFERENCE_METADATA__TARGET_CLASS_METADATA:
				return targetClassMetadata != null;
			case MetadataPackage.REFERENCE_METADATA__OPPOSITE_METADATA:
				return oppositeMetadata != null;
			case MetadataPackage.REFERENCE_METADATA__HAS_BIDIRECTIONAL:
				return hasBidirectional != HAS_BIDIRECTIONAL_EDEFAULT;
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
		result.append(" (containment: ");
		result.append(containment);
		result.append(", hasBidirectional: ");
		result.append(hasBidirectional);
		result.append(')');
		return result.toString();
	}

} //ReferenceMetadataImpl
