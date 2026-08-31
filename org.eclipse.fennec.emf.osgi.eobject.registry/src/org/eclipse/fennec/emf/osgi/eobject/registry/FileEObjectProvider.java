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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * initial state.
 * <p>
 * A directory walk only picks up files whose extension is in the allow-list -
 * {@link #DEFAULT_FILE_EXTENSIONS} unless one is given - and never picks up dotfiles, so a
 * {@code .keep} placeholder or a {@code README.md} beside the models is passed over quietly
 * instead of being parsed and reported as broken. A warning therefore means a real model file
 * failed to load. A location that names a file directly bypasses the allow-list. Each load pass
 * writes through
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
	/**
	 * The file extensions a directory walk attempts by default: {@code xmi}, {@code ecore},
	 * {@code json}, {@code xml}. Anything else found in a directory - a {@code .keep} placeholder,
	 * a {@code README.md}, an editor swap file - is passed over instead of parsed and reported as
	 * broken.
	 */
	public static final Set<String> DEFAULT_FILE_EXTENSIONS = Set.of("xmi", "ecore", "json", "xml");

	private static final Logger logger = Logger.getLogger(FileEObjectProvider.class.getName());

	private final String providerName;
	private final ResourceSet resourceSet;
	private final List<Path> locations;
	private final BiFunction<Resource, EObject, String> keyFunction;
	private final Set<String> fileExtensions;

	/**
	 * Creates a provider that attempts the {@link #DEFAULT_FILE_EXTENSIONS} when walking a
	 * directory.
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
		this(providerName, resourceSet, locations, keyFunction, DEFAULT_FILE_EXTENSIONS);
	}

	/**
	 * Creates a provider with an explicit extension allow-list for directory walks.
	 * <p>
	 * The allow-list only applies to files <em>found by walking a directory</em>: a location that
	 * names a file directly is always loaded, whatever it is called. Dotfiles are never walked into
	 * a load - a {@code .keep} placeholder, a {@code .DS_Store}, an editor's {@code .swp} file are
	 * not model content, and reporting them as broken resources would only train operators to
	 * ignore the warnings that matter.
	 *
	 * @param providerName   the provider's name - the entries' source tag; must not be {@code null}
	 * @param resourceSet    the resource set to load with; must not be {@code null}
	 * @param locations      files or directories to load; must not be {@code null}
	 * @param keyFunction    derives the entry key from (resource, root object), see
	 *                       {@link #uriFragmentKeys()} and {@link #featureKeys(String)}; returning
	 *                       {@code null} skips the object; must not be {@code null}
	 * @param fileExtensions the extensions a directory walk attempts, without the leading dot and
	 *                       matched case-insensitively (a leading dot is tolerated and stripped);
	 *                       <strong>empty means attempt every file</strong>. Must not be
	 *                       {@code null} - see {@link #DEFAULT_FILE_EXTENSIONS}
	 */
	public FileEObjectProvider(String providerName, ResourceSet resourceSet, List<Path> locations,
			BiFunction<Resource, EObject, String> keyFunction, Collection<String> fileExtensions) {
		this.providerName = Objects.requireNonNull(providerName, "providerName");
		this.resourceSet = Objects.requireNonNull(resourceSet, "resourceSet");
		this.locations = List.copyOf(Objects.requireNonNull(locations, "locations"));
		this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
		this.fileExtensions = normalizeExtensions(fileExtensions);
	}

	/** Trims, lower-cases and strips a leading dot; blank entries are dropped. */
	private static Set<String> normalizeExtensions(Collection<String> fileExtensions) {
		Objects.requireNonNull(fileExtensions, "fileExtensions");
		Set<String> normalized = new HashSet<>();
		for (String extension : fileExtensions) {
			if (extension == null || extension.isBlank()) {
				continue;
			}
			String trimmed = extension.trim().toLowerCase(Locale.ROOT);
			if (trimmed.startsWith(".")) {
				trimmed = trimmed.substring(1);
			}
			if (!trimmed.isEmpty()) {
				normalized.add(trimmed);
			}
		}
		return Set.copyOf(normalized);
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
			// a directly named location is deliberate: load it whatever it is called
			return List.of(location);
		}
		try (Stream<Path> walk = Files.walk(location)) {
			return walk.filter(Files::isRegularFile).filter(this::isLoadable).sorted().toList();
		} catch (IOException e) {
			logger.log(Level.WARNING,
					String.format("Provider %s: cannot walk directory %s - skipping", providerName, location), e);
			return List.of();
		}
	}

	/**
	 * Decides whether a file found by walking a directory is worth handing to EMF. Dotfiles never
	 * are; beyond those the {@code fileExtensions} allow-list decides, an empty one accepting
	 * everything. A pass-over is a normal, expected event and is therefore logged at
	 * {@link Level#FINE} - only a file that was meant to be a model and failed to parse deserves a
	 * warning.
	 */
	private boolean isLoadable(Path file) {
		String fileName = file.getFileName().toString();
		if (fileName.startsWith(".")) {
			logger.fine(() -> String.format("Provider %s: passing over dotfile %s", providerName, file));
			return false;
		}
		if (fileExtensions.isEmpty()) {
			return true;
		}
		int dot = fileName.lastIndexOf('.');
		String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
		if (fileExtensions.contains(extension)) {
			return true;
		}
		logger.fine(() -> String.format("Provider %s: passing over %s - extension not in %s", providerName, file,
				fileExtensions));
		return false;
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
