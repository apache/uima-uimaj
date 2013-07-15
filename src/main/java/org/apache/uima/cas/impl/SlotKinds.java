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

/**
 * The kinds of slots that can exist 
 *   an index for getting type-code specific values, 
 *   flag - whether or not they should be diff encoded
 *   flag - if they can be negative (and need their sign moved)
 *   
 * Some are real slots in the heap; others are descriptions of
 *   parts of values, eg. float exponent
 *   
 * Difference encoding costs 1 bit.  
 *   Measurements show it can lessen zip's effectiveness 
 *     (especially for single byte values (?)),
 *     probably because it causes more dispersion in 
 *     the value kinds.  
 *   Because of this 2-fold cost (1 bit and less zip),
 *     differencing being tried only for multi-byte 
 *     values (short, int, long), and heap refs
 *     - for array values, diff is with prev array value
 *       (for 1st value in array, diff is with prev FeatureStructure
 *       of the same type in the heap's 1st value if it exists
 *     - for non-array values or 1st array value, diff is with
 *       prev heap value for same type in heap  
 * 
 *   Not done for float parts - exponent too short, and
 *     mantissa too random.
 * 
 * CanBeNegative
 *   Many values are only positive e.g., array lengths
 *   Some values can be negative
 *     (all difference-encoded things can be negative)
 *   Represent as 1 bit + positive number, sign bit in 
 *     least sig. bit position.  This allows the
 *     bits to cluster closer to 0 on the positive side,
 *     which can make for fewer bytes to represent the number.
 */

/**
 * NOTE: adding or altering slots breaks backward compatability and
 * the ability do deserialize previously serialized things
 * 
 * This definition shared with BinaryCasSerDes4
 * 
 * Define all the slot kinds.
 */
public class SlotKinds {
  public static final boolean CAN_BE_NEGATIVE = true;
  public static final boolean IGNORED = true;
  public static final boolean IN_MAIN_HEAP = true;

  public enum SlotKind {
    Slot_ArrayLength(          ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_HeapRef(                CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_Int(                    CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_Byte(                 ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_Short(                  CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_TypeCode(             ! CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
  
    Slot_StrOffset(            ! CAN_BE_NEGATIVE, 4, !IN_MAIN_HEAP),
    Slot_StrLength(            ! CAN_BE_NEGATIVE, 4, !IN_MAIN_HEAP),
    Slot_Long_High(              CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    Slot_Long_Low (              CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
  
    // the next are not actual slot kinds, but instead
    // are codes used to control encoding of Floats and Doubles.
    Slot_Float_Mantissa_Sign( !  CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    // exponent is 8 bits, and shifted in the expectation
    // that many values may be between 1 and 0 (e.g., normalized values)
    //   -- so sign moving is needed
    Slot_Float_Exponent(      !  CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    
    Slot_Double_Mantissa_Sign(!  CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    Slot_Double_Exponent(     !  CAN_BE_NEGATIVE, 0, !IN_MAIN_HEAP),
    Slot_FsIndexes(              CAN_BE_NEGATIVE, 4, !IN_MAIN_HEAP),
    
    Slot_StrChars(               IGNORED,         2, !IN_MAIN_HEAP),
    
    Slot_Control(                IGNORED,         0, !IN_MAIN_HEAP),
    Slot_StrSeg(              !  CAN_BE_NEGATIVE, 0, ! IN_MAIN_HEAP),
    
    // the next slots are not serialized
    Slot_StrRef(                 CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_BooleanRef(          !  CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_ByteRef(                CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_ShortRef(               CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_LongRef(                CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_DoubleRef(              CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_Float(               !  CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    Slot_Boolean(             !  CAN_BE_NEGATIVE, 4, IN_MAIN_HEAP),
    // next used to capture original heap size
    Slot_MainHeap(               IGNORED,         4, !IN_MAIN_HEAP),
  
    ;
    public final boolean canBeNegative;
    public final boolean inMainHeap;
    public final int elementSize;
    
    public static final int NBR_SLOT_KIND_ZIP_STREAMS;
    static {NBR_SLOT_KIND_ZIP_STREAMS = Slot_StrRef.ordinal();}
    
    SlotKind(boolean canBeNegative, 
             int elementSize,
             boolean inMainHeap) {
      this.canBeNegative =canBeNegative;
      this.elementSize = elementSize; 
      this.inMainHeap = inMainHeap;
    }
  }
}

