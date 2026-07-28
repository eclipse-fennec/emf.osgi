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

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.junit.jupiter.api.Test;

/**
 * Byte-identity regression for the {@code fp1} canonicalization scheme (issue #17, §2).
 * <p>
 * The golden values below were captured from the pre-refactoring implementation — the
 * single hard-wired algorithm in {@code DefaultFingerprintService} before the
 * canonicalization was extracted behind a scheme-addressed abstraction. They pin the
 * scheme against unintended drift and prove the extraction independently of the
 * generics work: <b>a model without generics must hash exactly as it did before.</b>
 * <p>
 * If a change here fails, it changed the meaning of {@code fp1} for models that carry
 * no generics at all — which is a scheme bump, not a refactoring.
 */
class Fp1CanonicalFormRegressionTest {

    private static final String GENMODEL = "http://www.eclipse.org/emf/2002/GenModel";
    private static final String NS_URI = "http://example.org/regression/1.0";

    private static final String GOLDEN_KITCHEN_SINK =
            "fp1:53769d1840e67107fdc4f52518b5101b4d9403a0e20986f998f722e64119603f";
    private static final String GOLDEN_KITCHEN_SINK_WITH_INPUTS =
            "fp1:62296e61a5d078367e64ef3bf2f03927c4047925f357ec165a0f27de8f0caf92";
    private static final String GOLDEN_EMPTY =
            "fp1:d4b5f2961adf15a231c85cf6d41d38737538cccd507dd5181753d1dfbd506fcc";

    private final FingerprintService service = new DefaultFingerprintService();

    /**
     * A generics-free package exercising every branch of the v1 canonical form:
     * abstract/interface classes, two supertypes in declared order, ID attribute with
     * default literal, bidirectional containment reference with opposite, operation with
     * bounds/exceptions/parameters, enum with literals, custom data type, annotations
     * (multi-source, multi-detail, documentation-only).
     */
    private static EPackage kitchenSink() {
        EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
        pkg.setName("regression");
        pkg.setNsPrefix("reg");
        pkg.setNsURI(NS_URI);

        // --- data type ---
        EDataType money = EcoreFactory.eINSTANCE.createEDataType();
        money.setName("Money");
        money.setInstanceClassName("java.math.BigDecimal");
        money.setSerializable(true);
        pkg.getEClassifiers().add(money);

        // --- enum ---
        EEnum status = EcoreFactory.eINSTANCE.createEEnum();
        status.setName("Status");
        pkg.getEClassifiers().add(status);
        status.getELiterals().add(literal("DRAFT", 0, "draft"));
        status.getELiterals().add(literal("APPROVED", 1, "approved"));

        // --- exception classes (unsorted on purpose: the canonical form sorts them) ---
        EClass validationError = EcoreFactory.eINSTANCE.createEClass();
        validationError.setName("ValidationError");
        pkg.getEClassifiers().add(validationError);
        EClass accessError = EcoreFactory.eINSTANCE.createEClass();
        accessError.setName("AccessError");
        pkg.getEClassifiers().add(accessError);

        // --- interface + abstract base, both supertypes of Person ---
        EClass identifiable = EcoreFactory.eINSTANCE.createEClass();
        identifiable.setName("Identifiable");
        identifiable.setAbstract(true);
        identifiable.setInterface(true);
        pkg.getEClassifiers().add(identifiable);

        EClass base = EcoreFactory.eINSTANCE.createEClass();
        base.setName("Base");
        base.setAbstract(true);
        pkg.getEClassifiers().add(base);

        EClass person = EcoreFactory.eINSTANCE.createEClass();
        person.setName("Person");
        person.getESuperTypes().add(base);          // declared order is meaningful
        person.getESuperTypes().add(identifiable);
        pkg.getEClassifiers().add(person);

        EClass address = EcoreFactory.eINSTANCE.createEClass();
        address.setName("Address");
        pkg.getEClassifiers().add(address);

        // --- attributes ---
        EAttribute id = EcoreFactory.eINSTANCE.createEAttribute();
        id.setName("id");
        id.setEType(EcorePackage.Literals.ESTRING);
        id.setID(true);
        person.getEStructuralFeatures().add(id);

        EAttribute salary = EcoreFactory.eINSTANCE.createEAttribute();
        salary.setName("salary");
        salary.setEType(money);
        salary.setDefaultValueLiteral("0.00");
        salary.setUnique(false);
        salary.setOrdered(false);
        salary.setLowerBound(0);
        salary.setUpperBound(1);
        salary.setChangeable(true);
        salary.setDerived(false);
        salary.setTransient(false);
        person.getEStructuralFeatures().add(salary);

        EAttribute state = EcoreFactory.eINSTANCE.createEAttribute();
        state.setName("state");
        state.setEType(status);
        person.getEStructuralFeatures().add(state);

        EAttribute tags = EcoreFactory.eINSTANCE.createEAttribute();
        tags.setName("tags");
        tags.setEType(EcorePackage.Literals.ESTRING);
        tags.setUpperBound(-1);
        tags.setDerived(true);
        tags.setTransient(true);
        tags.setChangeable(false);
        person.getEStructuralFeatures().add(tags);

        EAttribute street = EcoreFactory.eINSTANCE.createEAttribute();
        street.setName("street");
        street.setEType(EcorePackage.Literals.ESTRING);
        address.getEStructuralFeatures().add(street);

        // --- bidirectional containment reference ---
        EReference addresses = EcoreFactory.eINSTANCE.createEReference();
        addresses.setName("addresses");
        addresses.setEType(address);
        addresses.setContainment(true);
        addresses.setUpperBound(-1);
        person.getEStructuralFeatures().add(addresses);

        EReference owner = EcoreFactory.eINSTANCE.createEReference();
        owner.setName("owner");
        owner.setEType(person);
        owner.setLowerBound(1);
        address.getEStructuralFeatures().add(owner);

        addresses.setEOpposite(owner);
        owner.setEOpposite(addresses);

        // --- operation with exceptions and parameters ---
        EOperation rename = EcoreFactory.eINSTANCE.createEOperation();
        rename.setName("rename");
        rename.setEType(EcorePackage.Literals.EBOOLEAN);
        rename.setLowerBound(1);
        rename.setUpperBound(1);
        rename.getEExceptions().add(validationError); // added unsorted; canonical form sorts
        rename.getEExceptions().add(accessError);
        EParameter newName = EcoreFactory.eINSTANCE.createEParameter();
        newName.setName("newName");
        newName.setEType(EcorePackage.Literals.ESTRING);
        newName.setLowerBound(1);
        rename.getEParameters().add(newName);
        EParameter locales = EcoreFactory.eINSTANCE.createEParameter();
        locales.setName("locales");
        locales.setEType(EcorePackage.Literals.ESTRING);
        locales.setUpperBound(-1);
        locales.setOrdered(false);
        locales.setUnique(false);
        rename.getEParameters().add(locales);
        person.getEOperations().add(rename);

        // --- annotations: sources out of order, details out of order, plus documentation ---
        EAnnotation ormAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        ormAnnotation.setSource("http://example.org/orm");
        ormAnnotation.getDetails().put("table", "PERSON");
        ormAnnotation.getDetails().put("schema", "PUBLIC");
        person.getEAnnotations().add(ormAnnotation);

        EAnnotation docAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        docAnnotation.setSource(GENMODEL);
        docAnnotation.getDetails().put("documentation", "Must not influence the hash.");
        person.getEAnnotations().add(docAnnotation);

        EAnnotation constraintAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        constraintAnnotation.setSource("http://example.org/constraint");
        constraintAnnotation.getDetails().put("min", "0");
        salary.getEAnnotations().add(constraintAnnotation);

        EAnnotation opAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        opAnnotation.setSource("http://example.org/audit");
        opAnnotation.getDetails().put("logged", "true");
        rename.getEAnnotations().add(opAnnotation);

        EAnnotation enumAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        enumAnnotation.setSource("http://example.org/ui");
        enumAnnotation.getDetails().put("widget", "combo");
        status.getEAnnotations().add(enumAnnotation);

        EAnnotation typeAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        typeAnnotation.setSource("http://example.org/ui");
        typeAnnotation.getDetails().put("format", "#,##0.00");
        money.getEAnnotations().add(typeAnnotation);

        return pkg;
    }

    private static EEnumLiteral literal(String name, int value, String text) {
        EEnumLiteral l = EcoreFactory.eINSTANCE.createEEnumLiteral();
        l.setName(name);
        l.setValue(value);
        l.setLiteral(text);
        return l;
    }

    /** A deliberately minimal package: only the nsURI, no classifiers at all. */
    private static EPackage empty() {
        EPackage pkg = EcoreFactory.eINSTANCE.createEPackage();
        pkg.setName("empty");
        pkg.setNsPrefix("empty");
        pkg.setNsURI("http://example.org/empty/1.0");
        return pkg;
    }

    // ---- golden values, captured before the scheme abstraction was extracted --------

    @Test
    void kitchenSinkHashIsUnchanged() {
        assertEquals(GOLDEN_KITCHEN_SINK, service.fingerprint(kitchenSink()),
                "fp1 must stay byte-identical for a generics-free model");
    }

    @Test
    void kitchenSinkWithDerivationInputsHashIsUnchanged() {
        assertEquals(GOLDEN_KITCHEN_SINK_WITH_INPUTS,
                service.fingerprint(kitchenSink(), "oclEngine=1.2.0", "eorm=abc"),
                "folding derivation inputs must stay byte-identical");
    }

    @Test
    void emptyPackageHashIsUnchanged() {
        assertEquals(GOLDEN_EMPTY, service.fingerprint(empty()),
                "the degenerate case must stay byte-identical too");
    }

    @Test
    void goldenValuesAreReachedThroughTheSchemeRegistryAsWell() {
        // The scheme-addressed entry point must compute the very same value as the
        // convenience method that uses the current scheme implicitly.
        assertEquals(GOLDEN_KITCHEN_SINK, service.fingerprintInScheme("fp1", kitchenSink()));
        assertEquals(GOLDEN_KITCHEN_SINK_WITH_INPUTS,
                service.fingerprintInScheme("fp1", kitchenSink(), "oclEngine=1.2.0", "eorm=abc"));
    }
}
