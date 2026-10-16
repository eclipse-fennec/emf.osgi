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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * Locates the EPackages of the buildpath bundles by nsURI, so a cross-package reference written
 * with the nsURI of the target package resolves during generation (issue #113). The locations come
 * from the generated_package capabilities and, for bundles without them, from the nsURI attributes
 * of the shipped ecore files.
 * @author Mark Hoffmann
 * @since 16 Oct 2026
 */
public final class NsUriIndex {

	private static final String NS_URI_ATTRIBUTE = "nsURI"; //$NON-NLS-1$
	private static final String EPACKAGE_ELEMENT = "EPackage"; //$NON-NLS-1$
	private static final String SUBPACKAGE_ELEMENT = "eSubpackages"; //$NON-NLS-1$

	/** nsURI to the bundle and the bundle relative ecore path */
	private final Map<String, Location> locations = new LinkedHashMap<>();

	record Location(String bsn, String path) {
	}

	/**
	 * Adds the location a generated_package capability names. The first bundle in buildpath
	 * order providing a nsURI wins.
	 * @param bsn the bundle symbolic name of the providing bundle
	 * @param nsUri the nsURI of the EPackage
	 * @param ecoreLocation the bundle relative location of the ecore
	 */
	void add(String bsn, String nsUri, String ecoreLocation) {
		if (bsn == null || nsUri == null || ecoreLocation == null) {
			return;
		}
		Location location = new Location(bsn, ecoreLocation.startsWith(UriSanatizer.SLASH) ? ecoreLocation.substring(1) : ecoreLocation);
		Location existing = locations.putIfAbsent(nsUri, location);
		if (existing != null && !existing.equals(location)) {
			FennecEmfGenerator.warn("The nsURI " + nsUri + " is provided by " + existing.bsn() + "/" + existing.path()
					+ " and " + location.bsn() + "/" + location.path() + ", using the first one");
		}
	}

	/**
	 * Adds the nsURIs of the EPackages (including the subpackages) an ecore file declares.
	 * @param bsn the bundle symbolic name of the providing bundle
	 * @param ecoreLocation the bundle relative location of the ecore
	 * @param in the content of the ecore, closed by the caller
	 */
	void addEcore(String bsn, String ecoreLocation, InputStream in) {
		try {
			readNsUris(in).forEach(nsUri -> add(bsn, nsUri, ecoreLocation));
		} catch (XMLStreamException e) {
			FennecEmfGenerator.warn("Could not read the nsURIs of " + bsn + "/" + ecoreLocation + ": " + e.getMessage());
		}
	}

	Map<String, Location> locations() {
		return Collections.unmodifiableMap(locations);
	}

	/**
	 * Registers a lazy {@link EPackage.Descriptor} per nsURI in the package registry of the
	 * resource set. The ecore is loaded under the relative form a hand written reference uses
	 * (<code>resource://&lt;bsn&gt;/../&lt;referenced bsn&gt;/&lt;path&gt;</code>), so a derived
	 * genmodel serializes its hrefs as relative paths. nsURIs of the global registry and of the
	 * EMF core packages are never registered: shadowing them with a loaded copy would break the
	 * type references of every model.
	 * @param resourceSet the resource set to register the descriptors in
	 * @param bsn the bundle symbolic name of the generating project
	 */
	void register(ResourceSet resourceSet, String bsn) {
		locations.forEach((nsUri, location) -> {
			if (EPackage.Registry.INSTANCE.containsKey(nsUri) || GenModelDeriver.isEmfCorePackage(nsUri)) {
				return;
			}
			URI uri = URI.createURI(UriSanatizer.SCHEMA_RESOURCE + bsn + "/../" + location.bsn() + UriSanatizer.SLASH + location.path()); //$NON-NLS-1$
			resourceSet.getPackageRegistry().put(nsUri, new BuildpathEPackageDescriptor(resourceSet, nsUri, uri));
		});
	}

	private static List<String> readNsUris(InputStream in) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		XMLStreamReader reader = factory.createXMLStreamReader(in);
		try {
			List<String> nsUris = new ArrayList<>();
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT
						&& (EPACKAGE_ELEMENT.equals(reader.getLocalName()) || SUBPACKAGE_ELEMENT.equals(reader.getLocalName()))) {
					String nsUri = reader.getAttributeValue(null, NS_URI_ATTRIBUTE);
					if (nsUri != null && !nsUri.isEmpty()) {
						nsUris.add(nsUri);
					}
				}
			}
			return nsUris;
		} finally {
			reader.close();
		}
	}

	/**
	 * Loads the ecore of a buildpath bundle on first access and returns the EPackage with the nsURI.
	 */
	private static final class BuildpathEPackageDescriptor implements EPackage.Descriptor {

		private final ResourceSet resourceSet;
		private final String nsUri;
		private final URI uri;

		BuildpathEPackageDescriptor(ResourceSet resourceSet, String nsUri, URI uri) {
			this.resourceSet = resourceSet;
			this.nsUri = nsUri;
			this.uri = uri;
		}

		@Override
		public EPackage getEPackage() {
			FennecEmfGenerator.info("Resolving the nsURI " + nsUri + " from " + uri);
			Resource resource = resourceSet.getResource(uri, true);
			for (EObject content : resource.getContents()) {
				if (content instanceof EPackage ePackage) {
					EPackage found = findPackage(ePackage);
					if (found != null) {
						return found;
					}
				}
			}
			FennecEmfGenerator.warn("The ecore " + uri + " does not contain an EPackage with the nsURI " + nsUri);
			return null;
		}

		@Override
		public EFactory getEFactory() {
			EPackage ePackage = getEPackage();
			return ePackage == null ? null : ePackage.getEFactoryInstance();
		}

		private EPackage findPackage(EPackage ePackage) {
			if (nsUri.equals(ePackage.getNsURI())) {
				return ePackage;
			}
			for (EPackage sub : ePackage.getESubpackages()) {
				EPackage found = findPackage(sub);
				if (found != null) {
					return found;
				}
			}
			return null;
		}
	}

}
