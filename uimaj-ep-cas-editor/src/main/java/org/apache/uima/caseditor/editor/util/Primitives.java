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
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelTypeSystem;


public class Primitives {

  /**
   * Retrieve the primitive java class for a primitive type.
   * 
   * @param ts
   * @param type
   * 
   * @return the primitive java class
   */
  public static Class<?> getPrimitiveClass(TypeSystem ts, Type type) {
    if (!type.isPrimitive())
      throw new IllegalArgumentException("Type " + type.getName() + " is not primitive!");
    
    // Note:
    // In a UIMA type system *only* the primitive string type can be 
    // sub-typed.
    
    if (ts.getType(CAS.TYPE_NAME_BOOLEAN).equals(type)) {
      return Boolean.class;
    }
    else if (ts.getType(CAS.TYPE_NAME_BYTE).equals(type)) {
      return Byte.class;
    }
    else if (ts.getType(CAS.TYPE_NAME_SHORT).equals(type)) {
      return Short.class;
    }
    else if (ts.getType(CAS.TYPE_NAME_INTEGER).equals(type)) {
      return Integer.class;
    }
    else if (ts.getType(CAS.TYPE_NAME_LONG).equals(type)) {
      return Long.class;
    }
    else if (ts.getType(CAS.TYPE_NAME_FLOAT).equals(type)) {
      return Float.class;
    }
    else if (ts.getType(CAS.TYPE_NAME_DOUBLE).equals(type)) {
      return Double.class;
    }
    else if (ts.getType(CAS.TYPE_NAME_STRING).equals(type) || 
            ts.subsumes(ts.getType(CAS.TYPE_NAME_STRING), type)) {
      return String.class;
    }
    else {
      throw new IllegalStateException("Unexpected primitive type: " + type.getName());
    }
  }
  
  /**
   * Retrieves the {@link Class} for the current primitive.
   *
   * @param f
   * @return the class
   */
  public static Class<?> getPrimitiveClass(TypeSystem ts, Feature f) {
    return getPrimitiveClass(ts, f.getRange());
  }

  /**
   * Retrieves the primitive value.
   *
   * @param structure
   * @param feature
   * @return the primitive value as object
   */
  public static Object getPrimitive(FeatureStructure structure, Feature feature) {
    
    TypeSystem ts = structure.getCAS().getTypeSystem();
    
    Class<?> primitiveClass = getPrimitiveClass(ts, feature);
    
    Object result;

    if (Boolean.class.equals(primitiveClass)) {
      result = structure.getBooleanValue(feature);
    } else if (Byte.class.equals(primitiveClass)) {
      result = structure.getByteValue(feature);
    } else if (Short.class.equals(primitiveClass)) {
      result = structure.getShortValue(feature);
    } else if (Integer.class.equals(primitiveClass)) {
      result = structure.getIntValue(feature);
    } else if (Long.class.equals(primitiveClass)) {
      result = structure.getLongValue(feature);
    } else if (Float.class.equals(primitiveClass)) {
      result = structure.getFloatValue(feature);
    } else if (Double.class.equals(primitiveClass)) {
      result = structure.getDoubleValue(feature);
    } else if (String.class.equals(primitiveClass)) {
      result = structure.getStringValue(feature);
      
      if (result == null)
        result = "";
    } else {
      throw new IllegalStateException("Unexpected type: " 
          + feature.getRange().getName());
    }

    return result;
  }
  
  public static boolean isRestrictedByAllowedValues(TypeSystem ts, Type type) {
    
    if (ts.getType(CAS.TYPE_NAME_STRING).equals(type) || 
            ts.subsumes(ts.getType(CAS.TYPE_NAME_STRING), type)) {
      LowLevelTypeSystem lts = ts.getLowLevelTypeSystem();
      final int typeCode = lts.ll_getCodeForType(type);
      String[] strings = lts.ll_getStringSet(typeCode);
      
      return strings.length > 0;
    }
    else {
      return false;
    }
    
  }
  
  public static String[] getRestrictedValues(TypeSystem ts, Type type) {
    if (isRestrictedByAllowedValues(ts, type)) {
      throw new IllegalArgumentException("Type " + type.getName() + " does not defines allowed values!");
    }
    
    LowLevelTypeSystem lts = ts.getLowLevelTypeSystem();
    final int typeCode = lts.ll_getCodeForType(type);
    
    return lts.ll_getStringSet(typeCode);
  }
  
}