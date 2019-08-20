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

import org.apache.uima.cas.CAS;

/**
 * This interface defines static final constants for Type Systems
 *   For the built-in types and features:
 *     - the type and feature codes
 *     - the adjOffsets
 *
 */
public interface TypeSystemConstants {

  /******************************************
   * built-in type codes
   ******************************************/
  // Code of root of hierarchy (will be 1 with current implementation)
  static final int topTypeCode = 1;
  static final int intTypeCode = 2;
  static final int floatTypeCode = 3;
  static final int stringTypeCode = 4;
  static final int arrayBaseTypeCode = 5;
  static final int fsArrayTypeCode = 6;
  static final int floatArrayTypeCode = 7;
  static final int intArrayTypeCode = 8;
  static final int stringArrayTypeCode = 9;
  // 10 list base
  static final int fsListTypeCode = 11; // 11           fs list
  static final int fsEListTypeCode = 12;// 12 empty     fs list
  static final int fsNeListTypeCode = 13;// 13 non-empty fs list
  static final int floatListTypeCode = 14; // 14           float list
  static final int floatEListTypeCode = 15;// 15 empty     float list
  static final int floatNeListTypeCode = 16;  // 16 non-empty float list
  static final int intListTypeCode = 17; // 17           integer list
  static final int intEListTypeCode = 18;  // 18 empty     integer list
  static final int intNeListTypeCode = 19; // 19 non-empty integer list
  static final int stringListTypeCode = 20;  // 20           string list
  static final int stringEListTypeCode = 21;  // 21 empty     string list
  static final int stringNeListTypeCode = 22;  // 22 non-empty string list

  static final int booleanTypeCode = 23;
  static final int byteTypeCode = 24;
  static final int shortTypeCode = 25;
  static final int longTypeCode = 26;
  static final int doubleTypeCode = 27;
  static final int booleanArrayTypeCode = 28;
  static final int byteArrayTypeCode = 29;
  static final int shortArrayTypeCode = 30;
  static final int longArrayTypeCode = 31;
  static final int doubleArrayTypeCode = 32;
  static final int sofaTypeCode = 33;
  static final int annotBaseTypeCode = 34;
  static final int annotTypeCode = 35;
  static final int docTypeCode = 36;  // DocumentAnnotation

//  static final int lastBuiltinV2TypeCode = 36;

  // new v3 type codes.. Above codes match v2
//  static final int fsArrayListTypeCode = 37;
//  static final int intArrayListTypeCode = 38;
//  static final int fsHashSetTypeCode = 39;

//  static final int numberOfNewBuiltInsSinceV2 = 3;

//  static final int javaObjectTypeCode = 37;
//  static final int javaObjectArrayTypeCode = 38;

  /**
   * Static final constants for built-in features
   */
  static final int sofaNumFeatCode = 9;  // ref from another pkg
  static final int sofaIdFeatCode = 10;
  static final int sofaStringFeatCode = 13;
  static final int sofaMimeFeatCode = 11;
  static final int sofaUriFeatCode = 14;
  static final int sofaArrayFeatCode = 12;
  static final int annotBaseSofaFeatCode = 15; // ref from another pkg
  static final int beginFeatCode = 16;
  static final int endFeatCode = 17;
  static final int langFeatCode = 18;

  /**
   * adjOffsets for builtin Features
   */
  static final int sofaNumFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType.getAdjOffset(CAS.FEATURE_BASE_NAME_SOFANUM);  
  static final int sofaIdFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType.getAdjOffset(CAS.FEATURE_BASE_NAME_SOFAID);
  static final int sofaStringFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType.getAdjOffset(CAS.FEATURE_BASE_NAME_SOFASTRING);
  static final int sofaMimeFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType.getAdjOffset(CAS.FEATURE_BASE_NAME_SOFAMIME);
  static final int sofaUriFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType.getAdjOffset(CAS.FEATURE_BASE_NAME_SOFAURI);
  static final int sofaArrayFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType.getAdjOffset(CAS.FEATURE_BASE_NAME_SOFAARRAY);
  static final int annotBaseSofaFeatAdjOffset = TypeSystemImpl.staticTsi.annotBaseType.getAdjOffset(CAS.FEATURE_BASE_NAME_SOFA);
  static final int beginFeatAdjOffset = TypeSystemImpl.staticTsi.annotType.getAdjOffset(CAS.FEATURE_BASE_NAME_BEGIN);
  static final int endFeatAdjOffset = TypeSystemImpl.staticTsi.annotType.getAdjOffset(CAS.FEATURE_BASE_NAME_END);
  static final int langFeatAdjOffset = TypeSystemImpl.staticTsi.docType.getAdjOffset(CAS.FEATURE_BASE_NAME_LANGUAGE);
}
