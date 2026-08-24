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
package org.eclipse.fennec.emf.osgi.codegen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.impl.GenModelFactoryImpl;
import org.eclipse.emf.codegen.ecore.genmodel.impl.GenModelImpl;
import org.eclipse.emf.codegen.ecore.genmodel.impl.GenModelPackageImpl;
import org.eclipse.emf.codegen.util.CodeGenUtil;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.annotation.provide.EPackage;
import org.eclipse.fennec.emf.osgi.codegen.FennecEmfGenerator.GeneratorOptions;
import org.eclipse.fennec.emf.osgi.codegen.adapter.BNDGeneratorAdapterFactory;
import org.eclipse.fennec.emf.osgi.constants.VersionConstant;
import org.osgi.resource.Capability;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.service.externalplugin.ExternalPlugin;
import aQute.bnd.service.generate.BuildContext;
import aQute.bnd.service.generate.Generator;
import aQute.bnd.service.generate.Options;
import aQute.lib.io.IO;

@ExternalPlugin(name = "fennecEMF", objectClass = Generator.class, version = VersionConstant.FENNECPROJECTS_EMF_VERSION)
public class FennecEmfGenerator implements Generator<GeneratorOptions> {

	public static final String ORIGINAL_GEN_MODEL_PATH = "originalGenModelPath";
	public static final String ORIGINAL_GEN_MODEL_PATHS_EXTRA = "originalGenModelPathsExtra";
	public static final String INCLUDE_GEN_MODEL_FOLDER = "includeGenModelFolder";
	/** key in the generator options data map holding the configured line delimiter, if any */
	public static final String LINE_DELIMITER = "lineDelimiter";

	/** The output folders default */
	private static final String OUTPUT_DEFAULT = "src-gen"; //$NON-NLS-1$
	/** which genmodel to generate */
	private static final String PROP_GENMODEL = "genmodel"; //$NON-NLS-1$
	/** where the genmodel will end up in the build model, should be used when not in the defaults model folder */
	private static final String PROP_GENMODEL_INLCLUDE_LOCATION = "genmodelIncludeLocation"; //$NON-NLS-1$
	/** PROP_OUTPUT */
	private static final String PROP_OUTPUT = "output"; //$NON-NLS-1$
	/** PROP_LOGFILE */
	private static final String PROP_LOGFILE = "logfile"; //$NON-NLS-1$
	/** line endings of the generated files: system (default), lf or crlf */
	private static final String PROP_LINE_ENDINGS = "lineEndings"; //$NON-NLS-1$
	/** Eclipse project preferences holding the project specific line delimiter */
	private static final String ECLIPSE_RUNTIME_PREFS_PATH = ".settings/org.eclipse.core.runtime.prefs"; //$NON-NLS-1$
	/** preference key for the project specific line delimiter, see org.eclipse.core.runtime.Platform#PREF_LINE_SEPARATOR */
	private static final String PREF_LINE_SEPARATOR = "line.separator"; //$NON-NLS-1$
	/** @EPackage annotation parameter control properties */
	private static final String PROP_INCLUDE_GENMODEL_ATTR = "includeGenModelAttr"; //$NON-NLS-1$
	private static final String PROP_INCLUDE_GENMODEL_SOURCE_LOCATIONS_ATTR = "includeGenModelSourceLocationsAttr"; //$NON-NLS-1$
	private static final String PROP_INCLUDE_ECORE_ATTR = "includeEcoreAttr"; //$NON-NLS-1$
	private static final String PROP_INCLUDE_ECORE_SOURCE_LOCATIONS_ATTR = "includeEcoreSourceLocationsAttr"; //$NON-NLS-1$

	private static volatile PrintStream logWriter;

	public static void info(String message) {
		PrintStream writer = logWriter;
		if(writer != null) {
			writer.println("[INFO] " + message); //$NON-NLS-1$
			writer.flush();
		} else {
			System.out.println("[INFO] " + message); //$NON-NLS-1$
		}
	}

	public static void warn(String message) {
		PrintStream writer = logWriter;
		if(writer != null) {
			writer.println("[WARN] " + message); //$NON-NLS-1$
			writer.flush();
		} else {
			System.out.println("[WARN] " + message); //$NON-NLS-1$
		}
	}

	public static void error(String message) {
		PrintStream writer = logWriter;
		if(writer != null) {
			writer.println("[ERROR] " + message); //$NON-NLS-1$
			writer.flush();
		} else {
			System.err.println("[ERROR] " + message); //$NON-NLS-1$
		}
	}

	public static void error(String message, Throwable t) {
		error(message);
		PrintStream writer = logWriter;
		if (writer != null) {
			t.printStackTrace(writer);
			writer.flush();
		} else {
			t.printStackTrace(System.err);
		}
	}

	private static void initializeLog(File base, String file) throws IOException {
		closeLog();
		File logFile = new File(base, file);
		IO.delete(logFile);
		if (logFile.createNewFile()) {
			logWriter = new PrintStream(logFile);
		}
	}

	// We don't really use it.
	public interface GeneratorOptions extends Options {
		Optional<File> output();
	}

	public Optional<String> generate(
			BuildContext context, 
			GeneratorOptions options) throws Exception {
		try {
			if(context.get(PROP_LOGFILE) != null) {
				initializeLog(context.getBase(), context.get(PROP_LOGFILE));
			}
			
			info("Running Eclipse Fennec EMF Codegen Version " + VersionConstant.FENNECPROJECTS_EMF_VERSION);
			
			String genFolder = context.get(PROP_OUTPUT);
			File output = null;
			if(genFolder != null) {
				output = context.getFile(genFolder);
			} else {
				output = context.getFile(OUTPUT_DEFAULT);
			}
			info("Output configured: " + genFolder);
			info("Output result: " + output);
			output.mkdirs();

			String genmodel = context.get(PROP_GENMODEL); 
			if(genmodel == null) {
				return Optional.of("genmodel attribute not set");
			}

			String genmodelLocation = context.get(PROP_GENMODEL_INLCLUDE_LOCATION);
			if(genmodelLocation == null) {
				info("genmodelLocation: null");
			} else {
				info("genmodelLocation: [" + genmodelLocation + "]");
			}

			// Read @EPackage annotation parameter configuration (default to true for backward compatibility)
			boolean includeGenModelAttr = !"false".equalsIgnoreCase(context.get(PROP_INCLUDE_GENMODEL_ATTR));
			boolean includeGenModelSourceLocationsAttr = !"false".equalsIgnoreCase(context.get(PROP_INCLUDE_GENMODEL_SOURCE_LOCATIONS_ATTR));
			boolean includeEcoreAttr = !"false".equalsIgnoreCase(context.get(PROP_INCLUDE_ECORE_ATTR));
			boolean includeEcoreSourceLocationsAttr = !"false".equalsIgnoreCase(context.get(PROP_INCLUDE_ECORE_SOURCE_LOCATIONS_ATTR));

			// Precedence: the lineEndings generate attribute always wins, the Eclipse
			// project preference (.settings) overrides the system default
			String lineEndings = context.get(PROP_LINE_ENDINGS);
			String lineDelimiter;
			if (lineEndings == null) {
				lineDelimiter = getEclipseProjectLineDelimiter(context.getBase());
				info("Line endings configured: " + (lineDelimiter == null
						? "system (default)"
						: describeLineDelimiter(lineDelimiter) + " (from " + ECLIPSE_RUNTIME_PREFS_PATH + ")"));
			} else if ("system".equalsIgnoreCase(lineEndings)) {
				lineDelimiter = null;
				info("Line endings configured: system");
			} else if ("lf".equalsIgnoreCase(lineEndings)) {
				lineDelimiter = "\n";
				info("Line endings configured: lf");
			} else if ("crlf".equalsIgnoreCase(lineEndings)) {
				lineDelimiter = "\r\n";
				info("Line endings configured: crlf");
			} else {
				return Optional.of("Unsupported lineEndings value '" + lineEndings + "'. Supported values are: system, lf, crlf");
			}

			File genmodelFile = new File(context.getBase(), genmodel);

			if(!genmodelFile.exists()) {
				return Optional.of("No genmodel found at " + genmodelFile.getPath());
			}

			Map<Container, Map<String, String>> refModels = extractedLocationsWithCap(context.getProject().getBuildpath());
			Project project = (Project) context.getParent();
			Iterator<String> iterator = project.getBsns().iterator();
			String bsn = iterator.hasNext() ? iterator.next() : context.getParent()
					.toString();
			return doGenerate(genFolder, genmodel,
					refModels,
					context.getBase(),
					bsn, genmodelLocation,
					includeGenModelAttr, includeGenModelSourceLocationsAttr,
					includeEcoreAttr, includeEcoreSourceLocationsAttr,
					lineDelimiter);
		} catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintWriter print = new PrintWriter(baos);
			print.println("Something went wrong: " + e.getMessage());
			e.printStackTrace(print);
			print.close();
			String error = new String(baos.toByteArray());
			baos.close();
			error(error);
			return Optional.of(error);
		} finally {
			closeLog();
		}
	}

	/**
	 * org.eclipse.emf.ecore.generated_package;class="org.eclipse.fennec.emf.osgi.example.model.basic.BasicPackage";uri="http://gecko.org/example/model/basic";genModel="/model/basic.genmodel";sourceLocations="other/main/resources/model/basic.genmodel,org.eclipse.fennec.emf.osgi.example.model.basic/other/main/resources/model/basic.genmodel"
	 * A bundle carrying multiple EPackages provides one such capability per EPackage; all of them contribute their locations.
	 * @param buildpath the buildpath containers
	 * @return the resolvable model locations per container
	 * @throws Exception
	 */
	private Map<Container, Map<String,String>> extractedLocationsWithCap(Collection<Container> buildpath)
			throws Exception {
		Map<Container, Map<String, String>> refModels = new HashMap<>();
		for(Container c : buildpath) {
			File f = c.getFile();
			List<Attrs> capabilities = List.of();
			if(!f.isDirectory()) {
				Domain domain = Domain.domain(c.getManifest());
				capabilities = generatedPackageCapabilities(domain.getProvideCapability());
			}
			Map<String, String> result = new HashMap<>();
			// scan the jar for ecore files unless every capability already names its ecore location
			boolean checkForEcore = capabilities.isEmpty();
			for (Attrs attrs : capabilities) {
				if (!mergeCapabilityLocations(attrs, result)) {
					checkForEcore = true;
				}
			}
			boolean scanForEcore = checkForEcore;
			try (Jar jar = new Jar(f)){
				jar.getResourceNames(s -> (scanForEcore && s.endsWith(".ecore")) || s.endsWith(".genmodel") ||  s.endsWith(".uml"))
					.forEach(s -> result.put(s, s));
			}
			refModels.put(c, result);
		}
		return refModels;
	}

	/**
	 * Collects the attributes of all {@link EPackage#NAMESPACE} capabilities, including the
	 * duplicate keys ({@code <namespace>~}, {@code <namespace>~~}, ...) bnd uses to store
	 * multiple capabilities of the same namespace in {@link Parameters}.
	 * @param provideCapability the parsed Provide-Capability header
	 * @return the attributes of all generated_package capabilities
	 */
	private static List<Attrs> generatedPackageCapabilities(Parameters provideCapability) {
		List<Attrs> capabilities = new ArrayList<>();
		provideCapability.forEach((namespace, attrs) -> {
			if (EPackage.NAMESPACE.equals(Processor.removeDuplicateMarker(namespace))) {
				capabilities.add(attrs);
			}
		});
		return capabilities;
	}

	/**
	 * Merges the model locations of one generated_package capability into the container's location map.
	 * @param attrs the capability attributes
	 * @param result the location map to merge into
	 * @return <code>true</code> if the capability named its ecore location
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private static boolean mergeCapabilityLocations(Attrs attrs, Map<String, String> result) throws Exception {
		CapabilityBuilder builder = new CapabilityBuilder(EPackage.NAMESPACE);
		builder.addAttributesOrDirectives(attrs);
		Capability capability = builder.synthetic();
		String genModelLocation = (String) capability.getAttributes().get("genModel");
		if (genModelLocation != null) {
			List<String> sourceLocations = (List<String>) capability.getAttributes().get("genModelSourceLocations");
			if(sourceLocations != null) {
				sourceLocations.forEach(l -> result.put(l, genModelLocation));
			}
			result.put(genModelLocation, genModelLocation);
		}
		String ecoreLocation = (String) capability.getAttributes().get("ecore");
		if(ecoreLocation == null) {
			return false;
		}
		List<String> ecoreSourceLocations = (List<String>) capability.getAttributes().get("ecoreSourceLocations");
		if(ecoreSourceLocations != null) {
			ecoreSourceLocations.forEach(l -> result.put(l, ecoreLocation));
		}
		result.put(ecoreLocation, ecoreLocation);
		String uri = (String) capability.getAttributes().get("uri");
		if(uri != null) {
			result.put(uri, ecoreLocation);
		}
		return true;
	}

	/**
	 * Reads the Eclipse project specific line delimiter preference
	 * ("New text file line delimiter" in the project's resource settings) from
	 * <code>.settings/org.eclipse.core.runtime.prefs</code>.
	 *
	 * @param base the project directory
	 * @return the configured line delimiter or <code>null</code> if the preference
	 *         file or the preference is absent or its value is not a valid delimiter
	 */
	private static String getEclipseProjectLineDelimiter(File base) {
		File prefsFile = new File(base, ECLIPSE_RUNTIME_PREFS_PATH);
		if (!prefsFile.isFile()) {
			return null;
		}
		Properties prefs = new Properties();
		try (InputStream in = new FileInputStream(prefsFile)) {
			prefs.load(in);
		} catch (IOException e) {
			error("Could not read " + prefsFile.getPath() + ", ignoring the project line delimiter preference", e);
			return null;
		}
		String lineDelimiter = prefs.getProperty(PREF_LINE_SEPARATOR);
		if (lineDelimiter == null) {
			return null;
		}
		if (!"\n".equals(lineDelimiter) && !"\r\n".equals(lineDelimiter) && !"\r".equals(lineDelimiter)) {
			error("Ignoring invalid " + PREF_LINE_SEPARATOR + " value in " + prefsFile.getPath());
			return null;
		}
		return lineDelimiter;
	}

	private static String describeLineDelimiter(String lineDelimiter) {
		switch (lineDelimiter) {
		case "\n": return "lf";
		case "\r\n": return "crlf";
		case "\r": return "cr";
		default: return "unknown";
		}
	}

	/**
	 *
	 */
	private static void closeLog() {
		PrintStream writer = logWriter;
		if(writer != null) {
			logWriter = null;
			writer.close();
		}
	}

	private void configureEMFGenerator(org.eclipse.emf.codegen.ecore.generator.Generator gen) {
		info("Configuring Jet");
		gen.getAdapterFactoryDescriptorRegistry().addDescriptor(GenModelPackage.eNS_URI, BNDGeneratorAdapterFactory.DESCRIPTOR);
	}

	private void configureEMF(ResourceSet resourceSet, Map<Container, Map<String, String>> refModels, String bsn, File base) {

		GenModelPackageImpl.init();
		GenModelFactoryImpl.init();
		org.eclipse.uml2.codegen.ecore.genmodel.impl.GenModelPackageImpl.init();

		resourceSet.getURIConverter().getURIHandlers().add(0, new ResourceUriHandler(refModels, bsn, base));
		resourceSet.getResourceFactoryRegistry().getContentTypeToFactoryMap().put(GenModelPackage.eCONTENT_TYPE, new XMIResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getContentTypeToFactoryMap().put("application/xmi", new XMIResourceFactoryImpl());
	}

	/**
	 * @param output
	 * @param genmodelPath
	 * @param genmodelFile
	 * @param refModels
	 * @param genmodelLocation
	 * @param string
	 * @param file
	 * @param includeGenModelAttr whether to include genModel in @EPackage annotation
	 * @param includeGenModelSourceLocationsAttr whether to include genModelSourceLocations in @EPackage annotation
	 * @param includeEcoreAttr whether to include ecore in @EPackage annotation
	 * @param includeEcoreSourceLocationsAttr whether to include ecoreSourceLocations in @EPackage annotation
	 * @param lineDelimiter the line delimiter to use for generated files, or <code>null</code> for the EMF default behavior
	 * @return
	 * @throws IOException
	 */
	protected Optional<String> doGenerate(String output, String genmodelPath, Map<Container, Map<String, String>> refModels, File base, String bsn, String genmodelLocation,
			boolean includeGenModelAttr, boolean includeGenModelSourceLocationsAttr, boolean includeEcoreAttr, boolean includeEcoreSourceLocationsAttr,
			String lineDelimiter) {
		info("Running for genmodel " + genmodelPath + " in " + base.getAbsolutePath()); 
		ResourceSet resourceSet = new ResourceSetImpl();
		try {
			configureEMF(resourceSet, refModels, bsn, base);
			URI genModelUri = URI.createURI("resource://" + bsn + "/" + genmodelPath);
			
			info("Loading " + genModelUri.toString());
			
			Resource resource = resourceSet.getResource(genModelUri, true);
			
			if(!resource.getErrors().isEmpty()) {
				return Optional.of(resource.getErrors().get(0).toString());
			}
			
			GenModel genModel = (GenModel) resource.getContents().get(0);
			info("Resolving all Models");
			EcoreUtil.resolveAll(genModel);
			Diagnostic genModelDiagnostic = Diagnostician.INSTANCE.validate(genModel);
			if (genModelDiagnostic.getSeverity() != Diagnostic.OK) {
				error("Genmodel is invalid");
				printResult(genModelDiagnostic, "");
				return Optional.empty();
			}
			org.eclipse.emf.codegen.ecore.generator.Generator gen = new org.eclipse.emf.codegen.ecore.generator.Generator();
			configureEMFGenerator(gen);
			
			String modelDirectory = "/" + bsn + (output.startsWith("/") ? "" : "/") + output;
			
			info("Setting modelDirectory" + modelDirectory);
			
			genModel.setModelDirectory(modelDirectory);
			gen.setInput(genModel);
			
			
			Map<String, Object> props = new HashMap<>();
			// Only set path properties when corresponding config is true
			// The template will include annotation attributes only when these values are present
			if (includeGenModelAttr || includeEcoreAttr || includeEcoreSourceLocationsAttr) {
				props.put(FennecEmfGenerator.ORIGINAL_GEN_MODEL_PATH, genmodelPath);
			}
			if (includeGenModelSourceLocationsAttr) {
				props.put(FennecEmfGenerator.ORIGINAL_GEN_MODEL_PATHS_EXTRA, Arrays.asList(base.getName() + "/" + genmodelPath));
			}
			props.put(FennecEmfGenerator.INCLUDE_GEN_MODEL_FOLDER, genmodelLocation);
			// Pass boolean flags to control annotation attributes independently
			props.put("includeGenModelAttr", includeGenModelAttr);
			props.put("includeEcoreAttr", includeEcoreAttr);
			props.put("includeEcoreSourceLocationsAttr", includeEcoreSourceLocationsAttr);
			if (lineDelimiter != null) {
				props.put(LINE_DELIMITER, lineDelimiter);
			}
			gen.getOptions().data = new Object[] {props};
			
			genModel.setCanGenerate(true);
			genModel.setUpdateClasspath(false);
			
			info("Starting generator run");
			try {
				Diagnostic diagnostic = gen.generate(genModel, GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE, CodeGenUtil.EclipseUtil.createMonitor(new LoggingProgressMonitor(), 1));
				printResult(diagnostic);
				if(diagnostic.getSeverity() != Diagnostic.OK) {
					return Optional.of(diagnostic.toString());
				} 
			} catch (Exception e) {
				String message = "An error appeared while generating: " + e.getMessage();
				error(message, e);
				return Optional.of(message);
			}
		} finally {
			resourceSet.getResources().forEach(Resource::unload);
			resourceSet.getResources().clear();
		}
		return Optional.empty();
	}

	/**
	 * @param diagnostic
	 */
	private void printResult(Diagnostic diagnostic) {
		printResult(diagnostic, ""); //$NON-NLS-1$
	}
	private void printResult(Diagnostic diagnostic, String prefix) {
		if(diagnostic.getSeverity() != Diagnostic.OK) {
			error(prefix + diagnostic.getMessage() + " - " + diagnostic.getSource()); //$NON-NLS-1$
			if(diagnostic.getException() != null) {
				error(prefix, diagnostic.getException());
				if(diagnostic.getException() instanceof NullPointerException npe && npe.getStackTrace().length > 0) {
					StackTraceElement stackTraceElement = npe.getStackTrace()[0];
					if(stackTraceElement.getClassName().equals(GenModelImpl.class.getName()) && stackTraceElement.getMethodName().equals("setImportManager")) {
						String message = prefix + "|-> Nullpointer Exception while setting Import Manager on the Genmodel indicates that the genmodel may need to be reloaded. This usually happens when a referenced Genmodel can't be loaded.";
						error(message);
					}
				}
				error("");
			}
		}
		diagnostic.getChildren().forEach(c -> printResult(c, prefix + "  "));
	}

	public static class LoggingProgressMonitor implements IProgressMonitor{

		private String name;


		@Override
		public void beginTask(String name, int totalWork) {
			info("beginTask " + name);
			this.name = name;
		}


		@Override
		public void done() {
			info("done");
		}


		@Override
		public void internalWorked(double work) {
			info("internally worked " + work + " on " + name);
		}


		@Override
		public boolean isCanceled() {
			return false;
		}


		@Override
		public void setCanceled(boolean value) {
			info(name + " cancled");
		}


		@Override
		public void setTaskName(String name) {
			this.name = name;

		}


		@Override
		public void subTask(String name) {
			info("subtask " + name);
		}


		@Override
		public void worked(int work) {
			info(" worked " + work + " on " + name);
		}

	}

}