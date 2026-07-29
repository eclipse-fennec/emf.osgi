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
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.FeatureMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Parameter Metadata</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl#getDiagnostics <em>Diagnostics</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl#getAllDiagnostics <em>All Diagnostics</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl#getOperationMetadata <em>Operation Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl#getEParameter <em>EParameter</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ParameterMetadataImpl#getTypeMetadata <em>Type Metadata</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ParameterMetadataImpl extends MinimalEObjectImpl.Container implements ParameterMetadata {
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
	 * The cached value of the '{@link #getEParameter() <em>EParameter</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEParameter()
	 * @generated
	 * @ordered
	 */
	protected EParameter eParameter;

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
	 * The cached value of the '{@link #getTypeMetadata() <em>Type Metadata</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTypeMetadata()
	 * @generated
	 * @ordered
	 */
	protected ClassMetadata typeMetadata;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ParameterMetadataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return MetadataPackage.Literals.PARAMETER_METADATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<MetadataDiagnostic> getDiagnostics() {
		if (diagnostics == null) {
			diagnostics = new EObjectContainmentEList<MetadataDiagnostic>(MetadataDiagnostic.class, this, MetadataPackage.PARAMETER_METADATA__DIAGNOSTICS);
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
		    for (FeatureMetadata featureMetadata : ((ClassMetadata) this).getFeatures()) {
		        result.addAll(featureMetadata.getAllDiagnostics());
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
	public OperationMetadata getOperationMetadata() {
		if (eContainerFeatureID() != MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA) return null;
		return (OperationMetadata)eInternalContainer();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetOperationMetadata(OperationMetadata newOperationMetadata, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newOperationMetadata, MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOperationMetadata(OperationMetadata newOperationMetadata) {
		if (newOperationMetadata != eInternalContainer() || (eContainerFeatureID() != MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA && newOperationMetadata != null)) {
			if (EcoreUtil.isAncestor(this, newOperationMetadata))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newOperationMetadata != null)
				msgs = ((InternalEObject)newOperationMetadata).eInverseAdd(this, MetadataPackage.OPERATION_METADATA__PARAMETERS, OperationMetadata.class, msgs);
			msgs = basicSetOperationMetadata(newOperationMetadata, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA, newOperationMetadata, newOperationMetadata));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EParameter getEParameter() {
		if (eParameter != null && eParameter.eIsProxy()) {
			InternalEObject oldEParameter = (InternalEObject)eParameter;
			eParameter = (EParameter)eResolveProxy(oldEParameter);
			if (eParameter != oldEParameter) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.PARAMETER_METADATA__EPARAMETER, oldEParameter, eParameter));
			}
		}
		return eParameter;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EParameter basicGetEParameter() {
		return eParameter;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEParameter(EParameter newEParameter) {
		EParameter oldEParameter = eParameter;
		eParameter = newEParameter;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.PARAMETER_METADATA__EPARAMETER, oldEParameter, eParameter));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.PARAMETER_METADATA__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ClassMetadata getTypeMetadata() {
		if (typeMetadata != null && typeMetadata.eIsProxy()) {
			InternalEObject oldTypeMetadata = (InternalEObject)typeMetadata;
			typeMetadata = (ClassMetadata)eResolveProxy(oldTypeMetadata);
			if (typeMetadata != oldTypeMetadata) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.PARAMETER_METADATA__TYPE_METADATA, oldTypeMetadata, typeMetadata));
			}
		}
		return typeMetadata;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ClassMetadata basicGetTypeMetadata() {
		return typeMetadata;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTypeMetadata(ClassMetadata newTypeMetadata) {
		ClassMetadata oldTypeMetadata = typeMetadata;
		typeMetadata = newTypeMetadata;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.PARAMETER_METADATA__TYPE_METADATA, oldTypeMetadata, typeMetadata));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetOperationMetadata((OperationMetadata)otherEnd, msgs);
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
			case MetadataPackage.PARAMETER_METADATA__DIAGNOSTICS:
				return ((InternalEList<?>)getDiagnostics()).basicRemove(otherEnd, msgs);
			case MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA:
				return basicSetOperationMetadata(null, msgs);
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
			case MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA:
				return eInternalContainer().eInverseRemove(this, MetadataPackage.OPERATION_METADATA__PARAMETERS, OperationMetadata.class, msgs);
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
			case MetadataPackage.PARAMETER_METADATA__DIAGNOSTICS:
				return getDiagnostics();
			case MetadataPackage.PARAMETER_METADATA__ALL_DIAGNOSTICS:
				return getAllDiagnostics();
			case MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA:
				return getOperationMetadata();
			case MetadataPackage.PARAMETER_METADATA__EPARAMETER:
				if (resolve) return getEParameter();
				return basicGetEParameter();
			case MetadataPackage.PARAMETER_METADATA__NAME:
				return getName();
			case MetadataPackage.PARAMETER_METADATA__TYPE_METADATA:
				if (resolve) return getTypeMetadata();
				return basicGetTypeMetadata();
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
			case MetadataPackage.PARAMETER_METADATA__DIAGNOSTICS:
				getDiagnostics().clear();
				getDiagnostics().addAll((Collection<? extends MetadataDiagnostic>)newValue);
				return;
			case MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA:
				setOperationMetadata((OperationMetadata)newValue);
				return;
			case MetadataPackage.PARAMETER_METADATA__EPARAMETER:
				setEParameter((EParameter)newValue);
				return;
			case MetadataPackage.PARAMETER_METADATA__NAME:
				setName((String)newValue);
				return;
			case MetadataPackage.PARAMETER_METADATA__TYPE_METADATA:
				setTypeMetadata((ClassMetadata)newValue);
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
			case MetadataPackage.PARAMETER_METADATA__DIAGNOSTICS:
				getDiagnostics().clear();
				return;
			case MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA:
				setOperationMetadata((OperationMetadata)null);
				return;
			case MetadataPackage.PARAMETER_METADATA__EPARAMETER:
				setEParameter((EParameter)null);
				return;
			case MetadataPackage.PARAMETER_METADATA__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MetadataPackage.PARAMETER_METADATA__TYPE_METADATA:
				setTypeMetadata((ClassMetadata)null);
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
			case MetadataPackage.PARAMETER_METADATA__DIAGNOSTICS:
				return diagnostics != null && !diagnostics.isEmpty();
			case MetadataPackage.PARAMETER_METADATA__ALL_DIAGNOSTICS:
				return !getAllDiagnostics().isEmpty();
			case MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA:
				return getOperationMetadata() != null;
			case MetadataPackage.PARAMETER_METADATA__EPARAMETER:
				return eParameter != null;
			case MetadataPackage.PARAMETER_METADATA__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MetadataPackage.PARAMETER_METADATA__TYPE_METADATA:
				return typeMetadata != null;
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
		result.append(')');
		return result.toString();
	}

} //ParameterMetadataImpl
