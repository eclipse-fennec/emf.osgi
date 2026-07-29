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
package org.eclipse.fennec.emf.osgi.codegen.templates.model.helper;

import java.util.Collection;
import java.util.Map;

import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.emf.osgi.codegen.FennecEmfGenerator;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;

/**
 * A helper for some special OSGi specific dependency deduction
 * 
 * @author Juergen Albert
 * @since 29 Aug 2022
 */
public class GeneratorHelper {
	
	private GeneratorHelper() {
	}
	
	public static String getVersion(GenPackage genPackage) {
		EAnnotation versionAnnotation = genPackage.getEcorePackage().getEAnnotation("Version");
		if(versionAnnotation == null) {
			return "1.0";
		}
		String value = versionAnnotation.getDetails().get("value");
		if(value == null) {
			return "1.0";
		}
		return value; 
	}

	/**
	 * Computes the fingerprint of the {@code .ecore}-loaded package at build time, so the
	 * generated configurator can carry it as a constant instead of hashing on the bind
	 * path (decision M13b in {@code docs/metadata-migration.md}, issue #58). Sound because
	 * the equivalence gate proves {@code .ecore} and generated code yield the same value.
	 * <p>
	 * An unresolved proxy degrades the canonical form (the type key falls back to
	 * {@code "#null"}), which would burn a wrong identity into the generated code and into
	 * every downstream consumer of it. In that case <b>no</b> constant is emitted and the
	 * model keeps the runtime computation: a missing fingerprint is recoverable, a wrong
	 * one is not.
	 *
	 * @param genPackage the package being generated; must not be {@code null}
	 * @return the fingerprint, or {@code null} if it cannot be computed reliably
	 */
	public static String getFingerprint(GenPackage genPackage) {
		EPackage ePackage = genPackage.getEcorePackage();
		if (ePackage == null) {
			FennecEmfGenerator.warn("No Ecore package for " + genPackage.getPackageName()
					+ " - the generated configurator gets no fingerprint constant");
			return null;
		}
		Resource resource = ePackage.eResource();
		if (resource != null) {
			ResourceSet resourceSet = resource.getResourceSet();
			if (resourceSet != null) {
				EcoreUtil.resolveAll(resourceSet);
			} else {
				EcoreUtil.resolveAll(resource);
			}
		}
		Map<EObject, Collection<Setting>> unresolved = EcoreUtil.UnresolvedProxyCrossReferencer.find(ePackage);
		if (!unresolved.isEmpty()) {
			FennecEmfGenerator.warn("Model " + ePackage.getNsURI() + " has " + unresolved.size()
					+ " unresolved proxy target(s) - no fingerprint constant is emitted, because the value would"
					+ " be computed over a degraded canonical form. Unresolved: " + unresolved.keySet());
			return null;
		}
		return FingerprintHelper.fingerprint(ePackage);
	}

	/**
	 * This method assumes that the relative paths between the genmodel and the ecore will be similar in the resulting bundle.
	 * It thus determines a the ecore path relative to the bundleGenmodelPath.
	 *
	 * @param bundleGenModelPath
	 * @param originalEcoreUri
	 * @param originalGenModelUri
	 * @return the ecore URI in the resulting bundle
	 */
	public static URI convertToBundleEcoreURI(URI bundleGenModelPath, URI originalEcoreUri, URI originalGenModelUri) {
    	URI dummy = URI.createURI("resources://bla/");
    	URI genModelPathResolved = bundleGenModelPath.resolve(dummy);
    	URI finalEcore = originalEcoreUri.
    			deresolve(originalGenModelUri).
    			resolve(genModelPathResolved).
    			deresolve(dummy);
		return finalEcore;
	}

}
