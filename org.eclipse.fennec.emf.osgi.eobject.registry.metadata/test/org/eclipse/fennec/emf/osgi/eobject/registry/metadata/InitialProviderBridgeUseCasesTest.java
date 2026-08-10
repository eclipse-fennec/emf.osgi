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
package org.eclipse.fennec.emf.osgi.eobject.registry.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.fennec.emf.osgi.annotation.provide.EPackage.FINGERPRINT_ATTRIBUTE;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistries;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryEntry;
import org.eclipse.fennec.emf.osgi.eobject.registry.EObjectRegistryWriter;
import org.eclipse.fennec.emf.osgi.eobject.registry.FileEObjectProvider;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.eclipse.fennec.emf.osgi.metadata.MetadataServices;
import org.eclipse.fennec.emf.osgi.metadata.MetadataWhiteboard;
import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The interplay of the three pieces, scenario by scenario: a <b>file initial provider</b>
 * loads authored mapping content, the <b>registry</b> holds it locally and resiliently,
 * the <b>metadata bridge</b> makes it answerable from an {@link EClass}. Each test is
 * one deployment situation that occurs in practice; together they are the worked
 * examples of the user guide.
 * <p>
 * The underlying problem: runtime code (a codec, a sensinact mapper) holds an EClass
 * and needs the authored content for it - but the content lives in files or a remote
 * model atlas, model bundles and content sources start in no guaranteed order, models
 * get re-registered with new fingerprints, and the network may be down. Classic
 * per-object service publication makes every one of these a special case; this setup
 * answers all of them with two replays (listener replay for late bridges, handler
 * replay for late models) and one gated publication.
 */
public class InitialProviderBridgeUseCasesTest {

	private static final String SENSORS_NS_URI = "http://fennec.eclipse.org/test/sensors/1.0";
	private static final String MAPPINGS_NS_URI = "http://fennec.eclipse.org/test/mappings/1.0";
	private static final String TYPE_ID = "sensinact.mapping";

	@TempDir
	Path tempDir;

	// the domain model: what the runtime holds an EClass of
	private EPackage sensorsPackage;
	private EClass temperatureSensor;
	private EClass humiditySensor;

	// the content model: what the authored files contain
	private EPackage mappingsPackage;
	private EClass mappingClass;
	private EAttribute midAttribute;
	private EAttribute sensorClassAttribute;
	private EAttribute targetAttribute;

	private MetadataWhiteboard whiteboard;
	private AspectAnchorResolver domainResolver;

	@BeforeEach
	public void setUp() {
		sensorsPackage = EcoreFactory.eINSTANCE.createEPackage();
		sensorsPackage.setName("sensors");
		sensorsPackage.setNsPrefix("sensors");
		sensorsPackage.setNsURI(SENSORS_NS_URI);
		temperatureSensor = eClass("TemperatureSensor");
		humiditySensor = eClass("HumiditySensor");
		sensorsPackage.getEClassifiers().add(temperatureSensor);
		sensorsPackage.getEClassifiers().add(humiditySensor);

		mappingsPackage = EcoreFactory.eINSTANCE.createEPackage();
		mappingsPackage.setName("mappings");
		mappingsPackage.setNsPrefix("mappings");
		mappingsPackage.setNsURI(MAPPINGS_NS_URI);
		mappingClass = eClass("Mapping");
		midAttribute = attribute(mappingClass, "mid");
		sensorClassAttribute = attribute(mappingClass, "sensorClass");
		targetAttribute = attribute(mappingClass, "target");
		mappingsPackage.getEClassifiers().add(mappingClass);

		whiteboard = MetadataServices.createWhiteboard();
		// the domain's anchor resolution: a mapping names the sensor class it belongs to -
		// the sensinact analog is ProviderMapping.getProviderClasses()
		domainResolver = entry -> {
			String className = (String) entry.object().eGet(sensorClassAttribute);
			EClassifier classifier = className == null ? null : sensorsPackage.getEClassifier(className);
			return classifier instanceof EClass anchor ? List.of(anchor) : List.of();
		};
	}

	private EClass eClass(String name) {
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		eClass.setName(name);
		return eClass;
	}

	private EAttribute attribute(EClass owner, String name) {
		EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();
		attribute.setName(name);
		attribute.setEType(EcorePackage.Literals.ESTRING);
		owner.getEStructuralFeatures().add(attribute);
		return attribute;
	}

	private EObject mapping(String mid, String sensorClassName, String target) {
		EObject mapping = EcoreUtil.create(mappingClass);
		mapping.eSet(midAttribute, mid);
		mapping.eSet(sensorClassAttribute, sensorClassName);
		mapping.eSet(targetAttribute, target);
		return mapping;
	}

	private ResourceSet resourceSet() {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		resourceSet.getPackageRegistry().put(MAPPINGS_NS_URI, mappingsPackage);
		return resourceSet;
	}

	private void writeFixture(String fileName, EObject... mappings) throws Exception {
		Path file = tempDir.resolve(fileName);
		Resource resource = resourceSet().createResource(URI.createFileURI(file.toAbsolutePath().toString()));
		resource.getContents().addAll(List.of(mappings));
		resource.save(Map.of());
	}

	private FileEObjectProvider fileProvider() {
		return new FileEObjectProvider("mapping-files", resourceSet(), List.of(tempDir),
				FileEObjectProvider.featureKeys("mid"));
	}

	/** Wires registry + bridge the way the OSGi components do, in the given order. */
	private EObjectRegistryWriter registryWithBridge() {
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("sensinact-mappings", fileProvider());
		RegistryMetadataBridge bridge = new RegistryMetadataBridge(whiteboard, TYPE_ID, domainResolver);
		whiteboard.addMetadataHandler(bridge);
		writer.getRegistry().addListener(bridge);
		return writer;
	}

	/**
	 * <b>Use case 1 - the normal day:</b> the model bundle is up, then the registry
	 * loads its authored mapping files. Runtime code holding the sensor EClass finds
	 * the mapping through one uniform lookup - no per-object services, no whiteboard of
	 * content services, no static registry.
	 */
	@Test
	public void testModelFirstFileContentAnchorsAtTheSensorClass() throws Exception {
		writeFixture("mappings.xmi", mapping("temp-1", "TemperatureSensor", "temperature"));
		whiteboard.registerPackage(sensorsPackage);

		registryWithBridge();

		AspectEntry aspect = whiteboard.getClassAspect(temperatureSensor, TYPE_ID).orElseThrow();
		assertThat(aspect.getContent().eGet(targetAttribute)).isEqualTo("temperature");
	}

	/**
	 * <b>Use case 2 - the late model bundle:</b> content is loaded before the sensor
	 * model registers (DS gives no start ordering - the class of bug that silently
	 * loses content, cf. issue #71). The handler replay contributes the aspects the
	 * moment the model's tree is built: nothing is lost, no code had to care about
	 * ordering.
	 */
	@Test
	public void testContentFirstLateModelBundleGetsAspectsOnRegistration() throws Exception {
		writeFixture("mappings.xmi", mapping("temp-1", "TemperatureSensor", "temperature"));

		registryWithBridge();
		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).as("model not registered yet").isEmpty();

		whiteboard.registerPackage(sensorsPackage);

		AspectEntry aspect = whiteboard.getClassAspect(temperatureSensor, TYPE_ID).orElseThrow();
		assertThat(aspect.getContent().eGet(targetAttribute)).isEqualTo("temperature");
	}

	/**
	 * <b>Use case 3 - the model version bump:</b> a second, diverging version of the
	 * sensor model registers (new fingerprint, same nsURI) while the first stays live.
	 * Metadata identity is the fingerprint, so the new version starts with an empty
	 * tree - without the bridge's handler role its aspects would silently be missing.
	 * Both versions answer the lookup.
	 */
	@Test
	public void testModelVersionBumpAspectsSurviveOnTheNewFingerprintTree() throws Exception {
		writeFixture("mappings.xmi", mapping("temp-1", "TemperatureSensor", "temperature"));
		whiteboard.registerPackage(sensorsPackage);
		registryWithBridge();

		EPackage sensorsV2 = EcoreUtil.copy(sensorsPackage);
		EClass temperatureV2 = (EClass) sensorsV2.getEClassifier("TemperatureSensor");
		attribute(temperatureV2, "accuracy");
		whiteboard.registerPackage(sensorsV2);

		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).as("v1 keeps its aspect").isPresent();
		AspectEntry v2Aspect = whiteboard.getClassAspect(temperatureV2, TYPE_ID).orElseThrow();
		assertThat(v2Aspect.getContent().eGet(targetAttribute)).isEqualTo("temperature");
	}

	/**
	 * <b>Use case 3b - the derived artifact at a version bump:</b> next to the authored,
	 * version-independent file content sits an artifact a compiler <em>derived</em> from one
	 * package instance - compiled OCL is the real case, and it holds the
	 * {@code EStructuralFeature} instances it resolved against. Such an entry names its
	 * version through {@code emf.fingerprint} and must stay on it: a copy on the new
	 * version's tree would navigate the old package's features (issue #81). The
	 * version-independent mapping keeps spanning both versions, unchanged.
	 */
	@Test
	public void testDerivedContentPinnedToItsVersionDoesNotFollowTheBump() throws Exception {
		writeFixture("mappings.xmi", mapping("hum-1", "HumiditySensor", "humidity"));
		String fingerprintV1 = whiteboard.registerPackage(sensorsPackage).orElseThrow().getModelFingerprint();
		EObjectRegistryWriter writer = registryWithBridge();
		writer.put("ocl-compiler", "temp-compiled", mapping("temp-compiled", "TemperatureSensor", "temperature"),
				Map.of(FINGERPRINT_ATTRIBUTE, fingerprintV1));

		EPackage sensorsV2 = EcoreUtil.copy(sensorsPackage);
		EClass temperatureV2 = (EClass) sensorsV2.getEClassifier("TemperatureSensor");
		EClass humidityV2 = (EClass) sensorsV2.getEClassifier("HumiditySensor");
		attribute(temperatureV2, "accuracy");
		whiteboard.registerPackage(sensorsV2);

		assertThat(whiteboard.getClassAspect(humidityV2, TYPE_ID)).as("authored content spans versions").isPresent();
		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).as("the derived artifact keeps its version")
				.isPresent();
		assertThat(whiteboard.getClassAspect(temperatureV2, TYPE_ID)).as("and is not copied onto the new one")
				.isEmpty();
	}

	/**
	 * <b>Use case 3c - the derived artifact ahead of its model:</b> the compiler was run
	 * against a version that is not deployed yet (a staged rollout, an atlas that already
	 * knows the next model). The entry names that version, waits, and lands the moment it
	 * registers - the handler replay of use case 2, narrowed to one version instead of all
	 * of them.
	 */
	@Test
	public void testDerivedContentWaitsForTheVersionItNames() throws Exception {
		writeFixture("mappings.xmi", mapping("hum-1", "HumiditySensor", "humidity"));
		EPackage sensorsV2 = EcoreUtil.copy(sensorsPackage);
		EClass temperatureV2 = (EClass) sensorsV2.getEClassifier("TemperatureSensor");
		attribute(temperatureV2, "accuracy");
		String fingerprintV2 = FingerprintHelper.fingerprint(sensorsV2);
		whiteboard.registerPackage(sensorsPackage);

		EObjectRegistryWriter writer = registryWithBridge();
		writer.put("ocl-compiler", "temp-compiled", mapping("temp-compiled", "TemperatureSensor", "temperature"),
				Map.of(FINGERPRINT_ATTRIBUTE, fingerprintV2));
		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).as("v2 is not deployed yet").isEmpty();

		whiteboard.registerPackage(sensorsV2);

		assertThat(whiteboard.getClassAspect(temperatureV2, TYPE_ID)).as("lands on the version it names").isPresent();
		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).as("never on the other one").isEmpty();
	}

	/**
	 * <b>Use case 4 - the dynamic source updates:</b> a model-atlas style client pushes
	 * a changed mapping for a key the files initially provided. The registry stays the
	 * source of truth, the aspect snapshot follows - and an unchanged re-push (the
	 * atlas ETag cache returns the identical instance) causes no aspect churn at all.
	 */
	@Test
	public void testDynamicSourceUpdateAspectSnapshotFollowsTheRegistry() throws Exception {
		writeFixture("mappings.xmi", mapping("temp-1", "TemperatureSensor", "temperature"));
		whiteboard.registerPackage(sensorsPackage);
		EObjectRegistryWriter writer = registryWithBridge();

		EObject fromAtlas = mapping("temp-1", "TemperatureSensor", "temperature-corrected");
		writer.sync("atlas", List.of(new EObjectRegistryEntry("temp-1", fromAtlas, "atlas", null)));

		AspectEntry updated = whiteboard.getClassAspect(temperatureSensor, TYPE_ID).orElseThrow();
		assertThat(updated.getContent().eGet(targetAttribute)).isEqualTo("temperature-corrected");

		// unchanged re-sync: identical instance, no event, the identical aspect stays
		writer.sync("atlas", List.of(new EObjectRegistryEntry("temp-1", fromAtlas, "atlas", null)));
		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID).orElseThrow()).isSameAs(updated);
	}

	/**
	 * <b>Use case 5 - the source disappears:</b> the atlas reports its content gone (or
	 * simply stops syncing - then nothing happens at all). Only atlas-owned entries and
	 * their aspects go; the file-provided content is untouched. This is the resilience
	 * requirement: an unreachable remote source never costs the locally held mappings.
	 */
	@Test
	public void testDynamicSourceGoneFileContentStays() throws Exception {
		writeFixture("mappings.xmi", mapping("temp-1", "TemperatureSensor", "temperature"));
		whiteboard.registerPackage(sensorsPackage);
		EObjectRegistryWriter writer = registryWithBridge();

		writer.sync("atlas",
				List.of(new EObjectRegistryEntry("hum-1", mapping("hum-1", "HumiditySensor", "humidity"), "atlas",
						null)));
		assertThat(whiteboard.getClassAspect(humiditySensor, TYPE_ID)).isPresent();

		writer.sync("atlas", List.of());

		assertThat(whiteboard.getClassAspect(humiditySensor, TYPE_ID)).as("atlas content gone").isEmpty();
		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).as("file content stays").isPresent();
		assertThat(writer.getRegistry().get("temp-1")).isPresent();
	}

	/**
	 * <b>Use case 6 - the late bridge:</b> registry and model are long up when the
	 * bridge (or a second bridge with another type id) arrives. The listener replay
	 * hands it the complete current content - late wiring is indistinguishable from
	 * early wiring.
	 */
	@Test
	public void testLateBridgeReplayAttachesExistingContent() throws Exception {
		writeFixture("mappings.xmi", mapping("temp-1", "TemperatureSensor", "temperature"));
		whiteboard.registerPackage(sensorsPackage);
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("sensinact-mappings", fileProvider());

		RegistryMetadataBridge lateBridge = new RegistryMetadataBridge(whiteboard, TYPE_ID, domainResolver);
		whiteboard.addMetadataHandler(lateBridge);
		writer.getRegistry().addListener(lateBridge);

		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).isPresent();
	}

	/**
	 * <b>Use case 7 - one mapping, many anchors:</b> a single authored mapping applies
	 * to several sensor classes (the sensinact {@code ProviderMapping.getProviderClasses()}
	 * shape). Every anchor answers; removing the entry clears every anchor.
	 */
	@Test
	public void testOneMappingAnchorsAtManySensorClasses() throws Exception {
		whiteboard.registerPackage(sensorsPackage);
		AspectAnchorResolver bothSensors = entry -> List.of(temperatureSensor, humiditySensor);
		EObjectRegistryWriter writer = EObjectRegistries.createRegistry("sensinact-mappings");
		RegistryMetadataBridge bridge = new RegistryMetadataBridge(whiteboard, TYPE_ID, bothSensors);
		whiteboard.addMetadataHandler(bridge);
		writer.getRegistry().addListener(bridge);

		writer.put("files", "combo-1", mapping("combo-1", null, "combined"), null);
		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).isPresent();
		assertThat(whiteboard.getClassAspect(humiditySensor, TYPE_ID)).isPresent();

		writer.remove("files", "combo-1");
		assertThat(whiteboard.getClassAspect(temperatureSensor, TYPE_ID)).isEmpty();
		assertThat(whiteboard.getClassAspect(humiditySensor, TYPE_ID)).isEmpty();
	}
}
