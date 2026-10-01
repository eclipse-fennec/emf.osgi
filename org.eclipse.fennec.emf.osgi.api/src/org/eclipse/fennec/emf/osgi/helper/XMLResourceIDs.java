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
package org.eclipse.fennec.emf.osgi.helper;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.ecore.xmi.XMLResource;

/**
 * Preserves {@code xmi:id}s when an object tree is moved or copied between {@link XMLResource}s.
 * <p>
 * EMF keeps the ids in the resource ({@code idToEObjectMap} / {@code eObjectToIDMap}), not on
 * the {@link EObject}. As soon as an object leaves its resource - by adding it to another
 * resource, removing it from its resource, or copying it with {@code EcoreUtil.copy} - its id
 * and the ids of all contained objects are gone. This helper collects the ids before and
 * re-applies them after.
 * <p>
 * Objects without an id are skipped, ids are never invented. A source or target that is not an
 * {@link XMLResource} has no ids to collect or apply; the operations then behave like the plain
 * EMF calls.
 *
 * @author Mark Hoffmann
 * @since 01.10.2026
 */
public final class XMLResourceIDs {

	/**
	 * A copy of an object tree together with the ids of the originals, keyed by the copied objects.
	 *
	 * @param <T>  the type of the copied root
	 * @param copy the copied root, not contained in any resource
	 * @param ids  the ids for the copied objects, ready for {@link XMLResourceIDs#applyIDs(Map, Resource)}
	 */
	public record IdentifiedCopy<T extends EObject>(T copy, Map<EObject, String> ids) {

		public IdentifiedCopy {
			requireNonNull(copy, "Copy must not be null");
			ids = Map.copyOf(ids);
		}
	}

	private XMLResourceIDs() {
	}

	/**
	 * Collects the ids of the given root and all its contained objects from the resources they
	 * are contained in.
	 *
	 * @param root the root of the tree, must not be {@code null}
	 * @return the ids by object in containment order; empty if the tree is not contained in an
	 *         {@link XMLResource} or carries no ids
	 */
	public static Map<EObject, String> collectIDs(EObject root) {
		requireNonNull(root, "Root must not be null");
		Map<EObject, String> ids = new LinkedHashMap<>();
		collectID(root, ids);
		for (TreeIterator<EObject> it = root.eAllContents(); it.hasNext();) {
			collectID(it.next(), ids);
		}
		return ids;
	}

	/**
	 * Applies the given ids to the target resource.
	 *
	 * @param ids    the ids by object, e.g. from {@link #collectIDs(EObject)}; must not be {@code null}
	 * @param target the resource the objects are contained in, must not be {@code null}
	 */
	public static void applyIDs(Map<EObject, String> ids, Resource target) {
		requireNonNull(ids, "IDs must not be null");
		requireNonNull(target, "Target resource must not be null");
		if (target instanceof XMLResource xmlResource) {
			ids.forEach(xmlResource::setID);
		}
	}

	/**
	 * Moves the given root into the contents of the target resource and keeps the ids of the root
	 * and all its contained objects.
	 *
	 * @param root   the root of the tree to move, must not be {@code null}
	 * @param target the resource to move the tree into, must not be {@code null}
	 */
	public static void moveWithIDs(EObject root, Resource target) {
		requireNonNull(target, "Target resource must not be null");
		Map<EObject, String> ids = collectIDs(root);
		target.getContents().add(root);
		applyIDs(ids, target);
	}

	/**
	 * Copies the given root with all contained objects and returns the copy together with the ids
	 * of the originals, keyed by the copied objects. The copy is not added to any resource; apply
	 * the ids with {@link #applyIDs(Map, Resource)} once it is placed.
	 *
	 * @param <T>  the type of the root
	 * @param root the root of the tree to copy, must not be {@code null}
	 * @return the copy and its ids, never {@code null}
	 */
	public static <T extends EObject> IdentifiedCopy<T> copyWithIDs(T root) {
		Map<EObject, String> originalIDs = collectIDs(root);
		Copier copier = new Copier();
		@SuppressWarnings("unchecked")
		T copy = (T) copier.copy(root);
		copier.copyReferences();
		Map<EObject, String> ids = new LinkedHashMap<>();
		originalIDs.forEach((original, id) -> ids.put(copier.get(original), id));
		return new IdentifiedCopy<>(copy, ids);
	}

	/**
	 * Copies the given root with all contained objects into the contents of the target resource
	 * and gives the copies the ids of the originals.
	 *
	 * @param <T>    the type of the root
	 * @param root   the root of the tree to copy, must not be {@code null}
	 * @param target the resource to add the copy to, must not be {@code null}
	 * @return the copied root, never {@code null}
	 */
	public static <T extends EObject> T copyWithIDs(T root, Resource target) {
		requireNonNull(target, "Target resource must not be null");
		IdentifiedCopy<T> identifiedCopy = copyWithIDs(root);
		target.getContents().add(identifiedCopy.copy());
		applyIDs(identifiedCopy.ids(), target);
		return identifiedCopy.copy();
	}

	private static void collectID(EObject eObject, Map<EObject, String> ids) {
		if (eObject.eResource() instanceof XMLResource xmlResource) {
			String id = xmlResource.getID(eObject);
			if (id != null) {
				ids.put(eObject, id);
			}
		}
	}
}
