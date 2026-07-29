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
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
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
 * An implementation of the model object '<em><b>Operation Metadata</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getDiagnostics <em>Diagnostics</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getAllDiagnostics <em>All Diagnostics</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getClassMetadata <em>Class Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getEOperation <em>EOperation</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getOperationID <em>Operation ID</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getReturnTypeMetadata <em>Return Type Metadata</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getParameters <em>Parameters</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.OperationMetadataImpl#getAspects <em>Aspects</em>}</li>
 * </ul>
 *
 * @generated
 */
public class OperationMetadataImpl extends MinimalEObjectImpl.Container implements OperationMetadata {
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
	 * The cached value of the '{@link #getEOperation() <em>EOperation</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEOperation()
	 * @generated
	 * @ordered
	 */
	protected EOperation eOperation;

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
	 * The default value of the '{@link #getOperationID() <em>Operation ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOperationID()
	 * @generated
	 * @ordered
	 */
	protected static final int OPERATION_ID_EDEFAULT = -1;

	/**
	 * The cached value of the '{@link #getOperationID() <em>Operation ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOperationID()
	 * @generated
	 * @ordered
	 */
	protected int operationID = OPERATION_ID_EDEFAULT;

	/**
	 * The cached value of the '{@link #getReturnTypeMetadata() <em>Return Type Metadata</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnTypeMetadata()
	 * @generated
	 * @ordered
	 */
	protected ClassMetadata returnTypeMetadata;

	/**
	 * The cached value of the '{@link #getParameters() <em>Parameters</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getParameters()
	 * @generated
	 * @ordered
	 */
	protected EList<ParameterMetadata> parameters;

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
	protected OperationMetadataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return MetadataPackage.Literals.OPERATION_METADATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<MetadataDiagnostic> getDiagnostics() {
		if (diagnostics == null) {
			diagnostics = new EObjectContainmentEList<MetadataDiagnostic>(MetadataDiagnostic.class, this, MetadataPackage.OPERATION_METADATA__DIAGNOSTICS);
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
	public ClassMetadata getClassMetadata() {
		if (eContainerFeatureID() != MetadataPackage.OPERATION_METADATA__CLASS_METADATA) return null;
		return (ClassMetadata)eInternalContainer();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetClassMetadata(ClassMetadata newClassMetadata, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newClassMetadata, MetadataPackage.OPERATION_METADATA__CLASS_METADATA, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setClassMetadata(ClassMetadata newClassMetadata) {
		if (newClassMetadata != eInternalContainer() || (eContainerFeatureID() != MetadataPackage.OPERATION_METADATA__CLASS_METADATA && newClassMetadata != null)) {
			if (EcoreUtil.isAncestor(this, newClassMetadata))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newClassMetadata != null)
				msgs = ((InternalEObject)newClassMetadata).eInverseAdd(this, MetadataPackage.CLASS_METADATA__OPERATIONS, ClassMetadata.class, msgs);
			msgs = basicSetClassMetadata(newClassMetadata, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.OPERATION_METADATA__CLASS_METADATA, newClassMetadata, newClassMetadata));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getEOperation() {
		if (eOperation != null && eOperation.eIsProxy()) {
			InternalEObject oldEOperation = (InternalEObject)eOperation;
			eOperation = (EOperation)eResolveProxy(oldEOperation);
			if (eOperation != oldEOperation) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.OPERATION_METADATA__EOPERATION, oldEOperation, eOperation));
			}
		}
		return eOperation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EOperation basicGetEOperation() {
		return eOperation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEOperation(EOperation newEOperation) {
		EOperation oldEOperation = eOperation;
		eOperation = newEOperation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.OPERATION_METADATA__EOPERATION, oldEOperation, eOperation));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.OPERATION_METADATA__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getOperationID() {
		return operationID;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOperationID(int newOperationID) {
		int oldOperationID = operationID;
		operationID = newOperationID;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.OPERATION_METADATA__OPERATION_ID, oldOperationID, operationID));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ClassMetadata getReturnTypeMetadata() {
		if (returnTypeMetadata != null && returnTypeMetadata.eIsProxy()) {
			InternalEObject oldReturnTypeMetadata = (InternalEObject)returnTypeMetadata;
			returnTypeMetadata = (ClassMetadata)eResolveProxy(oldReturnTypeMetadata);
			if (returnTypeMetadata != oldReturnTypeMetadata) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.OPERATION_METADATA__RETURN_TYPE_METADATA, oldReturnTypeMetadata, returnTypeMetadata));
			}
		}
		return returnTypeMetadata;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ClassMetadata basicGetReturnTypeMetadata() {
		return returnTypeMetadata;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setReturnTypeMetadata(ClassMetadata newReturnTypeMetadata) {
		ClassMetadata oldReturnTypeMetadata = returnTypeMetadata;
		returnTypeMetadata = newReturnTypeMetadata;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.OPERATION_METADATA__RETURN_TYPE_METADATA, oldReturnTypeMetadata, returnTypeMetadata));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ParameterMetadata> getParameters() {
		if (parameters == null) {
			parameters = new EObjectContainmentWithInverseEList<ParameterMetadata>(ParameterMetadata.class, this, MetadataPackage.OPERATION_METADATA__PARAMETERS, MetadataPackage.PARAMETER_METADATA__OPERATION_METADATA);
		}
		return parameters;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<AspectEntry> getAspects() {
		if (aspects == null) {
			aspects = new EObjectContainmentEList<AspectEntry>(AspectEntry.class, this, MetadataPackage.OPERATION_METADATA__ASPECTS);
		}
		return aspects;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case MetadataPackage.OPERATION_METADATA__CLASS_METADATA:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetClassMetadata((ClassMetadata)otherEnd, msgs);
			case MetadataPackage.OPERATION_METADATA__PARAMETERS:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getParameters()).basicAdd(otherEnd, msgs);
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
			case MetadataPackage.OPERATION_METADATA__DIAGNOSTICS:
				return ((InternalEList<?>)getDiagnostics()).basicRemove(otherEnd, msgs);
			case MetadataPackage.OPERATION_METADATA__CLASS_METADATA:
				return basicSetClassMetadata(null, msgs);
			case MetadataPackage.OPERATION_METADATA__PARAMETERS:
				return ((InternalEList<?>)getParameters()).basicRemove(otherEnd, msgs);
			case MetadataPackage.OPERATION_METADATA__ASPECTS:
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
			case MetadataPackage.OPERATION_METADATA__CLASS_METADATA:
				return eInternalContainer().eInverseRemove(this, MetadataPackage.CLASS_METADATA__OPERATIONS, ClassMetadata.class, msgs);
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
			case MetadataPackage.OPERATION_METADATA__DIAGNOSTICS:
				return getDiagnostics();
			case MetadataPackage.OPERATION_METADATA__ALL_DIAGNOSTICS:
				return getAllDiagnostics();
			case MetadataPackage.OPERATION_METADATA__CLASS_METADATA:
				return getClassMetadata();
			case MetadataPackage.OPERATION_METADATA__EOPERATION:
				if (resolve) return getEOperation();
				return basicGetEOperation();
			case MetadataPackage.OPERATION_METADATA__NAME:
				return getName();
			case MetadataPackage.OPERATION_METADATA__OPERATION_ID:
				return getOperationID();
			case MetadataPackage.OPERATION_METADATA__RETURN_TYPE_METADATA:
				if (resolve) return getReturnTypeMetadata();
				return basicGetReturnTypeMetadata();
			case MetadataPackage.OPERATION_METADATA__PARAMETERS:
				return getParameters();
			case MetadataPackage.OPERATION_METADATA__ASPECTS:
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
			case MetadataPackage.OPERATION_METADATA__DIAGNOSTICS:
				getDiagnostics().clear();
				getDiagnostics().addAll((Collection<? extends MetadataDiagnostic>)newValue);
				return;
			case MetadataPackage.OPERATION_METADATA__CLASS_METADATA:
				setClassMetadata((ClassMetadata)newValue);
				return;
			case MetadataPackage.OPERATION_METADATA__EOPERATION:
				setEOperation((EOperation)newValue);
				return;
			case MetadataPackage.OPERATION_METADATA__NAME:
				setName((String)newValue);
				return;
			case MetadataPackage.OPERATION_METADATA__OPERATION_ID:
				setOperationID((Integer)newValue);
				return;
			case MetadataPackage.OPERATION_METADATA__RETURN_TYPE_METADATA:
				setReturnTypeMetadata((ClassMetadata)newValue);
				return;
			case MetadataPackage.OPERATION_METADATA__PARAMETERS:
				getParameters().clear();
				getParameters().addAll((Collection<? extends ParameterMetadata>)newValue);
				return;
			case MetadataPackage.OPERATION_METADATA__ASPECTS:
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
			case MetadataPackage.OPERATION_METADATA__DIAGNOSTICS:
				getDiagnostics().clear();
				return;
			case MetadataPackage.OPERATION_METADATA__CLASS_METADATA:
				setClassMetadata((ClassMetadata)null);
				return;
			case MetadataPackage.OPERATION_METADATA__EOPERATION:
				setEOperation((EOperation)null);
				return;
			case MetadataPackage.OPERATION_METADATA__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MetadataPackage.OPERATION_METADATA__OPERATION_ID:
				setOperationID(OPERATION_ID_EDEFAULT);
				return;
			case MetadataPackage.OPERATION_METADATA__RETURN_TYPE_METADATA:
				setReturnTypeMetadata((ClassMetadata)null);
				return;
			case MetadataPackage.OPERATION_METADATA__PARAMETERS:
				getParameters().clear();
				return;
			case MetadataPackage.OPERATION_METADATA__ASPECTS:
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
			case MetadataPackage.OPERATION_METADATA__DIAGNOSTICS:
				return diagnostics != null && !diagnostics.isEmpty();
			case MetadataPackage.OPERATION_METADATA__ALL_DIAGNOSTICS:
				return !getAllDiagnostics().isEmpty();
			case MetadataPackage.OPERATION_METADATA__CLASS_METADATA:
				return getClassMetadata() != null;
			case MetadataPackage.OPERATION_METADATA__EOPERATION:
				return eOperation != null;
			case MetadataPackage.OPERATION_METADATA__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MetadataPackage.OPERATION_METADATA__OPERATION_ID:
				return operationID != OPERATION_ID_EDEFAULT;
			case MetadataPackage.OPERATION_METADATA__RETURN_TYPE_METADATA:
				return returnTypeMetadata != null;
			case MetadataPackage.OPERATION_METADATA__PARAMETERS:
				return parameters != null && !parameters.isEmpty();
			case MetadataPackage.OPERATION_METADATA__ASPECTS:
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
		result.append(", operationID: ");
		result.append(operationID);
		result.append(')');
		return result.toString();
	}

} //OperationMetadataImpl
