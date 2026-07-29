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

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Feature Metadata</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl#getDiagnostics <em>Diagnostics</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl#getAllDiagnostics <em>All Diagnostics</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl#getClassMetadata <em>Class Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl#getEFeature <em>EFeature</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl#getExtendedMetaDataName <em>Extended Meta Data Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl#getFeatureID <em>Feature ID</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.FeatureMetadataImpl#getAspects <em>Aspects</em>}</li>
 * </ul>
 *
 * @generated
 */
public abstract class FeatureMetadataImpl extends MinimalEObjectImpl.Container implements FeatureMetadata {
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
	 * The cached value of the '{@link #getEFeature() <em>EFeature</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEFeature()
	 * @generated
	 * @ordered
	 */
	protected EStructuralFeature eFeature;

	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getExtendedMetaDataName() <em>Extended Meta Data Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExtendedMetaDataName()
	 * @generated
	 * @ordered
	 */
	protected static final String EXTENDED_META_DATA_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getExtendedMetaDataName() <em>Extended Meta Data Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExtendedMetaDataName()
	 * @generated
	 * @ordered
	 */
	protected String extendedMetaDataName = EXTENDED_META_DATA_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getFeatureID() <em>Feature ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFeatureID()
	 * @generated
	 * @ordered
	 */
	protected static final int FEATURE_ID_EDEFAULT = -1;

	/**
	 * The cached value of the '{@link #getFeatureID() <em>Feature ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFeatureID()
	 * @generated
	 * @ordered
	 */
	protected int featureID = FEATURE_ID_EDEFAULT;

	/**
	 * The cached value of the '{@link #getAspects() <em>Aspects</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAspects()
	 * @generated
	 * @ordered
	 */
	protected EList<AspectEntry> aspects;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected FeatureMetadataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return MetadataPackage.Literals.FEATURE_METADATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<MetadataDiagnostic> getDiagnostics() {
		if (diagnostics == null) {
			diagnostics = new EObjectContainmentEList<MetadataDiagnostic>(MetadataDiagnostic.class, this, MetadataPackage.FEATURE_METADATA__DIAGNOSTICS);
		}
		return diagnostics;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<MetadataDiagnostic> getAllDiagnostics() {
		EList<MetadataDiagnostic> result = new BasicEList<>();
		result.addAll(getDiagnostics());
		EClass eClassType = eClass();
		if (eClassType == MetadataPackage.Literals.PACKAGE_METADATA) {
		    for (ClassMetadata classMetadata : ((PackageMetadata) this).getClasses()) {
		        result.addAll(classMetadata.getAllDiagnostics());
		    }
		} else if (eClassType == MetadataPackage.Literals.CLASS_METADATA) {
		    ClassMetadata classMetadata = (ClassMetadata) this;
		    for (FeatureMetadata featureMetadata : classMetadata.getFeatures()) {
		        result.addAll(featureMetadata.getAllDiagnostics());
		    }
		    for (OperationMetadata operationMetadata : classMetadata.getOperations()) {
		        result.addAll(operationMetadata.getAllDiagnostics());
		    }
		} else if (eClassType == MetadataPackage.Literals.OPERATION_METADATA) {
		    for (ParameterMetadata parameterMetadata : ((OperationMetadata) this).getParameters()) {
		        result.addAll(parameterMetadata.getAllDiagnostics());
		    }
		}
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ClassMetadata getClassMetadata() {
		if (eContainerFeatureID() != MetadataPackage.FEATURE_METADATA__CLASS_METADATA) return null;
		return (ClassMetadata)eInternalContainer();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetClassMetadata(ClassMetadata newClassMetadata, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newClassMetadata, MetadataPackage.FEATURE_METADATA__CLASS_METADATA, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setClassMetadata(ClassMetadata newClassMetadata) {
		if (newClassMetadata != eInternalContainer() || (eContainerFeatureID() != MetadataPackage.FEATURE_METADATA__CLASS_METADATA && newClassMetadata != null)) {
			if (EcoreUtil.isAncestor(this, newClassMetadata))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newClassMetadata != null)
				msgs = ((InternalEObject)newClassMetadata).eInverseAdd(this, MetadataPackage.CLASS_METADATA__FEATURES, ClassMetadata.class, msgs);
			msgs = basicSetClassMetadata(newClassMetadata, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.FEATURE_METADATA__CLASS_METADATA, newClassMetadata, newClassMetadata));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EStructuralFeature getEFeature() {
		if (eFeature != null && eFeature.eIsProxy()) {
			InternalEObject oldEFeature = (InternalEObject)eFeature;
			eFeature = (EStructuralFeature)eResolveProxy(oldEFeature);
			if (eFeature != oldEFeature) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.FEATURE_METADATA__EFEATURE, oldEFeature, eFeature));
			}
		}
		return eFeature;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EStructuralFeature basicGetEFeature() {
		return eFeature;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEFeature(EStructuralFeature newEFeature) {
		EStructuralFeature oldEFeature = eFeature;
		eFeature = newEFeature;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.FEATURE_METADATA__EFEATURE, oldEFeature, eFeature));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.FEATURE_METADATA__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getExtendedMetaDataName() {
		return extendedMetaDataName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setExtendedMetaDataName(String newExtendedMetaDataName) {
		String oldExtendedMetaDataName = extendedMetaDataName;
		extendedMetaDataName = newExtendedMetaDataName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.FEATURE_METADATA__EXTENDED_META_DATA_NAME, oldExtendedMetaDataName, extendedMetaDataName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getFeatureID() {
		return featureID;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFeatureID(int newFeatureID) {
		int oldFeatureID = featureID;
		featureID = newFeatureID;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.FEATURE_METADATA__FEATURE_ID, oldFeatureID, featureID));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<AspectEntry> getAspects() {
		if (aspects == null) {
			aspects = new EObjectContainmentEList<AspectEntry>(AspectEntry.class, this, MetadataPackage.FEATURE_METADATA__ASPECTS);
		}
		return aspects;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case MetadataPackage.FEATURE_METADATA__CLASS_METADATA:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetClassMetadata((ClassMetadata)otherEnd, msgs);
		}
		return super.eInverseAdd(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case MetadataPackage.FEATURE_METADATA__DIAGNOSTICS:
				return ((InternalEList<?>)getDiagnostics()).basicRemove(otherEnd, msgs);
			case MetadataPackage.FEATURE_METADATA__CLASS_METADATA:
				return basicSetClassMetadata(null, msgs);
			case MetadataPackage.FEATURE_METADATA__ASPECTS:
				return ((InternalEList<?>)getAspects()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eBasicRemoveFromContainerFeature(NotificationChain msgs) {
		switch (eContainerFeatureID()) {
			case MetadataPackage.FEATURE_METADATA__CLASS_METADATA:
				return eInternalContainer().eInverseRemove(this, MetadataPackage.CLASS_METADATA__FEATURES, ClassMetadata.class, msgs);
		}
		return super.eBasicRemoveFromContainerFeature(msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case MetadataPackage.FEATURE_METADATA__DIAGNOSTICS:
				return getDiagnostics();
			case MetadataPackage.FEATURE_METADATA__ALL_DIAGNOSTICS:
				return getAllDiagnostics();
			case MetadataPackage.FEATURE_METADATA__CLASS_METADATA:
				return getClassMetadata();
			case MetadataPackage.FEATURE_METADATA__EFEATURE:
				if (resolve) return getEFeature();
				return basicGetEFeature();
			case MetadataPackage.FEATURE_METADATA__NAME:
				return getName();
			case MetadataPackage.FEATURE_METADATA__EXTENDED_META_DATA_NAME:
				return getExtendedMetaDataName();
			case MetadataPackage.FEATURE_METADATA__FEATURE_ID:
				return getFeatureID();
			case MetadataPackage.FEATURE_METADATA__ASPECTS:
				return getAspects();
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
			case MetadataPackage.FEATURE_METADATA__DIAGNOSTICS:
				getDiagnostics().clear();
				getDiagnostics().addAll((Collection<? extends MetadataDiagnostic>)newValue);
				return;
			case MetadataPackage.FEATURE_METADATA__CLASS_METADATA:
				setClassMetadata((ClassMetadata)newValue);
				return;
			case MetadataPackage.FEATURE_METADATA__EFEATURE:
				setEFeature((EStructuralFeature)newValue);
				return;
			case MetadataPackage.FEATURE_METADATA__NAME:
				setName((String)newValue);
				return;
			case MetadataPackage.FEATURE_METADATA__EXTENDED_META_DATA_NAME:
				setExtendedMetaDataName((String)newValue);
				return;
			case MetadataPackage.FEATURE_METADATA__FEATURE_ID:
				setFeatureID((Integer)newValue);
				return;
			case MetadataPackage.FEATURE_METADATA__ASPECTS:
				getAspects().clear();
				getAspects().addAll((Collection<? extends AspectEntry>)newValue);
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
			case MetadataPackage.FEATURE_METADATA__DIAGNOSTICS:
				getDiagnostics().clear();
				return;
			case MetadataPackage.FEATURE_METADATA__CLASS_METADATA:
				setClassMetadata((ClassMetadata)null);
				return;
			case MetadataPackage.FEATURE_METADATA__EFEATURE:
				setEFeature((EStructuralFeature)null);
				return;
			case MetadataPackage.FEATURE_METADATA__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MetadataPackage.FEATURE_METADATA__EXTENDED_META_DATA_NAME:
				setExtendedMetaDataName(EXTENDED_META_DATA_NAME_EDEFAULT);
				return;
			case MetadataPackage.FEATURE_METADATA__FEATURE_ID:
				setFeatureID(FEATURE_ID_EDEFAULT);
				return;
			case MetadataPackage.FEATURE_METADATA__ASPECTS:
				getAspects().clear();
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
			case MetadataPackage.FEATURE_METADATA__DIAGNOSTICS:
				return diagnostics != null && !diagnostics.isEmpty();
			case MetadataPackage.FEATURE_METADATA__ALL_DIAGNOSTICS:
				return !getAllDiagnostics().isEmpty();
			case MetadataPackage.FEATURE_METADATA__CLASS_METADATA:
				return getClassMetadata() != null;
			case MetadataPackage.FEATURE_METADATA__EFEATURE:
				return eFeature != null;
			case MetadataPackage.FEATURE_METADATA__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MetadataPackage.FEATURE_METADATA__EXTENDED_META_DATA_NAME:
				return EXTENDED_META_DATA_NAME_EDEFAULT == null ? extendedMetaDataName != null : !EXTENDED_META_DATA_NAME_EDEFAULT.equals(extendedMetaDataName);
			case MetadataPackage.FEATURE_METADATA__FEATURE_ID:
				return this.featureID != FEATURE_ID_EDEFAULT;
			case MetadataPackage.FEATURE_METADATA__ASPECTS:
				return aspects != null && !aspects.isEmpty();
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
		result.append(" (name: ");
		result.append(name);
		result.append(", extendedMetaDataName: ");
		result.append(extendedMetaDataName);
		result.append(", featureID: ");
		result.append(featureID);
		result.append(')');
		return result.toString();
	}

} //FeatureMetadataImpl
