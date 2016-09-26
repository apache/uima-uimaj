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

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Subiterator implementation.
 * 
 * There are two bounding styles and 2 underlying forms.
 * 
 * The 2nd form is produced lazily when needed, and 
 * is made by a one-time forward traversal to compute unambigious subsets and store them into a list.
 *   - The 2nd form is needed only for unambiguous style if backwards or moveto(fs) operation.
 * 
 * The 1st form uses the underlying iterator directly, and does skipping as needed, while iterating  
 *   - going forward: 
 *       skip if unambigious and start is within prev span
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
 *       unambigious - convert to 2nd form
 *       skip backwards if strict and end lies outside of scope span
 *       
 * There are two styles of the bounding information.
 * 
 *   - the traditional one uses the standard comparator for annotations: begin (ascending), end (descending) and
 *     type priority ordering
 *   - the 2nd style uses just a begin value and an end value, no type priority ordering.  
 */
public class Subiterator<T extends AnnotationFS> extends FSIteratorImplBase<T> {

  private ArrayList<T> list = null; // used for form 2, lazily initialized

  private int pos = 0;  // used for form 2
  
  private final FSIteratorImplBase<T> it;

  private final FSIndexRepositoryImpl fsIndexRepo;
  
  private final AnnotationFS boundingAnnotation;  // the bounding annotation need not be a subtype of T
  
  private final int boundingBegin;
  private final int boundingEnd;
  private int prevEnd = 0;  // for unambiguous iterators
  private final boolean ambiguous;
  private final boolean strict;
  private final boolean isBounded;
  private boolean isListForm = false;
  private final boolean isBeginEndCompare;
  
  private final int startId;

  
  /**
   * Constructor called with annot == null and boundingBegin and boundingEnd specified, or
   *             called with annot != null (boundingBegin/End ignored)
   *             
   *    if annot == null, then this implies the comparisons should use just begin and end (not type priorities) and
   *                                        the range is inclusive with the begin/ end boundaries
   *             != null, then this implies the comparisons use the normal Annotation compare and
   *                                        the range is exclusive on the left with the boundaries.         
   * @param it the iterator to use
   * @param boundingAnnotation null or the bounding annotation
   * @param boundingBegin if boundingAnnotation is null, this is used as the bounding begin (inclusive); 
   *                      ignored if boundingAnnotation is not null
   * @param boundingEnd   if annot is null, this is used as the bounding end (inclusive); 
   *                      ignored if boundingAnnotation is not null
   * @param ambiguous true means normal iteration, 
   *                  false means to skip annotations whose begin lies between previous begin (inclusive) and end (exclusive)
   * @param strict true means to skip annotations whose end is greater than the bounding end position (ignoring type priorities)
   * @param isBounded false means it's an unambiguous iterator with no bounds narrowing; ambiguous taken to be false
   * @param fsIndexRepo the index repository for this iterator
   */
  Subiterator(
      FSIterator<T> it, 
      AnnotationFS boundingAnnotation, 
      int boundingBegin, 
      int boundingEnd, 
      boolean ambiguous, 
      boolean strict,
      boolean isBounded, 
      FSIndexRepositoryImpl fsIndexRepo
      ) {
    super();
    this.isBounded = isBounded;
    // non bounded iterators don't use any begin/end compares
    this.isBeginEndCompare = this.isBounded ? (boundingAnnotation == null) : false;
    this.it = (FSIteratorImplBase<T>) it;
    this.boundingAnnotation = this.isBounded ? boundingAnnotation : null;
    this.boundingBegin = (boundingAnnotation == null) ? boundingBegin : boundingAnnotation.getBegin();
    this.boundingEnd = (boundingAnnotation == null) ? boundingEnd : boundingAnnotation.getEnd();
    this.ambiguous = ambiguous;
    this.strict = strict;
    this.fsIndexRepo = fsIndexRepo;
    
    moveToStart();
    startId = (isValid()) ? ((FeatureStructureImpl)get()).getAddress() : 0;
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
    this.list = new ArrayList<T>();
    while (isValid()) {
      prevEnd = it.getEnd();
      list.add(it.get());
      it.moveToNext();
      movePastPrevAnnotation();
      adjustForStrictForward();
    }    
    isListForm = true;  // do at end, so up to this point, iterator is not the list form style
  }
  
  private void moveToExact(FeatureStructureImpl targetAnnotation) {
    it.moveTo(targetAnnotation);  // move to left-most equal one
    while (it.isValid()) {         // advance to the exact equal one
      if (targetAnnotation.getAddress() == ((FeatureStructureImpl)(it.get())).getAddress()) {
        break;
      }
      it.moveToNext();
    }
  }
  /**
   * Move to the starting position of the sub iterator
   *   Annotation bounding:  move to the annotation, then move forwards until you're at an element not equal to the annot.
   *    (by definition of the iterator)
   *   and adjust for strict
   */
  private void moveToStart() {
    if (!isBounded) {
      it.moveToFirst();
    } else if (isBeginEndCompare) {
      it.moveTo(this.boundingBegin, this.boundingEnd);
    } else {  // is annotation bounding
      it.moveTo(boundingAnnotation);
      movePastAnnot();  // only for the annot style, the range is exclusive by definition
    }
    
    adjustForStrictForward();
    setPrevEnd();
  }
  
  private void setPrevEnd() {
    if (!ambiguous && it.isValid()) {
      this.prevEnd = it.getEnd();
    }    
  }

  /**
   * For subiterators bounded by Annotations, the starting place is the 
   * item past the elements that are equal to the the bounding annot,
   * adjusted for strict exclusions
   */
  private void movePastAnnot() {
    Comparator<AnnotationFS> annotationComparator = getAnnotationComparator();
    while (isValid() && (0 == annotationComparator.compare(boundingAnnotation, it.get()))) {
      it.moveToNext();
    }
  }
  
  private Comparator<AnnotationFS> getAnnotationComparator() {
    return fsIndexRepo.getAnnotationFsComparator();
  }
  
  /** 
   * Called for begin/end compare, after moveTo(fs)
   * to eliminate the effect of the type order comparison before any adjustment for strict.
   * Move backwards while equal with begin/end iterator. 
   */
  private void adjustAfterMoveToForBeginEndComparator(AnnotationFS fs) {
    final int begin = fs.getBegin();
    final int end = fs.getEnd();
    
    while (it.isValid() && (it.getBegin() == begin) && (it.getEnd() == end)) {
      it.moveToPrevious();  
    }
    // are one position too far, move back one
    if (it.isValid()) {
      it.moveToNext();  
    } else {
      moveToStart();
    }
  }
  
  /**
   * For strict mode, advance iterator until the end is within the bounding end 
   */
  private void adjustForStrictForward() {
    if (strict && isBounded) {
      while (it.isValid() && (it.getEnd() > this.boundingEnd)) {
        it.moveToNext();
      }
    }
  }
  
  /**
   * For unambiguous, going forwards
   */
  private void movePastPrevAnnotation() {
    if (!ambiguous) {
      while (it.isValid() && (it.getBegin() < this.prevEnd)) {
        it.moveToNext();
      }
    }
  }
  
  private void adjustForStrictBackward() {
    if (strict && isBounded) {
      while (it.isValid() && (it.getEnd() > this.boundingEnd)) {
        it.moveToPrevious();
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
    return isListForm ? 
        (this.pos >= 0) && (this.pos < this.list.size()) :
          
        // assume that it position is adjusted for strict and unambiguous already
        (it.isValid() && 
            ( isBounded? (it.getBegin() <= this.boundingEnd) : 
              true));
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
        return this.list.get(this.pos);
      }
    } else {
      if (isValid()) {
        return it.get();
      }
    }
    throw new NoSuchElementException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  @Override
  public void moveToNext() {
    if (isListForm) {
      ++this.pos;
      // setPrevEnd not needed because list form already accounted for unambiguous
      return;
    }
   
    it.moveToNext();
    
    if (!ambiguous) {
        movePastPrevAnnotation();
    }

    adjustForStrictForward();
    
    // stop in bounded case if out of bounds going forwards UIMA-5063
    if (isBounded && it.isValid() && (it.get().getBegin() > boundingEnd)) {
      it.moveToLast();
      it.moveToNext();  // mark invalid
    } else {
      setPrevEnd();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  @Override
  public void moveToPrevious() {
    if (isListForm) {
      --this.pos;
      return;
    }

    if (!ambiguous) {
      // Convert to list form
      FeatureStructureImpl currentAnnotation = (FeatureStructureImpl) it.get();  // save to restore position
      convertToListForm();
      moveToExact(currentAnnotation);
      --this.pos;
      return;
    }
    
    // stop in bounded case if out of bounds going backwards UIMA-5063
    if (isBounded && isValid() && ((FeatureStructureImpl)it.get()).getAddress() == startId) {
      it.moveToFirst();
      it.moveToPrevious();  // make it invalid
    } else {
      it.moveToPrevious();
      adjustForStrictBackward();
    }
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
   * This operation is relatively expensive
   * 
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  @Override
  public void moveToLast() {
    if (isListForm) {
      this.pos = this.list.size() - 1;
    } else {
      convertToListForm();
      this.pos = this.list.size() - 1;
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
    AnnotationFS fsa = (AnnotationFS) fs;
    if (!ambiguous && !isListForm) {  // unambiguous must be in list form
      convertToListForm();
    }
    if (isListForm) {
      Comparator<AnnotationFS> annotationComparator = getAnnotationComparator();
      pos = Collections.binarySearch(this.list, (AnnotationFS) fs, annotationComparator);
      if (pos >= 0) {
        if (!isValid()) {
          return;
        }
        T foundFs = get();
        // Go back until we find a FS that is really smaller
        if (isBeginEndCompare) {
          adjustAfterMoveToForBeginEndComparator(fsa);
        } else {
          while (true) {
            moveToPrevious();
            if (isValid()) {
              if (annotationComparator.compare(get(), foundFs) != 0) {
                moveToNext(); // backed up too far, so go back
                break;
              }
            } else {
              moveToFirst();  // went to before first, so go back to 1st
              break;
            }
          }   
        }
      } else {
        // element wasn't found, 
        //set the position to the next (greater) element in the list or to an invalid position 
        //   if no element is greater
        pos = (-pos) - 1;
        if (isBeginEndCompare) {
          pos-- ;  // check to see if previous element, unequal with annotation compare, might be equal with begin-end compare
          adjustAfterMoveToForBeginEndComparator(fsa);
        }
      }
    } else {
      // is ambiguous, may be strict, always bounded (either by annotation or begin / end
      it.moveTo(fs);  // moves to left-most
      if (isBeginEndCompare) {
        adjustAfterMoveToForBeginEndComparator(fsa);
      }
      adjustForStrictForward();
    
      if (it.isValid()) {
        // mark invalid if end up outside of bounds after adjustments
        if (it.getBegin() > boundingEnd) {
          it.moveToLast();
          it.moveToNext();  // make invalid
        } else if (fsIndexRepo.getAnnotationFsComparator().compare(it.get(), boundingAnnotation) < 0) {
          it.moveToFirst();
          it.moveToPrevious(); // make invalid
        } 
      }
    }
  }
  
  /* 
   * @see org.apache.uima.cas.impl.FSIteratorImplBase#moveTo(java.util.Comparator)
   */
  @Override
  void moveTo(final int begin, final int end) {
    if (!ambiguous && !isListForm) {  // unambiguous must be in list form
      convertToListForm();
    }
    if (isListForm) {
      pos = Collections.binarySearch(this.list, null, getAnnotationBeginEndComparator(begin, end));
      if (pos >= 0) {
        if (!isValid()) {
          return;
        }
        
        // Go back to leftmost 
        while (it.isValid() && (it.getBegin() == begin) && (it.getEnd() == end)) {
          it.moveToPrevious();  
        }
        // are one position too far, move back one
        if (it.isValid()) {
          it.moveToNext();  
        } else {
          it.moveToFirst();
        }
      } else {
        // element wasn't found, 
        //set the position to the next (greater) element in the list or to an invalid position 
        //   if no element is greater
        pos = (-pos) - 1;
      }
    } else {
      // is ambiguous, may be strict, always bounded (either by annotation or begin / end
      it.moveTo(begin, end);
      // Go back to leftmost 
      while (isValid() && (it.getBegin() == begin) && (it.getEnd() == end)) {
        it.moveToPrevious();  
      }
      adjustForStrictForward();
      
      if (it.isValid()) {
        // mark invalid if end up outside of bounds after adjustments
        if (it.getBegin() > boundingEnd) {
          it.moveToLast();
          it.moveToNext();  // make invalid
        } else if (fsIndexRepo.getAnnotationFsComparator().compare(it.get(), boundingAnnotation) < 0) {
          it.moveToFirst();
          it.moveToPrevious(); // make invalid
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
        this.it, this.boundingAnnotation, this.boundingBegin, this.boundingEnd, this.ambiguous, this.strict, this.isBounded, this.fsIndexRepo);
    copy.list = this.list;  // non-final things
    copy.pos  = this.pos;
    return copy;
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
//    while (it.isValid() && it.getBegin() < start) {
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
//  }

}
