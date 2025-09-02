/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 *  
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *  
 * Contributors:
 *       Data In Motion - initial API and implementation
 */
package org.eclipse.fennec.emf.osgi.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.fennec.emf.osgi.ResourceSetFactory;
import org.eclipse.fennec.emf.osgi.example.model.basic.BasicPackage;
import org.eclipse.fennec.emf.osgi.example.model.basic.Person;
import org.eclipse.fennec.emf.osgi.example.model.extended.ExtendedPackage;
import org.eclipse.fennec.emf.osgi.example.model.extended.ExtendedPerson;
import org.eclipse.fennec.emf.osgi.model.info.EMFModelInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.test.assertj.bundle.BundleAssert;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.context.BundleInstallerExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 * 
 * @author jalbert
 * @since 8 Nov 2018
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@ExtendWith(BundleInstallerExtension.class)
public class EMFModelInfoTest {

	@InjectBundleContext
	BundleContext bc;
	private Bundle basicBundle;

	@BeforeEach
	private void start() {
		List<Bundle> bundles = Arrays.asList(bc.getBundles());
		basicBundle = bundles.stream()
				.filter(b -> "org.eclipse.fennec.emf.osgi.example.model.basic".equals(b.getSymbolicName())).findAny()
				.orElse(null);
		BundleAssert.assertThat(basicBundle).isNotNull();
	}

	@AfterEach
	private void stop() throws BundleException {
		basicBundle.start();
	}
	
	/**
	 * Trying to load an instance with a registered {@link EPackage}, then check if
	 * the {@link EClass} can be identified. Than unload the EPackage and expect
	 * that no {@link EClass} can be found
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws BundleException 
	 */
	@Test
	public void testFindEClassByClass(@InjectService(cardinality = 0) ServiceAware<ResourceSetFactory> saRF,
			@InjectService(cardinality = 0) ServiceAware<EMFModelInfo> saMI) throws IOException, InterruptedException, BundleException {
		
		
		EMFModelInfo emfModelInfo = saMI.waitForService(1000);

		Optional<EClassifier> eClassifierForClass = emfModelInfo.getEClassifierForClass(Person.class);
		Optional<EClassifier> eClassifierForClassByName = emfModelInfo.getEClassifierForClass(Person.class.getName());

		EClassifier eClassifierByClass = eClassifierForClass.orElse(null);
		EClassifier eClassifierByName = eClassifierForClassByName.orElse(null);
		assertNotNull(eClassifierByClass);
		assertNotNull(eClassifierByName);
		basicBundle.stop();

		Thread.sleep(1000);
		eClassifierForClass = emfModelInfo.getEClassifierForClass(Person.class);
		eClassifierForClassByName = emfModelInfo.getEClassifierForClass(Person.class.getName());

		eClassifierByClass = eClassifierForClass.orElse(null);
		eClassifierByName = eClassifierForClass.orElse(null);

		assertNull(eClassifierByClass);
		assertNull(eClassifierByName);

	}

	/**
	 * Trying to load an instance with a registered {@link EPackage}, then check if
	 * the {@link EClass} can be identified. Than unload the EPackage and expect
	 * that no {@link EClass} can be found
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws BundleException
	 */
	@Test
	public void testFindEClassByClassName(
			@InjectService(cardinality = 0) ServiceAware<EMFModelInfo> saMI,
			@InjectService(cardinality = 1) ExtendedPackage markerForStart
			)
			throws IOException, InterruptedException, BundleException {
		
		EClass mustExist = markerForStart.getExtendedPerson();
		Assertions.assertThat(mustExist).isNotNull();
		Assertions.assertThat(ExtendedPerson.class).isNotNull();
		EMFModelInfo emfModelInfo = saMI.waitForService(1000);
		List<Bundle> bundles = Arrays.asList(bc.getBundles());

		Bundle basicBundle = bundles.stream()
				.filter(b -> "org.eclipse.fennec.emf.osgi.example.model.basic".equals(b.getSymbolicName())).findAny()
				.orElse(null);
		BundleAssert.assertThat(basicBundle).isNotNull();
		Bundle extBundle = bundles.stream()
				.filter(b -> "org.eclipse.fennec.emf.osgi.example.model.extended".equals(b.getSymbolicName())).findAny()
				.orElse(null);

		BundleAssert.assertThat(extBundle).isNotNull();

		try {
			List<EClass> personHirachy = emfModelInfo.getUpperTypeHierarchyForEClass(getServiceAndRelease(bc, BasicPackage.class).getPerson());
	
			assertNotNull(personHirachy);
			assertEquals(2, personHirachy.size());
	
			extBundle.stop();
			Thread.sleep(1000);
	
			EClass eclass = getServiceAndRelease(bc, BasicPackage.class).getPerson();
			
			List<EClass> personHirachy2 = emfModelInfo.getUpperTypeHierarchyForEClass(eclass);
			System.out.println("");
	
			System.out.println("______ personHirachy2");
			System.out.println(personHirachy2);
			System.out.println("______");
			basicBundle.stop();
			Thread.sleep(1000);
			List<EClass> personHirachy3 = emfModelInfo.getUpperTypeHierarchyForEClass(eclass);
			System.out.println("");
			System.out.println("______ personHirachy3");
			System.out.println(personHirachy3);
			System.out.println("______");
	
			basicBundle.start();
			Thread.sleep(1000);
			List<EClass> personHirachy4 = emfModelInfo.getUpperTypeHierarchyForEClass(getServiceAndRelease(bc, BasicPackage.class).getPerson());
			System.out.println("");
			System.out.println("______ personHirachy 4");
			System.out.println(personHirachy4);
			System.out.println("______");
	
			extBundle.start();
	
			Thread.sleep(1000);
			List<EClass> personHirachy5 = emfModelInfo.getUpperTypeHierarchyForEClass(getServiceAndRelease(bc, BasicPackage.class).getPerson());
			System.out.println("");
			System.out.println("______ personHirachy5");
			System.out.println(personHirachy5);
			System.out.println("______");
	
			assertNotNull(personHirachy2);
	
			assertEquals(1, personHirachy2.size());
	
			assertNotNull(personHirachy3);
			assertEquals(0, personHirachy3.size());
	
			assertNotNull(personHirachy4);
			assertEquals(1, personHirachy4.size());
	
			assertNotNull(personHirachy5);
			assertEquals(2, personHirachy5.size());
		} finally {
			basicBundle.start();
			extBundle.start();
		}

	}

	private <T> T getServiceAndRelease(BundleContext ctx, Class<T> clazz){
		ServiceReference<T> serviceReference = ctx.getServiceReference(clazz);
		assertNotNull(serviceReference);
		T result = ctx.getService(serviceReference);
		ctx.ungetService(serviceReference);
		return result;
	}
	
}
