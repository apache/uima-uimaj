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

import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_ArrayLength;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Byte;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Control;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Double_Exponent;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Double_Mantissa_Sign;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Float_Exponent;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Float_Mantissa_Sign;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_FsIndexes;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_HeapRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Int;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Long_High;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Long_Low;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Short;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrChars;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrLength;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrOffset;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrSeg;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_TypeCode;

/**
 * Users "implement" this interface to get access to these constants in their code
 */
public interface SlotKindsConstants {
  boolean CAN_BE_NEGATIVE = true;
  boolean IGNORED = true;
  boolean IN_MAIN_HEAP = true;

  int arrayLength_i = Slot_ArrayLength.ordinal();
  int heapRef_i = Slot_HeapRef.ordinal();
  int int_i = Slot_Int.ordinal();
  int byte_i = Slot_Byte.ordinal();
  int short_i = Slot_Short.ordinal();
  int typeCode_i = Slot_TypeCode.ordinal();
  int strOffset_i = Slot_StrOffset.ordinal();
  int strLength_i = Slot_StrLength.ordinal();
  int long_High_i = Slot_Long_High.ordinal();
  int long_Low_i = Slot_Long_Low.ordinal();
  int float_Mantissa_Sign_i = Slot_Float_Mantissa_Sign.ordinal();
  int float_Exponent_i = Slot_Float_Exponent.ordinal();
  int double_Mantissa_Sign_i = Slot_Double_Mantissa_Sign.ordinal();
  int double_Exponent_i = Slot_Double_Exponent.ordinal();
  int fsIndexes_i = Slot_FsIndexes.ordinal();
  int strChars_i = Slot_StrChars.ordinal();
  int control_i = Slot_Control.ordinal();
  int strSeg_i = Slot_StrSeg.ordinal();

  int NBR_SLOT_KIND_ZIP_STREAMS = Slot_StrRef.ordinal();
}
