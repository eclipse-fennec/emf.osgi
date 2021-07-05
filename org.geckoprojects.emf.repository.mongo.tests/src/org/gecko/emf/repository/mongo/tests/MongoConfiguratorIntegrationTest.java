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
package org.gecko.emf.repository.mongo.tests;




import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.geckoprojects.emf.core.EMFNamespaces;
import org.geckoprojects.emf.core.EPackageConfigurator;
import org.geckoprojects.emf.core.ResourceFactoryConfigurator;
import org.geckoprojects.emf.example.model.basic.model.BasicFactory;
import org.geckoprojects.emf.example.model.basic.model.BasicPackage;
import org.geckoprojects.emf.example.model.basic.model.Person;
import org.geckoprojects.emf.mongo.ConverterService;
import org.geckoprojects.emf.mongo.InputStreamFactory;
import org.geckoprojects.emf.mongo.MongoIdFactory;
import org.geckoprojects.emf.mongo.OutputStreamFactory;
import org.geckoprojects.emf.mongo.QueryEngine;
import org.geckoprojects.emf.repository.EMFRepository;
import org.geckoprojects.emf.repository.mongo.annotations.RequireMongoEMFRepository;
import org.geckoprojects.emf.repository.mongo.api.EMFMongoConfiguratorConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Integration tests for the complete EMF mongo repository setup
 * @author Mark Hoffmann
 * @since 26.07.2017
 */
@RequireMongoEMFRepository
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class MongoConfiguratorIntegrationTest  {


		protected MongoClient client;
		protected List<MongoCollection<?>> collections = new LinkedList<>();
		protected String mongoHost = System.getProperty("mongo.host", "localhost");
		private ServiceRegistration<?> testPackageRegistration = null;

		@BeforeEach
		public void doBefore() {

			client = MongoClients.create(mongoHost);
		}

		@AfterEach
		public void doAfter() {
			collections.forEach(MongoCollection::drop);
			if (client != null) {
				client.close();
			}

		}

		protected MongoCollection<?> getCollection(String database, String collection) {
			MongoDatabase db = client.getDatabase(database);
			assertNotNull(db);
			MongoCollection<?> c = db.getCollection(collection);
			assertNotNull(c);
			collections.add(c);
			return c;
		}

		@Test
		public void defaultCheck(@InjectService ServiceAware<MongoIdFactory> saMongoIdFactory,
				@InjectService ServiceAware<QueryEngine> saQueryEngine,
				@InjectService ServiceAware<ConverterService> saConverterService,
				@InjectService ServiceAware<InputStreamFactory> saInputStreamFactory,
				@InjectService ServiceAware<OutputStreamFactory> saOutputStreamFactory)
				throws IOException, InvalidSyntaxException {

		}
	@Test
	public void testEMFMongoRepository() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		/**
		 * mongo.instances=test1
		 * test1.baseUris=mongodb://localhost
		 * test1.databases=test
		 */

		Dictionary<String, Object> configProperties = new Hashtable<>();
		configProperties.put("mongo.instances", "test1");
		configProperties.put("test1.baseUris", "mongodb://" + mongoHost);
		configProperties.put("test1.databases", "test");

		String clientId = "test1.test";


		
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=" + clientId + ")(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ServiceChecker<Object> rsfTracker = createTrackedChecker(filter, true)
				.assertCreations(0, false);
		
		ServiceChecker<EMFRepository> repoTracker = createTrackedChecker("(" + EMFRepository.PROP_ID + "=" + clientId + ")", false);
		ServiceChecker<MongoClientProvider> clientTracker = createTrackedChecker(MongoClientProvider.class);
		ServiceChecker<MongoDatabaseProvider> dbTracker = createTrackedChecker(MongoDatabaseProvider.class);
		
		repoTracker.assertCreations(0, false);
		clientTracker.assertCreations(0, false);
		dbTracker.assertCreations(0, false);
		
		Configuration repositoryConfig = createConfigForCleanup(EMFMongoConfiguratorConstants.EMF_MONGO_REPOSITORY_CONFIGURATOR_CONFIGURATION_NAME, "?", configProperties);
		
		rsfTracker.assertCreations(1, true).trackedServiceNotNull();
		clientTracker.assertCreations(1, true);
		dbTracker.assertCreations(1, true);
		repoTracker.assertCreations(1, true);

		EMFRepository repository = repoTracker.getTrackedService();

		Person person = BasicFactory.eINSTANCE.createPerson();
		person.setId("test");
		person.setFirstName("Emil");
		person.setLastName("Tester");
		URI uri = repository.createUri(person);
		assertEquals("mongodb://test1/test/Person/test#test", uri.toString());

		MongoCollection<?> collection = getCollection("test", "Person"); 
		collection.drop();

		assertEquals(0, collection.countDocuments());

		CountDownLatch latch = new CountDownLatch(1);
		latch.await(1, TimeUnit.SECONDS);
		
		repository.save(person);

		assertEquals(1, collection.countDocuments());

		Resource r = person.eResource();
		assertNotNull(r);
		ResourceSet rs = r.getResourceSet();
		assertNotNull(rs);
		assertEquals(1, rs.getResources().size());

		repository.detach(person);
		assertNull(person.eResource());
		assertEquals(0, rs.getResources().size());

		Person personResult = repository.getEObject(BasicPackage.Literals.PERSON, "test");
		assertNotNull(personResult);
		assertNotEquals(person, personResult);
		assertNotEquals(r, personResult.eResource());

		assertTrue(EcoreUtil.equals(person, personResult));

		deleteConfigurationAndRemoveFromCleanup(repositoryConfig);

		repoTracker.assertRemovals(1, true);
		dbTracker.assertRemovals(1, true);
		clientTracker.assertRemovals(1, true);
	}

	@Test
	public void testEMFMongoRepositoryPrototypeInstance() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		
		/**
		 * mongo.instances=test1
		 * test1.baseUris=mongodb://localhost
		 * test1.databases=test
		 */

		Dictionary<String, Object> configProperties = new Hashtable<>();
		configProperties.put("mongo.instances", "test1");
		configProperties.put("test1.baseUris", "mongodb://" + mongoHost);
		configProperties.put("test1.databases", "test3, test4");
		configProperties.put("test1." + EMFMongoConfiguratorConstants.MONGO_REPOSITORY_TYPE, EMFMongoConfiguratorConstants.Type.PROTOTYPE.toString());

		String clientId1 = "test1.test3";
		String clientId2 = "test1.test4";
		
		defaultCheck();
		
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=" + clientId1 + ")(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ServiceChecker<Object> rsfTracker1 = createTrackedChecker(filter, true)
				.assertCreations(0, false);
		filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=" + clientId2 + ")(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ServiceChecker<Object> rsfTracker2 = createTrackedChecker(filter, true)
				.assertCreations(0, false);

		ServiceChecker<EMFRepository> repoTracker = createTrackedChecker("(" + EMFRepository.PROP_ID + "=" + clientId1 + ")", false);
		ServiceChecker<EMFRepository> repo2Tracker = createTrackedChecker("(" + EMFRepository.PROP_ID + "=" + clientId2 + ")", false);
		ServiceChecker<MongoClientProvider> clientTracker = createTrackedChecker(MongoClientProvider.class, true);
		ServiceChecker<MongoDatabaseProvider> dbTracker = createTrackedChecker(MongoDatabaseProvider.class, true);

		rsfTracker1.assertCreations(0, false);
		rsfTracker2.assertCreations(0, false);
		repoTracker.assertCreations(0, false);
		repo2Tracker.assertCreations(0, false);
		clientTracker.assertCreations(0, false);
		dbTracker.assertCreations(0, false);
		
		Configuration configuration = createConfigForCleanup(EMFMongoConfiguratorConstants.EMF_MONGO_REPOSITORY_CONFIGURATOR_CONFIGURATION_NAME, "?", configProperties);

		repoTracker.assertCreations(1, true);
		repo2Tracker.assertCreations(1, true);
		clientTracker.assertCreations(1, true);
		dbTracker.assertCreations(2, true);
		rsfTracker1.assertCreations(1, true);
		rsfTracker2.assertCreations(1, true);

		ServiceObjects<EMFRepository> repo1ServiceObjects = getServiceObjects(EMFRepository.class,"(" + EMFRepository.PROP_ID + "=" + clientId1 + ")");

		
		EMFRepository repository1 = repo1ServiceObjects.getService();
		EMFRepository repository2 = repo1ServiceObjects.getService();
		assertNotEquals(repository1, repository2);

		repo1ServiceObjects.ungetService(repository1);
		repo1ServiceObjects.ungetService(repository2);
		
		ServiceObjects<EMFRepository> repo2ServiceObjects = getServiceObjects(EMFRepository.class,"(" + EMFRepository.PROP_ID + "=" + clientId2 + ")");
		
		
		repository1 = repo2ServiceObjects.getService();
		repository2 = repo2ServiceObjects.getService();
		assertNotEquals(repository1, repository2);
		
		repo2ServiceObjects.ungetService(repository1);
		repo2ServiceObjects.ungetService(repository2);
		
		deleteConfigurationAndRemoveFromCleanup(configuration);
		
		repoTracker.assertRemovals(1, true);
		repo2Tracker.assertRemovals(1, true);
		clientTracker.assertRemovals(1, true);
		dbTracker.assertRemovals(2, true);
		rsfTracker1.assertRemovals(1, true);
		rsfTracker2.assertRemovals(1, true);
	}

	@Test
	public void testEMFMongoRepositoryPrototypeDBLevel() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
	
		
		/**
		 * mongo.instances=test1
		 * test1.baseUris=mongodb://localhost
		 * test1.databases=test
		 */

		Dictionary<String, Object> configProperties = new Hashtable<>();
		configProperties.put("mongo.instances", "test1");
		configProperties.put("test1.baseUris", "mongodb://" + mongoHost);
		configProperties.put("test1.databases", "test5, test6");
		configProperties.put("test1.test5." + EMFMongoConfiguratorConstants.MONGO_REPOSITORY_TYPE, EMFMongoConfiguratorConstants.Type.PROTOTYPE.toString());
		configProperties.put("test1.test6." + EMFMongoConfiguratorConstants.MONGO_REPOSITORY_TYPE, EMFMongoConfiguratorConstants.Type.SINGLETON.toString());

		String clientId1 = "test1.test5";
		String clientId2 = "test1.test6";
		
		defaultCheck();
		
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=" + clientId1 + ")(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ServiceChecker<Object> rsfTracker1 = createTrackedChecker(filter, true)
				.assertCreations(0, false);
		filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=" + clientId2 + ")(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ServiceChecker<Object> rsfTracker2 = createTrackedChecker(filter, true)
				.assertCreations(0, false);

		ServiceChecker<EMFRepository> repoTracker = createTrackedChecker("(" + EMFRepository.PROP_ID + "=" + clientId1 + ")", false);
		ServiceChecker<EMFRepository> repo2Tracker = createTrackedChecker("(" + EMFRepository.PROP_ID + "=" + clientId2 + ")", false);
		ServiceChecker<MongoClientProvider> clientTracker = createTrackedChecker(MongoClientProvider.class);
		ServiceChecker<MongoDatabaseProvider> dbTracker = createTrackedChecker(MongoDatabaseProvider.class);

		repoTracker.assertCreations(0, false);
		repo2Tracker.assertCreations(0, false);
		clientTracker.assertCreations(0, false);
		dbTracker.assertCreations(0, false);
		rsfTracker1.assertCreations(0, false);
		rsfTracker2.assertCreations(0, false);
		
		Configuration configuration = createConfigForCleanup(EMFMongoConfiguratorConstants.EMF_MONGO_REPOSITORY_CONFIGURATOR_CONFIGURATION_NAME, "?", configProperties);

		repoTracker.assertCreations(1, true);
		repo2Tracker.assertCreations(1, true);
		clientTracker.assertCreations(1, true);
		dbTracker.assertCreations(2, true);
		rsfTracker1.assertCreations(1, true);
		rsfTracker2.assertCreations(1, true);

		ServiceObjects<EMFRepository> repo1ServiceObjects = getServiceObjects(EMFRepository.class,"(" + EMFRepository.PROP_ID + "=" + clientId1 + ")");

		
		EMFRepository repository1 = repo1ServiceObjects.getService();
		EMFRepository repository2 = repo1ServiceObjects.getService();
		assertNotEquals(repository1, repository2);

		repo1ServiceObjects.ungetService(repository1);
		repo1ServiceObjects.ungetService(repository2);
		
		ServiceObjects<EMFRepository> repo2ServiceObjects = getServiceObjects(EMFRepository.class,"(" + EMFRepository.PROP_ID + "=" + clientId2 + ")");
		
		
		repository1 = repo2ServiceObjects.getService();
		repository2 = repo2ServiceObjects.getService();
		assertEquals(repository1, repository2);
		
		repo2ServiceObjects.ungetService(repository1);
		repo2ServiceObjects.ungetService(repository2);
		
		deleteConfigurationAndRemoveFromCleanup(configuration);
		
		repoTracker.assertRemovals(1, true);
		repo2Tracker.assertRemovals(1, true);
		clientTracker.assertRemovals(1, true);
		dbTracker.assertRemovals(2, true);
		rsfTracker1.assertRemovals(1, true);
		rsfTracker2.assertRemovals(1, true);
		
	}

	@Test
	public void testVarReplacementDBAuth() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		registerServiceForCleanup(new TestPackageConfigurator(), new Hashtable<String, Object>(), EPackageConfigurator.class.getName(), ResourceFactoryConfigurator.class.getName());
	
		/**
		 * mongo.instances=test1
		 * test1.baseUris=mongodb://localhost
		 * test1.databases=test
		 */
		
		Dictionary<String, Object> configProperties = new Hashtable<>();
		configProperties.put("mongo.instances", "test1");
		configProperties.put("test1.baseUris", "mongodb://" + mongoHost);
		configProperties.put("test1.baseUris.env", "URI_ENV");
		configProperties.put("test1.databases", "test");
		configProperties.put("test1.authSource", "admin");
		configProperties.put("test1.user", "test");
		configProperties.put("test1.user.env", "USER_ENV");
		configProperties.put("test1.password", "1234");
		configProperties.put("test1.password.env", "PWD_ENV");
		
		System.setProperty("USER_ENV", "envUser");
		System.setProperty("URI_ENV", "mongodb://127.0.0.1");
		System.setProperty("PWD_ENV", "testPwd");
		
		String clientId = "test1.test";
		
		defaultCheck();
		
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=" + clientId + ")(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ServiceChecker<Object> rsfTracker = createTrackedChecker(filter, true)
				.assertCreations(0, false);

		ServiceChecker<EMFRepository> repoTracker = createTrackedChecker("(" + EMFRepository.PROP_ID + "=" + clientId + ")", false);
		ServiceChecker<MongoClientProvider> clientTracker = createTrackedChecker(MongoClientProvider.class);
		ServiceChecker<MongoDatabaseProvider> dbTracker = createTrackedChecker(MongoDatabaseProvider.class);
		
		repoTracker.assertCreations(0, false);
		clientTracker.assertCreations(0, false);
		dbTracker.assertCreations(0, false);
		rsfTracker.assertCreations(0, false);
		
		Configuration configuration = createConfigForCleanup(EMFMongoConfiguratorConstants.EMF_MONGO_REPOSITORY_CONFIGURATOR_CONFIGURATION_NAME, "?", configProperties);

		repoTracker.assertCreations(1, true);
		clientTracker.assertCreations(1, true);
		dbTracker.assertCreations(1, true);
		rsfTracker.assertCreations(1, true);
		
		ServiceReference<MongoClientProvider> clientProvider = getServiceReference(MongoClientProvider.class);
		MongoClientProvider mcp = getBundleContext().getService(clientProvider);
		assertNotNull(mcp);
		
		MongoCredential cred = mcp.getMongoClient().getCredential();
		assertEquals(System.getProperty("USER_ENV"), cred.getUserName());
		assertArrayEquals(System.getProperty("PWD_ENV").toCharArray(), cred.getPassword());
		
		assertEquals(System.getProperty("URI_ENV"), mcp.getURIs()[0]);
		
		ServiceReference<MongoDatabaseProvider> dbProvider = getServiceReference(MongoDatabaseProvider.class);
		assertNotNull(dbProvider);
		deleteConfigurationAndRemoveFromCleanup(configuration);
		repoTracker.assertRemovals(1, true);
		clientTracker.assertRemovals(1, true);
		dbTracker.assertRemovals(1, true);
		rsfTracker.assertRemovals(1, true);
	}
	
	@Test
	public void testVarReplacementInstanceAuth() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		registerServiceForCleanup(new TestPackageConfigurator(), new Hashtable<String, Object>(), EPackageConfigurator.class.getName(), ResourceFactoryConfigurator.class.getName());
		
		/**
		 * mongo.instances=test1
		 * test1.baseUris=mongodb://localhost
		 * test1.databases=test
		 */
		
		Dictionary<String, Object> configProperties = new Hashtable<>();
		String URI_ENV_NAME = "URI_ENV";
		String AUTH_SOURCE_ENV_NAME = "AUTH_SRC_ENV";
		String USER_ENV_NAME = "USER_ENV";
		String PWD_ENV_NAME = "PWD_ENV";
		configProperties.put("mongo.instances", "test1");
		configProperties.put("test1.baseUris", "mongodb://" + mongoHost);
		configProperties.put("test1.baseUris.env", URI_ENV_NAME);
		configProperties.put("test1.databases", "test");
		configProperties.put("test1.authSource", "test");
		configProperties.put("test1.authSource.env", AUTH_SOURCE_ENV_NAME);
		configProperties.put("test1.user", "test");
		configProperties.put("test1.user.env", USER_ENV_NAME);
		configProperties.put("test1.password", "1234");
		configProperties.put("test1.password.env", PWD_ENV_NAME);
		
		System.setProperty(USER_ENV_NAME, "envUser");
		System.setProperty(URI_ENV_NAME, "mongodb://127.0.0.1");
		System.setProperty(PWD_ENV_NAME, "envPwd");
		System.setProperty(AUTH_SOURCE_ENV_NAME, "envSource");
		
		String clientId = "test1.test";
		
		defaultCheck();
		
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=" + clientId + ")(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ServiceChecker<Object> rsfTracker = createTrackedChecker(filter, true)
				.assertCreations(0, false);
		
		ServiceChecker<EMFRepository> repoTracker = createTrackedChecker("(" + EMFRepository.PROP_ID + "=" + clientId + ")", false);
		ServiceChecker<MongoClientProvider> clientTracker = createTrackedChecker(MongoClientProvider.class);
		ServiceChecker<MongoDatabaseProvider> dbTracker = createTrackedChecker(MongoDatabaseProvider.class);
		
		repoTracker.assertCreations(0, false);
		clientTracker.assertCreations(0, false);
		dbTracker.assertCreations(0, false);
		rsfTracker.assertCreations(0, false);
		
		Configuration configuration = createConfigForCleanup(EMFMongoConfiguratorConstants.EMF_MONGO_REPOSITORY_CONFIGURATOR_CONFIGURATION_NAME, "?", configProperties);
		
		repoTracker.assertCreations(1, true);
		clientTracker.assertCreations(1, true);
		dbTracker.assertCreations(1, true);
		rsfTracker.assertCreations(1, true);
		
		ServiceReference<MongoClientProvider> clientProvider = getServiceReference(MongoClientProvider.class);
		MongoClientProvider mcp = getBundleContext().getService(clientProvider);
		assertNotNull(mcp);
		
		MongoCredential cred = mcp.getMongoClient().getCredential();
		assertEquals(System.getProperty(USER_ENV_NAME), cred.getUserName());
		assertArrayEquals(System.getProperty(PWD_ENV_NAME).toCharArray(), cred.getPassword());
		assertEquals(System.getProperty(AUTH_SOURCE_ENV_NAME), cred.getSource());
		
		assertEquals(1, mcp.getURIs().length);
		assertEquals(System.getProperty(URI_ENV_NAME), mcp.getURIs()[0]);
		
		ServiceReference<MongoDatabaseProvider> dbProvider = getServiceReference(MongoDatabaseProvider.class);
		assertNotNull(dbProvider);
		
		deleteConfigurationAndRemoveFromCleanup(configuration);
		repoTracker.assertRemovals(1, true);
		clientTracker.assertRemovals(1, true);
		dbTracker.assertRemovals(1, true);
		rsfTracker.assertRemovals(1, true);
	}
	
	@Test
	public void testVarReplacementInstanceAuthOverDBAuth() throws BundleException, InvalidSyntaxException, IOException, InterruptedException {
		registerServiceForCleanup(new TestPackageConfigurator(), new Hashtable<String, Object>(), EPackageConfigurator.class.getName(), ResourceFactoryConfigurator.class.getName());
		
		/**
		 * mongo.instances=test1
		 * test1.baseUris=mongodb://localhost
		 * test1.databases=test
		 */
		
		Dictionary<String, Object> configProperties = new Hashtable<>();
		String URI_ENV_NAME = "URI_ENV";
		String AUTH_SOURCE_ENV_NAME = "AUTH_SRC_ENV";
		String USER_ENV_NAME = "USER_ENV";
		String PWD_ENV_NAME = "PWD_ENV";
		String DB_USER_ENV_NAME = "DB_USER_ENV";
		String DB_PWD_ENV_NAME = "DB_PWD_ENV";
		configProperties.put("mongo.instances", "test1");
		configProperties.put("test1.baseUris", "mongodb://" + mongoHost);
		configProperties.put("test1.baseUris.env", URI_ENV_NAME);
		configProperties.put("test1.databases", "test");
		configProperties.put("test1.authSource", "test");
		configProperties.put("test1.authSource.env", AUTH_SOURCE_ENV_NAME);
		configProperties.put("test1.user", "test");
		configProperties.put("test1.user.env", USER_ENV_NAME);
		configProperties.put("test1.password", "1234");
		configProperties.put("test1.password.env", PWD_ENV_NAME);
		configProperties.put("test1.test.user", "test");
		configProperties.put("test1.test.user.env", DB_USER_ENV_NAME);
		configProperties.put("test1.test.password", "1234");
		configProperties.put("test1.test.password.env", DB_PWD_ENV_NAME);
		
		System.setProperty(USER_ENV_NAME, "envUser");
		System.setProperty(URI_ENV_NAME, "mongodb://127.0.0.1");
		System.setProperty(PWD_ENV_NAME, "envPwd");
		System.setProperty(AUTH_SOURCE_ENV_NAME, "envSource");
		
		String clientId = "test1.test";

		defaultCheck();
		
		String filter = "(&(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=mongo)(" + EMFNamespaces.EMF_CONFIGURATOR_NAME + "=" + clientId + ")(objectClass=org.geckoprojects.emf.core.ResourceSetFactory))";
		ServiceChecker<Object> rsfTracker = createTrackedChecker(filter, true)
				.assertCreations(0, false);
		
		ServiceChecker<EMFRepository> repoTracker = createTrackedChecker("(" + EMFRepository.PROP_ID + "=" + clientId + ")", false);
		ServiceChecker<MongoClientProvider> clientTracker = createTrackedChecker(MongoClientProvider.class);
		ServiceChecker<MongoDatabaseProvider> dbTracker = createTrackedChecker(MongoDatabaseProvider.class);
		
		repoTracker.assertCreations(0, false);
		clientTracker.assertCreations(0, false);
		dbTracker.assertCreations(0, false);
		rsfTracker.assertCreations(0, false);
		
		Configuration configuration = createConfigForCleanup(EMFMongoConfiguratorConstants.EMF_MONGO_REPOSITORY_CONFIGURATOR_CONFIGURATION_NAME, "?", configProperties);
		
		repoTracker.assertCreations(1, true);
		clientTracker.assertCreations(1, true);
		dbTracker.assertCreations(1, true);
		rsfTracker.assertCreations(1, true);
		
		ServiceReference<MongoClientProvider> clientProvider = getServiceReference(MongoClientProvider.class);
		MongoClientProvider mcp = getBundleContext().getService(clientProvider);
		assertNotNull(mcp);
		
		MongoCredential cred = mcp.getMongoClient().getCredential();
		assertEquals(System.getProperty(USER_ENV_NAME), cred.getUserName());
		assertArrayEquals(System.getProperty(PWD_ENV_NAME).toCharArray(), cred.getPassword());
		assertEquals(System.getProperty(AUTH_SOURCE_ENV_NAME), cred.getSource());
		
		assertEquals(1, mcp.getURIs().length);
		assertEquals(System.getProperty(URI_ENV_NAME), mcp.getURIs()[0]);
		
		ServiceReference<MongoDatabaseProvider> dbProvider = getServiceReference(MongoDatabaseProvider.class);
		assertNotNull(dbProvider);
		
		deleteConfigurationAndRemoveFromCleanup(configuration);
		repoTracker.assertRemovals(1, true);
		clientTracker.assertRemovals(1, true);
		dbTracker.assertRemovals(1, true);
		rsfTracker.assertRemovals(1, true);
	}
}
