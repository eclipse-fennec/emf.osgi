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
package org.eclipse.fennec.emf.osgi.extender;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.fennec.emf.osgi.constants.EMFNamespaces.EMF_MODEL_EXTENDER_DEFAULT_PATH;
import static org.eclipse.fennec.emf.osgi.constants.EMFNamespaces.EMF_MODEL_EXTENDER_PROP_MODELS_NAME;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.fennec.emf.osgi.constants.EMFNamespaces;
import org.eclipse.fennec.emf.osgi.extender.model.Model;
import org.eclipse.fennec.emf.osgi.helper.EcoreHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;

/**
 * Utility class for discovering and loading EMF ecore models from OSGi bundles.
 * <p>
 * Provides methods to:
 * <ul>
 *   <li>Check if a bundle declares an EMF model extender requirement via its wiring</li>
 *   <li>Extract model paths from the extender requirement attributes</li>
 *   <li>Load {@code .ecore} files from bundle entries and create {@link Model} instances</li>
 *   <li>Parse inline properties from path strings (semicolon-separated key=value pairs)</li>
 * </ul>
 *
 * @author Mark Hoffmann
 * @since 17.10.2022
 */
public class ModelHelper {

	private static final Logger logger = Logger.getLogger(ModelHelper.class.getName());

	/**
	 * Collects warnings and errors encountered during model loading.
	 */
	public static final class Diagnostic {
		public final List<String> warnings = new ArrayList<>();
		public final List<String> errors = new ArrayList<>();
	}

	private ModelHelper() {
	}

	/**
	 * Reads all EMF models from a bundle by scanning the given paths for {@code .ecore} files.
	 *
	 * @param bundle      the bundle to scan
	 * @param resourceSet the {@link ResourceSet} used to load ecore resources
	 * @param paths       the set of bundle-relative paths to scan (may include inline properties)
	 * @param diagnostic  collects errors and warnings during loading
	 * @return list of successfully loaded models, never {@code null}
	 */
	public static List<Model> readModelsFromBundle(final Bundle bundle,
			final ResourceSet resourceSet,
			final Set<String> paths,
			final Diagnostic diagnostic) {
		if (paths == null) {
			return Collections.emptyList();
		}
		return paths.stream()
				.map(path -> readModel(bundle, resourceSet, path, diagnostic))
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	/**
	 * Reads all {@code .ecore} model files from a single path in the bundle.
	 * <p>
	 * The path may point to a specific {@code .ecore} file or a directory.
	 * If a directory, all {@code .ecore} files in that directory (non-recursive) are loaded.
	 * The path may contain inline properties in the format {@code path;key=value;key2=value2}.
	 *
	 * @param bundle      the bundle to load models from
	 * @param resourceSet the {@link ResourceSet} used to load ecore resources
	 * @param path        the model path in the bundle, optionally with inline properties
	 * @param diagnostic  collects errors and warnings during loading
	 * @return list of loaded models from this path
	 */
	public static List<Model> readModel(final Bundle bundle,
			final ResourceSet resourceSet,
			final String path,
			final Diagnostic diagnostic) {
		final List<Model> models = new ArrayList<>();
		Map<String, String> properties = new HashMap<>();
		String plainPath = extractProperties(path, properties);
		final Enumeration<URL> urls;
		if (plainPath != null && plainPath.endsWith(".ecore")) {
			URL url = bundle.getEntry(plainPath);
			if (url == null) {
				urls = bundle.findEntries(plainPath, "*.ecore", false);
			} else {
				urls = Collections.enumeration(Collections.singleton(url));
			}
		} else {
			urls = bundle.findEntries(plainPath, "*.ecore", false);
		}
		loadModelsFromUrls(bundle.getBundleId(), resourceSet, path, diagnostic, models, properties, plainPath, urls);
		return models;
	}

	/**
	 * Iterates over the given URLs, loading each as an ecore model instance.
	 */
	private static void loadModelsFromUrls(final long bundleId, final ResourceSet resourceSet, final String path,
			final Diagnostic diagnostic, final List<Model> models, Map<String, String> properties, String plainPath,
			final Enumeration<URL> urls) {
		if (nonNull(urls)) {
			while (urls.hasMoreElements()) {
				final URL url = urls.nextElement();
				try {
					final Model model = loadModelInstance(bundleId, resourceSet, url, properties, diagnostic);
					if (nonNull(model)) {
						models.add(model);
					}
				} catch (final IOException ioe) {
					diagnostic.errors.add("Unable to load ecore " + plainPath + " : " + ioe.getMessage());
				}
			}
		} else {
			diagnostic.errors.add("No ecore models found at path " + path);
		}
	}

	/**
	 * Extracts inline properties from a path string.
	 * <p>
	 * Delegates to {@link EcoreHelper#extractProperties(String, Map)}.
	 *
	 * @param path       the path string, possibly with inline properties
	 * @param properties map to populate with extracted key-value pairs
	 * @return the plain path without properties, or the original path if no properties found
	 * @see EcoreHelper#extractProperties(String, Map)
	 */
	public static String extractProperties(String path, Map<String, String> properties) {
		return EcoreHelper.extractProperties(path, properties);
	}

	/**
	 * Loads a single ecore file from a URL and creates a {@link Model} instance.
	 * <p>
	 * The loaded {@link EPackage} is enriched with standard service properties
	 * ({@code emf.name}, {@code emf.nsURI}, {@code emf.registration}, {@code emf.model.scope}).
	 *
	 * @param bundleId    the bundle ID that provides this model
	 * @param resourceSet the {@link ResourceSet} used to load the ecore resource
	 * @param url         the URL of the {@code .ecore} file
	 * @param properties  additional properties extracted from the path, may be {@code null}
	 * @param diagnostic  collects errors and warnings during loading
	 * @return the loaded model, never {@code null}
	 * @throws IOException if the ecore file cannot be read or is empty
	 */
	public static Model loadModelInstance(final long bundleId,
			final ResourceSet resourceSet,
			final URL url,
			final Map<String, String> properties,
			final Diagnostic diagnostic) throws IOException {
		EPackage ePackage = EcoreHelper.loadEcore(url, resourceSet);
		Resource r = ePackage.eResource();
		try {
			Dictionary<String, Object> serviceProperties = new Hashtable<>();
			if (properties != null) {
				properties.forEach(serviceProperties::put);
			}
			serviceProperties.put(EMFNamespaces.EMF_NAME, ePackage.getName());
			serviceProperties.put(EMFNamespaces.EMF_MODEL_NSURI, ePackage.getNsURI());
			serviceProperties.put(EMFNamespaces.EMF_MODEL_REGISTRATION, EMFNamespaces.MODEL_REGISTRATION_EXTENDER);
			if (properties == null || !properties.containsKey(EMFNamespaces.EMF_MODEL_SCOPE)) {
				serviceProperties.put(EMFNamespaces.EMF_MODEL_SCOPE, EMFNamespaces.EMF_MODEL_SCOPE_STATIC);
			}
			return new Model(ePackage, serviceProperties, bundleId);
		} finally {
			if (r != null) {
				for (Resource.Diagnostic de : r.getErrors()) {
					diagnostic.errors.add(de.getMessage() + " [" + de.getLine() + ":" + de.getColumn() + "]");
				}
				for (Resource.Diagnostic dw : r.getWarnings()) {
					diagnostic.warnings.add(dw.getMessage() + " [" + dw.getLine() + ":" + dw.getColumn() + "]");
				}
			}
		}
	}

	/**
	 * Checks if a bundle has an EMF model extender requirement wired to the given extender bundle.
	 * <p>
	 * Examines the bundle's {@link BundleWiring} for requirements in the
	 * {@code osgi.extender} namespace that are wired to the extender bundle
	 * identified by {@code extenderBundleId}.
	 *
	 * @param bundle          the bundle to check
	 * @param extenderBundleId the bundle ID of this extender bundle
	 * @return set of model paths from the requirement attributes, or empty set if not a model bundle
	 */
	public static Set<String> isModelBundle(final Bundle bundle, final long extenderBundleId) {
		final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		if (isNull(bundleWiring)) {
			return Collections.emptySet();
		}

		final List<BundleRequirement> requirements = bundleWiring.getRequirements(ExtenderNamespace.EXTENDER_NAMESPACE);
		if (isNull(requirements) || requirements.isEmpty()) {
			return Collections.emptySet();
		}

		final List<BundleWire> wires = bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE);
		for (final BundleWire wire : wires) {
			if (nonNull(wire.getProviderWiring())
					&& wire.getProviderWiring().getBundle().getBundleId() == extenderBundleId) {
				return extractModelPath(wire);
			}
		}
		return Collections.emptySet();
	}

	/**
	 * Extracts model path(s) from a bundle wire's requirement attributes.
	 * <p>
	 * The {@code models} attribute may be a single {@link String} or a {@link List} of strings.
	 * Falls back to the default model path ({@code "model/"}) if no attribute is specified.
	 *
	 * @param wire the bundle wire containing the requirement attributes
	 * @return set of model paths
	 */
	@SuppressWarnings("unchecked")
	private static Set<String> extractModelPath(final BundleWire wire) {
		requireNonNull(wire);
		final Object val = wire.getRequirement().getAttributes().get(EMF_MODEL_EXTENDER_PROP_MODELS_NAME);
		if (nonNull(val)) {
			if (val instanceof String s) {
				return Collections.singleton(s);
			}
			if (val instanceof List<?> list) {
				return new HashSet<>((List<String>) list);
			}
			logger.severe(() -> "Attribute " + EMF_MODEL_EXTENDER_PROP_MODELS_NAME
					+ " for EMF models requirement has an invalid type: " + val
					+ ". Using default model path.");
		}
		return Collections.singleton(EMF_MODEL_EXTENDER_DEFAULT_PATH);
	}
}
