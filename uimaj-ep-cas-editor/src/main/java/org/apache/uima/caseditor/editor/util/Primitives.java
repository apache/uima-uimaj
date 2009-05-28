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

package org.apache.uima.caseditor.editor.util;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;

/**
 * This enumeration contains all primitives and some util methods.
 * TODO: remove all array and list
 * types, thats are FeatureStructures !!!
 */
public enum Primitives {

  /**
   * The uima {@link Boolean} type.
   */
  BOOLEAN(CAS.TYPE_NAME_BOOLEAN, Boolean.class),

  /**
   * The uima {@link Byte} type.
   */
  BYTE(CAS.TYPE_NAME_BYTE, Byte.class),

  /**
   * The uima {@link Short} type.
   */
  SHORT(CAS.TYPE_NAME_SHORT, Short.class),

  /**
   * The uima {@link Integer} type.
   */
  INTEGER(CAS.TYPE_NAME_INTEGER, Integer.class),

  /**
   * The uima {@link Long} type.
   */
  LONG(CAS.TYPE_NAME_LONG, Long.class),

  /**
   * The uima {@link Float} type.
   */
  FLOAT(CAS.TYPE_NAME_FLOAT, Float.class),

  /**
   * The uima {@link Double} type.
   */
  DOUBLE(CAS.TYPE_NAME_DOUBLE, Double.class),

  /**
   * The uima {@link String} type.
   */
  STRING(CAS.TYPE_NAME_STRING, String.class);

  private final String mTypeName;

  @SuppressWarnings("unchecked")
  private final Class mType;

  @SuppressWarnings("unchecked")
  private Primitives(String typeName, Class type) {
    mTypeName = typeName;
    mType = type;
  }

  /**
   * Retrieves the uima name of the type.
   *
   * @return the uima name.
   */
  public String getTypeName() {
    return mTypeName;
  }

  /**
   * Retrieves the type.
   *
   * @return the type
   */
  @SuppressWarnings("unchecked")
  public Class getType() {
    return mType;
  }

  /**
   * Checks if the given feature has the same type.
   *
   * @param feature
   * @return true if type is the same otherwise false
   */
  public boolean isCompatible(Feature feature) {
    return feature.getRange().getName().equals(getTypeName());
  }

  /**
   * Checks if a given <code>Feature</code> has a primitive type.
   *
   * @param f
   * @return true if primitive otherwise false
   */
  @Deprecated
  public static boolean isPrimitive(Feature f) {
    if (f == null) {
      throw new IllegalArgumentException();
    }

    return isPrimitive(f.getRange().getName());
  }

  /**
   * Checks if the given typeName is a primitive.
   *
   * @param typeName
   * @return true if primitive otherwise false.
   */
  @Deprecated
  public static boolean isPrimitive(String typeName) {
    if (typeName == null) {
      throw new IllegalArgumentException();
    }

    return getPrimitive(typeName) != null ? true : false;
  }

  /**
   * Retrieves the {@link Class} for the current primitive.
   *
   * @param f
   * @return the class
   */
  @SuppressWarnings("unchecked")
  public static Class getPrimitiveClass(Feature f) {
    assert Primitives.isPrimitive(f);

    return getPrimitive(f.getRange().getName()).getType();
  }

  /**
   * Retrieves a primitive by name.
   *
   * @param typeName
   * @return the primitive or null if none
   */
  public static Primitives getPrimitive(String typeName) {
    for (Primitives primitive : values()) {
      if (primitive.getTypeName().equals(typeName)) {
        return primitive;
      }
    }

    return null;
  }

  /**
   * Retrieves the primitive value.
   *
   * @param structure
   * @param feature
   * @return the primitive value as object
   */
  public static Object getPrimitiv(FeatureStructure structure, Feature feature) {
    Object result;

    if (Primitives.BOOLEAN.isCompatible(feature)) {
      result = structure.getBooleanValue(feature);
    } else if (Primitives.BYTE.isCompatible(feature)) {
      result = structure.getByteValue(feature);
    } else if (Primitives.SHORT.isCompatible(feature)) {
      result = structure.getShortValue(feature);
    } else if (Primitives.INTEGER.isCompatible(feature)) {
      result = structure.getIntValue(feature);
    } else if (Primitives.LONG.isCompatible(feature)) {
      result = structure.getLongValue(feature);
    } else if (Primitives.FLOAT.isCompatible(feature)) {
      result = structure.getFloatValue(feature);
    } else if (Primitives.DOUBLE.isCompatible(feature)) {
      result = structure.getDoubleValue(feature);
    } else if (Primitives.STRING.isCompatible(feature)) {
      result = structure.getStringValue(feature);
    } else {
      assert false;

      result = "unexpected type";
    }

    return result;
  }
}