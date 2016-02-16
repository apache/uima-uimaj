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
package org.apache.uima.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.internal.util.Int2ObjListMap;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.jcas.cas.CommonArray;
import org.apache.uima.jcas.cas.CommonPrimitiveArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Utility class for doing deep copies of FeatureStructures from one CAS to another. To handle cases
 * where the source CAS has multiple references to the same FS, you can create one instance of
 * CasCopier and use it to copy multiple FeatureStructures. The CasCopier will remember previously
 * copied FeatureStructures, so if you later copy another FS that has a reference to a previously
 * copied FS, it will not duplicate the multiply-referenced FS.
 * 
 * This class makes use of CASImpl methods, but is only passed CAS objects, which may be 
 * CAS Wrappers.  To make this more feasible, the implementors of CAS Wrappers need to implement
 * the method  getLowLevelCas() which should return a reference to the underlying CAS which can be 
 * successfully cast to a CASImpl.
 * 
 * The source and target CASs must be separate CASs (that is, not two views of the same CAS), with
 * one exception:
 * 
 *   If the CopyCasView API is being used, and the target View name is different from the source view name,
 *   
 * 
 */
public class CasCopier {
  
  private static final TypeImpl MISSING_TYPE = TypeImpl.singleton;
  private static final FeatureImpl MISSING_FEAT = FeatureImpl.singleton;
  
//  private static final int FRC_SKIP = 0;  // is the default, must be 0
//  private static final int FRC_STRING = 1;
//  private static final int FRC_LONG = 2;
//  private static final int FRC_DOUBLE = 3;
//  private static final int FRC_INT_LIKE = 4;
//  private static final int FRC_REF = 5;
//  
//  private static final int K_SRC_FEAT_OFFSET = 0;
//  private static final int K_TGT_FEAT_CODE = 1;
  
//  /**
//   * TypeInfo stores the mapping from the source to the target type system
//   * It is set up once at the start, to avoid looking up this correspondence (by name) repeatedly 
//   */
//  private class TypeInfo {
//    final int[] codesAndOffsets;  // indexed with count * 2
//    /**
//     * Feature Range class:  String, Long, double, int-like, ref, or skip for all others
//     */
//    final byte[] frc;
//    final int tgtTypeCode;
//        
//    TypeInfo(int srcTypeCode) {    
//
//      if (tgtTsi == srcTsi) {
//        tgtTypeCode = srcTypeCode;
//      } else {
//        Type srcType = srcTsi.ll_getTypeForCode(srcTypeCode);
//        Type tgtType = tgtTsi.getType(srcType.getName());
//        if (tgtType == null) {
//          // If in lenient mode, do not act on this FS. Instead just
//          // return (null) to the caller and let the caller deal with this case.
//          if (lenient) {
//            tgtTypeCode = 0;
//          } else {
//            throw new UIMARuntimeException(UIMARuntimeException.TYPE_NOT_FOUND_DURING_CAS_COPY,
//                new Object[] { srcType.getName() });
//          }
//        } else {
//          tgtTypeCode = tgtTsi.ll_getCodeForType(tgtType);
//        }
//      }
//                  
//      int[] srcFeatCodes = srcTsi.ll_getAppropriateFeatures(srcTypeCode);
//      int arrayLength = srcFeatCodes.length << 1;
//      
//      codesAndOffsets = new int[arrayLength];
//      frc = new byte[srcFeatCodes.length];
//
//      if (srcTsi == tgtTsi) {
//        for (int i = 0; i < srcFeatCodes.length; i++) {
//          final int srcFeatCode = srcFeatCodes[i];
//          Feature srcFeat = srcTsi.ll_getFeatureForCode(srcFeatCode);
//          setRangeClass((TypeImpl) srcFeat.getRange(), i);
//          final int i2 = i << 1;
//          codesAndOffsets[i2 + K_SRC_FEAT_OFFSET] = originalSrcCasImpl.getFeatureOffset(srcFeatCode);
//          codesAndOffsets[i2 + K_TGT_FEAT_CODE] = srcFeatCodes[i];
//        }
//      } else {        
//        for (int i = 0; i < srcFeatCodes.length; i++) { 
//         final int srcFeatCode = srcFeatCodes[i];
//         Feature srcFeat = srcTsi.ll_getFeatureForCode(srcFeatCode);
//          String srcFeatName = srcFeat.getName();
//          Feature tgtFeat = tgtTsi.getFeatureByFullName(srcFeatName);
//          if (tgtFeat == null) {
//            // If in lenient mode, ignore this feature and move on to the next
//            // feature in this FS (if one exists)
//            if (lenient) {
//              continue; // Ignore this feature in the source CAS since it doesn't exist in
//                        // in the target CAS.
//            } else {
//              throw new UIMARuntimeException(UIMARuntimeException.FEATURE_NOT_FOUND_DURING_CAS_COPY,
//                  new Object[] { srcFeatName });
//            }
//          } else {
//            final int i2 = i << 1;
//            int tgtFeatCode = ((FeatureImpl)tgtFeat).getCode();
//            codesAndOffsets[i2 + K_SRC_FEAT_OFFSET] = originalSrcCasImpl.getFeatureOffset(srcFeatCode);
//            codesAndOffsets[i2 + K_TGT_FEAT_CODE] = tgtFeatCode;
//          }
//
//          TypeImpl srcRangeType = (TypeImpl) srcFeat.getRange();
//          
//          // verify range types of features have the same name
//          if (!srcRangeType.getName().equals(
//               tgtFeat.getRange().getName())) {
//            throw new UIMARuntimeException(UIMARuntimeException.COPY_CAS_RANGE_TYPE_NAMES_NOT_EQUAL, 
//                new Object[] {srcFeatName, srcFeat.getRange().getName(), tgtFeat.getRange().getName()});
//          }
//          
//          setRangeClass(srcRangeType, i);
//        }
//      }        
//    }
//    
//    void setRangeClass(TypeImpl srcRangeType, int i) {
//      if (srcTsi.ll_subsumes(srcStringTypeCode, srcRangeType.getCode())) {
//        frc[i] = FRC_STRING;
//      } else if (srcRangeType == srcTsi.intType || 
//          srcRangeType == srcTsi.floatType ||
//          srcRangeType == srcTsi.booleanType ||
//          srcRangeType == srcTsi.byteType ||
//          srcRangeType == srcTsi.shortType) {
//        frc[i] = FRC_INT_LIKE;
//      } else if (srcRangeType == srcTsi.longType) {
//        frc[i] = FRC_LONG;
//      } else if (srcRangeType == srcTsi.doubleType) {
//        frc[i] = FRC_DOUBLE;
//      } else {
//        frc[i] = FRC_REF;
//      }
//    }
//  }
//    
//  private final TypeInfo[] tInfoArray;
  
  // these next are called original, as they are the views used to create the CasCopier instance
  private final CAS originalSrcCas;
  private final CAS originalTgtCas;
  // these next are the CASImpls of these
  private final CASImpl originalSrcCasImpl;
  private final CASImpl originalTgtCasImpl;
  
  // these next 2 are like the above, but for explicit view copying
  
  private CASImpl srcCasViewImpl;
  private CASImpl tgtCasViewImpl;
  
  private String srcViewName;  // these are used when the view name is changed
  private String tgtViewName;  // this is the corresponding target view name for the source view name
  
  private final TypeSystemImpl srcTsi;
  private final TypeSystemImpl tgtTsi;
  
  private final Int2ObjListMap<TypeImpl>    src2TgtType;  
  private final Int2ObjListMap<FeatureImpl> src2TgtFeat;
  
//  
//  private final TypeImpl srcStringType;
  
  /**
   * true if the copyCasView api was used, and the target view name corresponding to the source view name is changed
   */
  private boolean isChangeViewName = false;
  
  private Annotation srcCasDocumentAnnotation = null;
//  /**
//   * The source view name - may be null if the view is of the base CAS
//   */
//  private String mSrcCasViewName;
//  /**
//   * The target view name - not used unless doing a view copy 
//   * Allows copying a view to another CAS under a different name
//   */
//  private String mTgtCasViewName;
  
//  final private Feature mDestSofaFeature;
  
  final private boolean lenient; //true: ignore feature structures and features that are not defined in the destination CAS

  /**
   * key is source FS, value is target FS 
   * Target not set for DocumentAnnotation or SofaFSs
   * Target not set if lenient specified and src type isn't in target
   */
  final private Map<TOP, TOP> mFsMap;
  
  /**
   * Deferred calls to copy Features of a FS
   */
  final private Deque<Runnable> fsToDo = new ArrayDeque<>();


  /**
   * Creates a new CasCopier that can be used to copy FeatureStructures from one CAS to another.
   * Note that if you are merging data from multiple CASes, you must create a new CasCopier
   * for each source CAS.
   * 
   * Note: If the feature structure and/or feature is not defined in the type system of
   *       the destination CAS, the copy will fail (in other words, the lenient setting is false,
   *       by default).
   *       
   * @param aSrcCas
   *          the CAS to copy from.
   * @param aDestCas
   *          the CAS to copy into.
   */
  public CasCopier(CAS aSrcCas, CAS aDestCas) {
    this(aSrcCas, aDestCas, false);
  }

  /**
   * Creates a new CasCopier that can be used to copy FeatureStructures from one CAS to another.
   * Note that if you are merging data from multiple CASes, you must create a new CasCopier
   * for each source CAS. This version of the constructor supports a "lenient copy" option. When set,
   * the CAS copy function will ignore (not attempt to copy) FSs and features not defined in the type system
   * of the destination CAS, rather than throwing an exception.
   * 
   * @param aSrcCas
   *          the CAS to copy from.
   * @param aDestCas
   *          the CAS to copy into.
   * @param lenient
   *          ignore FSs and features not defined in the type system of the destination CAS
   */
  public CasCopier(CAS aSrcCas, CAS aDestCas, boolean lenient) {

    mFsMap = new IdentityHashMap<>(((CASImpl)(aSrcCas.getLowLevelCAS())).getLastUsedFsId());
    originalSrcCas = aSrcCas;
    originalTgtCas = aDestCas;
    
    originalSrcCasImpl = (CASImpl) aSrcCas.getLowLevelCAS(); 
    originalTgtCasImpl = (CASImpl) aDestCas.getLowLevelCAS(); 
    
    srcTsi = originalSrcCasImpl.getTypeSystemImpl();
    tgtTsi = originalTgtCasImpl.getTypeSystemImpl();

    src2TgtType = (srcTsi == tgtTsi) ? null : new Int2ObjListMap<TypeImpl>(srcTsi.getTypeArraySize());
    src2TgtFeat = (srcTsi == tgtTsi) ? null : new Int2ObjListMap<FeatureImpl>(srcTsi.getNumberOfFeatures() + 1);
    

//    tInfoArray = new TypeInfo[srcTsi.getLargestTypeCode() + 1];
    
//    srcStringType = srcTsi.stringType;
//    srcStringTypeCode = srcStringType.getCode();
    
//    mDestSofaFeature = aDestCas.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFA);
    this.lenient = lenient;
    
    // the next is to support the style of use where
    //   an instance of this copier is made, corresponding to two views in the same CAS
    //   or corresponding to two views in different CASs
    //   and then individual FeatureStructures are copied using copyFS(...)
    
    srcCasViewImpl = (CASImpl) originalSrcCas.getLowLevelCAS();
    tgtCasViewImpl = (CASImpl) originalTgtCas.getLowLevelCAS();
    
    srcViewName = srcCasViewImpl.getViewName();
    tgtViewName = tgtCasViewImpl.getViewName();
    
    if (srcViewName == null) {
      isChangeViewName = (tgtViewName == null) ? false : true;
    } else {
      isChangeViewName = !srcViewName.equals(tgtViewName);
    }
  }
  

  
  /**
   * Does a complete deep copy of one CAS into another CAS.  The contents of each view
   * in the source CAS will be copied to the same-named view in the destination CAS.  If
   * the view does not already exist it will be created.  All FeatureStructures that are
   * indexed in a view in the source CAS will become indexed in the same-named view in the
   * destination CAS.
   * 
   * Note: If the feature structure and/or feature is not defined in the type system of
   *       the destination CAS, the copy will fail (in other words, the lenient setting is false,
   *       by default).
   *
   * @param aSrcCas
   *          the CAS to copy from
   * @param aDestCas
   *          the CAS to copy to
   * @param aCopySofa
   *          if true, the sofa data and mimeType of each view will be copied. If false they will not.
   */  
  public static void copyCas(CAS aSrcCas, CAS aDestCas, boolean aCopySofa) {
   copyCas(aSrcCas, aDestCas, aCopySofa, false);
  }

  /**
   * Does a complete deep copy of one CAS into another CAS.  The contents of each view
   * in the source CAS will be copied to the same-named view in the destination CAS.  If
   * the view does not already exist it will be created.  All FeatureStructures that are
   * indexed in a view in the source CAS will become indexed in the same-named view in the
   * destination CAS. This version of the method supports a "lenient copy" option. When set,
   * the CAS copy function will ignore (not attempt to copy) FSs and features not defined in the type system
   * of the destination CAS, rather than throwing an exception.
   *
   * @param aSrcCas
   *          the CAS to copy from
   * @param aDestCas
   *          the CAS to copy to; must be a completely different CAS than the source (that is, not an alternative "view" of the source)
   * @param aCopySofa
   *          if true, the sofa data and mimeType of each view will be copied. If false they will not.
   * @param lenient
   *          ignore FSs and features not defined in the type system of the destination CAS
   */
  public static void copyCas(CAS aSrcCas, CAS aDestCas, boolean aCopySofa, boolean lenient) {
    CasCopier copier = new CasCopier(aSrcCas, aDestCas, lenient);
    
    // oops, this misses the initial view if a sofa FS has not yet been created
//    Iterator<SofaFS> sofaIter = aSrcCas.getSofaIterator();
//    while (sofaIter.hasNext()) {
//      SofaFS sofa = sofaIter.next();
//      CAS view = aSrcCas.getView(sofa);
//      copier.copyCasView(view, aCopySofa);
//    }
    
    if (copier.originalSrcCasImpl.getBaseCAS() == copier.originalTgtCasImpl.getBaseCAS()) {
      throw new UIMARuntimeException(UIMARuntimeException.ILLEGAL_CAS_COPY_TO_SAME_CAS);
    }
    
    Iterator<CAS> viewIterator = aSrcCas.getViewIterator();
    while (viewIterator.hasNext()) {
      CAS view = viewIterator.next();
      copier.copyCasView(view, aCopySofa); 

    }
  }

  /**
   * Does a deep copy of the contents of one CAS View into another CAS's same-named-view
   * If the destination view already exists in the destination CAS,
   * then it will be the target of the copy.  Otherwise, a new view will be created with
   * that name and will become the target of the copy.  All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.
   * 
   * @param aSrcCasView the CAS to copy from.  This must be a view in the src Cas set by the constructor
   * @param aCopySofa if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(CAS aSrcCasView, boolean aCopySofa) {
    copyCasViewDifferentCASs(aSrcCasView, getOrCreateView(originalTgtCas, aSrcCasView.getViewName()), aCopySofa);
  }
  
  /**
   * Does a deep copy of the contents of one CAS View into another CAS's same-named-view
   * If the destination view already exists in the destination CAS,
   * then it will be the target of the copy.  Otherwise, a new view will be created with
   * that name and will become the target of the copy.  All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.  Any views created because
   * of cross-view references will have the same view name as in the source.
   *
   * @param aSrcCasViewName the name of the view in the source CAS to copy from
   * @param aCopySofa if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(String aSrcCasViewName, boolean aCopySofa) {
    copyCasView(getOrCreateView(originalSrcCas, aSrcCasViewName), aCopySofa);
  }
  
  /**
   * Does a deep copy of the contents of one CAS View into another CAS view,
   * with a possibly different name.
   * If the destination view already exists in the destination CAS,
   * then it will be the target of the copy.  Otherwise, a new view will be created with
   * that name and will become the target of the copy.  
   * All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.  Any views created because
   * of cross-view references will have the same view name as in the source.
   * 
   * @param aSrcCasView The view in the source to copy from
   * @param aTgtCasViewName The name of the view in the destination CAS to copy into
   * @param aCopySofa if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(CAS aSrcCasView, String aTgtCasViewName, boolean aCopySofa) {
    copyCasView(aSrcCasView, getOrCreateView(originalTgtCas, aTgtCasViewName), aCopySofa);
  }

  /**
   * Does a deep copy of the contents of one CAS View into another CAS view,
   * with a possibly different name.
   * All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.  Any views created because
   * of cross-view references will have the same view name as in the source.
   * 
   * @param aSrcCasViewName The name of the view in the Source CAS to copy from
   * @param aTgtCasView The view in the destination CAS to copy into
   * @param aCopySofa if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(String aSrcCasViewName, CAS aTgtCasView, boolean aCopySofa) {
    copyCasView(getOrCreateView(originalSrcCas, aSrcCasViewName), aTgtCasView, aCopySofa);
  }

  private void copyCasViewDifferentCASs(CAS aSrcCasView, CAS aTgtCasView, boolean aCopySofa) {
    if (originalSrcCasImpl.getBaseCAS() == originalTgtCasImpl.getBaseCAS()) {
      throw new UIMARuntimeException(UIMARuntimeException.ILLEGAL_CAS_COPY_TO_SAME_CAS);
    }

    copyCasView(aSrcCasView, aTgtCasView, aCopySofa);
  }
  
  /**
   * Does a deep copy of the contents of one CAS View into another CAS view,
   * with a possibly different name.
   * All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.  Any views created because
   * of cross-view references will have the same view name as in the source.
   * 
   * If the source and target views are both views of the same CAS, then Feature Structures
   * in the view are effectively "cloned", with the following change:
   *   Subtypes of AnnotationBase in the source whose sofaRef is for the source View are
   *   cloned with their sofaRefs changed to the new targetView. 
   * 
   * @param aSrcCasView
   *          the CAS view to copy from. This must be a view of the srcCas set in the constructor
   * @param aTgtCasView 
   *          the CAS view to copy to. This must be a view of the tgtCas set in the constructor
   * @param aCopySofa
   *          if true, the sofa data and mimeType will be copied. If false they will not.  
   *          If true and the sofa data is already set in the target, will throw CASRuntimeException        
   */
  public void copyCasView(CAS aSrcCasView, CAS aTgtCasView, boolean aCopySofa) {
        
    if (!casViewsInSameCas(aSrcCasView, originalSrcCas)) {
      throw new UIMARuntimeException(UIMARuntimeException.VIEW_NOT_PART_OF_CAS, new Object[] {"Source"});
    }
    if (!casViewsInSameCas(aTgtCasView, originalTgtCas)) {
      throw new UIMARuntimeException(UIMARuntimeException.VIEW_NOT_PART_OF_CAS, new Object[] {"Destination"});
    }
    
//    mSrcCasViewName = aSrcCasView.getViewName(); 
//    mTgtCasViewName = aTgtCasView.getViewName();

    srcCasViewImpl = (CASImpl) aSrcCasView.getLowLevelCAS();
    tgtCasViewImpl = (CASImpl) aTgtCasView.getLowLevelCAS();
    
    srcViewName = srcCasViewImpl.getViewName();
    tgtViewName = tgtCasViewImpl.getViewName();
    isChangeViewName = !srcViewName.equals(tgtViewName);

    if ((aSrcCasView == srcCasViewImpl.getBaseCAS()) || (aTgtCasView == tgtCasViewImpl.getBaseCAS())) {
      throw new UIMARuntimeException(UIMARuntimeException.UNSUPPORTED_CAS_COPY_TO_OR_FROM_BASE_CAS);
    }
    
    srcCasDocumentAnnotation = null;  // each view needs to get this once
    
//    mLowLevelDestCas = aTgtCasView.getLowLevelCAS();
//    mLowLevelSrcCas = aSrcCasView.getLowLevelCAS();
    
    // The top level sofa associated with this view is copied (or not)
    
    if (aCopySofa) {
      // can't copy the SofaFS - just copy the sofa data and mime type
      SofaFS sofa = srcCasViewImpl.getSofa();
      if (null != sofa) {
        // if the sofa doesn't exist in the target, these calls will create it
        //  (view can exist without Sofa, at least for the initial view)
        String sofaMime = sofa.getSofaMime();
        String docTxt = srcCasViewImpl.getDocumentText();
        if (docTxt != null) {
          aTgtCasView.setSofaDataString(docTxt, sofaMime);
        } else {
          String sofaDataURI = srcCasViewImpl.getSofaDataURI();
          if (sofaDataURI != null) {
            aTgtCasView.setSofaDataURI(sofaDataURI, sofaMime);
          } else {
            TOP sofaDataArray = (TOP) srcCasViewImpl.getSofaDataArray();
            if (sofaDataArray != null) {
              aTgtCasView.setSofaDataArray(copyFs2Fs(sofaDataArray), sofaMime);
            }
          }
        }  
        
      }
    }

    // now copy indexed FS, but keep track so we don't index anything more than once
    //   Note: mFsMap might be used for this, but it doesn't index several kinds of FSs
    //         see the javadoc for this field for details
    // NOTE: FeatureStructure hashcode / equals use the int "address" of the FS in the heap.
    
    final PositiveIntSet indexedFs = new PositiveIntSet_impl();
    
    // The indexFs set starts out "cleared", but 
    // we don't clear the cas copier instance map "mFsMap" here, in order to skip actually copying the
    //   FSs when doing a full CAS copy with multiple views - the 2nd and subsequent
    //   views don't copy, but they do index.
    
    FSIterator<TOP> it = srcCasViewImpl.getIndexRepository().getAllIndexedFS(srcTsi.getTopType());
//    LowLevelIterator it = ((FSIndexRepositoryImpl)(srcCasViewImpl.getIndexRepository())).ll_getAllIndexedFS(srcTsi.getTopType());

    while (it.hasNext()) {
      final TOP fs = it.nextNvc();
//      System.out.format("debug  id: %,d  type: %s%n", fs.id(), fs._typeImpl.getShortName());
//    Iterator<LowLevelIndex> indexes = srcCasViewImpl.getIndexRepository().ll_getIndexes();
//    while (indexes.hasNext()) {
//      LowLevelIndex index = indexes.next();
//      LowLevelIterator iter = index.ll_iterator();
//      while (iter.isValid()) {
//        final int fs = iter.ll_get();
//        iter.moveToNext();
      if (!indexedFs.contains(fs.id())) {
        final TOP copyOfFs = copyFs2(fs);
        // If the lenient option is used, it's possible that no FS was
        // created (e.g., FS is not defined in the target CAS. So ignore
        // this FS in the source CAS and move on to the next FS.
        if (lenient && copyOfFs == null) {
          continue; // Move to the next FS in the source CAS
        }
        // otherwise, won't be null (error thrown instead)

        // check for annotations with null Sofa reference - this can happen
        // if the annotations were created with the Low Level CAS API. If the
        // Sofa reference isn't set, attempting to add the FS to the indexes
        // will fail.
//        if (fs instanceof AnnotationBase) {
//          AnnotationBase fsAb = (AnnotationBase) fs;
//          int sofaRef = tgtCasViewImpl.ll_getRefValue(copyOfFs, mDestSofaFeatureCode);
//          if (0 == sofaRef) {
//            tgtCasViewImpl.ll_setRefValue(copyOfFs, mDestSofaFeatureCode, tgtCasViewImpl.getSofaRef());
//          }
//        }

        // also don't index the DocumentAnnotation (it's indexed by default)
        if (!isDocumentAnnotation(fs)) {
          tgtCasViewImpl.getIndexRepository().addFS(copyOfFs);
        }
        indexedFs.add(fs.id());
      }
    }
  }

  /**
   * For long lists, and other structures, the straight-forward impl with recursion can
   * nest too deep, causing a Java failure - out of stack space.
   * 
   * This is a non-recursive impl, making use of an aux object: featureStructuresWithSlotsToSet to
   * hold copied FSs whose slots need to be scanned and set with values.
   * 
   * The main loop dequeues one element, and copies the features.
   * 
   * The copying of a FS copies the FS without setting the slots; instead it queues the
   * copied FS together with its source instance on featureStructuresWithSlotsToSet 
   * for later processing.
   * 
   */
  
  /**
   * Copy 1 feature structure from the originalSrcCas to a new Cas.  No indexing of the new FS is done.
   * If the FS has been copied previously (using this CasCopier instance) the 
   * same identical copy will be returned rather than making another copy.
   * 
   * View handling: ignores the view of the targetCas
   * 
   * @param aFS the Feature Structure to copy
   * @return a deep copy of the Feature Structure - any referred to FSs will also be copied.
   */
  
  public <T extends FeatureStructure> T copyFs(T aFS) {
    if (null == srcCasViewImpl) {
      srcCasViewImpl = originalSrcCasImpl;
    }
    if (null == tgtCasViewImpl) {
      tgtCasViewImpl = originalTgtCasImpl;
    }
    
    // safety - insure DocumentAnnotation is tested.
    srcCasDocumentAnnotation = null;  
    return copyFs2Fs(aFS);
  }
  
  /**
   * Copy one FS from the src CAS to the tgt CAS
   *   View context:
   *     The caller must set the srcCasViewImpl and the tgtCasViewImpl
   *     
   * @param aFS a Feature Structure reference in the originalSrcCas
   * @return a Feature Structure reference in the originalTgtCas
   */
  private <T extends FeatureStructure> T copyFs2(T aFS) {
    
    T copy = copyFsInner(aFS);  // doesn't copy the slot values, but enqueues them
    // the iteration is done this way because the body can add more to the queue
    while (fsToDo.size() > 0) {
      Runnable r = fsToDo.removeFirst();
      r.run();
    }
    return copy;
  }
  
  private <T extends FeatureStructure> T copyFs2Fs(T fs) {
    return copyFs2(fs);
  }

  /**
   * Copies a FS from the source CAS to the destination CAS. Also copies any referenced FSs, except
   * that previously copied FS will not be copied again.
   * 
   * @param srcFs
   *          the FS to copy. Must be contained within the source CAS.
   * @return the copy of <code>aFS</code> in the target CAS.
   */
  private <T extends FeatureStructure> T copyFsInner(T srcFs) {
    // FS must be in the source CAS
    // this test must be done by the caller if wanted.
//    assert (casViewsInSameCas(aFS.getCAS(), originalSrcCas));

    // check if we already copied this FS
    T copy = (T) mFsMap.get(srcFs);
    if (copy != null) {
      return copy;
    }
    
    // Certain types need to be handled specially

    // Sofa - cannot be created by normal methods. Instead, we return the Sofa with the
    // same Sofa ID in the target CAS. If it does not exist it will be created.
    if (srcFs instanceof Sofa) {
      Sofa srcSofa = (Sofa) srcFs;
      String destSofaId = getDestSofaId(srcSofa.getSofaID());
      // note: not put into the mFsMap, because each view needs a separate copy
      return (T) ((CASImpl)getOrCreateView(originalTgtCas, destSofaId)).getSofa();
    }

    // DocumentAnnotation - instead of creating a new instance, reuse the automatically created
    // instance in the destination view.
    if (isDocumentAnnotation(srcFs)) {
      Annotation da = (Annotation) srcFs;
      String destViewName = getDestSofaId(da.getView().getViewName());

      // the DocumentAnnotation could be indexed in a different view than the one being copied
      //   if it was ref'd for the 1st time from a cross-indexed fs
      // Note: The view might not exist in the target
      //   but this is unlikely.  To have this case this would require
      //   indexing some other feature structure in this view, which, in turn,
      //   has a reference to the DocumentAnnotation FS belonging to another view
      CASImpl destView = (CASImpl) getOrCreateView(originalTgtCas, destViewName);
      // do the no-create style so we can create it without adding it to the index yet
      Annotation destDocAnnot = destView.getDocumentAnnotationNoCreate();  
      if (destDocAnnot == null) {
        destDocAnnot = destView.createDocumentAnnotationNoRemoveNoIndex(0);
        copyFeatures(srcFs, destDocAnnot);
        destView.getIndexRepository().addFS(destDocAnnot);
      } else {
        AutoCloseable ac = tgtCasViewImpl.protectIndexes();
        try {
          copyFeatures(srcFs, destDocAnnot);
        } finally {
          try {
            ac.close();
          } catch (Exception e) {
          }
        }
      }
      // note note put into mFsMap, because each view needs a separate copy
      return (T) destDocAnnot;
    }

    // Arrays - need to be created a populated differently than "normal" FS
    if (srcFs instanceof CommonArray) {
      copy = copyArray(srcFs);
      mFsMap.put((TOP)srcFs, (TOP)copy);
      return copy;
    }

    TypeImpl tgtTi = getTargetType(((TOP)srcFs)._typeImpl);
    if (null == tgtTi) {
      return null; // not in target, no FS to create
    }
//    final TypeInfo tInfo = getTypeInfo(srcTypeCode);
//    final int tgtTypeCode = tInfo.tgtTypeCode;
//    if (tgtTypeCode == 0) {
//      return 0; // not in target, no FS to create
//    }
    // We need to use the LowLevel CAS interface to create the FS, because the usual
    // CAS.createFS() call doesn't allow us to create subtypes of AnnotationBase from
    // a base CAS. In any case we don't need the Sofa reference to be automatically
    // set because we'll set it manually when in the copyFeatures method.
    
    TOP tgtFs = tgtCasViewImpl.createFS(tgtTi);

    // add to map so we don't try to copy this more than once
    mFsMap.put((TOP)srcFs, tgtFs);

    fsToDo.addLast(() -> copyFeatures(srcFs, tgtFs));
    return (T) tgtFs;
  }
  
  /**
   * There are two cases for getting target sofa name from the source one, depending on whether or not
   * the API which allows specifying a different target view name for the source view name, is in use.
   * 
   * If so, then whenever the source sofa name is that src view name, replace it in the target with the 
   * specified different target view name.
   *     
   * @param id
   * @return id unless the id matches the source view name, and that name is being changed
   */
  private String getDestSofaId(String id) {
    return (isChangeViewName && id.equals(srcViewName)) ? tgtViewName : id;
  }
  
//  private TypeInfo getTypeInfo(int srcTypeCode) {
//    TypeInfo tInfo = tInfoArray[srcTypeCode];
//    if (tInfo == null) {
//      return tInfoArray[srcTypeCode] = new TypeInfo(srcTypeCode);
//    }
//    return tInfo;
//  }
  
  /**
   * Copy feature values from one FS to another. For reference-valued features, this does a deep
   * copy.
   * 
   * @param srcFS
   *          FeatureStructure to copy from
   * @param tgtFS
   *          FeatureStructure to copy to, which must not be in the index (index corruption checks skipped)
   */
  private <T extends FeatureStructure> void copyFeatures(T srcFSi, T tgtFSi) {
    TOP srcFS = (TOP) srcFSi;
    TOP tgtFS = (TOP) tgtFSi;
    for (FeatureImpl fi : srcFS.getTypeImpl().getFeatureImpls()) {
      FeatureImpl tgtFi = getTargetFeature(fi);
      if (null == tgtFi) {
        continue;  // skip copying features not in the target type system
      }
      
      if (!CASImpl.copyFeatureExceptFsRef(srcFS, fi, tgtFS, tgtFi)) {
        // feature is a reference to another FS, so enqueue that to copy
        //   unless it's the sofa feature for AnnotationBase - that's feature final, set when created
        if (fi.isAnnotBaseSofaRef) {
          continue;
        }
        TOP refFs = srcFS.getFeatureValueNc(fi);
        if (null != refFs) {
          tgtFS.setFeatureValueNcNj(tgtFi, copyFsInner(refFs));  // recursive call
        }
      }
    }
  }    
//    
//    // set feature values
//    
////    final int srcTypeCode = srcCasViewImpl.getTypeCode(srcFS);
//    
////    final TypeInfo tInfo = getTypeInfo(srcTypeCode);
//        
//    tgtCasViewImpl.setCacheNotInIndex(tgtFS);
//        
//    for (int i = 0; i < tInfo.codesAndOffsets.length; i = i + 2) {
//      final int tgtFeatCode = tInfo.codesAndOffsets[i + K_TGT_FEAT_CODE];
//      if (0 == tgtFeatCode) {
//        continue; 
//      }
//      final int srcFeatOffset = tInfo.codesAndOffsets[i + K_SRC_FEAT_OFFSET];
//      switch (tInfo.frc[i >> 1]) {
//      case FRC_SKIP:
//        break;
//      case FRC_STRING:
//        // need feature code to check subtype constraints
//        tgtCasViewImpl.ll_setStringValue(tgtFS, tgtFeatCode, srcCasViewImpl.ll_getStringValueFeatOffset(srcFS, srcFeatOffset));
//        break;
//      case FRC_INT_LIKE:
//        tgtCasViewImpl.ll_setIntValue(tgtFS, tgtFeatCode, srcCasViewImpl.ll_getIntValueFeatOffset(srcFS, srcFeatOffset));
//        break;
//      case FRC_LONG:
//        tgtCasViewImpl.ll_setLongValue(tgtFS,  tgtFeatCode,  srcCasViewImpl.ll_getLongValueFeatOffset(srcFS,  srcFeatOffset));
//        break;
//      case FRC_DOUBLE:
//        tgtCasViewImpl.ll_setDoubleValue(tgtFS,  tgtFeatCode,  srcCasViewImpl.ll_getDoubleValueFeatOffset(srcFS,  srcFeatOffset));
//        break;
//      case FRC_REF:
//        int refFS = srcCasViewImpl.ll_getRefValueFeatOffset(srcFS, srcFeatOffset);
//        if (refFS != 0) {
//          int copyRefFs = copyFsInner(refFS);
//          tgtCasViewImpl.ll_setRefValue(tgtFS, tgtFeatCode, copyRefFs);
//        }
//        break;
//      default:
//        throw new UIMARuntimeException();  // internal error
//      }      
//    }
//  }
  
  /**
   * Note: if lenient is in effect, this method will return false for
   * FSs which are not copied because the target doesn't have that type.
   * It also returns false for sofa FSs and the documentAnnotation FS.
   * @param aFS a feature structure
   * @return true if the given FS has already been copied using this CasCopier.
   */
  public boolean alreadyCopied(FeatureStructure aFS) {
    return alreadyCopied(((TOP)aFS));
  }
  
  /**
   * Note: if lenient is in effect, this method will return false for
   * FSs which are not copied because the target doesn't have that type.
   * It also returns false for sofa FSs and the documentAnnotation FS.
   * @param aFS a feature structure
   * @return true if the given FS has already been copied using this CasCopier.
   */
  public boolean alreadyCopied(TOP aFS) {
    return mFsMap.get(aFS) != null;
  }

  /**
   * @param arrayFS
   * @return a copy of the array
   */
  private <T extends FeatureStructure> T copyArray(T srcFS) {
    final CommonArray srcCA = (CommonArray) srcFS;
    final int size = srcCA.size();
    final TypeImpl tgtTi = getTargetType(((TOP)srcFS)._typeImpl);
    
    if (srcFS instanceof CommonPrimitiveArray) {
      CommonArray copy = (CommonArray) tgtCasViewImpl.createArray(tgtTi, size);
      copy.copyValuesFrom(srcCA);
      return (T) copy;
    }
    
    FSArray fsArray = (FSArray) tgtCasViewImpl.createArray(tgtTi, size);

    int i = 0;
    TOP[] tgtArray = fsArray._getTheArray();
    
    for (TOP srcItem : ((FSArray)srcFS)._getTheArray()) {
      if (null != srcItem) {
        tgtArray[i] = copyFsInner(srcItem);
      }
      i++;
    }

    return (T) fsArray;
  }
  
  /**
   * Gets the named view; if the view doesn't exist it will be created.
   */
  private static CASImpl getOrCreateView(CAS aCas, String aViewName) {
    //TODO: there should be some way to do this without the try...catch
    try { // throws if view doesn't exist
      return (CASImpl) aCas.getView(aViewName).getLowLevelCAS(); 
    }
    catch(CASRuntimeException e) {
      //create the view
      return (CASImpl) aCas.createView(aViewName).getLowLevelCAS(); 
    }
  }  
  
  /**
   * Determines whether the given FS is the DocumentAnnotation in the srcCasView.  
   * This is more than just a type check; we actually check if it is the one "special"
   * DocumentAnnotation that CAS.getDocumentAnnotation() would return.
   */
  private <T extends FeatureStructure> boolean isDocumentAnnotation(T aFS) {
    if (((TOP)aFS)._getTypeCode() != TypeSystemImpl.docTypeCode) {
      return false;
    }
    if (srcCasDocumentAnnotation == null) {
      srcCasDocumentAnnotation = srcCasViewImpl.getDocumentAnnotationNoCreate(); 
    }
    return aFS == srcCasDocumentAnnotation;
  }
  
  /**
   * Change from https://issues.apache.org/jira/browse/UIMA-3112 :
   *   requires that the CAS returned from the getLowLevelCAS() be castable to CASImpl
   * @param c1 -
   * @param c2 -
   * @return true if both views are in the same CAS (e.g., they have the same base CAS)
   */
  private boolean casViewsInSameCas(CAS c1, CAS c2) {
    if (null == c1 || null == c2) {
      return false;
    }

    CASImpl ci1 = (CASImpl) c1.getLowLevelCAS();
    CASImpl ci2 = (CASImpl) c2.getLowLevelCAS();
    
    return ci1.getBaseCAS() == ci2.getBaseCAS();
  }
  
  private TypeImpl getTargetType(TypeImpl srcTi) {
    if (srcTsi == tgtTsi) {
      return srcTi;
    }
    int srcTypeCode = srcTi.getCode();
    TypeImpl r = src2TgtType.get(srcTypeCode);
    if (r == null) {
      r = tgtTsi.getType(srcTi.getName());
      src2TgtType.put(srcTypeCode, (null == r) ? MISSING_TYPE : r);
    }
    return (r == MISSING_TYPE) ? null : r;
  }
  
  // tiny method to inline
  private FeatureImpl getTargetFeature(FeatureImpl srcFi) {
    if (srcTsi == tgtTsi) {
      return srcFi;
    }
    return getTargetFeature2(srcFi);
  }
  
  private FeatureImpl getTargetFeature2(FeatureImpl srcFi) {
    int srcFeatCode = srcFi.getCode();
    FeatureImpl r = src2TgtFeat.get(srcFeatCode);
    if (r == null) {
      TypeImpl d = (TypeImpl) srcFi.getDomain();
      TypeImpl td = getTargetType(d);
      if (td == null) {
        return null;
      }
      r = td.getFeatureByBaseName(srcFi.getShortName());
      src2TgtFeat.put(srcFeatCode, (null == r) ? MISSING_FEAT : r);
    }
    return (r == MISSING_FEAT) ? null : r;    
  }
  
}
