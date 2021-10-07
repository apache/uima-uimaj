package org.apache.uima.cas.impl;
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



import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;

/**
 * The implementation of an array-valued feature in the type system.
 * Shared by the top defining type and all of its subtypes
 * 
 */
public class FeatureArrayImpl extends FeatureImpl {

  private Object getterArrayMethodRef;
  private Object setterArrayMethodRef;  

  FeatureArrayImpl(TypeImpl typeImpl, String shortName, TypeImpl rangeType, TypeSystemImpl tsi, boolean isMultipleRefsAllowed, SlotKind slotKind) {
    super(typeImpl, shortName, rangeType, tsi, isMultipleRefsAllowed, slotKind);
  }

  Object getGetterArrayMethodRef() {
    return getterArrayMethodRef;
  }

  void setGetterArrayMethodRef(Object getterArrayMethodRef) {
    this.getterArrayMethodRef = getterArrayMethodRef;
  }

  Object getSetterArrayMethodRef() {
    return setterArrayMethodRef;
  }

  void setSetterArrayMethodRef(Object setterArrayMethodRef) {
    this.setterArrayMethodRef = setterArrayMethodRef;
  }
}
