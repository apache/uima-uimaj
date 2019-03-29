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
 *          * redesigned 8/2017 rechecked 3/2019
 *          *   for skipping, the type is always used, that is
 *          *   items are skipped over only if having the same type
 *          *****************************************
 *     - unambiguous (for no-bounds or coveredBy, only: skip over FSs whose begin is < previous one's end) 
 *     - strict (for coveredBy only: skip if end is > bounding Annotation's end)
 *   - positioning (for moveTo)
 *          *****************************************
 *          * redesigned 8/2017 revised 3/2019
 *          *   TypeOrder specification is always used
 *          *     by passing to underlying iterators as comparator
 *          *****************************************
 *         -- is automatic in underlying iterator: 
 *         -- see use of "lto" (linear type order) for use in this class, if not null
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
 *     - strict (coveredBy only)
 *     - covering (skip when end falls within bounds)
 *     
 *   moveTo(fs): uses isUseTypePriority
 *     - is passed to underlying iterator,
 *     - used to set internal comparator (for use with list forms)
 *     - used to set lto to null / non-null (Linear Type Order)
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
 *       skip if unambiguous (noBound or coveredBy only) and start is within prev span
 *       skip if strict (coveredBy only) and end lies outside of scope span
 *     
 *   - going backward:
 *       if unambiguous - convert to form 2
 *       skip if strict (coveredBy only) and end lies outside of scope span
 *       
 *   - going to particular fs (left most match)
 *       if unambiguous - convert to form 2
 *       skip (forward) if strict (coveredBy only) and end lies outside of scope span
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
  private final boolean isDoEqualsTest;
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
    if (this.isUnambiguous) {
      switch (this.boundsUse) {
      case notBounded:   // ok
      case coveredBy:    // ok
        break;
      default: throw new IllegalArgumentException("Unambiguous (NonOverlapping) specification only "
            + "allowed for notBounded or coveredBy subiterator specifications");
      }
    }

    if (strict) {
      if (BoundsUse.coveredBy != boundsUse && BoundsUse.sameBeginEnd != boundsUse) {
        throw new IllegalArgumentException("Strict (includeAnnotationsWithEndBeyondBounds = false)"
            + " is only allowed for coveredBy subiterator specification");
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

    isDoEqualsTest = (boundsUse == BoundsUse.coveredBy || boundsUse == BoundsUse.sameBeginEnd) && 
        this.boundingAnnot._inSetSortedIndex();

    if (boundsUse == BoundsUse.covering) {
      // compute start position and isEmpty setting
      int span = ((LowLevelIterator<?>)it).ll_maxAnnotSpan();  // an optimization, the largest end-begin annotation
      int begin = boundEnd - span;
      if (begin > boundBegin) {
        makeInvalid();
        coveringStartPos = null;
        isEmpty = true;
        startId = 0;
        return;
      }
      if (begin < 0) {
        begin = 0;
      }
      coveringStartPos = new Annotation(jcas, begin, Integer.MAX_VALUE);
    } else {
      coveringStartPos = null;  
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
      boolean isDoEqualsTest
      ) {
    
    this.it = (LowLevelIterator<Annotation>) it;
    this.boundingAnnot = boundingAnnot;  // could be same begin/end, coveredby, or covering
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
    this.startId = startId;
    this.isEmpty = isEmpty;
    if (isEmpty) {
      makeInvalid();
    }    
    this.isDoEqualsTest = isDoEqualsTest;
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
    this.list = new ArrayList<>();
    while (isValid()) {
      list.add(it.getNvc());
      moveToNext();  // does all the adjustments, so list has only appropriate elements
    }
    this.pos = 0;
    isListForm = true;  // do at end, so up to this point, iterator is not the list form style
  }
  
  
  /**
   * Move to the starting position of the sub iterator.
   * isEmpty may not yet be set.
   * Never list form when called.
   * 
   * Mimics regular iterators when moveToStart  moves the underlying iterator to a valid position 
   *   in front of a bound which is limiting on the left (coveredBy, sameBeginEnd) 
   */
  private void moveToStart() {
    
    switch (boundsUse) {
    
    case notBounded:
      it.moveToFirstNoReinit();
      break;
      
    case sameBeginEnd:
      it.moveToNoReinit(boundingAnnot);  
      if (it.isValid()) {
        // no need for mimic position if type priorities are in effect; moveTo will either
        //   find equal match and position to left most of the equal, including types, or
        //   not find equal match and position to next greater one, which won't match next test
        if (is_beyond_bounds_chk_sameBeginEnd()) { 
          this.isEmpty = true;  // iterator was made invalid
          return;
        }
        // skip over bounding annotation
        while (equalToBounds(it.getNvc())) {
          it.moveToNextNvc();
          if (!it.isValid()) {
            return;
          }
        }
      }
      return; // skip setting prev end - not used for sameBeginEnd
    
    case coveredBy:
      it.moveToNoReinit(boundingAnnot);
      //   if an annotation is present (found), position is on it, and if not,
      //   position is at the next annotation that is higher than (or invalid, if there is none)
      //     note that the next found position could be beyond the end.
      if (it.isValid()) {
        // skip over bounding annotation
        while (equalToBounds(it.getNvc())) {
          it.moveToNextNvc();
          if (! it.isValid()) {
            return;
          }
        }
        // adjust for strict
        if (adjustForStrictNvc_forward()) {
          if (is_beyond_bounds_chk_coveredByNvc()) {  // is beyond end iff annot.begin > boundEnd
            return; // iterator became invalid 
          }
        }
      }
      break;
    
    case covering:
      it.moveToNoReinit(coveringStartPos); // sufficiently before the bounds
      if (it.isValid()) {
        adjustForCovering_forward(); 
      }
      is_beyond_bounds_chk_covering(); // make invalid if ran off end
      return;  // skip setting prev end - not used for covering;
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
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#getNvc()
   */
  @Override
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
    // maybe move past previous annotation
    if (isUnambiguous) {               // skip until start > prev end
      while (it.isValid() && (it.getNvc().getBegin() < this.prevEnd)) {
        it.moveToNext();
      }
      if (it.isValid()) {
        this.prevEnd = it.getNvc().getEnd();
      }
    }

    // next section does 
    //   1. adjusting for invalid end positions (coveredBy and covering) and
    //   2. checks for out-of-bounds, including hitting the bound in covering
    switch (boundsUse) {
    case coveredBy:
      if (it.isValid() && adjustForStrictNvc_forward()) {
        boolean moved = false;
        while (equalToBounds(it.getNvc())) {
          it.moveToNextNvc();
          moved = true;
          if ( ! it.isValid()) {
            break;
          }
        }
        if (moved) {
          if (it.isValid()) {
            adjustForStrictNvc_forward();
          }
        }
        is_beyond_bounds_chk_coveredByNvc(); // is beyond end iff annot.begin > boundEnd
        return;  
      } // else is invalid
      return;
      
    case covering:
      adjustForCovering_forward();
      if (it.isValid() && equalToBounds(it.getNvc())) {
        makeInvalid();
      } else {
        is_beyond_bounds_chk_covering();  // does isvalid check
      }
      return;
      
    case sameBeginEnd:
      if (it.isValid()) {
        while (equalToBounds(it.getNvc())) {
          it.moveToNextNvc();
          if ( ! it.isValid()) {
            break;
          }
        }
      }
      is_beyond_bounds_chk_sameBeginEnd();  // does isValid check
      return;
    
    case notBounded:
    default:
      return;
    }   
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

    adjustForStrictOrCoveringAndBoundSkip_backwards();
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
   * for the first time, if the index is not already in list form, but cheap subsequently
   * 
   * @see org.apache.uima.cas.FSIterator#moveToLastNoReinit()
   */
  @Override
  public void moveToLastNoReinit() {
    
    if (isEmpty) {
      return;
    }
    
    if (isUnambiguous && !isListForm) {
      convertToListForm();
    }
    
    if (isListForm) {
      this.pos = this.list.size() - 1;
    } else {
    

      switch (boundsUse) {

      case coveredBy:
        moveToJustPastBoundsAndBackup(boundEnd + 1, Integer.MAX_VALUE, 
            // continue backing up if: 
            a -> a.getBegin() > boundEnd
              || (a.getBegin() == boundEnd && 
                    // back up if the item type < bound type to get to the maximal type
                  a.getEnd() == boundEnd && lto != null && lto.lessThan(boundType, a._getTypeImpl()))
            );
        // adjust for strict, and check if hit bound
        if (isStrict) {
          while (it.isValid() && it.getNvc().getEnd() > boundEnd) {
            maybeMoveToPrevBounded();
          }        
        }
        // skip over original bound if found
        while (it.isValid() && equalToBounds(it.getNvc())) {
          maybeMoveToPrevBounded(); 
        }
        break;
      
      case covering:
        moveToJustPastBoundsAndBackup(boundBegin + 1, Integer.MAX_VALUE, 
            a -> a.getBegin() > boundBegin ||                  // keep backing up while a.begin too big
                 (a.getBegin() == boundBegin && a.getEnd() < boundEnd) ||  // keep backing up while a.begin ==, but end is too small
                 (a.getBegin() == boundBegin && a.getEnd() == boundEnd &&  // if begin/end ==, check type order if exists.
                  lto != null && lto.lessThan(boundType, a._getTypeImpl()))
             );
       
        // skip over equal bounds
        while (it.isValid() && equalToBounds(it.getNvc())) {
          maybeMoveToPrevBounded();
        }
        // handle skipping cases where the end is < boundEnd
        while (it.isValid() && it.getNvc().getEnd() < boundEnd) {
          maybeMoveToPrevBounded();
        }
        break;

      case sameBeginEnd:
        moveToJustPastBoundsAndBackup(boundBegin + 1,  Integer.MAX_VALUE,
            // keep moving backwards if begin too large, or
            //   is ==, but end is too small, or
            //   is ==, and end is ==, but end type is too large.
            a -> a.getBegin() > boundBegin ||
                 (a.getBegin() == boundBegin &&
                  (a.getEnd() < boundEnd  ||
                   a.getEnd() == boundEnd && lto != null && lto.lessThan(boundType, a._getTypeImpl()))));
   
        // skip over original bound if found
        while (it.isValid() && equalToBounds(it.getNvc())) {
          maybeMoveToPrevBounded();
        }
        break;
  
      case notBounded:
        default:
        Misc.internalError();  // never happen, because should always be bounded here
        break;
      }
    } 
  }

  /**
   * Called by move to Last (only) and only for non-empty iterators, to move to a place just beyond the last spot, and then backup
   * while the goBacwards is true
   * 
   * Includes adjustForStrictOrCoveringAndBoundSkip going backwards
   * @param begin a position just past the last spot
   * @param end a position just past the last spot
   * @param continue_going_backwards when true, continue to backup
   */
  private void moveToJustPastBoundsAndBackup(int begin, int end, Predicate<Annotation> continue_going_backwards) {
    it.moveToNoReinit(new Annotation(jcas, begin, end));
    if (it.isValid()) {
      Annotation a = it.getNvc();
      while (continue_going_backwards.test(a)) {
        if (a._id == startId) {
          // the continue_going_backwards says the current position is too high, but
          // we're at the start position, which is a contradiction
          Misc.internalError();
        } 
        it.moveToPreviousNvc();
        if (!it.isValid()) {
          // this can't happen, unless the iterator is empty.
          // and that condition was tested earlier
          Misc.internalError();
        }
        a = it.getNvc();
      }
    } else {
      it.moveToLastNoReinit();
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
    Annotation fsa = (Annotation) fs;
    
    if (isEmpty) return;
    
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
//      int begin = fsa.getBegin();
//      int end   = fsa.getEnd();
//      Type type = fsa.getType();

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
        moveToNextNvc();  // backed up one too much          
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
      return;
    }
    
    Annotation a = it.getNvc();
    int begin = a.getBegin();
    int end;
    
    // skip some for covering or coveredby, and then check if beyond end.
    switch (boundsUse) {
    
    case covering:
             // condition true if need to move forwards because current spot is invalid
      while ((begin <= this.boundBegin &&  // stop if go too far
             a._id != boundingAnnot._id &&                                 // stop if hit bounding annot
             ((end = a.getEnd()) < this.boundEnd || 
              (end == this.boundEnd && 
               (lto != null && lto.lessThan(a._getTypeImpl(), this.boundType)))))) {               
        it.moveToNextNvc();
        if (! it.isValid()) {
          return;
        }
      }
      
      is_beyond_bounds_chk_coveringNvc();
      return;
      
    case coveredBy:
      // mimic move to first if before bounds
      if (begin  < boundBegin ||
          (begin == boundBegin &&
           ( ! isStrict ||
            ((end = a.getEnd()) > boundEnd ||
             (end == boundEnd && lto != null && lto.lessThan(a._getTypeImpl(), boundType)))))) {
        moveToFirstNoReinit();  // move to left most position
      } else {
        // skip over boundary
        while (equalToBounds(it.getNvc())) {
          it.moveToNext();
          if ( ! it.isValid()) {
            return;  // ran off the end
          }
        }
      }
   
      // strict moving: condition true if need to move forwards because current spot is invalid
      if (isStrict) {
        while ((begin = (a = it.getNvc()).getBegin()) <= this.boundEnd &&  // stop if begin > bound end
               ((end = a.getEnd()) > boundEnd ||
                end == boundEnd && lto != null && lto.lessThan(boundType, a._getTypeImpl()))) {
          it.moveToNextNvc();
          if (! it.isValid()) {
            return;
          }
        }
      }
      // check if beyond the bounds
      is_beyond_bounds_chk_coveredByNvc();  // is beyond end iff annot.begin > boundEnd
      return;
      
    case sameBeginEnd:
      // mimic move to first if before bound
      //  use case: a bunch of same begin ends, different types
      //              no type order, bounding annot == same begin end: means all
      //              with type order, bounding annot == same begin end:  some might be in front
      //                with ! isSameBeginEndType  (skip over bounds with == id) <<< no need to test, is done later by caller
      if (begin < boundBegin || 
          (begin == boundBegin && 
           ((end = a.getEnd()) > boundEnd ||
            (end == boundEnd && lto != null && lto.lessThan(a._getTypeImpl(), boundType))))) {
        moveToFirstNoReinit();
      } else {
        // skip over boundary
        while (equalToBounds(it.getNvc())) {
          it.moveToNext();
          if ( ! it.isValid()) {
            return;  // ran off the end
          }
        }
      }
      // check if beyond the bounds
      is_beyond_bounds_chk_sameBeginEndNvc();
      return;
      
    case notBounded:
      default:  // for notBounded or sameBeginEnd, no need to skip some or check beyond end (was already done)
        return;
    }
  }
  
    
  /**
   * @return true if iterator was outside - beyond, and therefore, made invalid
   *              or iterator is invalid to start with
   */
  private boolean is_beyond_bounds_chk_sameBeginEnd() {
    if (it.isValid()) {
      return is_beyond_bounds_chk_sameBeginEndNvc();
    }
    return true;
  }
  
  private boolean is_beyond_bounds_chk_sameBeginEndNvc() {
    
    Annotation a = it.getNvc();
    if (a.getBegin() != this.boundBegin ||
        a.getEnd() != this.boundEnd ||
        (isUseTypePriority && a._getTypeImpl() != boundType)) {
      makeInvalid();
      return true;
    } else return false;
   
  }
  
  /**
   * @return true if iterator was outside - beyond, and therefore, made invalid
   *              or iterator is invalid to start with
   */
  private boolean is_beyond_bounds_chk_coveredByNvc() {
    
    if (it.getNvc().getBegin() > boundEnd) {
      makeInvalid();
      return true;
    }
    return false;
  }

  
  /**
   * @return true if iterator was outside - beyond, and therefore, made invalid
   *              or iterator is invalid to start with
   *              
   *   For covering case, if the annotation is equal to the bounding one, it's considered outside.
   *      Use case: a bunch of same begin / end,  one in middle is bounding one, and 
   *                move To is to left of it, or to right of it , without typePriorities
   *                  to left: is ok, to right, is outside but without type priorities,
   *                     all are considered == to the bound                 
   */
  private boolean is_beyond_bounds_chk_covering() {
    if ( ! it.isValid()) {
      return true; 
    }
    return is_beyond_bounds_chk_coveringNvc();
  }
  
  private boolean is_beyond_bounds_chk_coveringNvc() {
    Annotation a = it.getNvc();
    int begin = a.getBegin();
    int end = a.getEnd();
  
    if (begin > this.boundBegin ||
        (begin == this.boundBegin &&
         (end < boundEnd ||
          (end == boundEnd && lto != null && lto.lessThan(a._getTypeImpl(),  boundType))))) {
      makeInvalid();
      return true;
    } else { 
      return false;
    }
  }
  
  private void adjustForStrictOrCoveringAndBoundSkip_backwards() {
    switch(boundsUse) {

    case coveredBy:
      // handle strict
      if (isStrict) {
        while (it.isValid() && it.getNvc().getEnd() > boundEnd) {
          maybeMoveToPrevBounded();
        }        
      }
      // skip over original bound if found
      while (it.isValid() && equalToBounds(it.getNvc())) {
        maybeMoveToPrevBounded();   // can be multiple equal to bounds
      }
      break;
      
    case covering:
      // because this method is move to previous, 
      //   the position cannot be on the bounds.
      // handle skipping cases where the end is < boundEnd
      while (it.isValid() && it.getNvc().getEnd() < boundEnd) {
        maybeMoveToPrevBounded();
      }
      break;
      
    case sameBeginEnd:
      while (it.isValid() && equalToBounds(it.getNvc())) {
        maybeMoveToPrevBounded(); // can be multiple equal to bounds
      }
      break;
      
      default:  // same as no bounds case
    }    
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
    return isDoEqualsTest && fs._id == boundingAnnot._id ||
           (isSkipSameBeginEndType &&
            fs.getBegin() == boundBegin &&
            fs.getEnd() == boundEnd &&
            fs.getType() == boundType); 
  }
    
  private void maybeSetPrevEnd() {
    if (isUnambiguous && it.isValid()) {
      this.prevEnd = it.getNvc().getEnd();
    }    
  }
  
    
  /**
   * 
   * @param forward
   * @return true if iterator still valid, false if not valid
   */
  private boolean adjustForStrictNvc_forward() {    
    if (isStrict) {
      Annotation item = it.getNvc();
      while (item.getEnd() > this.boundEnd) {
        
        it.moveToNextNvc();
        if (!isValid()) {
          return false;
        }
        item = it.getNvc();
        if (item.getBegin() > this.boundEnd) { // not >= because could of 0 length annot at end
          makeInvalid();
          return false;
        }
        
      }
      return true;
    } else {
      return true;
    }
  }
  
  /**
   * Assume: on entry the subiterator might not be in a "valid" position
   *   Case: moveToJustPastBoundsAndBackup...
   * 
   * Adjust: skip over annotations whose "end" is < bound end, or
   *   whose end is == to bound end and begin == bound begin 
   *     and linearTypeOrder exists, 
   *     and annotation lessThan the bound type.
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
   *     position of end is == or > span end: OK (except if begin is ==, then do lto check)
   *     position of end is < span end:
   *       if backward: moveToPrev until get valid position or run out.  if run out, mark invalid
   *       if forward: move to next while position of begin is <= span begin.
   *      
   * @param forward true if moving forward
   */
  private void adjustForCovering_forward() {
    if (!it.isValid()) {
      return;
    }
    Annotation a = it.getNvc();
    int begin = a.getBegin();
    int end = a.getEnd();
    
    // moveTo may move to invalid position
    // if the cur pos item has a begin beyond the bound, it cannot be a covering annotation
    if (begin > this.boundBegin) {
      makeInvalid();
      return;
    }
    
    // begin is <= bound begin
      
    // skip until get an FS whose end >= boundEnd, it is a candidate.
    //   stop if begin gets too large (going forwards)
    // while test: is true if need to move to skip over a too-small "end"
    while (it.isValid() && 
           (begin = (a = it.getNvc()).getBegin()) <= this.boundBegin &&
           ( (end = a.getEnd()) < this.boundEnd || 
             (end == this.boundEnd && lto != null && lto.lessThan(a._getTypeImpl(), this.boundType)))) {
      it.moveToNextNvc();
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
          
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FSIterator<T> copy() {
    Subiterator<T> copy = new Subiterator<>(
        this.it.copy(),
        this.boundingAnnot,
        !this.isUnambiguous,
        this.isStrict,
        this.boundsUse,
        this.isUseTypePriority,
        this.isSkipSameBeginEndType,

        this.startId,
        this.isEmpty,
        this.coveringStartPos,
        this.isDoEqualsTest);
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
    return comparatorMaybeNoTypeWithoutId;
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
