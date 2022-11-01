/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 *  
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *  
 * Contributors:
 *       Data In Motion - initial API and implementation
 */
package org.gecko.emf.osgi.example.model.manual;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.gecko.emf.osgi.example.model.manual.ManualPackage
 * @generated
 */
public interface ManualFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	ManualFactory eINSTANCE = org.gecko.emf.osgi.example.model.manual.impl.ManualFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Foo</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Foo</em>'.
	 * @generated
	 */
	Foo createFoo();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	ManualPackage getManualPackage();

} //ManualFactory
