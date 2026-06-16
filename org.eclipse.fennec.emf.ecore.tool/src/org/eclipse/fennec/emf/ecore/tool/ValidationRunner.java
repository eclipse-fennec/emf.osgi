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

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;

/**
 * Runs the EMF validation mechanism ({@link Diagnostician}) over the contents
 * of a resource and renders the resulting {@link Diagnostic} tree.
 */
public final class ValidationRunner {

	private ValidationRunner() {
		// utility
	}

	/**
	 * Validates every root object of the resource and writes a human readable
	 * report to {@code out}.
	 *
	 * @param resource the loaded resource
	 * @param out      the writer to render the findings to
	 * @return {@code true} if no errors were found (resource may still contain
	 *         warnings), {@code false} if at least one error was reported
	 */
	public static boolean validate(Resource resource, PrintWriter out) {
		boolean ok = true;

		// Surface parse-level problems first - a malformed file may still load partially.
		if (!resource.getErrors().isEmpty() || !resource.getWarnings().isEmpty()) {
			out.println("Resource diagnostics:");
			resource.getErrors().forEach(d -> out.println("  [ERROR]   " + d.getMessage()));
			resource.getWarnings().forEach(d -> out.println("  [WARNING] " + d.getMessage()));
			out.println();
			ok = resource.getErrors().isEmpty();
		}

		if (resource.getContents().isEmpty()) {
			out.println("No model content was loaded.");
			return false;
		}

		for (EObject root : resource.getContents()) {
			Diagnostic diagnostic = Diagnostician.INSTANCE.validate(root);
			out.println("Validation of " + label(root) + ": " + severityName(diagnostic.getSeverity()));
			printChildren(diagnostic, out, 1);
			out.println();
			if (diagnostic.getSeverity() >= Diagnostic.ERROR) {
				ok = false;
			}
		}

		out.println(ok ? "RESULT: VALID" : "RESULT: INVALID");
		return ok;
	}

	private static void printChildren(Diagnostic diagnostic, PrintWriter out, int indent) {
		for (Diagnostic child : diagnostic.getChildren()) {
			out.println(indent(indent) + "[" + severityName(child.getSeverity()) + "] " + child.getMessage());
			printChildren(child, out, indent + 1);
		}
	}

	private static String label(EObject object) {
		return object.eClass().getName();
	}

	private static String indent(int level) {
		return "  ".repeat(level);
	}

	private static String severityName(int severity) {
		switch (severity) {
			case Diagnostic.OK:
				return "OK";
			case Diagnostic.INFO:
				return "INFO";
			case Diagnostic.WARNING:
				return "WARNING";
			case Diagnostic.ERROR:
				return "ERROR";
			case Diagnostic.CANCEL:
				return "CANCEL";
			default:
				return "UNKNOWN(" + severity + ")";
		}
	}
}
