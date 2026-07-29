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

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Aspect Entry</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A cross-cutting concern attached to a metadata element - the composition replacement for the donor model's typed aspect hierarchy (issue #60).
 * 
 * One concrete class instead of Aspect/PackageAspect/ClassAspect/FeatureAspect/OperationAspect: providers no longer subclass the metadata model to attach their content, they put their own EObject into the content slot. This is what makes the metadata model independent of its consumers - a provider can ship its content model in its own bundle without this model knowing the type.
 * 
 * The owner is the containing metadata element, reachable via eContainer(). There is no typed back-reference, because one entry type is contained at four different levels; an eOpposite would have to name a single container type.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getTypeId <em>Type Id</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getContent <em>Content</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getTransientContent <em>Transient Content</em>}</li>
 *   <li>{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getDiagnostics <em>Diagnostics</em>}</li>
 * </ul>
 *
 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAspectEntry()
 * @model
 * @generated
 */
@ProviderType
public interface AspectEntry extends EObject {
	/**
	 * Returns the value of the '<em><b>Type Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Unique identifier for the aspect type (e.g. 'codec', 'orm', 'history'), taken from the providing component. Used to look up entries by type; the same typeId appears at most once per metadata element.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Type Id</em>' attribute.
	 * @see #setTypeId(String)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAspectEntry_TypeId()
	 * @model
	 * @generated
	 */
	String getTypeId();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getTypeId <em>Type Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type Id</em>' attribute.
	 * @see #getTypeId()
	 * @generated
	 */
	void setTypeId(String value);

	/**
	 * Returns the value of the '<em><b>Content</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * The provider's content, as a contained EObject. Serializable by construction: an entry that carries its content here survives writing the metadata tree to an index and reading it back, provided the content's EPackage is available at read time. This is the slot for anything that is part of the metadata (e.g. OCL expressions as EObjects, resolved configuration).
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Content</em>' containment reference.
	 * @see #setContent(EObject)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAspectEntry_Content()
	 * @model containment="true"
	 * @generated
	 */
	EObject getContent();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getContent <em>Content</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Content</em>' containment reference.
	 * @see #getContent()
	 * @generated
	 */
	void setContent(EObject value);

	/**
	 * Returns the value of the '<em><b>Transient Content</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Runtime-only content that cannot or should not be serialized - compiled expressions, caches, handles to live services. Never written to an index and never replicated; a consumer reading a persisted metadata tree must be able to work without it, or rebuild it.
	 * 
	 * Both slots exist on purpose: forcing serializable content into a transient slot loses it across a restart, forcing runtime state into the content slot breaks serialization.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Transient Content</em>' attribute.
	 * @see #setTransientContent(Object)
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAspectEntry_TransientContent()
	 * @model transient="true"
	 * @generated
	 */
	Object getTransientContent();

	/**
	 * Sets the value of the '{@link org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry#getTransientContent <em>Transient Content</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Transient Content</em>' attribute.
	 * @see #getTransientContent()
	 * @generated
	 */
	void setTransientContent(Object value);

	/**
	 * Returns the value of the '<em><b>Diagnostics</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Diagnostics collected while this entry was built (e.g. annotation parsing warnings, invalid key combinations). Managed separately from the owning element's diagnostics and not part of its allDiagnostics.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Diagnostics</em>' containment reference list.
	 * @see org.eclipse.fennec.emf.osgi.model.metadata.MetadataPackage#getAspectEntry_Diagnostics()
	 * @model containment="true"
	 * @generated
	 */
	EList<MetadataDiagnostic> getDiagnostics();

} // AspectEntry
