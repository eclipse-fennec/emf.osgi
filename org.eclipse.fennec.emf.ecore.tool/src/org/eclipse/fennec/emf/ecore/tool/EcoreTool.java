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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMLResource;

/**
 * Plain-Java command line entry point for loading, validating, round-tripping
 * and inspecting Ecore/XMI files.
 * <p>
 * It is intended for cross-language conformance testing: EMF implementations
 * written in other languages can be checked against this reference EMF runtime
 * to ensure they produce valid Ecore files and valid XMI.
 * <p>
 * Exit codes:
 * <ul>
 *   <li>{@code 0} - success (model loaded; in validate mode also: no errors)</li>
 *   <li>{@code 1} - validation reported at least one error</li>
 *   <li>{@code 2} - usage error or the model could not be loaded</li>
 * </ul>
 */
public final class EcoreTool {

	/** Operating mode of the tool. */
	enum Mode {
		VALIDATE,
		ROUNDTRIP,
		PRINT
	}

	private static final int EXIT_OK = 0;
	private static final int EXIT_INVALID = 1;
	private static final int EXIT_USAGE = 2;

	private EcoreTool() {
		// entry point only
	}

	public static void main(String[] args) {
		int code = run(args);
		System.exit(code);
	}

	static int run(String[] args) {
		Mode mode = Mode.PRINT;
		File input = null;
		File output = null;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			switch (arg) {
				case "-h":
				case "--help":
					printUsage(new PrintWriter(System.out, true));
					return EXIT_OK;
				case "--validate":
					mode = Mode.VALIDATE;
					break;
				case "--roundtrip":
					mode = Mode.ROUNDTRIP;
					break;
				case "--print":
					mode = Mode.PRINT;
					break;
				case "-o":
				case "--output":
					if (i + 1 >= args.length) {
						return usageError("Missing value for " + arg);
					}
					output = new File(args[++i]);
					break;
				default:
					if (arg.startsWith("-")) {
						return usageError("Unknown option: " + arg);
					}
					if (input != null) {
						return usageError("Only one input file is supported, got also: " + arg);
					}
					input = new File(arg);
					break;
			}
		}

		if (input == null) {
			return usageError("No input file given");
		}

		try {
			ModelLoader loader = new ModelLoader();
			Resource resource = loader.load(input);
			// Make the loaded packages resolvable for validation / instance models.
			for (EObject root : resource.getContents()) {
				if (root instanceof EPackage ePackage) {
					loader.registerPackage(ePackage);
				}
			}

			switch (mode) {
				case VALIDATE:
					return runValidate(resource, output);
				case ROUNDTRIP:
					return runRoundtrip(resource, input, output);
				case PRINT:
				default:
					return runPrint(resource, output);
			}
		} catch (IOException e) {
			System.err.println("Failed to load '" + input + "': " + e.getMessage());
			return EXIT_USAGE;
		}
	}

	private static int runValidate(Resource resource, File output) throws IOException {
		boolean valid;
		if (output == null) {
			PrintWriter out = new PrintWriter(System.out, true);
			valid = ValidationRunner.validate(resource, out);
			out.flush();
		} else {
			try (PrintWriter out = newFileWriter(output)) {
				valid = ValidationRunner.validate(resource, out);
			}
			System.out.println("Validation report written to " + output.getAbsolutePath());
		}
		return valid ? EXIT_OK : EXIT_INVALID;
	}

	private static int runPrint(Resource resource, File output) throws IOException {
		if (output == null) {
			PrintWriter out = new PrintWriter(System.out, true);
			StructurePrinter.print(resource, out);
			out.flush();
		} else {
			try (PrintWriter out = newFileWriter(output)) {
				StructurePrinter.print(resource, out);
			}
			System.out.println("Structure written to " + output.getAbsolutePath());
		}
		return EXIT_OK;
	}

	private static int runRoundtrip(Resource resource, File input, File output) throws IOException {
		File target = output != null ? output : defaultRoundtripTarget(input);
		Map<Object, Object> options = Map.of(
				XMLResource.OPTION_ENCODING, StandardCharsets.UTF_8.name(),
				XMLResource.OPTION_SAVE_TYPE_INFORMATION, Boolean.TRUE);
		try (OutputStream os = Files.newOutputStream(target.toPath())) {
			resource.save(os, options);
		}
		System.out.println("Round-trip output written to " + target.getAbsolutePath());
		return EXIT_OK;
	}

	private static File defaultRoundtripTarget(File input) {
		String name = input.getName();
		int dot = name.lastIndexOf('.');
		String base = dot < 0 ? name : name.substring(0, dot);
		String ext = dot < 0 ? "" : name.substring(dot);
		File parent = input.getAbsoluteFile().getParentFile();
		return new File(parent, base + ".roundtrip" + ext);
	}

	private static PrintWriter newFileWriter(File output) throws IOException {
		return new PrintWriter(Files.newBufferedWriter(output.toPath(), StandardCharsets.UTF_8));
	}

	private static int usageError(String message) {
		System.err.println("Error: " + message);
		System.err.println();
		printUsage(new PrintWriter(System.err, true));
		return EXIT_USAGE;
	}

	private static void printUsage(PrintWriter out) {
		out.println("Fennec EMF Ecore Tool");
		out.println();
		out.println("Loads an Ecore/XMI file with a standalone EMF runtime and runs one of");
		out.println("several modes. Intended for cross-language EMF conformance / round-trip tests.");
		out.println();
		out.println("Usage:");
		out.println("  java -jar org.eclipse.fennec.emf.ecore.tool.jar [mode] [options] <input-file>");
		out.println();
		out.println("Modes (default: --print):");
		out.println("  --validate      Run the EMF validation mechanism and print the findings.");
		out.println("                  Exits with code 1 if validation errors are found.");
		out.println("  --roundtrip     Load the model and serialize it again (round-trip test).");
		out.println("  --print         Print the structure of the loaded EPackage(s).");
		out.println();
		out.println("Options:");
		out.println("  -o, --output <file>   Write the result to <file> instead of the console.");
		out.println("                        In --roundtrip mode this is the re-serialized model");
		out.println("                        (default: <input>.roundtrip.<ext> next to the input).");
		out.println("  -h, --help            Show this help.");
	}
}
