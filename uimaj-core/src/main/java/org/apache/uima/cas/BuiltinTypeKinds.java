package org.apache.uima.cas;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.internal.util.Misc;

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

/**
 * Constants representing Built in type collections
 *
 * String Sets: 
 * 
 *     creatableArrays
 *     primitiveTypeNames == noncreatable primitives
 *     creatableBuiltinJcas (e.g. empty/non-empty FloatList
 *     non creatable primitives (e.g. can't do createFS for primitive int)
 *     
 *     non creatable and builtin Arrays
 */
public class BuiltinTypeKinds {
  
  private static final Set<String> primitiveTypeNames = new HashSet<String>();

  public static final Set<String> creatableArrays = new HashSet<String>();
   
  public static final Set<String> nonCreatablePrimitives = primitiveTypeNames;

  /**
   *  These types can not be created with CAS.createFS().
   *    Arrays can be created using CAS.create&lt;XYZ&gt;Array  XYZ = Boolean, Byte, etc.
   */
  public static final Set<String> nonCreatableTypesAndBuiltinArrays = new HashSet<String>();
  
  /**
   * These types are
   *   - builtin, but could be extended by user 
   *   - creatable - so they need a generator.
   *     -- non-creatable built-in types are not generated 
   */
  public static final Set<String> creatableBuiltinJCas = new HashSet<String>();

  static {
    Misc.addAll(primitiveTypeNames, 
        CAS.TYPE_NAME_BOOLEAN,
        CAS.TYPE_NAME_BYTE,
        CAS.TYPE_NAME_SHORT,
        CAS.TYPE_NAME_INTEGER,
        CAS.TYPE_NAME_LONG,
        CAS.TYPE_NAME_FLOAT,
        CAS.TYPE_NAME_DOUBLE,
        CAS.TYPE_NAME_STRING
//        CAS.TYPE_NAME_JAVA_OBJECT
        );
    
    Misc.addAll(creatableArrays,  
        CAS.TYPE_NAME_BOOLEAN_ARRAY,
        CAS.TYPE_NAME_BYTE_ARRAY,
        CAS.TYPE_NAME_SHORT_ARRAY,
        CAS.TYPE_NAME_INTEGER_ARRAY,
        CAS.TYPE_NAME_LONG_ARRAY,
        CAS.TYPE_NAME_FLOAT_ARRAY,
        CAS.TYPE_NAME_DOUBLE_ARRAY,
        CAS.TYPE_NAME_STRING_ARRAY,
        CAS.TYPE_NAME_FS_ARRAY
//        CAS.TYPE_NAME_JAVA_OBJECT_ARRAY
        );
        
    Misc.addAll(creatableBuiltinJCas, 
        CAS.TYPE_NAME_EMPTY_FLOAT_LIST,
        CAS.TYPE_NAME_EMPTY_FS_LIST,
        CAS.TYPE_NAME_EMPTY_INTEGER_LIST,
        CAS.TYPE_NAME_EMPTY_STRING_LIST,
        CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST,
        CAS.TYPE_NAME_NON_EMPTY_FS_LIST,
        CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST,
        CAS.TYPE_NAME_NON_EMPTY_STRING_LIST,
        CAS.TYPE_NAME_TOP,
        CAS.TYPE_NAME_ANNOTATION_BASE,
        CAS.TYPE_NAME_ANNOTATION
//        CAS.TYPE_NAME_FS_ARRAY_LIST,
//        CAS.TYPE_NAME_INT_ARRAY_LIST,
//        CAS.TYPE_NAME_FS_HASH_SET
    );    
    creatableBuiltinJCas.addAll(creatableArrays);
 
    nonCreatableTypesAndBuiltinArrays.addAll(nonCreatablePrimitives);
    nonCreatableTypesAndBuiltinArrays.addAll(creatableArrays);
    Misc.addAll(nonCreatableTypesAndBuiltinArrays, CAS.TYPE_NAME_SOFA);
    
       
  }
  
  /*****************  public getters and predicates *****************/
    
  /**
   * @param name -
   * @return -
   */
  public static boolean primitiveTypeNames_contains(String name) {
    return primitiveTypeNames.contains(name);
  }
  
  /**
   * @param name -
   * @return -
   */
  public static boolean nonCreatableTypesAndBuiltinArrays_contains(String name) {
    return nonCreatableTypesAndBuiltinArrays.contains(name);
  }
  
}
