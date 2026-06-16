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
package org.eclipse.fennec.emf.ecore.tool;

import java.io.PrintWriter;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Renders the structure of the loaded model - {@link EPackage}s with their
 * classifiers, attributes, references, operations and enum literals - as
 * indented plain text.
 */
public final class StructurePrinter {

	private StructurePrinter() {
		// utility
	}

	/**
	 * Prints the structure of every root element of the resource.
	 *
	 * @param resource the loaded resource
	 * @param out      the writer to render to
	 */
	public static void print(Resource resource, PrintWriter out) {
		if (resource.getContents().isEmpty()) {
			out.println("No model content was loaded.");
			return;
		}
		for (EObject root : resource.getContents()) {
			if (root instanceof EPackage ePackage) {
				printPackage(ePackage, out, 0);
			} else {
				out.println("Root element (not an EPackage): " + root.eClass().getName());
			}
		}
	}

	private static void printPackage(EPackage ePackage, PrintWriter out, int indent) {
		String pad = indent(indent);
		out.println(pad + "EPackage " + ePackage.getName());
		out.println(pad + "  nsURI    : " + ePackage.getNsURI());
		out.println(pad + "  nsPrefix : " + ePackage.getNsPrefix());

		for (EClassifier classifier : ePackage.getEClassifiers()) {
			printClassifier(classifier, out, indent + 1);
		}

		for (EPackage sub : ePackage.getESubpackages()) {
			out.println();
			out.println(pad + "  Subpackage:");
			printPackage(sub, out, indent + 2);
		}
	}

	private static void printClassifier(EClassifier classifier, PrintWriter out, int indent) {
		String pad = indent(indent);
		if (classifier instanceof EClass eClass) {
			StringBuilder header = new StringBuilder(pad);
			header.append(eClass.isInterface() ? "interface " : (eClass.isAbstract() ? "abstract class " : "class "));
			header.append(eClass.getName());
			if (!eClass.getESuperTypes().isEmpty()) {
				header.append(" extends ");
				header.append(eClass.getESuperTypes().stream().map(EClass::getName).collect(Collectors.joining(", ")));
			}
			out.println(header);

			for (EAttribute attribute : eClass.getEAttributes()) {
				out.println(pad + "  attr " + attribute.getName() + " : " + typeName(attribute)
						+ multiplicity(attribute) + flags(attribute.isID() ? "id" : null));
			}
			for (EReference reference : eClass.getEReferences()) {
				StringBuilder ref = new StringBuilder(pad);
				ref.append("  ref  ").append(reference.getName()).append(" : ").append(typeName(reference))
						.append(multiplicity(reference));
				ref.append(flags(reference.isContainment() ? "containment" : null));
				if (reference.getEOpposite() != null) {
					ref.append(" opposite=").append(reference.getEOpposite().getName());
				}
				out.println(ref);
			}
			for (EOperation operation : eClass.getEOperations()) {
				String params = operation.getEParameters().stream()
						.map(p -> p.getName() + " : " + typeName(p))
						.collect(Collectors.joining(", "));
				out.println(pad + "  op   " + operation.getName() + "(" + params + ") : "
						+ (operation.getEType() == null ? "void" : typeName(operation)) + multiplicity(operation));
			}
		} else if (classifier instanceof EEnum eEnum) {
			out.println(pad + "enum " + eEnum.getName());
			for (EEnumLiteral literal : eEnum.getELiterals()) {
				out.println(pad + "  literal " + literal.getName() + " = " + literal.getValue()
						+ (literal.getLiteral().equals(literal.getName()) ? "" : " (\"" + literal.getLiteral() + "\")"));
			}
		} else if (classifier instanceof EDataType eDataType) {
			out.println(pad + "datatype " + eDataType.getName() + " : "
					+ (eDataType.getInstanceTypeName() == null ? "?" : eDataType.getInstanceTypeName()));
		} else {
			out.println(pad + "classifier " + classifier.getName());
		}
	}

	private static String typeName(ETypedElement element) {
		return element.getEType() == null ? "<unresolved>" : element.getEType().getName();
	}

	private static String multiplicity(ETypedElement element) {
		int lower = element.getLowerBound();
		int upper = element.getUpperBound();
		if (lower == 0 && upper == 1) {
			return "";
		}
		String upperText = upper == ETypedElement.UNBOUNDED_MULTIPLICITY ? "*" : Integer.toString(upper);
		return " [" + lower + ".." + upperText + "]";
	}

	private static String flags(String flag) {
		return flag == null ? "" : " {" + flag + "}";
	}

	private static String indent(int level) {
		return "  ".repeat(level);
	}
}
