/********************************************************************
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
 ********************************************************************/
package org.eclipse.fennec.emf.osgi.codegen.adapter;

import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.fennec.emf.osgi.codegen.FennecEmfGenerator;

/**
 * EMF codegen generator adapter factory that is responsible to create the Bnd adapter
 * @author Mark Hoffmann
 * @since 30.03.2018
 */
public class BNDGeneratorAdapterFactory extends GenModelGeneratorAdapterFactory {

	public static final GeneratorAdapterFactory.Descriptor DESCRIPTOR = ()->{
			FennecEmfGenerator.info("Creating BNDGeneratorAdapterFactory");
			return new BNDGeneratorAdapterFactory();
		};

		
		
	@Override
	public Adapter createGenPackageAdapter() {
		if (genPackageGeneratorAdapter == null)
		{
			FennecEmfGenerator.info("Creating FennecGenPackageGeneratorAdapter");
			genPackageGeneratorAdapter = new FennecGenPackageGeneratorAdapter(this);
		}
		return genPackageGeneratorAdapter;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory#createGenModelAdapter()
	 */
	@Override
	public Adapter createGenModelAdapter() {
		if (genModelGeneratorAdapter == null)
		{
			FennecEmfGenerator.info("Creating FennecGenModelGeneratorAdapter");
			genModelGeneratorAdapter = new FennecGenModelGeneratorAdapter(this);
		} 
		return genModelGeneratorAdapter;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory#createGenClassAdapter()
	 */
	@Override
	public Adapter createGenClassAdapter() {
		if (genClassGeneratorAdapter == null)
		{
			FennecEmfGenerator.info("Creating FennecGenClassGeneratorAdapter");
			genClassGeneratorAdapter = new FennecGenClassGeneratorAdapter(this);
		} 
		return genClassGeneratorAdapter;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory#createGenEnumAdapter()
	 */
	@Override
	public Adapter createGenEnumAdapter() {
		if (genEnumGeneratorAdapter == null)
		{
			FennecEmfGenerator.info("Creating FennecGenEnumGeneratorAdapter");
			genEnumGeneratorAdapter = new FennecGenEnumGeneratorAdapter(this);
		} 
		return genEnumGeneratorAdapter;
	}
}
