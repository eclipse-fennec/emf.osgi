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
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
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
 * An implementation of the model object '<em><b>Class Metadata</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getDiagnostics <em>Diagnostics</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getAllDiagnostics <em>All Diagnostics</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getPackage <em>Package</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getEClass <em>EClass</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getClassifierID <em>Classifier ID</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getTypeURI <em>Type URI</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getFeatures <em>Features</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getOperations <em>Operations</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getSuperTypes <em>Super Types</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getAllSuperTypes <em>All Super Types</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getIdFeatures <em>Id Features</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#isHasId <em>Has Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.impl.ClassMetadataImpl#getAspects <em>Aspects</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ClassMetadataImpl extends MinimalEObjectImpl.Container implements ClassMetadata {
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
	 * The cached value of the '{@link #getEClass() <em>EClass</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEClass()
	 * @generated
	 * @ordered
	 */
	protected EClass eClass;

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
	 * The default value of the '{@link #getClassifierID() <em>Classifier ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClassifierID()
	 * @generated
	 * @ordered
	 */
	protected static final int CLASSIFIER_ID_EDEFAULT = -1;

	/**
	 * The cached value of the '{@link #getClassifierID() <em>Classifier ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClassifierID()
	 * @generated
	 * @ordered
	 */
	protected int classifierID = CLASSIFIER_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getTypeURI() <em>Type URI</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTypeURI()
	 * @generated
	 * @ordered
	 */
	protected static final String TYPE_URI_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTypeURI() <em>Type URI</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTypeURI()
	 * @generated
	 * @ordered
	 */
	protected String typeURI = TYPE_URI_EDEFAULT;

	/**
	 * The cached value of the '{@link #getFeatures() <em>Features</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFeatures()
	 * @generated
	 * @ordered
	 */
	protected EList<FeatureMetadata> features;

	/**
	 * The cached value of the '{@link #getOperations() <em>Operations</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOperations()
	 * @generated
	 * @ordered
	 */
	protected EList<OperationMetadata> operations;

	/**
	 * The cached value of the '{@link #getSuperTypes() <em>Super Types</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSuperTypes()
	 * @generated
	 * @ordered
	 */
	protected EList<ClassMetadata> superTypes;

	/**
	 * The cached value of the '{@link #getAllSuperTypes() <em>All Super Types</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAllSuperTypes()
	 * @generated
	 * @ordered
	 */
	protected EList<ClassMetadata> allSuperTypes;

	/**
	 * The cached value of the '{@link #getIdFeatures() <em>Id Features</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIdFeatures()
	 * @generated
	 * @ordered
	 */
	protected EList<FeatureMetadata> idFeatures;

	/**
	 * The default value of the '{@link #isHasId() <em>Has Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isHasId()
	 * @generated
	 * @ordered
	 */
	protected static final boolean HAS_ID_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isHasId() <em>Has Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isHasId()
	 * @generated
	 * @ordered
	 */
	protected boolean hasId = HAS_ID_EDEFAULT;

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
	protected ClassMetadataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return MetadataPackage.Literals.CLASS_METADATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<MetadataDiagnostic> getDiagnostics() {
		if (diagnostics == null) {
			diagnostics = new EObjectContainmentEList<MetadataDiagnostic>(MetadataDiagnostic.class, this, MetadataPackage.CLASS_METADATA__DIAGNOSTICS);
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
	public PackageMetadata getPackage() {
		if (eContainerFeatureID() != MetadataPackage.CLASS_METADATA__PACKAGE) return null;
		return (PackageMetadata)eInternalContainer();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetPackage(PackageMetadata newPackage, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newPackage, MetadataPackage.CLASS_METADATA__PACKAGE, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPackage(PackageMetadata newPackage) {
		if (newPackage != eInternalContainer() || (eContainerFeatureID() != MetadataPackage.CLASS_METADATA__PACKAGE && newPackage != null)) {
			if (EcoreUtil.isAncestor(this, newPackage))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newPackage != null)
				msgs = ((InternalEObject)newPackage).eInverseAdd(this, MetadataPackage.PACKAGE_METADATA__CLASSES, PackageMetadata.class, msgs);
			msgs = basicSetPackage(newPackage, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.CLASS_METADATA__PACKAGE, newPackage, newPackage));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getEClass() {
		if (eClass != null && eClass.eIsProxy()) {
			InternalEObject oldEClass = (InternalEObject)eClass;
			eClass = (EClass)eResolveProxy(oldEClass);
			if (eClass != oldEClass) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, MetadataPackage.CLASS_METADATA__ECLASS, oldEClass, eClass));
			}
		}
		return eClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass basicGetEClass() {
		return eClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEClass(EClass newEClass) {
		EClass oldEClass = eClass;
		eClass = newEClass;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.CLASS_METADATA__ECLASS, oldEClass, eClass));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.CLASS_METADATA__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getClassifierID() {
		return classifierID;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setClassifierID(int newClassifierID) {
		int oldClassifierID = classifierID;
		classifierID = newClassifierID;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.CLASS_METADATA__CLASSIFIER_ID, oldClassifierID, classifierID));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTypeURI() {
		return typeURI;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTypeURI(String newTypeURI) {
		String oldTypeURI = typeURI;
		typeURI = newTypeURI;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.CLASS_METADATA__TYPE_URI, oldTypeURI, typeURI));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<FeatureMetadata> getFeatures() {
		if (features == null) {
			features = new EObjectContainmentWithInverseEList<FeatureMetadata>(FeatureMetadata.class, this, MetadataPackage.CLASS_METADATA__FEATURES, MetadataPackage.FEATURE_METADATA__CLASS_METADATA);
		}
		return features;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<OperationMetadata> getOperations() {
		if (operations == null) {
			operations = new EObjectContainmentWithInverseEList<OperationMetadata>(OperationMetadata.class, this, MetadataPackage.CLASS_METADATA__OPERATIONS, MetadataPackage.OPERATION_METADATA__CLASS_METADATA);
		}
		return operations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ClassMetadata> getSuperTypes() {
		if (superTypes == null) {
			superTypes = new EObjectResolvingEList<ClassMetadata>(ClassMetadata.class, this, MetadataPackage.CLASS_METADATA__SUPER_TYPES);
		}
		return superTypes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ClassMetadata> getAllSuperTypes() {
		if (allSuperTypes == null) {
			allSuperTypes = new EObjectResolvingEList<ClassMetadata>(ClassMetadata.class, this, MetadataPackage.CLASS_METADATA__ALL_SUPER_TYPES);
		}
		return allSuperTypes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<FeatureMetadata> getIdFeatures() {
		if (idFeatures == null) {
			idFeatures = new EObjectResolvingEList<FeatureMetadata>(FeatureMetadata.class, this, MetadataPackage.CLASS_METADATA__ID_FEATURES);
		}
		return idFeatures;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isHasId() {
		return hasId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setHasId(boolean newHasId) {
		boolean oldHasId = hasId;
		hasId = newHasId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.CLASS_METADATA__HAS_ID, oldHasId, hasId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<AspectEntry> getAspects() {
		if (aspects == null) {
			aspects = new EObjectContainmentEList<AspectEntry>(AspectEntry.class, this, MetadataPackage.CLASS_METADATA__ASPECTS);
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
			case MetadataPackage.CLASS_METADATA__PACKAGE:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetPackage((PackageMetadata)otherEnd, msgs);
			case MetadataPackage.CLASS_METADATA__FEATURES:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getFeatures()).basicAdd(otherEnd, msgs);
			case MetadataPackage.CLASS_METADATA__OPERATIONS:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getOperations()).basicAdd(otherEnd, msgs);
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
			case MetadataPackage.CLASS_METADATA__DIAGNOSTICS:
				return ((InternalEList<?>)getDiagnostics()).basicRemove(otherEnd, msgs);
			case MetadataPackage.CLASS_METADATA__PACKAGE:
				return basicSetPackage(null, msgs);
			case MetadataPackage.CLASS_METADATA__FEATURES:
				return ((InternalEList<?>)getFeatures()).basicRemove(otherEnd, msgs);
			case MetadataPackage.CLASS_METADATA__OPERATIONS:
				return ((InternalEList<?>)getOperations()).basicRemove(otherEnd, msgs);
			case MetadataPackage.CLASS_METADATA__ASPECTS:
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
			case MetadataPackage.CLASS_METADATA__PACKAGE:
				return eInternalContainer().eInverseRemove(this, MetadataPackage.PACKAGE_METADATA__CLASSES, PackageMetadata.class, msgs);
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
			case MetadataPackage.CLASS_METADATA__DIAGNOSTICS:
				return getDiagnostics();
			case MetadataPackage.CLASS_METADATA__ALL_DIAGNOSTICS:
				return getAllDiagnostics();
			case MetadataPackage.CLASS_METADATA__PACKAGE:
				return getPackage();
			case MetadataPackage.CLASS_METADATA__ECLASS:
				if (resolve) return getEClass();
				return basicGetEClass();
			case MetadataPackage.CLASS_METADATA__NAME:
				return getName();
			case MetadataPackage.CLASS_METADATA__CLASSIFIER_ID:
				return getClassifierID();
			case MetadataPackage.CLASS_METADATA__TYPE_URI:
				return getTypeURI();
			case MetadataPackage.CLASS_METADATA__FEATURES:
				return getFeatures();
			case MetadataPackage.CLASS_METADATA__OPERATIONS:
				return getOperations();
			case MetadataPackage.CLASS_METADATA__SUPER_TYPES:
				return getSuperTypes();
			case MetadataPackage.CLASS_METADATA__ALL_SUPER_TYPES:
				return getAllSuperTypes();
			case MetadataPackage.CLASS_METADATA__ID_FEATURES:
				return getIdFeatures();
			case MetadataPackage.CLASS_METADATA__HAS_ID:
				return isHasId();
			case MetadataPackage.CLASS_METADATA__ASPECTS:
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
			case MetadataPackage.CLASS_METADATA__DIAGNOSTICS:
				getDiagnostics().clear();
				getDiagnostics().addAll((Collection<? extends MetadataDiagnostic>)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__PACKAGE:
				setPackage((PackageMetadata)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__ECLASS:
				setEClass((EClass)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__NAME:
				setName((String)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__CLASSIFIER_ID:
				setClassifierID((Integer)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__TYPE_URI:
				setTypeURI((String)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__FEATURES:
				getFeatures().clear();
				getFeatures().addAll((Collection<? extends FeatureMetadata>)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__OPERATIONS:
				getOperations().clear();
				getOperations().addAll((Collection<? extends OperationMetadata>)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__SUPER_TYPES:
				getSuperTypes().clear();
				getSuperTypes().addAll((Collection<? extends ClassMetadata>)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__ALL_SUPER_TYPES:
				getAllSuperTypes().clear();
				getAllSuperTypes().addAll((Collection<? extends ClassMetadata>)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__ID_FEATURES:
				getIdFeatures().clear();
				getIdFeatures().addAll((Collection<? extends FeatureMetadata>)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__HAS_ID:
				setHasId((Boolean)newValue);
				return;
			case MetadataPackage.CLASS_METADATA__ASPECTS:
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
			case MetadataPackage.CLASS_METADATA__DIAGNOSTICS:
				getDiagnostics().clear();
				return;
			case MetadataPackage.CLASS_METADATA__PACKAGE:
				setPackage((PackageMetadata)null);
				return;
			case MetadataPackage.CLASS_METADATA__ECLASS:
				setEClass((EClass)null);
				return;
			case MetadataPackage.CLASS_METADATA__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MetadataPackage.CLASS_METADATA__CLASSIFIER_ID:
				setClassifierID(CLASSIFIER_ID_EDEFAULT);
				return;
			case MetadataPackage.CLASS_METADATA__TYPE_URI:
				setTypeURI(TYPE_URI_EDEFAULT);
				return;
			case MetadataPackage.CLASS_METADATA__FEATURES:
				getFeatures().clear();
				return;
			case MetadataPackage.CLASS_METADATA__OPERATIONS:
				getOperations().clear();
				return;
			case MetadataPackage.CLASS_METADATA__SUPER_TYPES:
				getSuperTypes().clear();
				return;
			case MetadataPackage.CLASS_METADATA__ALL_SUPER_TYPES:
				getAllSuperTypes().clear();
				return;
			case MetadataPackage.CLASS_METADATA__ID_FEATURES:
				getIdFeatures().clear();
				return;
			case MetadataPackage.CLASS_METADATA__HAS_ID:
				setHasId(HAS_ID_EDEFAULT);
				return;
			case MetadataPackage.CLASS_METADATA__ASPECTS:
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
			case MetadataPackage.CLASS_METADATA__DIAGNOSTICS:
				return diagnostics != null && !diagnostics.isEmpty();
			case MetadataPackage.CLASS_METADATA__ALL_DIAGNOSTICS:
				return !getAllDiagnostics().isEmpty();
			case MetadataPackage.CLASS_METADATA__PACKAGE:
				return getPackage() != null;
			case MetadataPackage.CLASS_METADATA__ECLASS:
				return eClass != null;
			case MetadataPackage.CLASS_METADATA__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MetadataPackage.CLASS_METADATA__CLASSIFIER_ID:
				return classifierID != CLASSIFIER_ID_EDEFAULT;
			case MetadataPackage.CLASS_METADATA__TYPE_URI:
				return TYPE_URI_EDEFAULT == null ? typeURI != null : !TYPE_URI_EDEFAULT.equals(typeURI);
			case MetadataPackage.CLASS_METADATA__FEATURES:
				return features != null && !features.isEmpty();
			case MetadataPackage.CLASS_METADATA__OPERATIONS:
				return operations != null && !operations.isEmpty();
			case MetadataPackage.CLASS_METADATA__SUPER_TYPES:
				return superTypes != null && !superTypes.isEmpty();
			case MetadataPackage.CLASS_METADATA__ALL_SUPER_TYPES:
				return allSuperTypes != null && !allSuperTypes.isEmpty();
			case MetadataPackage.CLASS_METADATA__ID_FEATURES:
				return idFeatures != null && !idFeatures.isEmpty();
			case MetadataPackage.CLASS_METADATA__HAS_ID:
				return hasId != HAS_ID_EDEFAULT;
			case MetadataPackage.CLASS_METADATA__ASPECTS:
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
		result.append(", classifierID: ");
		result.append(classifierID);
		result.append(", typeURI: ");
		result.append(typeURI);
		result.append(", hasId: ");
		result.append(hasId);
		result.append(')');
		return result.toString();
	}

} //ClassMetadataImpl
