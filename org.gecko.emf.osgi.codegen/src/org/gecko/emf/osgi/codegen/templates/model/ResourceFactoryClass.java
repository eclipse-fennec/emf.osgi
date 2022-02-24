package org.gecko.emf.osgi.codegen.templates.model;

import java.util.*;
import org.eclipse.emf.codegen.ecore.genmodel.*;

public class ResourceFactoryClass
{
  protected static String nl;
  public static synchronized ResourceFactoryClass create(String lineSeparator)
  {
    nl = lineSeparator;
    ResourceFactoryClass result = new ResourceFactoryClass();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "";
  protected final String TEXT_2 = "/*";
  protected final String TEXT_3 = NL + " * ";
  protected final String TEXT_4 = NL + " */" + NL + "package ";
  protected final String TEXT_5 = ";" + NL;
  protected final String TEXT_6 = NL + NL + "/**" + NL + " * <!-- begin-user-doc -->" + NL + " * The <b>Resource Factory</b> associated with the package." + NL + " * <!-- end-user-doc -->" + NL + " * @see ";
  protected final String TEXT_7 = NL + " * @generated" + NL + " */" + NL + " @Component" + NL + " @ProvideEMFResourceConfigurator( name = ";
  protected final String TEXT_8 = ".eNAME,";
  protected final String TEXT_9 = NL + "\tcontentType = { \"";
  protected final String TEXT_10 = "\" },";
  protected final String TEXT_11 = NL + "\tcontentType = { \"\" },";
  protected final String TEXT_12 = " ";
  protected final String TEXT_13 = NL + "\tfileExtension = {" + NL + "\t\"";
  protected final String TEXT_14 = "\"";
  protected final String TEXT_15 = "," + NL + " \t\"";
  protected final String TEXT_16 = NL + " \t},";
  protected final String TEXT_17 = " " + NL + "\tversion = \"1.0.0\"" + NL + ")";
  protected final String TEXT_18 = NL + "@Deprecated";
  protected final String TEXT_19 = NL + "public class ";
  protected final String TEXT_20 = " extends ";
  protected final String TEXT_21 = NL + "{";
  protected final String TEXT_22 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tpublic static final ";
  protected final String TEXT_23 = " copyright = ";
  protected final String TEXT_24 = ";";
  protected final String TEXT_25 = NL;
  protected final String TEXT_26 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprotected ";
  protected final String TEXT_27 = " extendedMetaData;" + NL;
  protected final String TEXT_28 = " xmlMap = new ";
  protected final String TEXT_29 = "();" + NL;
  protected final String TEXT_30 = NL + "\t/**" + NL + "\t * Creates an instance of the resource factory." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tpublic ";
  protected final String TEXT_31 = "()" + NL + "\t{" + NL + "\t\tsuper();";
  protected final String TEXT_32 = NL + "\t\textendedMetaData = new ";
  protected final String TEXT_33 = "(new ";
  protected final String TEXT_34 = "(";
  protected final String TEXT_35 = ".Registry.INSTANCE));" + NL + "\t\textendedMetaData.putPackage(null, ";
  protected final String TEXT_36 = ".eINSTANCE);";
  protected final String TEXT_37 = NL + "\t\txmlMap.setNoNamespacePackage(";
  protected final String TEXT_38 = NL + "\t}" + NL + "" + NL + "\t/**" + NL + "\t * Creates an instance of the resource." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */";
  protected final String TEXT_39 = NL + "\t@Override";
  protected final String TEXT_40 = NL + "\tpublic Resource createResource(URI uri)" + NL + "\t{";
  protected final String TEXT_41 = NL + "\t\t";
  protected final String TEXT_42 = " result = new ";
  protected final String TEXT_43 = "(uri);" + NL + "\t\tresult.getDefaultSaveOptions().put(";
  protected final String TEXT_44 = ".OPTION_EXTENDED_META_DATA, ";
  protected final String TEXT_45 = "Boolean.TRUE";
  protected final String TEXT_46 = "extendedMetaData";
  protected final String TEXT_47 = ");" + NL + "\t\tresult.getDefaultLoadOptions().put(";
  protected final String TEXT_48 = ");" + NL + "" + NL + "\t\tresult.getDefaultSaveOptions().put(";
  protected final String TEXT_49 = ".OPTION_SCHEMA_LOCATION, Boolean.TRUE);" + NL + "" + NL + "\t\tresult.getDefaultLoadOptions().put(";
  protected final String TEXT_50 = ".OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.TRUE);" + NL + "\t\tresult.getDefaultSaveOptions().put(";
  protected final String TEXT_51 = ".OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.TRUE);" + NL + "" + NL + "\t\tresult.getDefaultLoadOptions().put(";
  protected final String TEXT_52 = ".OPTION_USE_LEXICAL_HANDLER, Boolean.TRUE);";
  protected final String TEXT_53 = NL + "\t\tresult.getDefaultLoadOptions().put(";
  protected final String TEXT_54 = ".OPTION_USE_DATA_CONVERTER, Boolean.TRUE);";
  protected final String TEXT_55 = ".OPTION_XML_MAP, xmlMap);" + NL + "\t\tresult.getDefaultLoadOptions().put(";
  protected final String TEXT_56 = ".OPTION_XML_MAP, xmlMap);";
  protected final String TEXT_57 = NL + "\t\tResource result = new ";
  protected final String TEXT_58 = "(uri);";
  protected final String TEXT_59 = NL + "\t\treturn result;";
  protected final String TEXT_60 = NL + "\t}" + NL + "" + NL + "} //";

  public String generate(Object argument)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    
/**
 * Copyright (c) 2002-2006 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 */

    GenPackage genPackage = (GenPackage)argument; GenModel genModel=genPackage.getGenModel();
    final boolean isJDK50 = genModel.getComplianceLevel().getValue() >= GenJDKLevel.JDK50;
    stringBuffer.append(TEXT_1);
    stringBuffer.append(TEXT_2);
    {GenBase copyrightHolder = argument instanceof GenBase ? (GenBase)argument : argument instanceof Object[] && ((Object[])argument)[0] instanceof GenBase ? (GenBase)((Object[])argument)[0] : null;
    if (copyrightHolder != null && copyrightHolder.hasCopyright()) {
    stringBuffer.append(TEXT_3);
    stringBuffer.append(copyrightHolder.getCopyright(copyrightHolder.getGenModel().getIndentation(stringBuffer)));
    }}
    stringBuffer.append(TEXT_4);
    stringBuffer.append(genPackage.getUtilitiesPackageName());
    stringBuffer.append(TEXT_5);
    genModel.addImport("org.osgi.service.component.annotations.Component");
    genModel.getImportedName("org.eclipse.emf.common.util.URI");
    genModel.getImportedName("org.eclipse.emf.ecore.resource.Resource");
    genModel.addImport("org.gecko.emf.osgi.annotation.provide.ProvideEMFResourceConfigurator");
    genModel.addImport(genPackage.getQualifiedPackageInterfaceName());
    genModel.markImportLocation(stringBuffer);
    stringBuffer.append(TEXT_6);
    stringBuffer.append(genPackage.getQualifiedResourceClassName());
    if (genPackage.hasAPITags()) {
    stringBuffer.append(TEXT_3);
    stringBuffer.append(genPackage.getAPITags(genModel.getIndentation(stringBuffer)));
    }
    stringBuffer.append(TEXT_7);
    stringBuffer.append(genPackage.getPackageInterfaceName());
    stringBuffer.append(TEXT_8);
    if (genPackage.getContentTypeIdentifier() != null) {
    stringBuffer.append(TEXT_9);
    stringBuffer.append(genPackage.getContentTypeIdentifier());
    stringBuffer.append(TEXT_10);
    } else {
    stringBuffer.append(TEXT_11);
    }
    stringBuffer.append(TEXT_12);
    if (!genPackage.getFileExtensionList().isEmpty()) {
    Iterator<String> fileExtensionIterator = genPackage.getFileExtensionList().iterator(); if (fileExtensionIterator.hasNext()) { String fileExtension = fileExtensionIterator.next();
    stringBuffer.append(TEXT_13);
    stringBuffer.append(fileExtension);
    stringBuffer.append(TEXT_14);
    while(fileExtensionIterator.hasNext()) { fileExtension = fileExtensionIterator.next();
    stringBuffer.append(TEXT_15);
    stringBuffer.append(fileExtension);
    stringBuffer.append(TEXT_14);
    }
    stringBuffer.append(TEXT_16);
    }
    stringBuffer.append(TEXT_12);
    }
    stringBuffer.append(TEXT_17);
    if (isJDK50 && genPackage.hasAPIDeprecatedTag()) {
    stringBuffer.append(TEXT_18);
    }
    stringBuffer.append(TEXT_19);
    stringBuffer.append(genPackage.getResourceFactoryClassName());
    stringBuffer.append(TEXT_20);
    stringBuffer.append(genPackage.getImportedResourceFactoryBaseClassName());
    stringBuffer.append(TEXT_21);
    if (genModel.hasCopyrightField()) {
    stringBuffer.append(TEXT_22);
    stringBuffer.append(genModel.getImportedName("java.lang.String"));
    stringBuffer.append(TEXT_23);
    stringBuffer.append(genModel.getCopyrightFieldLiteral());
    stringBuffer.append(TEXT_24);
    stringBuffer.append(genModel.getNonNLS());
    stringBuffer.append(TEXT_25);
    }
    if (genPackage.hasExtendedMetaData() && !genPackage.hasTargetNamespace()) {
    stringBuffer.append(TEXT_26);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.util.ExtendedMetaData"));
    stringBuffer.append(TEXT_27);
    } else if (genPackage.hasXMLMap()) {
    stringBuffer.append(TEXT_26);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource$XMLMap"));
    stringBuffer.append(TEXT_28);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.impl.XMLMapImpl"));
    stringBuffer.append(TEXT_29);
    }
    stringBuffer.append(TEXT_30);
    stringBuffer.append(genPackage.getResourceFactoryClassName());
    stringBuffer.append(TEXT_31);
    if (genPackage.hasExtendedMetaData() && !genPackage.hasTargetNamespace()) {
    stringBuffer.append(TEXT_32);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.util.BasicExtendedMetaData"));
    stringBuffer.append(TEXT_33);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.impl.EPackageRegistryImpl"));
    stringBuffer.append(TEXT_34);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
    stringBuffer.append(TEXT_35);
    stringBuffer.append(genPackage.getImportedPackageInterfaceName());
    stringBuffer.append(TEXT_36);
    } else if (genPackage.hasXMLMap() && !genPackage.hasTargetNamespace()) {
    stringBuffer.append(TEXT_37);
    stringBuffer.append(genPackage.getImportedPackageInterfaceName());
    stringBuffer.append(TEXT_36);
    }
    stringBuffer.append(TEXT_38);
    if (genModel.useClassOverrideAnnotation()) {
    stringBuffer.append(TEXT_39);
    }
    stringBuffer.append(TEXT_40);
    if (genPackage.hasExtendedMetaData()) {
    stringBuffer.append(TEXT_41);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_42);
    stringBuffer.append(genPackage.getResourceClassName());
    stringBuffer.append(TEXT_43);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_44);
    if (genPackage.hasTargetNamespace()){
    stringBuffer.append(TEXT_45);
    }else{
    stringBuffer.append(TEXT_46);
    }
    stringBuffer.append(TEXT_47);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_44);
    if (genPackage.hasTargetNamespace()){
    stringBuffer.append(TEXT_45);
    }else{
    stringBuffer.append(TEXT_46);
    }
    stringBuffer.append(TEXT_48);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_49);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_50);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_51);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_52);
    if (genPackage.isDataTypeConverters() && genPackage.hasDocumentRoot()) {
    stringBuffer.append(TEXT_53);
    stringBuffer.append(genPackage.getResourceClassName());
    stringBuffer.append(TEXT_54);
    }
    } else if (genPackage.hasXMLMap()) {
    stringBuffer.append(TEXT_41);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_42);
    stringBuffer.append(genPackage.getResourceClassName());
    stringBuffer.append(TEXT_43);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_55);
    stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.XMLResource"));
    stringBuffer.append(TEXT_56);
    } else {
    stringBuffer.append(TEXT_57);
    stringBuffer.append(genPackage.getResourceClassName());
    stringBuffer.append(TEXT_58);
    }
    stringBuffer.append(TEXT_59);
    //ResourceFactoryClass/createResource.override.javajetinc
    stringBuffer.append(TEXT_60);
    stringBuffer.append(genPackage.getResourceFactoryClassName());
    genModel.emitSortedImports();
    stringBuffer.append(TEXT_25);
    return stringBuffer.toString();
  }
}
