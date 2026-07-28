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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypeParameter;

/**
 * The {@code fp1} canonicalization scheme: a canonical textual form of an
 * {@link EPackage}'s <em>semantics</em>, traversing the in-memory model and never the
 * serialized bytes, hashed with SHA-256.
 * <p>
 * Rules:
 * <ul>
 *   <li>Classifiers of a package are emitted <b>sorted by name</b> — their order in
 *       the package carries no meaning.</li>
 *   <li>Structural features, operations, parameters, supertypes, type parameters and enum
 *       literals are emitted in <b>declared order</b> — order is meaningful
 *       (feature/operation IDs, inherited feature order, type-parameter position).</li>
 *   <li>EAnnotations are emitted sorted by source, their details sorted by key.
 *       <b>Tooling-configuration sources are excluded entirely</b> (the GenModel
 *       namespaces): they configure how the Java artifact is generated — base package,
 *       compliance level — never the observable behaviour of the model. An annotation is
 *       fingerprint-relevant when it affects structure, validation, serialization
 *       (ExtendedMetaData!), mapping or delegates. The <b>{@code documentation}</b>
 *       detail is excluded under <em>every</em> source, and an annotation left with no
 *       remaining details is dropped entirely.</li>
 *   <li>Type references use a stable {@code nsURI#Name} key, so they resolve
 *       identically regardless of resource/serialization state.</li>
 *   <li>Operation exceptions are sorted — they are a set, their order carries no meaning.</li>
 *   <li>Derivation input tokens are sorted — their order carries no meaning.</li>
 * </ul>
 *
 * <h2>Generics</h2>
 * Type parameters and generic types are part of the canonical form (issue #17):
 * {@link org.eclipse.emf.ecore.ETypeParameter}s of classes and operations with their
 * bounds, type arguments of generic types, type-parameter references and wildcards with
 * their bounds.
 * <p>
 * Generic detail is emitted <b>only where it adds information</b> beyond the plain
 * {@code eType}. That is not an optimization but a correctness requirement: EMF creates an
 * {@link org.eclipse.emf.ecore.EGenericType} wrapper for every {@code setEType} call, so
 * emitting those wrappers unconditionally would move the hash of every model — including
 * the overwhelming majority that use no generics at all. A generic type is informative
 * when it references a type parameter, carries type arguments, or is a wildcard.
 * <p>
 * Type parameter <b>names</b> are part of the form, consistent with the treatment of every
 * other name in this scheme; an alpha-rename therefore yields a new model version. That is
 * the conservative direction on purpose: a false "same" would serve one version's objects
 * with another version's metadata, whereas a false "different" only costs precision.
 *
 * @author Mark Hoffmann
 * @see CanonicalizationScheme for why a published scheme is frozen
 */
final class Fp1CanonicalizationScheme implements CanonicalizationScheme {

    static final String TAG = "fp1";

    private static final String DOCUMENTATION_KEY = "documentation";

    /**
     * Annotation sources excluded from the canonical form as a whole: everything under
     * them is tooling configuration (how the Java artifact is generated), never model
     * content. Two GenModel namespaces exist in the wild (EMF and UML2), hence a list.
     * <p>
     * <b>This list is part of the frozen {@code fp1} contract.</b> Any content change
     * moves the fingerprints of models carrying such annotations and is therefore a new
     * scheme tag, not an edit here.
     */
    private static final Set<String> IGNORED_SOURCES = Set.of(
            "http://www.eclipse.org/emf/2002/GenModel",
            "http://www.eclipse.org/uml2/2.2.0/GenModel");

    /**
     * Second tier of the ignorelist: detail keys excluded per source. Deliberately empty
     * in {@code fp1} — the tier exists so the shape is settled; populating it is a new
     * scheme tag for the same reason as above.
     */
    private static final Map<String, Set<String>> IGNORED_KEYS_BY_SOURCE = Map.of();

    @Override
    public String tag() {
        return TAG;
    }

    @Override
    public String canonicalForm(EPackage ePackage, String... derivationInputs) {
        StringBuilder sb = new StringBuilder(1024);
        appendPackage(sb, ePackage);
        appendDerivationInputs(sb, derivationInputs);
        return sb.toString();
    }

    private void appendPackage(StringBuilder sb, EPackage ePackage) {
        sb.append("P|").append(ePackage.getNsURI()).append('\n');

        List<EClassifier> classifiers = new ArrayList<>(ePackage.getEClassifiers());
        classifiers.sort(Comparator.comparing(EClassifier::getName, Comparator.nullsFirst(Comparator.naturalOrder())));
        for (EClassifier classifier : classifiers) {
            appendClassifier(sb, classifier);
        }
    }

    private void appendClassifier(StringBuilder sb, EClassifier classifier) {
        if (classifier instanceof EClass eClass) {
            sb.append("C|").append(eClass.getName())
                .append("|abstract=").append(eClass.isAbstract())
                .append("|interface=").append(eClass.isInterface());
            // Supertypes in declared order (affects inherited feature order).
            for (EClass sup : eClass.getESuperTypes()) {
                sb.append("|super=").append(typeKey(sup));
            }
            sb.append('\n');
            // Type parameters in declared order (position is meaningful).
            appendTypeParameters(sb, "  ", eClass.getETypeParameters());
            // Only generic supertypes that add information beyond the plain |super= key
            // above — a plain supertype is mirrored as an argument-free generic supertype.
            List<EGenericType> genericSuperTypes = eClass.getEGenericSuperTypes();
            for (int i = 0; i < genericSuperTypes.size(); i++) {
                EGenericType genericSuperType = genericSuperTypes.get(i);
                if (isInformative(genericSuperType)) {
                    sb.append("  G|super[").append(i).append("]=")
                        .append(genericRepr(genericSuperType)).append('\n');
                }
            }
            appendAnnotations(sb, eClass);
            for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
                appendFeature(sb, feature);
            }
            for (EOperation operation : eClass.getEOperations()) {
                appendOperation(sb, operation);
            }
        } else if (classifier instanceof EEnum eEnum) {
            sb.append("E|").append(eEnum.getName()).append('\n');
            appendTypeParameters(sb, "  ", eEnum.getETypeParameters());
            appendAnnotations(sb, eEnum);
            for (EEnumLiteral literal : eEnum.getELiterals()) {
                sb.append("  L|").append(literal.getName())
                    .append("|value=").append(literal.getValue())
                    .append("|literal=").append(literal.getLiteral()).append('\n');
            }
        } else if (classifier instanceof EDataType dataType) {
            sb.append("D|").append(dataType.getName())
                .append("|instanceClassName=").append(dataType.getInstanceClassName())
                .append("|serializable=").append(dataType.isSerializable());
            appendInstanceTypeName(sb, dataType);
            sb.append('\n');
            // EDataType is an EClassifier, so it may be generic too: `Wrapper<T>`.
            appendTypeParameters(sb, "  ", dataType.getETypeParameters());
            appendAnnotations(sb, dataType);
        }
    }

    /**
     * Appends {@code |instanceTypeName=} only when it says more than
     * {@code instanceClassName}, which is already emitted.
     * <p>
     * The two differ exactly when the instance type is generic:
     * {@code instanceTypeName="java.util.List<java.lang.String>"} erases to
     * {@code instanceClassName="java.util.List"}. Emitting only the erasure would make
     * {@code List<String>} and {@code List<Integer>} the same model version. EMF keeps the
     * two in sync for non-generic types, so the guard is also what preserves the hash of
     * every data type that is not generic.
     *
     * @param sb the canonical form under construction
     * @param dataType the data type being emitted
     */
    private void appendInstanceTypeName(StringBuilder sb, EDataType dataType) {
        String instanceTypeName = dataType.getInstanceTypeName();
        if (instanceTypeName != null && !instanceTypeName.equals(dataType.getInstanceClassName())) {
            sb.append("|instanceTypeName=").append(instanceTypeName);
        }
    }

    private void appendFeature(StringBuilder sb, EStructuralFeature feature) {
        if (feature instanceof EAttribute attr) {
            sb.append("  A|").append(attr.getName())
                .append("|type=").append(typeKey(attr.getEType()))
                .append("|id=").append(attr.isID())
                .append("|default=").append(attr.getDefaultValueLiteral());
        } else if (feature instanceof EReference ref) {
            sb.append("  R|").append(ref.getName())
                .append("|type=").append(typeKey(ref.getEType()))
                .append("|containment=").append(ref.isContainment())
                .append("|container=").append(ref.isContainer())
                .append("|opposite=").append(oppositeKey(ref.getEOpposite()));
        } else {
            sb.append("  F|").append(feature.getName())
                .append("|type=").append(typeKey(feature.getEType()));
        }
        sb.append("|ordered=").append(feature.isOrdered())
            .append("|unique=").append(feature.isUnique())
            .append("|lb=").append(feature.getLowerBound())
            .append("|ub=").append(feature.getUpperBound())
            .append("|changeable=").append(feature.isChangeable())
            .append("|derived=").append(feature.isDerived())
            .append("|transient=").append(feature.isTransient());
        appendGenericType(sb, feature.getEGenericType());
        sb.append('\n');
        appendAnnotations(sb, feature);
    }

    private void appendOperation(StringBuilder sb, EOperation operation) {
        sb.append("  O|").append(operation.getName())
            .append("|type=").append(typeKey(operation.getEType()))
            .append("|ordered=").append(operation.isOrdered())
            .append("|unique=").append(operation.isUnique())
            .append("|lb=").append(operation.getLowerBound())
            .append("|ub=").append(operation.getUpperBound());
        appendGenericType(sb, operation.getEGenericType());
        // Exceptions: set semantics, order not meaningful -> sorted.
        List<String> exceptions = new ArrayList<>();
        for (EClassifier ex : operation.getEExceptions()) {
            exceptions.add(typeKey(ex));
        }
        exceptions.sort(Comparator.naturalOrder());
        for (String ex : exceptions) {
            sb.append("|throws=").append(ex);
        }
        // Generic exceptions likewise, but only the informative ones — a plain exception is
        // mirrored as an argument-free generic exception and already covered above.
        List<String> genericExceptions = new ArrayList<>();
        for (EGenericType genericException : operation.getEGenericExceptions()) {
            if (isInformative(genericException)) {
                genericExceptions.add(genericRepr(genericException));
            }
        }
        genericExceptions.sort(Comparator.naturalOrder());
        for (String genericException : genericExceptions) {
            sb.append("|gthrows=").append(genericException);
        }
        sb.append('\n');
        // Type parameters in declared order (position is meaningful).
        appendTypeParameters(sb, "    ", operation.getETypeParameters());
        // Parameters in declared order (position is meaningful).
        for (EParameter parameter : operation.getEParameters()) {
            sb.append("    p|").append(parameter.getName())
                .append("|type=").append(typeKey(parameter.getEType()))
                .append("|ordered=").append(parameter.isOrdered())
                .append("|unique=").append(parameter.isUnique())
                .append("|lb=").append(parameter.getLowerBound())
                .append("|ub=").append(parameter.getUpperBound());
            appendGenericType(sb, parameter.getEGenericType());
            sb.append('\n');
        }
        appendAnnotations(sb, operation);
    }

    /**
     * Emits {@link ETypeParameter}s in declared order with their bounds, one line each.
     * Emits nothing when there are none — which is what keeps models without generics
     * byte-identical.
     *
     * @param sb the canonical form under construction
     * @param indent the indentation marking the owning scope (class vs. operation)
     * @param typeParameters the type parameters in declared order
     */
    private void appendTypeParameters(StringBuilder sb, String indent, List<ETypeParameter> typeParameters) {
        for (ETypeParameter typeParameter : typeParameters) {
            sb.append(indent).append("T|").append(typeParameter.getName());
            // Bounds in declared order: `T extends A & B` is not `T extends B & A` in the
            // generated API, even though the constraint is the same.
            for (EGenericType bound : typeParameter.getEBounds()) {
                sb.append("|bound=").append(genericRepr(bound));
            }
            sb.append('\n');
            appendAnnotations(sb, typeParameter);
        }
    }

    /**
     * Appends {@code |generic=<repr>} for a typed element's generic type — but only when it
     * carries information beyond the element's {@code eType}, which is already emitted.
     * <p>
     * EMF creates an {@link EGenericType} wrapper for every {@code setEType} call, so
     * emitting unconditionally here would move the fingerprint of every model that uses no
     * generics at all.
     *
     * @param sb the canonical form under construction
     * @param genericType the typed element's generic type; may be {@code null}
     */
    private void appendGenericType(StringBuilder sb, EGenericType genericType) {
        if (isInformative(genericType)) {
            sb.append("|generic=").append(genericRepr(genericType));
        }
    }

    /**
     * Whether a generic type says more than the plain classifier reference it wraps: it
     * references a type parameter, carries type arguments, or is a wildcard.
     *
     * @param genericType the generic type to test; may be {@code null}
     * @return {@code true} if it must be emitted
     */
    private boolean isInformative(EGenericType genericType) {
        if (genericType == null) {
            return false;
        }
        return genericType.getETypeParameter() != null
                || !genericType.getETypeArguments().isEmpty()
                || genericType.getEClassifier() == null;
    }

    /**
     * Stable textual form of a generic type: a type-parameter reference ({@code var:T}), a
     * classifier key with optional type arguments ({@code nsURI#Box<nsURI#Person>}), or a
     * wildcard with its bounds ({@code ?+nsURI#Person}).
     *
     * @param genericType the generic type; may be {@code null}
     * @return the representation, {@code "null"} when absent
     */
    private String genericRepr(EGenericType genericType) {
        if (genericType == null) {
            return "null";
        }
        StringBuilder repr = new StringBuilder();
        ETypeParameter typeParameter = genericType.getETypeParameter();
        if (typeParameter != null) {
            // The name identifies the parameter within its declaring scope; the scope
            // itself is fixed by where this representation is emitted.
            repr.append("var:").append(typeParameter.getName());
        } else if (genericType.getEClassifier() != null) {
            repr.append(typeKey(genericType.getEClassifier()));
        } else {
            repr.append('?');
            if (genericType.getEUpperBound() != null) {
                repr.append('+').append(genericRepr(genericType.getEUpperBound()));
            }
            if (genericType.getELowerBound() != null) {
                repr.append('-').append(genericRepr(genericType.getELowerBound()));
            }
        }
        List<EGenericType> typeArguments = genericType.getETypeArguments();
        if (!typeArguments.isEmpty()) {
            repr.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    repr.append(',');
                }
                repr.append(genericRepr(typeArguments.get(i)));
            }
            repr.append('>');
        }
        return repr.toString();
    }

    /**
     * Emits annotations sorted by source with details sorted by key, applying the
     * two-tier ignorelist: {@link #IGNORED_SOURCES} drops whole annotations (tooling
     * configuration), {@link #IGNORED_KEYS_BY_SOURCE} and the global
     * {@code documentation} key drop single details. Annotations left without any
     * relevant detail are dropped, so documentation-only metadata is invisible to the
     * fingerprint.
     */
    private void appendAnnotations(StringBuilder sb, EModelElement element) {
        // Sort annotations by source.
        List<EAnnotation> annotations = new ArrayList<>(element.getEAnnotations());
        annotations.sort(Comparator.comparing(EAnnotation::getSource, Comparator.nullsFirst(Comparator.naturalOrder())));
        for (EAnnotation annotation : annotations) {
            String source = annotation.getSource();
            if (source != null && IGNORED_SOURCES.contains(source)) {
                continue; // tooling configuration -> never model content
            }
            Set<String> ignoredKeys = source == null
                    ? Set.of() : IGNORED_KEYS_BY_SOURCE.getOrDefault(source, Set.of());
            Map<String, String> details = new TreeMap<>();
            for (Map.Entry<String, String> entry : annotation.getDetails()) {
                String key = entry.getKey();
                if (DOCUMENTATION_KEY.equals(key) || (key != null && ignoredKeys.contains(key))) {
                    continue;
                }
                details.put(key, entry.getValue());
            }
            if (details.isEmpty()) {
                continue; // documentation-only (or empty) annotation -> not log-relevant
            }
            sb.append("    @|").append(annotation.getSource());
            for (Map.Entry<String, String> entry : details.entrySet()) {
                sb.append('|').append(entry.getKey()).append('=').append(entry.getValue());
            }
            sb.append('\n');
        }
    }

    private void appendDerivationInputs(StringBuilder sb, String... derivationInputs) {
        if (derivationInputs == null || derivationInputs.length == 0) {
            return;
        }
        List<String> inputs = new ArrayList<>();
        for (String input : derivationInputs) {
            if (input != null) {
                inputs.add(input);
            }
        }
        inputs.sort(Comparator.naturalOrder()); // order not significant
        sb.append("I|");
        for (String input : inputs) {
            sb.append(input).append(';');
        }
        sb.append('\n');
    }

    /** Stable {@code nsURI#Name} key for a classifier reference; {@code "null"} when absent. */
    private String typeKey(EClassifier classifier) {
        if (classifier == null) {
            return "null";
        }
        EPackage pkg = classifier.getEPackage();
        String ns = pkg != null ? pkg.getNsURI() : "";
        return ns + "#" + classifier.getName();
    }

    private String oppositeKey(EReference opposite) {
        if (opposite == null) {
            return "";
        }
        return typeKey(opposite.getEContainingClass()) + "/" + opposite.getName();
    }
}
