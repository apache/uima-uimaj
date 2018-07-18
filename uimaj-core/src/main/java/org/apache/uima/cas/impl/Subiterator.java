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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Predicate;

import org.apache.uima.cas.FSComparators;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.impl.JCasImpl;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Internal implementation of Subiterators.
 * External APIs are
 *   - methods on AnnotationIndex
 *   - the SelectFSs Interface
 *   
 * This implementation extends the V2 implementation to support the 
 * variations allowed by SelectFSs and AnnotationIndex
 * 
 * Several of the variations arise around how to handle FSs that are comparator-equal.
 * 
 * The variations are in three broad categories - bounds, skipping over FSs, and positioning using type ordering or not
 *   - bounds:
 *      -- no bounds
 *      -- same begin / end 
 *      -- coveredBy   
 *      -- covering  
 *   - skipping over FSs
 *     - skip over the bounding annotation(s)
 *        -- if it is the == one (uimaFIT style)
 *        -- if it is comparator == (the UIMA v2 style)
 *          --- with or without typePriority
 *            ----  if without typePriority, with or without type ==
 *          *****************************************
 *          * redesigned 8/2017
 *          *   for skipping, the type is always used, that is
 *          *   items are skipped over only if having the same type
 *          *****************************************
 *     - unambiguous (for no-bounds or coveredBy, only: skip over FSs whose begin is < previous one's end) 
 *     - strict (for coveredBy only: skip if end is > bounding Annotation's end)
 *   - positioning (for moveTo)
 *          *****************************************
 *          * redesigned 8/2017
 *          *   TypeOrderKey is always used
 *          *****************************************
 *     - NO maybe ignore type priorities while positioning to left-most equal item
 *       -- NO if ignoring type priorities, maybe still compare types for equal when adjusting
 *          NO to left-most equal item.
 *          
 * Positioning variations affect only the moveTo(fs) method.
 *
 * This class is not used for select sources which are ordered, but non-sorted collections / arrays.
 * This class is used only with an AnnotationIndex.
 * 
 * Interaction among bounds, skipping, and positioning:
 *   while moveTo(fs), items are skipped over.
 *     - unambiguous
 *     - skip over bounding annotation
 *     - strict
 *   moveTo(fs): uses isUseTypePriority and isPositionUsesType and isSkipEquals
 *   while moveTo(fs), bounds limit position
 *   
 *   while moving to First/Last:
 *     bounds provide edge
 *     skipping done if skipped element at edge
 *     (moveToLast & unambiguous - special calculation from start)
 *     
 */

/**
 * Subiterator implementation.
 * 
 * There are 2 underlying forms.
 * 
 * The 2nd form is produced lazily when needed, and 
 * is made by a one-time forward traversal to compute unambiguous subsets and store them into a list.
 *   - The 2nd form is needed only for unambiguous style if backwards or moveTo(fs), or moveToLast operations.
 * 
 * The 1st form uses the underlying iterator directly, and does skipping as needed, while iterating  
 *   - going forward: 
 *       skip if unambiguous and start is within prev span
 *       skip if strict and end lies outside of scope span
 *     
 *   - going backward:
 *       if unambiguous - convert to form 2
 *       skip if strict and end lies outside of scope span
 *       
 *   - going to particular fs (left most match)
 *       if unambiguous - convert to form 2
 *       skip (forward) if strict and end lies outside of scope span
 *
 *   - going to first:
 *       unambiguous - no testing needed, no prior span 
 *       skip if strict and end lies outside of scope span
 *     
 *   - going to last:
 *       unambiguous - convert to 2nd form
 *       skip backwards if strict and end lies outside of scope span
 *       
 * There are two styles of the bounding information.
 * 
 *   - the traditional one uses the standard comparator for annotations: begin (ascending), end (descending) and
 *     type priority ordering
 *   - the 2nd style uses just a begin value and an end value, no type priority ordering. 
 *   
 * Interaction with copy-on-write concurrent modification avoidance
 *   As with other iterators, the moveToFirst/Last/feature-structure-position "resets" the underlying
 *     iterators to match their current indexes.  
 *   This implementation maintains local data: the list form and the isEmpty flag.  These would
 *     also need recomputing for the above operations, if isIndexesHaveBeenUpdated() is true. 
 *   
 */
public class Subiterator<T extends AnnotationFS> implements LowLevelIterator<T> {

  private static final boolean IS_GOING_FORWARDS = true; 
  private static final boolean IS_GOING_BACKWARDS = false;
  
  public static enum BoundsUse {
    coveredBy,  // iterate within bounds specified by controlling Feature Structure
    covering,   // iterate over FSs which cover the bounds specified by controlling Feature Structure
    sameBeginEnd,  // iterate over FSs having the same begin and end 
    notBounded,
  }
    
  /*
   * The generic type of this is Annotation, not T, because that allows the use of a comparator which is comparing
   * a key which is not T but some super type of T.
   */
  private ArrayList<Annotation> list = null; // used for form 2, lazily initialized

  private int pos = 0;  // used for form 2
  
  private final LowLevelIterator<Annotation> it;
  
  private final Annotation boundingAnnot;  // the bounding annotation need not be a subtype of T
  
  private final Annotation coveringStartPos;
  private final Annotation coveringEndPos;  // an approx end position
  
  private final boolean isUnambiguous;  // true means need to skip until start is past prev end (going forward)
  private final boolean isStrict;     // only true for coveredby; means skip things while iterating where the end is outside the bounds
  /** true if bounds is one of sameBeginEnd, coveredBy, covering */
  private final boolean isBounded;
  /** for moveTo-leftmost, and bounds skipping if isSkipEquals */
  private final boolean isUseTypePriority;
//  private final boolean underlying_iterator_using_typepriorities;
//  /** two uses: bounds skip if isSkipEqual, move-to-leftmost if !isUseTypePriority */
//  private final boolean isPositionUsesType;
  /** for bounds skipping alternative */
  private final boolean isSkipSameBeginEndType;
  /** one of notBounded, sameBeginEnd, coveredBy, covering */
  private final BoundsUse boundsUse; 
  
  /**
   * isEmpty is a potentially expensive calculation, involving potentially traversing all the iterators in a particular type hierarchy
   * It is only done:
   *   - at init time
   *   - at moveToFirst/last/FS - these can cause regenerating the copy-on-write objects via the getNonNullCow call
   *       if no new nonNullCow was obtained, no need to recalculate empty
   */
  private boolean isEmpty;
  
  private int prevEnd = 0;  // for unambiguous iterators
  
  /**
   * list form is recalculated at moveToFirst/last/fs, same as isEmpty
   */
  private boolean isListForm = false;
  
  /**
   * startId is recalculated at moveToFirst/last/fs, same as isEmpty
   */
  private int startId;

  private final int boundBegin;
  private final int boundEnd;
  private final TypeImpl boundType;
  
  /** null if ! isTypePriority */
  private final LinearTypeOrder lto; 
  
  // only used in moveTo, in listform
  // if ignoreTypeOrdering, use that form of comparator
  private final Comparator<TOP> comparatorMaybeNoTypeWithoutId;  // implements compare(fs1, fs2) for begin/end/type priority

  // only used when locating existing item after converting to list form
  private final Comparator<TOP> annotationComparator_withId;
  private final JCasImpl jcas;
  
  /**
   * Caller is the implementation of AnnotationIndex, FSIndex_annotation.
   * 
   * A normal iterator is passed in, already positioned to where things should start.
   * 
   * A bounding FS is passed in (except for unbounded unambiguous iteration.
   *             
   * @param it the iterator to use, positioned to the correct starting place
   * @param boundingAnnot null or the bounding annotation
   * @param ambiguous false means to skip annotations whose begin lies between previously returned begin (inclusive) and end (exclusive)
   * @param strict true means to skip annotations whose end is greater than the bounding end position (ignoring type priorities)
   * @param boundsUse null if no bounds, boundingAnnot used for start position if non-null
   * @param isUseTypePriority false to ignore type priorities, and just use begin/end and maybe type
   * @param isSkipSameBeginEndType used only for coveredBy or covering case, 
   *                     false means to only skip returning an FS if it has the same id() as the bounding fs
   */
  Subiterator(
      FSIterator<T> it, 
      AnnotationFS boundingAnnot, 
      boolean ambiguous, 
      boolean strict,    // omit FSs whose end > bounds, requires boundsUse == boundedby
      BoundsUse boundsUse, // null if no bounds; boundingAnnot used for start position if non-null
      boolean isUseTypePriority,
      boolean isSkipSameBeginEndType  // useAnnotationEquals
      ) {
    
    this.it = (LowLevelIterator<Annotation>) it;
    this.boundingAnnot = (Annotation) boundingAnnot;  // could be same begin/end, coveredby, or covering
    this.isBounded = boundsUse != null && boundsUse != BoundsUse.notBounded;
    this.boundsUse = (boundsUse == null) ? BoundsUse.notBounded : boundsUse;
    this.isUnambiguous = !ambiguous;
    if (strict) {
      if (BoundsUse.coveredBy != boundsUse && BoundsUse.sameBeginEnd != boundsUse) {
        throw new IllegalArgumentException("Strict requires BoundsUse.coveredBy or BoundsUse.sameBeginEnd");
      }
    }
    this.isStrict = strict;
    this.isSkipSameBeginEndType = isSkipSameBeginEndType;
 
    if (isBounded && (null == boundingAnnot || !(boundingAnnot instanceof Annotation))) {
      Misc.internalError(new IllegalArgumentException("Bounded Subiterators require a bounding annotation"));
    }
    this.boundBegin = isBounded ? boundingAnnot.getBegin() : -1;
    this.boundEnd = isBounded ? boundingAnnot.getEnd(): -1;
    this.boundType = isBounded ? (TypeImpl) boundingAnnot.getType() : null;

    FSIndexRepositoryImpl ir = this.it.ll_getIndex().getCasImpl().indexRepository;
//    underlying_iterator_using_typepriorities =  ir.isAnnotationComparator_usesTypeOrder();

    if (boundsUse == BoundsUse.covering && isUseTypePriority) {
      throw new IllegalArgumentException("Cannot specify isUseTypePriority with BoundsUse.covering");
    }

    this.isUseTypePriority = isUseTypePriority;
    lto = isUseTypePriority ? ir.getDefaultTypeOrder() : null;
    
    this.comparatorMaybeNoTypeWithoutId = ir.getAnnotationFsComparator(
        FSComparators.WITHOUT_ID, 
        isUseTypePriority ?  FSComparators.WITH_TYPE_ORDER : FSComparators.WITHOUT_TYPE_ORDER);
    this.annotationComparator_withId = ir.getAnnotationFsComparatorWithId();
        
    this.jcas = (JCasImpl) ll_getIndex().getCasImpl().getJCas();
            
    if (boundsUse == BoundsUse.covering) {
      // compute start position and isEmpty setting
      int span = ((LowLevelIterator<?>)it).ll_maxAnnotSpan();  // an optimization, the largest end-begin annotation
      int begin = boundEnd - span;
      if (begin > boundBegin) {
        makeInvalid();
        coveringStartPos = coveringEndPos = null;
        isEmpty = true;
        startId = 0;
        return;
      }
      if (begin < 0) {
        begin = 0;
      }
      coveringStartPos = new Annotation(jcas, begin, Integer.MAX_VALUE);
      coveringEndPos = new Annotation(jcas, boundBegin + 1, boundBegin + 1);
    } else {
      coveringStartPos = coveringEndPos = null;
    }
    
    moveToStartSetEmptyAndId();
  }
  
  private void moveToStartSetEmptyAndId() {
    moveToStart();
    isEmpty = !isValid();
    startId = isValid() ? getNvc()._id() : 0;    
  }
   
  /** 
   * copy constructor - no move to start
   * @param it -
   * @param boundingAnnot -
   * @param ambiguous -
   * @param strict -
   * @param boundsUse -
   * @param isUseTypePriority -
   * @param isSkipSameBeginEndType -
   * @param startId -
   * @param isEmpty -
   * @param converingStartPos -
   * @param converingEndPos -
   */
  Subiterator(
      FSIterator<Annotation> it, 
      Annotation boundingAnnot, 
      boolean ambiguous, 
      boolean strict,    // omit FSs whose end > bounds
      BoundsUse boundsUse, // null if boundingAnnot being used for starting position in unambiguous iterator
      boolean isUseTypePriority,
      boolean isSkipSameBeginEndType,
      int startId,
      boolean isEmpty,
      Annotation coveringStartPos,
      Annotation coveringEndPos
      ) {
    
    this.it = (LowLevelIterator<Annotation>) it;
    this.boundingAnnot = (Annotation) boundingAnnot;  // could be same begin/end, coveredby, or covering
    this.isBounded = boundsUse != null && boundsUse != BoundsUse.notBounded;
    this.boundsUse = (boundsUse == null) ? BoundsUse.notBounded : boundsUse;
    this.isUnambiguous = !ambiguous;
    if (strict) {
      if (BoundsUse.coveredBy != boundsUse && BoundsUse.sameBeginEnd != boundsUse) {
        throw new IllegalArgumentException("Strict requires BoundsUse.coveredBy or BoundsUse.sameBeginEnd");
      }
    }
    this.isStrict = strict;
    this.isSkipSameBeginEndType = isSkipSameBeginEndType;

    this.boundBegin = isBounded ? boundingAnnot.getBegin() : -1;
    this.boundEnd = isBounded ? boundingAnnot.getEnd(): -1;
    this.boundType = isBounded ? (TypeImpl) boundingAnnot.getType() : null;

    FSIndexRepositoryImpl ir = this.it.ll_getIndex().getCasImpl().indexRepository;
//    underlying_iterator_using_typepriorities =  ir.isAnnotationComparator_usesTypeOrder();

    this.isUseTypePriority = isUseTypePriority;
    lto = isUseTypePriority ? ir.getDefaultTypeOrder() : null;

    this.comparatorMaybeNoTypeWithoutId = ir.getAnnotationFsComparator(
        FSComparators.WITHOUT_ID, 
        isUseTypePriority ?  FSComparators.WITH_TYPE_ORDER : FSComparators.WITHOUT_TYPE_ORDER);
    this.annotationComparator_withId = ir.getAnnotationFsComparatorWithId();
    
    this.jcas = (JCasImpl) ll_getIndex().getCasImpl().getJCas();
    
    this.coveringStartPos = coveringStartPos;
    this.coveringEndPos = coveringEndPos;
    this.startId = startId;
    this.isEmpty = isEmpty;
    if (isEmpty) {
      makeInvalid();
    }    
  }

  /**
   * Converting to list form - called for 
   *   unambiguous iterator going backwards, 
   *   unambiguous iterator doing a moveTo(fs) operation
   *   unambiguous iterator doing a moveToLast() operation 
   */
  
  private void convertToListForm() {
    moveToStart();  // moves to the start annotation, including moving past equals for annot style, 
                    // and accommodating strict
    this.list = new ArrayList<Annotation>();
    while (isValid()) {
      list.add((Annotation) it.getNvc());
      moveToNext();  // does all the adjustments, so list has only appropriate elements
    }
    this.pos = 0;
    isListForm = true;  // do at end, so up to this point, iterator is not the list form style
  }
  
  
  /**
   * Move to the starting position of the sub iterator.
   * isEmpty may not yet be set.
   * Never list form when called.
   */
  private void moveToStart() {
    
    switch (boundsUse) {
    
    case notBounded:
      it.moveToFirstNoReinit();
      break;
      
    case sameBeginEnd:
      it.moveToNoReinit(boundingAnnot);
      if (it.isValid()) {
        skipOverBoundingAnnot(IS_GOING_FORWARDS);
      }
      break;
    
    case coveredBy:
      it.moveToNoReinit(boundingAnnot);
      if (it.isValid()) {
        adjustForStrictOrCoveringAndBoundSkipNvc(IS_GOING_FORWARDS);
      }
      break;
    
    case covering:
      it.moveToNoReinit(coveringStartPos); // sufficiently before the bounds
      if (it.isValid()) {
        adjustForCoveringAndBoundSkip(IS_GOING_FORWARDS); 
      }
      break;
    }
    
    if (! isBoundOk()) {
      return;
    }
    
    maybeSetPrevEnd();  // used for unambiguous
  }
  
  
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    if (isListForm) {
      return (this.pos >= 0) && (this.pos < this.list.size());
    }

    // assume all non-list form movements leave the underlying iterator
    //   positioned either as invalid, or at the valid spot including the bounds, for 
    //   a get();
    
    return it.isValid();
//    if (!it.isValid()) {
//      return false;
//    }
//        
//    // if iterator is valid, may still be invalid due to bounds
//    switch (boundsUse) {
//    case notBounded:
//      return true;
//      
//    case coveredBy: {
//      Annotation item = it.getNvc();
//      if (item.getBegin() > boundEnd) return false;
//      if 
//    }
//      if (t.get().getBegin() <= boundEnd;
//      
//      
//    case covering:
//      return it.isValid();
//    case sameBeginEnd:
//      Annotation a = it.get();
//      return a.getBegin() == boundBegin &&
//             a.getEnd() == boundEnd;
//    }
//    return false; // ignored, just hear to get rid of invalid compiler error report
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#getNvc()
   */
  public T getNvc() {
    if (isListForm) {
      return (T) this.list.get(this.pos);
    } else {
      return (T)it.getNvc();
    }
  }
  
  @Override
  public void moveToNextNvc() {  
    // no isValid check because caller checked: "Nvc"
    if (isListForm) {
      ++this.pos;
      // maybeSetPrevEnd not needed because list form already accounted for unambiguous
      return;
    }
      
    it.moveToNextNvc();
    if (isUnambiguous) {               // skip until start > prev end
      movePastPrevAnnotation();
    }

    adjustForStrictOrCoveringAndBoundSkip(IS_GOING_FORWARDS);
    
    // stop logic going forwards for various bounding cases
    if (it.isValid()) {
      // stop in bounded case if out of bounds going forwards UIMA-5063
      if ( ! isBoundOkNvc()) {
        return;
      }
//      switch(boundsUse) {
//      case notBounded: 
//        break;
//      case coveredBy:
//        maybeMakeItInvalid_bounds(it.getNvc(), a -> a.getBegin() > boundEnd);
//        break;
//      case covering:
//        maybeMakeItInvalid_bounds(it.getNvc(), a -> a.getBegin() > boundBegin);
//        break;
//      case sameBeginEnd:
//        maybeMakeItInvalidSameBeginEnd();
//        break;
//      }
    } 
    maybeSetPrevEnd();
  }
    
  @Override  
  public void moveToPreviousNvc() {    
    // no isValid check because caller checked: "Nvc"
    if (isListForm) {
      --this.pos;
      return;
    }
    
    if (isUnambiguous) {
      // Convert to list form
      Annotation currentAnnotation = it.getNvc();  // save to restore position
      convertToListForm();
      pos = Collections.binarySearch(this.list, currentAnnotation, annotationComparator_withId);
      --this.pos;
      return;
    }

    // is ambiguous, not list form
    maybeMoveToPrevBounded();  // makes iterator invalid if moving before startId 

    adjustForStrictOrCoveringAndBoundSkip(IS_GOING_BACKWARDS); 
    
  // stop logic going backwards for various bounding cases
  // this is done by the maybeMoveToPrev call, where it compares to the saved startId
//    if (it.isValid()) {
//      // stop in bounded case if out of bounds going forwards UIMA-5063
//      switch(boundsUse) {
//      case notBounded: 
//        break;
//      case coveredBy:
//        break;
//      case covering:
//        maybeMakeItInvalid_bounds(it.getNvc(), a -> a.getBegin() > boundBegin);
//        break;
//      case sameBeginEnd:
//        maybeMakeItInvalidSameBeginEnd();
//        break;
//      }
//    } 
  }
  

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToFirstNoReinit()
   */
  @Override
  public void moveToFirstNoReinit() {
  
    if (isEmpty) {
      return;
    }
    
    if (isListForm) {
      this.pos = 0;
    } else {
      moveToStart();
    }
  }
  
  private void resetList() {
    if (isListForm) {
      isListForm = false;
      if (list != null) {
        list.clear();
      };      
    }
  }

  /*
   * This operation is relatively expensive one time for unambiguous
   * 
   * @see org.apache.uima.cas.FSIterator#moveToLastNoReinit()
   */
  @Override
  public void moveToLastNoReinit() {
//    if (isIndexesHaveBeenUpdated()) {
//      moveToFirst(); // done to recompute is empty, reset list, recompute bounds, etc.
//    }
    
    if (isEmpty) {
      return;
    }
    
    if (isUnambiguous && !isListForm) {
      convertToListForm();
    }
    
    if (isListForm) {
      this.pos = this.list.size() - 1;
    } else {
    
      // always bounded case because if unambig. case, above logic converted to list form and handled as list
      // and unambig is the only Subiterator case without bounds
      assert isBounded;

      switch (boundsUse) {
      case notBounded:
        Misc.internalError();  // never happen, because should always be bounded here
        break;

      case coveredBy:
        moveToJustPastBoundsAndBackup(boundEnd + 1, boundEnd + 1, a -> a.getBegin() > boundEnd);
        break;
      
      case covering:
        moveToJustPastBoundsAndBackup(boundBegin + 1, boundBegin + 1, a -> a.getBegin() > boundBegin);
        break;

      case sameBeginEnd:
        moveToJustPastBoundsAndBackup(boundBegin,  boundEnd + 1, a -> a.getEnd() != boundEnd);
        break;
      }
    } 
  }

  /**
   * Called by move to Last, to move to a place just beyond the last spot, and then backup
   * while the goBacwards is true
   * 
   * Includes adjustForStrictOrCoveringAndBoundSkip going backwards
   * @param begin a position just past the last spot
   * @param end a position just past the last spot
   * @param goBackwards when true, continue to backup
   */
  private void moveToJustPastBoundsAndBackup(int begin, int end, Predicate<Annotation> goBackwards) {
    it.moveToNoReinit(new Annotation(jcas, begin, end));
    if (it.isValid()) {
      Annotation a = it.getNvc();
      while (goBackwards.test(a)) {
        if (a._id == startId) {
          assert a.getBegin() <= boundEnd; // because it's non-empty
            // use this as the last
          break;  // no need to adjust for strict and bound skip because the startId has that incorporated
        } 
        it.moveToPreviousNvc();
        adjustForStrictOrCoveringAndBoundSkip(false);
        if (!it.isValid()) {
          break;
        }
        a = it.getNvc();
      }
    } else {
      it.moveToLastNoReinit();
      adjustForStrictOrCoveringAndBoundSkip(false);
    }
  }
  // default visibility - referred to by flat iterator
  static Comparator<AnnotationFS> getAnnotationBeginEndComparator(final int boundingBegin, final int boundingEnd) {
    return new Comparator<AnnotationFS>() {

      @Override
      public int compare(AnnotationFS o1, AnnotationFS o2) {
        AnnotationFS a = (o1 == null) ? o2 : o1;
        boolean isReverse = o1 == null;
        final int b;
        if ((b = a.getBegin()) < boundingBegin) {
          return isReverse? 1 : -1;
        }
        if (b > boundingBegin) {
          return isReverse? -1 : 1;
        }
        
        final int e;
        if ((e = a.getEnd()) < boundingEnd) {
          return isReverse? -1 : 1;
        }
        if (e > boundingEnd) {
          return isReverse? 1 : -1;
        }
        return 0;
      }
    };
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToNoReinit(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void moveToNoReinit(FeatureStructure fs) {
    if (! (fs instanceof Annotation)) {
      throw new IllegalArgumentException("Argument must be a subtype of Annotation");
    }
    
    if (isEmpty) return;
    
    Annotation fsa = (Annotation) fs;
    if (isUnambiguous && !isListForm) {  // unambiguous must be in list form
      convertToListForm();
    }
    
    // need to handle 4 cases of not-covered, covered-by, covering, and same
    
    if (isListForm) {
      // W A R N I N G - don't use it.xxx forms here, they don't work for list form
      // Don't need strict, skip-over-boundary, or type priority adjustments, 
      //   because these were done when the list form was created
      //   Want to move to leftmost
      pos = Collections.binarySearch(this.list, fsa, comparatorMaybeNoTypeWithoutId);
      int begin = fsa.getBegin();
      int end   = fsa.getEnd();
      Type type = fsa.getType();

      // Go back until we find a FS that is really smaller

      if (pos >= 0) {
        moveToPrevious();
      } else {
        // no exact match
        pos = (-pos) - 1;
        
        if (!isValid()) {
          // means the position is one beyond the end - where the insert should be.
          return;
        }
      }
      
      // next compare is without type if isUseTypePriority is false
      while (isValid() && 0 == comparatorMaybeNoTypeWithoutId.compare((Annotation)getNvc(), fsa)) {
        moveToPreviousNvc();
      }
        
      if (isValid()) {
        moveToNextNvc();  // backed up one to much          
      } else {
        moveToFirstNoReinit();
      }          

      return;
    } 
    
    
    // not list form
    // is ambiguous (because if unambiguous, would have been converted to list), may be strict.
    // Always bounded (if unbounded, that's only when subiterator is being used to 
    // implement "unambiguous", and that mode requires the "list" form above.)
    // can be one of 3 bounds: coveredBy, covering, and sameBeginEnd.
    
    it.moveToNoReinit(fs);  // may move before, within, or after bounds
    if (!it.isValid()) {
      makeInvalid();
      return;
    }
    
    if ((boundsUse == BoundsUse.coveredBy ||
         boundsUse == BoundsUse.sameBeginEnd) &&
        it.getNvc().getBegin() < boundBegin) {
      moveToFirstNoReinit();  
    } else if (isAboveBound()) {
      makeInvalid();
      return;
    }

    // next call will mark iterator invalid if goes out of bounds while adjusting
    adjustForStrictOrCoveringAndBoundSkip(IS_GOING_FORWARDS); // "covering" case adjustments
      
//      if (it.isValid()) {
//        // if beyond bound, mark invalid 
//        // if before bound, move to first
//        Annotation a = it.get();
//        
//        switch (boundsUse) {
//        case coveredBy:
//        case sameBeginEnd:
//          if (a.getBegin() > boundEnd) {
//            makeInvalid();
//          } else if (isUseTypePriority && annotationComparator.compare(a,  boundingAnnot) < 0) {
//            // with type priority, position is before bound.
//            moveToFirstNoReinit();
//          } else { // is not type priority case - see if too low
//            final int b = a.getBegin();
//            final int e = a.getEnd();
//            if (b < boundBegin ||  
//                ( b == boundBegin && e > boundEnd)) {
//              moveToFirstNoReinit();
//            } else if (isPositionUsesType && 
//                       b == boundBegin && 
//                       e == boundEnd &&
//                       a.getType() != boundType) {
//              /** Subiterator {0} has bound type: {1}, begin: {2}, end: {3}, for coveredBy, not using type priorities, matching FS with same begin end and different type {4}, cannot order these*/
//                     throw new CASRuntimeException(CASRuntimeException.SUBITERATOR_AMBIGUOUS_POSITION_DIFFERENT_TYPES, 
//                         this, boundType.getName(), b, e, a.getType().getName()); 
//                   }
//          }
//          break;
//        case covering:
//          break;
//        default:
//          Misc.internalError();
//        }
//      }
    
  }
  
  private boolean isBoundOk() {
    if ( ! it.isValid()) {
      return false;
    }
    return isBoundOkNvc();
  }
 
  private boolean isBoundOkNvc() {
    if ( ! it.isValid()) {
      return false;
    }
    if (boundsUse == BoundsUse.notBounded) {
      return true;
    }
    
    Annotation a = it.getNvc();
    int begin = a.getBegin();
    int end = a.getEnd();
    boolean ok;
    
    switch (boundsUse) {
    
    case covering:
      if (begin == boundBegin && end == boundEnd) {
        ok = (lto == null) || lto.lessThan(a._getTypeImpl(), boundType);
      } else {
        ok = begin <= boundBegin &&
             end    >= boundEnd;
      }
      break;
      
    case coveredBy:
      if (begin == boundBegin && end == boundEnd) {
        ok = (lto == null) || lto.lessThan(boundType, a._getTypeImpl());
      } else {
        ok = begin >= boundBegin && 
             begin <= boundEnd;
      }
      break;
             // getEnd not tested, doesn't play a role unless strict, which is tested elsewhere
//    case sameBeginEnd:  // equivalent to default
    case sameBeginEnd:
    default:       // the default case is sameBeginEnd 
      if (begin == boundBegin && end == boundEnd) {
        ok = (lto == null) || boundType == a._getTypeImpl();
      } else {
        ok = false;
      }
      break;
    }
    
    
    if (! ok) {
      makeInvalid();
    }
    return ok;
  }
  
  private boolean isAboveBound() {
    Annotation a = it.getNvc();
    switch (boundsUse) {
    case notBounded:
      return false;
    case covering:
      return a.getBegin() > boundBegin;
    case coveredBy:
      return a.getBegin() > boundEnd;
//    case sameBeginEnd:  // equivalent to default
    default:
      return a.getBegin() != boundBegin || a.getEnd() < boundEnd;
    }
  }
  
  /**
   * While adjusting, check for going out of bounds, and mark iterater invalid if so.
   * 
   * @param forward_or_backward - true for forward
   */
  private void adjustForStrictOrCoveringAndBoundSkip(boolean forward_or_backward) {
    if ( ! isValid() ) {
      return;
    }
    adjustForStrictOrCoveringAndBoundSkipNvc(forward_or_backward);
  }
  
  private void adjustForStrictOrCoveringAndBoundSkipNvc(boolean forward_or_backward) {
    if (boundsUse == BoundsUse.covering) {
      adjustForCoveringAndBoundSkip(forward_or_backward);
      return;
    }

    // this used for both coveredBy and sameBeginEnd
    adjustForStrict(forward_or_backward);  // skips if needed because end is out of range
    if (skipOverBoundingAnnot(forward_or_backward)) { // skips if match the bound
      adjustForStrict(forward_or_backward);  // re-do strict skip if above moved
    }
  }
  
  /**
   * Marks iterator invalid if out of bounds
   * @param forward_or_backwards true for forwards
   */ 
  private void adjustForCoveringAndBoundSkip(boolean forward_or_backwards) {
    adjustForCovering(forward_or_backwards);
    if (skipOverBoundingAnnot(forward_or_backwards)) {
      // may not be invalid, incase there are multiple FSs with same begin/end
      adjustForCovering(forward_or_backwards);
    }    
  }
  
  /**
   * Marks iterator invalid if move makes it beyond bounds
   * 
   * advance over FSs that are equal (2 choices) to the bounding annotation.
   * May leave the iterator in invalid state.
   * @param forward - true means move forward, false means move backwards
   * @return true if some move happened, false if nothing happened
   */
  private boolean skipOverBoundingAnnot(boolean forward) {
    boolean moved = false;
    if (isBounded && it.isValid()) { // skip if not bounded or iterator is invalid
      while (equalToBounds(it.getNvc())) {
        moved = true;
        if (forward) {
          it.moveToNextNvc();
        } else {
          maybeMoveToPrevBounded();
        }
        if (!it.isValid()) {
          break;
        }
        if ( ! isBoundOkNvc()) {
          return true;
        }        
      }
    }
    return moved;
  }
  
  /**
   * Special equalToBounds used only for having bounded iterators 
   * skip returning the bounding annotation
   * 
   * Two styles: uimaFIT style: only skip the exact one (id's the same)
   *             uima style: skip all that compare equal using the AnnotationIndex comparator
   * @param fs -
   * @return true if should be skipped
   */
  private boolean equalToBounds(Annotation fs) {
    if (fs._id == boundingAnnot._id) return true;
    
    if (isSkipSameBeginEndType) {
      return isBeginEndTypeEqual(fs, boundBegin, boundEnd, boundType);
    }
    return false;
  }
    
  private void maybeSetPrevEnd() {
    if (isUnambiguous && it.isValid()) {
      this.prevEnd = it.getNvc().getEnd();
    }    
  }
  
  
  /**
   * For strict mode (implies coveredby), move iterator until the end is within the bounding end
   * Check after any move if out-of-bounds, and mark invalid if so. 
   * 
   * While moving forwards,  mark iterator invalid if its begin > boundEnd.
   * While moving backwards, mark iterator invalid if begin < boundBegin.
   * While moving, skip over FSs that are equal (2 choices) to the bounding annotation. 
   */
  private void adjustForStrict(boolean forward) {
    if (!isValid()) {
      return;
    }
    
    if (isStrict) {
      Annotation item = it.getNvc();
      while (item.getEnd() > this.boundEnd) {
        if (forward) {
          it.moveToNextNvc();
          if (!isValid()) {
            return;
          }
          item = it.getNvc();
          if (item.getBegin() > this.boundEnd) {
            makeInvalid();
            return;
          }
        } else {
          maybeMoveToPrevBounded(); // makes iterator invalid if moving before startId
          if (!isValid()) {
            return;
          }
          item = it.getNvc();
        }
      }
    }
  }
  
  /**
   * Marks iterator invalid if moves beyond bound
   * 
   * when covering (which is different from coveredBy and sameBeginEnd,
   *  means get all annotations which span the bounds), 
   *  skip items where the end < bounds end,
   *    because those items don't span the bounding FS
   * 
   * Edge case: item with same begin and end is considered "covering"
   *              but subject to exclusion based on equalToBounds skipping
   * Cases:
   *   position of begin is after span begin - mark "invalid"
   *   position of begin is before or == span begin:
   *     position of end is == or > span end: OK
   *     position of end is < span end:
   *     if backward: moveToPrev until get valid position or run out.  if run out, mark invalid
   *     if forward: move to next until position of begin advances but is <= span begin.
   *      
   * @param forward
   */
  private void adjustForCovering(boolean forward) {
    if (!it.isValid()) {
      return;
    }
    // moveTo may move to invalid position
    // if the cur pos item has a begin beyond the bound, it cannot be a covering annotation
    if (it.getNvc().getBegin() > this.boundBegin) {
      makeInvalid();
      return;
    }
    
    // skip until get an FS whose end >= boundEnd, it is a candidate.
    //   stop if begin gets too large (going forwards)
    while (it.isValid() && (it.getNvc().getEnd() < this.boundEnd)) {
      if (forward) {
        it.moveToNextNvc();
        if (it.isValid() && it.getNvc().getBegin() > this.boundBegin) {
          makeInvalid();
          return;
        }
      } else {
        maybeMoveToPrevBounded();  // move to prev, if hit bound, makes invalid
      }
    }
  }
  
  /**
   * Assume: iterator is valid
   */
  private void maybeMoveToPrevBounded() {
    if (it.getNvc()._id == startId) {
      it.moveToFirstNoReinit();  // so next is invalid
    }
    it.moveToPreviousNvc();
  }
  
//  /**
//   * Adjust in case the underlying iterator is using type priorities, but we want to ignore them.
//   *   For the case where there's a moveTo(fs) operation, that didn't go to the left-most due to 
//   *   type priorities.
//   * if not using type priorities (but comparator has type priorities),
//   *   move backwards among equal begin/end/(maybe type) as current element,
//   *     stop if get to exact equal id, or hit iterator boundary
//   * @param exactId 0 or the id to stop the traversal on
//   * @return true if stopped on equal exactId
//   */
//  private boolean maybe_adjustToLeftmost_IgnoringTypePriority_boundingBegin(int exactId) {
//        
//    if (this.isUseTypePriority) {
//      return false; // skip if this subiterator is using type priorities
//    }
//    
//    // not using type priorities.
////    if (isPositionUsesType) {
//      // even though type priorities are not being used, need to go to leftmost of the same type
//      // as the bounding annotation.  No way to do this...  So not implementing this for now.
////    }
//
//    if (!this.underlying_iterator_using_typepriorities) {  // not using type priorities, but underlying iterator isn't either
//      return false;
//    }
//
//    // not using type priorities, but underlying iterator is.  
//    Annotation a = it.getNvc();
//    int begin = a.getBegin();
//    int end = a.getEnd();
////    Type type = a.getType();  // not using type priorities implies want to ignore type match
//    do {
//      int id = a._id;
//      if (id == exactId) {
//        return true;         // early stop for one caller
//      }
//      if (id == startId) { // start id may not be set yet; if so it is set to 0 so this test is always false
//        return false;  // iterator is at the start, can't move more
//      }
//      it.moveToPrevious();
//      if (!it.isValid()) {
//        it.moveToFirstNoReinit();  // not moveToStart - called by moveToStart
//        return false;  
//      }
//      a = it.getNvc();
//    } while (isBeginEndEqual(a, begin, end));
//    it.moveToNext();  // went back one to far
//    return false;
//  }
      
  private boolean isBeginEndTypeEqual(Annotation fs, int begin, int end, Type type) {
    if (fs.getBegin() != begin || fs.getEnd() != end) {
      return false;  
    }
    
    // begin and end compare
    
//    if (!isUseTypePriority) {
//      return true;  // if not using type priority, no compare with type
//    }
    
    // using type priority
//    if (!underlying_iterator_using_typepriorities) {
//      return true; // can't compare types if underlying iterator doesn't use type priorities
//                   // because that would require searching in both directions among == begin/end
//                   // values
//                   // TODO maybe implement a general search, among begin/end equal things?
//    }
//    // using type priorities, and the comparator has type priorities
    return fs.getType() == type;
  }
  
//  private boolean isBeginEndEqual(Annotation fs, int begin, int end) {
//    return fs.getBegin() == begin && fs.getEnd() == end;
//  }
  
  /**
   * For unambiguous, going forwards
   */
  private void movePastPrevAnnotation() {
    if (isUnambiguous) {
      while (it.isValid() && (it.get().getBegin() < this.prevEnd)) {
        it.moveToNext();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FSIterator<T> copy() {
    Subiterator<T> copy = new Subiterator<T>(
        this.it.copy(), 
        this.boundingAnnot, 
        ! this.isUnambiguous,
        this.isStrict,
        this.boundsUse,
        this.isUseTypePriority, 
        this.isSkipSameBeginEndType,
        
        this.startId,
        this.isEmpty,
        this.coveringStartPos,
        this.coveringEndPos);
    copy.list = this.list;  // non-final things
    copy.pos  = this.pos;
    copy.isListForm = this.isListForm;
    return copy;
  }

  
  /**
   * This is unsupported because its expensive to compute
   * in many cases, and may not be needed.
   */
  @Override
  public int ll_indexSizeMaybeNotCurrent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int ll_maxAnnotSpan() {
    if (isEmpty) {
      return 0;
    }
    return ((LowLevelIterator)it).ll_maxAnnotSpan();
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return ((LowLevelIterator<T>)it).ll_getIndex();
  }

  private void makeInvalid() {
    it.moveToFirstNoReinit();
    it.moveToPrevious();
  }

  /**
   * Used to determine when some precomputed things (e.g. listform) need to be recalculated
   * @return true if one or more of the underlying indexes of the underlying iterator have been updated
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return ((LowLevelIterator<?>)it).isIndexesHaveBeenUpdated();
  }

  @Override
  public boolean maybeReinitIterator() {
    if (it.maybeReinitIterator()) {
      resetList();
      moveToStartSetEmptyAndId();
      return true;
    }   
    return false;
  }

  @Override
  public Comparator<TOP> getComparator() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public int size() {
    FSIterator<T> it2 = copy();
    int sz = 0;
    while (it2.hasNext()) {
      sz++;
      it2.nextNvc();
    }
    return sz;
  }

  @Override
  public FeatureStructure[] getArray() {
    FSIterator<T> it2 = copy();
    ArrayList<FeatureStructure> a = new ArrayList<>();
    while (it2.hasNext()) {
      a.add(it2.nextNvc());
    }
    return a.toArray(new FeatureStructure[a.size()]);
  }

//  /**
//   * Simple implementation:
//   *   move to leftmost, then
//   *   advance until == or invalid
//   */
//  @Override
//  public void moveToExactNoReinit(FeatureStructure fs) {
//    moveToNoReinit(fs);
//    while (isValid()) {
//      T f = getNvc();
//      if (f == fs) {
//        return;
//      }
//      if (comparatorMaybeNoTypeWithoutId.compare((TOP)f, (TOP)fs) != 0) {
//        makeInvalid();
//        return;
//      }
//      moveToNextNvc();
//    }
//  }

//  // makes an extra copy of the items
//  private void initAmbiguousSubiterator(AnnotationFS annot, final boolean strict) {
//    final int start = (annot == null) ? 0 : annot.getBegin();
//    final int boundingEnd = (annot == null) ? Integer.MAX_VALUE : annot.getEnd();
//    if (annot == null) {
//      it.moveToFirst();
//    } else {
//      it.moveToNoReinit(annot);  // to "earliest" equal, or if none are equal, to the one just later than annot
//    }
//    
//    // This is a little silly, it skips 1 of possibly many indexed annotations if the earliest one is "equal"
//    //    (just means matching the keys) to the control annot  4/2015
//    if (it.isValid() && it.get().equals(annot)) {
//      it.moveToNext();
//    }
//    // Skip annotations whose start is before the start parameter.
//    // should never have any???
//    while (it.isValid() && it.get().getBegin() < start) {
//      it.moveToNext();
//    }
//    T current;
//    while (it.isValid()) {
//      current = it.get();
//      // If the start of the current annotation is past the boundingEnd parameter,
//      // we're done.
//      if (current.getBegin() > boundingEnd) {
//        break;
//      }
//      it.moveToNext();
//      if (strict && current.getEnd() > boundingEnd) {
//        continue;
//      }
//      this.list.add(current);
//    }
//  }
//
//  private void initUnambiguousSubiterator(AnnotationFS annot, final boolean strict) {
//    final int start = annot.getBegin();
//    final int boundingEnd = annot.getEnd();
//    it.moveToNoReinit(annot);
//    
//    if (it.isValid() && it.get().equals(annot)) {
//      it.moveToNext();
//    }
//    if (!it.isValid()) {
//      return;
//    }
//    annot = it.get();
//    // Skip annotations with begin positions before the given start
//    // position.
//    while (it.isValid() && ((start > annot.getBegin()) || (strict && annot.getEnd() > boundingEnd))) {
//      it.moveToNext();
//    }
//    // Add annotations.
//    if (!it.isValid()) {
//      return;
//    }
//    T current = null;
//    while (it.isValid()) {
//      final T next = it.get();
//      // If the next annotation overlaps, skip it. Don't check while there is no "current" yet.
//      if ((current != null) && (next.getBegin() < current.getEnd())) {
//        it.moveToNext();
//        continue;
//      }
//      // If we're past the boundingEnd, stop.
//      if (next.getBegin() > boundingEnd) {
//        break;
//      }
//      // We have an annotation that's within the boundaries and doesn't
//      // overlap
//      // with the previous annotation. We add this annotation if we're not
//      // strict, or the end position is within the limits.
//      if (!strict || next.getEnd() <= boundingEnd) {
//        current = next;
//        this.list.add(current);
//      }
//      it.moveToNext();
//    }
///** 
//* Called for begin/end compare, after moveTo(fs)
//* to eliminate the effect of the type order comparison before any adjustment for strict.
//* Move backwards while equal with begin/end iterator. 
//*/
//private void adjustAfterMoveToForBeginEndComparator(FeatureStructure aFs) {
// final Annotation fs = (Annotation) aFs;
// final int begin = fs.getBegin();
// final int end = fs.getEnd();
// 
// while (it.isValid()) {
//   Annotation item = it.getNvc();
//   if (item.getBegin() != begin || item.getEnd() != end) {
//     break;
//   }
//   it.moveToPrevious();  
// }
// // are one position too far, move back one
// if (it.isValid()) {
//   it.moveToNext();  
// } else {
//   moveToStart();
// }
//}

//  }

}
