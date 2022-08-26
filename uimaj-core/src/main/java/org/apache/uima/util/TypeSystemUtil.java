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

package org.apache.uima.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelTypeSystem;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.spi.TypeSystemDescriptionProvider;

public class TypeSystemUtil {

  /**
   * Loads type system descriptions and resolves their imports. For example when you place a
   * {@link TypeSystemDescriptionProvider} implementation and place the type system descriptions it
   * should provide in the same package, you can use this method to conveniently load them simply by
   * name in the provider implementation.
   * 
   * <pre>
   * public class MyTypeSystemDescriptionProvider implements TypeSystemDescriptionProvider {
   *   {@code @Override}
   *   public List<TypeSystemDescription> listTypeSystemDescriptions() {
   *     return TypeSystemUtil.loadTypeSystemDescriptionsFromClasspath(getClass(), "TypeSystem1.xml",
   *             "TypeSystem2.xml");
   *   }
   * }
   * </pre>
   * 
   * 
   * @param aContext
   *          a context class. If the locations are not absolute, then they are looked up relative
   *          to this context class as per {@link Class#getResource(String)}.
   * @param typeSystemDescriptionLocations
   *          type system description locations to load.
   * @return list of the loaded and resolved descriptions.
   */
  public static List<TypeSystemDescription> loadTypeSystemDescriptionsFromClasspath(
          Class<?> aContext, String... typeSystemDescriptionLocations) {

    ResourceManager resMgr = new ResourceManager_impl(aContext.getClassLoader());
    try {
      List<TypeSystemDescription> typeSystemDescriptions = new ArrayList<>();

      for (String typeSystem : typeSystemDescriptionLocations) {
        URL resource = aContext.getResource(typeSystem);
        if (resource == null) {
          UIMAFramework.getLogger()
                  .error("Unable to locate type system description as a resource [{}]", typeSystem);
          continue;
        }

        try {
          TypeSystemDescription tsd = UIMAFramework.getXMLParser()
                  .parseTypeSystemDescription(new XMLInputSource(resource));
          tsd.resolveImports(resMgr);
          typeSystemDescriptions.add(tsd);
        } catch (InvalidXMLException | IOException e) {
          UIMAFramework.getLogger().error("Error loading type system description [{}] from [{}]",
                  typeSystem, resource, e);
        }
      }

      return typeSystemDescriptions;
    } finally {
      resMgr.destroy();
    }
  }

  /**
   * Convert a {@link TypeSystem} to an equivalent {@link TypeSystemDescription}.
   * 
   * @param aTypeSystem
   *          type system object to convert
   * @return a TypeSystemDescription that is equivalent to <code>aTypeSystem</code>
   */
  public static TypeSystemDescription typeSystem2TypeSystemDescription(TypeSystem aTypeSystem) {
    ResourceSpecifierFactory fact = UIMAFramework.getResourceSpecifierFactory();
    TypeSystemDescription tsDesc = fact.createTypeSystemDescription();
    Iterator<Type> typeIter = aTypeSystem.getTypeIterator();
    List<TypeDescription> typeDescs = new ArrayList<>();
    while (typeIter.hasNext()) {
      Type type = typeIter.next();
      if (!type.getName().startsWith("uima.cas") && !type.getName().equals("uima.tcas.Annotation")
              && !type.isArray()) {
        typeDescs.add(type2TypeDescription(type, aTypeSystem));
      }
    }
    TypeDescription[] typeDescArr = new TypeDescription[typeDescs.size()];
    typeDescs.toArray(typeDescArr);
    tsDesc.setTypes(typeDescArr);

    return tsDesc;
  }

  /**
   * Convert a {@link Type} to an equivalent {@link TypeDescription}.
   * 
   * @param aType
   *          type object to convert
   * @param aTypeSystem
   *          the TypeSystem that contains <code>aType</code>
   * @return a TypeDescription that is equivalent to <code>aType</code>
   */
  public static TypeDescription type2TypeDescription(Type aType, TypeSystem aTypeSystem) {
    TypeDescription typeDesc = UIMAFramework.getResourceSpecifierFactory().createTypeDescription();
    typeDesc.setName(aType.getName());
    Type superType = aTypeSystem.getParent(aType);
    typeDesc.setSupertypeName(superType.getName());
    // special handling for string subtypes (which have "allowed values", rather than features)
    Type stringType = aTypeSystem.getType("uima.cas.String");
    if (aTypeSystem.subsumes(stringType, aType)) {
      String[] allowedValues = getAllowedValuesForType(aType, aTypeSystem);
      AllowedValue[] avObjs = new AllowedValue[allowedValues.length];
      for (int i = 0; i < allowedValues.length; i++) {
        AllowedValue av = UIMAFramework.getResourceSpecifierFactory().createAllowedValue();
        av.setString(allowedValues[i]);
        avObjs[i] = av;
      }
      typeDesc.setAllowedValues(avObjs);
    } else {
      List<FeatureDescription> featDescs = new ArrayList<>();
      for (Feature feat : aType.getFeatures()) {
        if (!superType.getFeatures().contains(feat)) {
          featDescs.add(feature2FeatureDescription(feat));
        }
      }
      FeatureDescription[] featDescArr = new FeatureDescription[featDescs.size()];
      featDescs.toArray(featDescArr);
      typeDesc.setFeatures(featDescArr);
    }
    return typeDesc;
  }

  /**
   * Convert a {@link Feature} to an equivalent {@link FeatureDescription}.
   * 
   * @param aFeature
   *          feature object to convert
   * @return a FeatureDescription that is equivalent to <code>aFeature</code>
   */
  public static FeatureDescription feature2FeatureDescription(Feature aFeature) {
    FeatureDescription featDesc = UIMAFramework.getResourceSpecifierFactory()
            .createFeatureDescription();
    featDesc.setName(aFeature.getShortName());
    if (aFeature.isMultipleReferencesAllowed()) {
      featDesc.setMultipleReferencesAllowed(true);
    }
    Type rangeType = aFeature.getRange();
    // special check for array range types, which are represented in the CAS as
    // elementType[] but in the descriptor as an FSArray with an <elementType>
    if (rangeType.isArray() && !rangeType.getComponentType().isPrimitive()) {
      featDesc.setRangeTypeName(CAS.TYPE_NAME_FS_ARRAY);
      String elementTypeName = rangeType.getComponentType().getName();
      if (!CAS.TYPE_NAME_TOP.equals(elementTypeName)) {
        featDesc.setElementType(elementTypeName);
      }
    } else {
      featDesc.setRangeTypeName(rangeType.getName());
    }
    return featDesc;
  }

  /**
   * Gets the allowed values for a string subtype.
   * 
   * @param aType
   *          the type, which must be a subtype of uima.cas.String
   * @param aTypeSystem
   *          the type system to use
   * @return array of allowed values for <code>aType</code> TODO - this should be a method on Type.
   */
  public static String[] getAllowedValuesForType(Type aType, TypeSystem aTypeSystem) {
    LowLevelTypeSystem lts = aTypeSystem.getLowLevelTypeSystem();
    return lts.ll_getStringSet(lts.ll_getCodeForType(aType));
  }

}
