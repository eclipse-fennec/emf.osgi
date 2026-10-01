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
package org.eclipse.fennec.emf.osgi.components;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

/**
 * A resource locator that resolves the location of a registered generated package to the resource of
 * the package itself.
 * <p>
 * {@link ResourceSetImpl} only asks its package registry with the URI exactly as requested. A
 * reference to a generated model by its location, e.g.
 * {@code platform:/plugin/org.example.model/model/example.ecore#//Foo}, therefore stays a proxy or
 * loads the model file as a second, dynamic copy of the package. This locator resolves such a
 * location to the generated package, if
 * <ul>
 * <li>a bundle declares the location for the package in its {@code generated_package} capability, or</li>
 * <li>the URI converter of the resource set normalizes the location to the namespace URI of a
 * registered package, e.g. through a configured {@code UriMapProvider}.</li>
 * </ul>
 * A resource that is already contained in the resource set takes precedence.
 */
final class GeneratedPackageResourceLocator extends ResourceSetImpl.ResourceLocator {

	private final Function<URI, Optional<String>> nsUriByLocation;

	/**
	 * Creates the locator and installs it in the given resource set.
	 * @param resourceSet the resource set, must not be <code>null</code>
	 * @param nsUriByLocation returns the namespace URI of the generated package declared at a location
	 */
	GeneratedPackageResourceLocator(ResourceSetImpl resourceSet, Function<URI, Optional<String>> nsUriByLocation) {
		super(requireNonNull(resourceSet));
		this.nsUriByLocation = requireNonNull(nsUriByLocation);
	}

	@Override
	public Resource getResource(URI uri, boolean loadOnDemand) {
		Resource resource = basicGetResource(uri, false);
		if (resource != null) {
			if (loadOnDemand && !resource.isLoaded()) {
				demandLoadHelper(resource);
			}
			return resource;
		}
		Optional<Resource> packageResource = getGeneratedPackageResource(uri);
		if (packageResource.isPresent()) {
			Map<URI, Resource> map = resourceSet.getURIResourceMap();
			if (map != null) {
				map.put(uri, packageResource.get());
			}
			return packageResource.get();
		}
		return basicGetResource(uri, loadOnDemand);
	}

	/**
	 * Resolves a location to the resource of a registered package.
	 * @param uri the requested location
	 * @return the resource of the package, or an empty {@link Optional}
	 */
	private Optional<Resource> getGeneratedPackageResource(URI uri) {
		URI normalized = resourceSet.getURIConverter().normalize(uri);
		Optional<String> nsUri = nsUriByLocation.apply(uri).or(() -> nsUriByLocation.apply(normalized));
		if (nsUri.isEmpty() && !normalized.equals(uri)) {
			nsUri = Optional.of(normalized.toString());
		}
		return nsUri.map(resourceSet.getPackageRegistry()::getEPackage).map(EPackage::eResource);
	}
}
