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

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.fennec.emf.osgi.configurator.ResourceSetConfigurator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Makes the declared location of every generated package resolvable to the package itself.
 * <p>
 * Tracks the {@code org.eclipse.emf.ecore.generated_package} capabilities of the installed bundles
 * and installs a {@link GeneratedPackageResourceLocator} in each resource set the resource set
 * factory creates. A reference like
 * {@code platform:/plugin/org.example.model/model/example.ecore#//Foo} then resolves to the
 * registered generated package, the same as a reference by its namespace URI. Unlike in Eclipse,
 * where such a reference loads the model file as a dynamic copy, no second identity of the package
 * appears. See issue #114.
 */
@Component(name = "GeneratedPackageLocationConfigurator", immediate = true, service = ResourceSetConfigurator.class)
public class GeneratedPackageLocationConfiguratorComponent implements ResourceSetConfigurator {

	private static final Logger logger = Logger.getLogger(GeneratedPackageLocationConfiguratorComponent.class.getName());

	private final GeneratedPackageLocations locations = new GeneratedPackageLocations();
	private BundleTracker<Map<URI, String>> tracker;

	/**
	 * Called on component activation, starts tracking the generated packages
	 * @param context the bundle context
	 */
	@Activate
	public void activate(BundleContext context) {
		tracker = new BundleTracker<>(context, Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING,
				new GeneratedPackageCustomizer());
		tracker.open();
	}

	/**
	 * Called on component deactivation, stops tracking the generated packages
	 */
	@Deactivate
	public void deactivate() {
		tracker.close();
		locations.clear();
	}

	@Override
	public void configureResourceSet(ResourceSet resourceSet) {
		if (resourceSet instanceof ResourceSetImpl resourceSetImpl) {
			new GeneratedPackageResourceLocator(resourceSetImpl, locations::getNsUri);
		} else {
			logger.fine(() -> "Cannot resolve generated package locations in a resource set of type "
					+ resourceSet.getClass().getName());
		}
	}

	/**
	 * Maintains the locations of a bundle while it is tracked.
	 */
	private class GeneratedPackageCustomizer implements BundleTrackerCustomizer<Map<URI, String>> {

		@Override
		public Map<URI, String> addingBundle(Bundle bundle, BundleEvent event) {
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			if (revision == null || (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0
					|| revision.getSymbolicName() == null) {
				return null;
			}
			List<Map<String, Object>> attributes = revision
					.getDeclaredCapabilities(GeneratedPackageLocations.GENERATED_PACKAGE_NAMESPACE).stream()
					.map(c -> c.getAttributes()).toList();
			if (attributes.isEmpty()) {
				return null;
			}
			Map<URI, String> bundleLocations = GeneratedPackageLocations.locationsOf(revision.getSymbolicName(), attributes);
			locations.add(bundleLocations);
			return bundleLocations;
		}

		@Override
		public void modifiedBundle(Bundle bundle, BundleEvent event, Map<URI, String> bundleLocations) {
			// the capabilities of a revision do not change
		}

		@Override
		public void removedBundle(Bundle bundle, BundleEvent event, Map<URI, String> bundleLocations) {
			locations.remove(bundleLocations);
		}
	}
}
