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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Converts a UIMA TypeSystemDescription to an Ecore model.
 */
public class UimaTypeSystem2Ecore {
  /**
   * Converts a UIMA TypeSystem descriptor to an Ecore model
   * 
   * @param aUimaTypeSystemFilePath
   *          file path to UIMA TypeSystem descritpor
   * @param aOutputResource
   *          An EMF Resource to be populated with the Ecore model
   * @param aOptions
   *          a Map defining options for the conversion. Valid keys for this map are defined as
   *          constants on this class.
   * 
   * @throws InvalidXMLException
   *           if the TypeSystem descriptor, or one of its imports, is not valid or if there are
   *           duplicate, inconsistent definitions of the same type.
   * @throws IOException
   *           if an failure occur while reading the descriptor file
   */
  public static void uimaTypeSystem2Ecore(String aUimaTypeSystemFilePath, Resource aOutputResource,
          Map aOptions) throws InvalidXMLException, IOException {
    TypeSystemDescription tsDesc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(aUimaTypeSystemFilePath));
    uimaTypeSystem2Ecore(tsDesc, aOutputResource, aOptions);
  }

  /**
   * Converts a UIMA TypeSystemDescription to an Ecore model
   * 
   * @param aTypeSystem
   *          UIMA TypeSystemDescription object to convert
   * @param aOutputResource
   *          An EMF Resource to be populated with the Ecore model
   * @param aOptions
   *          a Map defining options for the conversion. Valid keys for this map are defined as
   *          constants on this class.
   * 
   * @throws InvalidXMLException
   *           if the TypeSystem descriptor imports another descriptor that could not be
   *           successfully parsed, or if there are duplicate, inconsistent definitions of the same
   *           type.
   */
  public static void uimaTypeSystem2Ecore(TypeSystemDescription aTypeSystem,
          Resource aOutputResource, Map aOptions) throws InvalidXMLException {
    uimaTypeSystem2Ecore(aTypeSystem, aOutputResource, aOptions, null);
  }

  /**
   * Converts a UIMA TypeSystemDescription to an Ecore model
   * 
   * @param aTypeSystem
   *          UIMA TypeSystemDescription object to convert
   * @param aOutputResource
   *          An EMF Resource to be populated with the Ecore model
   * @param aOptions
   *          a Map defining options for the conversion. Valid keys for this map are defined as
   *          constants on this class.
   * @param aSchemaLocationMap
   *          optional parameter - if non-null, this map will be populated with (Namespace URI,
   *          Schema Location) pairs, suitable for inclusion in the "schemaLocation" attribute of
   *          XMI instance documents.
   */
  public static void uimaTypeSystem2Ecore(TypeSystemDescription aTypeSystem,
          Resource aOutputResource, Map aOptions, Map aSchemaLocationMap)
          throws InvalidXMLException {
    // Add the default definition of uima.tcas.DocumentAnnotation. If the
    // user also defines this type (with additional features), it will be merged
    // with this. First clone the aTypeSystem object so user won't notice
    // we have added a new type definition to their TypeSystemDescription.
    aTypeSystem = (TypeSystemDescription) aTypeSystem.clone();
    TypeDescription docAnnotType = aTypeSystem.addType("uima.tcas.DocumentAnnotation", "",
            "uima.tcas.Annotation");
    docAnnotType.addFeature("language", "", "uima.cas.String");

    // resolve imports
    aTypeSystem.resolveImports();

    // merge, to eliminate duplicate type definitions
    try {
      aTypeSystem = CasCreationUtils.mergeTypeSystems(Arrays.asList(new TypeSystemDescription[] { aTypeSystem }));
    } catch (ResourceInitializationException e) {
      throw new InvalidXMLException(e);
    }

    if (aOptions == null) {
      aOptions = Collections.EMPTY_MAP;
    }

    // load Ecore model for the UIMA Built-in types
    ResourceSet resSet = aOutputResource.getResourceSet();
    if (resSet == null) {
      resSet = new ResourceSetImpl();
      resSet.getResources().add(aOutputResource);
    }
    loadUimaBuiltinsEcore(resSet, aSchemaLocationMap);

    // Do this in two passes. First pass creates EPackages, EClasses, and EEnums (for string
    // subtypes)
    // Second pass sets supertypes and creates EStructuralFeatures
    TypeDescription[] types = aTypeSystem.getTypes();
    EPackage firstPackage = null;
    for (int i = 0; i < types.length; i++) {
      TypeDescription type = types[i];
      EClassifier eclassifier = uimaType2EClassifier(type, aOptions);
      // EPackages may also have been created. Add the root EPackage to the resource.
      EPackage rootPackage = eclassifier.getEPackage();
      while (rootPackage.getESuperPackage() != null)
        rootPackage = rootPackage.getESuperPackage();
      aOutputResource.getContents().add(rootPackage);
      if (aSchemaLocationMap != null) {
        String schemaLoc = aOutputResource.getURI() + "#"
                + aOutputResource.getURIFragment(eclassifier.getEPackage());
        aSchemaLocationMap.put(eclassifier.getEPackage().getNsURI(), schemaLoc);
      }
      if (firstPackage == null) {
        firstPackage = eclassifier.getEPackage();
      }
    }

    // Now make second pass to set supertype and create feautres
    for (int i = 0; i < types.length; i++) {
      TypeDescription type = types[i];
      EClassifier eclassifier = lookupEClassifierForType(type.getName());
      if (eclassifier instanceof EClass) {
        EClass eclass = (EClass) eclassifier;
        // set supertype
        String supertypeName = type.getSupertypeName();
        EClassifier superclass = lookupEClassifierForType(supertypeName); // creates EClass if not
        // already existing
        eclass.getESuperTypes().add((EClass)superclass);

        // set features
        FeatureDescription[] features = type.getFeatures();
        for (int j = 0; j < features.length; j++) {
          eclass.getEStructuralFeatures()
                  .add(uimaFeature2EStructuralFeature(features[j], aOptions));
        }
      }
    }

    // add descriptive type system attributes as EAnnotations on first package
    EAnnotation eannot = EcoreFactory.eINSTANCE.createEAnnotation();
    eannot.setSource("http://uima.apache.org");
    if (aTypeSystem.getName() != null && aTypeSystem.getName().length() > 0)
      eannot.getDetails().put("name", aTypeSystem.getName());
    if (aTypeSystem.getDescription() != null && aTypeSystem.getDescription().length() > 0)
      eannot.getDetails().put("description", aTypeSystem.getDescription());
    if (aTypeSystem.getVersion() != null && aTypeSystem.getVersion().length() > 0)
      eannot.getDetails().put("version", aTypeSystem.getVersion());
    if (aTypeSystem.getVendor() != null && aTypeSystem.getVendor().length() > 0)
      eannot.getDetails().put("vendor", aTypeSystem.getVendor());
    firstPackage.getEAnnotations().add(eannot);
  }

  private static Resource loadUimaBuiltinsEcore(ResourceSet resourceSet, Map aSchemaLocationMap) {
    // load Ecore model for UIMA built-in types (use classloader to locate)
    URL uimaEcoreUrl = UimaTypeSystem2Ecore.class.getResource("/uima.ecore");
    if (uimaEcoreUrl == null) {
      throw new UIMARuntimeException(UIMARuntimeException.UIMA_ECORE_NOT_FOUND, new Object[0]);
    }
    Resource uimaEcoreResource = resourceSet.getResource(URI.createURI(uimaEcoreUrl.toString()),
            true);
    // register core UIMA packages (I'm surprised I need to do this manually)
    TreeIterator iter = uimaEcoreResource.getAllContents();
    while (iter.hasNext()) {
      Object current = iter.next();
      if (current instanceof EPackage) {
        EPackage pkg = (EPackage) current;
        EPackage.Registry.INSTANCE.put(pkg.getNsURI(), pkg);
        if (aSchemaLocationMap != null) {
          String schemaLoc = uimaEcoreResource.getURI() + "#"
                  + uimaEcoreResource.getURIFragment(pkg);
          aSchemaLocationMap.put(pkg.getNsURI(), schemaLoc);
        }
      }
    }
    return uimaEcoreResource;
  }

  private static EClassifier uimaType2EClassifier(TypeDescription aType, Map aOptions) {
    // separate name into package name and class name
    String fullTypeName = aType.getName();
    String uimaNamespace, shortTypeName;
    int lastDot = fullTypeName.lastIndexOf('.');
    if (lastDot <= 0) {
      uimaNamespace = null;
      shortTypeName = fullTypeName;
    } else {
      uimaNamespace = fullTypeName.substring(0, lastDot);
      shortTypeName = fullTypeName.substring(lastDot + 1);
    }

    // does EPackage already exist for this URI?
    EPackage ePackage = uimaNamespace2EPackage(uimaNamespace);

    EClassifier eclassifier;
    // if aType is a "subtype" of uima.cas.String, create an EEnum for it
    if (CAS.TYPE_NAME_STRING.equals(aType.getSupertypeName())) {
      eclassifier = EcoreFactory.eINSTANCE.createEEnum();
      AllowedValue[] vals = aType.getAllowedValues();
      for (int i = 0; i < vals.length; i++) {
        EEnumLiteral literal = EcoreFactory.eINSTANCE.createEEnumLiteral();
        literal.setValue(i);
        literal.setName(vals[i].getString());
        if (vals[i].getDescription() != null && vals[i].getDescription().length() > 0) {
          EAnnotation eannot = EcoreFactory.eINSTANCE.createEAnnotation();
          eannot.setSource("http://uima.apache.org");
          eannot.getDetails().put("description", vals[i].getDescription());
          literal.getEAnnotations().add(eannot);
        }
        ((EEnum) eclassifier).getELiterals().add(literal);
      }
    } else {
      // create EClass
      eclassifier = EcoreFactory.eINSTANCE.createEClass();
    }

    // set name of EClassifier
    eclassifier.setName(shortTypeName);
    // add to package
    ePackage.getEClassifiers().add(eclassifier);
    // set description as EAnnotation
    if (aType.getDescription() != null && aType.getDescription().length() > 0) {
      EAnnotation eannot = EcoreFactory.eINSTANCE.createEAnnotation();
      eannot.setSource("http://uima.apache.org");
      eannot.getDetails().put("description", aType.getDescription());
      eclassifier.getEAnnotations().add(eannot);
    }
    return eclassifier;
  }

  private static EStructuralFeature uimaFeature2EStructuralFeature(FeatureDescription aFeature,
          Map aOptions) {
    String range = aFeature.getRangeTypeName();
    boolean multiRefAllowed = aFeature.getMultipleReferencesAllowed() == null ? false : aFeature
            .getMultipleReferencesAllowed().booleanValue();
    EStructuralFeature efeat;
    // map primitive types to EAttributes
    if (CAS.TYPE_NAME_STRING.equals(range)) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEString());
    } else if (CAS.TYPE_NAME_INTEGER.equals(range)) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEInt());
    } else if (CAS.TYPE_NAME_FLOAT.equals(range)) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEFloat());
    } else if (CAS.TYPE_NAME_BYTE.equals(range)) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEByte());
    } else if (CAS.TYPE_NAME_SHORT.equals(range)) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEShort());
    } else if (CAS.TYPE_NAME_LONG.equals(range)) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getELong());
    } else if (CAS.TYPE_NAME_DOUBLE.equals(range)) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEDouble());
    } else if (CAS.TYPE_NAME_BOOLEAN.equals(range)) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEBoolean());
    }
    // map arrays and lists to multivalued EAttributes if multiple references not allowed
    else if ((CAS.TYPE_NAME_STRING_ARRAY.equals(range) || CAS.TYPE_NAME_STRING_LIST.equals(range))
            && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEString());
      efeat.setUpperBound(-1);
    } else if ((CAS.TYPE_NAME_INTEGER_ARRAY.equals(range) || CAS.TYPE_NAME_INTEGER_LIST
            .equals(range))
            && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEInt());
      efeat.setUpperBound(-1);
    } else if ((CAS.TYPE_NAME_FLOAT_ARRAY.equals(range) || CAS.TYPE_NAME_FLOAT_LIST.equals(range))
            && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEFloat());
      efeat.setUpperBound(-1);
    } else if (CAS.TYPE_NAME_SHORT_ARRAY.equals(range) && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEShort());
      efeat.setUpperBound(-1);
    } else if (CAS.TYPE_NAME_LONG_ARRAY.equals(range) && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getELong());
      efeat.setUpperBound(-1);
    } else if (CAS.TYPE_NAME_DOUBLE_ARRAY.equals(range) && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEDouble());
      efeat.setUpperBound(-1);
    } else if (CAS.TYPE_NAME_BOOLEAN_ARRAY.equals(range) && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEBoolean());
      efeat.setUpperBound(-1);
    }
    // Ecore has a special type EByteArray that we use instead of a
    // multi-valued EByte property. This gives a slightly more efficient
    // serialization.
    else if (CAS.TYPE_NAME_BYTE_ARRAY.equals(range) && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEAttribute();
      efeat.setEType(EcorePackage.eINSTANCE.getEByteArray());
    }
    // FSArrays and FSLists map to multivalued references if multiple references not allowed
    else if ((CAS.TYPE_NAME_FS_ARRAY.equals(range) || CAS.TYPE_NAME_FS_LIST.equals(range))
            && !multiRefAllowed) {
      efeat = EcoreFactory.eINSTANCE.createEReference();
      String elementType = aFeature.getElementType();
      if (elementType == null) {
        elementType = CAS.TYPE_NAME_TOP;
      }
      efeat.setEType(lookupEClassifierForType(elementType));
      efeat.setUpperBound(-1);
    } else // non-primitive, non-array, non-list type.
    // map to EAttribute if it's an EEnum, otherwise map to EReference
    {
      EClassifier etype = lookupEClassifierForType(range);
      if (etype instanceof EEnum) {
        efeat = EcoreFactory.eINSTANCE.createEAttribute();
      } else {
        efeat = EcoreFactory.eINSTANCE.createEReference();
      }
      efeat.setEType(etype);
    }

    efeat.setName(aFeature.getName());

    // use EAnnotation to record:
    // - the description of the feature
    // - for multi-valued properties, the name of the UIMA type used to
    // implement it (to distinguish between array and list)
    // - for FSList or FSArray that are NOT represented by multi-valued
    // properties, the element type
    if ((aFeature.getDescription() != null && aFeature.getDescription().length() > 0)
            || efeat.isMany() || aFeature.getElementType() != null) {
      EAnnotation eannot = EcoreFactory.eINSTANCE.createEAnnotation();
      eannot.setSource("http://uima.apache.org");
      if (aFeature.getDescription() != null && aFeature.getDescription().length() > 0) {
        eannot.getDetails().put("description", aFeature.getDescription());
      }
      if (efeat.isMany()) {
        eannot.getDetails().put("uimaType", aFeature.getRangeTypeName());
      }
      if (!efeat.isMany() && aFeature.getElementType() != null) {
        eannot.getDetails().put("elementType", aFeature.getElementType());
      }
      efeat.getEAnnotations().add(eannot);
    }
    return efeat;
  }

  private static EClassifier lookupEClassifierForType(String aFullTypeName) {
    // separate name into package name and class name
    String uimaNamespace, shortTypeName;
    int lastDot = aFullTypeName.lastIndexOf('.');
    if (lastDot <= 0) {
      uimaNamespace = null;
      shortTypeName = aFullTypeName;
    } else {
      uimaNamespace = aFullTypeName.substring(0, lastDot);
      shortTypeName = aFullTypeName.substring(lastDot + 1);
    }
    String nsUri = uimaNamespace2NamespaceUri(uimaNamespace);

    // does EPackage already exist for this URI?
    EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(nsUri);
    if (ePackage == null) {
      return null;
    }
    return ePackage.getEClassifier(shortTypeName);
  }

  /**
   * Gets or creates an EPackage for a UIMA namespace. Actually will create a whole chain of nested
   * EPackages, one for each component of the UIMA namespace, but only the leaf node of the chain
   * will be returned.
   * 
   * @param uimaNamespace
   *          UIMA namespace
   * @return EPackage corresponding to this namespace.
   */
  private static EPackage uimaNamespace2EPackage(String uimaNamespace) {
    // convert UIMA namespace (dotted string) to namespace URI
    String nsUri = uimaNamespace2NamespaceUri(uimaNamespace);
    // see if package already exists for this URI
    EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(nsUri);
    if (ePackage == null) {
      // package name is last component of namespace.
      // all other components form the parent namespace
      String parentNamespace = null;
      String packageName;
      if (uimaNamespace != null) {
        int lastDot = uimaNamespace.lastIndexOf('.');
        packageName = uimaNamespace.substring(lastDot + 1);
        if (lastDot > 0) {
          parentNamespace = uimaNamespace.substring(0, lastDot);
        }
      } else {
        packageName = "noNamespace";
      }

      // create Package
      ePackage = EcoreFactory.eINSTANCE.createEPackage();
      ePackage.setNsURI(nsUri);
      ePackage.setName(packageName);
      EPackage.Registry.INSTANCE.put(nsUri, ePackage);

      // get or create SuperPackage if any
      if (parentNamespace != null) {
        EPackage superPackage = uimaNamespace2EPackage(parentNamespace);
        superPackage.getESubpackages().add(ePackage);
      }
    }
    return ePackage;
  }

  private static String uimaNamespace2NamespaceUri(String uimaNamespace) {
    if (uimaNamespace == null || uimaNamespace.length() == 0) {
      return XmiCasSerializer.DEFAULT_NAMESPACE_URI;
    }
    // Our convention is that the Namespace URI is "http:///", followed by the UIMA namespace, with
    // dots converted to slashes, and with ".ecore" appended. (This is EMF's convention for
    // constructing a namespace URI from a Java package name.)
    return "http:///" + uimaNamespace.replace('.', '/') + ".ecore";
  }

  /**
   * Main program. Takes two arguments: the filename of an input TypeSystem descriptor file and the
   * filename of the Ecore/XMI file to generate.
   */
  public static void main(String[] args) throws Exception {
    // register default resource factory
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*",
            new XMIResourceFactoryImpl());

    ResourceSet resourceSet = new ResourceSetImpl();
    URI outputURI = URI.createFileURI(args[1]);
    Resource outputResource = resourceSet.createResource(outputURI);
    Map options = new HashMap();
    // options.put(OPTION_PRESERVE_UIMA_LIST_TYPES, Boolean.TRUE);
    uimaTypeSystem2Ecore(args[0], outputResource, options);
    outputResource.save(null);
  }
}
