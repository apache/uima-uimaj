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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.internal.util.Misc;

// @formatter:off
/**
 * This class gets initialized with two type systems, and then provides resources to map type and
 * feature codes between them.
 * 
 * It is used by some Binary serialization/ deserialization code to allow non-exact matched type
 * systems to send and receive CASes in a binary-like format.
 * 
 * Use cases:
 * 
 * Serializing:  Source ts -%gt; generate serialized form in Target ts 
 * Deserializing: Target ts -%gt; generate deserialized form in Source ts
 *   - either from remote or
 *   - from disk-stored-form
 * 
 * Mapping details:
 *   Types are mapped by name. 
 *     Same-named types do not need to have the same number of features.
 *     Same-named features must have same Range - otherwise, not mapped.
 *     Types with 0 features mapped allowed.
 * LifeCycle:
 *   Instance of this are created for a CAS when needed, and then
 *   kept in the (source) TypeSystemImpl, in a map indexed by
 *   the target type system (identity map)
 * 
 */
// @formatter:on
public class CasTypeSystemMapper {

  public final TypeSystemImpl tsSrc; // source type system
  // weak ref to target type system, to allow that object to be gc'd
  // which in turn allows a weak map using these as keys to reclaim space
  public final WeakReference<TypeSystemImpl> tsTgt;

  /**
   * Map from source types to target types. Source type code used as index, value is target type or
   * null if the type doesn't exist in the target
   */
  private final List<TypeImpl> tSrc2Tgt = new ArrayList<>();

  /**
   * Map from target types to source types. Source type code used as index, value is target type or
   * null if the type doesn't exist in the target
   */
  private final List<TypeImpl> tTgt2Src = new ArrayList<>();
  /**
   * Feature mapping from source to target first key is the src type code, 2nd is the src feature
   * offset (origin 0)
   */
  private final FeatureImpl[][] fSrc2Tgt;

  // @formatter:off
  /**
   * Feature mapping from target to source 
   *   first key is the tgt type code, 2nd is the tgt feature offset 
   * Only used for type codes that are not arrays.
   * Use: When serializing a source type that exists in the target, have to output
   *   the slots in the target feature order
   *   Also, when comparing the slots in the target with a given source
   */
  // @formatter:on
  private final FeatureImpl[][] fTgt2Src;

  private final boolean typeSystemsSame;

  public boolean isEqual() {
    return typeSystemsSame;
  }

  public CasTypeSystemMapper(TypeSystemImpl tsSrc, TypeSystemImpl tsTgt) {
    if (!tsSrc.isCommitted() || !tsTgt.isCommitted()) {
      /** Type Systems must be committed before calling this method */
      throw new CASRuntimeException(CASRuntimeException.TYPESYSTEMS_NOT_COMMITTED);
    }
    this.tsSrc = tsSrc;
    this.tsTgt = new WeakReference<>(tsTgt);
    boolean tss = true;

    if (tsSrc != tsTgt) {

      fSrc2Tgt = new FeatureImpl[tsSrc.getTypeArraySize()][];
      fTgt2Src = new FeatureImpl[tsTgt.getTypeArraySize()][];

      boolean b1 = addTypes(tSrc2Tgt, tsSrc, tsTgt);
      boolean b2 = addTypes(tTgt2Src, tsTgt, tsSrc); // both directions
      boolean b3 = addFeatures(fSrc2Tgt, tsSrc, tsTgt);
      boolean b4 = addFeatures(fTgt2Src, tsTgt, tsSrc);

      if (!b1 || !b2 || !b3 || !b4) {
        tss = false;
      }
    } else {
      fSrc2Tgt = null;
      fTgt2Src = null;
    }
    typeSystemsSame = tss;
  }

  /**
   * @param srcType
   *          -
   * @return Type in other type system, or this one if map is empty
   */
  public TypeImpl mapTypeSrc2Tgt(TypeImpl srcType) {
    return (tSrc2Tgt.size() == 0) ? srcType : tSrc2Tgt.get(srcType.getCode());
  }

  // public TypeImpl mapTypeSrc2Tgt(int srcTypeCode) {
  // return tSrc2Tgt.get(srcTypeCode);
  // }

  /**
   * @param tgtType
   *          -
   * @return 0 if type doesn't have corresponding code in other type system
   */
  public TypeImpl mapTypeTgt2Src(TypeImpl tgtType) {
    return (tTgt2Src.size() == 0) ? tgtType : tTgt2Src.get(tgtType.getCode());
  }

  public TypeImpl mapTypeCodeTgt2Src(int tgtTypeCode) {
    return (tTgt2Src.size() == 0) ? tsSrc.getTypeForCode(tgtTypeCode) : tTgt2Src.get(tgtTypeCode);
  }

  /**
   * 
   * @param type
   *          -
   * @param src2tgt
   *          -
   * @return 0 if type doesn't have corresponding code in other type system
   */
  public TypeImpl mapTypeCode2Other(TypeImpl type, boolean src2tgt) {
    return (src2tgt) ? mapTypeSrc2Tgt(type) : mapTypeTgt2Src(type);
  }

  /**
   * Get target feature, given src type and feature
   * 
   * @param srcType
   *          the source type
   * @param srcFeat
   *          the source feature
   * @return the target feature or null
   */
  public FeatureImpl getTgtFeature(TypeImpl srcType, FeatureImpl srcFeat) {
    return getToFeature(fSrc2Tgt, srcType, srcFeat);
  }

  public FeatureImpl getSrcFeature(TypeImpl tgtType, FeatureImpl tgtFeat) {
    return getToFeature(fTgt2Src, tgtType, tgtFeat);
  }

  /**
   * Given a tgt type, return an array of source features in the order they would appear in the
   * target.
   * 
   * @param tgtType
   *          -
   * @return array of corresponding source features, in target type order
   */
  public FeatureImpl[] getSrcFeatures(TypeImpl tgtType) {
    return fTgt2Src[tgtType.getCode()];
  }

  public FeatureImpl getToFeature(FeatureImpl[][] mapByTypeCode, TypeImpl fromType,
          FeatureImpl fromFeat) {
    if (mapByTypeCode == null) { // is null if type systems ==
      return fromFeat;
    }
    FeatureImpl[] map = mapByTypeCode[fromType.getCode()];
    if (map == null) {
      return null;
    }
    final int offset = fromFeat.getOffset();
    if (map.length <= offset) {
      return null;
    }
    return map[offset];
  }

  /**
   * return true if no types are filtered
   * 
   * @param map
   * @param tsSrc
   * @param tsTgt
   * @return
   */
  private boolean addTypes(List<TypeImpl> map, TypeSystemImpl tsSrc, TypeSystemImpl tsTgt) {
    boolean r = true;
    for (TypeImpl tSrc : tsSrc.getAllTypes()) {
      TypeImpl ti = tsTgt.getType(tSrc.getName());
      Misc.setWithExpand(map, tSrc.getCode(), ti);
      r = r & (null != ti); // make r true only if all types are found
    }
    return r;
  }

  // @formatter:off
  /**
   * Create the map from tsFrom to tsTo for all the features, by type
   *   -- map created using type and feature name equality
   *   -- note: the features may have different definitions; map is by name only
   *     --- e.g., one may have String range, the other float range.
   *     --- in this case, the return is set to false.
   * @param map the map to update
   * @param tsFrom the From type system
   * @param tsTo the to type system
   * @return true if all the tsFrom features are found in tsTo and following fields are the same:
   *         rangeType.name, featureOffset, isMultipleRefsAllowed
   */
  // @formatter:on
  private boolean addFeatures(FeatureImpl[][] map, TypeSystemImpl tsFrom, TypeSystemImpl tsTo) {
    boolean r = true;

    for (TypeImpl ti : tsFrom.getAllTypes()) {
      TypeImpl toTi = tsTo.getType(ti.getName());
      if (toTi == null) {
        r = false;
        continue; // no corresponding type in tsTo
      }

      final FeatureImpl[] map1 = map[ti.getCode()] = new FeatureImpl[ti.getFeatureImpls().length];

      for (FeatureImpl fi : ti.getFeatureImpls()) {
        FeatureImpl toFi = toTi.getFeatureByBaseName(fi.getShortName());
        if (toFi == null) {
          r = false;
        } else {
          map1[fi.getOffset()] = toFi;
          // return false if the same-named feature doesn't match
          if (r && (!fi.getRange().getName().equals(toFi.getRange().getName())
                  || fi.getOffset() != toFi.getOffset()
                  || fi.isMultipleReferencesAllowed() != toFi.isMultipleReferencesAllowed())) {
            r = false;
          }
        }
      }
    }
    return r;
  }

}
