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
package org.eclipse.fennec.emf.osgi.metadata.impl;

import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.eclipse.fennec.emf.osgi.metadata.MetadataHandler;
import org.eclipse.fennec.emf.osgi.metadata.MetadataIndex;
import org.eclipse.fennec.emf.osgi.metadata.MetadataService;
import org.eclipse.fennec.emf.osgi.metadata.MetadataWhiteboard;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * OSGi wiring for {@link MetadataServiceImpl}: turns EMF's registry into a whiteboard by
 * tracking {@link EPackage} services, and lets the index and the handlers come and go.
 * <p>
 * Registered under both {@link MetadataWhiteboard} and {@link MetadataService}, so a
 * consumer can inject read-only access while lifecycle stays with whoever asks for the
 * whiteboard.
 * <p>
 * The {@link FingerprintService} reference is mandatory and static: model identity is the
 * key everything else hangs on, so the component must not start without it, and swapping
 * it underneath a populated registry would invalidate every key at once. It is injected
 * through the constructor because SCR calls bind methods in descriptor order - a bind
 * method could run after {@code addEPackage} for services present at activation, losing
 * those packages permanently. Constructor injection always precedes any bind method.
 *
 * @author Data In Motion Consulting
 */
@Component(name = "MetadataServiceComponent", service = { MetadataWhiteboard.class,
		MetadataService.class }, immediate = true)
public class MetadataServiceComponent extends MetadataServiceImpl {

	@Activate
	public MetadataServiceComponent(@Reference FingerprintService fingerprintService) {
		setFingerprintService(fingerprintService);
	}

	/**
	 * An EPackage service appeared. Its service properties travel along as transient build
	 * context for the handlers.
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeEPackage")
	void addEPackage(EPackage ePackage, Map<String, Object> properties) {
		registerPackage(ePackage, properties);
	}

	void removeEPackage(EPackage ePackage) {
		unregisterPackage(ePackage);
	}

	/**
	 * An index appeared. It is populated from the registry, so binding one after packages
	 * are already registered loses nothing.
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unbindIndex")
	void bindIndex(MetadataIndex index) {
		setMetadataIndex(index);
	}

	void unbindIndex(MetadataIndex index) {
		unsetMetadataIndex(index);
	}

	/**
	 * A handler appeared. It immediately sees every model version already known, so a
	 * contributor arriving late still gets its entries onto existing trees.
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeHandler")
	void addHandler(MetadataHandler handler) {
		addMetadataHandler(handler);
	}

	void removeHandler(MetadataHandler handler) {
		removeMetadataHandler(handler);
	}
}
