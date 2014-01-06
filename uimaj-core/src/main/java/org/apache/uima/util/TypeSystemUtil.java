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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelTypeSystem;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

public class TypeSystemUtil {

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
    ArrayList<TypeDescription> typeDescs = new ArrayList<TypeDescription>();
    while (typeIter.hasNext()) {
      Type type = typeIter.next();
      if (!type.getName().startsWith("uima.cas") && !type.getName().equals("uima.tcas.Annotation") &&
          !type.isArray()) {
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
      ArrayList<FeatureDescription> featDescs = new ArrayList<FeatureDescription>();
      for (Feature feat : aType.getFeatures()){ 
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
    //special check for array range types, which are represented in the CAS as
    //elementType[] but in the descriptor as an FSArray with an <elementType>
    if (rangeType.isArray() && !rangeType.getComponentType().isPrimitive()) {
      featDesc.setRangeTypeName(CAS.TYPE_NAME_FS_ARRAY);
      String elementTypeName = rangeType.getComponentType().getName();
      if (!CAS.TYPE_NAME_TOP.equals(elementTypeName)) {
        featDesc.setElementType(elementTypeName);
      }
    }
    else {
      featDesc.setRangeTypeName(rangeType.getName());
    }
    return featDesc;
  }

  /**
   * Gets the allowed values for a string subtype.
   * 
   * @param aType
   *          the type, which must be a subtype of uima.cas.String
   * @param aTypeSystem the type system to use
   * @return array of allowed values for <code>aType</code> TODO - this should be a method on
   *         Type.
   */
  public static String[] getAllowedValuesForType(Type aType, TypeSystem aTypeSystem) {
    LowLevelTypeSystem lts = aTypeSystem.getLowLevelTypeSystem();
    return lts.ll_getStringSet(lts.ll_getCodeForType(aType));
  }

}
