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

public class TypeImpl_array extends TypeImpl {
  
  private final TypeImpl componentType;
  
  private final boolean isHeapStoredArray;
  
  private final SlotKind slotKind;
  
  public TypeImpl_array(String name, TypeImpl componentType, TypeSystemImpl tsi, TypeImpl supertype, 
      SlotKind slotKind, boolean isHeapStoredArray, Class<?> javaClass) {
    super(name, tsi, supertype, javaClass);
    this.isInheritanceFinal = true;
    this.isFeatureFinal = true;
    this.componentType = componentType;
    this.isHeapStoredArray = isHeapStoredArray;
    this.slotKind = slotKind;
  }
  
  public Type getComponentType() {
    return componentType;
  }
  
  TypeImpl consolidateType(TypeImpl topType, TypeImpl fsArrayType) {
    if (!(componentType instanceof TypeImpl_primitive)) {
        return fsArrayType;  // booleanArrayType, stringArrayType etc.
    }
    // is a primitive array
    return this;
  }
  
  @Override
  public boolean isArray() {
    return true;
  }
  
  boolean isHeapStoredArray() {
    return isHeapStoredArray;
  }
  
  SlotKind getSlotKind() {
    return slotKind;
  }
}
