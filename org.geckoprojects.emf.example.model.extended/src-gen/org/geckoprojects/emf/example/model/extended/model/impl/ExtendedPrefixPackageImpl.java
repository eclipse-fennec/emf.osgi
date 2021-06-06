/**
 */
package org.geckoprojects.emf.example.model.extended.model.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EPackageImpl;

import org.geckoprojects.emf.example.model.basic.model.BasicPackage;

import org.geckoprojects.emf.example.model.extended.model.ExtendedAddress;
import org.geckoprojects.emf.example.model.extended.model.ExtendedPerson;
import org.geckoprojects.emf.example.model.extended.model.ExtendedPrefixFactory;
import org.geckoprojects.emf.example.model.extended.model.ExtendedPrefixPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class ExtendedPrefixPackageImpl extends EPackageImpl implements ExtendedPrefixPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass extendedAddressEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass extendedPersonEClass = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see org.geckoprojects.emf.example.model.extended.model.ExtendedPrefixPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private ExtendedPrefixPackageImpl() {
		super(eNS_URI, ExtendedPrefixFactory.eINSTANCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 *
	 * <p>This method is used to initialize {@link ExtendedPrefixPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static ExtendedPrefixPackage init() {
		if (isInited) return (ExtendedPrefixPackage)EPackage.Registry.INSTANCE.getEPackage(ExtendedPrefixPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredExtendedPrefixPackage = EPackage.Registry.INSTANCE.get(eNS_URI);
		ExtendedPrefixPackageImpl theExtendedPrefixPackage = registeredExtendedPrefixPackage instanceof ExtendedPrefixPackageImpl ? (ExtendedPrefixPackageImpl)registeredExtendedPrefixPackage : new ExtendedPrefixPackageImpl();

		isInited = true;

		// Initialize simple dependencies
		BasicPackage.eINSTANCE.eClass();

		// Create package meta-data objects
		theExtendedPrefixPackage.createPackageContents();

		// Initialize created meta-data
		theExtendedPrefixPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theExtendedPrefixPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(ExtendedPrefixPackage.eNS_URI, theExtendedPrefixPackage);
		return theExtendedPrefixPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getExtendedAddress() {
		return extendedAddressEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getExtendedPerson() {
		return extendedPersonEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ExtendedPrefixFactory getExtendedPrefixFactory() {
		return (ExtendedPrefixFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		extendedAddressEClass = createEClass(EXTENDED_ADDRESS);

		extendedPersonEClass = createEClass(EXTENDED_PERSON);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Obtain other dependent packages
		BasicPackage theBasicPackage = (BasicPackage)EPackage.Registry.INSTANCE.getEPackage(BasicPackage.eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		extendedAddressEClass.getESuperTypes().add(theBasicPackage.getAddress());
		extendedPersonEClass.getESuperTypes().add(theBasicPackage.getEmployeeInfo());
		extendedPersonEClass.getESuperTypes().add(theBasicPackage.getPerson());

		// Initialize classes, features, and operations; add parameters
		initEClass(extendedAddressEClass, ExtendedAddress.class, "ExtendedAddress", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(extendedPersonEClass, ExtendedPerson.class, "ExtendedPerson", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		// Create resource
		createResource(eNS_URI);
	}

} //ExtendedPrefixPackageImpl
