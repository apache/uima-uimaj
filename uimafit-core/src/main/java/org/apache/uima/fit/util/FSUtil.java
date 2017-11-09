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
package org.apache.uima.fit.util;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.FSCollectionFactory.createArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createBooleanArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createByteArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createDoubleArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSList;
import static org.apache.uima.fit.util.FSCollectionFactory.createFloatArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createFloatList;
import static org.apache.uima.fit.util.FSCollectionFactory.createIntArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createIntegerList;
import static org.apache.uima.fit.util.FSCollectionFactory.createLongArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createShortArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createStringArray;
import static org.apache.uima.fit.util.FSCollectionFactory.createStringList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

public class FSUtil {
  private static Feature getMandatoryFeature(FeatureStructure aFS, String aFeature)
  {
    Feature feat = aFS.getType().getFeatureByBaseName(aFeature);
    
    if (feat == null) {
      throw new IllegalArgumentException("Type [" + aFS.getType() + "] has no feature with name ["
              + aFeature + "]");
    }
    
    return feat;
  }
  
  private static void requireSingleValue(Feature aFeature, Object aArray)
  {
    if (aArray == null) {
      throw new IllegalArgumentException("Cannot set [" + aFeature.getName() + "] to a null value.");
    }
    
    if (Array.getLength(aArray) != 1) {
      throw new IllegalArgumentException("Feature [" + aFeature.getName()
              + "] requires a single value but got " + asList(aArray));
    }
  }
  
  private static boolean isListType(TypeSystem aTS, Type aType)
  {
    return aTS.subsumes(aTS.getType(CAS.TYPE_NAME_LIST_BASE), aType);
  }

  public static boolean hasFeature(FeatureStructure aFS, String aFeature) {
    return aFS.getType().getFeatureByBaseName(aFeature) != null;
  }

  public static boolean isMultiValuedFeature(FeatureStructure aFS, String aFeature) {
    Feature feat = aFS.getType().getFeatureByBaseName(aFeature);

    return isMultiValuedFeature(aFS, feat);
  }

  public static boolean isMultiValuedFeature(FeatureStructure aFS, Feature feat) {
    return isMultiValuedFeature(aFS.getCAS().getTypeSystem(), feat);
  }

  public static boolean isMultiValuedFeature(TypeSystem aTypeSystem, Feature feat) {
    if (feat == null) {
      return false;
    }
    
    return feat.getRange().isArray() || isListType(aTypeSystem, feat.getRange());
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, boolean... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isPrimitive()) {
      requireSingleValue(feat, aValue);
      aFS.setBooleanValue(feat, aValue[0]);
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else {
      aFS.setFeatureValue(feat, createBooleanArray(aFS.getCAS(), aValue));
    }
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, byte... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isPrimitive()) {
      requireSingleValue(feat, aValue);
      aFS.setByteValue(feat, aValue[0]);
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else {
      aFS.setFeatureValue(feat, createByteArray(aFS.getCAS(), aValue));
    }
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, double... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isPrimitive()) {
      requireSingleValue(feat, aValue);
      aFS.setDoubleValue(feat, aValue[0]);
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else {
      aFS.setFeatureValue(feat, createDoubleArray(aFS.getCAS(), aValue));
    }
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, float... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isPrimitive()) {
      requireSingleValue(feat, aValue);
      aFS.setFloatValue(feat, aValue[0]);
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else if (feat.getRange().isArray()) {
      aFS.setFeatureValue(feat, createFloatArray(aFS.getCAS(), aValue));
    }
    else {
      aFS.setFeatureValue(feat, createFloatList(aFS.getCAS(), aValue));
    }
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, int... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isPrimitive()) {
      requireSingleValue(feat, aValue);
      aFS.setIntValue(feat, aValue[0]);
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else if (feat.getRange().isArray()) {
      aFS.setFeatureValue(feat, createIntArray(aFS.getCAS(), aValue));
    }
    else {
      aFS.setFeatureValue(feat, createIntegerList(aFS.getCAS(), aValue));
    }
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, long... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isPrimitive()) {
      requireSingleValue(feat, aValue);
      aFS.setLongValue(feat, aValue[0]);
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else {
      aFS.setFeatureValue(feat, createLongArray(aFS.getCAS(), aValue));
    }
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, short... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isPrimitive()) {
      requireSingleValue(feat, aValue);
      aFS.setShortValue(feat, aValue[0]);
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else {
      aFS.setFeatureValue(feat, createShortArray(aFS.getCAS(), aValue));
    }
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, String... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isPrimitive()) {
      requireSingleValue(feat, aValue);
      aFS.setStringValue(feat, aValue[0]);
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else if (feat.getRange().isArray()) {
      aFS.setFeatureValue(feat, createStringArray(aFS.getCAS(), aValue));
    }
    else {
      aFS.setFeatureValue(feat, createStringList(aFS.getCAS(), aValue));
    }
  }

  public static void setFeature(FeatureStructure aFS, String aFeature, FeatureStructure... aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (feat.getRange().isArray()) {
      aFS.setFeatureValue(feat, createArrayFS(aFS.getCAS(), aValue));
    }
    else if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else if (isListType(aFS.getCAS().getTypeSystem(), feat.getRange())) {
      aFS.setFeatureValue(feat, createFSList(aFS.getCAS(), aValue));
    }
    else {
      requireSingleValue(feat, aValue);
      aFS.setFeatureValue(feat, aValue[0]);
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void setFeature(FeatureStructure aFS, String aFeature, Collection aValue) {
    Feature feat = getMandatoryFeature(aFS, aFeature);
    if (aValue == null) {
      aFS.setFeatureValue(feat, null);
    }
    else if (feat.getRange().isArray()) {
      switch (feat.getRange().getName()) {
        case CAS.TYPE_NAME_BOOLEAN_ARRAY:
          aFS.setFeatureValue(feat, createBooleanArray(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_BYTE_ARRAY:
          aFS.setFeatureValue(feat, createByteArray(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_DOUBLE_ARRAY:
          aFS.setFeatureValue(feat, createDoubleArray(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_FLOAT_ARRAY:
          aFS.setFeatureValue(feat, createFloatArray(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_INTEGER_ARRAY:
          aFS.setFeatureValue(feat, createIntArray(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_LONG_ARRAY:
          aFS.setFeatureValue(feat, createLongArray(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_SHORT_ARRAY:
          aFS.setFeatureValue(feat, createShortArray(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_STRING_ARRAY:
          aFS.setFeatureValue(feat, createStringArray(aFS.getCAS(), aValue));
          break;
        default:
          aFS.setFeatureValue(feat, createArrayFS(aFS.getCAS(), aValue));
          break;
      }
    }
    else {
      switch (feat.getRange().getName()) {
        case CAS.TYPE_NAME_FLOAT_LIST:
          aFS.setFeatureValue(feat, createFloatList(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_INTEGER_LIST:
          aFS.setFeatureValue(feat, createIntegerList(aFS.getCAS(), aValue));
          break;
        case CAS.TYPE_NAME_STRING_LIST:
          aFS.setFeatureValue(feat, createStringList(aFS.getCAS(), aValue));
          break;
        default: 
          aFS.setFeatureValue(feat, createFSList(aFS.getCAS(), aValue));
          break;
      }
    }
  }

  public static <T> T getFeature(FeatureStructure aFS, String aFeature, Class<T> aClazz)
  {
    Feature feat = getMandatoryFeature(aFS, aFeature);

    return getFeature(aFS, feat, aClazz);
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T> T getFeature(FeatureStructure aFS, Feature aFeature, Class<T> aClazz)
  {
    if (aFeature.getRange().isPrimitive()) {
      switch (aFeature.getRange().getName()) {
        case CAS.TYPE_NAME_BOOLEAN:
          return aClazz.cast(aFS.getBooleanValue(aFeature));
        case CAS.TYPE_NAME_BYTE:
          return aClazz.cast(aFS.getByteValue(aFeature));
        case CAS.TYPE_NAME_DOUBLE:
          return aClazz.cast(aFS.getDoubleValue(aFeature));
        case CAS.TYPE_NAME_FLOAT:
          return aClazz.cast(aFS.getFloatValue(aFeature));
        case CAS.TYPE_NAME_INTEGER:
          return aClazz.cast(aFS.getIntValue(aFeature));
        case CAS.TYPE_NAME_LONG:
          return aClazz.cast(aFS.getLongValue(aFeature));
        case CAS.TYPE_NAME_SHORT:
          return aClazz.cast(aFS.getShortValue(aFeature));
        case CAS.TYPE_NAME_STRING:
          return aClazz.cast(aFS.getStringValue(aFeature));
        default:
          throw new IllegalArgumentException("Unable to coerce value of feature [" + aFeature.getName()
                  + "] with type [" + aFeature.getRange().getName() + "] into [" + aClazz.getName() + "]");
      }
    }
    
    // "null" case
    if (aFS.getFeatureValue(aFeature) == null) {
      return null;
    }
    
    // Here we store the values before we coerce them into the final target type
    // "target" is actually an array
    Object target;
    int length;
    
    // Handle case where feature is an array
    if (aFeature.getRange().isArray()) {
      CommonArrayFS source = (CommonArrayFS) aFS.getFeatureValue(aFeature);
      length = source.size();
      switch (aFeature.getRange().getComponentType().getName()) {
        case CAS.TYPE_NAME_BOOLEAN:
          target = new boolean[length];
          ((BooleanArrayFS) source).copyToArray(0, (boolean[]) target, 0, length);
          break;
        case CAS.TYPE_NAME_BYTE:
          target = new byte[length];
          ((ByteArrayFS) source).copyToArray(0, (byte[]) target, 0, length);
          break;
        case CAS.TYPE_NAME_DOUBLE:
          target = new double[length];
          ((DoubleArrayFS) source).copyToArray(0, (double[]) target, 0, length);
          break;
        case CAS.TYPE_NAME_FLOAT:
          target = new float[length];
          ((FloatArrayFS) source).copyToArray(0, (float[]) target, 0, length);
          break;
        case CAS.TYPE_NAME_INTEGER:
          target = new int[length];
          ((IntArrayFS) source).copyToArray(0, (int[]) target, 0, length);
          break;
        case CAS.TYPE_NAME_LONG:
          target = new long[length];
          ((LongArrayFS) source).copyToArray(0, (long[]) target, 0, length);
          break;
        case CAS.TYPE_NAME_SHORT:
          target = new short[length];
          ((ShortArrayFS) source).copyToArray(0, (short[]) target, 0, length);
          break;
        case CAS.TYPE_NAME_STRING:
          target = new String[length];
          ((StringArrayFS) source).copyToArray(0, (String[]) target, 0, length);
          break;
        default:
          if (aClazz.isArray()) {
            target = Array.newInstance(aClazz.getComponentType(), length);
          }
          else {
            target = new FeatureStructure[length];
          }
          ((ArrayFS) source).copyToArray(0, (FeatureStructure[]) target, 0, length);
          break;
      }
    }
    // Handle case where feature is a list
    else if (isListType(aFS.getCAS().getTypeSystem(), aFeature.getRange())) {
      // Get length of list
      length = 0;
      {
        FeatureStructure cur = aFS.getFeatureValue(aFeature);
        // We assume to by facing a non-empty element if it has a "head" feature
        while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
          length++;
          cur = cur.getFeatureValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
        }
      }

      switch (aFeature.getRange().getName()) {
        case CAS.TYPE_NAME_FLOAT_LIST: {
          float[] floatTarget = new float[length];
          int i = 0;
          FeatureStructure cur = aFS.getFeatureValue(aFeature);
          // We assume to by facing a non-empty element if it has a "head" feature
          while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
            floatTarget[i] = cur.getFloatValue(cur.getType().getFeatureByBaseName(
                    CAS.FEATURE_BASE_NAME_HEAD));
            cur = cur.getFeatureValue(cur.getType()
                    .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
          }
          target = floatTarget;
          break;
        }
        case CAS.TYPE_NAME_INTEGER_LIST: {
          int[] intTarget = new int[length];
          int i = 0;
          FeatureStructure cur = aFS.getFeatureValue(aFeature);
          // We assume to by facing a non-empty element if it has a "head" feature
          while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
            intTarget[i] = cur.getIntValue(cur.getType().getFeatureByBaseName(
                    CAS.FEATURE_BASE_NAME_HEAD));
            cur = cur.getFeatureValue(cur.getType()
                    .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
          }
          target = intTarget;
          break;
        }
        case CAS.TYPE_NAME_STRING_LIST: {
          String[] stringTarget = new String[length];
          int i = 0;
          FeatureStructure cur = aFS.getFeatureValue(aFeature);
          // We assume to by facing a non-empty element if it has a "head" feature
          while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
            stringTarget[i] = cur.getStringValue(cur.getType().getFeatureByBaseName(
                    CAS.FEATURE_BASE_NAME_HEAD));
            cur = cur.getFeatureValue(cur.getType()
                    .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
          }
          target = stringTarget;
          break;
        }
        default: {
          if (aClazz.isArray()) {
            target = Array.newInstance(aClazz.getComponentType(), length);
          } else {
            target = new FeatureStructure[length];
          }
          int i = 0;
          FeatureStructure cur = aFS.getFeatureValue(aFeature);
          // We assume to by facing a non-empty element if it has a "head" feature
          while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
            Array.set(target, i,
                    cur.getFeatureValue(cur.getType().getFeatureByBaseName(
                            CAS.FEATURE_BASE_NAME_HEAD)));
            i++;
            cur = cur.getFeatureValue(cur.getType()
                    .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
          }
          break;
        }
      }
    }    
    else if (aFS.getCAS().getTypeSystem()
              .subsumes(CasUtil.getType(aFS.getCAS(), aClazz), aFeature.getRange())) {
      return (T) aFS.getFeatureValue(aFeature);
    }    
    else {
      throw new IllegalArgumentException("Unable to coerce value of feature [" + aFeature.getName()
              + "] with type [" + aFeature.getRange().getName() + "] into [" + aClazz.getName() + "]");
    }

    // Handle case where return value is an array
    if (aClazz.isArray()) {
      return aClazz.cast(target);
    }
    
    // Handle case where return value is a collection
    if (Collection.class.isAssignableFrom(aClazz)) {
      Collection targetCollection;
      
      if (aClazz.isInterface()) {
        // If the target is an interface, try using a default implementation;
        if (List.class.isAssignableFrom(aClazz)) {
          targetCollection = new ArrayList(length);
        }
        else if (Set.class.isAssignableFrom(aClazz)) {
          targetCollection = new HashSet(length);
        }
        else {
          throw new IllegalArgumentException("Unable to coerce value of feature [" + aFeature.getName()
                  + "] with type [" + aFeature.getRange().getName() + "] into [" + aClazz.getName() + "]");
        }
      }
      else {
        // Try to instantiate using 0-args constructor
        try {
          targetCollection = (Collection) aClazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
          throw new IllegalArgumentException("Unable to coerce value of feature [" + aFeature.getName()
                  + "] with type [" + aFeature.getRange().getName() + "] into [" + aClazz.getName() + "]", e);
        }
      }
      for (int i = 0; i < length; i++) {
        targetCollection.add(Array.get(target, i));
      }
      return aClazz.cast(targetCollection);
    }
    
    throw new IllegalArgumentException("Unable to coerce value of feature [" + aFeature.getName()
            + "] with type [" + aFeature.getRange().getName() + "] into [" + aClazz.getName() + "]");
  }
}
