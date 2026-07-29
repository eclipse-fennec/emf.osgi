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

import java.util.List;

import org.eclipse.fennec.emf.osgi.model.metadata.AspectEntry;
import org.eclipse.fennec.emf.osgi.model.metadata.AttributeMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ClassMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.DiagnosticSeverity;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataDiagnostic;
import org.eclipse.fennec.emf.osgi.model.metadata.MetadataFactory;
import org.eclipse.fennec.emf.osgi.model.metadata.OperationMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.PackageMetadata;
import org.eclipse.fennec.emf.osgi.model.metadata.ParameterMetadata;
import org.junit.jupiter.api.Test;

/**
 * What {@code allDiagnostics} aggregates, and what it deliberately does not.
 * <p>
 * The donor model stopped at features, so a problem found on an operation or a parameter
 * never surfaced at the class - a gap rather than a decision, and nothing depended on it
 * (issue #62). Operations are a first-class attachment point per M8, so their diagnostics
 * belong in the roll-up.
 * <p>
 * Aspect entry diagnostics stay out on purpose: an entry belongs to whoever contributed
 * it, and a problem while building one says nothing about the health of the metadata
 * element carrying it.
 */
class DiagnosticAggregationTest {

	@Test
	void testLeafElementsAggregateOnlyTheirOwn() {
		AttributeMetadata attribute = MetadataFactory.eINSTANCE.createAttributeMetadata();
		attribute.getDiagnostics().add(diagnostic("attribute"));

		ParameterMetadata parameter = MetadataFactory.eINSTANCE.createParameterMetadata();
		parameter.getDiagnostics().add(diagnostic("parameter"));

		assertThat(messages(attribute.getAllDiagnostics())).containsExactly("attribute");
		assertThat(messages(parameter.getAllDiagnostics())).containsExactly("parameter");
	}

	@Test
	void testOperationRollsUpItsParameters() {
		OperationMetadata operation = MetadataFactory.eINSTANCE.createOperationMetadata();
		operation.getDiagnostics().add(diagnostic("operation"));
		ParameterMetadata parameter = MetadataFactory.eINSTANCE.createParameterMetadata();
		parameter.getDiagnostics().add(diagnostic("parameter"));
		operation.getParameters().add(parameter);

		assertThat(messages(operation.getAllDiagnostics()))
				.containsExactlyInAnyOrder("operation", "parameter");
	}

	@Test
	void testClassRollsUpFeaturesAndOperations() {
		ClassMetadata classMetadata = MetadataFactory.eINSTANCE.createClassMetadata();
		classMetadata.getDiagnostics().add(diagnostic("class"));

		AttributeMetadata attribute = MetadataFactory.eINSTANCE.createAttributeMetadata();
		attribute.getDiagnostics().add(diagnostic("feature"));
		classMetadata.getFeatures().add(attribute);

		OperationMetadata operation = MetadataFactory.eINSTANCE.createOperationMetadata();
		operation.getDiagnostics().add(diagnostic("operation"));
		ParameterMetadata parameter = MetadataFactory.eINSTANCE.createParameterMetadata();
		parameter.getDiagnostics().add(diagnostic("parameter"));
		operation.getParameters().add(parameter);
		classMetadata.getOperations().add(operation);

		assertThat(messages(classMetadata.getAllDiagnostics()))
				.as("the operation branch is what the donor model was missing")
				.containsExactlyInAnyOrder("class", "feature", "operation", "parameter");
	}

	@Test
	void testPackageRollsUpTheWholeTree() {
		PackageMetadata packageMetadata = MetadataFactory.eINSTANCE.createPackageMetadata();
		packageMetadata.getDiagnostics().add(diagnostic("package"));

		ClassMetadata classMetadata = MetadataFactory.eINSTANCE.createClassMetadata();
		classMetadata.getDiagnostics().add(diagnostic("class"));
		AttributeMetadata attribute = MetadataFactory.eINSTANCE.createAttributeMetadata();
		attribute.getDiagnostics().add(diagnostic("feature"));
		classMetadata.getFeatures().add(attribute);
		OperationMetadata operation = MetadataFactory.eINSTANCE.createOperationMetadata();
		operation.getDiagnostics().add(diagnostic("operation"));
		classMetadata.getOperations().add(operation);
		packageMetadata.getClasses().add(classMetadata);

		assertThat(messages(packageMetadata.getAllDiagnostics()))
				.containsExactlyInAnyOrder("package", "class", "feature", "operation");
	}

	@Test
	void testAspectEntryDiagnosticsStayWithTheirEntry() {
		PackageMetadata packageMetadata = MetadataFactory.eINSTANCE.createPackageMetadata();
		AspectEntry entry = MetadataFactory.eINSTANCE.createAspectEntry();
		entry.setTypeId("codec");
		entry.getDiagnostics().add(diagnostic("aspect"));
		packageMetadata.getAspects().add(entry);

		assertThat(packageMetadata.getAllDiagnostics())
				.as("a contributor's problem is not the metadata element's problem")
				.isEmpty();
		assertThat(messages(entry.getDiagnostics())).containsExactly("aspect");
	}

	@Test
	void testEmptyTreeAggregatesNothing() {
		PackageMetadata packageMetadata = MetadataFactory.eINSTANCE.createPackageMetadata();
		packageMetadata.getClasses().add(MetadataFactory.eINSTANCE.createClassMetadata());

		assertThat(packageMetadata.getAllDiagnostics()).isEmpty();
	}

	@Test
	void testSeverityDefaultsToWarning() {
		MetadataDiagnostic diagnostic = MetadataFactory.eINSTANCE.createMetadataDiagnostic();

		assertThat(diagnostic.getSeverity()).isEqualTo(DiagnosticSeverity.WARNING);
	}

	private static MetadataDiagnostic diagnostic(String message) {
		MetadataDiagnostic diagnostic = MetadataFactory.eINSTANCE.createMetadataDiagnostic();
		diagnostic.setMessage(message);
		return diagnostic;
	}

	private static List<String> messages(List<MetadataDiagnostic> diagnostics) {
		return diagnostics.stream().map(MetadataDiagnostic::getMessage).toList();
	}
}
