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
package org.eclipse.fennec.emf.osgi.components.fingerprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryRegistryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The fingerprint of a package with cross-package references must not depend on whether
 * those references were resolvable when the value was computed (model.atlas#251: a
 * package fingerprinted on upload — in a ResourceSet that knew the referenced packages —
 * and again on replay after a restart — in one that did not — produced two values, and
 * the drift warning fired on every restart).
 * <p>
 * Type references enter the canonical form as {@code nsURI#Name} keys only. A resolved
 * classifier yields the key from its package and name; a proxy carries neither, so the
 * key is derived from its proxy URI. For a reference addressed by nsURI both roads lead
 * to the same key.
 *
 * @author Mark Hoffmann
 */
class Fp1ProxyResolutionIndependenceTest {

    private static final String NS_A = "http://test/a";
    private static final String NS_B = "http://test/b";

    private final FingerprintService service = new DefaultFingerprintService();

    // ---- fixtures ------------------------------------------------------------

    /** Package A: {@code Thing} with an attribute and a reference back to {@code Holder}. */
    private static EPackage packageA() {
        EPackage a = EcoreFactory.eINSTANCE.createEPackage();
        a.setName("a");
        a.setNsURI(NS_A);
        a.setNsPrefix("a");
        EClass thing = EcoreFactory.eINSTANCE.createEClass();
        thing.setName("Thing");
        a.getEClassifiers().add(thing);
        EAttribute label = EcoreFactory.eINSTANCE.createEAttribute();
        label.setName("label");
        label.setEType(EcorePackage.Literals.ESTRING);
        thing.getEStructuralFeatures().add(label);
        EReference holder = EcoreFactory.eINSTANCE.createEReference();
        holder.setName("holder");
        thing.getEStructuralFeatures().add(holder);
        EClass problem = EcoreFactory.eINSTANCE.createEClass();
        problem.setName("Problem");
        a.getEClassifiers().add(problem);
        return a;
    }

    /**
     * Package B, referencing A in every position the canonical form emits a type key:
     * feature type, opposite, supertype, operation return type, parameter type and
     * exception. {@code thingClass} and {@code holderOpposite} are either A's real
     * elements or proxies to them.
     */
    private static EPackage packageB(EClass thingClass, EClass problemClass, EReference holderOpposite) {
        EPackage b = EcoreFactory.eINSTANCE.createEPackage();
        b.setName("b");
        b.setNsURI(NS_B);
        b.setNsPrefix("b");

        EClass holder = EcoreFactory.eINSTANCE.createEClass();
        holder.setName("Holder");
        holder.getESuperTypes().add(thingClass);
        b.getEClassifiers().add(holder);

        EReference thing = EcoreFactory.eINSTANCE.createEReference();
        thing.setName("thing");
        thing.setEType(thingClass);
        thing.setEOpposite(holderOpposite);
        holder.getEStructuralFeatures().add(thing);

        EOperation find = EcoreFactory.eINSTANCE.createEOperation();
        find.setName("find");
        find.setEType(thingClass);
        EParameter template = EcoreFactory.eINSTANCE.createEParameter();
        template.setName("template");
        template.setEType(thingClass);
        find.getEParameters().add(template);
        find.getEExceptions().add(problemClass);
        holder.getEOperations().add(find);
        return b;
    }

    private static EClass proxyClass(String uri) {
        EClass proxy = EcoreFactory.eINSTANCE.createEClass();
        ((InternalEObject) proxy).eSetProxyURI(URI.createURI(uri));
        return proxy;
    }

    private static EReference proxyReference(String uri) {
        EReference proxy = EcoreFactory.eINSTANCE.createEReference();
        ((InternalEObject) proxy).eSetProxyURI(URI.createURI(uri));
        return proxy;
    }

    /** B with every cross-package reference resolved to A's real elements. */
    private static EPackage resolvedB() {
        EPackage a = packageA();
        EClass thing = (EClass) a.getEClassifier("Thing");
        EClass problem = (EClass) a.getEClassifier("Problem");
        EReference holder = (EReference) thing.getEStructuralFeature("holder");
        return packageB(thing, problem, holder);
    }

    /** B with every cross-package reference left as an nsURI-addressed proxy. */
    private static EPackage unresolvedB() {
        return packageB(proxyClass(NS_A + "#//Thing"), proxyClass(NS_A + "#//Problem"),
                proxyReference(NS_A + "#//Thing/holder"));
    }

    /** The proxies must still be proxies after fingerprinting — otherwise the test is vacuous. */
    private static void assertStillUnresolved(EPackage b) {
        EClass holder = (EClass) b.getEClassifier("Holder");
        EReference thing = (EReference) holder.getEStructuralFeature("thing");
        assertTrue(thing.getEType().eIsProxy(), "feature type should have stayed a proxy");
        assertTrue(holder.getESuperTypes().get(0).eIsProxy(), "supertype should have stayed a proxy");
    }

    // ---- acceptance ----------------------------------------------------------

    @Test
    void nsUriProxiesHashLikeTheResolvedReferences() {
        EPackage resolved = resolvedB();
        EPackage unresolved = unresolvedB();

        String fpResolved = service.fingerprint(resolved);
        String fpUnresolved = service.fingerprint(unresolved);

        assertStillUnresolved(unresolved);
        assertEquals(fpResolved, fpUnresolved,
                "a cross-package reference addressed by nsURI must hash the same resolved and unresolved");
    }

    @Test
    void differentProxyTargetsStayDistinguishable() {
        // Before the fix every proxy collapsed to "#null": two packages differing only in
        // WHICH foreign type they reference were indistinguishable while unresolved.
        EPackage toThing = packageB(proxyClass(NS_A + "#//Thing"), proxyClass(NS_A + "#//Problem"),
                proxyReference(NS_A + "#//Thing/holder"));
        EPackage toOther = packageB(proxyClass(NS_A + "#//Other"), proxyClass(NS_A + "#//Problem"),
                proxyReference(NS_A + "#//Other/holder"));

        assertNotEquals(service.fingerprint(toThing), service.fingerprint(toOther),
                "proxies to different targets must yield different fingerprints");
    }

    @Test
    void proxiesAddressedByDocumentLocationAreDeterministicButNotUnified() {
        // A reference by relative file path cannot be mapped onto the nsURI without loading
        // the target; it stays distinct from the resolved value but must be reproducible.
        EPackage first = packageB(proxyClass("../a/model/a.ecore#//Thing"),
                proxyClass("../a/model/a.ecore#//Problem"), proxyReference("../a/model/a.ecore#//Thing/holder"));
        EPackage second = packageB(proxyClass("../a/model/a.ecore#//Thing"),
                proxyClass("../a/model/a.ecore#//Problem"), proxyReference("../a/model/a.ecore#//Thing/holder"));

        assertEquals(service.fingerprint(first), service.fingerprint(second));
        assertNotEquals(service.fingerprint(first), service.fingerprint(resolvedB()),
                "a location-addressed proxy is not the same key as the resolved nsURI key");
    }

    @Test
    void nonPathFragmentsAreEmittedVerbatimNotAsNull() {
        EPackage idFragment = packageB(proxyClass(NS_A + "#thing-id"), proxyClass(NS_A + "#//Problem"),
                proxyReference(NS_A + "#//Thing/holder"));
        EPackage otherId = packageB(proxyClass(NS_A + "#other-id"), proxyClass(NS_A + "#//Problem"),
                proxyReference(NS_A + "#//Thing/holder"));

        assertNotEquals(service.fingerprint(idFragment), service.fingerprint(otherId),
                "distinct ID fragments must not collapse to one key");
    }

    /**
     * The model.atlas#251 shape end to end: B is serialized while A is resolvable and
     * loaded again into a ResourceSet that knows nothing about A. The fingerprint of
     * the package as it stands in memory before saving must equal the one computed on
     * the reloaded, unresolved copy.
     */
    @Test
    void reloadedWithoutTheTargetPackageHashesLikeTheOriginal(@TempDir Path dir) throws IOException {
        EPackage a = packageA();
        EClass thing = (EClass) a.getEClassifier("Thing");
        EClass problem = (EClass) a.getEClassifier("Problem");
        EReference holder = (EReference) thing.getEStructuralFeature("holder");
        EPackage b = packageB(thing, problem, holder);

        // Upload-side context: A is addressable under its nsURI, so B's references are
        // serialized as nsURI hrefs — the published-schema rule.
        ResourceSet writing = newResourceSet();
        Resource aResource = new EcoreResourceFactoryImpl().createResource(URI.createURI(NS_A));
        writing.getResources().add(aResource);
        aResource.getContents().add(a);
        URI bUri = URI.createFileURI(dir.resolve("b.ecore").toString());
        Resource bResource = writing.createResource(bUri);
        bResource.getContents().add(b);
        String fpOriginal = service.fingerprint(b);
        bResource.save(Map.of());

        // Replay-side context: a ResourceSet that cannot resolve A.
        ResourceSet reading = newResourceSet();
        EPackage reloaded = (EPackage) reading.getResource(bUri, true).getContents().get(0);
        String fpReloaded = service.fingerprint(reloaded);

        assertStillUnresolved(reloaded);
        assertEquals(fpOriginal, fpReloaded,
                "the same package must fingerprint identically whether or not A is resolvable");
        assertFalse(fpReloaded.isBlank());
    }

    /**
     * A ResourceSet with an ecore factory only: a demand-load of {@code http://test/a}
     * finds no factory and fails locally instead of attempting a network fetch.
     */
    private static ResourceSet newResourceSet() {
        ResourceSetImpl rs = new ResourceSetImpl();
        ResourceFactoryRegistryImpl factories = new ResourceFactoryRegistryImpl();
        factories.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.setResourceFactoryRegistry(factories);
        return rs;
    }
}
