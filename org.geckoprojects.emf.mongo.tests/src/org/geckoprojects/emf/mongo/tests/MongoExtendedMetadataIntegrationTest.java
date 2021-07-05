/**
 * Copyright (c) 2012 - 2017 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.geckoprojects.emf.mongo.tests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.geckoprojects.emf.core.EMFNamespaces;
import org.geckoprojects.emf.core.ResourceSetConfigurator;
import org.geckoprojects.emf.core.ResourceSetFactory;
import org.geckoprojects.emf.example.model.basic.model.BasicFactory;
import org.geckoprojects.emf.example.model.basic.model.BusinessPerson;
import org.geckoprojects.emf.example.model.basic.model.EmployeeInfo;
import org.geckoprojects.emf.example.model.basic.model.GenderType;
import org.geckoprojects.emf.mongo.Options;
import org.geckoprojects.emf.mongo.handlers.MongoResourceSetConfigurator;
import org.geckoprojects.emf.mongo.handlers.MongoResourceSetConfiguratorComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

/**
 * Integration tests for the complete EMF mongo setup
 * @author Mark Hoffmann
 * @since 26.07.2017
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class MongoExtendedMetadataIntegrationTest extends MongoEMFSetting {

	/**
	 * Test creation of object and returning results
	 * @throws IOException 
	 * @throws BundleException 
	 * @throws InvalidSyntaxException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testSaveNoExtendedMetadataAttribute() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		
		defaultSetup();
		
		String dbAlias = "testDB";
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(objectClass=org.geckoprojects.emf.core.ResourceSetConfigurator)(" + MongoResourceSetConfiguratorComponent.PROP_MONGO_ALIAS + "=" + dbAlias + "))";
		ResourceSetConfigurator rsc = (ResourceSetConfigurator) createTrackedChecker(filter, true).trackedServiceNotNull().getTrackedService();
		assertTrue(rsc instanceof MongoResourceSetConfigurator);
		
		filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ResourceSetFactory rsf = (ResourceSetFactory) createTrackedChecker(filter, true).trackedServiceNotNull().getTrackedService();
		ResourceSet resourceSet = rsf.createResourceSet();
		
		System.out.println("Dropping DB");
		MongoCollection<Document> bpCollection = client.getDatabase("test").getCollection("BusinessPerson");
		bpCollection.drop();
		
		assertEquals(0, bpCollection.countDocuments());
		Resource resource = resourceSet.createResource(URI.createURI("mongodb://"+ mongoHost + ":27017/test/BusinessPerson/"));
		
		BusinessPerson person = BasicFactory.eINSTANCE.createBusinessPerson();
		person.setFirstName("Mark");
		person.setLastName("Hoffmann" );
		person.setGender(GenderType.MALE);
		assertNull(person.getId());
		person.setCompanyIdCardNumber("test1234");
		resource.getContents().add(person);
		resource.save(null);
		
		resource.getContents().clear();
		resource.unload();
		/*
		 * Find person in the collection
		 */
		//		long start = System.currentTimeMillis();
		Resource findResource = resourceSet.createResource(URI.createURI("mongodb://" + mongoHost + ":27017/test/BusinessPerson/" + person.getId()));
		findResource.load(null);
		
		// get the person
		assertNotNull(findResource);
		assertFalse(findResource.getContents().isEmpty());
		assertEquals(1, findResource.getContents().size());
		
		
		// doing some object checks
		BusinessPerson p = (BusinessPerson) findResource.getContents().get(0);
		assertEquals("Mark", p.getFirstName());
		assertEquals("Hoffmann", p.getLastName());
		assertEquals(GenderType.MALE, p.getGender());
		assertNotNull(p.getId());
		assertEquals("test1234", p.getCompanyIdCardNumber());
		
		assertEquals(1, bpCollection.countDocuments());
		FindIterable<Document> docIterable = bpCollection.find();
		Document first = docIterable.first();
		Object cidField = first.get("companyIdCardNumber");
		assertNotNull(cidField);
		assertEquals("test1234", cidField);
		
		bpCollection.drop();
	}
	
	/**
	 * Test creation of object and returning results
	 * @throws IOException 
	 * @throws BundleException 
	 * @throws InvalidSyntaxException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testSaveExtendedMetadataAttribute() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		defaultSetup();
		
		String dbAlias = "testDB";
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(objectClass=org.geckoprojects.emf.core.ResourceSetConfigurator)(" + MongoResourceSetConfiguratorComponent.PROP_MONGO_ALIAS + "=" + dbAlias + "))";
		ResourceSetConfigurator rsc = (ResourceSetConfigurator) createTrackedChecker(filter, true).trackedServiceNotNull().getTrackedService();
		assertTrue(rsc instanceof MongoResourceSetConfigurator);
		
		filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ResourceSetFactory rsf = (ResourceSetFactory) createTrackedChecker(filter, true).trackedServiceNotNull().getTrackedService();
		ResourceSet resourceSet = rsf.createResourceSet();
		
		System.out.println("Dropping DB");
		MongoCollection<Document> bpCollection = client.getDatabase("test").getCollection("BusinessPerson");
		bpCollection.drop();
		
		assertEquals(0, bpCollection.countDocuments());
		Resource resource = resourceSet.createResource(URI.createURI("mongodb://"+ mongoHost + ":27017/test/BusinessPerson/"));
		
		BusinessPerson person = BasicFactory.eINSTANCE.createBusinessPerson();
		person.setFirstName("Mark");
		person.setLastName("Hoffmann" );
		person.setGender(GenderType.MALE);
		assertNull(person.getId());
		person.setCompanyIdCardNumber("test1234");
		resource.getContents().add(person);
		Map<String, Object> options = new HashMap<>();
		options.put(Options.OPTION_USE_EXTENDED_METADATA, Boolean.TRUE);
		resource.save(options);
		
		resource.getContents().clear();
		resource.unload();
		/*
		 * Find person in the collection
		 */
		//		long start = System.currentTimeMillis();
		Resource findResource = resourceSet.createResource(URI.createURI("mongodb://" + mongoHost + ":27017/test/BusinessPerson/" + person.getId()));
		findResource.load(options);
		
		// get the person
		assertNotNull(findResource);
		assertFalse(findResource.getContents().isEmpty());
		assertEquals(1, findResource.getContents().size());
		
		
		// doing some object checks
		BusinessPerson p = (BusinessPerson) findResource.getContents().get(0);
		assertEquals("Mark", p.getFirstName());
		assertEquals("Hoffmann", p.getLastName());
		assertEquals(GenderType.MALE, p.getGender());
		assertNotNull(p.getId());
		assertEquals("test1234", p.getCompanyIdCardNumber());
		
		assertEquals(1, bpCollection.countDocuments());
		FindIterable<Document> docIterable = bpCollection.find();
		Document first = docIterable.first();
		Object cidField = first.get("compId");
		assertNotNull(cidField);
		assertEquals("test1234", cidField);
		
		bpCollection.drop();
	}
	
	/**
	 * Test creation of object and returning results
	 * @throws IOException 
	 * @throws BundleException 
	 * @throws InvalidSyntaxException 
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSaveNoExtendedMetadataReferences() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		defaultSetup();
		
		String dbAlias = "testDB";
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(objectClass=org.geckoprojects.emf.core.ResourceSetConfigurator)(" + MongoResourceSetConfiguratorComponent.PROP_MONGO_ALIAS + "=" + dbAlias + "))";
		ResourceSetConfigurator rsc = (ResourceSetConfigurator) createTrackedChecker(filter, true).trackedServiceNotNull().getTrackedService();
		assertTrue(rsc instanceof MongoResourceSetConfigurator);
		
		filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ResourceSetFactory rsf = (ResourceSetFactory) createTrackedChecker(filter, true).trackedServiceNotNull().getTrackedService();
		ResourceSet resourceSet = rsf.createResourceSet();
		
		System.out.println("Dropping DB");
		MongoCollection<Document> bpCollection = client.getDatabase("test").getCollection("BusinessPerson");
		bpCollection.drop();
		
		assertEquals(0, bpCollection.countDocuments());
		Resource resource = resourceSet.createResource(URI.createURI("mongodb://"+ mongoHost + ":27017/test/BusinessPerson/"));
		
		BusinessPerson person = BasicFactory.eINSTANCE.createBusinessPerson();
		person.setFirstName("Mark");
		person.setLastName("Hoffmann" );
		person.setGender(GenderType.MALE);
		assertNull(person.getId());
		person.setCompanyIdCardNumber("test1234");
		EmployeeInfo einfo01 = BasicFactory.eINSTANCE.createEmployeeInfo();
		einfo01.setPosition("CTO");
		EmployeeInfo einfo02 = BasicFactory.eINSTANCE.createEmployeeInfo();
		einfo02.setPosition("CEO");
		person.getEmployeeInfo().add(einfo01);
		person.getEmployeeInfo().add(einfo02);
		
		resource.getContents().add(person);
		resource.save(null);
		
		resource.getContents().clear();
		resource.unload();
		/*
		 * Find person in the collection
		 */
		//		long start = System.currentTimeMillis();
		Resource findResource = resourceSet.createResource(URI.createURI("mongodb://" + mongoHost + ":27017/test/BusinessPerson/" + person.getId()));
		findResource.load(null);
		
		// get the person
		assertNotNull(findResource);
		assertFalse(findResource.getContents().isEmpty());
		assertEquals(1, findResource.getContents().size());
		
		
		// doing some object checks
		BusinessPerson p = (BusinessPerson) findResource.getContents().get(0);
		assertEquals("Mark", p.getFirstName());
		assertEquals("Hoffmann", p.getLastName());
		assertEquals(GenderType.MALE, p.getGender());
		assertNotNull(p.getId());
		assertEquals("test1234", p.getCompanyIdCardNumber());
		assertEquals(2, p.getEmployeeInfo().size());
		assertEquals("CTO", p.getEmployeeInfo().get(0).getPosition());
		assertEquals("CEO", p.getEmployeeInfo().get(1).getPosition());
		
		assertEquals(1, bpCollection.countDocuments());
		FindIterable<Document> docIterable = bpCollection.find();
		Document first = docIterable.first();
		Object eInfoField = first.get("employeeInfo");
		assertNotNull(eInfoField);
		assertTrue(eInfoField instanceof List);
		List<Document> docs = (List<Document>) eInfoField;
		assertEquals("CTO", docs.get(0).get("position"));
		assertEquals("CEO", docs.get(1).get("position"));
		Object eInfo2Field = first.get("employeeInfo2");
		assertNull(eInfo2Field);
		
		bpCollection.drop();
	}

	/**
	 * Test creation of object and returning results
	 * @throws IOException 
	 * @throws BundleException 
	 * @throws InvalidSyntaxException 
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSaveExtendedMetadataReference() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		defaultSetup();
		
		String dbAlias = "testDB";
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(objectClass=org.geckoprojects.emf.core.ResourceSetConfigurator)(" + MongoResourceSetConfiguratorComponent.PROP_MONGO_ALIAS + "=" + dbAlias + "))";
		ResourceSetConfigurator rsc = (ResourceSetConfigurator) createTrackedChecker(filter, true).trackedServiceNotNull().getTrackedService();
		assertTrue(rsc instanceof MongoResourceSetConfigurator);
		
		filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ResourceSetFactory rsf = (ResourceSetFactory) createTrackedChecker(filter, true).trackedServiceNotNull().getTrackedService();
		ResourceSet resourceSet = rsf.createResourceSet();
		
		System.out.println("Dropping DB");
		MongoCollection<Document> bpCollection = client.getDatabase("test").getCollection("BusinessPerson");
		bpCollection.drop();
		
		assertEquals(0, bpCollection.countDocuments());
		Resource resource = resourceSet.createResource(URI.createURI("mongodb://"+ mongoHost + ":27017/test/BusinessPerson/"));
		
		BusinessPerson person = BasicFactory.eINSTANCE.createBusinessPerson();
		person.setFirstName("Mark");
		person.setLastName("Hoffmann" );
		person.setGender(GenderType.MALE);
		assertNull(person.getId());
		person.setCompanyIdCardNumber("test1234");
		EmployeeInfo einfo01 = BasicFactory.eINSTANCE.createEmployeeInfo();
		einfo01.setPosition("CTO");
		EmployeeInfo einfo02 = BasicFactory.eINSTANCE.createEmployeeInfo();
		einfo02.setPosition("CEO");
		person.getEmployeeInfo().add(einfo01);
		person.getEmployeeInfo().add(einfo02);
		
		resource.getContents().add(person);
		Map<String, Object> options = new HashMap<>();
		options.put(Options.OPTION_USE_EXTENDED_METADATA, Boolean.TRUE);
		resource.save(options);
		
		resource.getContents().clear();
		resource.unload();
		/*
		 * Find person in the collection
		 */
		//		long start = System.currentTimeMillis();
		Resource findResource = resourceSet.createResource(URI.createURI("mongodb://" + mongoHost + ":27017/test/BusinessPerson/" + person.getId()));
		findResource.load(options);
		
		// get the person
		assertNotNull(findResource);
		assertFalse(findResource.getContents().isEmpty());
		assertEquals(1, findResource.getContents().size());
		
		
		// doing some object checks
		BusinessPerson p = (BusinessPerson) findResource.getContents().get(0);
		assertEquals("Mark", p.getFirstName());
		assertEquals("Hoffmann", p.getLastName());
		assertEquals(GenderType.MALE, p.getGender());
		assertNotNull(p.getId());
		assertEquals("test1234", p.getCompanyIdCardNumber());
		assertEquals(2, p.getEmployeeInfo().size());
		assertEquals("CTO", p.getEmployeeInfo().get(0).getPosition());
		assertEquals("CEO", p.getEmployeeInfo().get(1).getPosition());
		
		assertEquals(1, bpCollection.countDocuments());
		FindIterable<Document> docIterable = bpCollection.find();
		Document first = docIterable.first();
		Object eInfoField = first.get("eInfo");
		assertNotNull(eInfoField);
		assertTrue(eInfoField instanceof List);
		List<Document> docs = (List<Document>) eInfoField;
		assertEquals("CTO", docs.get(0).get("position"));
		assertEquals("CEO", docs.get(1).get("position"));
		Object eInfo2Field = first.get("employeeInfo");
		assertNull(eInfo2Field);
		
		bpCollection.drop();
	}

}
