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

//@formatter:off
/**
 * This interface defines static final constants for Type Systems
 *   For the built-in types and features:
 *     - the type and feature codes
 *     - the adjOffsets
 */
//@formatter:on
public interface TypeSystemConstants {

  /******************************************
   * built-in type codes
   ******************************************/
  // Code of root of hierarchy (will be 1 with current implementation)
  int topTypeCode = 1;
  int intTypeCode = 2;
  int floatTypeCode = 3;
  int stringTypeCode = 4;
  int arrayBaseTypeCode = 5;
  int fsArrayTypeCode = 6;
  int floatArrayTypeCode = 7;
  int intArrayTypeCode = 8;
  int stringArrayTypeCode = 9;
  // 10 list base
  int fsListTypeCode = 11; // 11 fs list
  int fsEListTypeCode = 12;// 12 empty fs list
  int fsNeListTypeCode = 13;// 13 non-empty fs list
  int floatListTypeCode = 14; // 14 float list
  int floatEListTypeCode = 15;// 15 empty float list
  int floatNeListTypeCode = 16; // 16 non-empty float list
  int intListTypeCode = 17; // 17 integer list
  int intEListTypeCode = 18; // 18 empty integer list
  int intNeListTypeCode = 19; // 19 non-empty integer list
  int stringListTypeCode = 20; // 20 string list
  int stringEListTypeCode = 21; // 21 empty string list
  int stringNeListTypeCode = 22; // 22 non-empty string list

  int booleanTypeCode = 23;
  int byteTypeCode = 24;
  int shortTypeCode = 25;
  int longTypeCode = 26;
  int doubleTypeCode = 27;
  int booleanArrayTypeCode = 28;
  int byteArrayTypeCode = 29;
  int shortArrayTypeCode = 30;
  int longArrayTypeCode = 31;
  int doubleArrayTypeCode = 32;
  int sofaTypeCode = 33;
  int annotBaseTypeCode = 34;
  int annotTypeCode = 35;
  int docTypeCode = 36; // DocumentAnnotation

  // static final int lastBuiltinV2TypeCode = 36;

  // new v3 type codes.. Above codes match v2
  // static final int fsArrayListTypeCode = 37;
  // static final int intArrayListTypeCode = 38;
  // static final int fsHashSetTypeCode = 39;

  // static final int numberOfNewBuiltInsSinceV2 = 3;

  // static final int javaObjectTypeCode = 37;
  // static final int javaObjectArrayTypeCode = 38;

  /**
   * Static final constants for built-in features
   */
  int sofaNumFeatCode = 9; // ref from another pkg
  int sofaIdFeatCode = 10;
  int sofaStringFeatCode = 13;
  int sofaMimeFeatCode = 11;
  int sofaUriFeatCode = 14;
  int sofaArrayFeatCode = 12;
  int annotBaseSofaFeatCode = 15; // ref from another pkg
  int beginFeatCode = 16;
  int endFeatCode = 17;
  int langFeatCode = 18;

  /**
   * adjOffsets for builtin Features
   */
  int sofaNumFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_SOFANUM);
  int sofaIdFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_SOFAID);
  int sofaStringFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_SOFASTRING);
  int sofaMimeFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_SOFAMIME);
  int sofaUriFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_SOFAURI);
  int sofaArrayFeatAdjOffset = TypeSystemImpl.staticTsi.sofaType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_SOFAARRAY);
  int annotBaseSofaFeatAdjOffset = TypeSystemImpl.staticTsi.annotBaseType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_SOFA);
  int beginFeatAdjOffset = TypeSystemImpl.staticTsi.annotType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_BEGIN);
  int endFeatAdjOffset = TypeSystemImpl.staticTsi.annotType.getAdjOffset(CAS.FEATURE_BASE_NAME_END);
  int langFeatAdjOffset = TypeSystemImpl.staticTsi.docType
          .getAdjOffset(CAS.FEATURE_BASE_NAME_LANGUAGE);
}
