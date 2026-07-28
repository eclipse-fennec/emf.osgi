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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.junit.jupiter.api.Test;

/**
 * Generics coverage of the canonical form (issue #17, §1).
 * <p>
 * Before this was fixed, {@code EGenericType} and {@code ETypeParameter} appeared
 * nowhere in the canonicalization, so two {@link EPackage}s differing <em>solely</em> in
 * type parameters or type arguments hashed identically. Because registration is keyed by
 * fingerprint (#15), that collapsed two semantically different model versions into one:
 * the second {@link EPackage} instance was discarded and its objects would have been
 * served the first version's metadata — exactly what
 * {@code MetadataServiceImpl.registerPackage} documents as "never".
 * <p>
 * Note on type-parameter <b>names</b>: they are part of the canonical form, consistent
 * with v1's treatment of every other name. An alpha-rename therefore yields a new model
 * version. That is deliberately the conservative direction — a false "same" serves one
 * version's objects with another's metadata, while a false "different" only costs
 * precision (a second coexisting entry).
 *
 * @see Fp1CanonicalFormRegressionTest for the byte-identity guarantee on models without
 *      generics
 */
class DefaultFingerprintServiceGenericsTest {

    private static final String NS_URI = "http://example.org/generics/1.0";

    private final FingerprintService service = new DefaultFingerprintService();

    // ---- builders ------------------------------------------------------------

    private static EPackage pkg() {
        EPackage p = EcoreFactory.eINSTANCE.createEPackage();
        p.setName("generics");
        p.setNsPrefix("gen");
        p.setNsURI(NS_URI);
        return p;
    }

    private static EClass cls(EPackage p, String name) {
        EClass c = EcoreFactory.eINSTANCE.createEClass();
        c.setName(name);
        p.getEClassifiers().add(c);
        return c;
    }

    private static EGenericType generic(EClassifier classifier) {
        EGenericType g = EcoreFactory.eINSTANCE.createEGenericType();
        g.setEClassifier(classifier);
        return g;
    }

    private static EGenericType generic(ETypeParameter parameter) {
        EGenericType g = EcoreFactory.eINSTANCE.createEGenericType();
        g.setETypeParameter(parameter);
        return g;
    }

    /** {@code class Box<P> { P value }} — the type parameter's name is the variable. */
    private static EPackage boxWithTypeParameterNamed(String parameterName) {
        EPackage p = pkg();
        EClass box = cls(p, "Box");
        ETypeParameter parameter = EcoreFactory.eINSTANCE.createETypeParameter();
        parameter.setName(parameterName);
        box.getETypeParameters().add(parameter);

        EAttribute value = EcoreFactory.eINSTANCE.createEAttribute();
        value.setName("value");
        value.setEGenericType(generic(parameter));
        box.getEStructuralFeatures().add(value);
        return p;
    }

    /** {@code class Box<T extends bound> { T value }} — {@code null} bound = unbounded. */
    private static EPackage boxWithBound(EClassifier bound) {
        EPackage p = pkg();
        EClass box = cls(p, "Box");
        ETypeParameter parameter = EcoreFactory.eINSTANCE.createETypeParameter();
        parameter.setName("T");
        if (bound != null) {
            parameter.getEBounds().add(generic(bound));
        }
        box.getETypeParameters().add(parameter);

        EAttribute value = EcoreFactory.eINSTANCE.createEAttribute();
        value.setName("value");
        value.setEGenericType(generic(parameter));
        box.getEStructuralFeatures().add(value);
        return p;
    }

    /**
     * {@code class Box<T>{T value}  class Person  class Address  class Repo { Box<arg> box }}
     * <p>
     * The reference's {@code eType} is {@code Box} in every variant — only the type
     * <em>argument</em> differs, which is precisely what the v1 form could not see.
     */
    private static EPackage repoHoldingBoxOf(String argumentClassName) {
        EPackage p = boxWithTypeParameterNamed("T");
        EClass box = (EClass) p.getEClassifier("Box");
        EClass person = cls(p, "Person");
        EClass address = cls(p, "Address");
        EClass repo = cls(p, "Repo");

        EGenericType boxOfArgument = generic(box);
        boxOfArgument.getETypeArguments()
                .add(generic("Person".equals(argumentClassName) ? person : address));

        EReference boxRef = EcoreFactory.eINSTANCE.createEReference();
        boxRef.setName("box");
        boxRef.setContainment(true);
        boxRef.setEGenericType(boxOfArgument);
        repo.getEStructuralFeatures().add(boxRef);
        return p;
    }

    /** {@code class StringBox extends Box<arg>} — supertype identity is Box in both. */
    private static EPackage subclassOfBoxOf(EClassifier argument) {
        EPackage p = boxWithTypeParameterNamed("T");
        EClass box = (EClass) p.getEClassifier("Box");
        EClass concrete = cls(p, "ConcreteBox");

        EGenericType boxOfArgument = generic(box);
        boxOfArgument.getETypeArguments().add(generic(argument));
        concrete.getEGenericSuperTypes().add(boxOfArgument);
        return p;
    }

    /** {@code class Repo { Box<?> box }} vs. {@code Box<? extends upper>}. */
    private static EPackage repoHoldingBoxOfWildcard(EClassifier upperBound) {
        EPackage p = boxWithTypeParameterNamed("T");
        EClass box = (EClass) p.getEClassifier("Box");
        EClass repo = cls(p, "Repo");

        EGenericType wildcard = EcoreFactory.eINSTANCE.createEGenericType();
        if (upperBound != null) {
            wildcard.setEUpperBound(generic(upperBound));
        }
        EGenericType boxOfWildcard = generic(box);
        boxOfWildcard.getETypeArguments().add(wildcard);

        EReference boxRef = EcoreFactory.eINSTANCE.createEReference();
        boxRef.setName("box");
        boxRef.setContainment(true);
        boxRef.setEGenericType(boxOfWildcard);
        repo.getEStructuralFeatures().add(boxRef);
        return p;
    }

    /** {@code class Service { <P> P identity(P in) }} — operation-level type parameter. */
    private static EPackage serviceWithOperationTypeParameter(String parameterName) {
        EPackage p = pkg();
        EClass service = cls(p, "Service");

        EOperation identity = EcoreFactory.eINSTANCE.createEOperation();
        identity.setName("identity");
        ETypeParameter parameter = EcoreFactory.eINSTANCE.createETypeParameter();
        parameter.setName(parameterName);
        identity.getETypeParameters().add(parameter);
        identity.setEGenericType(generic(parameter));

        EParameter in = EcoreFactory.eINSTANCE.createEParameter();
        in.setName("in");
        in.setEGenericType(generic(parameter));
        identity.getEParameters().add(in);
        service.getEOperations().add(identity);
        return p;
    }

    // ---- reproducibility still holds in the presence of generics ---------------

    @Test
    void genericModelIsReproducible() {
        assertEquals(service.fingerprint(repoHoldingBoxOf("Person")),
                service.fingerprint(repoHoldingBoxOf("Person")),
                "identical generic content must still hash identically");
        assertEquals(service.fingerprint(boxWithBound(EcorePackage.Literals.ESTRING)),
                service.fingerprint(boxWithBound(EcorePackage.Literals.ESTRING)));
        assertEquals(service.fingerprint(serviceWithOperationTypeParameter("T")),
                service.fingerprint(serviceWithOperationTypeParameter("T")));
    }

    // ---- the identifying property, per generics construct ----------------------

    @Test
    void typeParameterNameIsPartOfTheKey() {
        assertNotEquals(service.fingerprint(boxWithTypeParameterNamed("T")),
                service.fingerprint(boxWithTypeParameterNamed("E")),
                "Box<T> and Box<E> are different model versions");
    }

    @Test
    void typeParameterBoundIsPartOfTheKey() {
        assertNotEquals(service.fingerprint(boxWithBound(null)),
                service.fingerprint(boxWithBound(EcorePackage.Literals.ESTRING)),
                "Box<T> and Box<T extends EString> are different model versions");
        assertNotEquals(service.fingerprint(boxWithBound(EcorePackage.Literals.ESTRING)),
                service.fingerprint(boxWithBound(EcorePackage.Literals.EINT)),
                "a changed bound must change the fingerprint");
    }

    @Test
    void addingATypeParameterChangesFingerprint() {
        EPackage plain = pkg();
        EClass box = cls(plain, "Box");
        EAttribute value = EcoreFactory.eINSTANCE.createEAttribute();
        value.setName("value");
        value.setEType(EcorePackage.Literals.ESTRING);
        box.getEStructuralFeatures().add(value);

        EPackage generified = pkg();
        EClass genericBox = cls(generified, "Box");
        ETypeParameter parameter = EcoreFactory.eINSTANCE.createETypeParameter();
        parameter.setName("T");
        genericBox.getETypeParameters().add(parameter);
        EAttribute genericValue = EcoreFactory.eINSTANCE.createEAttribute();
        genericValue.setName("value");
        genericValue.setEType(EcorePackage.Literals.ESTRING);
        genericBox.getEStructuralFeatures().add(genericValue);

        assertNotEquals(service.fingerprint(plain), service.fingerprint(generified),
                "generifying a class must change the fingerprint");
    }

    @Test
    void typeArgumentOfAFeatureIsPartOfTheKey() {
        // The headline case: eType is Box in both, so v1 emitted the identical line.
        assertNotEquals(service.fingerprint(repoHoldingBoxOf("Person")),
                service.fingerprint(repoHoldingBoxOf("Address")),
                "Box<Person> and Box<Address> are different model versions");
    }

    @Test
    void typeArgumentOfAGenericSuperTypeIsPartOfTheKey() {
        assertNotEquals(service.fingerprint(subclassOfBoxOf(EcorePackage.Literals.ESTRING)),
                service.fingerprint(subclassOfBoxOf(EcorePackage.Literals.EINT)),
                "extends Box<EString> and extends Box<EInt> are different model versions");
    }

    @Test
    void wildcardBoundIsPartOfTheKey() {
        assertNotEquals(service.fingerprint(repoHoldingBoxOfWildcard(null)),
                service.fingerprint(repoHoldingBoxOfWildcard(EcorePackage.Literals.ESTRING)),
                "Box<?> and Box<? extends EString> are different model versions");
    }

    @Test
    void operationTypeParameterIsPartOfTheKey() {
        assertNotEquals(service.fingerprint(serviceWithOperationTypeParameter("T")),
                service.fingerprint(serviceWithOperationTypeParameter("R")),
                "an operation's type parameter must be part of the canonical form");
    }

    // ---- generic data types ----------------------------------------------------

    /** {@code EDataType Wrapper} whose instance type is {@code java.util.List<argument>}. */
    private static EPackage wrapperOfInstanceType(String instanceTypeName) {
        EPackage p = pkg();
        EDataType wrapper = EcoreFactory.eINSTANCE.createEDataType();
        wrapper.setName("Wrapper");
        wrapper.setInstanceTypeName(instanceTypeName);
        p.getEClassifiers().add(wrapper);
        return p;
    }

    @Test
    void genericInstanceTypeNameOfADataTypeIsPartOfTheKey() {
        // Both erase to instanceClassName "java.util.List" — the erasure alone cannot tell
        // these two model versions apart.
        EPackage listOfString = wrapperOfInstanceType("java.util.List<java.lang.String>");
        EPackage listOfInteger = wrapperOfInstanceType("java.util.List<java.lang.Integer>");

        assertEquals("java.util.List",
                ((EDataType) listOfString.getEClassifier("Wrapper")).getInstanceClassName(),
                "precondition: the raw instance class name is identical");
        assertEquals("java.util.List",
                ((EDataType) listOfInteger.getEClassifier("Wrapper")).getInstanceClassName());

        assertNotEquals(service.fingerprint(listOfString), service.fingerprint(listOfInteger),
                "List<String> and List<Integer> are different model versions");
    }

    @Test
    void nonGenericDataTypeIsUnaffectedByGenericsSupport() {
        // EMF keeps instanceTypeName == instanceClassName for non-generic types, so nothing
        // extra may be emitted — otherwise every data type's hash would move.
        EPackage viaClassName = pkg();
        EDataType money = EcoreFactory.eINSTANCE.createEDataType();
        money.setName("Money");
        money.setInstanceClassName("java.math.BigDecimal");
        viaClassName.getEClassifiers().add(money);

        EPackage viaTypeName = pkg();
        EDataType money2 = EcoreFactory.eINSTANCE.createEDataType();
        money2.setName("Money");
        money2.setInstanceTypeName("java.math.BigDecimal");
        viaTypeName.getEClassifiers().add(money2);

        assertEquals(service.fingerprint(viaClassName), service.fingerprint(viaTypeName),
                "a non-generic instance type must hash the same either way");
    }

    @Test
    void typeParameterOfADataTypeIsPartOfTheKey() {
        EPackage withT = pkg();
        EDataType wrapperT = EcoreFactory.eINSTANCE.createEDataType();
        wrapperT.setName("Wrapper");
        ETypeParameter t = EcoreFactory.eINSTANCE.createETypeParameter();
        t.setName("T");
        wrapperT.getETypeParameters().add(t);
        withT.getEClassifiers().add(wrapperT);

        EPackage withoutT = pkg();
        EDataType wrapperRaw = EcoreFactory.eINSTANCE.createEDataType();
        wrapperRaw.setName("Wrapper");
        withoutT.getEClassifiers().add(wrapperRaw);

        assertNotEquals(service.fingerprint(withT), service.fingerprint(withoutT),
                "a data type's type parameter must be part of the canonical form");
    }

    // ---- canonicality: a raw eType reference must not be perturbed --------------

    @Test
    void plainTypedFeatureIsUnaffectedByGenericsSupport() {
        // EMF creates an EGenericType wrapper for every setEType call. Emitting generic
        // detail for those wrappers would change the hash of every non-generic model —
        // the canonical form must only speak up when the generic type adds information.
        EPackage viaEType = pkg();
        EClass a = cls(viaEType, "Holder");
        EAttribute plain = EcoreFactory.eINSTANCE.createEAttribute();
        plain.setName("value");
        plain.setEType(EcorePackage.Literals.ESTRING);
        a.getEStructuralFeatures().add(plain);

        EPackage viaGenericType = pkg();
        EClass b = cls(viaGenericType, "Holder");
        EAttribute wrapped = EcoreFactory.eINSTANCE.createEAttribute();
        wrapped.setName("value");
        wrapped.setEGenericType(generic(EcorePackage.Literals.ESTRING));
        b.getEStructuralFeatures().add(wrapped);

        assertEquals(service.fingerprint(viaEType), service.fingerprint(viaGenericType),
                "setEType and an equivalent argument-free EGenericType must hash the same");
    }

    @Test
    void nonGenericSuperTypeIsUnaffectedByGenericsSupport() {
        EPackage viaESuperTypes = pkg();
        EClass base = cls(viaESuperTypes, "Base");
        EClass sub = cls(viaESuperTypes, "Sub");
        sub.getESuperTypes().add(base);

        EPackage viaGenericSuperTypes = pkg();
        EClass base2 = cls(viaGenericSuperTypes, "Base");
        EClass sub2 = cls(viaGenericSuperTypes, "Sub");
        sub2.getEGenericSuperTypes().add(generic(base2));

        assertEquals(service.fingerprint(viaESuperTypes), service.fingerprint(viaGenericSuperTypes),
                "an argument-free generic supertype must hash like a plain supertype");
    }
}
