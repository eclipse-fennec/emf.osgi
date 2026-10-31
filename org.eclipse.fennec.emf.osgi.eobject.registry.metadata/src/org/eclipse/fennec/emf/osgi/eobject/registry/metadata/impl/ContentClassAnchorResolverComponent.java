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
package org.eclipse.fennec.emf.osgi.eobject.registry.metadata.impl;

import java.util.Collection;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryEntry;
import org.eclipse.fennec.emf.osgi.eobject.registry.metadata.AspectAnchorResolver;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link AspectAnchorResolver#contentClass()} default as a service. The bridge
 * reference is mandatory and targets this service unless a configuration names another
 * resolver - so "use the default" is an explicit selection, never the fallback for a
 * resolver that is not there yet.
 *
 * @author Data In Motion Consulting
 */
@Component(property = AspectAnchorResolver.ANCHOR_RESOLVER + "=" + AspectAnchorResolver.CONTENT_CLASS)
public class ContentClassAnchorResolverComponent implements AspectAnchorResolver {

	private final AspectAnchorResolver delegate = AspectAnchorResolver.contentClass();

	@Override
	public Collection<EClass> anchorsOf(EObjectRegistryEntry entry) {
		return delegate.anchorsOf(entry);
	}
}
