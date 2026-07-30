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
package org.eclipse.fennec.emf.osgi.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
import org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService;
import org.eclipse.fennec.emf.osgi.fingerprint.util.FingerprintHelper;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.junit.jupiter.api.Test;

/**
 * What the non-OSGi bootstrap has to deliver: a whiteboard that is fully wired on return.
 * <p>
 * The point of the factory is that a caller outside OSGi never touches the implementation
 * class, so what is asserted here is the wiring DS would otherwise do - the mandatory
 * {@link org.eclipse.fennec.emf.osgi.fingerprint.FingerprintService FingerprintService} is
 * in place, an index answers, and handlers passed in see the very first registration rather
 * than only a replay.
 */
class MetadataServicesTest {

	private static final String NS_URI = "http://fennec.eclipse.org/test/metadata";

	@Test
	void testWhiteboardIsUsableWithoutFurtherWiring() {
		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(new DefaultFingerprintService());

		PackageMetadata metadata = whiteboard.registerPackage(personPackage("name")).orElseThrow();

		assertThat(metadata.getModelFingerprint()).as("the fingerprint service was wired").isNotBlank();
		assertThat(whiteboard.getPackageMetadataByFingerprint(metadata.getModelFingerprint())).contains(metadata);
	}

	@Test
	void testDefaultIndexAnswersFromTheStart() {
		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(new DefaultFingerprintService());
		whiteboard.registerPackage(personPackage("name"));

		assertThat(whiteboard.getIndexReader()).as("an in-memory default index is already bound").isPresent();
		assertThat(whiteboard.getClassMetadataByName("Person", NS_URI)).isPresent();
	}

	@Test
	void testHandlersSeeTheFirstRegistration() {
		RecordingHandler handler = new RecordingHandler();
		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(new DefaultFingerprintService(), handler);

		assertThat(handler.seen).as("nothing registered yet").isEmpty();

		PackageMetadata metadata = whiteboard.registerPackage(personPackage("name")).orElseThrow();

		assertThat(whiteboard.getMetadataHandlers()).containsExactly(handler);
		assertThat(handler.seen).as("the handler was in place before the package arrived").containsExactly(metadata);
	}

	@Test
	void testAllHandlersAreRegisteredInOrder() {
		RecordingHandler first = new RecordingHandler();
		RecordingHandler second = new RecordingHandler();

		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(new DefaultFingerprintService(), first,
				second);

		assertThat(whiteboard.getMetadataHandlers()).containsExactly(first, second);
	}

	@Test
	void testNoHandlersIsFine() {
		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(new DefaultFingerprintService());

		assertThat(whiteboard.getMetadataHandlers()).isEmpty();
	}

	@Test
	void testNullHandlerEntriesAreIgnored() {
		RecordingHandler handler = new RecordingHandler();

		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(new DefaultFingerprintService(), null,
				handler, null);

		assertThat(whiteboard.getMetadataHandlers()).containsExactly(handler);
	}

	@Test
	void testMissingFingerprintServiceFailsImmediately() {
		// The cast picks the explicit overload: a bare null is ambiguous between the two
		// varargs signatures. Passing no service at all is the parameterless overload, which
		// is a different contract - see below.
		assertThatNullPointerException()
				.as("an explicitly passed null is a caller error - failing here beats failing on first use")
				.isThrownBy(() -> MetadataServices.createWhiteboard((FingerprintService) null))
				.withMessageContaining("fingerprintService");
	}

	// ---- parameterless overload (issue #67) -------------------------------------------

	@Test
	void testDefaultFingerprintServiceIsWiredWithoutAParameter() {
		// The whole point: a caller with no opinion about model identity gets a usable
		// whiteboard without naming the implementation - which lives in a private package.
		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard();

		PackageMetadata metadata = whiteboard.registerPackage(personPackage("name")).orElseThrow();

		assertThat(metadata.getModelFingerprint()).as("a default fingerprint service was wired").isNotBlank();
		assertThat(whiteboard.getPackageMetadataByFingerprint(metadata.getModelFingerprint())).contains(metadata);
	}

	@Test
	void testDefaultMatchesTheExplicitlyPassedService() {
		// The default has to be the same identity scheme the registry components emit, or a
		// whiteboard built outside OSGi would key models differently than the OSGi path.
		EPackage ePackage = personPackage("name");

		String withDefault = MetadataServices.createWhiteboard().registerPackage(ePackage).orElseThrow()
				.getModelFingerprint();
		String explicit = MetadataServices.createWhiteboard(FingerprintHelper.getDefaultFingerprintService())
				.registerPackage(ePackage).orElseThrow().getModelFingerprint();

		assertThat(withDefault).isEqualTo(explicit);
	}

	@Test
	void testHandlersWorkWithoutAServiceParameter() {
		RecordingHandler handler = new RecordingHandler();

		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard(handler);
		PackageMetadata metadata = whiteboard.registerPackage(personPackage("name")).orElseThrow();

		assertThat(whiteboard.getMetadataHandlers()).containsExactly(handler);
		assertThat(handler.seen).containsExactly(metadata);
	}

	@Test
	void testFingerprintServiceIsReplaceableThroughTheInterface() {
		// "Default, overridable" rather than a dead end: the collaborator is settable on the
		// interface, so a caller that does have an opinion can install it after the fact.
		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard();
		whiteboard.setFingerprintService(new PrefixingFingerprintService());

		PackageMetadata metadata = whiteboard.registerPackage(personPackage("name")).orElseThrow();

		assertThat(metadata.getModelFingerprint()).startsWith(PrefixingFingerprintService.PREFIX);
	}

	@Test
	void testNullFingerprintServiceOnTheInterfaceIsIgnored() {
		// Mandatory collaborator, so there is no unset: the previous one stays in place.
		MetadataWhiteboard whiteboard = MetadataServices.createWhiteboard();
		whiteboard.setFingerprintService(null);

		assertThat(whiteboard.registerPackage(personPackage("name")).orElseThrow().getModelFingerprint())
				.as("the default survived the null").isNotBlank();
	}

	private static EPackage personPackage(String attributeName) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("test");
		ePackage.setNsPrefix("test");
		ePackage.setNsURI(NS_URI);

		EClass person = EcoreFactory.eINSTANCE.createEClass();
		person.setName("Person");
		ePackage.getEClassifiers().add(person);

		EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();
		attribute.setName(attributeName);
		attribute.setEType(EcorePackage.Literals.ESTRING);
		person.getEStructuralFeatures().add(attribute);

		return ePackage;
	}

	private static final class RecordingHandler implements MetadataHandler {

		private final List<PackageMetadata> seen = new ArrayList<>();

		@Override
		public void onPackageRegistered(PackageMetadata packageMetadata) {
			seen.add(packageMetadata);
		}
	}

	/** Recognizable values, so a replaced service is visible in the resulting tree. */
	private static final class PrefixingFingerprintService implements FingerprintService {

		private static final String PREFIX = "custom:";

		@Override
		public String fingerprint(EPackage ePackage, String... derivationInputs) {
			return PREFIX + ePackage.getNsURI();
		}

		@Override
		public String currentScheme() {
			return "custom";
		}

		@Override
		public Set<String> supportedSchemes() {
			return Set.of("custom");
		}

		@Override
		public String fingerprintInScheme(String scheme, EPackage ePackage, String... derivationInputs) {
			return fingerprint(ePackage, derivationInputs);
		}
	}
}
