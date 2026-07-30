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
package org.eclipse.fennec.emf.osgi.metadata;

import static java.util.Objects.requireNonNull;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.eclipse.fennec.emf.osgi.metadata.impl.MetadataServiceImpl;

/**
 * Bootstrap for plain Java: unit tests and standalone tools that need a
 * {@link MetadataWhiteboard} without a running OSGi framework.
 * <p>
 * In OSGi the whiteboard arrives as a service and DS wires the mandatory
 * {@link FingerprintService} and every {@link MetadataHandler}; nothing here is needed. On a
 * flat classpath the same wiring has to happen by hand, and this factory is where that
 * knowledge lives - the implementation class is in a private package, so a downstream
 * bundle rebuilding this sequence itself would have to compile against
 * {@code org.eclipse.fennec.emf.osgi.metadata.impl} and negate that import in its manifest.
 * <p>
 * Model identity is the key everything else hangs on, so the {@link FingerprintService} is
 * explicit in {@link #createWhiteboard(FingerprintService, MetadataHandler...)}. The
 * parameterless {@link #createWhiteboard(MetadataHandler...)} takes the shipped default from
 * {@link FingerprintHelper#getDefaultFingerprintService()} - the implementation class itself
 * sits in a private package, so that accessor is the only way to reach it without the same
 * manifest workaround one package over.
 *
 * @author Data In Motion Consulting
 */
public final class MetadataServices {

	private MetadataServices() {
		// static factory
	}

	/**
	 * Creates a whiteboard over an empty registry with the default
	 * {@link FingerprintService}, wired for use outside OSGi.
	 * <p>
	 * The identity scheme of that default is the same one the registry components emit their
	 * {@code emf.fingerprint} service property with, so a whiteboard built here keys models
	 * exactly as the OSGi path would. Use
	 * {@link #createWhiteboard(FingerprintService, MetadataHandler...)} to be explicit, or
	 * {@link MetadataWhiteboard#setFingerprintService(FingerprintService)} to replace the
	 * default afterwards.
	 *
	 * @param handlers handlers to register up front; may be empty, {@code null} entries are
	 *            ignored
	 * @return a new whiteboard, ready to register packages
	 */
	public static MetadataWhiteboard createWhiteboard(MetadataHandler... handlers) {
		return createWhiteboard(FingerprintHelper.getDefaultFingerprintService(), handlers);
	}

	/**
	 * Creates a whiteboard over an empty registry, wired for use outside OSGi.
	 * <p>
	 * The handlers are added before the whiteboard is handed back, so they see every model
	 * version from the first {@link MetadataWhiteboard#registerPackage(EPackage)} onwards -
	 * the same guarantee DS gives, without relying on the replay that
	 * {@link MetadataWhiteboard#addMetadataHandler(MetadataHandler)} performs for late
	 * arrivals.
	 * <p>
	 * An index is not part of the signature: it is optional in OSGi too, and
	 * {@link MetadataWhiteboard#setMetadataIndex(MetadataIndex)} is public API for callers
	 * that want a specific one. The returned whiteboard already carries an in-memory
	 * default, so URI and name lookups answer from the start.
	 *
	 * @param fingerprintService computes model identity; must not be {@code null}
	 * @param handlers handlers to register up front; may be empty, {@code null} entries are
	 *            ignored
	 * @return a new whiteboard, ready to register packages
	 * @throws NullPointerException if {@code fingerprintService} is {@code null}
	 */
	public static MetadataWhiteboard createWhiteboard(FingerprintService fingerprintService,
			MetadataHandler... handlers) {
		requireNonNull(fingerprintService, "fingerprintService");

		MetadataServiceImpl service = new MetadataServiceImpl();
		service.setFingerprintService(fingerprintService);
		if (handlers != null) {
			for (MetadataHandler handler : handlers) {
				service.addMetadataHandler(handler);
			}
		}
		return service;
	}
}
