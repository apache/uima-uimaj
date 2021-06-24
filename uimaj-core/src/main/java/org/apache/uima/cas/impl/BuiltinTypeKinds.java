package org.apache.uima.cas.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.EmptyFloatList;
import org.apache.uima.jcas.cas.EmptyIntegerList;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.FloatList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyFloatList;
import org.apache.uima.jcas.cas.NonEmptyIntegerList;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

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

// @formatter:off
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
 * non creatable and builtin Arrays
 */
// @formatter:on
public class BuiltinTypeKinds {

  private static final Set<String> primitiveTypeNames = new HashSet<>();

  public static final Set<String> creatableArrays = new HashSet<>();

  public static final Set<String> nonCreatablePrimitives = primitiveTypeNames;

  /**
   * These types can not be created with CAS.createFS(). Arrays can be created using
   * CAS.create&lt;XYZ&gt;Array XYZ = Boolean, Byte, etc.
   */
  public static final Set<String> nonCreatableTypesAndBuiltinArrays = new HashSet<>();

  public static final Set<String> creatableBuiltinJCasClassNames = new HashSet<>();

  static {
    Misc.addAll(creatableBuiltinJCasClassNames, BooleanArray.class.getName(),
            ByteArray.class.getName(), ShortArray.class.getName(), IntegerArray.class.getName(),
            LongArray.class.getName(), FloatArray.class.getName(), DoubleArray.class.getName(),
            StringArray.class.getName(), FSArray.class.getName(),

            EmptyFloatList.class.getName(), NonEmptyFloatList.class.getName(),
            FloatList.class.getName(), EmptyIntegerList.class.getName(),
            NonEmptyIntegerList.class.getName(), IntegerList.class.getName(),
            EmptyStringList.class.getName(), NonEmptyStringList.class.getName(),
            StringList.class.getName(), EmptyFSList.class.getName(), NonEmptyFSList.class.getName(),
            FSList.class.getName(),

            TOP.class.getName(), AnnotationBase.class.getName(), Annotation.class.getName(),

            Sofa.class.getName(),
            // no default class for next in the classpath
            "org.apache.uima.jcas.tcas.DocumentAnnotation");
  }

  /**
   * These types are - builtin, but could be extended by user - creatable - so they need a
   * generator. -- non-creatable built-in types are not generated
   */
  public static final Set<String> creatableBuiltinJCas = new HashSet<>();

  static {
    Misc.addAll(primitiveTypeNames, CAS.TYPE_NAME_BOOLEAN, CAS.TYPE_NAME_BYTE, CAS.TYPE_NAME_SHORT,
            CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_LONG, CAS.TYPE_NAME_FLOAT, CAS.TYPE_NAME_DOUBLE,
            CAS.TYPE_NAME_STRING
    // CAS.TYPE_NAME_JAVA_OBJECT
    );

    Misc.addAll(creatableArrays, CAS.TYPE_NAME_BOOLEAN_ARRAY, CAS.TYPE_NAME_BYTE_ARRAY,
            CAS.TYPE_NAME_SHORT_ARRAY, CAS.TYPE_NAME_INTEGER_ARRAY, CAS.TYPE_NAME_LONG_ARRAY,
            CAS.TYPE_NAME_FLOAT_ARRAY, CAS.TYPE_NAME_DOUBLE_ARRAY, CAS.TYPE_NAME_STRING_ARRAY,
            CAS.TYPE_NAME_FS_ARRAY
    // CAS.TYPE_NAME_JAVA_OBJECT_ARRAY
    );

    Misc.addAll(creatableBuiltinJCas, CAS.TYPE_NAME_EMPTY_FLOAT_LIST, CAS.TYPE_NAME_EMPTY_FS_LIST,
            CAS.TYPE_NAME_EMPTY_INTEGER_LIST, CAS.TYPE_NAME_EMPTY_STRING_LIST,
            CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST, CAS.TYPE_NAME_NON_EMPTY_FS_LIST,
            CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST, CAS.TYPE_NAME_NON_EMPTY_STRING_LIST,
            CAS.TYPE_NAME_TOP, CAS.TYPE_NAME_ANNOTATION_BASE, CAS.TYPE_NAME_ANNOTATION
    // CAS.TYPE_NAME_DOCUMENT_ANNOTATION // https://issues.apache.org/jira/browse/UIMA-5586
    // these are semi-builtin (for backwards compatibility - not to change users type system codes
    // if not used)
    // CAS.TYPE_NAME_FS_ARRAY_LIST,
    // CAS.TYPE_NAME_INT_ARRAY_LIST,
    // CAS.TYPE_NAME_FS_HASH_SET
    );
    creatableBuiltinJCas.addAll(creatableArrays);

    nonCreatableTypesAndBuiltinArrays.addAll(nonCreatablePrimitives);
    nonCreatableTypesAndBuiltinArrays.addAll(creatableArrays);
    Misc.addAll(nonCreatableTypesAndBuiltinArrays, CAS.TYPE_NAME_SOFA);

  }

  /***************** public getters and predicates *****************/

  /**
   * @param name
   *          -
   * @return -
   */
  public static boolean primitiveTypeNames_contains(String name) {
    return primitiveTypeNames.contains(name);
  }

  /**
   * @param name
   *          -
   * @return -
   */
  public static boolean nonCreatableTypesAndBuiltinArrays_contains(String name) {
    return nonCreatableTypesAndBuiltinArrays.contains(name);
  }

}
