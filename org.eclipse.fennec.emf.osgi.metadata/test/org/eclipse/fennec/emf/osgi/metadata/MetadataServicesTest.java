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

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.fennec.emf.osgi.components.fingerprint.DefaultFingerprintService;
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
		assertThatNullPointerException()
				.as("model identity has no default - failing here beats failing on first use")
				.isThrownBy(() -> MetadataServices.createWhiteboard(null))
				.withMessageContaining("fingerprintService");
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
}
