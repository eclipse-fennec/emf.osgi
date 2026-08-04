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
package org.eclipse.fennec.emf.osgi.eobject.registry.metadata;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryEntry;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Resolves which EClasses a registry entry "belongs to" - the anchors its aspect is
 * attached at. Anchor resolution is domain-specific: the default anchors at the
 * content's own {@code eClass()}; a sensinact provider mapping would anchor at
 * {@code mapping.getProviderClasses()} instead (one entry, many anchors).
 * <p>
 * In OSGi a resolver can be a service the bridge component references by target.
 *
 * @author Data In Motion Consulting
 */
@ConsumerType
@FunctionalInterface
public interface AspectAnchorResolver {

	/**
	 * Returns the anchor classes of the given entry.
	 *
	 * @param entry the registry entry; never {@code null}
	 * @return the anchors; empty means "attach nowhere", never {@code null}
	 */
	Collection<EClass> anchorsOf(EObjectRegistryEntry entry);

	/**
	 * The default resolver: the content's own EClass.
	 *
	 * @return the default resolver
	 */
	static AspectAnchorResolver contentClass() {
		return entry -> List.of(entry.object().eClass());
	}
}
