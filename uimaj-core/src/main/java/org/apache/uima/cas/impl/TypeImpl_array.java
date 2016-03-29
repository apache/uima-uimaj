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

import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;

public class TypeImpl_array extends TypeImpl implements TypeSystemConstants {
  
  private final TypeImpl componentType;
  
  private final boolean isHeapStoredArray;
  
  /** Component slot kind */
  private final SlotKind  componentSlotKind;
  
  public TypeImpl_array(String name, TypeImpl componentType, TypeSystemImpl tsi, TypeImpl supertype, 
      SlotKind componentSlotKind, boolean isHeapStoredArray, Class<?> javaClass) {
    super(name, tsi, supertype, javaClass);
    this.isInheritanceFinal = true;
    this.isFeatureFinal = true;
    this.componentType = componentType;
    this.isHeapStoredArray = isHeapStoredArray;
    this.componentSlotKind = componentSlotKind;
  }
  
  @Override
  public TypeImpl getComponentType() {
    return componentType;
  }
  
  @Override
  TypeImpl consolidateType(TypeImpl topType, TypeImpl fsArrayType) {
    if (!(componentType.isPrimitive())) {
        return fsArrayType;  // booleanArrayType, stringArrayType etc.
    }
    // is a primitive array
    return this;
  }
  
  @Override
  public boolean isArray() {
    return true;
  }
  
  @Override
  boolean isHeapStoredArray() {
    return isHeapStoredArray;
  }
  
  @Override
  boolean isAuxStoredArray() {
    return !isHeapStoredArray;
  }
  
  /** Component Slot Kind */
  @Override
  public SlotKind getComponentSlotKind() {
    return componentSlotKind;
  }
  
  @Override
  public boolean subsumes(TypeImpl subType) {
    if (this == subType) {
      return true;
    }
    
    // Need special handling for arrays
    
    if (!subType.isArray()) {
      return false;  // arrays never subsume non-arrays
    }
    
    // Yes, the code below is intentional. Until we actually support real arrays of some particular fs,
    
    // We have FSArray is the supertype of xxxx[] ( xxxx is non-primitive) AND
    //           xxx[] is the supertype of FSArray
    // (this second relation because all we can generate are instances of FSArray
    // and we must be able to assign them to xxx[] )
    
    
    final TypeImpl superType = this;
    final int superTypeCode = getCode();
    
    if (superTypeCode == fsArrayTypeCode) {
      return !subType.isPrimitiveArrayType();
    }
    
    if (subType.getCode() == fsArrayTypeCode) {
      return superTypeCode == arrayBaseTypeCode ||
             !isPrimitiveArrayType();
    }

    // at this point, we could have arrays of other primitive types, or
    // arrays of specific types: xxx[]

    // If both types are arrays, simply compare the components.
    return getComponentType().subsumes(subType.getComponentType());
 
//    } else if (isSubArray) {
//      // If the subtype is an array, and the supertype is not, then the
//      // supertype must be top, or the abstract array base.
//      return superTypeCode == topTypeCode || superTypeCode == arrayBaseTypeCode;
//    }
        
  }
}
