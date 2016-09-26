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

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Collection of builder style methods to specify selection of FSs from indexes
 * Comment codes:
 *   AI = implies AnnotationIndex
 */
public class SelectFSs_impl <T extends FeatureStructure> implements SelectFSs<T> {
  static enum BoundsUse {
    coveredBy,
    covering,
    sameBeginEnd,
  }
  
  private CASImpl view;
  private FSIndex<T> index; 
  private TypeImpl ti;
  private int shift; 
  
  private boolean isTypePriority = false;
  private boolean isPositionUsesType = false;
  private boolean isSkipEquals = false; // for boundsUse only
  private boolean isNonOverlapping = false;
  private boolean isEndWithinBounds = false;
  private boolean isAllViews = false;
  private boolean isNullOK = false;
  private boolean isUnordered = false;
  private boolean isBackwards = false;
  private boolean isShift = false;
  
  private BoundsUse boundsUse = null; 
  
  private TOP startingFs = null;
  private Annotation boundingFs = null;
  private int boundingBegin = -1;
  private int boundingEnd   = -1;
  
  /** derived **/
  private boolean isUseAnnotationIndex = false;  
  
  /************************************************
   * Constructors
   *   always need the cas
   *   might also have the type
   * Caller will convert other forms for the cas (e.g. jcas) 
   * and type (e.g. type name, MyType.type, MyType.class) to 
   * these arg forms.
   ************************************************/
  public SelectFSs_impl(CAS cas) {
    this.view = (CASImpl) cas.getLowLevelCAS();
  }
    
  public SelectFSs_impl(CAS cas, Type type) {
    this(cas);
    this.ti = (TypeImpl) type;
  }
  
  /************************************************
   * Builders
   ************************************************/
  /**
   * INDEX
   * If not specified, defaults to all FSs (unordered) unless AnnotationIndex implied
   */
  @Override
  public <N extends TOP> SelectFSs_impl<N> index(String indexName) {
    this.index = view.indexRepository.getIndex(indexName);
    return (SelectFSs_impl<N>) this;
  }
  
  @Override
  public <N extends TOP> SelectFSs_impl<N> index(FSIndex<N> aIndex) {
    this.index = (FSIndex<T>) aIndex;
    return (SelectFSs_impl<N>) this;
  }

  /**
   * TYPE
   * if not specified defaults to the index's uppermost type.  
   */
  @Override
  public <N extends TOP> SelectFSs_impl<N> type(Type uimaType) {
    this.ti = (TypeImpl) uimaType;
    return (SelectFSs_impl<N>) this;
  }
  @Override
  public <N extends TOP> SelectFSs_impl<N> type(String fullyQualifiedTypeName) {
    this.ti = view.getTypeSystemImpl().getType(fullyQualifiedTypeName);
    return (SelectFSs_impl<N>) this;
  }
  @Override
  public <N extends TOP> SelectFSs_impl<N> type(int jcasClass_dot_type) {
    this.ti = (TypeImpl) view.getJCas().getCasType(jcasClass_dot_type);
    return (SelectFSs_impl<N>) this;
  }
  @Override
  public <N extends TOP> SelectFSs_impl<N> type(Class<N> jcasClass_dot_class) {
    this.ti = (TypeImpl) view.getJCasImpl().getCasType(jcasClass_dot_class);
    return (SelectFSs_impl<N>) this;
  }  
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#typePriority()
   */
  @Override
  public SelectFSs<T> typePriority() {
    this.isTypePriority = true;
    return this;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#typePriority(boolean)
   */
  @Override
  public SelectFSs<T> typePriority(boolean aTypePriority) {
    this.isTypePriority = aTypePriority;
    return this;
  }

  /*********************************
   * boolean operations
   *********************************/
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#positionUsesType()
   */
  @Override
  public SelectFSs<T> positionUsesType() {
    this.isPositionUsesType = true;
    return this;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#positionUsesType(boolean)
   */
  @Override
  public SelectFSs<T> positionUsesType(boolean aPositionUsesType) {
    this.isPositionUsesType = aPositionUsesType;
    return this;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#skipEquals()
   */
  @Override
  public SelectFSs<T> skipEquals() {
    this.isSkipEquals = true;
    return this;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#skipEquals(boolean)
   */
  @Override
  public SelectFSs<T> skipEquals(boolean aSkipEquals) {
    this.isSkipEquals = aSkipEquals;
    return this;
  }

  /**
   * Filters while iterating
   **/
  
  @Override
  public SelectFSs_impl<T> nonOverlapping() { // AI known as unambiguous
    this.isNonOverlapping = true;
    return this;
  }  
  @Override
  public SelectFSs_impl<T> nonOverlapping(boolean bNonOverlapping) { // AI
    this.isNonOverlapping = bNonOverlapping;
    return this;
  } 
  
  @Override
  public SelectFSs_impl<T> endWithinBounds() { // AI known as "strict"
    isEndWithinBounds = true;
    return this;
  }  
  @Override
  public SelectFSs_impl<T> endWithinBounds(boolean bEndWithinBounds) { // AI
    isEndWithinBounds = bEndWithinBounds;
    return this;
  } 
  
//  public SelectFSs_impl<T> useTypePriorities() {
//    return this;
//  }
//  public SelectFSs_impl<T> useTypePriorities(boolean useTypePriorities) {
//    return this;
//  }
  
  /**
   * Miscellaneous
   **/
  
  @Override
  public SelectFSs_impl<T> allViews() {
    this.isAllViews = true;
    return this;
  }
  @Override
  public SelectFSs_impl<T> allViews(boolean bAllViews) {
    this.isAllViews = bAllViews;
    return this;
  }
  
  @Override
  public SelectFSs_impl<T> nullOK() { // applies to get() and single()
    this.isNullOK = true;
    return this;
  }  
  @Override
  public SelectFSs_impl<T> nullOK(boolean bNullOk) {  // applies to get() and single()
    this.isNullOK = bNullOk;
    return this;
  }
    
  @Override
  public SelectFSs_impl<T> unordered() {   // ignored if not ordered index
    this.isUnordered = true;
    return this;
  }                
  @Override
  public SelectFSs_impl<T> unordered(boolean bUnordered) { // ignored if not ordered index
    this.isUnordered = bUnordered;
    return this;
  } 
  
  @Override
  public SelectFSs_impl<T> backwards() { // ignored if not ordered index
    this.isBackwards = true;
    return this;
  }                  
  @Override
  public SelectFSs_impl<T> backwards(boolean bBackwards) { // ignored if not ordered index
    this.isBackwards = bBackwards;
    return this;
  } 
  
//  public SelectFSs_impl<T> noSubtypes() {
//    return this;
//  }
//  public SelectFSs_impl<T> noSubtypes(boolean noSubtypes) {
//    return this;
//  }

  /*********************************
   * starting position
   *********************************/
  @Override
  public SelectFSs_impl<T> startAt(TOP fs) {  // Ordered
    this.startingFs = fs;
    return this;
  } 
  @Override
  public SelectFSs_impl<T> startAt(int begin, int end) {  // AI
    this.startingFs = new Annotation(view.getJCas(), begin, end);
    return this;
  } 
  
  @Override
  public SelectFSs_impl<T> startAt(TOP fs, int offset) {  // Ordered
    this.startingFs = fs;
    this.shift = offset;
    this.isShift = true;
    return this;
  } 
  @Override
  public SelectFSs_impl<T> startAt(int begin, int end, int offset) {  // AI
    this.startingFs = new Annotation(view.getJCas(), begin, end);
    this.shift = offset;
    this.isShift = true;
    return this;
  }  
    
  /*********************************
   * subselection based on boundingFs
   *********************************/
  @Override
  public SelectFSs_impl<T> coveredBy(Annotation fs) {       // AI
    boundsUse = BoundsUse.coveredBy;
    this.boundingFs = fs;
    return this;
  }
  
  @Override
  public SelectFSs_impl<T> coveredBy(int begin, int end) {       // AI
    boundsUse = BoundsUse.coveredBy;
    this.boundingBegin = begin;
    this.boundingEnd = end;
    return this;
  }

  @Override
  public SelectFSs_impl<T> covering(Annotation fs) {      // AI
    boundsUse = BoundsUse.covering;
    this.boundingFs = fs;
    return this;
  }

  @Override
  public SelectFSs_impl<T> covering(int begin, int end) {      // AI
    boundsUse = BoundsUse.covering;
    this.boundingBegin = begin;
    this.boundingEnd = end;
    return this;
  }

  @Override
  public SelectFSs_impl<T> between(Annotation fs1, Annotation fs2) {   // AI
    final boolean reverse = fs1.getEnd() > fs2.getBegin();
    this.boundingFs = new Annotation(
        view.getJCas(), 
        (reverse ? fs2 : fs1).getEnd(), 
        (reverse ? fs1 : fs2).getBegin());
    this.boundsUse = BoundsUse.coveredBy;
    return this;
  }

  /**
   * prepare terminal operations
   */
  
  /**
   * Default type Annotation if not specified and annotation index is implied.
   * 
   */
  private void prepareTerminalOp() {
    isUseAnnotationIndex = !isAllViews && (
        ((index != null) && (index instanceof AnnotationIndex)) ||
        isNonOverlapping || isEndWithinBounds || boundsUse != null);
    
    if (isUseAnnotationIndex && null == ti) {
      ti = (TypeImpl) view.getAnnotationType();
    } 
    
    if (ti == null) {
      ti = view.getTypeSystemImpl().getTopType();
    }
    
    if (index == null && isUseAnnotationIndex) {
      this.index = (FSIndex<T>) view.getAnnotationIndex(ti);
    }
    
    // index may still be null at this point.
    
  }
  
  /*********************************
   * terminal operations
   * returning other than SelectFSs
   * 
   * Hierarchy of interpretation of setup:
   *   - index
   *   - type
   * 
   *   - allViews: ignored: things only with annotation index
   *               order among views is arbitrary, each view done together
   *               base view skipped
   *               
   *********************************/
  
  /**
   * F S I t e r a t o r
   * -------------------
   * ignored: backwards (because the fsIterator explicitly goes forwards and backwards)
   */
  @Override
  public FSIterator<T> fsIterator() {
    prepareTerminalOp();
    if (isAllViews) {
      return new FsIterator_aggregation_common<T>(getPlainIteratorsForAllViews(), null);
    }
    return plainFsIterator(index, view);
  }
  
  
  private FSIterator<T>[] getPlainIteratorsForAllViews() {
    final int nbrViews = view.getNumberOfViews();
    FSIterator<T>[] ita = new FSIterator[nbrViews];
    
    for (int i = 1; i <= nbrViews; i++) {
      CASImpl v = (i == 1) ? view.getInitialView() : (CASImpl) view.getView(i);
      ita[i - 1] = plainFsIterator(getIndexForView(v), v);
    }
    return ita;
  }
  
  private FSIndex<T> getIndexForView(CASImpl v) {
    if (index == null) {
      return null;
    }
    
    FSIndexRepositoryImpl ir = (FSIndexRepositoryImpl) v.getIndexRepository();
    if (index instanceof FsIndex_iicp) {
      FsIndex_iicp idx = (FsIndex_iicp) index;
      return ir.getIndexBySpec(idx.getTypeCode(), idx.getIndexingStrategy(), idx.getComparatorImplForIndexSpecs()); 
    }
    FsIndex_singletype idx = (FsIndex_singletype) index;
    return ir.getIndexBySpec(idx.getTypeCode(), idx.getIndexingStrategy(), idx.getComparatorImplForIndexSpecs());
  }
  
  
  private FSIterator<T> plainFsIterator(FSIndex idx, CASImpl v) {
    if (null == idx) { 
      // no bounds, not ordered
      // type could be null
      return v.indexRepository.getAllIndexedFS(ti);  
    }
    
    final boolean isIndexOrdered = idx.getIndexingStrategy() == FSIndex.SORTED_INDEX;
    final boolean isAnnotationIndex = idx instanceof AnnotationIndex;
    final AnnotationIndex ai = isAnnotationIndex ? (AnnotationIndex)idx: null;
    if (boundsUse == null) {
      if (!isIndexOrdered) {
        return idx.iterator(startingFs);       
      } else {
        // index is ordered but no bounds are being used - return plain fsIterator or maybe nonOverlapping version
        FSIterator<T> it = (isAnnotationIndex && isNonOverlapping)
                             ? ai.iterator(true)
                             : (isUnordered && idx instanceof FsIndex_iicp) 
                                 ? ((FsIndex_iicp<T>)idx).iteratorUnordered()
                                 : idx.iterator(); 
        return maybeShift(maybePosition(it, startingFs, isAnnotationIndex, isTypePriority, isPositionUsesType)); 
      }
    }
    // bounds in use, index must be annotation index, is ordered
    
    return (FSIterator<T>) new Subiterator<>(
        idx.iterator(),
        boundingFs,
        !isNonOverlapping, 
        isEndWithinBounds,
        true, // is bounded
        isTypePriority, 
        isPositionUsesType, 
        isSkipEquals,
        v.indexRepository.getAnnotationFsComparator());    
  }
  
  
  @Override
  public Iterator<T> iterator() {
    return null;
  }
  @Override
  public List<T> asList() {
    return null;
  }
  @Override
  public Spliterator<T> spliterator() {
    return null;
  }
  @Override
  public T get() {
    return null;
  }
  @Override
  public T single() {
    return null;
  }
  
  /**
   * works for AnnotationIndex or general index
   * 
   * position taken from startingFs (not necessarily an Annotation subtype)
   *   - goes to left-most "equal" using comparitor, or if none equal, to the first one > startingFs
   *     -- using moveTo(fs)
   * 
   * special processing for AnnotationIndex (only):
   *   - typePriority - use or ignore
   *     -- ignored: after moveTo(fs), moveToPrevious while begin && end ==
   *       --- and if isPositionUsesType types are == 
   * @param it
   * @return it positioned if needed
   */
  public static <T extends FeatureStructure> FSIterator<T> maybePosition(
      FSIterator<T> it,
      TOP startingFs,
      boolean isAnnotationIndex,
      boolean isTypePriority,
      boolean isPositionUsesType) {
    if (!it.isValid() || startingFs == null) {
      return it;
    }
    
    it.moveTo(startingFs);
    
    if (isAnnotationIndex) {
      if (!isTypePriority) {
        int begin = ((Annotation)startingFs).getBegin();
        int end = ((Annotation)startingFs).getEnd();
        Type type = startingFs.getType();
        Annotation fs = (Annotation) it.get();
        while (begin == fs.getBegin() && end == fs.getEnd() 
               && (!isPositionUsesType || type == fs.getType())) {
          it.moveToPreviousNvc();
          if (!it.isValid()) {
            it.moveToFirst();
            return it;
          }
          fs = (Annotation) it.get();
        }
        it.moveToNext();
      }
    }
    return it;
  }
  
  private FSIterator<T> maybeShift(FSIterator<T> it) {
    if (isShift) {
      int ps = Math.abs(shift);
      
      for (int i = 0; i < ps; i++) {
        if (shift < 0) {
          it.moveToPrevious();
        } else {
          it.moveToNext();
        }
      }
    }
    return it;
  }
  /********************************************
   * The methods below are alternatives 
   * to the methods above, that combine
   * frequently used patterns into more
   * concise forms using positional arguments
   ********************************************/

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#at(org.apache.uima.jcas.tcas.Annotation)
   */
  @Override
  public SelectFSs<T> at(Annotation fs) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#at(int, int)
   */
  @Override
  public SelectFSs<T> at(int begin, int end) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#following(org.apache.uima.jcas.tcas.Annotation)
   */
  @Override
  public SelectFSs<T> following(Annotation fs) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#following(int, int)
   */
  @Override
  public SelectFSs<T> following(int begin, int end) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#following(org.apache.uima.jcas.tcas.Annotation, int)
   */
  @Override
  public SelectFSs<T> following(Annotation fs, int offset) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#following(int, int, int)
   */
  @Override
  public SelectFSs<T> following(int begin, int end, int offset) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#preceding(org.apache.uima.jcas.tcas.Annotation)
   */
  @Override
  public SelectFSs<T> preceding(Annotation fs) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#preceding(int, int)
   */
  @Override
  public SelectFSs<T> preceding(int begin, int end) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#preceding(org.apache.uima.jcas.tcas.Annotation, int)
   */
  @Override
  public SelectFSs<T> preceding(Annotation fs, int offset) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.SelectFSs#preceding(int, int, int)
   */
  @Override
  public SelectFSs<T> preceding(int begin, int end, int offset) {
    // TODO Auto-generated method stub
    return null;
  }
  

  
  /************************
   * NOT USED
   */
//  public SelectFSs_impl<T> sameBeginEnd() {  // AI
//    boundsUse = BoundsUse.sameBeginEnd;
//    return this;
//  }

  /**
   * validations
   *   isAnnotationIndex => startingFs is Annotation 
   */
  
  
}
