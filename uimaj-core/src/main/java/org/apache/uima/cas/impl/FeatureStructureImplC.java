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

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.UimaSerializableFSs;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.impl.JCasImpl;

//@formatter:off
/**
 * Feature structure implementation (for non JCas and JCas)
 * 
 * Each FS has 
 *   - int data 
 *     - used for boolean, byte, short, int, long, float, double data
 *       -- long and double use 2 int slots
 *     - may be null if all slots are in JCas cover objects as fields
 *   - ref data
 *     - used for references to other Java objects, such as 
 *       -- strings
 *       -- other feature structures
 *       -- arbitrary Java Objects
 *     - may be null if all slots are in JCas cover objects as fields
 *   - an id: an incrementing integer, starting at 1, per CAS, of all FSs created for that CAS
 *   - a ref to the casView where this FS was created
 *   - a ref to the TypeImpl for this class
 *     -- can't be static - may be multiple type systems in use
 * 
 */
//@formatter:on
public class FeatureStructureImplC implements FeatureStructureImpl {

  // note: these must be enabled to make the test cases work
  public static final String DISABLE_RUNTIME_FEATURE_VALIDATION = "uima.disable_runtime_feature_validation";
  public static final boolean IS_ENABLE_RUNTIME_FEATURE_VALIDATION = !Misc
          .getNoValueSystemProperty(DISABLE_RUNTIME_FEATURE_VALIDATION);

  public static final String DISABLE_RUNTIME_FEATURE_VALUE_VALIDATION = "uima.disable_runtime_feature_value_validation";
  public static final boolean IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION = !Misc
          .getNoValueSystemProperty(DISABLE_RUNTIME_FEATURE_VALUE_VALIDATION);

  public static final String V2_PRETTY_PRINT = "uima.v2_pretty_print_format";
  public static final boolean IS_V2_PRETTY_PRINT = // debug true ||
          Misc.getNoValueSystemProperty(V2_PRETTY_PRINT);

  private static final boolean traceFSs = CASImpl.traceFSs;

  // next is for experiment (Not implemented) of allocating multiple int arrays for different fss

//@formatter:off
  // // 3322 2222 2222 1111 1111 1100 0000 0000
  // // 1098 7654 3210 9876 5432 1098 7654 3210
  // //-------------------------------------------
  // // 0000 0000 0001 1111 1111 1000 0000 0000 int offset mask
  // // 0111 1111 1110 0000 0000 0000 0000 0000 ref offset mask
  //
  // private static final int bitMaskIntOffset = 0x001ff800;
  // private static final int bitMaskRefOffset = 0x7fe00000;
  // private static final int shiftIntOffset = 11;
  // private static final int shiftRefOffset = 21;
//@formatter:on

  private static final int _BIT_IN_SET_SORTED_INDEX = 1;
  private static final int _BIT_PEAR_TRAMPOLINE = 2;
  private static final int _BIT_JCASHASHMAP_RESERVE = 4;

  // data storage
  // slots start with _ to prevent name collision with JCas style getters and setters.

//@formatter:off
  /**
   * Experiment:
   *   goal: speed up allocation and maybe improve locality of reference
   *         a) have _intData and _refData point to 
   *             1) for array sizes < 256, a common shared array used with an offset
   *             2) for array sizes > 256, individual arrays as is the previous design case
   * 
   *         b) have accesses use an offset kept in the flags; 
   *            allocate in blocks of 1k
   *              the larger, the less java object overhead per
   *              the larger, the less "breakage" waste
   *              the smaller, the better GC 
   * offset = 10 bits * 2 (one for int, one for ref)
   * 
   *   results: on 16-way processor (64 hyperthreaded cores), caused 2x slowdown, probably due to cache
   *     contention.         
   */
//@formatter:on
  private final int[] _intData;
  private final Object[] _refData;
  protected final int _id; // a separate slot for access without loading _intData object
  private int _flags = 0; // a set of flags
                          // bit 0 (least significant): fs is in one or more non-bag indexes
                          // bit 1 is on for Pear trampoline FS sharing base int/ref data
                          // bit 2 is on for "reserve" element in JCasHashMap
                          // bit 3-20 reserved
                          // bits 21-30 reserved // experiment: ref offset
                          // bits 11-20 reserved // experiment: int offset
                          // bit 31 reserved

  /**
   * These next two object references are the same for every FS of this class created in one view.
   * So, they could be stored in a shared object But that would trade off saving one "reference" for
   * adding one extra load to get to the value This design uses more space instead.
   */

  /**
   * The view this Feature Structure was originally created in. Feature Structures may be indexed in
   * multiple views, or in no views.
   * 
   * Also used to access other metadata including the type system
   */
  public final CASImpl _casView;

  private TypeImpl _typeImpl; // for backwards compatibility and deser typed arrays: support
                              // switching the type

  // Called only to generate a dummy value for the REMOVED flag in bag indexes

  public FeatureStructureImplC() {
    _casView = null;
    _typeImpl = null;
    _intData = null;
    _refData = null;
    _id = 0;
  }

  /**
   * For use in creating search keys
   * 
   * @param id
   *          -
   */
  protected FeatureStructureImplC(int id) {
    _casView = null;
    _typeImpl = null;
    _intData = null;
    _refData = null;
    _id = id;
  }

  /**
   * For non-JCas use, Internal Use Only, called by cas.createFS via generators
   * 
   * @param casView
   *          -
   * @param type
   *          -
   */
  protected FeatureStructureImplC(TypeImpl type, CASImpl casView) {
    _casView = casView;
    _typeImpl = type;
    _id = casView.getNextFsId((TOP) this);

    if (_casView.maybeMakeBaseVersionForPear(this, _typeImpl)) {
      _setPearTrampoline();
    }

    FeatureStructureImplC baseFs = _casView.pearBaseFs;
    if (null != baseFs) {
      _intData = baseFs._intData;
      _refData = baseFs._refData;
      _casView.pearBaseFs = null;
    } else {
      _intData = _allocIntData();
      _refData = _allocRefData();
    }

    if (traceFSs && !(this instanceof CommonArrayFS)) {
      _casView.traceFSCreate(this);
    }

    _casView.maybeHoldOntoFS(this);
  }

  /**
   * For JCas use (done this way to allow "final") The TypeImpl is derived from the JCas cover class
   * name
   * 
   * @param jcasImpl
   *          - the view this is being created in
   */

  protected FeatureStructureImplC(JCasImpl jcasImpl) {
    _casView = jcasImpl.getCasImpl();
    _typeImpl = _casView.getTypeSystemImpl().getJCasRegisteredType(getTypeIndexID());
    _id = _casView.getNextFsId((TOP) this);

    if (null == _typeImpl) {
      throw new CASRuntimeException(CASRuntimeException.JCAS_TYPE_NOT_IN_CAS,
              this.getClass().getName());
    }

    if (_casView.maybeMakeBaseVersionForPear(this, _typeImpl)) {
      _setPearTrampoline();
    }

    FeatureStructureImplC baseFs = _casView.pearBaseFs;
    if (null != baseFs) {
      _intData = baseFs._intData;
      _refData = baseFs._refData;
      _casView.pearBaseFs = null;
    } else {
      _intData = _allocIntData();
      _refData = _allocRefData();
    }

    if (traceFSs && !(this instanceof CommonArrayFS)) {
      _casView.traceFSCreate(this);
    }

    _casView.maybeHoldOntoFS(this);

    // if (_typeImpl.featUimaUID != null) {
    // final int id = _casView.getAndIncrUimaUID();
    // _setLongValueNcNj(_typeImpl.featUimaUID, id);
    // _casView.add2uid2fs(id, (TOP)this);
    // }
  }

  private int[] _allocIntData() {
    final int c = _typeImpl.nbrOfUsedIntDataSlots;
    if (c != 0) {
      // _setIntDataArrayOffset(_casView.allocIntData(c));
      // return _casView.getReturnIntDataForAlloc();
      return new int[c];
    }
    return null;
  }

  private Object[] _allocRefData() {
    final int c = _typeImpl.nbrOfUsedRefDataSlots;
    if (c != 0) {
      // _setRefDataArrayOffset(_casView.allocRefData(c));
      // return _casView.getReturnRefDataForAlloc();
      return new Object[c];
    }
    return null;
  }

  // ***********************
  // Index Add Remove
  // ***********************

  /**
   * add the corresponding FeatureStructure to all Cas indexes in the view where this FS was created
   */
  public void addToIndexes() {
    _casView.addFsToIndexes(this);
  }

  /**
   * add this FS to indexes in a specific view, perhaps different from the creation view
   * 
   * @param jcas
   *          the JCas
   */
  public void addToIndexes(JCas jcas) {
    jcas.getCas().addFsToIndexes(this);
  }

  public void addToIndexes(CAS cas) {
    cas.addFsToIndexes(this);
  }

  /**
   * remove the corresponding FeatureStructure from all Cas indexes in the view where this FS was
   * created
   */
  public void removeFromIndexes() {
    removeFromIndexes(_casView);
  }

  /**
   * remove this FS from indexes in a specific view, perhaps different from the view where this was
   * created.
   * 
   * @param cas
   *          the Cas
   */
  public void removeFromIndexes(CAS cas) {
    cas.removeFsFromIndexes(this);
  }

  /**
   * remove this FS from indexes in a specific view, perhaps different from the view where this was
   * created.
   * 
   * @param jcas
   *          the Cas
   */
  public void removeFromIndexes(JCas jcas) {
    jcas.removeFsFromIndexes(this);
  }

  public LowLevelCAS getLowLevelCas() {
    return _casView;
  }

  // *******************************
  // IDs and Type
  // *******************************
  /**
   * NOTE: Possible name collision
   * 
   * @return the internal id of this fs - unique to this CAS, a positive int
   */
  @Override
  public final int getAddress() {
    return _casView.ll_getFSRef(this); // adds this fs to the internal map if needed
  };

  @Override
  public final int _id() {
    return _id;
  };

  /**
   * Returns the UIMA TypeImpl value
   */
  @Override
  public Type getType() {
    return _typeImpl;
  }

  /**
   * starts with _
   * 
   * @return the UIMA TypeImpl for this Feature Structure
   */
  @Override
  public int _getTypeCode() {
    return _typeImpl.getCode();
  }

  public CASImpl _getView() {
    return _casView;
  }

//@formatter:off
  /* *********************************************************
   * Get and Set features indirectly, via Feature objects
   * 
   * There are two implementations, depending on whether or not
   * the feature has a JCas getter/setter.
   *   - If yes, then these just delegate to that (via a 
   *     functional interface stored in the Feature)
   *     -- there are multiple functional interfaces, corresponding
   *        to the all the different (primitive) return values:
   *        boolean, byte, short, int, long, float, double, and "Object"
   *          used for String and FeatureStructures
   *   - if no, then converge the code to an _intData or _refData reference
   ***********************************************************/
//@formatter:on

//@formatter:off
  /**************************************
   *           S E T T E R S 
   * 4 levels of checking:  
   *   - check feature for validity (fv)
   *     -- this is skipped with feature comes from fs type info (internal calls)
   *   - check for setting something which could corrupt indexes (ci)
   *     -- this is skipped when the caller knows 
   *        --- the FS is not in the index, perhaps because they just created it
   *     -- skipped when the range is not a valid index key   
   *   - check for needing to log (journal) setting  (jrnl)
   *     -- this is skipped when the caller knows 
   *       --- no journalling is enabled or
   *       --- the FS is a new (above-the-line) FS
   *   - check the value is suitable
   *     -- this can be skipped if Java is doing the checking (via the type of the argument)
   *     -- done for string subtypes and Feature References
   *       --- skipped if the caller knows the value is OK (e.g., it is copying an existing FS)
   * 
   *   The jrnl and ic checks require the FeatureImpl. 
   *     For setters using these checks, there are two versions: 
   *       - one with the arg being the FeatureImpl (if it is available at the caller) and
   *       - one with the int offset (common code coverts this to the Feature Impl).
   * 
   * all 4 checks are normally done by the standard API call in the FeatureStructure interface
   * setXyzValue(Feature, value)
   * 
   * Besides the standard API call, other setter methods have suffixes and prefixes to the setter name
   *   - prefix is "_" to avoid conflicting with existing other names
   *   - suffixes are: 
   *     -- Nfc:    skip feature validity checking, ( ! fv,   jrnl,   ci )  (int/Feat)
   *     -- NcNj:   implies Nfc,                    ( ! fv, ! jrnl, ! ci )  (int/Feat)
   *     -- NcWj:   implies Nfc,                    ( ! fv,   jrnl, ! ci )  (int)
   *          The is for setters where value checking might be needed (i.e., Java checking isn't sufficient)
   *     -- NcNjNv: implies Nfc, skips all checks
   * 
   * For JCas setters: convert offset to feature
   **************************************/
//@formatter:on

  private void checkFeatRange(Feature feat, String shortRangeName) {
    if (!(feat.getRange().getShortName().equals(shortRangeName))) {
      /* Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}". */
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, feat.getName(),
              "uima.cas." + shortRangeName, feat.getRange().getName());
    }

  }

  @Override
  public void setBooleanValue(Feature feat, boolean v) {
    checkFeatRange(feat, "Boolean");
    _setIntValueCJ((FeatureImpl) feat, v ? 1 : 0);
  }

  public void _setBooleanValueNfc(int adjOffset, boolean v) {
    _setIntValueNfcCJ(adjOffset, v ? 1 : 0);
  }

  public final void _setBooleanValueNcNj(FeatureImpl fi, boolean v) {
    _setIntValueCommon(fi, v ? 1 : 0);
  }

  public final void _setBooleanValueNcNj(int adjOffset, boolean v) {
    _setIntValueCommon(adjOffset, v ? 1 : 0);
  }

  @Override
  public void setByteValue(Feature feat, byte v) {
    checkFeatRange(feat, "Byte");
    _setIntValueCJ((FeatureImpl) feat, v);
  }

  public void _setByteValueNfc(int adjOffset, byte v) {
    _setIntValueNfcCJ(adjOffset, v);
  }

  public void _setByteValueNcNj(FeatureImpl fi, byte v) {
    _setIntValueCommon(fi, v);
  }

  public void _setByteValueNcNj(int adjOffset, byte v) {
    _setIntValueCommon(adjOffset, v);
  }

  @Override
  public void setShortValue(Feature feat, short v) {
    checkFeatRange(feat, "Short");
    _setIntValueCJ((FeatureImpl) feat, v);
  }

  public void _setShortValueNfc(int adjOffset, short v) {
    _setIntValueNfcCJ(adjOffset, v);
  }

  public void _setShortValueNcNj(FeatureImpl fi, short v) {
    _setIntValueCommon(fi, v);
  }

  public void _setShortValueNcNj(int adjOffset, short v) {
    _setIntValueCommon(adjOffset, v);
  }

  @Override
  public void setIntValue(Feature feat, int v) {
    checkFeatRange(feat, "Integer");
    _setIntValueCJ((FeatureImpl) feat, v);
  }

  public void _setIntValueNfc(int adjOffset, int v) {
    _setIntValueNfcCJ(adjOffset, v);
  }

  public void _setIntValueNcNj(FeatureImpl fi, int v) {
    _setIntValueCommon(fi, v);
  }

  public void _setIntValueNcNj(int adjOffset, int v) {
    _setIntValueCommon(adjOffset, v);
  }

  @Override
  public void setLongValue(Feature feat, long v) {
    checkFeatRange(feat, "Long");
    _setLongValueCJ((FeatureImpl) feat, v);
  }

  public void _setLongValueNfc(int adjOffset, long v) {
    FeatureImpl fi = _getFeatFromAdjOffset(adjOffset, true);
    _casView.setLongValue(this, fi, v); // has trace call
  }

  public void _setLongValueNcNj(FeatureImpl fi, long v) {
    _setLongValueNcNj(fi.getAdjustedOffset(), v);
  }

  public void _setLongValueNcNj(int adjOffset, long v) {
    // final int offset = adjOffset + _getIntDataArrayOffset();
    _intData[adjOffset] = (int) v; // narrowing cast discards all but lowest 32 bits; may change
                                   // sign of value
    _intData[adjOffset + 1] = (int) (v >> 32);
    if (traceFSs) {
      _casView.traceFSfeat(this, _getFeatFromAdjOffset(adjOffset, true), v);
    }
  }

  @Override
  public void setFloatValue(Feature feat, float v) {
    checkFeatRange(feat, "Float");
    _setIntValueCJ((FeatureImpl) feat, CASImpl.float2int(v));
  }

  protected void _setFloatValueNfc(int adjOffset, float v) {
    _setIntValueNfc(adjOffset, CASImpl.float2int(v));
  }

  public void _setFloatValueNcNj(FeatureImpl fi, float v) {
    _setIntValueCommon(fi, CASImpl.float2int(v));
    // _intData[fi.getAdjustedOffset()] = CASImpl.float2int(v);
  }

  public void _setFloatValueNcNj(int adjOffset, float v) {
    _setIntValueCommon(adjOffset, CASImpl.float2int(v));
    // _intData[adjOffset] = CASImpl.float2int(v);
  }

  @Override
  public void setDoubleValue(Feature feat, double v) {
    checkFeatRange(feat, "Double");
    _setLongValueCJ((FeatureImpl) feat, CASImpl.double2long(v));
  }

  protected void _setDoubleValueNfc(int adjOffset, double v) {
    _setLongValueNfc(adjOffset, CASImpl.double2long(v));
  }

  public void _setDoubleValueNcNj(FeatureImpl fi, double v) {
    _setLongValueNcNj(fi, CASImpl.double2long(v));
  }

  public void _setDoubleValueNcNj(int adjOffset, double v) {
    _setLongValueNcNj(adjOffset, CASImpl.double2long(v));
  }

  @Override
  public void setStringValue(Feature feat, String v) {
    // if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat); // done by _setRefValueCJ
    // if (IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION) featureValueValidation(feat, v); // verifies
    // feat can take a string
    subStringRangeCheck(feat, v);
    _setRefValueCJ((FeatureImpl) feat, v);
  }

  public void _setStringValueNfc(int adjOffset, String v) {
    FeatureImpl fi = _getFeatFromAdjOffset(adjOffset, false);
    subStringRangeCheck(fi, v);
    _setRefValueNfcCJ(fi, v);
  }

  public void _setStringValueNcNj(FeatureImpl fi, String v) {
    subStringRangeCheck(fi, v);
    _setRefValueCommon(fi, v);
  }

  /**
   * Skips substring range checking, but maybe does journalling
   * 
   * @param adjOffset
   *          offset
   * @param v
   *          to set
   */
  public void _setStringValueNcWj(int adjOffset, String v) {
    _setRefValueCommonWj(_getFeatFromAdjOffset(adjOffset, false), v);
  }

  @Override
  public void setFeatureValue(Feature feat, FeatureStructure v) {
    FeatureImpl fi = (FeatureImpl) feat;
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    if (IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION)
      featureValueValidation(feat, v);
    // no need to check for index corruption because fs refs can't be index keys
    _setRefValueCommon(fi, _maybeGetBaseForPearFs((TOP) v));
    _casView.maybeLogUpdate(this, fi);
  }

  public void _setFeatureValueNcNj(FeatureImpl fi, Object v) {
    _setRefValueCommon(fi, v);
  }

  public void _setFeatureValueNcNj(int adjOffset, Object v) {
    _setRefValueCommon(adjOffset, v);
  }

  /**
   * Called when setting a FS value which might be a trampoline
   * 
   * @param v
   *          the FS to check
   * @param <N>
   *          the type of the FS
   * @return the FS or if it was a trampoline, the base FS
   */
  protected <N extends TOP> N _maybeGetBaseForPearFs(N v) {
    return (v == null) ? null : v._maybeGetBaseForPearFs();
  }

  /**
   * Called to convert to the base FS from a Pear version
   * 
   * @param <N>
   *          the type of the FS
   * @return the FS or if it was a trampoline, the base FS
   */
  public <N extends TOP> N _maybeGetBaseForPearFs() {
    return this._isPearTrampoline() ? _casView.getBaseFsFromTrampoline((N) this) : (N) this;
  }

  /**
   * Called when getting a FS value which might need to return a Pear context's trampoline
   * 
   * @param v
   *          the FS to check
   * @param <N>
   *          the type of the FS
   * @return the FS or if we're in a Pear context, perhaps the trampoline (only some classes might
   *         have trampolines)
   */
  protected <N extends TOP> N _maybeGetPearFs(N v) {
    return (_casView.inPearContext()) ? CASImpl.pearConvert(v) : v;
  }

  /**
   * @param <N>
   *          the type of the FS
   * @return the FS or if we're in a Pear context and the PEAR defines a different version, the PEAR
   *         version.
   */
  public <N extends TOP> N _maybeGetPearFs() {
    return (_casView.inPearContext()) ? CASImpl.pearConvert((N) this) : (N) this;
  }

  /**
   * Nc - no check, Wj = with journaling if needed
   * 
   * @param adjOffset
   *          -
   * @param v
   *          -
   */
  public void _setFeatureValueNcWj(int adjOffset, FeatureStructure v) {
    _setRefValueCommonWj(_getFeatFromAdjOffset(adjOffset, false), _maybeGetBaseForPearFs((TOP) v));
  }

  // @Override
  // public void setJavaObjectValue(Feature feat, Object v) {
  // FeatureImpl fi = (FeatureImpl) feat;
  //
  // if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
  // if (IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION) featureValueValidation(feat, v);
  // _setRefValueCJ(fi, v);
  // }

  // public void _setJavaObjectValueNcNj(FeatureImpl fi, Object v) {
  // _setRefValueCommon(fi, v);
  // }

  @Override
  public void setFeatureValueFromString(Feature feat, String s) throws CASRuntimeException {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    CASImpl.setFeatureValueFromString(this, (FeatureImpl) feat, s);
  }

  /**
   * All 3 checks
   * 
   * @param fi
   *          - the feature
   * @param v
   *          - the value
   */
  protected void _setIntValueCJ(FeatureImpl fi, int v) {
    if (!fi.isInInt) {

      /** Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}". */
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, fi.getName(),
              "boolean, byte, short, int, or float", fi.getRange().getName());

    }
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(fi);
    _casView.setWithCheckAndJournal((TOP) this, fi.getCode(), () -> _setIntValueCommon(fi, v));

  }

  /**
   * All 3 checks for long
   * 
   * @param fi
   *          - the feature
   * @param v
   *          - the value
   */
  protected void _setLongValueCJ(FeatureImpl fi, long v) {
    if (!fi.isInInt) {
      /** Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}". */
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, fi.getName(),
              "long or double", fi.getRange().getName());
    }
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(fi);
    _casView.setLongValue(this, fi, v);
  }

  /**
   * 2 checks, no feature check
   * 
   * @param adjOffset
   *          - the feature offset
   * @param v
   *          - the value
   */
  protected void _setIntValueNfcCJ(int adjOffset, int v) {
    FeatureImpl fi = _getFeatFromAdjOffset(adjOffset, true);
    _casView.setWithCheckAndJournal((TOP) this, fi, () -> _setIntValueCommon(adjOffset, v));
  }

  /**
   * 2 checks, no feature check
   * 
   * @param fi
   *          - the feature
   * @param v
   *          - the value
   */
  protected void _setLongValueNfcCJ(FeatureImpl fi, long v) {
    _casView.setLongValue(this, fi, v);
  }

  protected void _setRefValueCJ(FeatureImpl fi, Object v) {
    if (fi.isInInt) {
      /** Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}". */
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, fi.getName(), "int",
              fi.getRange().getName());
    }
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(fi);
    _casView.setWithCheckAndJournal((TOP) this, fi.getCode(), () -> _setRefValueCommon(fi, v));

  }

  /**
   * 2 checks, no feature check
   * 
   * @param fi
   *          - the feature
   * @param v
   *          - the value
   */
  protected void _setRefValueNfcCJ(FeatureImpl fi, Object v) {
    _casView.setWithCheckAndJournal((TOP) this, fi.getCode(), () -> _setRefValueCommon(fi, v));
  }

//@formatter:off
  /********************************************************************************************************
   * G E T T E R S
   * 
   *  (The array getters are part of the Classes for the built-in arrays, here are only the non-array ones)
   * 
   *  getXyzValue(Feature feat) - this is the standard from V2 plain API
   *                            - it does validity checking (normally) that the feature belongs to the type
   *  getXyzValueNc(FeatureImpl feat) - skips the validity checking that the feature belongs to the type.                          
   *********************************************************************************************************/
//@formatter:on
  @Override
  public boolean getBooleanValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    checkFeatRange(feat, "Boolean");
    return _getBooleanValueNc((FeatureImpl) feat);
  }

  public boolean _getBooleanValueNc(FeatureImpl fi) {
    return _getIntValueCommon(fi) == 1;
  }

  // for JCas use
  public boolean _getBooleanValueNc(int adjOffset) {
    return _getIntValueCommon(adjOffset) == 1;
  }

  @Override
  public byte getByteValue(Feature feat) {
    checkFeatRange(feat, "Byte");
    return (byte) _getIntValueCommon((FeatureImpl) feat);
  }

  public byte _getByteValueNc(FeatureImpl feat) {
    return (byte) _getIntValueNc(feat);
  }

  public byte _getByteValueNc(int adjOffset) {
    return (byte) _getIntValueNc(adjOffset);
  }

  @Override
  public short getShortValue(Feature feat) {
    checkFeatRange(feat, "Short");
    return (short) _getIntValueCommon((FeatureImpl) feat);
  }

  public short _getShortValueNc(FeatureImpl feat) {
    return (short) _getIntValueNc(feat);
  }

  public short _getShortValueNc(int adjOffset) {
    return (short) _getIntValueNc(adjOffset);
  }

  @Override
  public int getIntValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    checkFeatRange(feat, "Integer");
    return _getIntValueCommon((FeatureImpl) feat);
  }

  public int _getIntValueNc(FeatureImpl feat) {
    return _getIntValueCommon(feat);
  }

  public int _getIntValueNc(int adjOffset) {
    return _getIntValueCommon(adjOffset);
  }

  @Override
  public long getLongValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    checkFeatRange(feat, "Long");
    return _getLongValueNc((FeatureImpl) feat);
  }

  public long _getLongValueNc(FeatureImpl feat) {
    return _getLongValueNc(feat.getAdjustedOffset());

  }

  public long _getLongValueNc(int adjOffset) {
    /**
     * When converting the lower 32 bits to a long, sign extension is done, so have to 0 out those
     * bits before or-ing in the high order 32 bits.
     */
    // final int offset = adjOffset + _getIntDataArrayOffset();
    return ((_intData[adjOffset]) & 0x00000000ffffffffL) | (((long) _intData[adjOffset + 1]) << 32);
  }

  @Override
  public float getFloatValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    checkFeatRange(feat, "Float");
    return _getFloatValueNc(((FeatureImpl) feat).getAdjustedOffset());
  }

  public float _getFloatValueNc(FeatureImpl fi) {
    return _getFloatValueNc(fi.getAdjustedOffset());
  }

  public float _getFloatValueNc(int adjOffset) {
    return CASImpl.int2float(_getIntValueCommon(adjOffset));
  }

  @Override
  public double getDoubleValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    checkFeatRange(feat, "Double");
    return _getDoubleValueNc((FeatureImpl) feat);
  }

  public double _getDoubleValueNc(FeatureImpl fi) {
    return _getDoubleValueNc(fi.getAdjustedOffset());
  }

  public double _getDoubleValueNc(int adjOffset) {
    return CASImpl.long2double(_getLongValueNc(adjOffset));
  }

  @Override
  public String getStringValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    // checkFeatRange(feat, "String");
    return _getStringValueNc((FeatureImpl) feat);
  }

  public String _getStringValueNc(FeatureImpl feat) {
    return _getStringValueNc(feat.getAdjustedOffset());
  }

  public String _getStringValueNc(int adjOffset) {
    return (String) _refData[adjOffset /* + _getRefDataArrayOffset() */];
  }

  @Override
  public TOP getFeatureValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION)
      _Check_feature_defined_for_this_type(feat);
    _check_feature_range_is_FeatureStructure(feat, this);
    return _getFeatureValueNc((FeatureImpl) feat);
  }

  public TOP _getFeatureValueNc(FeatureImpl feat) {
    return _getFeatureValueNc(feat.getAdjustedOffset());
  }

  public TOP _getFeatureValueNc(int adjOffset) {
    return _maybeGetPearFs((TOP) _refData[adjOffset /* + _getRefDataArrayOffset() */]);
  }

  // @Override
  // public Object getJavaObjectValue(Feature feat) {
  // if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
  // return _getJavaObjectValueNc((FeatureImpl) feat);
  // }
  //
  // public Object _getJavaObjectValueNc(FeatureImpl fi) { return _getRefValueCommon(fi); }
  //
  // public Object _getJavaObjectValueNc(int adjOffset) { return _getRefValueCommon(adjOffset); }
  //
  /**
   * @return the CAS view where this FS was created
   */
  @Override
  public CAS getCAS() {
    return this._casView;
  }

  public CASImpl getCASImpl() { // was package private 9-03
    return this._casView;
  }

//@formatter:off
  /**
   * See http://www.javaworld.com/article/2076332/java-se/how-to-avoid-traps-and-correctly-override-methods-from-java-lang-object.html
   * for suggestions on avoiding bugs in implementing clone
   * 
   * Because we have final fields for _intData, _refData, and _id, we can't use clone.
   * Instead, we use the createFS to create the FS of the right type.  This will use the generators.
   * 
   * Strategy for cloning:
   *   Goal is to create an independent instance of some subtype of this class, with 
   *   all the fields properly copied from this instance.
   *     - some fields could be in the _intData and _refData
   *     - some fields could be stored as features
   * 
   * Subcases to handle:
   *   - arrays - these have no features.
   * 
   * Note: CasCopier doesn't call this because it needs to do a deep copy
   *       This is not used by the framework
   * 
   * @return a new Feature Structure as a new instance of the same class, 
   *         with a new _id field, 
   *         with its features set to the values of the features in this Feature Structure
   * @throws CASRuntimeException (different from Object.clone()) if an exception occurs   
   */
//@formatter:on
  @Override
  public FeatureStructureImplC clone() throws CASRuntimeException {

    if (_typeImpl.isArray()) {
      CommonArrayFS original = (CommonArrayFS) this;
      CommonArrayFS copy = (CommonArrayFS) _casView.createArray(_typeImpl, original.size());
      copy.copyValuesFrom(original);
      return (FeatureStructureImplC) copy;
    }

    TOP fs = _casView.createFS(_typeImpl);
    TOP srcFs = (TOP) this;

    fs._copyIntAndRefArraysEqTypesFrom(srcFs);

    /* copy all the feature values except the sofa ref which is already set as part of creation */
    // for (FeatureImpl feat : _typeImpl.getFeatureImpls()) {
    // CASImpl.copyFeature(srcFs, feat, fs);
    // } // end of for loop
    return fs;
  }

  @Override
  public int hashCode() {
    return _id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object) must match hashCode must match comparator == 0,
   * equal == true Only valid for FSs in same CAS
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FeatureStructureImplC) {
      FeatureStructureImplC c2 = (FeatureStructureImplC) obj;

      if (c2._id != this._id)
        return false;

      return (_casView == null && c2._casView == null) || (_casView != null && c2._casView != null
              && (_casView == c2._casView || _casView.getBaseCAS() == c2._casView.getBaseCAS()));

      // if (_casView == null && c2._casView == null) {
      // return true; // special case for removed marker
      // }
      // if (_casView != null && c2._casView != null &&
      // (_casView == c2._casView ||
      // _casView.getBaseCAS() == c2._casView.getBaseCAS())) {
      // return true;
      // }
    }
    // throw new IllegalArgumentException("Can't invoke equals on two FS from different CASes.");
    return false;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Pretty printing.
  // public for use by superclass for backwards compatibility
  public static class PrintReferences {

    static final int NO_LABEL = 0;

    static final int WITH_LABEL = 1;

    static final int JUST_LABEL = 2;

    private static final String refNamePrefix = "#";

    // map from fs to special string #nnnn for printing refs
    // three states:
    // 1) key not in map
    // 2) key in map, but value is "seen once" - first time value seen
    // 3) key in map, value is #nnnn - when value is seen more than once
    private Map<FeatureStructure, String> tree = new HashMap<>();

    private Set<FeatureStructure> seen = new HashSet<>();

    private int count;

    private PrintReferences() {
      this.count = 0;
    }

    /**
     * @param fs
     *          -
     * @return true if seen before
     */
    boolean addReference(FeatureStructure fs) {
      String v = tree.get(fs);
      if (null == v) {
        tree.put(fs, "seen once");
        return false;
      }
      if (v.equals("seen once")) {
        tree.put(fs, refNamePrefix + Integer.toString(this.count++));
      }
      return true;
    }

    String getLabel(FeatureStructure ref) {
      return this.tree.get(ref);
    }

    int printInfo(FeatureStructure ref) {
      String k = this.tree.get(ref);
      if (k == null || k.equals("seen once")) {
        return NO_LABEL;
      }
      if (this.seen.contains(ref)) {
        return JUST_LABEL;
      }
      this.seen.add(ref);
      return WITH_LABEL;
    }

  }

  private final void getPrintRefs(PrintReferences printRefs) {
    getPrintRefs(printRefs, this);
  }

  /**
   * This is called, once, at the top level thing being printed. It recursively descends any
   * references, and updates the PrintReferences with info needed to handle circular structures
   * 
   * @param printRefs
   *          the PrintReferences to update
   * @param fs
   *          the top level FS being pretty printed, to descend if needed
   */
  private final void getPrintRefs(PrintReferences printRefs, FeatureStructureImplC fs) {
    if (null == fs) {
      return;
    }
    boolean seenBefore = printRefs.addReference(fs);
    if (seenBefore) {
      return;
    }

    final TypeImpl ti = fs._typeImpl;
    if (ti != null) { // null for REMOVED marker
      // for v2 style, don't descend fs arrays; these are omitted
      if (!IS_V2_PRETTY_PRINT && ti.isArray() && (fs instanceof FSArray)) {
        for (TOP item : ((FSArray) fs)._getTheArray()) {
          getPrintRefs(printRefs, item);
        }
      } else {
        if (fs instanceof UimaSerializableFSs) {
          ((UimaSerializableFSs) fs)._save_fsRefs_to_cas_data();
        }
        ti.getFeaturesAsStream().filter(fi -> fi.getRangeImpl().isRefType) // is ref type
                .map(fi -> fs.getFeatureValue(fi)) // get the feature value
                .filter(refFs -> refFs != null) // skip null ones
                .forEachOrdered(refFs -> getPrintRefs(printRefs, refFs));
      }
    }
  }

  @Override
  public String toString() {
    return toString(3);
  }

  @Override
  public String toString(int indent) {
    StringBuilder buf = new StringBuilder();
    prettyPrint(0, indent, buf, true, null);
    return buf.toString();
  }

  /*
   * This next bit is to remain backward compatible with callers using StringBuilders or
   * StringBuffers. (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.FeatureStructureImpl#prettyPrint(int, int,
   * java.lang.StringBuilder, boolean)
   */
  @Override
  public void prettyPrint(int indent, int incr, StringBuilder buf, boolean useShortNames) {
    prettyPrint(indent, incr, buf, useShortNames, null);
  }

  @Override
  public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames) {
    prettyPrint(indent, incr, buf, useShortNames, null);
  }

  /**
   * Top level, does computation of self-ref Pretty prints this Feature Structure, no trailing nl
   * Old form - uses string buffer.
   * 
   * @param indent
   *          the indent amount
   * @param incr
   *          the amount the indent is increased for a level
   * @param buf
   *          where the resulting string is built
   * @param useShortNames
   *          true to use short name
   * @param s
   *          extra string to print
   * @deprecated use form with StringBuilder (not StringBuffer)
   */
  @Deprecated
  @Override
  public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames, String s) {
    PrintReferences printRefs = new PrintReferences();
    getPrintRefs(printRefs);
    prettyPrint(indent, incr, buf, useShortNames, s, printRefs);
  }

  /**
   * Top level, does computation of self-ref Pretty prints this Feature Structure, no trailing nl
   * 
   * @param indent
   *          the indent amount
   * @param incr
   *          the amount the indent is increased for a level
   * @param buf
   *          where the resulting string is built
   * @param useShortNames
   *          true to use short name
   * @param s
   *          extra string to print
   */
  @Override
  public void prettyPrint(int indent, int incr, StringBuilder buf, boolean useShortNames,
          String s) {
    PrintReferences printRefs = new PrintReferences();
    getPrintRefs(printRefs);
    prettyPrint(indent, incr, buf, useShortNames, s, printRefs);
  }

  // old version from v2 using StringBuffer
  /**
   * Internal Use Only, public only for backwards compatibility
   * 
   * @param indent
   *          -
   * @param incr
   *          -
   * @param buf
   *          -
   * @param useShortNames
   *          -
   * @param s
   *          -
   * @param printRefs
   *          -
   * @deprecated because uses StringBuffer, not builder, for version 2 compatibility only
   */
  @Deprecated
  public void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames, String s,
          PrintReferences printRefs) {

    StringBuilder b2 = new StringBuilder(buf);
    prettyPrint(indent, incr, b2, useShortNames, s, printRefs);

    buf.setLength(0);
    buf.append(b2.toString());
  }

  public void prettyPrint(int indent, // the current indent position
          int incr, // the delta to indent this FS printing
          StringBuilder buf, boolean useShortNames, String s, // carries the "#123" id refs for
                                                              // others to use, labels this fs with
                                                              // that.
          PrintReferences printRefs) {
    prettyPrint(indent, incr, buf, useShortNames, s, printRefs, false);
  }

  /**
   * 
   * @param sb
   *          -
   */
  public void prettyPrintShort(StringBuilder sb) {
    prettyPrint(0, 2, sb, true, "", new PrintReferences(), true);
  }

  /**
   * recursively called by ppval
   * 
   * @param indent
   *          -
   * @param incr
   *          -
   * @param buf
   *          -
   * @param useShortNames
   *          -
   * @param s
   *          -
   * @param printRefs
   *          -
   * @param isShortForm_arg
   *          -
   */
  private void prettyPrint(int indent, // the current indent position
          int incr, // the delta to indent this FS printing
          final StringBuilder buf, boolean useShortNames, String s, // carries the "#123" id refs
                                                                    // for others to use, labels
                                                                    // this fs with that.
          PrintReferences printRefs, boolean isShortForm_arg) { // short form only prints type:_id
                                                                // for refs

    final boolean isShortForm =
            // isShortForm_arg;
            // debug
            // (this._id == 2512)
            // ? false
            // :
            isShortForm_arg;

    try {
      indent += incr;
      if (!IS_V2_PRETTY_PRINT && indent > 20 * incr) {
        buf.append(" ... past indent limit ... ");
        return;
      }

      final int printInfo = printRefs.printInfo(this);
      if (printInfo != PrintReferences.NO_LABEL) {
        String label = printRefs.getLabel(this);
        if (!label.equals("seen once")) {
          buf.append(printRefs.getLabel(this));
        }
        if (printInfo == PrintReferences.JUST_LABEL) {
          buf.append(IS_V2_PRETTY_PRINT ? ' ' : '\n');
          return;
        }
        buf.append(' ');
      }
      if (_typeImpl == null) {
        buf.append((_id == 0) ? " Special REMOVED marker "
                : _isJCasHashMapReserve() ? (" Special JCasHashMap Reserve, id = " + _id)
                        : " Special Search Key, id = " + _id);
      } else {
        if (useShortNames) {
          buf.append(getType().getShortName());
        } else {
          buf.append(getType().getName());
        }

        if (!IS_V2_PRETTY_PRINT) {
          buf.append(':').append(_id);
        }

        if (s != null) {
          buf.append(" \"" + s + "\"");
        }
      }
      buf.append('\n');

      // final int typeClass = this._casView.ll_getTypeClass(this.getType());

      if (_typeImpl == null) { // happens for special version which is REMOVED marker
        return;
      }
      switch (_getTypeCode()) {
        case TypeSystemConstants.stringArrayTypeCode: {
          StringArray a = (StringArray) this;
          printArrayElements(a.size(), i -> appendOrNull(buf, a.get(i)), indent, incr, buf);
          return;
        }
        case TypeSystemConstants.intArrayTypeCode: {
          IntegerArray a = (IntegerArray) this;
          printArrayElements(a.size(), i -> appendOrNull(buf, Integer.toString(a.get(i))), indent,
                  incr, buf);
          return;
        }
        case TypeSystemConstants.floatArrayTypeCode: {
          FloatArray a = (FloatArray) this;
          printArrayElements(a.size(), i -> appendOrNull(buf, Float.toString(a.get(i))), indent,
                  incr, buf);
          return;
        }
        case TypeSystemConstants.booleanArrayTypeCode: {
          BooleanArray a = (BooleanArray) this;
          printArrayElements(a.size(), i -> appendOrNull(buf, Boolean.toString(a.get(i))), indent,
                  incr, buf);
          return;
        }
        case TypeSystemConstants.byteArrayTypeCode: {
          ByteArray a = (ByteArray) this;
          printArrayElements(a.size(), i -> appendOrNull(buf, Byte.toString(a.get(i))), indent,
                  incr, buf);
          return;
        }
        case TypeSystemConstants.shortArrayTypeCode: {
          ShortArray a = (ShortArray) this;
          printArrayElements(a.size(), i -> appendOrNull(buf, Short.toString(a.get(i))), indent,
                  incr, buf);
          return;
        }
        case TypeSystemConstants.longArrayTypeCode: {
          LongArray a = (LongArray) this;
          printArrayElements(a.size(), i -> appendOrNull(buf, Long.toString(a.get(i))), indent,
                  incr, buf);
          return;
        }
        case TypeSystemConstants.doubleArrayTypeCode: {
          DoubleArray a = (DoubleArray) this;
          printArrayElements(a.size(), i -> appendOrNull(buf, Double.toString(a.get(i))), indent,
                  incr, buf);
          return;
        }
        case TypeSystemConstants.fsArrayTypeCode: {
          if (IS_V2_PRETTY_PRINT) {
            break; // v2 did not descend to print FSArray contents
          }
          FSArray a = (FSArray) this;
          printFSArrayElements(a, indent, incr, buf, useShortNames, printRefs, isShortForm);

          return;
        } // end of case

      } // end of switch

      // if get here, non of the cases in the above switch fit

      if (this instanceof FSArray) { // catches instance of FSArrays which are "typed" to hold
                                     // specific element types
        if (IS_V2_PRETTY_PRINT) {
          return; // v2 did not descend to print fs array contents
        }
        FSArray a = (FSArray) this;
        printFSArrayElements(a, indent, incr, buf, useShortNames, printRefs, isShortForm);
        return;
      }

      for (FeatureImpl fi : _typeImpl.getFeatureImpls()) {
        Misc.indent(buf, indent);
        buf.append(fi.getShortName() + ": ");
        TypeImpl range = (TypeImpl) fi.getRange();
        if (range.isPrimitive()) { // Strings and string subtypes are primitive
          addStringOrPrimitive(buf, fi);
          continue;
        }

        // not primitive
        FeatureStructureImplC val = null;
        boolean hadException = false;
        try {
          val = getFeatureValue(fi);
        } catch (Exception e) {
          buf.append("<exception ").append(e.getMessage()).append(">\n");
          hadException = true;
        }
        if (!hadException) {
          if (isShortForm) {
            if (null == val) {
              buf.append("<null>");
            } else {
              buf.append(val._getTypeImpl().getShortName()).append(':').append(val._id);
            }
          } else {
            // treat sofa refs special, since they're pervasive
            if (val instanceof Sofa) {
              buf.append(((Sofa) val).getSofaID());
            } else {
              ppval(val, indent, incr, buf, useShortNames, printRefs, false);
            }
          }

          // if (val != null && !val._typeImpl.getName().equals(CAS.TYPE_NAME_SOFA)) {
          // val.prettyPrint(indent, incr, buf, useShortNames, null, printRefs);
          // } else {
          // buf.append((val == null) ? "<null>\n" : ((SofaFS) val).getSofaID() + '\n');
          // }
        }

      }
    } catch (Exception e) {
      buf.append("**Caught exception: ").append(e);
      // StringWriter sw = new StringWriter();
      // e.printStackTrace(new PrintWriter(sw, true));
      // buf.append(sw.toString());
    }
  }

  public StringBuilder addStringOrPrimitive(StringBuilder sb, FeatureImpl fi) {
    TypeImpl range = (TypeImpl) fi.getRange();

    if (range.isStringOrStringSubtype()) {
      String stringVal = getStringValue(fi);
      stringVal = (null == stringVal) ? "<null>" : "\"" + Misc.elideString(stringVal, 80) + "\"";
      sb.append(stringVal); // caller adds nl
    } else {
      sb.append(this.getFeatureValueAsString(fi));
    }
    return sb;
  }

  private void ppval(FeatureStructureImplC val, int indent, int incr, StringBuilder buf,
          boolean useShortNames, PrintReferences printRefs, boolean isShortForm) {
    if (val != null && !val._typeImpl.getName().equals(CAS.TYPE_NAME_SOFA)) {
      val.prettyPrint(indent, incr, buf, useShortNames, null, printRefs, isShortForm);
    } else {
      buf.append((val == null) ? "<null>" : "sofa id: " + ((SofaFS) val).getSofaID());
    }
  }

//@formatter:off
  /**
   * For printing arrays except FSArrays; called after printing the type:nnn
   *   prints the length
   *   if the length = 0 that's all
   *   otherwise:
   *   uses Misc.addElementsToStringBuilder to output the elements.  This routine does
   *       [ + array contents + ], unless the line is too long, in which case it switches to multi-line
   * @param arrayLen the length
   * @param f the feature structure
   * @param indent the current indent
   * @param incr the indent incr
   * @param buf the stringbuilder where the result is added
   */
//@formatter:on
  private void printArrayElements(int arrayLen, IntConsumer f, int indent, int incr,
          StringBuilder buf) {
    Misc.indent(buf, indent);
    buf.append("Array length: " + arrayLen);
    if (arrayLen == 0) {
      return;
    }

    Misc.indent(buf, indent);
    buf.append("Array elements: ");
    if (IS_V2_PRETTY_PRINT) {
      buf.append("[");
      for (int i = 0; i < arrayLen; i++) {
        if (i > 0) {
          buf.append(", ");
        }
        f.accept(i); // this._casView.ll_getStringArrayValue(this.getAddress(), i);
      }
      buf.append("]"); // no extra new line
    } else {
      // no limit to size
      Misc.addElementsToStringBuilder(buf, arrayLen, arrayLen, indent, incr,
              (sb, i) -> f.accept(i));
    }
  }

//@formatter:off
  /**
   * For printing FSArrays; called after printing the type:nnn
   * Only called if ! IS_V2_PRETTY_PRINT, since v2 didn't print the array contents
   *   prints the length
   *   if the length = 0 that's all
   * otherwise:
   * 
   * @param arrayLen
   *          the length
   * @param f
   *          the feature structure
   * @param indent
   *          the current indent
   * @param incr
   *          the indent incr
   * @param buf
   *          the stringbuilder where the result is added
   */
//@formatter:on
  private void printFSArrayElements(FSArray fsarray, int indent, int incr, StringBuilder buf,
          boolean useShortNames, PrintReferences printRefs, boolean isShortForm) {
    Misc.indent(buf, indent);
    int arraylen = fsarray.size();
    buf.append("Array length: " + arraylen);
    if (arraylen == 0) {
      return;
    }

    Misc.indent(buf, indent);
    buf.append("Array elements: [");

    indent += incr;
    for (int i = 0; i < arraylen; i++) {
      Misc.indent(buf, indent);
      ppval((TOP) fsarray.get(i), indent, incr, buf, useShortNames, printRefs, isShortForm);
    }
    Misc.indent(buf, indent - incr);
    buf.append(']');
  }

  private void appendOrNull(StringBuilder sb, String v) {
    sb.append((v == null) ? "null" : v);
  }

  public int getTypeIndexID() {
    throw new CASRuntimeException(UIMARuntimeException.INTERNAL_ERROR); // dummy, always overridden
  }

  /**
   * Internal Use only
   * 
   * @param slotKind
   *          -
   * @param fi
   *          -
   * @param v
   *          -
   */
  public void _setIntLikeValue(SlotKind slotKind, FeatureImpl fi, int v) {
    switch (slotKind) {
      case Slot_Boolean:
        setBooleanValue(fi, v == 1);
        break;
      case Slot_Byte:
        setByteValue(fi, (byte) v);
        break;
      case Slot_Short:
        setShortValue(fi, (short) v);
        break;
      case Slot_Int:
        setIntValue(fi, v);
        break;
      case Slot_Float:
        setFloatValue(fi, CASImpl.int2float(v));
        break;
      default:
        Misc.internalError();
    }
  }

  /**
   * Internal Use only - no feature check, no journaling
   * 
   * @param slotKind
   *          -
   * @param fi
   *          -
   * @param v
   *          -
   */
  public void _setIntLikeValueNcNj(SlotKind slotKind, FeatureImpl fi, int v) {
    switch (slotKind) {
      case Slot_Boolean:
        _setBooleanValueNcNj(fi, v == 1);
        break;
      case Slot_Byte:
        _setByteValueNcNj(fi, (byte) v);
        break;
      case Slot_Short:
        _setShortValueNcNj(fi, (short) v);
        break;
      case Slot_Int:
        _setIntValueNcNj(fi, v);
        break;
      case Slot_Float:
        _setFloatValueNcNj(fi, CASImpl.int2float(v));
        break;
      default:
        Misc.internalError();
    }
  }

  /**
   * for compressed form 4 - for getting the prev value of int-like slots Uses unchecked forms for
   * feature access
   * 
   * @param slotKind
   *          -
   * @param f
   *          -
   * @return -
   */
  public int _getIntLikeValue(SlotKind slotKind, FeatureImpl f) {
    if (null == f) {
      switch (slotKind) {

        case Slot_Boolean: {
          BooleanArray a = (BooleanArray) this;
          return (a.size() == 0) ? 0 : a.get(0) ? 1 : 0;
        }

        case Slot_Byte: {
          ByteArray a = (ByteArray) this;
          return (a.size() == 0) ? 0 : a.get(0);
        }

        case Slot_Short: {
          ShortArray a = (ShortArray) this;
          return (a.size() == 0) ? 0 : a.get(0);
        }

        case Slot_Int: {
          IntegerArray a = (IntegerArray) this;
          return (a.size() == 0) ? 0 : a.get(0);
        }

        case Slot_Float: {
          FloatArray a = (FloatArray) this;
          return (a.size() == 0) ? 0 : CASImpl.float2int(a.get(0));
        }
        default:
          Misc.internalError();
          return 0;
      }
    }

    switch (slotKind) {
      case Slot_Boolean:
        return _getBooleanValueNc(f) ? 1 : 0;
      case Slot_Byte:
        return _getByteValueNc(f);
      case Slot_Short:
        return _getShortValueNc(f);
      case Slot_Int:
        return _getIntValueNc(f);
      case Slot_Float:
        return CASImpl.float2int(_getFloatValueNc(f));
      default:
        Misc.internalError();
        return 0;
    }
  }

  @Override
  public String getFeatureValueAsString(Feature feat) {
    FeatureImpl fi = (FeatureImpl) feat;
    TypeImpl range = fi.getRangeImpl();
    if (fi.isInInt) {
      switch (range.getCode()) {
        case TypeSystemConstants.floatTypeCode:
          return Float.toString(getFloatValue(feat));
        case TypeSystemConstants.booleanTypeCode:
          return Boolean.toString(getBooleanValue(feat));
        case TypeSystemConstants.longTypeCode:
          return Long.toString(getLongValue(feat));
        case TypeSystemConstants.doubleTypeCode:
          return Double.toString(getDoubleValue(feat));
        case TypeSystemConstants.byteTypeCode:
          return Byte.toString(getByteValue(feat));
        case TypeSystemConstants.shortTypeCode:
          return Short.toString(getShortValue(feat));
        default: // int,
          return Integer.toString(getIntValue(feat));
      }
    }

    if (range instanceof TypeImpl_string) {
      return getStringValue(feat);
    }

    // if (range.getCode() == TypeSystemConstants.javaObjectTypeCode) {
    // return CASImpl.serializeJavaObject(getJavaObjectValue(feat));
    // }

    if (range.isRefType) {
      TOP ref = getFeatureValue(feat);
      return (ref == null) ? null : ref.toString();
    }

    Misc.internalError();
    return null; // needed to avoid compile error
  }

  protected boolean _inSetSortedIndex() {
    return (_flags & _BIT_IN_SET_SORTED_INDEX) != 0;
  }

  protected void _setInSetSortedIndexed() {
    _flags |= _BIT_IN_SET_SORTED_INDEX;
  }

  /**
   * All callers of this must insure fs is not indexed in **Any** View
   */
  protected void _resetInSetSortedIndex() {
    _flags &= ~_BIT_IN_SET_SORTED_INDEX;
  }

  protected void _setJCasHashMapReserve() {
    _flags |= _BIT_JCASHASHMAP_RESERVE;
  }

  public boolean _isJCasHashMapReserve() {
    return (_flags & _BIT_JCASHASHMAP_RESERVE) != 0;
  }

  protected void _setPearTrampoline() {
    _flags |= _BIT_PEAR_TRAMPOLINE;
  }

  protected boolean _isPearTrampoline() {
    return (_flags & _BIT_PEAR_TRAMPOLINE) != 0;
  }

  protected FeatureImpl _getFeatFromAdjOffset(int adjOffset, boolean isInInt) {
    return _typeImpl.getFeatureByAdjOffset(adjOffset, isInInt);
  }

  private int _getIntValueCommon(FeatureImpl feat) {
    return _intData[feat.getAdjustedOffset() /* + _getIntDataArrayOffset() */];
  }

  private int _getIntValueCommon(int adjOffset) {
    return _intData[adjOffset /* + _getIntDataArrayOffset() */];
  }

  private Object _getRefValueCommon(FeatureImpl feat) {
    return _refData[feat.getAdjustedOffset() /* + _getRefDataArrayOffset() */];
  }

  public Object _getRefValueCommon(int adjOffset) {
    return _refData[adjOffset /* + _getRefDataArrayOffset() */];
  }

  private void _setIntValueCommon(FeatureImpl fi, int v) {
    _intData[fi.getAdjustedOffset() /* + _getIntDataArrayOffset() */] = v;
    if (traceFSs) {
      _casView.traceFSfeat(this, fi, v);
    }
  }

  private void _setIntValueCommon(int adjOffset, int v) {
    _intData[adjOffset /* + _getIntDataArrayOffset() */] = v;
    if (traceFSs) {
      _casView.traceFSfeat(this, _getFeatFromAdjOffset(adjOffset, true), v);
    }
  }

  private void _setRefValueCommon(FeatureImpl fi, Object v) {
    final int adjOffset = fi.getAdjustedOffset();
    // if (adjOffset >= _refData.length) {
    // System.out.format("Debug feature %s has adjusted Offset: %d but length of refs is %d%n",
    // fi.getName(), adjOffset, _refData.length);
    // System.out.format("Debug domain: %s, highest def type: %s%n",
    // ((TypeImpl)fi.getDomain()).toString(2), fi.getHighestDefiningType());
    // }

    _setRefValueCommon(adjOffset, v);
  }

  public void _setRefValueCommon(int adjOffset, Object v) {
    _refData[adjOffset /* + _getRefDataArrayOffset() */] = v;
    if (traceFSs) {
      _casView.traceFSfeat(this, _getFeatFromAdjOffset(adjOffset, false), v);
    }

  }

  // used also for sofa string setting
  protected void _setRefValueCommonWj(FeatureImpl fi, Object v) {
    _setRefValueCommon(fi, v);
    _casView.maybeLogUpdate(this, fi);
  }

  // private String getTraceRepOfObj(Object v) {
  // if (v instanceof TOP) {
  // TOP fs = (TOP) v;
  // return fs._typeImpl.getShortName() + ':' + fs._id;
  // } else {
  // return (v == null) ? "null" : v.toString();
  // }
  // }

  /*************************************
   * Validation checking
   *************************************/
  private void _Check_feature_defined_for_this_type(Feature feat) {
    if (!(((TypeImpl) (feat.getDomain())).subsumes(_typeImpl))) {
      /* Feature "{0}" is not defined for type "{1}". */
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT, feat.getName(),
              _typeImpl.getName());
    }
  }

  private void _check_feature_range_is_FeatureStructure(Feature feat, FeatureStructureImplC fs) {
    Type range = feat.getRange();
    if (range.isPrimitive()) {
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE_NOT_FS, feat.getName(),
              fs.getType().getName(), feat.getRange().getName());
    }
  }

  // private void featureValidation(Feature feat, Object x) {
  // featureValidation(feat);
  // if (feat.getRange())
  // }

  private void featureValueValidation(Feature feat, Object v) {
    TypeImpl range = (TypeImpl) feat.getRange();
    if ((range.isArray() && !isOkArray(range, v))
            || (!range.isArray() && (!range.subsumesValue(v)))) {
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, feat.getName(),
              range.getName(), (v == null) ? "null" : v.getClass().getName());
    }
  }

  // called when range isArray() is true, only
  private boolean isOkArray(TypeImpl range, Object v) {
    if (v == null) {
      return true;
    }

    final int rangeTypeCode = range.getCode();

    /* The assignment is stricter than the Java rules - must match */
    switch (rangeTypeCode) {
      case TypeSystemConstants.booleanArrayTypeCode:
        return v instanceof BooleanArray;
      case TypeSystemConstants.byteArrayTypeCode:
        return v instanceof ByteArray;
      case TypeSystemConstants.shortArrayTypeCode:
        return v instanceof ShortArray;
      case TypeSystemConstants.intArrayTypeCode:
        return v instanceof IntegerArray;
      case TypeSystemConstants.floatArrayTypeCode:
        return v instanceof FloatArray;
      case TypeSystemConstants.longArrayTypeCode:
        return v instanceof LongArray;
      case TypeSystemConstants.doubleArrayTypeCode:
        return v instanceof DoubleArray;
      case TypeSystemConstants.stringArrayTypeCode:
        return v instanceof StringArray;
      // case TypeSystemConstants.javaObjectArrayTypeCode:
      // return v instanceof JavaObjectArray;
      case TypeSystemConstants.fsArrayTypeCode:
        return v instanceof FSArray;
    }

    // it is possible that the array has a special type code corresponding to a type
    // "someUserType"[]
    // meaning an array of some user type. UIMA implements these as instances of FSArray (I think)

    if (!(v instanceof FSArray)) {
      return false;
    }

    return true;
  }

  private void subStringRangeCheck(Feature feat, String v) {
    Type range = feat.getRange();
    if (range instanceof TypeImpl_stringSubtype) {
      if (v != null) { // null values always OK
        ((TypeImpl_stringSubtype) range).validateIsInAllowedValues(v);
      }
    }
  }

  // protected Object[] _getRefData() {
  // return _refData;
  // }

  /**
   * @param src
   *          the FS to copy features from
   */
  public void _copyIntAndRefArraysFrom(FeatureStructureImplC src) {
    if (src._intData != null && _intData != null) {
      // System.arraycopy(src._intData, src._getIntDataArrayOffset(), _intData,
      // _getIntDataArrayOffset(),
      // Math.min(src._typeImpl.nbrOfUsedIntDataSlots, _typeImpl.nbrOfUsedIntDataSlots));
      System.arraycopy(src._intData, 0, _intData, 0,
              Math.min(src._intData.length, _intData.length));
    }
    if (src._refData != null && _refData != null) {
      // System.arraycopy(src._refData, src._getRefDataArrayOffset(), _refData,
      // _getRefDataArrayOffset(),
      // Math.min(src._typeImpl.nbrOfUsedRefDataSlots, _typeImpl.nbrOfUsedRefDataSlots));
      System.arraycopy(src._refData, 0, _refData, 0,
              Math.min(src._refData.length, _refData.length));
    }
  }

  /**
   * copy int and ref data for two instances, each having the exact same type
   * 
   * @param src
   *          the FS to copy features from
   */
  public void _copyIntAndRefArraysEqTypesFrom(FeatureStructureImplC src) {
    if (_intData != null) {
      // System.arraycopy(src._intData, src._getIntDataArrayOffset(), _intData,
      // _getIntDataArrayOffset(), _typeImpl.nbrOfUsedIntDataSlots);
      System.arraycopy(src._intData, 0, _intData, 0, _typeImpl.nbrOfUsedIntDataSlots);
    }
    if (_refData != null) {
      // System.arraycopy(src._refData, src._getRefDataArrayOffset(), _refData,
      // _getRefDataArrayOffset(), _typeImpl.nbrOfUsedRefDataSlots);
      System.arraycopy(src._refData, 0, _refData, 0, _typeImpl.nbrOfUsedRefDataSlots);
    }
  }

  /**
   * @param src
   *          the FS to copy features from
   */
  public void _copyIntArrayEqTypesFrom(FeatureStructureImplC src) {
    if (_intData != null) {
      System.arraycopy(src._intData, 0, _intData, 0, _intData.length);
    }
  }

  public String toShortString() {
    return new StringBuilder(_typeImpl.getShortName()).append(':').append(_id).toString();
  }

  // private int _getIntDataArrayOffset() {
  // return (_flags & bitMaskIntOffset) >> shiftIntOffset;
  // }
  //
  // private void _setIntDataArrayOffset(int v) {
  // _flags = (_flags & ~bitMaskIntOffset) | v << shiftIntOffset;
  // }
  //
  // private int _getRefDataArrayOffset() {
  // return _flags >> shiftRefOffset;
  // }
  //
  // private void _setRefDataArrayOffset(int v) {
  // _flags = (_flags & ~bitMaskRefOffset) | v << shiftRefOffset;
  // }

  public final TypeImpl _getTypeImpl() {
    return _typeImpl;
  }

  protected final void _setTypeImpl(TypeImpl ti) {
    _typeImpl = ti;
  }

  public static int compare(FeatureStructureImplC a, FeatureStructureImplC b) {
    return Integer.compare(a._id, b._id);
  }

  protected final static int wrapGetIntCatchException(MethodHandle mh) {
    try {
      return (int) mh.invokeExact();
    } catch (Throwable t) {
      throw new UIMA_IllegalStateException(UIMA_IllegalStateException.JCAS_NO_TYPE, null, t);
    }
  }

}
