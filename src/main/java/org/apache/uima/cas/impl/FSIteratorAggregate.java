package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

/**
 * Aggregate several FS iterators.  Simply iterates over one after the other, no sorting or merging
 * of any kind occurs.  Intended for use in 
 * {@link FSIndexRepositoryImpl#getAllIndexedFS(org.apache.uima.cas.Type)}.
 * 
 * <p>Note: this class does not support moveTo(FS), as it is not sorted.
 */
class FSIteratorAggregate extends FSIteratorImplBase {
  
  // Internal contract for this class is that isValid() iff the current iterator isValid().

  // A list of iterators, unordered.
  private final List iterators;
  
  // The offset of the current index.
  private int iteratorIndex = 0;
  
  // Not used.
  private FSIteratorAggregate() {
    super();
    this.iterators = new ArrayList();
  }
  
  /**
   * The one and only constructor.
   * @param c Collection of input iterators.
   */
  public FSIteratorAggregate(Collection c) {
    super();
    this.iterators = new ArrayList();
    this.iterators.addAll(c);
  }

  public FSIterator copy() {
    ArrayList itCopies = new ArrayList(this.iterators.size());
    for (int i = 0; i < this.iterators.size(); i++) {
      itCopies.add(((FSIterator) this.iterators.get(i)).copy());
    }
    FSIteratorAggregate copy = new FSIteratorAggregate(itCopies);
    copy.iteratorIndex = this.iteratorIndex;
    return copy;
  }

  public FeatureStructure get() throws NoSuchElementException {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return ((FSIterator) this.iterators.get(this.iteratorIndex)).get();
  }

  public boolean isValid() {
    return (this.iteratorIndex < this.iterators.size());
  }

  public void moveTo(FeatureStructure fs) {
    throw new UnsupportedOperationException("This operation is not supported on an aggregate iterator.");
  }

  public void moveToFirst() {
    // Go through the iterators, starting with the first one
    this.iteratorIndex = 0;
    while (this.iteratorIndex < this.iterators.size()) {
      FSIterator it = (FSIterator) this.iterators.get(this.iteratorIndex);
      // Reset iterator to first position
      it.moveToFirst();
      // If the iterator is valid (i.e., non-empty), return...
      if (it.isValid()) {
        return;
      }
      // ...else try the next one
      ++this.iteratorIndex;
    }
    // If we get here, all iterators are empty.
  }

  public void moveToLast() {
    // See comments on moveToFirst()
    this.iteratorIndex = this.iterators.size() - 1;
    while (this.iteratorIndex >= 0) {
      FSIterator it = (FSIterator) this.iterators.get(this.iteratorIndex);
      it.moveToLast();
      if (it.isValid()) {
        return;
      }
      --this.iteratorIndex;
    }
  }

  public void moveToNext() {
    // No point in going anywhere if iterator is not valid.
    if (!isValid()) {
      return;
    }
    // Grab current iterator and inc.
    FSIterator current = (FSIterator) this.iterators.get(this.iteratorIndex);
    current.moveToNext();
    // If we're ok with the current iterator, return.
    if (current.isValid()) {
      return;
    }
    ++this.iteratorIndex;
    while (this.iteratorIndex < this.iterators.size()) {
      current = (FSIterator) this.iterators.get(this.iteratorIndex);
      current.moveToFirst();
      if (current.isValid()) {
        return;
      }
      ++this.iteratorIndex;
    }
    // If we get here, the iterator is no longer valid, there are no more elements.
  }

  public void moveToPrevious() {
    // No point in going anywhere if iterator is not valid.
    if (!isValid()) {
      return;
    }
    // Grab current iterator and dec.
    FSIterator current = (FSIterator) this.iterators.get(this.iteratorIndex);
    current.moveToPrevious();
    // If we're ok with the current iterator, return.
    if (current.isValid()) {
      return;
    }
    --this.iteratorIndex;
    while (this.iteratorIndex >= 0) {
      current = (FSIterator) this.iterators.get(this.iteratorIndex);
      current.moveToLast();
      if (current.isValid()) {
        return;
      }
      --this.iteratorIndex;
    }
    // If we get here, the iterator is no longer valid, there are no more elements.  Set internal
    // counter to the invalid position.
    this.iteratorIndex = this.iterators.size();
  }

}
