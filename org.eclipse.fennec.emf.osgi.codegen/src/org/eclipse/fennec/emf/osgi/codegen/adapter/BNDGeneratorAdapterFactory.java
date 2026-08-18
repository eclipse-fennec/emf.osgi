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

import java.util.Map;

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

	/**
	 * Returns the line delimiter configured via the <code>lineEndings</code> generate attribute,
	 * or <code>null</code> if the EMF default (delimiter of the existing target file, then the
	 * system line separator) should be used.
	 * @param adapterFactory the adapter factory of the calling generator adapter
	 * @return the configured line delimiter or <code>null</code>
	 */
	public static String getConfiguredLineDelimiter(GeneratorAdapterFactory adapterFactory) {
		Object[] data = adapterFactory.getGenerator().getOptions().data;
		if (data != null && data.length > 0 && data[0] instanceof Map<?, ?> props
				&& props.get(FennecEmfGenerator.LINE_DELIMITER) instanceof String lineDelimiter) {
			return lineDelimiter;
		}
		return null;
	}


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
