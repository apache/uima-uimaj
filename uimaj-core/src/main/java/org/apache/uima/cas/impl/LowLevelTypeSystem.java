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

package org.apache.uima.cas.impl;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

/**
 * Low-level version of the type system APIs. Use in conjunction with the
 * {@link org.apache.uima.cas.impl.LowLevelCAS LowLevelCAS} APIs.
 * 
 * <p>
 * Use
 * {@link org.apache.uima.cas.impl.LowLevelCAS#ll_getTypeSystem() LowLevelCAS.ll_getTypeSystem()} to
 * access a low-level type system.
 * 
 */
public interface LowLevelTypeSystem {

  /**
   * Type code that is returned on unknown type names.
   */
  static final int UNKNOWN_TYPE_CODE = 0;

  /**
   * Feature code that is returned on unknown feature names.
   */
  static final int UNKNOWN_FEATURE_CODE = 0;

  /**
   * Get the type code for a given type name.
   * 
   * @param typeName
   *          The name of the type.
   * @return The code for the type. A return value of <code>0</code> means that the a type of that
   *         name does not exist in the type system.
   */
  int ll_getCodeForTypeName(String typeName);

  /**
   * Get the code of an existing type object.
   * 
   * @param type
   *          A type object.
   * @return The type code for the input type.
   */
  int ll_getCodeForType(Type type);

  /**
   * Get the feature code for a given feature name.
   * 
   * @param featureName
   *          The name of the feature.
   * @return The code for the feature. A return value of <code>0</code> means that the name does
   *         not represent a feature in the type system.
   */
  int ll_getCodeForFeatureName(String featureName);

  /**
   * Get the code for a given feature object.
   * 
   * @param feature
   *          A feature object.
   * @return The code for the feature.
   */
  int ll_getCodeForFeature(Feature feature);

  /**
   * Get a type object for a given code.
   * 
   * @param typeCode
   *          The code of the type.
   * @return A type object, or <code>null</code> if <code>typeCode</code> is not a valid type
   *         code.
   */
  Type ll_getTypeForCode(int typeCode);

  /**
   * Get a feature object for a given code.
   * 
   * @param featureCode
   *          The code of the feature.
   * @return A feature object, or <code>null</code> if <code>featureCode</code> is not a valid
   *         feature code.
   */
  Feature ll_getFeatureForCode(int featureCode);

  /**
   * Get an array of the feature codes for the features on this type.
   * 
   * @param typeCode
   *          Input type code.
   * @return The array of appropriate features for <code>typeCode</code>.
   */
  int[] ll_getAppropriateFeatures(int typeCode);

  /**
   * Get the domain type for a given feature.
   * 
   * @param featureCode
   *          Input feature code.
   * @return The domain type code for <code>featureCode</code>.
   */
  int ll_getDomainType(int featureCode);

  /**
   * Get the range type for a given feature.
   * 
   * @param featureCode
   *          Input feature code.
   * @return The range type code for <code>featureCode</code>.
   */
  int ll_getRangeType(int featureCode);

  /**
   * Check subsumption between two types.
   * 
   * @param type1 -
   * @param type2 -
   * @return <code>true</code> iff <code>type1</code> subsumes <code>type2</code>.
   */
  boolean ll_subsumes(int type1, int type2);

  /**
   * Determine the type class of a type. This is useful for generic CAS exploiters to determine what
   * kind of data they're looking at. The type classes currently defined are:
   * <ul>
   * <li><code>TYPE_CLASS_INVALID</code> -- Not a valid type code.</li>
   * <li><code>TYPE_CLASS_INT</code> -- Integer type. </li>
   * <li><code>TYPE_CLASS_FLOAT</code> -- Float type.</li>
   * <li><code>TYPE_CLASS_STRING</code> -- String type.</li>
   * <li><code>TYPE_CLASS_INTARRAY</code> -- Integer array.</li>
   * <li><code>TYPE_CLASS_FLOATARRAY</code> -- Float array.</li>
   * <li><code>TYPE_CLASS_STRINGARRAY</code> -- String array.</li>
   * <li><code>TYPE_CLASS_FSARRAY</code> -- FS array.</li>
   * <li><code>TYPE_CLASS_FS</code> -- FS type, i.e., all other types, including all user-defined
   * types.</li>
   * </ul>
   * This method is on the CAS, not the type system, since the specific properties of types are
   * specific to the CAS. The type system does not know, for example, that the CAS treats arrays
   * specially.
   * 
   * @param typeCode
   *          The type code.
   * @return A type class for the type code. <code>TYPE_CLASS_INVALID</code> if the type code
   *         argument does not represent a valid type code.
   */
  int ll_getTypeClass(int typeCode);

  /**
   * Check if type is a string subtype.
   * 
   * @param type
   *          The type to be checked.
   * @return <code>true</code> iff <code>type</code> is a subtype of String.
   */
  boolean ll_isStringSubtype(int type);

  /**
   * Checks if the type code is that of a reference type (anything that's not a basic type,
   * currently Integer, String, Float, Boolean, Byte, Short, Long, Double, 
   * and subtypes of String - specifying allowed-values).
   * 
   * @param typeCode
   *          The type code to check.
   * @return <code>true</code> iff <code>typeCode</code> is the type code of a reference type.
   */
  boolean ll_isRefType(int typeCode);

  /**
   * Check if <code>typeCode</code> is the type code of an array type.
   * 
   * @param typeCode
   *          The type code to check.
   * @return <code>true</code> iff <code>typeCode</code> is an array type code.
   */
  boolean ll_isArrayType(int typeCode);

  /**
   * Check if <code>typeCode</code> is the type code of a primitive type.
   * 
   * @param typeCode
   *          The type code to check.
   * @return <code>true</code> iff <code>typeCode</code> is a primitive type code.
   */
  boolean ll_isPrimitiveType(int typeCode);

  /**
   * Get the type code for the array type with <code>componentTypeCode</code> as component type
   * code.
   * 
   * @param componentTypeCode
   *          The type code of the component type.
   * @return The type code for the requested array type, or
   *         {@link #UNKNOWN_TYPE_CODE UNKNOWN_TYPE_CODE} if <code>componentTypeCode</code> is not
   *         a valid type code.
   */
  int ll_getArrayType(int componentTypeCode);

  /**
   * Check the input type code.
   * 
   * @param typeCode
   *          Type code to check.
   * @return <code>true</code> iff <code>typeCode</code> is a valid type code.
   */
  boolean ll_isValidTypeCode(int typeCode);

  /**
   * Get the component type of an array type code.
   * 
   * @param arrayTypeCode
   *          The input array type code.
   * @return The type code for the component type, or {@link #UNKNOWN_TYPE_CODE UNKNOWN_TYPE_CODE}
   *         if <code>arrayTypeCode</code> is not valid or not an array type.
   */
  int ll_getComponentType(int arrayTypeCode);

  /**
   * Get the parent type for the input type.
   * 
   * @param typeCode
   *          The type code we want the parent for.
   * @return The type code of the parent type.
   */
  int ll_getParentType(int typeCode);
  
  /**
   * Get the string set (sorted) for a string subtype.
   * @param typeCode Input type code; should be a string subtype.
   * @return The set of allowable string values for subtypes of uima.cas.String.  If the input type
   * code is not a proper subtype of String, returns <code>null</code>. 
   */
  String[] ll_getStringSet(int typeCode);
}
