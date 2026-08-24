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
package org.eclipse.fennec.emf.osgi.codegen;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.codegen.ecore.genmodel.GenJDKLevel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenResourceKind;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;

import aQute.bnd.build.Container;

/**
 * Derives a genmodel from an ecore that carries its genmodel options as GenModel
 * annotations, so a project only needs to maintain the ecore (issue #88). The derived
 * genmodel is saved next to the ecore and the normal genmodel based generation runs on it.
 * @author Juergen Albert
 * @since 24 Aug 2026
 */
public final class GenModelDeriver {

	/** the EMF standard annotation source carrying genmodel options in an ecore */
	public static final String GENMODEL_ANNOTATION_SOURCE = "http://www.eclipse.org/emf/2002/GenModel"; //$NON-NLS-1$

	private static final String DEFAULT_ROOT_EXTENDS_CLASS = "org.eclipse.emf.ecore.impl.MinimalEObjectImpl$Container"; //$NON-NLS-1$
	private static final String ECORE_EXTENSION = ".ecore"; //$NON-NLS-1$
	private static final String GENMODEL_EXTENSION = ".genmodel"; //$NON-NLS-1$

	private GenModelDeriver() {
	}

	/**
	 * Derives a genmodel from the GenModel annotations of the given ecore and saves it next
	 * to the ecore, overwriting the previously derived file. GenPackages of EPackages
	 * referenced across bundles are resolved from the genmodels those buildpath bundles ship
	 * and linked as usedGenPackages.
	 *
	 * @param ecorePath the project relative path of the ecore
	 * @param output the output folder of the generated code, becomes the genmodel's model directory
	 * @param refModels the resolvable model locations per buildpath container
	 * @param bsn the bundle symbolic name of the generating project
	 * @param base the project directory
	 * @return the project relative path of the saved genmodel
	 * @throws IllegalStateException with a user readable message when the genmodel cannot be derived
	 * @throws Exception when loading or saving a model fails
	 */
	public static String derive(String ecorePath, String output, Map<Container, Map<String, String>> refModels, String bsn, File base) throws Exception {
		ResourceSet resourceSet = new ResourceSetImpl();
		try {
			FennecEmfGenerator.configureEMF(resourceSet, refModels, bsn, base);
			URI ecoreUri = URI.createURI(UriSanatizer.SCHEMA_RESOURCE + bsn + UriSanatizer.SLASH + ecorePath);
			FennecEmfGenerator.info("Deriving a genmodel from " + ecoreUri);
			Resource ecoreResource = resourceSet.getResource(ecoreUri, true);
			if (!ecoreResource.getErrors().isEmpty()) {
				throw new IllegalStateException("Could not load " + ecorePath + ": " + ecoreResource.getErrors().get(0));
			}
			List<EPackage> rootPackages = ecoreResource.getContents().stream()
					.filter(EPackage.class::isInstance)
					.map(EPackage.class::cast)
					.toList();
			if (rootPackages.isEmpty()) {
				throw new IllegalStateException("The ecore " + ecorePath + " does not contain an EPackage");
			}
			rootPackages.forEach(EcoreUtil::resolveAll);

			String genModelPath = genModelPath(ecorePath);
			URI genModelUri = URI.createURI(UriSanatizer.SCHEMA_RESOURCE + bsn + UriSanatizer.SLASH + genModelPath);
			Resource genModelResource = resourceSet.createResource(genModelUri, GenModelPackage.eCONTENT_TYPE);
			GenModel genModel = GenModelFactory.eINSTANCE.createGenModel();
			genModelResource.getContents().add(genModel);
			genModel.initialize(rootPackages);

			applyGenModelOptions(genModel, rootPackages.get(0), bsn, output);
			int genPackageCount = Math.min(rootPackages.size(), genModel.getGenPackages().size());
			for (int i = 0; i < genPackageCount; i++) {
				configureGenPackage(genModel.getGenPackages().get(i), rootPackages.get(i));
			}
			addUsedGenPackages(genModel, rootPackages, resourceSet, refModels, bsn);
			genModel.getForeignModel().add(new File(ecorePath).getName());
			// applied last: initialize and proxy resolution may flip it for large models
			for (int i = 0; i < genPackageCount; i++) {
				genModel.getGenPackages().get(i).setLoadInitialization(annotationBoolean(rootPackages.get(i), "loadInitialization", false));
			}
			genModelResource.save(Map.of(XMLResource.OPTION_LINE_WIDTH, 80, XMLResource.OPTION_ENCODING, "UTF-8")); //$NON-NLS-1$
			FennecEmfGenerator.info("Saved the derived genmodel to " + genModelPath);
			return genModelPath;
		} finally {
			resourceSet.getResources().forEach(Resource::unload);
			resourceSet.getResources().clear();
		}
	}

	private static String genModelPath(String ecorePath) {
		if (ecorePath.endsWith(ECORE_EXTENSION)) {
			return ecorePath.substring(0, ecorePath.length() - ECORE_EXTENSION.length()) + GENMODEL_EXTENSION;
		}
		return ecorePath + GENMODEL_EXTENSION;
	}

	private static void applyGenModelOptions(GenModel genModel, EPackage ePackage, String bsn, String output) {
		genModel.setModelName(capitalize(ePackage.getName()));
		genModel.setModelPluginID(bsn);
		genModel.setModelDirectory(UriSanatizer.SLASH + bsn + (output.startsWith(UriSanatizer.SLASH) ? "" : UriSanatizer.SLASH) + output);
		genModel.setComplianceLevel(GenJDKLevel.JDK170_LITERAL);
		genModel.setImporterID("org.eclipse.emf.importer.ecore"); //$NON-NLS-1$
		genModel.setBundleManifest(false);
		genModel.setCopyrightFields(false);
		genModel.setOperationReflection(true);
		genModel.setImportOrganizing(true);
		genModel.setOSGiCompatible(annotationBoolean(ePackage, "oSGiCompatible", true));
		genModel.setSuppressInterfaces(annotationBoolean(ePackage, "suppressInterfaces", false));
		genModel.setSuppressEMFTypes(annotationBoolean(ePackage, "suppressEMFTypes", false));
		genModel.setSuppressEMFMetaData(annotationBoolean(ePackage, "suppressEMFMetaData", false));
		genModel.setSuppressGenModelAnnotations(annotationBoolean(ePackage, "suppressGenModelAnnotations", true));
		genModel.setPublicConstructors(annotationBoolean(ePackage, "publicConstructors", false));
		genModel.setRootExtendsClass(annotation(ePackage, "rootExtendsClass").orElse(DEFAULT_ROOT_EXTENDS_CLASS));
		annotation(ePackage, "rootExtendsInterface").ifPresent(genModel::setRootExtendsInterface);
		annotation(ePackage, "copyrightText").ifPresent(genModel::setCopyrightText);
	}

	private static void configureGenPackage(GenPackage genPackage, EPackage ePackage) {
		String basePackage = annotation(ePackage, "basePackage").orElseGet(() -> deriveBasePackage(ePackage.getNsURI()));
		if (basePackage != null && !basePackage.isEmpty()) {
			genPackage.setBasePackage(basePackage);
		}
		genPackage.setPrefix(annotation(ePackage, "prefix").orElseGet(() -> capitalize(ePackage.getName())));
		annotation(ePackage, "fileExtensions").or(() -> annotation(ePackage, "fileExtension")).ifPresent(genPackage::setFileExtensions);
		annotation(ePackage, "resource").map(GenResourceKind::get).ifPresent(genPackage::setResource);
		annotation(ePackage, "contentTypeIdentifier").ifPresent(genPackage::setContentTypeIdentifier);
		genPackage.setLiteralsInterface(annotationBoolean(ePackage, "literalsInterface", true));
		genPackage.setDisposableProviderFactory(true);
	}

	private static Optional<String> annotation(EPackage ePackage, String key) {
		EAnnotation annotation = ePackage.getEAnnotation(GENMODEL_ANNOTATION_SOURCE);
		if (annotation == null) {
			return Optional.empty();
		}
		String value = annotation.getDetails().get(key);
		return value == null || value.isEmpty() ? Optional.empty() : Optional.of(value);
	}

	private static boolean annotationBoolean(EPackage ePackage, String key, boolean defaultValue) {
		return annotation(ePackage, key).map(Boolean::parseBoolean).orElse(defaultValue);
	}

	/**
	 * Derives the base package from a nsURI the way the EMF ecore importer does: the host
	 * segments reversed plus the path segments except the last one, which usually names the model.
	 * @param nsUri the nsURI of the EPackage
	 * @return the derived base package or <code>null</code> when the nsURI has no usable host
	 */
	private static String deriveBasePackage(String nsUri) {
		if (nsUri == null || nsUri.isEmpty()) {
			return null;
		}
		int schemeEnd = nsUri.indexOf("://"); //$NON-NLS-1$
		if (schemeEnd < 0) {
			return null;
		}
		String rest = nsUri.substring(schemeEnd + 3);
		int pathStart = rest.indexOf('/');
		String host = pathStart < 0 ? rest : rest.substring(0, pathStart);
		String path = pathStart < 0 ? "" : rest.substring(pathStart);
		if (host.isEmpty()) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		String[] hostParts = host.split("\\."); //$NON-NLS-1$
		for (int i = hostParts.length - 1; i >= 0; i--) {
			if (result.length() > 0) {
				result.append('.');
			}
			result.append(hostParts[i].toLowerCase(Locale.ROOT));
		}
		String[] pathParts = path.split(UriSanatizer.SLASH);
		for (int i = 1; i < pathParts.length - 1; i++) {
			if (!pathParts[i].isEmpty()) {
				result.append('.').append(pathParts[i].toLowerCase(Locale.ROOT));
			}
		}
		return result.toString();
	}

	private static String capitalize(String name) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static void addUsedGenPackages(GenModel genModel, List<EPackage> rootPackages, ResourceSet resourceSet, Map<Container, Map<String, String>> refModels, String bsn) {
		for (EPackage external : findReferencedExternalPackages(rootPackages)) {
			GenPackage genPackage = findBuildpathGenPackage(external, resourceSet, refModels, bsn);
			if (!genModel.getUsedGenPackages().contains(genPackage)) {
				genModel.getUsedGenPackages().add(genPackage);
			}
		}
	}

	/**
	 * Collects all EPackages outside this ecore that classifiers of the given packages reference,
	 * following references of the collected packages transitively. EMF core packages are skipped,
	 * EMF synthesizes their GenPackages itself.
	 * @param rootPackages the root EPackages of the ecore
	 * @return the referenced external EPackages
	 */
	private static Set<EPackage> findReferencedExternalPackages(List<EPackage> rootPackages) {
		Set<String> visited = new HashSet<>();
		rootPackages.forEach(p -> collectNsURIs(p, visited));
		Set<EPackage> referenced = new LinkedHashSet<>();
		Deque<EPackage> worklist = new ArrayDeque<>(rootPackages);
		while (!worklist.isEmpty()) {
			EPackage current = worklist.poll();
			for (TreeIterator<EObject> it = current.eAllContents(); it.hasNext();) {
				for (EObject crossReferenced : it.next().eCrossReferences()) {
					if (crossReferenced instanceof EClassifier classifier) {
						EPackage ePackage = classifier.getEPackage();
						if (ePackage != null && ePackage.getNsURI() != null && !isEmfCorePackage(ePackage.getNsURI())
								&& visited.add(ePackage.getNsURI())) {
							referenced.add(ePackage);
							worklist.add(ePackage);
						}
					}
				}
			}
		}
		return referenced;
	}

	private static void collectNsURIs(EPackage ePackage, Set<String> out) {
		if (ePackage.getNsURI() != null) {
			out.add(ePackage.getNsURI());
		}
		ePackage.getESubpackages().forEach(sub -> collectNsURIs(sub, out));
	}

	private static boolean isEmfCorePackage(String nsURI) {
		return nsURI.startsWith("http://www.eclipse.org/emf/") //$NON-NLS-1$
				|| nsURI.startsWith("http://www.w3.org/") //$NON-NLS-1$
				|| nsURI.startsWith("http:///org/eclipse/emf/"); //$NON-NLS-1$
	}

	/**
	 * Finds the GenPackage of the referenced EPackage in the genmodels shipped by the buildpath
	 * bundle that provides the EPackage.
	 * @param external the referenced EPackage
	 * @param resourceSet the resource set to load the genmodels with
	 * @param refModels the resolvable model locations per buildpath container
	 * @param bsn the bundle symbolic name of the generating project
	 * @return the GenPackage, never <code>null</code>
	 * @throws IllegalStateException when the bundle or its genmodel cannot be located
	 */
	private static GenPackage findBuildpathGenPackage(EPackage external, ResourceSet resourceSet, Map<Container, Map<String, String>> refModels, String bsn) {
		String nsURI = external.getNsURI();
		Resource resource = external.eResource();
		if (resource == null) {
			throw new IllegalStateException("The referenced EPackage " + nsURI + " is not contained in a resource, cannot locate its genmodel");
		}
		URI sanitized = UriSanatizer.trimmedSanitize(resource.getURI());
		if (sanitized == null || !UriSanatizer.RESOURCE_SCHEMA_NAME.equals(sanitized.scheme())) {
			throw new IllegalStateException("Cannot locate the bundle providing the referenced EPackage " + nsURI + " (" + resource.getURI() + ")");
		}
		String referencedBsn = sanitized.host();
		if (bsn.equals(referencedBsn)) {
			throw new IllegalStateException("The referenced EPackage " + nsURI + " lives in this project. In ecore mode only EPackages of buildpath bundles can be referenced");
		}
		Map<String, String> locations = findContainerLocations(refModels, referencedBsn, nsURI);
		URI bundleBase = bundleBase(resource.getURI(), referencedBsn);
		Set<URI> candidates = new LinkedHashSet<>();
		for (String location : locations.values()) {
			if (location.endsWith(GENMODEL_EXTENSION)) {
				String path = location.startsWith(UriSanatizer.SLASH) ? location.substring(1) : location;
				candidates.add(bundleBase.appendSegments(URI.createURI(path).segments()));
			}
		}
		if (candidates.isEmpty()) {
			throw new IllegalStateException("The bundle " + referencedBsn + " providing the referenced EPackage " + nsURI
					+ " does not ship a genmodel, which ecore mode needs to link the usedGenPackages");
		}
		for (URI candidate : candidates) {
			GenPackage genPackage = findGenPackageInGenModel(resourceSet, candidate, nsURI);
			if (genPackage != null) {
				FennecEmfGenerator.info("Linking usedGenPackage for " + nsURI + " from " + candidate);
				return genPackage;
			}
		}
		throw new IllegalStateException("No GenPackage for the referenced EPackage " + nsURI + " found in the genmodels of the bundle "
				+ referencedBsn + ": " + candidates);
	}

	private static Map<String, String> findContainerLocations(Map<Container, Map<String, String>> refModels, String referencedBsn, String nsURI) {
		for (Entry<Container, Map<String, String>> entry : refModels.entrySet()) {
			if (referencedBsn.equals(ResourceUriHandler.getBSN(entry.getKey()))) {
				return entry.getValue();
			}
		}
		throw new IllegalStateException("No buildpath entry found for the bundle " + referencedBsn + " providing the referenced EPackage " + nsURI);
	}

	/**
	 * The URI prefix of the referenced bundle, preserving the relative form the ecore used
	 * (e.g. <code>resource://bsn/../other.project</code>) so the hrefs in the saved genmodel
	 * serialize as the same relative paths a hand written genmodel would use.
	 */
	private static URI bundleBase(URI ecoreUri, String referencedBsn) {
		List<String> segments = ecoreUri.segmentsList();
		int index = segments.indexOf(referencedBsn);
		if (index >= 0) {
			return ecoreUri.trimSegments(segments.size() - index - 1);
		}
		return URI.createURI(UriSanatizer.SCHEMA_RESOURCE + referencedBsn);
	}

	private static GenPackage findGenPackageInGenModel(ResourceSet resourceSet, URI genModelUri, String nsURI) {
		Resource resource;
		try {
			resource = resourceSet.getResource(genModelUri, true);
		} catch (Exception e) {
			FennecEmfGenerator.warn("Could not load the genmodel " + genModelUri + ": " + e.getMessage());
			return null;
		}
		for (EObject content : resource.getContents()) {
			if (content instanceof GenModel genModel) {
				GenPackage genPackage = findGenPackage(genModel.getGenPackages(), nsURI);
				if (genPackage != null) {
					return genPackage;
				}
			}
		}
		return null;
	}

	private static GenPackage findGenPackage(List<GenPackage> genPackages, String nsURI) {
		for (GenPackage genPackage : genPackages) {
			EPackage ecorePackage = genPackage.getEcorePackage();
			if (ecorePackage != null && nsURI.equals(ecorePackage.getNsURI())) {
				return genPackage;
			}
			GenPackage nested = findGenPackage(genPackage.getNestedGenPackages(), nsURI);
			if (nested != null) {
				return nested;
			}
		}
		return null;
	}

}
