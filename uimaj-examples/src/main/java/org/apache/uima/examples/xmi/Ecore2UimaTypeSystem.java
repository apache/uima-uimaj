/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.examples.xmi;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Converts an Ecore model to a UIMA TypeSystemDescription.
 */
public class Ecore2UimaTypeSystem {
  /**
   * Configures the handling of multi-valued properties in the Ecore model. If set to Boolean.FALSE
   * (the default), UIMA array types (e.g. FSArray) will be generated. If set to Boolean.TRUE, UIMA
   * list types (e.g. FSList) will be generated. Note that for primitive types that have no
   * corresponding list type (Byte, Short, Long, Double, and Boolean), array types will always be
   * used.
   */
  public static final String OPTION_GENERATE_UIMA_LIST_TYPES = "OPTION_GENERATE_UIMA_LIST_TYPES";

  /**
   * Configures the assignment of supertypes to EClasses that have no declared supertype. If set to
   * Boolean.TRUE (the default), if such an EClass has "begin" and "end" properties of type EInt,
   * the superclass will be set to uima.tcas.Annotation. If set to Boolean.FALSE, all EClasses with
   * no declared supertype will have their supertype set to uima.cas.TOP.
   */
  public static final String OPTION_CREATE_ANNOTATION_SUBTYPES = "OPTION_CREATE_ANNOTATION_SUBTYPES";

  private static ResourceSpecifierFactory uimaFactory = UIMAFramework.getResourceSpecifierFactory();

  /**
   * Converts an Ecore model to a UIMA TypeSytemDescription.
   * 
   * @param aEcoreFilePath
   *          file path to a .ecore model file
   * @param aOptions
   *          a Map defining options for the conversion. Valid keys for this map are defined as
   *          constants on this class.
   * 
   * @return The UIMA TypeSystemDescription corresponding to the Ecore model
   * @throws URISyntaxException
   *           if there is a problem finding or reading the .ecore file
   */
  public static TypeSystemDescription ecore2UimaTypeSystem(String aEcoreFilePath, Map aOptions)
          throws URISyntaxException {
    // register default resource factory
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*",
            new XMIResourceFactoryImpl());
    // create resource set to hold the resource we're loading and its dependent resources
    ResourceSet resourceSet = new ResourceSetImpl();
    // convert file path to absolute path -- seems to be required for propery proxy resolution
    File inputFile = new File(aEcoreFilePath);
    URI absoluteInputURI = URI.createFileURI(inputFile.getAbsolutePath());
    // load the resource
    Resource resource = resourceSet.getResource(absoluteInputURI, true);
    // convert to UIMA TypeSystem
    return ecore2UimaTypeSystem(resource, aOptions);
  }

  /**
   * Converts an Ecore model to a UIMA TypeSytemDescription.
   * 
   * @param aEcoreResource
   *          An EMF Resource containing the Ecore model
   * @param aOptions
   *          a Map defining options for the conversion. Valid keys for this map are defined as
   *          constants on this class.
   * 
   * @return The UIMA TypeSystemDescription corresponding to the Ecore model
   * @throws URISyntaxException
   *           if there is a problem reading from the resource
   */
  public static TypeSystemDescription ecore2UimaTypeSystem(Resource aEcoreResource, Map aOptions)
          throws URISyntaxException {
    if (aOptions == null) {
      aOptions = Collections.EMPTY_MAP;
    }

    TypeSystemDescription tsDesc = uimaFactory.createTypeSystemDescription();

    // try to get descriptive info from EAnnotation with NS "http://uima.apache.org",
    // on the first EPackage in the Resource
    EPackage ePackage = (EPackage) aEcoreResource.getContents().get(0);
    EAnnotation eannot = ePackage.getEAnnotation("http://uima.apache.org");
    if (eannot != null) {
      tsDesc.setName((String) eannot.getDetails().get("name"));
      tsDesc.setDescription((String) eannot.getDetails().get("description"));
      tsDesc.setVendor((String) eannot.getDetails().get("vendor"));
      tsDesc.setVersion((String) eannot.getDetails().get("version"));
    }

    // convert types
    List types = new ArrayList();
    Iterator iter = aEcoreResource.getContents().iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof EPackage) {
        ePackage2UimaTypes((EPackage) obj, types, aOptions);
      }
    }
    TypeDescription[] typeArr = new TypeDescription[types.size()];
    types.toArray(typeArr);
    tsDesc.setTypes(typeArr);
    return tsDesc;
  }

  private static void ePackage2UimaTypes(EPackage aEPackage, List aResultTypes, Map aOptions)
          throws URISyntaxException {
    String nsUri = aEPackage.getNsURI();
    String uimaNamespace = namespaceUri2UimaNamespace(nsUri);
    // skip the uima.cas package, since it contains only feature-final built-ins
    if ("uima.cas".equals(uimaNamespace)) {
      return;
    }

    Iterator iter = aEPackage.getEClassifiers().iterator();
    while (iter.hasNext()) {
      Object classifier = iter.next();
      if (classifier instanceof EClass) {
        EClass eclass = (EClass) classifier;
        TypeDescription type = eclass2UimaType(eclass, uimaNamespace, aOptions);
        // skip uima.tcas.Annotation, since it is feature-final
        if (!"uima.tcas.Annotation".equals(type.getName())) {
          aResultTypes.add(type);
        }
      } else if (classifier instanceof EEnum) {
        EEnum eenum = (EEnum) classifier;
        TypeDescription type = eenum2UimaType(eenum, uimaNamespace, aOptions);
        aResultTypes.add(type);
      }
    }
    // now process nested subpckages
    iter = aEPackage.getESubpackages().iterator();
    while (iter.hasNext()) {
      ePackage2UimaTypes((EPackage) iter.next(), aResultTypes, aOptions);
    }
  }

  private static TypeDescription eclass2UimaType(EClass aEClass, String aUimaNamespace, Map aOptions)
          throws URISyntaxException {
    TypeDescription type = uimaFactory.createTypeDescription();
    // set name
    if (aUimaNamespace != null) {
      type.setName(aUimaNamespace + "." + aEClass.getName());
    } else {
      type.setName(aEClass.getName());
    }
    // try to get desecription from EAnnotation
    EAnnotation eannot = aEClass.getEAnnotation("http://uima.apache.org");
    if (eannot != null) {
      type.setDescription((String) eannot.getDetails().get("description"));
    }
    // set supertype
    EList supertypes = aEClass.getESuperTypes();
    if (supertypes.isEmpty()) // supertype not defined in the Ecore model
    {
      if (aOptions.get(OPTION_CREATE_ANNOTATION_SUBTYPES) == Boolean.FALSE) {
        type.setSupertypeName(CAS.TYPE_NAME_TOP);
      } else {
        // if this class has "begin" and "end" attributes of type EInt, make it a subtype of
        // annotation
        EStructuralFeature begin = aEClass.getEStructuralFeature("begin");
        EStructuralFeature end = aEClass.getEStructuralFeature("end");
        if (begin != null && end != null && begin.getEType() == EcorePackage.eINSTANCE.getEInt()
                && end.getEType() == EcorePackage.eINSTANCE.getEInt()) {
          type.setSupertypeName(CAS.TYPE_NAME_ANNOTATION);
        } else {
          type.setSupertypeName(CAS.TYPE_NAME_TOP);
        }
      }
    } else {
      EClass supertype = (EClass) supertypes.get(0);
      // if the supertype is EObject, translate that to uima.cas.TOP
      if (supertype.equals(EcorePackage.eINSTANCE.getEObject())) {
        type.setSupertypeName(CAS.TYPE_NAME_TOP);
      }
      // otherwise translate the name according to our conventions
      String uimaSupertypeName = getUimaTypeName(supertype, false, aOptions);
      type.setSupertypeName(uimaSupertypeName);

      // if there are multiple supertypes, the first one is arbitrarily chosen
      // as the single supertype for the UIMA type. Other features are copied-down.
      if (supertypes.size() > 1) {
        System.err.println("Warning: EClass " + aEClass.getName()
                + " defines multiple supertypes. " + "The UIMA supertype will be "
                + type.getSupertypeName()
                + "; features inherited from other supertypes will be copied down.");
      }
    }
    // set features
    EList eFeatures = aEClass.getEStructuralFeatures();
    Iterator iter = eFeatures.iterator();
    List uimaFeatures = new ArrayList();
    while (iter.hasNext()) {
      EStructuralFeature eFeat = (EStructuralFeature) iter.next();
      FeatureDescription uimaFeat = eStructuralFeature2UimaFeature(eFeat, aOptions);
      uimaFeatures.add(uimaFeat);
    }
    // copy down features from additional supertypes
    for (int i = 1; i < supertypes.size(); i++) {
      EClass copyFrom = (EClass) supertypes.get(i);
      EList copyFeatures = copyFrom.getEStructuralFeatures();
      Iterator iter2 = copyFeatures.iterator();
      while (iter2.hasNext()) {
        EStructuralFeature eFeat = (EStructuralFeature) iter2.next();
        // do not copy if this feature is a duplicate of one defined on the class
        // or inherited from its primary supertype
        EList locallyDefinedFeatures = aEClass.getEStructuralFeatures();
        EList firstSupertypesFeatures = ((EClass) supertypes.get(0)).getEAllStructuralFeatures();
        if (!containsNamedElement(locallyDefinedFeatures, eFeat.getName())
                && !containsNamedElement(firstSupertypesFeatures, eFeat.getName())) {
          FeatureDescription uimaFeat = eStructuralFeature2UimaFeature(eFeat, aOptions);
          uimaFeatures.add(uimaFeat);
        }
      }
    }

    FeatureDescription[] featureArr = new FeatureDescription[uimaFeatures.size()];
    uimaFeatures.toArray(featureArr);
    type.setFeatures(featureArr);
    return type;
  }

  private static boolean containsNamedElement(EList locallyDefinedFeatures, String name) {
    Iterator iter = locallyDefinedFeatures.iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof ENamedElement) {
        if (name.equals(((ENamedElement) obj).getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private static TypeDescription eenum2UimaType(EEnum aEEnum, String aUimaNamespace, Map aOptions)
          throws URISyntaxException {
    TypeDescription type = uimaFactory.createTypeDescription();
    // set name
    if (aUimaNamespace != null) {
      type.setName(aUimaNamespace + "." + aEEnum.getName());
    } else {
      type.setName(aEEnum.getName());
    }
    // set supetype to String
    type.setSupertypeName(CAS.TYPE_NAME_STRING);
    // try to get desecription from EAnnotation
    EAnnotation eannot = aEEnum.getEAnnotation("http://uima.apache.org");
    if (eannot != null) {
      type.setDescription((String) eannot.getDetails().get("description"));
    }
    // set allowed values
    EList literals = aEEnum.getELiterals();
    AllowedValue[] vals = new AllowedValue[literals.size()];
    for (int i = 0; i < literals.size(); i++) {
      EEnumLiteral literal = (EEnumLiteral) literals.get(i);
      vals[i] = uimaFactory.createAllowedValue();
      vals[i].setString(literal.getName());
      EAnnotation literalAnnot = literal.getEAnnotation("http://uima.apache.org");
      if (literalAnnot != null) {
        vals[i].setDescription((String) literalAnnot.getDetails().get("description"));
      }
    }
    type.setAllowedValues(vals);
    return type;
  }

  /**
   * @param attr -
   * @return -
   */
  private static FeatureDescription eStructuralFeature2UimaFeature(
          EStructuralFeature aStructuralFeature, Map aOptions) throws URISyntaxException {
    FeatureDescription feat = uimaFactory.createFeatureDescription();
    feat.setName(aStructuralFeature.getName());
    String rangeTypeName = null;
    String elementTypeName = null;
    EAnnotation eannot = aStructuralFeature.getEAnnotation("http://uima.apache.org");
    if (eannot != null) {
      feat.setDescription((String) eannot.getDetails().get("description"));
      // the UIMA type name to use may be recorded as an EAnnotation; this is
      // particularly important for arrays and lists, since Ecore doesn't distinguish between
      // these two possible implementations for a multi-valued property
      rangeTypeName = (String) eannot.getDetails().get("uimaType");
      // the elemnt type may also be specified as an EAnnotation; this is
      // used for the case where an FSArray or FSList is NOT represented
      // as a multi-valued property
      elementTypeName = (String) eannot.getDetails().get("elementType");
    }
    EClassifier attrRangeType = aStructuralFeature.getEType();

    // if range type wasn't specified in an EAnnotation, compute it ourselves
    if (rangeTypeName == null) {
      rangeTypeName = getUimaTypeName(attrRangeType, aStructuralFeature.isMany(), aOptions);
    }
    feat.setRangeTypeName(rangeTypeName);

    if (aStructuralFeature.isMany()) {
      // set the element type of the array/list to the EType of the structural feature
      // (except primitive, or TOP, which are assumed)
      String uimaElementType = getUimaTypeName(attrRangeType, false, aOptions);
      if (!CAS.TYPE_NAME_INTEGER.equals(uimaElementType)
              && !CAS.TYPE_NAME_FLOAT.equals(uimaElementType)
              && !CAS.TYPE_NAME_STRING.equals(uimaElementType)
              && !CAS.TYPE_NAME_TOP.equals(uimaElementType)
              && !CAS.TYPE_NAME_BYTE.equals(uimaElementType)
              && !CAS.TYPE_NAME_SHORT.equals(uimaElementType)
              && !CAS.TYPE_NAME_LONG.equals(uimaElementType)
              && !CAS.TYPE_NAME_DOUBLE.equals(uimaElementType)
              && !CAS.TYPE_NAME_BOOLEAN.equals(uimaElementType)) {
        feat.setElementType(uimaElementType);
      }
    } else if (!aStructuralFeature.getEType().equals(EcorePackage.eINSTANCE.getEByteArray())) {
      // if in Ecore we have a single-valued property whose range type is an array or list,
      // we need to set "multiple references allowed" to true in the UIMA type system
      // (exception: don't do this for the EByteArray data type, which is implicilty a
      // multi-valued type)
      if (isArrayOrList(rangeTypeName)) {
        feat.setMultipleReferencesAllowed(Boolean.TRUE);
        // also, set element type if one was contained in the EAnnotation
        feat.setElementType(elementTypeName);
      }
    }
    return feat;
  }

  private static boolean isArrayOrList(String rangeTypeName) {
    return CAS.TYPE_NAME_FS_LIST.equals(rangeTypeName)
            || CAS.TYPE_NAME_INTEGER_LIST.equals(rangeTypeName)
            || CAS.TYPE_NAME_FLOAT_LIST.equals(rangeTypeName)
            || CAS.TYPE_NAME_STRING_LIST.equals(rangeTypeName)
            || CAS.TYPE_NAME_FS_ARRAY.equals(rangeTypeName)
            || CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName)
            || CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName)
            || CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName)
            || CAS.TYPE_NAME_BYTE_ARRAY.equals(rangeTypeName)
            || CAS.TYPE_NAME_SHORT_ARRAY.equals(rangeTypeName)
            || CAS.TYPE_NAME_LONG_ARRAY.equals(rangeTypeName)
            || CAS.TYPE_NAME_DOUBLE_ARRAY.equals(rangeTypeName)
            || CAS.TYPE_NAME_BOOLEAN_ARRAY.equals(rangeTypeName);

  }

  private static String getUimaTypeName(EClassifier aEcoreType, boolean aMultiValued, Map aOptions)
          throws URISyntaxException {
    boolean useUimaLists = Boolean.TRUE.equals(aOptions.get(OPTION_GENERATE_UIMA_LIST_TYPES));

    if (aEcoreType.eIsProxy()) {
      // try to resolve
      aEcoreType = (EClassifier) EcoreUtil.resolve(aEcoreType, aEcoreType);
      if (aEcoreType.eIsProxy()) {
        throw new UIMARuntimeException(UIMARuntimeException.ECORE_UNRESOLVED_PROXY,
                new Object[] { aEcoreType.toString() });
      }
    }

    if (aEcoreType instanceof EClass || aEcoreType instanceof EEnum) {
      // maps to non-primitive UIMA type
      if (aMultiValued) {
        // UIMA doesn't have typed arrays or lists of nonprimitives
        return useUimaLists ? CAS.TYPE_NAME_FS_LIST : CAS.TYPE_NAME_FS_ARRAY;
      }

      // Derive type name from package name
      EPackage epackage = aEcoreType.getEPackage();
      if (epackage != null) {
        String uimaNamespace = namespaceUri2UimaNamespace(epackage.getNsURI());
        if (uimaNamespace != null)
          return uimaNamespace + '.' + aEcoreType.getName();
        else
          return aEcoreType.getName();
      } else {
        return aEcoreType.getName();
      }
    } else // primitive type
    {
      if (aEcoreType.equals(EcorePackage.eINSTANCE.getEInt())) {
        return aMultiValued ? (useUimaLists ? CAS.TYPE_NAME_INTEGER_LIST
                : CAS.TYPE_NAME_INTEGER_ARRAY) : CAS.TYPE_NAME_INTEGER;
      } else if (aEcoreType.equals(EcorePackage.eINSTANCE.getEShort())) {
        return aMultiValued ? CAS.TYPE_NAME_SHORT_ARRAY : CAS.TYPE_NAME_SHORT;
      } else if (aEcoreType.equals(EcorePackage.eINSTANCE.getELong())) {
        return aMultiValued ? CAS.TYPE_NAME_LONG_ARRAY : CAS.TYPE_NAME_LONG;
      } else if (aEcoreType.equals(EcorePackage.eINSTANCE.getEByte())) {
        return aMultiValued ? CAS.TYPE_NAME_BYTE_ARRAY : CAS.TYPE_NAME_BYTE;
      } else if (aEcoreType.equals(EcorePackage.eINSTANCE.getEFloat())) {
        return aMultiValued ? (useUimaLists ? CAS.TYPE_NAME_FLOAT_LIST : CAS.TYPE_NAME_FLOAT_ARRAY)
                : CAS.TYPE_NAME_FLOAT;
      } else if (aEcoreType.equals(EcorePackage.eINSTANCE.getEDouble())) {
        return aMultiValued ? CAS.TYPE_NAME_DOUBLE_ARRAY : CAS.TYPE_NAME_DOUBLE;
      } else if (aEcoreType.equals(EcorePackage.eINSTANCE.getEBoolean())) {
        return aMultiValued ? CAS.TYPE_NAME_BOOLEAN_ARRAY : CAS.TYPE_NAME_BOOLEAN;
      }
      // Ecore has a special type EByteArray that we use instead of a
      // multi-valued EByte property. This gives a slightly more efficient
      // serialization
      else if (aEcoreType.equals(EcorePackage.eINSTANCE.getEByteArray())) {
        return CAS.TYPE_NAME_BYTE_ARRAY;
      } else // any other datatype maps to String
      {
        if (!aEcoreType.equals(EcorePackage.eINSTANCE.getEString())) {
          System.err.println("Warning: unknown EDataType " + aEcoreType.getName()
                  + " being mapped to uima.cas.String.");
        }
        return aMultiValued ? (useUimaLists ? CAS.TYPE_NAME_STRING_LIST
                : CAS.TYPE_NAME_STRING_ARRAY) : CAS.TYPE_NAME_STRING;
      }
    }
  }

  private static String namespaceUri2UimaNamespace(String nsUri) throws URISyntaxException {
    // Check for the special "no namespace URI", which maps to the null UIMA namespace
    if (XmiCasSerializer.DEFAULT_NAMESPACE_URI.equals(nsUri)) {
      return null;
    }
    // Our convention is that the UIMA namespace is the URI path, with leading slashes
    // removed, trailing ".ecore" removed, and internal slashes converted to dots
    java.net.URI uri = new java.net.URI(nsUri);
    String uimaNs = uri.getPath();
    if (uimaNs == null) {
      // The URI is a URN
      uimaNs = uri.getSchemeSpecificPart();
      uimaNs = uimaNs.replace(':', '.');
    } else {
      // The URI is a URL
      while (uimaNs.startsWith("/")) {
	uimaNs = uimaNs.substring(1);
      }
      if (uimaNs.endsWith(".ecore")) {
	uimaNs = uimaNs.substring(0, uimaNs.length() - 6);
      }
      uimaNs = uimaNs.replace('/', '.');
    }
    uimaNs = uimaNs.replace('-', '_');
    return uimaNs;
  }

  /**
   * Main program. Takes two arguments: the filename of an input .ecore file and the filename of the
   * UIMA TypeSystem file to generate.
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("Usage: java " + Ecore2UimaTypeSystem.class.getName()
              + " <ecore filename> <filename of UIMA TypeSystem file to generate>");
      return;
    }
    if (!new File(args[0]).exists()) {
      System.err.println("File " + args[0] + " does not exist");
      return;
    }

    Map options = new HashMap();
    // options.put(OPTION_GENERATE_UIMA_LIST_TYPES, Boolean.TRUE);
    TypeSystemDescription tsDesc = ecore2UimaTypeSystem(args[0], options);

    FileOutputStream os = new FileOutputStream(args[1]);
    try {
      tsDesc.toXML(os);
    } finally {
      os.close();
    }

    // test creating a CAS
    try {
      CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[0]);
    } catch (Exception e) {
      System.err
              .println("Warning: CAS could not be created from the output type system.  The following problem occurred:");
      System.err.println(e.getMessage());
    }
  }
}
