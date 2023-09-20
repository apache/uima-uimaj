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

import static java.lang.Integer.MAX_VALUE;
import static org.apache.uima.cas.impl.Subiterator.BoundsUse.coveredBy;
import static org.apache.uima.cas.impl.Subiterator.BoundsUse.covering;
import static org.apache.uima.cas.impl.Subiterator.BoundsUse.notBounded;
import static org.apache.uima.cas.impl.Subiterator.BoundsUse.sameBeginEnd;
import static org.apache.uima.cas.text.AnnotationPredicates.coveredBy;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;
import static org.apache.uima.cas.text.AnnotationPredicates.overlappingAtEnd;

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

//@formatter:off
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
 */
//@formatter:on
public class Subiterator<T extends AnnotationFS> implements LowLevelIterator<T> {

  public enum BoundsUse {
    coveredBy, // iterate within bounds specified by controlling Feature Structure
    covering, // iterate over FSs which cover the bounds specified by controlling Feature Structure
    sameBeginEnd, // iterate over FSs having the same begin and end
    notBounded,
  }

  /*
   * The generic type of this is Annotation, not T, because that allows the use of a comparator
   * which is comparing a key which is not T but some super type of T.
   */
  private ArrayList<Annotation> list = null; // used for form 2, lazily initialized

  private int pos = 0; // used for form 2

  private final LowLevelIterator<Annotation> it;

  /** the bounding annotation need not be a subtype of T */
  private final Annotation boundingAnnot;
  private final int boundBegin;
  private final int boundEnd;

  private final Annotation originalBoundingAnnotation;
  private final int originalBoundBegin;
  private final int originalBoundEnd;
  private final TypeImpl boundType;

  private final Annotation coveringStartPos;

  /** true means need to skip until start is past prev end (going forward) */
  private final boolean isUnambiguous;
  /**
   * only true for coveredby; means skip things while iterating where the end is outside the bounds
   */
  private final boolean isStrict;
  /** true if bounds is one of sameBeginEnd, coveredBy, covering */
  private final boolean isBounded;
  /** for moveTo-leftmost, and bounds skipping if isSkipEquals */
  private final boolean isUseTypePriority;
  // private final boolean underlying_iterator_using_typepriorities;
  // /** two uses: bounds skip if isSkipEqual, move-to-leftmost if !isUseTypePriority */
  // private final boolean isPositionUsesType;
  /** for bounds skipping alternative */
  private final boolean isSkipSameBeginEndType;
  private final boolean isDoEqualsTest;
  /** one of notBounded, sameBeginEnd, coveredBy, covering */
  private final BoundsUse boundsUse;
  private final boolean isIncludesAnnotationsStartingAtEndPosition;
  private final boolean isIncludeZeroWidthAtBegin;
  private final boolean isIncludeZeroWidthAtEnd;

//@formatter:off
  /**
   * isEmpty is a potentially expensive calculation, involving potentially traversing all the iterators in a particular type hierarchy
   * It is only done:
   *   - at init time
   *   - at moveToFirst/last/FS - these can cause regenerating the copy-on-write objects via the getNonNullCow call
   *       if no new nonNullCow was obtained, no need to recalculate empty
   */
//@formatter:on
  private boolean isEmpty;

  private int prevBegin = -1; // for unambiguous iterators
  private int prevEnd = -1; // for unambiguous iterators

  /**
   * list form is recalculated at moveToFirst/last/fs, same as isEmpty
   */
  private boolean isListForm = false;

  /**
   * startId is recalculated at moveToFirst/last/fs, same as isEmpty
   */
  private int startId;

  /** null if ! isTypePriority */
  private final LinearTypeOrder lto;

  // only used in moveTo, in listform
  // if ignoreTypeOrdering, use that form of comparator
  private final Comparator<TOP> comparatorMaybeNoTypeWithoutId; // implements compare(fs1, fs2) for
                                                                // begin/end/type priority

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
   * @param it
   *          the iterator to use, positioned to the correct starting place
   * @param boundingAnnot
   *          null or the bounding annotation
   * @param originalBoundingAnnotation
   *          an original bounding annotation which will not be returned or not depending on the
   *          conditions for the bounding annotation to be returned. This can be useful if the
   *          original selection was converted to another selection type, e.g. when converting a
   *          following-selection to a covered-by selection under the condition that the original
   *          boundary of the following-selection (the start position) should be excluded. If set,
   *          the bounding type is obtained from this annotation instead of the bounding annotation
   * @param ambiguous
   *          false means to skip annotations whose begin lies between previously returned begin
   *          (inclusive) and end (exclusive)
   * @param strict
   *          true means to skip annotations whose end is greater than the bounding end position
   *          (ignoring type priorities)
   * @param boundsUse
   *          null if no bounds, boundingAnnot used for start position if non-null
   * @param isUseTypePriority
   *          false to ignore type priorities, and just use begin/end and maybe type
   * @param isSkipSameBeginEndType
   *          used only for coveredBy or covering case, false means to only skip returning an FS if
   *          it has the same id() as the bounding fs
   * @param isIncludeZeroWidthAtBegin
   *          for a covered-by selection to tell if a zero-width annotation at the start should be
   *          included.
   * @param isIncludeZeroWidthAtEnd
   *          for a covered-by selection to tell if a zero-width annotation at the start should be
   *          included.
   */
  Subiterator(FSIterator<T> it, AnnotationFS boundingAnnot, AnnotationFS originalBoundingAnnotation,
          boolean ambiguous, boolean strict, // omit FSs whose end > bounds, requires boundsUse ==
                                             // boundedby
          BoundsUse boundsUse, // null if no bounds; boundingAnnot used for start position if
                               // non-null
          boolean isUseTypePriority, boolean isSkipSameBeginEndType, // useAnnotationEquals
          boolean isNonStrictIncludesAnnotationsStartingAtEndPosition,
          boolean isIncludeZeroWidthAtBegin, boolean isIncludeZeroWidthAtEnd) {

    this.it = (LowLevelIterator<Annotation>) it;

    this.boundingAnnot = (Annotation) boundingAnnot; // could be same begin/end, coveredby, or
                                                     // covering
    this.originalBoundingAnnotation = (Annotation) ((originalBoundingAnnotation != null)
            ? originalBoundingAnnotation
            : boundingAnnot);
    isBounded = boundsUse != null && boundsUse != BoundsUse.notBounded;
    this.boundsUse = (boundsUse == null) ? BoundsUse.notBounded : boundsUse;
    if (isBounded && (null == boundingAnnot || !(boundingAnnot instanceof Annotation))) {
      Misc.internalError(
              new IllegalArgumentException("Bounded Subiterators require a bounding annotation"));
    }
    boundBegin = isBounded ? this.boundingAnnot.getBegin() : -1;
    boundEnd = isBounded ? this.boundingAnnot.getEnd() : -1;
    boundType = isBounded ? (TypeImpl) this.originalBoundingAnnotation.getType() : null;
    originalBoundBegin = isBounded ? this.originalBoundingAnnotation.getBegin() : -1;
    originalBoundEnd = isBounded ? this.originalBoundingAnnotation.getEnd() : -1;

    isIncludesAnnotationsStartingAtEndPosition = isNonStrictIncludesAnnotationsStartingAtEndPosition;
    this.isIncludeZeroWidthAtBegin = isIncludeZeroWidthAtBegin;
    this.isIncludeZeroWidthAtEnd = isIncludeZeroWidthAtEnd;

    isUnambiguous = !ambiguous;
    if (isUnambiguous) {
      switch (this.boundsUse) {
        case notBounded: // ok
        case coveredBy: // ok
          break;
        default:
          throw new IllegalArgumentException("Unambiguous (NonOverlapping) specification only "
                  + "allowed for notBounded or coveredBy subiterator specifications");
      }
    }

    if (strict) {
      if (BoundsUse.coveredBy != boundsUse && BoundsUse.sameBeginEnd != boundsUse) {
        throw new IllegalArgumentException("Strict (includeAnnotationsWithEndBeyondBounds = false)"
                + " is only allowed for coveredBy subiterator specification");
      }
    }
    isStrict = strict;
    this.isSkipSameBeginEndType = isSkipSameBeginEndType;

    FSIndexRepositoryImpl ir = this.it.ll_getIndex().getCasImpl().indexRepository;
    // underlying_iterator_using_typepriorities = ir.isAnnotationComparator_usesTypeOrder();

    if (boundsUse == BoundsUse.covering && isUseTypePriority) {
      throw new IllegalArgumentException(
              "Cannot specify isUseTypePriority with BoundsUse.covering");
    }

    this.isUseTypePriority = isUseTypePriority;
    lto = isUseTypePriority ? ir.getDefaultTypeOrder() : null;

    comparatorMaybeNoTypeWithoutId = ir.getAnnotationFsComparator(FSComparators.WITHOUT_ID,
            isUseTypePriority ? FSComparators.WITH_TYPE_ORDER : FSComparators.WITHOUT_TYPE_ORDER);
    annotationComparator_withId = ir.getAnnotationFsComparatorWithId();

    jcas = (JCasImpl) ll_getIndex().getCasImpl().getJCas();

    isDoEqualsTest = (boundsUse == coveredBy || boundsUse == sameBeginEnd || boundsUse == covering)
            && this.originalBoundingAnnotation._inSetSortedIndex();

    if (boundsUse == BoundsUse.covering) {
      // compute start position and isEmpty setting
      int span = ((LowLevelIterator<?>) it).ll_maxAnnotSpan(); // an optimization, the largest
                                                               // end-begin annotation
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
      coveringStartPos = SelectFSs_impl.makePosAnnot(jcas, begin, Integer.MAX_VALUE);
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
   * 
   * @param it
   *          -
   * @param boundingAnnot
   *          -
   * @param ambiguous
   *          -
   * @param strict
   *          -
   * @param boundsUse
   *          -
   * @param isUseTypePriority
   *          -
   * @param isSkipSameBeginEndType
   *          -
   * @param startId
   *          -
   * @param isEmpty
   *          -
   */
  Subiterator(FSIterator<Annotation> it, Annotation boundingAnnot,
          Annotation originalBoundingAnnotation, boolean ambiguous, boolean strict, // omit FSs
                                                                                    // whose end >
                                                                                    // bounds
          BoundsUse boundsUse, // null if boundingAnnot being used for starting position in
                               // unambiguous iterator
          boolean isUseTypePriority, boolean isSkipSameBeginEndType, int startId, boolean isEmpty,
          Annotation coveringStartPos, boolean isDoEqualsTest,
          boolean isStrictIncludesAnnotationsStartingAtEndPosition,
          boolean isIncludeZeroWidthAtBegin, boolean isIncludeZeroWidthAtEnd) {

    this.it = (LowLevelIterator<Annotation>) it;
    isIncludesAnnotationsStartingAtEndPosition = isStrictIncludesAnnotationsStartingAtEndPosition;
    this.isIncludeZeroWidthAtBegin = isIncludeZeroWidthAtBegin;
    this.isIncludeZeroWidthAtEnd = isIncludeZeroWidthAtEnd;
    this.boundingAnnot = boundingAnnot; // could be same begin/end, coveredby, or covering
    this.originalBoundingAnnotation = originalBoundingAnnotation;
    isBounded = boundsUse != null && boundsUse != BoundsUse.notBounded;
    this.boundsUse = (boundsUse == null) ? BoundsUse.notBounded : boundsUse;
    isUnambiguous = !ambiguous;
    if (strict) {
      if (BoundsUse.coveredBy != boundsUse && BoundsUse.sameBeginEnd != boundsUse) {
        throw new IllegalArgumentException(
                "Strict requires BoundsUse.coveredBy or BoundsUse.sameBeginEnd");
      }
    }
    isStrict = strict;
    this.isSkipSameBeginEndType = isSkipSameBeginEndType;

    boundBegin = isBounded ? boundingAnnot.getBegin() : -1;
    boundEnd = isBounded ? boundingAnnot.getEnd() : -1;
    boundType = isBounded ? (TypeImpl) originalBoundingAnnotation.getType() : null;
    originalBoundBegin = isBounded ? this.originalBoundingAnnotation.getBegin() : -1;
    originalBoundEnd = isBounded ? this.originalBoundingAnnotation.getEnd() : -1;

    FSIndexRepositoryImpl ir = this.it.ll_getIndex().getCasImpl().indexRepository;
    // underlying_iterator_using_typepriorities = ir.isAnnotationComparator_usesTypeOrder();

    this.isUseTypePriority = isUseTypePriority;
    lto = isUseTypePriority ? ir.getDefaultTypeOrder() : null;

    comparatorMaybeNoTypeWithoutId = ir.getAnnotationFsComparator(FSComparators.WITHOUT_ID,
            isUseTypePriority ? FSComparators.WITH_TYPE_ORDER : FSComparators.WITHOUT_TYPE_ORDER);
    annotationComparator_withId = ir.getAnnotationFsComparatorWithId();

    jcas = (JCasImpl) ll_getIndex().getCasImpl().getJCas();

    this.coveringStartPos = coveringStartPos;
    this.startId = startId;
    this.isEmpty = isEmpty;
    if (isEmpty) {
      makeInvalid();
    }
    this.isDoEqualsTest = isDoEqualsTest;
  }

//@formatter:off
  /**
   * Converting to list form - called for 
   *   unambiguous iterator going backwards, 
   *   unambiguous iterator doing a moveTo(fs) operation
   *   unambiguous iterator doing a moveToLast() operation 
   */
//@formatter:on
  private void convertToListForm() {
    // moves to the start annotation, including moving past equals for annot style,
    // and accommodating strict
    moveToStart();
    list = new ArrayList<>();
    while (isValid()) {
      list.add(it.getNvc());
      // does all the adjustments, so list has only appropriate elements
      moveToNextNvc();
    }
    pos = 0;
    // do at end, so up to this point, iterator is not the list form style
    isListForm = true;
  }

  /**
   * Move to the starting position of the sub iterator.
   */
  private void moveToStart() {
    // If the subiterator would be in list form here, we would have to call moveTo instead of
    // moveTo_iterators. Also, we would get a stack overflow because toListForm actually calls this
    // method and moveTo in turn may call toListForm.
    assert !isListForm : "Must not be in list form at this point!";

    prevBegin = -1;
    prevEnd = -1;

    switch (boundsUse) {
      case notBounded:
        it.moveToFirstNoReinit();
        maybeSetPrevBounds(); // used for unambiguous
        break;
      case sameBeginEnd:
        moveTo_iterators(boundingAnnot, true);
        break;
      case coveredBy:
        moveTo_iterators(boundingAnnot, true);
        break;
      case covering:
        moveTo_iterators(coveringStartPos, true);
        break;
    }
  }

  @Override
  public boolean isValid() {
    if (isListForm) {
      return (pos >= 0) && (pos < list.size());
    }
    // assume all non-list form movements leave the underlying iterator
    // positioned either as invalid, or at the valid spot including the bounds, for
    // a get();

    return it.isValid();
  }

  @Override
  public T getNvc() {
    if (isListForm) {
      return (T) list.get(pos);
    } else {
      return (T) it.getNvc();
    }
  }

  @Override
  public void moveToNextNvc() {
    // no isValid check because caller checked: "Nvc"
    if (isListForm) {
      ++pos;
      // maybeSetPrevEnd not needed because list form already accounted for unambiguous
      return;
    }

    it.moveToNextNvc();

    // maybe move past previous annotation
    if (isUnambiguous) {
      // Skip while the current annotation is still overlapping with the previous one
      while (it.isValid() && overlapping(it.getNvc(), prevBegin, prevEnd)) {
        it.moveToNext();
      }
    }

    // next section does
    // 1. adjusting for invalid end positions (coveredBy and covering) and
    // 2. checks for out-of-bounds, including hitting the bound in covering
    switch (boundsUse) {
      case coveredBy:
        if (it.isValid() && adjustForStrictNvc_forward()) {
          boolean moved = false;
          while (equalToBounds(it.getNvc())) {
            it.moveToNextNvc();
            moved = true;
            if (!it.isValid()) {
              break;
            }
          }
          if (moved) {
            if (it.isValid()) {
              adjustForStrictNvc_forward();
            }
          }
          if (it.isValid()) {
            is_beyond_bounds_chk_coveredByNvc(); // is beyond end iff annot.begin > boundEnd
          }
          maybeSetPrevBounds(); // used for unambiguous
          return;
        } // else is invalid
        return;

      case covering:
        adjustForCovering_forward();
        if (it.isValid() && equalToBounds(it.getNvc())) {
          makeInvalid();
        } else {
          is_beyond_bounds_chk_covering(); // does isvalid check
        }
        maybeSetPrevBounds(); // used for unambiguous
        return;

      case sameBeginEnd:
        if (it.isValid()) {
          while (equalToBounds(it.getNvc())) {
            it.moveToNextNvc();
            if (!it.isValid()) {
              break;
            }
          }
        }
        is_beyond_bounds_chk_sameBeginEnd(); // does isValid check
        maybeSetPrevBounds(); // used for unambiguous
        return;

      case notBounded:
      default:
        maybeSetPrevBounds(); // used for unambiguous
        return;
    }
  }

  @Override
  public void moveToPreviousNvc() {
    // no isValid check because caller checked: "Nvc"
    if (isListForm) {
      --pos;
      return;
    }

    if (isUnambiguous) {
      // Convert to list form
      Annotation currentAnnotation = it.getNvc(); // save to restore position
      convertToListForm();
      pos = Collections.binarySearch(list, currentAnnotation, annotationComparator_withId);
      --pos;
      return;
    }

    // is ambiguous, not list form
    maybeMoveToPrevBounded(); // makes iterator invalid if moving before startId

    adjustForStrictOrCoveringAndBoundSkip(true);
  }

  @Override
  public void moveToFirstNoReinit() {

    if (isEmpty) {
      return;
    }

    if (isListForm) {
      pos = 0;
    } else {
      moveToStart();
    }
  }

  private void resetList() {
    if (isListForm) {
      isListForm = false;
      if (list != null) {
        list.clear();
      }
      ;
    }
  }

  /*
   * This operation is relatively expensive one time for unambiguous for the first time, if the
   * index is not already in list form, but cheap subsequently
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
      pos = list.size() - 1;
    } else {

      switch (boundsUse) {

        case coveredBy:
          moveToJustPastBoundsAndBackup(boundEnd + 1, MAX_VALUE,
                  // continue backing up if:
                  a -> a.getBegin() > boundEnd || (!isIncludesAnnotationsStartingAtEndPosition
                          && a.getBegin() < a.getEnd() && a.getBegin() == boundEnd) ||
                  // back up if the item type < bound type to get to the maximal type
                          (a.getBegin() == boundEnd && a.getEnd() == boundEnd && lto != null
                                  && lto.lessThan(boundType, a._getTypeImpl())));
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
          moveToJustPastBoundsAndBackup(boundBegin + 1, MAX_VALUE, a -> a.getBegin() > boundBegin || // keep
                                                                                                     // backing
                                                                                                     // up
                                                                                                     // while
                                                                                                     // a.begin
                                                                                                     // too
                                                                                                     // big
                  (a.getBegin() == boundBegin && a.getEnd() < boundEnd) || // keep backing up while
                                                                           // a.begin ==, but end is
                                                                           // too small
                  (a.getBegin() == boundBegin && a.getEnd() == boundEnd && // if begin/end ==, check
                                                                           // type order if exists.
                          lto != null && lto.lessThan(boundType, a._getTypeImpl())));

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
          moveToJustPastBoundsAndBackup(boundBegin + 1, MAX_VALUE,
                  // keep moving backwards if begin too large, or
                  // is ==, but end is too small, or
                  // is ==, and end is ==, but end type is too large.
                  a -> a.getBegin() > boundBegin || (a.getBegin() == boundBegin
                          && (a.getEnd() < boundEnd || a.getEnd() == boundEnd && lto != null
                                  && lto.lessThan(boundType, a._getTypeImpl()))));

          // skip over original bound if found
          while (it.isValid() && equalToBounds(it.getNvc())) {
            maybeMoveToPrevBounded();
          }
          break;

        case notBounded:
        default:
          Misc.internalError(); // never happen, because should always be bounded here
          break;
      }
    }
  }

  /**
   * Called by move to Last (only) and only for non-empty iterators, to move to a place just beyond
   * the last spot, and then backup while the goBackwards is true
   * 
   * Includes adjustForStrictOrCoveringAndBoundSkip going backwards
   * 
   * @param begin
   *          a position just past the last spot
   * @param end
   *          a position just past the last spot
   * @param continue_going_backwards
   *          when true, continue to backup
   */
  private void moveToJustPastBoundsAndBackup(int begin, int end,
          Predicate<Annotation> continue_going_backwards) {
    it.moveToNoReinit(SelectFSs_impl.makePosAnnot(jcas, begin, end));

    if (!it.isValid()) {
      it.moveToLastNoReinit();
    }

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
  }

  // default visibility - referred to by flat iterator
  static Comparator<AnnotationFS> getAnnotationBeginEndComparator(final int boundingBegin,
          final int boundingEnd) {
    return new Comparator<AnnotationFS>() {

      @Override
      public int compare(AnnotationFS o1, AnnotationFS o2) {
        AnnotationFS a = (o1 == null) ? o2 : o1;
        boolean isReverse = o1 == null;
        final int b;
        if ((b = a.getBegin()) < boundingBegin) {
          return isReverse ? 1 : -1;
        }
        if (b > boundingBegin) {
          return isReverse ? -1 : 1;
        }

        final int e;
        if ((e = a.getEnd()) < boundingEnd) {
          return isReverse ? -1 : 1;
        }
        if (e > boundingEnd) {
          return isReverse ? 1 : -1;
        }
        return 0;
      }
    };
  }

  @Override
  public void moveToNoReinit(FeatureStructure fs) {
    if (isEmpty) {
      return;
    }

    // unambiguous must be in list form
    if (isUnambiguous && !isListForm) {
      convertToListForm();
    }

    if (isListForm) {
      moveTo_listForm(fs);
      return;
    }

    // not list form
    // is ambiguous (because if unambiguous, would have been converted to list), may be strict.
    // Always bounded (if unbounded, that's only when subiterator is being used to
    // implement "unambiguous", and that mode requires the "list" form above.)
    // can be one of 3 bounds: coveredBy, covering, and sameBeginEnd.
    moveTo_iterators(fs, false);
  }

  private void moveTo_listForm(FeatureStructure fs) {
    Annotation fsa = (Annotation) fs;

    // W A R N I N G - don't use it.xxx forms here, they don't work for list form
    // Don't need strict, skip-over-boundary, or type priority adjustments,
    // because these were done when the list form was created
    // Want to move to leftmost
    pos = Collections.binarySearch(list, fsa, comparatorMaybeNoTypeWithoutId);
    // int begin = fsa.getBegin();
    // int end = fsa.getEnd();
    // Type type = fsa.getType();

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
    while (isValid() && 0 == comparatorMaybeNoTypeWithoutId.compare((Annotation) getNvc(), fsa)) {
      moveToPreviousNvc();
    }

    if (isValid()) {
      moveToNextNvc(); // backed up one too much
    } else {
      moveToFirstNoReinit();
    }
  }

  /**
   * Move the sub iterator to the given position. isEmpty may not yet be set. Never list form when
   * called.
   * 
   * Mimics regular iterators when moveToStart moves the underlying iterator to a valid position in
   * front of a bound which is limiting on the left (coveredBy, sameBeginEnd)
   */
  private void moveTo_iterators(FeatureStructure fs, boolean initialPositioning) {
    it.moveToNoReinit(fs);

    // CASE: If the iterator is pointing on the bounds annotation, we must first skip this in
    // forward direction to ensure we find a potentially matching FSes occurring in the indexes
    // after the bounds annotation.
    if (it.isValid()) {
      // Check if the move went outside the bounds
      if (!initialPositioning) {
        switch (boundsUse) {
          case covering:
            if (is_beyond_bounds_chk_coveringNvc()) {
              return;
            }
            break;
          case coveredBy:
            if (is_beyond_bounds_chk_coveredByNvc()) {
              return;
            }
            break;
          case sameBeginEnd:
            if (is_beyond_bounds_chk_sameBeginEnd()) {
              return;
            }
            break;
          case notBounded:
          default:
            // No check necessary
            break;
        }
      }

      if (equalToBounds(it.getNvc())) {
        it.moveToNext();
        if (!it.isValid()) {
          it.moveToLastNoReinit();
        }
      }
    }

    switch (boundsUse) {
      case sameBeginEnd:
        maybeAdjustForAmbiguityAndIgnoringTypePriorities_forward(!initialPositioning);

        if (it.isValid()) {
          // no need for mimic position if type priorities are in effect; moveTo will either
          // find equal match and position to left most of the equal, including types, or
          // not find equal match and position to next greater one, which won't match next test
          if (is_beyond_bounds_chk_sameBeginEnd()) {
            isEmpty = true; // iterator was made invalid
            return;
          }
          // skip over bounding annotation
          while (equalToBounds(it.getNvc())) {
            it.moveToNextNvc();
            if (is_beyond_bounds_chk_sameBeginEnd()) {
              return;
            }
          }
        }
        return; // skip setting prev end - not used for sameBeginEnd

      case coveredBy:
        maybeAdjustForAmbiguityAndIgnoringTypePriorities_forward(!initialPositioning);

        // If an annotation is present (found), position is on it, and if not,
        // position is at the next annotation that is higher than (or invalid, if there
        // is none). Note that the next found position could be beyond the end.
        if (it.isValid()) {
          // skip over bounding annotation
          while (equalToBounds(it.getNvc())) {
            it.moveToNextNvc();
            if (!it.isValid()) {
              return;
            }
          }
          // adjust for strict
          if (adjustForStrictNvc_forward()) {
            if (is_beyond_bounds_chk_coveredByNvc()) { // is beyond end iff annot.begin > boundEnd
              return; // iterator became invalid
            }
          }
        }
        break;

      case covering:
        if (it.isValid()) {
          adjustForCovering_forward();
        }
        is_beyond_bounds_chk_covering(); // make invalid if ran off end
        return; // skip setting prev end - not used for covering;

      case notBounded:
      default:
        if (!it.isValid()) {
          return;
        }
        break;
    }

    maybeSetPrevBounds(); // used for unambiguous
  }

  /**
   * @return true if iterator was outside - beyond, and therefore, made invalid or iterator is
   *         invalid to start with
   */
  private boolean is_beyond_bounds_chk_sameBeginEnd() {
    if (it.isValid()) {
      return is_beyond_bounds_chk_sameBeginEndNvc();
    }
    return true;
  }

  private boolean is_beyond_bounds_chk_sameBeginEndNvc() {

    Annotation a = it.getNvc();
    if (a.getBegin() != boundBegin || a.getEnd() != boundEnd
            || (isUseTypePriority && a._getTypeImpl() != boundType)) {
      makeInvalid();
      return true;
    } else {
      return false;
    }

  }

  /**
   * @return true if iterator was outside - beyond, and therefore, made invalid or iterator is
   *         invalid to start with
   */
  private boolean is_beyond_bounds_chk_coveredByNvc() {
    if (it.getNvc().getBegin() > boundEnd) {
      makeInvalid();
      return true;
    }
    return false;
  }

//@formatter:off
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
//@formatter:on
  private boolean is_beyond_bounds_chk_covering() {
    if (!it.isValid()) {
      return true;
    }
    return is_beyond_bounds_chk_coveringNvc();
  }

  private boolean is_beyond_bounds_chk_coveringNvc() {
    Annotation a = it.getNvc();
    int begin = a.getBegin();
    int end = a.getEnd();

    if (begin > boundBegin || (begin == boundBegin && (end < boundEnd
            || (end == boundEnd && lto != null && lto.lessThan(a._getTypeImpl(), boundType))))) {
      makeInvalid();
      return true;
    } else {
      return false;
    }
  }

  private void maybeAdjustForAmbiguityAndIgnoringTypePriorities_forward(boolean oneStepOnly) {
    if (isUseTypePriority) {
      return;
    }

    // But if the iterator is ambiguous and if we also ignore the type priorities, then we need to
    // seek backwards in the index as to not skip any potentially relevant annotations at the same
    // position as the bounding annotation but (randomly) appearing before the bounding annotation
    // in the index.
    boolean wasValid = it.isValid();

    // CASE: Previously called "it.moveToNoReinit(boundingAnnot)" moved beyond end of the index
    //
    // If the bounding annotation evaluates to being "greater" than any of the annotation in the
    // index according to the index order, then the iterator comes back invalid.
    if (!wasValid) {
      it.moveToLastNoReinit();
    }

    boolean wentBack = adjustForStrictOrCoveringAndBoundSkip(oneStepOnly);

    if (!wentBack && !wasValid) {
      // No backwards seeking was performed and the iterator was initially invalid, so we
      // invalidate it again
      makeInvalid();
    }
  }

  private boolean coveredByBounds(Annotation ann) {
    if (isStrict) {
      return coveredBy(ann, boundBegin, boundEnd)
              || (isIncludesAnnotationsStartingAtEndPosition && ann.getBegin() == boundEnd);
    } else {
      return (coveredBy(ann, boundBegin, boundEnd) || overlappingAtEnd(ann, boundBegin, boundEnd))
              || (isIncludesAnnotationsStartingAtEndPosition && ann.getBegin() == boundEnd);
    }
  }

  /**
   * Skip over annotations which are not valid for the given bounds use. This can be used in two
   * cases.
   * <ul>
   * <li>Seeking backwards from the current to the first valid annotation relative to the beginning
   * of the index.</li>
   * <li>Seeking backwards from the current position to the first valid annotation relative to the
   * current position.</li>
   * </ul>
   */
  private boolean adjustForStrictOrCoveringAndBoundSkip(boolean oneStepOnly) {
    boolean wentBack = false;
    boolean lastSeenWasEqualToBounds = false;
    switch (boundsUse) {
      case coveredBy:
        // We need to try seeking backwards because we may have skipped covered annotations which
        // start within the selection range but do not end within it.
        while (it.isValid()
                && (equalToBounds(it.getNvc()) || (oneStepOnly ? !coveredByBounds(it.getNvc())
                        : it.getNvc().getBegin() >= boundBegin))) {
          it.moveToPreviousNvc();
          wentBack = true;
        }
        break;

      case covering:
        // because this method is move to previous, the position cannot be on the bounds.
        // handle skipping cases where the end is < boundEnd
        while (it.isValid()
                && (equalToBounds(it.getNvc())
                        || (oneStepOnly
                                ? it.getNvc().getBegin() <= boundBegin
                                        && (it.getNvc().getEnd() < boundEnd
                                                || (it.getNvc().getEnd() == boundEnd && lto != null
                                                        && lto.lessThan(it.getNvc()._getTypeImpl(),
                                                                boundType)))
                                : it.getNvc().getEnd() < boundEnd))) {
          // FIXME: I can't really wrap my head around the logic of "maybeMoveToPrevBounded()"
          // Possibly this is buggy and should be changed to t.moveToPreviousNvc() - after all, we
          // do have the backing up logic below in case the iterator becomes invalid...
          maybeMoveToPrevBounded();
          wentBack = true;
        }
        break;

      case sameBeginEnd:
        while (it.isValid() && ((lastSeenWasEqualToBounds = equalToBounds(it.getNvc()))
                || (!oneStepOnly && (it.getNvc().getBegin() > boundBegin
                        || (it.getNvc().getBegin() == boundBegin
                                && it.getNvc().getEnd() <= boundEnd))))) {
          it.moveToPreviousNvc(); // can be multiple equal to bounds
          wentBack = true;
        }
        break;

      default: // same as no bounds case
    }

    if (wentBack) {
      if (!it.isValid()) {
        if (!oneStepOnly || lastSeenWasEqualToBounds) {
          it.moveToFirstNoReinit();
        }
      } else {
        if (!oneStepOnly && it.getNvc().getBegin() < boundingAnnot.getBegin()) {
          it.moveToNextNvc();
        } else if (boundsUse == BoundsUse.sameBeginEnd
                && (oneStepOnly ? it.getNvc().getBegin() != boundingAnnot.getBegin()
                        : it.getNvc().getBegin() == boundingAnnot.getBegin())
                && it.getNvc().getEnd() != boundingAnnot.getEnd()) {
          it.moveToNextNvc();
        }
      }
    }

    return wentBack;
  }

//@formatter:off
  /**
   * Special equalToBounds used only for having bounded iterators 
   * skip returning the bounding annotation
   * 
   * Two styles: uimaFIT style: only skip the exact one (id's the same)
   *             uima style: skip all that compare equal using the AnnotationIndex comparator
   * @param fs -
   * @return true if should be skipped
   */
//@formatter:on
  private boolean equalToBounds(Annotation fs) {
    return isDoEqualsTest && fs._id == originalBoundingAnnotation._id || (isSkipSameBeginEndType
            && fs.getBegin() == boundBegin && fs.getEnd() == boundEnd && fs.getType() == boundType);
  }

  private void maybeSetPrevBounds() {
    if (isUnambiguous && it.isValid()) {
      Annotation a = it.getNvc();
      prevBegin = a.getBegin();
      prevEnd = a.getEnd();
    }
  }

  /**
   * @return true if iterator still valid, false if not valid
   */
  private boolean adjustForStrictNvc_forward() {
    Annotation item = it.getNvc();

    boolean originalBoundZeroWidth = originalBoundBegin == originalBoundEnd;

    while (
    // following/preceding and bounds is zero-width and item is zero-width
    (boundsUse == notBounded && item.getBegin() == item.getEnd()
            && item.getBegin() == originalBoundBegin) ||
    // Bounds is zero-width at start
            (!isIncludeZeroWidthAtBegin && originalBoundZeroWidth
                    && item.getBegin() == originalBoundBegin)
            ||
            // Item is zero-width at end
            (!isIncludeZeroWidthAtEnd && originalBoundEnd == item.getBegin()
                    && item.getBegin() == item.getEnd())) {
      it.moveToNextNvc();
      if (!isValid()) {
        return false;
      }

      item = it.getNvc();
    }

    if (!isStrict) {
      if (isIncludesAnnotationsStartingAtEndPosition) {
        return true;
      }

      while ((item.getBegin() == boundEnd && item.getBegin() < item.getEnd())
              || equalToBounds(item) || (isUnambiguous && overlapping(item, prevBegin, prevEnd))) {
        it.moveToNextNvc();
        if (!isValid()) {
          return false;
        }

        item = it.getNvc();
        if (item.getBegin() > boundEnd) { // not >= because could of 0 length annot at end
          makeInvalid();
          return false;
        }
      }
      return true;
    }

    while (item.getEnd() > boundEnd || equalToBounds(item)
            || (isUnambiguous && overlapping(item, prevBegin, prevEnd)) ||
            // Item is zero-width at end
            (!isIncludeZeroWidthAtEnd && boundEnd == item.getBegin()
                    && item.getBegin() == item.getEnd())
            ||
            // Bounds is zero-width at end
            (!isIncludeZeroWidthAtEnd && originalBoundZeroWidth
                    && item.getEnd() == originalBoundBegin)) {
      it.moveToNextNvc();
      if (!isValid()) {
        return false;
      }

      item = it.getNvc();
      if (item.getBegin() > boundEnd) { // not >= because could of 0 length annot at end
        makeInvalid();
        return false;
      }
    }
    return true;
  }

//@formatter:off
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
   */
//@formatter:on
  private void adjustForCovering_forward() {
    if (!it.isValid()) {
      return;
    }
    Annotation a = it.getNvc();
    int begin = a.getBegin();
    int end = a.getEnd();

    // moveTo may move to invalid position
    // if the cur pos item has a begin beyond the bound, it cannot be a covering annotation
    if (begin > boundBegin) {
      makeInvalid();
      return;
    }

    // begin is <= bound begin

    // skip until get an FS whose end >= boundEnd, it is a candidate.
    // stop if begin gets too large (going forwards)
    // while test: is true if need to move to skip over a too-small "end"
    while (it.isValid() && (equalToBounds(a = it.getNvc()) || (a.getBegin()) <= boundBegin
            && ((end = a.getEnd()) < boundEnd || (end == boundEnd && lto != null
                    && lto.lessThan(a._getTypeImpl(), boundType))))) {
      it.moveToNextNvc();
    }
  }

  /**
   * Assume: iterator is valid
   */
  private void maybeMoveToPrevBounded() {
    if (it.getNvc()._id == startId) {
      it.moveToFirstNoReinit(); // so next is invalid
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
    Subiterator<T> copy = new Subiterator<>(it.copy(), boundingAnnot,
            originalBoundingAnnotation, !isUnambiguous, isStrict, boundsUse,
            isUseTypePriority, isSkipSameBeginEndType,

            startId, isEmpty, coveringStartPos, isDoEqualsTest,
            isIncludesAnnotationsStartingAtEndPosition, isIncludeZeroWidthAtBegin,
            isIncludeZeroWidthAtEnd);
    copy.list = list; // non-final things
    copy.pos = pos;
    copy.isListForm = isListForm;
    return copy;
  }

  /**
   * This is unsupported because its expensive to compute in many cases, and may not be needed.
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
    return ((LowLevelIterator) it).ll_maxAnnotSpan();
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return ((LowLevelIterator<T>) it).ll_getIndex();
  }

  private void makeInvalid() {
    it.moveToFirstNoReinit();
    it.moveToPrevious();
  }

  /**
   * Used to determine when some precomputed things (e.g. listform) need to be recalculated
   * 
   * @return true if one or more of the underlying indexes of the underlying iterator have been
   *         updated
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return ((LowLevelIterator<?>) it).isIndexesHaveBeenUpdated();
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
}
