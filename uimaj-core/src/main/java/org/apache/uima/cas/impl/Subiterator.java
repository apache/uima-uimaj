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
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.impl.JCasImpl;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Subiterator implementation.
 * 
 * There are two bounding styles and 2 underlying forms.
 * 
 * The 2nd form is produced lazily when needed, and 
 * is made by a one-time forward traversal to compute unambiguous subsets and store them into a list.
 *   - The 2nd form is needed only for unambiguous style if backwards or moveto(fs) operation.
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
 */
public class Subiterator<T extends AnnotationFS> implements LowLevelIterator<T> {


  public static enum BoundsUse {
    coveredBy,
    covering,
    sameBeginEnd,
    notBounded,
  }
    
  /*
   * The generic type of this is Annotation, not T, because that allows the use of a comparator which is comparing
   * a key which is not T but some super type of T.
   */
  private ArrayList<Annotation> list = null; // used for form 2, lazily initialized

  private int pos = 0;  // used for form 2
  
  private final FSIterator<Annotation> it;
  
  private final Annotation boundingAnnot;  // the bounding annotation need not be a subtype of T
  
  private final Annotation coveringStartPos;
  private final Annotation coveringEndPos;  // an approx end position
  
  private final boolean isAmbiguous;  // true means ordinary, false means to skip until start is past prev end
  private final boolean isStrict;
  private final boolean isBounded;
  private final boolean isTypePriority;
  private final boolean isPositionUsesType;
  private final boolean isSkipEquals;
  private final BoundsUse boundsUse;
  
  private final boolean isEmpty;
  
  private int prevEnd = 0;  // for unambiguous iterators
  private boolean isListForm = false;
  
  private final int startId;

  private final int boundBegin;
  private final int boundEnd;
  private final TypeImpl boundType;
  
  private final Comparator<TOP> annotationComparator;  // implements compare(fs1, fs2) for begin/end/type priority
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
   * @param isBounded false means it's an unambiguous iterator with no bounds narrowing
   * @param isTypePriority false to ignore type priorities, and just use begin/end and maybe type
   * @param isPositionUsesType only used if isTypePriority is false, and says to still compare types for eq
   * @param isSkipEquals used only for bounded case, 
   *                     false means to only skip returning an FS if it has the same id() as the bounding fs
   * @param annotationComparator indexRepository.getAnnotationComparator() value
   */
  Subiterator(
      FSIterator<T> it, 
      AnnotationFS boundingAnnot, 
      boolean ambiguous, 
      boolean strict,    // omit FSs whose end > bounds
      BoundsUse boundsUse, // null if no bounds; boundingAnnot used for start position if non-null
      boolean isTypePriority,
      boolean isPositionUsesType, 
      boolean isSkipEquals,
      Comparator<TOP> annotationComparator
      ) {
    this.it = (FSIterator<Annotation>) it;
    this.boundingAnnot = (Annotation) boundingAnnot;
    this.isBounded = boundsUse != null && boundsUse != BoundsUse.notBounded;
    this.boundsUse = (boundsUse == null) ? BoundsUse.notBounded : boundsUse;
    this.isAmbiguous = ambiguous;
    this.isStrict = strict;
    this.isPositionUsesType = isPositionUsesType;
    this.isSkipEquals = isSkipEquals;
 
    if (isBounded && (null == boundingAnnot || !(boundingAnnot instanceof Annotation))) {
      Misc.internalError(new IllegalArgumentException("Bounded Subiterators require a bounding annotation"));
    }
    this.boundBegin = isBounded ? boundingAnnot.getBegin() : -1;
    this.boundEnd = isBounded ? boundingAnnot.getEnd(): -1;
    this.boundType = isBounded ? (TypeImpl) boundingAnnot.getType() : null;

    this.isTypePriority = (boundsUse == BoundsUse.covering) ? false : isTypePriority;

    this.annotationComparator = annotationComparator;
    this.jcas = (JCasImpl) ((LowLevelIterator<T>)it).ll_getIndex().getCasImpl().getJCas();
    
    if (boundsUse == BoundsUse.covering) {
      // compute start position and isEmpty setting
      int span = ((LowLevelIterator<?>)it).ll_maxAnnotSpan();
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
    
    moveToStart();
    isEmpty = !isValid();
    startId = isValid() ? get()._id() : 0;
  }
   
  /** 
   * copy constructor - no move to start
   * @param it -
   * @param boundingAnnot -
   * @param ambiguous -
   * @param strict -
   * @param isBounded -
   * @param isTypePriority -
   * @param isPositionUsesType -
   * @param isSkipEquals -
   * @param annotationComparator -
   */
  Subiterator(
      FSIterator<Annotation> it, 
      Annotation boundingAnnot, 
      boolean ambiguous, 
      boolean strict,    // omit FSs whose end > bounds
      BoundsUse boundsUse, // null if boundingAnnot being used for starting position in unambiguous iterator
      boolean isTypePriority,
      boolean isPositionUsesType, 
      boolean isSkipEquals,
      Comparator<TOP> annotationComparator,
      int startId,
      boolean isEmpty,
      Annotation coveringStartPos,
      Annotation coveringEndPos
      ) {
    this.it = it;
    this.boundingAnnot = boundingAnnot;
    this.isBounded = boundsUse != null;
    this.boundsUse = boundsUse;
    this.isAmbiguous = ambiguous;
    this.isStrict = strict;
    this.isTypePriority = isTypePriority;
    this.isPositionUsesType = isPositionUsesType;
    this.isSkipEquals = isSkipEquals;
    this.startId = startId;

    this.boundBegin = isBounded ? boundingAnnot.getBegin() : -1;
    this.boundEnd = isBounded ? boundingAnnot.getEnd(): -1;
    this.boundType = isBounded ? (TypeImpl) boundingAnnot.getType() : null;
    
    this.annotationComparator = annotationComparator;
    this.isEmpty = isEmpty;
    this.jcas = (JCasImpl) ((LowLevelIterator<T>)it).ll_getIndex().getCasImpl().getJCas();
    this.coveringStartPos = coveringStartPos;
    this.coveringEndPos = coveringEndPos;
  }

  /**
   * Converting to list form - called for 
   *   unambiguous iterator going backwards, 
   *   unambiguous iterator doing a moveTo(fs) operation
   *   iterator doing a moveToLast() operation
   * 
   */
  private void convertToListForm() {
    moveToStart();  // moves to the start annotation, including moving past equals for annot style, and accommodating strict
    this.list = new ArrayList<Annotation>();
    while (isValid()) {
      list.add((Annotation) it.getNvc());
      moveToNext();
    }
    this.pos = 0;
    isListForm = true;  // do at end, so up to this point, iterator is not the list form style
  }
  
  private void moveToExact(Annotation targetAnnotation) {
    it.moveTo(targetAnnotation);  // move to left-most equal one
    boolean found = adjustForTypePriorityBoundingBegin(targetAnnotation._id);
    if (!found)
    while (it.isValid()) {         // advance to the exact equal one
      if (targetAnnotation._id() == it.getNvc()._id()) {
        break;
      }
      it.moveToNext();
    }
  }
  /**
   * Move to the starting position of the sub iterator
   * isEmpty may not yet be set
   */
  private void moveToStart() {
    switch (boundsUse) {
    case notBounded:
      it.moveToFirst();
      break;
    case sameBeginEnd:
      it.moveTo(boundingAnnot);
      adjustForTypePriorityBoundingBegin(0);
      skipOverBoundingAnnot(true);
      maybeMakeItInvalidSameBeginEnd(); 
    case coveredBy:
      it.moveTo(boundingAnnot);
      adjustForTypePriorityBoundingBegin(0);
      adjustForStrictOrCoveringAndBoundSkip(true); // forward
      break;
    case covering:
      it.moveTo(coveringStartPos); // sufficiently before the bounds
      adjustForTypePriorityBoundingBegin(0);
      adjustForCoveringAndBoundSkip(true);  // forward
      break;
    }
    maybeSetPrevEnd();  // used for unambiguous
  }
  
  private void adjustForStrictOrCoveringAndBoundSkip(boolean forward) {
    if (boundsUse == BoundsUse.covering) {
      adjustForCoveringAndBoundSkip(forward);
      return;
    }
    // this used for both coveredBy and sameBeginEnd
    adjustForStrict(forward);
    if (skipOverBoundingAnnot(forward)) {
      adjustForStrict(forward);
    }
  }
  
  private void adjustForCoveringAndBoundSkip(boolean forward) {
    adjustForCovering(forward);
    if (skipOverBoundingAnnot(forward)) {
      adjustForCovering(forward);
    }    
  }
  
  /**
   * advance over FSs that are equal (2 choices) to the bounding annotation.
   * May leave the iterator in invalid state.
   * @param forward - true means move forward, false means move backwards
   * @return true if some move happened, false if nothing happened
   */
  private boolean skipOverBoundingAnnot(boolean forward) {
    boolean moved = false;
    if (isBounded && it.isValid()) { // skip if not bounded or iterator is invalid
      while (equalToBounds(it.get())) {
        moved = true;
        if (forward) {
          it.moveToNextNvc();
        } else {
          maybeMoveToPrevBounded();
        }
        if (!it.isValid()) {
          break;
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
    return fs._id == boundingAnnot._id ||
           (isSkipEquals && annotationComparator.compare(fs,  boundingAnnot) == 0);
  }
    
  private void maybeSetPrevEnd() {
    if (!isAmbiguous && it.isValid()) {
      this.prevEnd = it.getNvc().getEnd();
    }    
  }
  
  
  /**
   * For strict mode, advance iterator until the end is within the bounding end 
   */
  private void adjustForStrict(boolean forward) {
    if (isStrict && boundsUse == BoundsUse.coveredBy) {
      while (it.isValid() && (it.getNvc().getEnd() > this.boundEnd)) {
        if (forward) {
          it.moveToNextNvc();
        } else {
          maybeMoveToPrevBounded();
        }
      }
    }
  }
  
  /**
   * when covering, skip items where the end < bounds end
   * may result in iterator becoming invalid
   * @param forward
   */
  private void adjustForCovering(boolean forward) {
    if (!it.isValid()) {
      return;
    }
    // moveTo may move to invalid position
    if (it.getNvc().getBegin() > this.boundBegin) {
      makeInvalid();
      return;
    }
    while (it.isValid() && (it.getNvc().getEnd() < this.boundEnd)) {
      if (forward) {
        it.moveToNextNvc();
        if (it.isValid() && it.getNvc().getBegin() > this.boundBegin) {
          makeInvalid();
          return;
        }
      } else {
        maybeMoveToPrevBounded();
      }
    }
  }
  
  /**
   * Assume: iterator is valid
   */
  private void maybeMoveToPrevBounded() {
    if (it.get()._id == startId) {
      it.moveToFirst();  // so next is invalid
    }
    it.moveToPreviousNvc();
  }
  
  /**
   * moves the iterator backwards if not using type priorities
   * to the left most position with the same begin /end (and maybe type) as the fs at the current position.
   * @param exactId 0 or the id to stop the traversal on
   * @return false unless stopped on the matching exactId
   */
  private boolean adjustForTypePriorityBoundingBegin(int exactId) {
    if (isTypePriority || !it.isValid()) {
      return false;
    }
    Annotation a = it.get();
    int begin = a.getBegin();
    int end = a.getEnd();
    Type type = a.getType();
    do {
      int id = it.get()._id;
      if (id == exactId) {
        return true;
      }
      if (id == startId) { // start id may not be set yet; if so it is set to 0 so this test is always false
        return false;  // iterator is at the start, can't move more
      }
      it.moveToPrevious();
      if (!it.isValid()) {
        it.moveToFirst();  // not moveToStart - called by moveToStart
//        Annotation f = it.getNvc();
//        if (!isBeginEndTypeEqualToBound(it.getNvc())) {
//          it.moveToPrevious();  // make invalid
//        }      
        return false;  
      }
    } while (isBeginEndTypeEqual(it.get(), begin, end, type));
    it.moveToNext();  // went back one to far
    return false;
  }
    
  private boolean isBeginEndTypeEqualToBound(Annotation fs) {
    return fs.getBegin() == boundBegin && fs.getEnd() == boundEnd &&
            (!isPositionUsesType || fs.getType() == boundType);
  }
  
  private boolean isBeginEndTypeEqual(Annotation fs, int begin, int end, Type type) {
    return fs.getBegin() == begin && fs.getEnd() == end &&
        (!isPositionUsesType || fs.getType() == type);
  }
  
  /**
   * For unambiguous, going forwards
   */
  private void movePastPrevAnnotation() {
    if (!isAmbiguous) {
      while (it.isValid() && (it.get().getBegin() < this.prevEnd)) {
        it.moveToNext();
      }
    }
  }
  
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    if (isEmpty) return false;
    if (isListForm) {
      return (this.pos >= 0) && (this.pos < this.list.size());
    }
    
    if (!it.isValid()) {
      return false;
    }
    
    switch (boundsUse) {
    case notBounded:
      return true;
    case coveredBy:
      return it.get().getBegin() <= boundEnd;
    case covering:
      return it.isValid();
    case sameBeginEnd:
      Annotation a = it.get();
      return a.getBegin() == boundBegin &&
             a.getEnd() == boundEnd;
    }
    return false; // ignored, just hear to get rid of invalid compiler error report
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#get()
   */
  @Override
  public T get() throws NoSuchElementException {
    if (isListForm) {
      if ((this.pos >= 0) && (this.pos < this.list.size())) {
        return (T) this.list.get(this.pos);
      }
    } else {
      if (isValid()) {
        return (T)it.get();
      }
    }
    throw new NoSuchElementException();
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
      return (T)it.get();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  @Override
  public void moveToNext() {
    if (!isValid()) return;
    moveToNextNvc();
  }
  
  @Override
  public void moveToNextNvc() {
    if (isListForm) {
      ++this.pos;
      // maybeSetPrevEnd not needed because list form already accounted for unambiguous
      return;
    }
   
    it.moveToNextNvc();
    if (!isAmbiguous) {               // skip until start > prev end
      movePastPrevAnnotation();
    }

    adjustForStrictOrCoveringAndBoundSkip(true);
    
    // stop logic going forwards for various bounding cases
    if (it.isValid()) {
      // stop in bounded case if out of bounds going forwards UIMA-5063
      switch(boundsUse) {
      case notBounded: 
        break;
      case coveredBy:
        maybeMakeItInvalid_bounds(it.getNvc(), a -> a.getBegin() > boundEnd);
        break;
      case covering:
        maybeMakeItInvalid_bounds(it.getNvc(), a -> a.getBegin() > boundBegin);
        break;
      case sameBeginEnd:
        maybeMakeItInvalidSameBeginEnd();
        break;
      }
    } 
    maybeSetPrevEnd();
  }
  
  private void maybeMakeItInvalidSameBeginEnd() {
    maybeMakeItInvalid_bounds(it.getNvc(), a -> a.getBegin() != boundBegin || a.getEnd() != boundEnd);
  }
  
  private void maybeMakeItInvalid_bounds(Annotation a, Predicate<Annotation> outOfBounds) {
    if (outOfBounds.test(a)) {
      makeInvalid();
    }
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  public void moveToPrevious() {
    if (!isValid()) {
      return;
    }
    moveToPreviousNvc();
  }
  
  @Override  
  public void moveToPreviousNvc() {
    if (isListForm) {
      --this.pos;
      return;
    }

    if (!isAmbiguous) {
      // Convert to list form
      Annotation currentAnnotation = it.get();  // save to restore position
      convertToListForm();
      moveToExact(currentAnnotation);
      --this.pos;
      return;
    }

    if (it.isValid()) {
      maybeMoveToPrevBounded();
    }

    adjustForStrictOrCoveringAndBoundSkip(false); // moving backwards
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
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  @Override
  public void moveToFirst() {
    if (isListForm) {
      this.pos = 0;
    } else {
      moveToStart();
    }
  }

  /*
   * This operation is relatively expensive one time for unambiguous
   * 
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  @Override
  public void moveToLast() {
    if (isEmpty) {
      return;
    }
    if (!isAmbiguous && !isListForm) {
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
    it.moveTo(new Annotation (jcas, begin, end));
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
      it.moveToLast();
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
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void moveTo(FeatureStructure fs) {
    if (isEmpty) return;
    
    Annotation fsa = (Annotation) fs;
    if (!isAmbiguous && !isListForm) {  // unambiguous must be in list form
      convertToListForm();
    }
    if (isListForm) {
      // W A R N I N G - don't use it.xxx forms here, they don't work for list form
      // Don't need strict, skip-over-boundary, or type priority adjustments, 
      //   because these were done when the list form was created
      pos = Collections.binarySearch(this.list, fsa, annotationComparator);
      int begin = fsa.getBegin();
      int end   = fsa.getEnd();
      Type type = fsa.getType();

      if (pos >= 0) {
        if (!isValid()) {
          return;
        }
        
        // Go back until we find a FS that is really smaller
        moveToPrevious();
        
        while (isValid() && isBeginEndTypeEqual((Annotation) get(), begin, end, type)) {
          moveToPrevious();
        }
        
        if (isValid()) {
          moveToNext();  // backed up one to much          
        } else {
          moveToFirst();
        }
          
      } else {
        // element wasn't found, 
        //set the position to the next (greater) element in the list or to an invalid position 
        //   if no element is greater
        pos = (-pos) - 1;
        if (!isValid()) {
          moveToLast();
          if (!isValid()) {
            return;
          }
        }
        while (isValid() && isBeginEndTypeEqual((Annotation) get(), begin, end, type)) {
          moveToPrevious();
        }
        if (!isValid()) {
          moveToFirst();
          return;
        }
        moveToNext();  // back up one
      }
    } else {
      // not list form
      // is ambiguous, may be strict, always bounded
      it.moveTo(fs);  // may move before, within, or after bounds
      adjustForTypePriorityBoundingBegin(0);
      adjustForStrictOrCoveringAndBoundSkip(true);
      
      if (it.isValid()) {
        // mark invalid if end up outside of bounds after adjustments
        Annotation a = it.get();
        if (a.getBegin() > boundEnd) {
          makeInvalid();
        } else if (isTypePriority && annotationComparator.compare(a,  boundingAnnot) < 0) {
          makeInvalid();
        } else if (a.getBegin() < boundBegin || 
                   a.getBegin() > boundEnd ||
                   (a.getBegin() == boundBegin && a.getEnd() > boundEnd) ||
                   (isPositionUsesType && 
                       a.getBegin() == boundBegin && 
                       a.getEnd() == boundEnd &&
                       a.getType() != boundType)) {
          makeInvalid();
        }
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
        this.isAmbiguous,
        this.isStrict,
        this.boundsUse,
        this.isTypePriority, 
        this.isPositionUsesType, 
        this.isSkipEquals,
        this.annotationComparator,
        this.startId,
        this.isEmpty,
        this.coveringStartPos,
        this.coveringEndPos);
    copy.list = this.list;  // non-final things
    copy.pos  = this.pos;
    copy.isListForm = this.isListForm;
    return copy;
  }

  @Override
  public int ll_indexSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int ll_maxAnnotSpan() {
    return ((LowLevelIterator)it).ll_maxAnnotSpan();
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return ((LowLevelIterator<T>)it).ll_getIndex();
  }

  private void makeInvalid() {
    it.moveToFirst();
    it.moveToPrevious();
  }



//  // makes an extra copy of the items
//  private void initAmbiguousSubiterator(AnnotationFS annot, final boolean strict) {
//    final int start = (annot == null) ? 0 : annot.getBegin();
//    final int boundingEnd = (annot == null) ? Integer.MAX_VALUE : annot.getEnd();
//    if (annot == null) {
//      it.moveToFirst();
//    } else {
//      it.moveTo(annot);  // to "earliest" equal, or if none are equal, to the one just later than annot
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
//    it.moveTo(annot);
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
