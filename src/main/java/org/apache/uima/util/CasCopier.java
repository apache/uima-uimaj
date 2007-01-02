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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Utility class for doing deep copies of FeatureStructures from one CAS to another. To handle cases
 * where the source CAS has multiple references to the same FS, you can create one instance of
 * CasCopier and use it to copy multiple FeatureStructures. The CasCopier will remember previously
 * copied FeatureStructures, so if you later copy another FS that has a reference to a previously
 * copied FS, it will not duplicate the multiply-referenced FS.
 */
public class CasCopier {
  private CAS mDestCas;

  private Map mFsMap = new HashMap();

  /**
   * Creates a new CasCopier that can be used to copy FeatureStructures from one CAS to another.
   * 
   * @param aDestCas
   *          the CAS to copy into.
   */
  public CasCopier(CAS aDestCas) {
    mDestCas = aDestCas;
  }
  
  /**
   * Does a complete deep copy of one CAS into another CAS.  The contents of each view
   * in the source CAS will be copied to the same-named view in the destination CAS.  If
   * the view does not already exist it will be created.  All FeatureStructures that are
   * indexed in a view in the source CAS will become indexed in the same-named view in the
   * destination CAS.
   * 
   * @param aSrcCas
   *          the CAS to copy from
   * @param aDestCas
   *          the CAS to copy to
   * @param aCopySofa
   *          if true, the sofa data and mimeType of each view will be copied. If false they will not.
   */  
  public static void copyCas(CAS aSrcCas, CAS aDestCas, boolean aCopySofa) {
    //create views if they do not exist
    Iterator sofaIter = aSrcCas.getSofaIterator();
    while (sofaIter.hasNext()) {
      SofaFS sofa = ((SofaFS)sofaIter.next());
      try {
        aDestCas.getView(sofa.getSofaID()); //TODO: is this safe with sofa mappings?
      }
      catch(CASRuntimeException e) {
        //create the view
       aDestCas.createView(sofa.getSofaID()); //TODO: is this safe with sofa mappings?
      }
    }
    
    CasCopier copier = new CasCopier(aDestCas);
    
    sofaIter = aSrcCas.getSofaIterator();
    while (sofaIter.hasNext()) {
      SofaFS sofa = ((SofaFS)sofaIter.next());
      CAS view = aSrcCas.getView(sofa);
      copier.copyCasView(view, aCopySofa);
    }
  }

  /**
   * Does a deep copy of the contents of one CAS View into another CAS.
   * If a view with the same name as <code>aSrcCasView</code> exists in the destination CAS,
   * then it will be the target of the copy.  Otherwise, a new view will be created with
   * that name and will become the target of the copy.  All FeatureStructures that are indexed 
   * in the source CAS view will become indexed in the target view.
   * 
   * @param aSrcCasView
   *          the CAS to copy from
   * @param aCopySofa
   *          if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(CAS aSrcCasView, boolean aCopySofa) {
    //get or create the target view
    CAS targetView;
    try {
      targetView = mDestCas.getView(aSrcCasView.getViewName()); //TODO: is this safe with sofa mappings?
    }
    catch(CASRuntimeException e) {
      //create the view
      targetView = mDestCas.createView(aSrcCasView.getViewName());
    }
    
    if (aCopySofa) {
      // can't copy the SofaFS - just copy the sofa data and mime type
      String sofaMime = aSrcCasView.getSofa().getSofaMime();
      if (aSrcCasView.getDocumentText() != null) {
        targetView.setSofaDataString(aSrcCasView.getDocumentText(), sofaMime);
      } else if (aSrcCasView.getSofaDataURI() != null) {
        targetView.setSofaDataURI(aSrcCasView.getSofaDataURI(), sofaMime);
      } else if (aSrcCasView.getSofaDataArray() != null) {
        targetView.setSofaDataArray(copyFs(aSrcCasView.getSofaDataArray()), sofaMime);
      }
    }

    // now copy indexed FS, but keep track so we don't index anything more than once
    Set indexedFs = new HashSet();
    Iterator indexes = aSrcCasView.getIndexRepository().getIndexes();
    while (indexes.hasNext()) {
      FSIndex index = (FSIndex) indexes.next();
      Iterator iter = index.iterator();
      while (iter.hasNext()) {
        FeatureStructure fs = (FeatureStructure) iter.next();
        if (!indexedFs.contains(fs)) {
          FeatureStructure copyOfFs = copyFs(fs);
          // also don't index the DocumentAnnotation (it's indexed by default)
          //TODO: clean this up
          if (!(copyOfFs instanceof AnnotationFS) ||
              !copyOfFs.equals(((AnnotationFS)copyOfFs).getView().getDocumentAnnotation())) {
            targetView.addFsToIndexes(copyOfFs);
          }
          indexedFs.add(fs);
        }
      }
    }
  }

  /**
   * Copies an FS from the source CAS to the destination CAS. Also copies any referenced FS, except
   * that previously copied FS will not be copied again.
   * 
   * @param aFS
   *          the FS to copy. Must be contained within the source CAS.
   * @return the copy of <code>aFS</code> in the target CAS.
   */
  public FeatureStructure copyFs(FeatureStructure aFS) {
    // check if we already copied this FS
    FeatureStructure copy = (FeatureStructure) mFsMap.get(aFS);
    if (copy != null)
      return copy;

    // get the type of the FS
    Type srcType = aFS.getType();

    // Certain types need to be handled specially

    // Sofa - cannot be created by normal methods.  Instead, we return the Sofa with the
    // same Sofa ID in the target CAS.  If it does not exist it will be created.
    if (aFS instanceof SofaFS) {
      String sofaId = ((SofaFS)aFS).getSofaID();
      SofaFS destSofa;
      //TODO: would be better if we could check for sofa existence without the try...catch
      try {
        destSofa = mDestCas.getView(sofaId).getSofa(); //TODO: is this safe with sofa mappings?
      }
      catch(CASRuntimeException e) {
        //create the Sofa
        destSofa = mDestCas.createView(sofaId).getSofa();
      }
      return destSofa;
    }

    // DocumentAnnotation - instead of creating a new instance, reuse the automatically created
    // instance in the destination view.
    if (aFS instanceof AnnotationFS && 
        aFS.equals(((AnnotationFS)aFS).getView().getDocumentAnnotation())) {         
      String viewName = ((AnnotationFS)aFS).getView().getViewName();
      CAS destView = mDestCas.getView(viewName);
      FeatureStructure destDocAnnot = destView.getDocumentAnnotation();
      if (destDocAnnot != null) {
        copyFeatures(aFS, destDocAnnot);
      }
      return destDocAnnot;
    }

    // Arrays - need to be created a populated differently than "normal" FS
    if (aFS.getType().isArray()) {
      copy = copyArray(aFS);
      mFsMap.put(aFS, copy);
      return copy;
    }

    // create a new FS of the same type in the target CAS
    // TODO: could optimize type lookup if type systems are identical
    Type destType = mDestCas.getTypeSystem().getType(srcType.getName());
    if (destType == null) {
      throw new UIMARuntimeException(UIMARuntimeException.TYPE_NOT_FOUND_DURING_CAS_COPY,
              new Object[] { srcType.getName() });
    }
    FeatureStructure destFs = mDestCas.createFS(destType);

    // add to map so we don't try to copy this more than once
    mFsMap.put(aFS, destFs);

    copyFeatures(aFS, destFs);
    return destFs;
  }

  /**
   * Copy feature values from one FS to another. For reference-valued features, this does a deep
   * copy.
   * 
   * @param aSrcFS
   *          FeatureStructure to copy from
   * @param aDestFS
   *          FeatureStructure to copy to
   */
  private void copyFeatures(FeatureStructure aSrcFS, FeatureStructure aDestFS) {
    // set feature values
    Type destType = aDestFS.getType();
    Iterator featIter = destType.getFeatures().iterator();
    while (featIter.hasNext()) {
      Feature srcFeat = (Feature) featIter.next();
      // TODO: could optimize type lookup if type systems are identical
      Feature destFeat;
      if (destType == aSrcFS.getType()) {
        // sharing same type system, so destFeat == srcFeat
        destFeat = srcFeat;
      } else {
        // not sharing same type system, so do a name loop up in destination type system
        destFeat = destType.getFeatureByBaseName(srcFeat.getShortName());
        if (destFeat == null) {
          throw new UIMARuntimeException(UIMARuntimeException.TYPE_NOT_FOUND_DURING_CAS_COPY,
                  new Object[] { srcFeat.getName() });
        }
      }

      // copy primitive values using their string representation
      // TODO: could be optimized but this code would be very messy if we have to
      // enumerate all possible primitive types. Maybe LowLevel CAS API could help?
      if (srcFeat.getRange().isPrimitive()) {
        aDestFS.setFeatureValueFromString(destFeat, aSrcFS.getFeatureValueAsString(srcFeat));
      } else {
        // recursive copy
        FeatureStructure refFS = aSrcFS.getFeatureValue(srcFeat);
        if (refFS != null) {
          FeatureStructure copyRefFs = copyFs(refFS);
          aDestFS.setFeatureValue(destFeat, copyRefFs);
        }
      }
    }
  }

  /**
   * Returns whether the given FS has already been copied using this CasCopier.
   * 
   * @param aFS
   * @return
   */
  public boolean alreadyCopied(FeatureStructure aFS) {
    return mFsMap.containsKey(aFS);
  }

  /**
   * @param arrayFS
   * @return
   */
  private FeatureStructure copyArray(FeatureStructure aSrcFs) {
    // TODO: there should be a way to do this without enumerating all the array types!
    if (aSrcFs instanceof StringArrayFS) {
      StringArrayFS arrayFs = (StringArrayFS) aSrcFs;
      int len = arrayFs.size();
      StringArrayFS destFS = mDestCas.createStringArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof IntArrayFS) {
      IntArrayFS arrayFs = (IntArrayFS) aSrcFs;
      int len = arrayFs.size();
      IntArrayFS destFS = mDestCas.createIntArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof ByteArrayFS) {
      ByteArrayFS arrayFs = (ByteArrayFS) aSrcFs;
      int len = arrayFs.size();
      ByteArrayFS destFS = mDestCas.createByteArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof ShortArrayFS) {
      ShortArrayFS arrayFs = (ShortArrayFS) aSrcFs;
      int len = arrayFs.size();
      ShortArrayFS destFS = mDestCas.createShortArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof LongArrayFS) {
      LongArrayFS arrayFs = (LongArrayFS) aSrcFs;
      int len = arrayFs.size();
      LongArrayFS destFS = mDestCas.createLongArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof FloatArrayFS) {
      FloatArrayFS arrayFs = (FloatArrayFS) aSrcFs;
      int len = arrayFs.size();
      FloatArrayFS destFS = mDestCas.createFloatArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof DoubleArrayFS) {
      DoubleArrayFS arrayFs = (DoubleArrayFS) aSrcFs;
      int len = arrayFs.size();
      DoubleArrayFS destFS = mDestCas.createDoubleArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof BooleanArrayFS) {
      BooleanArrayFS arrayFs = (BooleanArrayFS) aSrcFs;
      int len = arrayFs.size();
      BooleanArrayFS destFS = mDestCas.createBooleanArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof ArrayFS) {
      ArrayFS arrayFs = (ArrayFS) aSrcFs;
      int len = arrayFs.size();
      ArrayFS destFS = mDestCas.createArrayFS(len);
      for (int i = 0; i < len; i++) {
        FeatureStructure copyElem = copyFs(arrayFs.get(i));
        destFS.set(i, copyElem);
      }
      return destFS;
    }
    assert false; // the set of array types should be exhaustive, so we should never get here
    return null;
  }
}
