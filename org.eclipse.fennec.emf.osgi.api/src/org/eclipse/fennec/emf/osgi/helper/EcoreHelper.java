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
package org.eclipse.fennec.emf.osgi.helper;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Utility class for loading EMF resources from multiple sources (classpath, URL,
 * file path, InputStream) with an optional shared {@link ResourceSet}.
 * <p>
 * Provides:
 * <ul>
 *   <li><strong>Generic loading</strong> ({@code load}) — loads any root {@link EObject}
 *       from an XMI/Ecore resource, type-safe via {@code Class<T>}</li>
 *   <li><strong>EPackage-specific loading</strong> ({@code loadEcore}) — additionally sets
 *       the Resource URI to the EPackage's {@code nsURI}</li>
 *   <li><strong>Detached loading</strong> ({@code loadEcoreDetached}) — removes the EPackage
 *       from its Resource after loading</li>
 *   <li>{@link #detach(EObject)} — public method to detach any EObject from its Resource
 *       and remove the Resource from the ResourceSet</li>
 * </ul>
 *
 * @author Mark Hoffmann
 * @since 20.02.2026
 */
public class EcoreHelper {

	private final ResourceSet resourceSet;

	/**
	 * Creates a new EcoreHelper with a default {@link ResourceSet}.
	 */
	public EcoreHelper() {
		this(createResourceSet());
	}

	/**
	 * Creates a new EcoreHelper with the provided {@link ResourceSet}.
	 *
	 * @param resourceSet the resource set to use, must not be {@code null}
	 */
	public EcoreHelper(ResourceSet resourceSet) {
		this.resourceSet = requireNonNull(resourceSet, "ResourceSet must not be null");
	}

	/**
	 * Creates a {@link ResourceSet} pre-configured for loading {@code .ecore} files.
	 * <p>
	 * Registers {@link EcorePackage} and sets up resource factories for
	 * {@code ecore}, {@code xmi}, and the default ({@code *}) extension.
	 *
	 * @return a new, configured ResourceSet
	 */
	public static ResourceSet createResourceSet() {
		ResourceSet rs = new ResourceSetImpl();
		rs.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		Map<String, Object> factoryMap = rs.getResourceFactoryRegistry().getExtensionToFactoryMap();
		factoryMap.put(EcorePackage.eNAME, new EcoreResourceFactoryImpl());
		factoryMap.put("xmi", new XMIResourceFactoryImpl());
		factoryMap.put("*", new XMIResourceFactoryImpl());
		return rs;
	}

	// === Generic loading (any EObject type) ===

	/**
	 * Loads the root {@link EObject} from an {@link InputStream} and casts it to the given type.
	 * <p>
	 * The loaded object remains attached to its Resource. Callers can access
	 * {@code eObject.eResource().getErrors()} / {@code .getWarnings()} for diagnostics.
	 *
	 * @param <T>  the expected root object type
	 * @param is   the input stream to read from, must not be {@code null}
	 * @param uri  the URI to associate with the resource
	 * @param type the expected root object class
	 * @return the loaded object
	 * @throws IOException if the resource cannot be read, is empty, or the root object
	 *                     is not an instance of the expected type
	 */
	public <T extends EObject> T load(InputStream is, URI uri, Class<T> type) throws IOException {
		requireNonNull(is, "InputStream must not be null");
		requireNonNull(uri, "URI must not be null");
		requireNonNull(type, "Type must not be null");
		Resource resource = resourceSet.createResource(uri);
		resource.load(is, null);
		return extractRoot(resource, type);
	}

	/**
	 * Loads the root {@link EObject} from a {@link URL} and casts it to the given type.
	 *
	 * @param <T>  the expected root object type
	 * @param url  the URL to load from
	 * @param type the expected root object class
	 * @return the loaded object (attached)
	 * @throws IOException if the resource cannot be read, is empty, or type mismatch
	 */
	public <T extends EObject> T load(URL url, Class<T> type) throws IOException {
		requireNonNull(url, "URL must not be null");
		try (InputStream is = url.openStream()) {
			return load(is, URI.createURI(url.toExternalForm()), type);
		}
	}

	/**
	 * Loads the root {@link EObject} from a file {@link Path} and casts it to the given type.
	 *
	 * @param <T>      the expected root object type
	 * @param filePath the path to the file
	 * @param type     the expected root object class
	 * @return the loaded object (attached)
	 * @throws IOException if the resource cannot be read, is empty, or type mismatch
	 */
	public <T extends EObject> T load(Path filePath, Class<T> type) throws IOException {
		requireNonNull(filePath, "Path must not be null");
		return load(filePath.toUri().toURL(), type);
	}

	/**
	 * Loads the root {@link EObject} from the classpath and casts it to the given type.
	 *
	 * @param <T>          the expected root object type
	 * @param resourceName the resource name (e.g. {@code "model.xmi"})
	 * @param contextClass the class whose classloader/package is used to resolve the resource
	 * @param type         the expected root object class
	 * @return the loaded object (attached)
	 * @throws IOException if the resource cannot be found, is empty, or type mismatch
	 */
	public <T extends EObject> T load(String resourceName, Class<?> contextClass, Class<T> type) throws IOException {
		requireNonNull(resourceName, "Resource name must not be null");
		requireNonNull(contextClass, "Context class must not be null");
		URL url = contextClass.getResource(resourceName);
		if (url == null) {
			throw new IOException("Resource not found: " + resourceName + " relative to " + contextClass.getName());
		}
		return load(url, type);
	}

	// === EPackage-specific loading (attached, URI set to nsURI) ===

	/**
	 * Loads an {@code .ecore} model from an {@link InputStream}.
	 * <p>
	 * The loaded EPackage remains attached to its Resource. The Resource URI is
	 * set to the EPackage's {@code nsURI} so that EMF can resolve internal references.
	 * Callers can access {@code ePackage.eResource().getErrors()} / {@code .getWarnings()}
	 * for diagnostics.
	 *
	 * @param is  the input stream to read from, must not be {@code null}
	 * @param uri the URI to associate with the resource
	 * @return the loaded EPackage
	 * @throws IOException if the ecore file cannot be read or is empty
	 */
	public EPackage loadEcore(InputStream is, URI uri) throws IOException {
		EPackage ePackage = load(is, uri, EPackage.class);
		ePackage.eResource().setURI(URI.createURI(ePackage.getNsURI()));
		return ePackage;
	}

	/**
	 * Loads an {@code .ecore} model from a {@link URL}.
	 *
	 * @param url the URL of the ecore file
	 * @return the loaded EPackage (attached)
	 * @throws IOException if the ecore file cannot be read or is empty
	 */
	public EPackage loadEcore(URL url) throws IOException {
		requireNonNull(url, "URL must not be null");
		try (InputStream is = url.openStream()) {
			return loadEcore(is, URI.createURI(url.toExternalForm()));
		}
	}

	/**
	 * Loads an {@code .ecore} model from a file {@link Path}.
	 *
	 * @param filePath the path to the ecore file
	 * @return the loaded EPackage (attached)
	 * @throws IOException if the ecore file cannot be read or is empty
	 */
	public EPackage loadEcore(Path filePath) throws IOException {
		requireNonNull(filePath, "Path must not be null");
		return loadEcore(filePath.toUri().toURL());
	}

	/**
	 * Loads an {@code .ecore} model from the classpath relative to the given context class.
	 *
	 * @param resourceName the resource name (e.g. {@code "manual.ecore"})
	 * @param contextClass the class whose classloader/package is used to resolve the resource
	 * @return the loaded EPackage (attached)
	 * @throws IOException if the resource cannot be found or is empty
	 */
	public EPackage loadEcore(String resourceName, Class<?> contextClass) throws IOException {
		requireNonNull(resourceName, "Resource name must not be null");
		requireNonNull(contextClass, "Context class must not be null");
		URL url = contextClass.getResource(resourceName);
		if (url == null) {
			throw new IOException("Resource not found: " + resourceName + " relative to " + contextClass.getName());
		}
		return loadEcore(url);
	}

	// === Detached loading (EPackage removed from Resource) ===

	/**
	 * Loads an {@code .ecore} model from an {@link InputStream} and detaches it.
	 * <p>
	 * The EPackage is removed from its Resource after loading. This is lightweight
	 * and suitable for tests or standalone usage where resource tracking is not needed.
	 *
	 * @param is  the input stream to read from
	 * @param uri the URI to associate with the resource
	 * @return the loaded EPackage (detached, {@code eResource()} will be {@code null})
	 * @throws IOException if the ecore file cannot be read or is empty
	 */
	public EPackage loadEcoreDetached(InputStream is, URI uri) throws IOException {
		EPackage ePackage = loadEcore(is, uri);
		detach(ePackage);
		return ePackage;
	}

	/**
	 * Loads an {@code .ecore} model from a {@link URL} and detaches it.
	 *
	 * @param url the URL of the ecore file
	 * @return the loaded EPackage (detached)
	 * @throws IOException if the ecore file cannot be read or is empty
	 */
	public EPackage loadEcoreDetached(URL url) throws IOException {
		EPackage ePackage = loadEcore(url);
		detach(ePackage);
		return ePackage;
	}

	/**
	 * Loads an {@code .ecore} model from a file {@link Path} and detaches it.
	 *
	 * @param filePath the path to the ecore file
	 * @return the loaded EPackage (detached)
	 * @throws IOException if the ecore file cannot be read or is empty
	 */
	public EPackage loadEcoreDetached(Path filePath) throws IOException {
		EPackage ePackage = loadEcore(filePath);
		detach(ePackage);
		return ePackage;
	}

	/**
	 * Loads an {@code .ecore} model from the classpath and detaches it.
	 *
	 * @param resourceName the resource name
	 * @param contextClass the class whose classloader/package is used to resolve the resource
	 * @return the loaded EPackage (detached)
	 * @throws IOException if the resource cannot be found or is empty
	 */
	public EPackage loadEcoreDetached(String resourceName, Class<?> contextClass) throws IOException {
		EPackage ePackage = loadEcore(resourceName, contextClass);
		detach(ePackage);
		return ePackage;
	}

	// === Static one-shot loading ===

	/**
	 * Loads an {@code .ecore} model from a {@link URL} using the provided {@link ResourceSet}.
	 * <p>
	 * This is a convenience method for one-shot loading without creating an EcoreHelper instance.
	 *
	 * @param url         the URL of the ecore file
	 * @param resourceSet the resource set to use
	 * @return the loaded EPackage (attached)
	 * @throws IOException if the ecore file cannot be read or is empty
	 */
	public static EPackage loadEcore(URL url, ResourceSet resourceSet) throws IOException {
		return new EcoreHelper(resourceSet).loadEcore(url);
	}

	// === Convenience accessors ===

	/**
	 * Returns the {@link EClass} with the given name from the EPackage.
	 *
	 * @param ePackage  the package to search
	 * @param className the classifier name
	 * @return the EClass
	 * @throws IllegalArgumentException if no EClass with that name exists
	 */
	public static EClass getEClass(EPackage ePackage, String className) {
		requireNonNull(ePackage, "EPackage must not be null");
		requireNonNull(className, "Class name must not be null");
		EClassifier classifier = ePackage.getEClassifier(className);
		if (classifier instanceof EClass eClass) {
			return eClass;
		}
		throw new IllegalArgumentException(
				"No EClass '" + className + "' found in package " + ePackage.getNsURI());
	}

	/**
	 * Returns the {@link EStructuralFeature} with the given name from the EClass.
	 *
	 * @param eClass      the class to search
	 * @param featureName the feature name
	 * @return the structural feature
	 * @throws IllegalArgumentException if no feature with that name exists
	 */
	public static EStructuralFeature getFeature(EClass eClass, String featureName) {
		requireNonNull(eClass, "EClass must not be null");
		requireNonNull(featureName, "Feature name must not be null");
		EStructuralFeature feature = eClass.getEStructuralFeature(featureName);
		if (feature == null) {
			throw new IllegalArgumentException(
					"No feature '" + featureName + "' found in class " + eClass.getName());
		}
		return feature;
	}

	// === Property extraction ===

	/**
	 * Extracts inline properties from a path string.
	 * <p>
	 * Expects the first segment to be the path itself, followed by semicolon-separated
	 * key=value pairs. For example:
	 * <pre>
	 *   /model;foo=bar;test=me
	 * </pre>
	 * returns {@code "/model"} and puts {@code foo=bar, test=me} into the properties map.
	 *
	 * @param path       the path string, possibly with inline properties
	 * @param properties map to populate with extracted key-value pairs
	 * @return the plain path without properties, or the original path if no properties found
	 */
	public static String extractProperties(String path, Map<String, String> properties) {
		if (isNull(path) || path.isEmpty()) {
			return path;
		}
		String[] parts = path.split(";");
		if (parts.length > 1) {
			for (int i = 1; i < parts.length; i++) {
				String[] kvArray = parts[i].split("=");
				if (kvArray.length > 0 && properties != null) {
					String value = kvArray.length > 1 ? kvArray[1] : null;
					properties.put(kvArray[0], value);
				}
			}
			return parts[0];
		} else {
			return parts.length == 1 ? parts[0] : path;
		}
	}

	// === ResourceSet access ===

	/**
	 * Returns the {@link ResourceSet} used by this helper.
	 *
	 * @return the resource set
	 */
	public ResourceSet getResourceSet() {
		return resourceSet;
	}

	// === Cleanup ===

	/**
	 * Unloads all resources and clears the resource set.
	 */
	public void releaseAll() {
		for (Resource resource : resourceSet.getResources()) {
			resource.unload();
		}
		resourceSet.getResources().clear();
	}

	// === Detach ===

	/**
	 * Detaches an {@link EObject} from its {@link Resource} and removes the Resource
	 * from the {@link ResourceSet}.
	 * <p>
	 * After this call, {@code eObject.eResource()} will return {@code null}.
	 * This is useful for lightweight standalone usage where resource tracking is not needed.
	 *
	 * @param eObject the object to detach, must not be {@code null}
	 */
	public void detach(EObject eObject) {
		requireNonNull(eObject, "EObject must not be null");
		Resource resource = eObject.eResource();
		if (resource != null) {
			resource.getContents().remove(eObject);
			resourceSet.getResources().remove(resource);
		}
	}

	// === Internal helpers ===

	private static <T extends EObject> T extractRoot(Resource resource, Class<T> type) throws IOException {
		if (resource.getContents().isEmpty()) {
			throw new IOException("Resource is empty: " + resource.getURI());
		}
		EObject root = resource.getContents().get(0);
		if (!type.isInstance(root)) {
			throw new IOException("Expected root object of type " + type.getName()
					+ " but found " + root.getClass().getName() + " in " + resource.getURI());
		}
		return type.cast(root);
	}
}
