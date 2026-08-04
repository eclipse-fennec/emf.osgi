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
package org.eclipse.fennec.emf.osgi.eobject.registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * The default initial provider: loads EObjects from local resource files, so a
 * connected system holds its content without any remote source. Plain Java - usable via
 * the non-OSGi bootstrap with a caller-configured {@link ResourceSet}; the OSGi layer
 * wraps it in a factory component that takes the {@code ResourceSet} from the
 * {@code ResourceSetFactory} service.
 * <p>
 * Locations are files or directories (directories are walked recursively); every root
 * object of every loadable resource becomes one entry. A broken file is logged and
 * skipped - it never fails the whole load. An empty location set is a valid, empty
 * initial state. Each load pass writes through
 * {@link EObjectRegistryWriter#sync(String, java.util.Collection)}, so a re-load swaps
 * cleanly against the previous state.
 * <p>
 * Entry keys come from a {@link #uriFragmentKeys() key function}; entry properties
 * carry the model anchoring ({@code emf.nsURI} of the object's package) and the source
 * file ({@code file.location}).
 *
 * @author Data In Motion Consulting
 */
public class FileEObjectProvider implements EObjectProvider {

	/** Entry property holding the absolute path of the file an entry was loaded from. */
	public static final String PROP_FILE_LOCATION = "file.location";
	/** Entry property holding the nsURI of the entry object's EPackage. */
	public static final String PROP_NS_URI = "emf.nsURI";

	private static final Logger logger = Logger.getLogger(FileEObjectProvider.class.getName());

	private final String providerName;
	private final ResourceSet resourceSet;
	private final List<Path> locations;
	private final BiFunction<Resource, EObject, String> keyFunction;

	/**
	 * Creates a provider.
	 *
	 * @param providerName the provider's name - the entries' source tag; must not be
	 *                     {@code null}
	 * @param resourceSet  the resource set to load with; must not be {@code null}
	 * @param locations    files or directories to load; must not be {@code null}
	 * @param keyFunction  derives the entry key from (resource, root object), see
	 *                     {@link #uriFragmentKeys()} and {@link #featureKeys(String)};
	 *                     returning {@code null} skips the object; must not be
	 *                     {@code null}
	 */
	public FileEObjectProvider(String providerName, ResourceSet resourceSet, List<Path> locations,
			BiFunction<Resource, EObject, String> keyFunction) {
		this.providerName = Objects.requireNonNull(providerName, "providerName");
		this.resourceSet = Objects.requireNonNull(resourceSet, "resourceSet");
		this.locations = List.copyOf(Objects.requireNonNull(locations, "locations"));
		this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
	}

	/**
	 * The default key function: {@code <fileName>#<uriFragment>} - stable as long as
	 * the file keeps its name and structure.
	 *
	 * @return the key function
	 */
	public static BiFunction<Resource, EObject, String> uriFragmentKeys() {
		return (resource, object) -> resource.getURI().lastSegment() + "#" + resource.getURIFragment(object);
	}

	/**
	 * A feature-derived key function: reads the named attribute of the root object
	 * (e.g. an id attribute). Objects without the feature or with a null value are
	 * skipped (logged by the provider).
	 *
	 * @param featureName the attribute name; must not be {@code null}
	 * @return the key function
	 */
	public static BiFunction<Resource, EObject, String> featureKeys(String featureName) {
		Objects.requireNonNull(featureName, "featureName");
		return (resource, object) -> {
			EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
			if (feature == null) {
				return null;
			}
			Object value = object.eGet(feature);
			return value == null ? null : value.toString();
		};
	}

	@Override
	public CompletableFuture<Void> load(EObjectRegistryWriter writer) {
		Objects.requireNonNull(writer, "writer");
		List<EObjectRegistryEntry> state = new ArrayList<>();
		for (Path location : locations) {
			if (!Files.exists(location)) {
				logger.warning(() -> String.format("Provider %s: location %s does not exist - skipping", providerName,
						location));
				continue;
			}
			for (Path file : filesOf(location)) {
				loadFile(file, state);
			}
		}
		writer.sync(providerName, state);
		return CompletableFuture.completedFuture(null);
	}

	private List<Path> filesOf(Path location) {
		if (Files.isRegularFile(location)) {
			return List.of(location);
		}
		try (Stream<Path> walk = Files.walk(location)) {
			return walk.filter(Files::isRegularFile).sorted().toList();
		} catch (IOException e) {
			logger.log(Level.WARNING,
					String.format("Provider %s: cannot walk directory %s - skipping", providerName, location), e);
			return List.of();
		}
	}

	private void loadFile(Path file, List<EObjectRegistryEntry> state) {
		URI uri = URI.createFileURI(file.toAbsolutePath().toString());
		Resource resource;
		try {
			resource = resourceSet.getResource(uri, true);
		} catch (RuntimeException e) {
			logger.log(Level.WARNING,
					String.format("Provider %s: cannot load %s - skipping this file", providerName, file), e);
			return;
		}
		for (EObject root : List.copyOf(resource.getContents())) {
			String key = keyFunction.apply(resource, root);
			if (key == null || key.isBlank()) {
				logger.warning(() -> String.format("Provider %s: no key derivable for a %s in %s - skipping it",
						providerName, root.eClass().getName(), file));
				continue;
			}
			state.add(new EObjectRegistryEntry(key, root, providerName, entryProperties(file, root)));
		}
	}

	private Map<String, Object> entryProperties(Path file, EObject root) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(PROP_FILE_LOCATION, file.toAbsolutePath().toString());
		EPackage ePackage = root.eClass().getEPackage();
		if (ePackage != null && ePackage.getNsURI() != null) {
			properties.put(PROP_NS_URI, ePackage.getNsURI());
		}
		return properties;
	}
}
