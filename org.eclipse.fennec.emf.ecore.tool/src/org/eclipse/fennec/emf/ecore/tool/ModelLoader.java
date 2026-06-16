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
package org.eclipse.fennec.emf.ecore.tool;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreValidator;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Sets up a standalone (non-OSGi, non-Eclipse) EMF environment and loads
 * Ecore/XMI resources from the file system.
 * <p>
 * Mirrors the registrations that EMF would normally pick up from plugin
 * descriptors / OSGi services, so that {@code .ecore} and {@code .xmi} files
 * can be parsed by a plain {@code java -jar} process.
 */
public final class ModelLoader {

	private final ResourceSet resourceSet;

	public ModelLoader() {
		this.resourceSet = new ResourceSetImpl();

		Map<String, Object> factories = resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap();
		factories.put("ecore", new EcoreResourceFactoryImpl());
		factories.put("xmi", new XMIResourceFactoryImpl());
		factories.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		// Ecore itself must be known to resolve the meta-model of loaded .ecore files.
		resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

		// Register the Ecore validator so the Diagnostician applies real Ecore constraints.
		EValidator.Registry.INSTANCE.put(EcorePackage.eINSTANCE, EcoreValidator.INSTANCE);
	}

	/**
	 * Loads the given file into a fresh {@link Resource}.
	 *
	 * @param input the file to load, must not be {@code null}
	 * @return the loaded resource (never {@code null}); inspect
	 *         {@link Resource#getErrors()} for parse problems
	 * @throws IOException if the file cannot be read or parsed
	 */
	public Resource load(File input) throws IOException {
		Objects.requireNonNull(input, "input file must not be null");
		if (!input.isFile()) {
			throw new IOException("Input file does not exist or is not a regular file: " + input.getAbsolutePath());
		}
		URI uri = URI.createFileURI(input.getAbsolutePath());
		Resource resource = resourceSet.createResource(uri);
		resource.load(Map.of());
		return resource;
	}

	/**
	 * Registers an already loaded {@link EPackage} so that cross references and
	 * instance models can be resolved against it.
	 *
	 * @param ePackage the package to register
	 */
	public void registerPackage(EPackage ePackage) {
		Objects.requireNonNull(ePackage, "ePackage must not be null");
		if (ePackage.getNsURI() != null) {
			resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
		}
	}

	public ResourceSet getResourceSet() {
		return resourceSet;
	}
}
