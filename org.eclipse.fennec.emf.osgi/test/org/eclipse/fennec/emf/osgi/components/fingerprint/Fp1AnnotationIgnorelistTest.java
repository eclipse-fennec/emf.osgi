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

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.junit.jupiter.api.Test;

/**
 * The fp1 annotation ignorelist (issue #52): representation independence requires that
 * tooling-configuration annotations — which exist in the {@code .ecore} but configure
 * only how the Java artifact is generated — never affect the fingerprint, while every
 * annotation with behavioural force (serialization, validation, mapping, delegates)
 * always does.
 * <p>
 * Positive tests prove the ignoring, negative tests prove that everything else still
 * counts. Golden-value neutrality is proven separately by
 * {@link Fp1CanonicalFormRegressionTest}.
 */
class Fp1AnnotationIgnorelistTest {

    private static final String GENMODEL = "http://www.eclipse.org/emf/2002/GenModel";
    private static final String UML2_GENMODEL = "http://www.eclipse.org/uml2/2.2.0/GenModel";
    private static final String UML2_UML = "http://www.eclipse.org/uml2/2.0.0/UML";
    private static final String EXTENDED_METADATA = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData";
    private static final String ORM = "http://example.org/orm";

    private final FingerprintService service = new DefaultFingerprintService();

    // ---- fixtures --------------------------------------------------------------

    /** A minimal package: one class {@code Person} with one attribute {@code name}. */
    private static EPackage base() {
        EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
        pkg.setName("ignorelist");
        pkg.setNsPrefix("ign");
        pkg.setNsURI("http://example.org/ignorelist/1.0");

        EClass person = EcoreFactory.eINSTANCE.createEClass();
        person.setName("Person");
        pkg.getEClassifiers().add(person);

        EAttribute name = EcoreFactory.eINSTANCE.createEAttribute();
        name.setName("name");
        name.setEType(EcorePackage.Literals.ESTRING);
        person.getEStructuralFeatures().add(name);
        return pkg;
    }

    private static EClass person(EPackage pkg) {
        return (EClass) pkg.getEClassifier("Person");
    }

    /** Attaches an annotation with alternating key/value detail pairs to an element. */
    private static EAnnotation annotate(EClass target, String source, String... keyValues) {
        EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
        annotation.setSource(source);
        for (int i = 0; i < keyValues.length; i += 2) {
            annotation.getDetails().put(keyValues[i], keyValues[i + 1]);
        }
        target.getEAnnotations().add(annotation);
        return annotation;
    }

    // ---- positive: tooling configuration is ignored ----------------------------

    @Test
    void genModelBuildConfigurationDoesNotAffectTheFingerprint() {
        EPackage plain = base();
        EPackage annotated = base();
        annotate(person(annotated), GENMODEL,
                "basePackage", "com.foo", "complianceLevel", "17.0", "oSGiCompatible", "true");
        assertEquals(service.fingerprint(plain), service.fingerprint(annotated),
                "GenModel build configuration must NOT change the fingerprint");
    }

    @Test
    void changingGenModelBuildConfigurationDoesNotAffectTheFingerprint() {
        EPackage a = base();
        annotate(person(a), GENMODEL, "basePackage", "com.foo", "complianceLevel", "17.0");
        EPackage b = base();
        annotate(person(b), GENMODEL, "basePackage", "com.bar", "complianceLevel", "21.0");
        assertEquals(service.fingerprint(a), service.fingerprint(b),
                "a base-package rename or compliance bump is the same model version");
    }

    @Test
    void uml2GenModelSourceIsIgnoredToo() {
        EPackage plain = base();
        EPackage annotated = base();
        annotate(person(annotated), UML2_GENMODEL, "originalName", "Person");
        assertEquals(service.fingerprint(plain), service.fingerprint(annotated),
                "the UML2 GenModel namespace is tooling configuration as well");
    }

    @Test
    void documentationKeyIsIgnoredUnderEverySource() {
        EPackage plain = base();
        EPackage annotated = base();
        annotate(person(annotated), ORM, "documentation", "maps a person");
        assertEquals(service.fingerprint(plain), service.fingerprint(annotated),
                "a documentation-only annotation is invisible regardless of its source");
    }

    @Test
    void ignoredSourceDoesNotAppearInTheCanonicalForm() {
        EPackage annotated = base();
        annotate(person(annotated), GENMODEL, "basePackage", "com.foo");
        String form = ((DefaultFingerprintService) service)
                .canonicalForm(Fp1CanonicalizationScheme.TAG, annotated);
        assertFalse(form.contains(GENMODEL),
                "an ignored source must leave no trace in the canonical form");
    }

    // ---- negative: behavioural annotations still count -------------------------

    @Test
    void extendedMetaDataChangesTheFingerprint() {
        EPackage element = base();
        annotate(person(element), EXTENDED_METADATA, "kind", "element");
        EPackage attribute = base();
        annotate(person(attribute), EXTENDED_METADATA, "kind", "attribute");
        assertNotEquals(service.fingerprint(element), service.fingerprint(attribute),
                "ExtendedMetaData determines the XML wire format and MUST change the fingerprint");
        assertNotEquals(service.fingerprint(base()), service.fingerprint(element),
                "adding ExtendedMetaData MUST change the fingerprint");
    }

    @Test
    void domainAnnotationChangesTheFingerprint() {
        EPackage plain = base();
        EPackage mapped = base();
        annotate(person(mapped), ORM, "table", "PERSON");
        assertNotEquals(service.fingerprint(plain), service.fingerprint(mapped),
                "an ORM mapping changes what the model means and MUST change the fingerprint");

        EPackage remapped = base();
        annotate(person(remapped), ORM, "table", "PERSONS");
        assertNotEquals(service.fingerprint(mapped), service.fingerprint(remapped),
                "a changed mapping value MUST change the fingerprint");
    }

    @Test
    void unlistedSourceStillCounts() {
        EPackage plain = base();
        EPackage annotated = base();
        annotate(person(annotated), UML2_UML, "originalName", "Person");
        assertNotEquals(service.fingerprint(plain), service.fingerprint(annotated),
                "only the listed GenModel namespaces are ignored — unknown sources are domain content");
    }

    @Test
    void mixedAnnotationKeepsItsNonDocumentationDetails() {
        EPackage withDoc = base();
        annotate(person(withDoc), ORM, "documentation", "maps a person", "table", "PERSON");
        EPackage withoutDoc = base();
        annotate(person(withoutDoc), ORM, "table", "PERSON");
        assertEquals(service.fingerprint(withDoc), service.fingerprint(withoutDoc),
                "the documentation detail is dropped, the rest of the annotation is kept");
        assertNotEquals(service.fingerprint(base()), service.fingerprint(withDoc),
                "the surviving detail still changes the fingerprint");
    }
}
