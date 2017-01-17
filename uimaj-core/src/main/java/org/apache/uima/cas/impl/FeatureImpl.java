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

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;

/**
 * The implementation of features in the type system.
 * A featureImpl instance is shared by the top defining type and all of its subtypes
 * 
 */
public class FeatureImpl implements Feature {
  
  private final int featureCode;        // unique id for this feature, in this type system
  /**
   * the 0 based offset for this feature ignoring ref/int distinction, in feature order, without regard to JCas implemented features; set at commit time
   * used by v2 style de/serializers
   */
  private       short featureOffset = -1;  
  private       short adjustedFeatureOffset = -1; // the offset in the storage array for this feature, adjusted to exclude JCas implemented features; set at commit time
  
                // not used 2/29/16 to be removed
//                int registryIndex = -1; // set from JCas classes feature registry
                                        // used to setup index corruption bitset                
  public final boolean isInInt;        // specifies which array the data is in
  private final boolean isMultipleRefsAllowed;
  /**
   * true if the range is a long or double
   */
  public final boolean isLongOrDouble; 
  private TypeImpl highestDefiningType;  // not final, could change
  
  private final TypeImpl rangeType;
  
//  private final TypeSystemImpl ts;

  /**
   * true for the feature which is the AnnotationBase sofa reference.
   */
  public final boolean isAnnotBaseSofaRef;
  private final String shortName;     //         feat
    
//  protected Object jcasGetter;  // null or the functional interface to call to get this feature
//  protected Object jcasSetter;  // null or the functional interface to call to set this feature
//  protected Object jcasSetterNcNj; // null or the functional interface to call to set this feature, no check for corruption, no journaling
    
  private final SlotKind slotKind;
  /** type class of the range, including CasSerializer List constants */
  public  final int rangeTypeClass; // set from CasSerializerSupport.classifyType

  private FeatureImpl() {
    featureCode = 0;
    isInInt = false;
    rangeType = null;
    isMultipleRefsAllowed = false;
    isAnnotBaseSofaRef = false;
    shortName = null;
    slotKind = null;
    rangeTypeClass = 0;
    isLongOrDouble = false;
  }

  FeatureImpl(TypeImpl typeImpl, String shortName, TypeImpl rangeType, TypeSystemImpl tsi, boolean isMultipleRefsAllowed, SlotKind slotKind) {
//  this.code = code;
  this.highestDefiningType = typeImpl;  
  List<FeatureImpl> feats = tsi.features;
  featureCode = feats.size();
  
  this.rangeType = rangeType;
  this.isLongOrDouble = rangeType.isLongOrDouble;
  this.slotKind = slotKind;
  this.shortName = shortName;
  this.isMultipleRefsAllowed = isMultipleRefsAllowed;
  this.isAnnotBaseSofaRef = (highestDefiningType.getCode() == TypeSystemConstants.annotBaseTypeCode) && shortName.equals(CAS.FEATURE_BASE_NAME_SOFA);
  this.isInInt = tsi.isInInt(rangeType);
  this.rangeTypeClass = CasSerializerSupport.classifyType(rangeType);
  typeImpl.addFeature(this);  // might throw if existing feature with different range
  feats.add(this);
}

  /**
   * @return the internal code of this feature. Necessary when using low-level APIs.
   */
  public int getCode() {
    return this.featureCode;
  }

  /**
   * Get the domain type for this feature.
   * 
   * @return The domain type. This can not be <code>null</code>.
   */
  public Type getDomain() {
    return this.highestDefiningType;
  }
 
  /**
   * Get the range type for this feature.
   *   * @return The range type. This can not be <code>null</code>.
   */
  public Type getRange() {
    return this.rangeType;
  }
  
  public TypeImpl getRangeImpl() {
    return this.rangeType;
  }
  
  public SlotKind getSlotKind() {
    return this.slotKind;
  }

  /**
   * Get the fully qualified name for this feature.
   * The Feature qualifier is that of the highest defining type.
   * 
   * @return The name. This can not be <code>null</code>.
   */
  public String getName() {
    return highestDefiningType.getName() + TypeSystem.FEATURE_SEPARATOR + shortName;
  }

  public String getShortName() {
    return this.shortName;
  }

  @Override
  public String toString() {
    return String.format(
        "%s [%s: rangeType=%s, isMultipleRefsAllowed=%s, slotKind=%s]",
        this.getClass().getSimpleName(), getName(), rangeType, isMultipleRefsAllowed,
        slotKind);
  }

//  public String getGetterSetterName(boolean isGet) {
//    String shortName1stLetterUpperCase = Character.toUpperCase(this.shortName.charAt(0)) + 
//        ((shortName.length() == 1) ? "" : this.shortName.substring(1));
//
//    return (isGet ? "get" : "set")  + shortName1stLetterUpperCase;
//  }
  
  public boolean isMultipleReferencesAllowed() {
    return this.isMultipleRefsAllowed;
  }
  
  /**
   * @return the 0-based offset for this feature
   */
  public int getOffset() {
    return featureOffset;
  }
  
  void setOffset(int offset) {
    if (offset > Short.MAX_VALUE) {
      throw new RuntimeException("Feature Offset exceeds maximum of 32767");
    }
    featureOffset = (short) offset;
  }
  
  public int getAdjustedOffset() {
    return adjustedFeatureOffset;
  }
  
  void setAdjustedOffset(int offset) {    
    if (offset > Short.MAX_VALUE) {
      throw new RuntimeException("Feature Offset exceeds maximum of 32767");
    }
    adjustedFeatureOffset = (short) offset;
  }

//  /**
//   * @return the functionalGetter
//   */
//  Object getJCasGetter() {
//    return jcasGetter;
//  }
//
//  /**
//   * @param functionalGetter the functionalGetter to set
//   */
//  void setJCasGetter(Object functionalGetter) {
//    this.jcasGetter = functionalGetter;
//  }
//
//  /**
//   * @return the functionalSetter
//   */
//  Object getJCasSetter() {
//    return jcasSetter;
//  }
//  
//  /**
//   * @return the functional setter with no index corruption checking, no journalling
//   */
//  Object getJCasSetterNcNj() {
//    return jcasSetterNcNj;
//  }
  
  /* Set for these values done directly in TypeSystemImpl, computeAdjustedFeatureOffsets */
  
  TypeImpl getHighestDefiningType() {
    return highestDefiningType;
  }
  
  void setHighestDefiningType(Type type) {
    highestDefiningType = (TypeImpl) type;
  }

  /**
   * throw if v is not in the allowed value set of the range type
   * @param v the value to check
   */
  public void validateIsInAllowedValue(String v) {
    TypeImpl_stringSubtype ti = (TypeImpl_stringSubtype) getRangeImpl();
    ti.validateIsInAllowedValues(v);
  }
  
  /**
   * Used by CAS Copier to denote missing feature
   */
  public final static FeatureImpl singleton = new FeatureImpl();

  /**
   * Hashcode and equals are used, possibly for features in different type systems, 
   * where the features should be "equal".  Example: fitering during serialization.
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
//    return this.featureCode;  // can't use this across different type systems
    result = prime * result + ((highestDefiningType == null) ? 0 : highestDefiningType.getName().hashCode());
    result = prime * result + (isMultipleRefsAllowed ? 1231 : 1237);
    result = prime * result + ((rangeType == null) ? 0 : rangeType.getName().hashCode());
    result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
    return result;
  }

  /**
   * This should work across different type systems, for instance 
   * when using filtered serialization
   */
  @Override
  public int compareTo(Feature o) {
    if (this == o) {
      return 0;
    }    
    FeatureImpl other = (FeatureImpl) o;

    int c;
    c = this.shortName.compareTo(other.shortName);
    if (c != 0) return c;
    c = highestDefiningType.getName().compareTo(other.highestDefiningType.getName());
    if (c != 0) return c;
    c = Boolean.compare(this.isMultipleRefsAllowed, other.isMultipleRefsAllowed);
    if (c != 0) return c;
    c = rangeType.getName().compareTo(other.rangeType.getName());
    if (c != 0) return c;

    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof FeatureImpl)) return false;
    
    FeatureImpl other = (FeatureImpl) obj;
//    return this.featureCode == other.featureCode;  // can't use this across different type systems
    if (!highestDefiningType.getName().equals(other.highestDefiningType.getName())) return false;
    if (isMultipleRefsAllowed != other.isMultipleRefsAllowed) return false;
    if (!rangeType.getName().equals(other.rangeType.getName())) return false;
    if (!shortName.equals(other.shortName)) return false;
    return true;
  }
  
  
}
