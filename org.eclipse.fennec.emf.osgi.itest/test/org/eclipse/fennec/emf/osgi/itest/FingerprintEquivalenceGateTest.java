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
package org.eclipse.fennec.emf.osgi.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;
import org.eclipse.fennec.emf.osgi.example.model.basic.BasicPackage;
import org.eclipse.fennec.emf.osgi.example.model.extended.ExtendedPackage;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.junit.jupiter.api.Test;

/**
 * The equivalence gate (issue #57): the {@code .ecore} file and the code generated
 * from it are the same model version in two representations, so both must yield the
 * <b>same fingerprint</b>. This is what makes a build-time fingerprint constant (#58)
 * and a manifest capability (#59) sound — and it is required regardless, because the
 * runtime computes fingerprints for dynamically loaded models that coexist with
 * generated ones.
 * <p>
 * Failure rule (M14): a divergence caused by a <em>domain</em> annotation is a
 * generator gap — the {@code .ecore} is the semantic reference and the generator must
 * reproduce the annotation. A divergence caused by <em>tooling</em> configuration is an
 * fp1 ignorelist issue (after the freeze: a new scheme tag).
 * <p>
 * This is a permanent test, not a one-off check — it guards representation
 * independence for every model added later. Cross-ecore references are covered by the
 * extended model (supertypes/types from basic and Ecore); the loader asserts full proxy
 * resolution before comparing, because an unresolved proxy degrades the canonical form
 * (conservatively: false-different, never false-same).
 */
class FingerprintEquivalenceGateTest {

    // ---- generated example models (the codegen path) ---------------------------

    @Test
    void basicEcoreEqualsGeneratedPackage() {
        EPackage fromEcore = loadEcore(
                "../org.eclipse.fennec.emf.osgi.example.model.basic/other/main/resources/model/basic.ecore");
        assertEquals(FingerprintHelper.fingerprint(fromEcore), FingerprintHelper.fingerprint(BasicPackage.eINSTANCE),
                ".ecore and generated BasicPackage must be the same model version");
    }

    @Test
    void extendedEcoreEqualsGeneratedPackage() {
        // extended carries ExtendedMetaData and a custom Version annotation — the
        // annotation-reproduction case of the gate.
        EPackage fromEcore = loadEcore(
                "../org.eclipse.fennec.emf.osgi.example.model.extended/model/extended.ecore");
        assertEquals(FingerprintHelper.fingerprint(fromEcore), FingerprintHelper.fingerprint(ExtendedPackage.eINSTANCE),
                ".ecore and generated ExtendedPackage must be the same model version");
    }

    // ---- EMF's own models (the EMF generator, GenModel annotations in the wild) --

    @Test
    void ecoreItselfEqualsItsShippedEcoreFile() {
        EPackage fromEcore = loadEcoreFromClasspath("model/Ecore.ecore");
        assertEquals(FingerprintHelper.fingerprint(fromEcore), FingerprintHelper.fingerprint(EcorePackage.eINSTANCE),
                "Ecore.ecore (shipped in the EMF jar, incl. documentation annotations) and EcorePackage.eINSTANCE"
                        + " must be the same model version");
    }

    @Test
    void xmlTypeEqualsItsShippedEcoreFile() {
        // XMLType is dense with ExtendedMetaData annotations — the strongest
        // behavioural-annotation case available in compiled form.
        EPackage fromEcore = loadEcoreFromClasspath("model/XMLType.ecore");
        assertEquals(FingerprintHelper.fingerprint(fromEcore), FingerprintHelper.fingerprint(XMLTypePackage.eINSTANCE),
                "XMLType.ecore and XMLTypePackage.eINSTANCE must be the same model version");
    }

    // ---- helpers ----------------------------------------------------------------

    private static EPackage loadEcore(String relativePath) {
        File file = new File(relativePath);
        assertTrue(file.isFile(), "corpus file missing: " + file.getAbsolutePath());
        return loadEcore(URI.createFileURI(file.getAbsolutePath()));
    }

    private static EPackage loadEcoreFromClasspath(String resource) {
        URL url = EcorePackage.class.getClassLoader().getResource(resource);
        assertTrue(url != null, "classpath resource missing: " + resource);
        return loadEcore(URI.createURI(url.toString()));
    }

    private static EPackage loadEcore(URI uri) {
        ResourceSet resourceSet = new ResourceSetImpl();
        // The example .ecore files reference other models with Eclipse-workspace-relative
        // paths (e.g. ../../org.eclipse.emf.ecore/model/Ecore.ecore#//EClass) that the
        // codegen resolves through its ResourceUriHandler. Replicate that resolution
        // here by suffix: without it the proxies stay unresolved and degrade to "#null" —
        // which is exactly the divergence this gate exists to catch.
        resourceSet.setURIConverter(new ExtensibleURIConverterImpl() {
            @Override
            public URI normalize(URI toNormalize) {
                String value = toNormalize.trimFragment().toString();
                if (value.endsWith("org.eclipse.emf.ecore/model/Ecore.ecore")) {
                    return URI.createURI(
                            EcorePackage.class.getClassLoader().getResource("model/Ecore.ecore").toString());
                }
                int marker = value.indexOf("org.eclipse.fennec.emf.osgi.example.model.");
                if (marker >= 0 && !toNormalize.isFile()) {
                    return URI.createFileURI(new File("../" + value.substring(marker)).getAbsolutePath());
                }
                return super.normalize(toNormalize);
            }
        });
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",
                new EcoreResourceFactoryImpl());
        Resource resource = resourceSet.getResource(uri, true);
        EPackage ePackage = (EPackage) resource.getContents().get(0);
        EcoreUtil.resolveAll(resourceSet);
        assertTrue(EcoreUtil.UnresolvedProxyCrossReferencer.find(resource).isEmpty(),
                "corpus model has unresolved proxies — the gate would compare a degraded form: " + uri);
        return ePackage;
    }
}
