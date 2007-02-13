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
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.SortedIntSet;

public class FSIndexRepositoryImpl implements FSIndexRepositoryMgr, LowLevelIndexRepository {

  // Implementation note: the use of equals() here is pretty hairy and
  // should probably be fixed. We rely on the fact that when two
  // FSIndexComparators are compared, the type of the comparators is
  // ignored! A fix for this would be to split the FSIndexComparator
  // class into two classes, one for the key-comparator pairs, and one
  // for the combination of the two. Note also that we compare two
  // IndexIteratorCachePairs by comparing their
  // index.getComparator()s.

  /**
   * A pair of an index and an iterator cache. An iterator cache is the set of all indexes necessary
   * to create an iterator for the type of the index. compareTo() is based on types and the
   * comparator of the index.
   */
  private class IndexIteratorCachePair implements Comparable {

    // The "root" index, i.e., index of the type of the iterator.
    private FSLeafIndexImpl index = null;

    // A list of indexes (the sub-indexes that we need for an
    // iterator). I.e., one index for each type that's subsumed by the
    // iterator
    // type.
    private ArrayList iteratorCache = null;

    private IndexIteratorCachePair() {
      super();
    }

    // Two IICPs are equal iff their index comparators are equal AND their
    // indexing strategy is the same.
    public boolean equals(Object o) {
      if (!(o instanceof IndexIteratorCachePair)) {
        return false;
      }
      final IndexIteratorCachePair iicp = (IndexIteratorCachePair) o;
      return this.index.getComparator().equals(iicp.index.getComparator())
          && (this.index.getIndexingStrategy() == iicp.index.getIndexingStrategy());
    }

    public int hashCode() {
      throw new UnsupportedOperationException();
    }

    // Populate the cache.
    private void createIndexIteratorCache() {
      if (this.iteratorCache != null) {
        return;
      }
      this.iteratorCache = new ArrayList();
      final Type rootType = this.index.getComparator().getType();
      ArrayList allTypes = null;
      if (this.index.getIndexingStrategy() == FSIndex.DEFAULT_BAG_INDEX) {
        allTypes = new ArrayList();
        allTypes.add(rootType);
      } else {
        allTypes = getAllSubsumedTypes(rootType, FSIndexRepositoryImpl.this.typeSystem);
      }
      final int len = allTypes.size();
      int typeCode, indexPos;
      ArrayList indexList;
      for (int i = 0; i < len; i++) {
        typeCode = ((TypeImpl) allTypes.get(i)).getCode();
        indexList = FSIndexRepositoryImpl.this.indexArray[typeCode];
        indexPos = indexList.indexOf(this);
        if (indexPos >= 0) {
          this.iteratorCache.add(((IndexIteratorCachePair) indexList.get(indexPos)).index);
        }
      }
    }

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(Object o) {
      IndexIteratorCachePair cp = (IndexIteratorCachePair) o;
      final int typeCode1 = ((TypeImpl) this.index.getType()).getCode();
      final int typeCode2 = ((TypeImpl) cp.index.getType()).getCode();
      if (typeCode1 < typeCode2) {
        return -1;
      } else if (typeCode1 > typeCode2) {
        return 1;
      } else { // types are equal
        return this.index.getComparator().compareTo(cp.index.getComparator());
      }
    }

    int size() {
      int size = 0;
      for (int i = 0; i < this.iteratorCache.size(); i++) {
        size += ((LowLevelIndex) this.iteratorCache.get(i)).size();
      }
      return size;
    }

  }

  /**
   * Comparator wrapper; will used wrapped comparator, and on equality use address of FS for further
   * distinction.
   */
  private static class IteratorComparator implements IntComparator {

    private final IntComparator comp;

    private IteratorComparator(IntComparator comp) {
      super();
      this.comp = comp;
    }

    /**
     * @see org.apache.uima.internal.util.IntComparator#compare(int, int)
     */
    public int compare(int i, int j) {
      final int compResult = this.comp.compare(i, j);
      if (compResult == 0) {
        if (i < j) {
          return -1;
        } else if (i > j) {
          return 1;
        }
      }
      return compResult;
    }

  }

  /**
   * The iterator implementation for indexes. Tricky because the iterator needs to be able to move
   * backwards as well as forwards.
   */
  private class PointerIterator implements IntPointerIterator, LowLevelIterator {

    // The IICP
    private IndexIteratorCachePair iicp;

    // An array of integer arrays, one for each subtype.
    private ComparableIntPointerIterator[] indexes;

    // snapshot to detectIllegalIndexUpdates
    // need to move this to ComparableIntPointerIterator so it can be tested

    // Size of index (iterator) array.
    private int indexesSize;

    // The number of currently active indexes (some may be invalid).
    private int numIndexes;

    // The current index, i.e., the index that contains the current element.
    private int currentIndex;

    // Remember the direction of the previous move, so we can save ourselves
    // some work.
    private boolean wentForward;

    // Comparator that is used to compare FS addresses for the purposes of
    // iteration. If two FSs are identical wrt the comparator of the index,
    // we still need to be able to distinguish them to be able to have a
    // well-defined sequence. In that case, we arbitrarily order FSs by
    // their
    // addresses. We need to do this in order to be able to ensure that a
    // reverse iterator produces the reverse order of the forward iterator.
    private IntComparator iteratorComparator;

    // The next element in the iterator. When next < 0, there is no
    // next.
    // private int next;

    private PointerIterator() {
      super();
    }

    private void initPointerIterator(IndexIteratorCachePair iicp0) {
      this.iicp = iicp0;
      // Make sure the iterator cache exists.
      iicp0.createIndexIteratorCache();
      ArrayList iteratorCache = iicp0.iteratorCache;
      this.indexesSize = iteratorCache.size();
      this.indexes = new ComparableIntPointerIterator[this.indexesSize];
      this.numIndexes = this.indexesSize;
      this.iteratorComparator = new IteratorComparator((FSLeafIndexImpl) iteratorCache.get(0));
      ComparableIntPointerIterator it;
      for (int i = 0; i < this.indexesSize; i++) {
        final FSLeafIndexImpl leafIndex = ((FSLeafIndexImpl) iteratorCache.get(i));
        it = leafIndex.pointerIterator(this.iteratorComparator,
            FSIndexRepositoryImpl.this.detectIllegalIndexUpdates, ((TypeImpl) leafIndex.getType())
                .getCode());
        this.indexes[i] = it;
      }
    }

    private PointerIterator(IndexIteratorCachePair iicp) {
      super();
      initPointerIterator(iicp);
      moveToFirst();
    }

    private PointerIterator(IndexIteratorCachePair iicp, int fs) {
      super();
      initPointerIterator(iicp);
      moveTo(fs);
    }

    public boolean isValid() {
      // We're valid as long as at least one index is.
      return (this.numIndexes > 0);
    }

    private ComparableIntPointerIterator checkConcurrentModification(int i) {
      ComparableIntPointerIterator cipi = this.indexes[i];
      if (cipi.isConcurrentModification())
        throw new ConcurrentModificationException();
      return cipi;
    }

    private void resetConcurrentModification(int i) {
      ComparableIntPointerIterator cipi = this.indexes[i];
      cipi.resetConcurrentModification();
    }

    private void checkConcurrentModificationAll() {
      for (int i = 0; i < this.indexes.length; i++) {
        checkConcurrentModification(i);
      }
    }

    public void moveToFirst() {
      for (int i = 0; i < this.indexes.length; i++) {
        resetConcurrentModification(i);
        this.indexes[i].moveToFirst();
      }
      this.numIndexes = this.indexes.length;
      checkIndexesTo(this.numIndexes);
      // bubbleSort(indexes, numIndexes);
      Arrays.sort(this.indexes, 0, this.numIndexes);
      this.wentForward = true;
      return;
    }

    public void moveToLast() {
      for (int i = 0; i < this.indexes.length; i++) {
        resetConcurrentModification(i);
        this.indexes[i].moveToLast();
      }
      this.numIndexes = this.indexes.length;
      checkIndexesTo(this.numIndexes);
      // bubbleSort(indexes, numIndexes);
      Arrays.sort(this.indexes, 0, this.numIndexes);
      this.currentIndex = (this.numIndexes - 1);
      this.wentForward = false;
      return;
    }

    public void moveToNext() {
      // If we're not valid, return.
      if (!isValid()) {
        return;
      }
      checkConcurrentModificationAll();
      // Increment iterators, taking into account which direction the last
      // move
      // was in.
      boolean tempWentForward = this.wentForward;
      incrementIterators();
      // If we're not valid, return.
      if (!isValid()) {
        return;
      }
      // The individual iterators are pointing at the correct elements,
      // and
      // we can simply sort them to find the next one.
      // bubbleSort(indexes, numIndexes);
      // Arrays.sort(indexes, 0, numIndexes);
      if (tempWentForward) {
        insert(0, this.indexes, this.numIndexes);
      } else {
        Arrays.sort(this.indexes, 0, this.numIndexes);
      }
      // Moving up, the smallest element is the next one to show.
      this.currentIndex = 0;
      return;
    }

    private final void insert(int pos, Comparable[] array, int size) {
      final int max = size - 1;
      int comp, next;
      Comparable tmp;
      while (pos < max) {
        next = pos + 1;
        comp = array[pos].compareTo(array[next]);
        if (comp <= 0) {
          return;
        }
        tmp = array[pos];
        array[pos] = array[next];
        array[next] = tmp;
        ++pos;
      }
    }

    private void ensureIndexValidity(int index) {
      // assert(index >= 0);
      // assert(index < numIndexes);
      if (!this.indexes[index].isValid()) {
        // If the index is not valid, we throw it out.
        if ((index + 1) == this.numIndexes) {
          // If the index was the last index in the array, we just
          // shrink the
          // array.
          --this.numIndexes;
        } else {
          // Else we shrink the array and swap the previously last
          // element to
          // the position where we want to delete the index.
          --this.numIndexes;
          ComparableIntPointerIterator tempIt = this.indexes[index];
          this.indexes[index] = this.indexes[this.numIndexes];
          this.indexes[this.numIndexes] = tempIt;
        }
      }
    }

    private void checkIndexesTo(int max) {
      // Because of the way checkIndexValidity() works, we need to work
      // back
      // to front.
      for (int i = (max - 1); i >= 0; i--) {
        ensureIndexValidity(i);
      }
    }

    private void incrementIterators() {
      if (this.wentForward) {
        // This is the easy case. We just need to increment the current
        // index.
        this.indexes[this.currentIndex].inc();
        // Make sure it's still valid.
        ensureIndexValidity(this.currentIndex);
      } else {
        // Else we need to increment everything, including the currently
        // inactive indexes!
        ComparableIntPointerIterator it;
        for (int i = 0; i < this.indexesSize; i++) {
          // Any iterator other than the current one needs to be
          // incremented
          // until it's pointing at something that's greater than the
          // current
          // element.
          if (i != this.currentIndex) {
            it = this.indexes[i];
            // If the iterator we're considering is not valid, we
            // set it to the
            // first element. This should be it for this iterator...
            if (!it.isValid()) {
              it.moveToFirst();
            }
            // while (it.isValid() &&
            // (it.compareTo(indexes[this.currentIndex]) < 0)) {
            // Increment the iterator while it is valid and pointing
            // at something
            // smaller than the current element.
            while (it.isValid()
                && (this.iteratorComparator
                    .compare(it.get(), this.indexes[this.currentIndex].get()) < 0)) {
              it.inc();
            }
          }
        }
        // Increment the current index.
        this.indexes[this.currentIndex].inc();
        // Set number of this.indexes to all indexes.
        this.numIndexes = this.indexesSize;
        // Ensure validity of all active iterators.
        checkIndexesTo(this.numIndexes);
      }
      this.wentForward = true;
    }

    public void moveToPrevious() {
      // If we're not valid, return.
      if (!isValid()) {
        return;
      }
      checkConcurrentModificationAll();
      // Decrement iterators, taking into account which direction the last
      // move
      // was in.
      decrementIterators();
      // If we're not valid, return.
      if (!isValid()) {
        return;
      }
      // bubbleSort(indexes, this.numIndexes);
      Arrays.sort(this.indexes, 0, this.numIndexes);
      this.currentIndex = (this.numIndexes - 1);
      return;
    }

    private void decrementIterators() {
      // Note: this does not sort the iterators.
      if (!this.wentForward) {
        // This is the easy case. We just need to decrement the current
        // index.
        this.indexes[this.currentIndex].dec();
        ensureIndexValidity(this.currentIndex);
      } else {
        // Else the current index is fine, but we have to decrement all
        // indexes.
        ComparableIntPointerIterator it;
        for (int i = 0; i < this.indexesSize; i++) {
          if (i != this.currentIndex) {
            it = this.indexes[i];
            // while (it.isValid() &&
            // (it.compareTo(indexes[this.currentIndex]) > 0)) {
            if (!it.isValid()) {
              it.moveToLast();
            }
            while (it.isValid()
                && (this.iteratorComparator
                    .compare(it.get(), this.indexes[this.currentIndex].get()) > 0)) {
              it.dec();
            }
          }
        }
        this.indexes[this.currentIndex].dec();
        this.numIndexes = this.indexesSize;
        checkIndexesTo(this.numIndexes);
      }
      this.wentForward = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#ll_get()
     */
    public int get() throws NoSuchElementException {
      return ll_get();
    }

    public int ll_get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return checkConcurrentModification(this.currentIndex).get();
    }

    public Object copy() {
      // If this.isValid(), return a copy pointing to the same element.
      if (this.isValid()) {
        return new PointerIterator(this.iicp, this.get());
      }
      // Else, create a copy that is also not valid.
      PointerIterator pi = new PointerIterator(this.iicp);
      pi.moveToFirst();
      pi.moveToPrevious();
      return pi;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    public void moveTo(int fs) {
      // Need to consider all iterators.
      this.numIndexes = this.indexes.length;
      // Set all iterators to insertion point.
      for (int i = 0; i < this.numIndexes; i++) {
        resetConcurrentModification(i);
        this.indexes[i].moveTo(fs);
      }
      // Check validity of all indexes.
      checkIndexesTo(this.numIndexes);
      // Sort the valid indexes.
      if (this.numIndexes > 1) {
        Arrays.sort(this.indexes, 0, this.numIndexes);
      }
      // bubbleSort(indexes, numIndexes);
      // The way we compute the insertion point, we're look forward.
      this.wentForward = true;
      this.currentIndex = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToNext()
     */
    public void inc() {
      moveToNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToPrevious()
     */
    public void dec() {
      moveToPrevious();
    }

    public int ll_indexSize() {
      return this.iicp.size();
    }

    public LowLevelIndex ll_getIndex() {
      return this.iicp.index;
    }

  }

  private class IndexImpl implements FSIndex, FSIndexImpl {

    private IndexIteratorCachePair iicp;

    private IndexImpl(IndexIteratorCachePair iicp) {
      super();
      this.iicp = iicp;
    }

    public int ll_compare(int ref1, int ref2) {
      return this.iicp.index.ll_compare(ref1, ref2);
    }

    public int getIndexingStrategy() {
      return this.iicp.index.getIndexingStrategy();
    }

    public FSIndexComparator getComparator() {
      return this.iicp.index.getComparator();
    }

    protected IntComparator getIntComparator() {
      return this.iicp.index.getIntComparator();
    }

    public void flush() {
      this.iicp.index.flush();
    }

    /**
     * @see org.apache.uima.cas.FSIndex#compare(FeatureStructure, FeatureStructure)
     */
    public int compare(FeatureStructure fs1, FeatureStructure fs2) {
      return this.iicp.index.compare(fs1, fs2);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
     */
    public boolean contains(FeatureStructure fs) {
      return this.iicp.index.contains(fs);
    }

    public FeatureStructure find(FeatureStructure fs) {
      return this.iicp.index.find(fs);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#getType()
     */
    public Type getType() {
      return this.iicp.index.getType();
    }

    /**
     * @see org.apache.uima.cas.FSIndex#iterator()
     */
    public FSIterator iterator() {
      return new FSIteratorWrapper(new PointerIterator(this.iicp), FSIndexRepositoryImpl.this.cas);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#iterator(FeatureStructure)
     */
    public FSIterator iterator(FeatureStructure fs) {
      return new FSIteratorWrapper(new PointerIterator(this.iicp, ((FeatureStructureImpl) fs)
          .getAddress()), FSIndexRepositoryImpl.this.cas);
    }

    public IntPointerIterator getIntIterator() {
      return new PointerIterator(this.iicp);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#size()
     */
    public int size() {
      this.iicp.createIndexIteratorCache();
      // int size = this.iicp.index.size();
      int size = 0;
      final ArrayList subIndex = this.iicp.iteratorCache;
      final int max = subIndex.size();
      for (int i = 0; i < max; i++) {
        size += ((FSIndex) subIndex.get(i)).size();
      }
      return size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIndex#ll_iterator()
     */
    public LowLevelIterator ll_iterator() {
      return new PointerIterator(this.iicp);
    }

    public LowLevelIterator ll_iterator(boolean ambiguous) {
      if (ambiguous) {
        return this.ll_iterator();
      }
      return new LLUnambiguousIteratorImpl(this.ll_iterator(), this.iicp.index.lowLevelCAS);
    }

  }

  // private class AnnotIndexImpl
  // extends IndexImpl
  // implements AnnotationIndex, FSIndexImpl {
  //
  // private AnnotIndexImpl(IndexIteratorCachePair iicp) {
  // super(iicp);
  // }
  //
  // public FSIterator subiterator(AnnotationFS annot) {
  // return new Subiterator(
  // this.getIntIterator(),
  // annot,
  // (CASImpl) FSIndexRepositoryImpl.this.cas,
  // this.getIntComparator());
  // }
  //
  // public FSIterator subiterator(AnnotationFS annot, boolean ambiguous) {
  // if (ambiguous) {
  // return subiterator(annot);
  // } else {
  // return new UnambiguousIterator(subiterator(annot), this);
  // }
  // }
  //
  // public FSIterator unambigousIterator() {
  // return new UnambiguousIterator(iterator(), this);
  // }
  //
  // }

  /**
   * The default size of an index.
   */
  public static final int DEFAULT_INDEX_SIZE = 100;

  // A reference to the CAS.
  private CASImpl cas;

  // A reference to the type system.
  private TypeSystemImpl typeSystem;

  // Is the index repository locked?
  private boolean locked = false;

  // An array of ArrayLists, one for each type in the type hierarchy.
  // The ArrayLists are unordered lists of IndexIteratorCachePairs for
  // that type.
  private ArrayList[] indexArray;

  // an array of ints, one for each type in the type hierarchy.
  // Used to enable iterators to detect modifications (adds / removes)
  // to indexes they're iterating over while they're iterating over them.
  // not private so it can be seen by FSLeafIndexImpl
  int[] detectIllegalIndexUpdates;

  // A map from names to IndexIteratorCachePairs. Different names may map to
  // the same index.
  private HashMap name2indexMap;

  private LinearTypeOrderBuilder defaultOrderBuilder = null;

  private LinearTypeOrder defaultTypeOrder = null;

  private FSIndexRepositoryImpl() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param cas
   */
  FSIndexRepositoryImpl(CASImpl cas) {
    super();
    this.cas = cas;
    this.typeSystem = cas.getTypeSystemImpl();
    this.name2indexMap = new HashMap();
    init();
  }

  /**
   * Constructor for views.
   * 
   * @param cas
   * @param baseIndexRepository
   */
  FSIndexRepositoryImpl(CASImpl cas, FSIndexRepositoryImpl baseIndexRepo) {
    super();
    this.cas = cas;
    this.typeSystem = cas.getTypeSystemImpl();
    this.name2indexMap = new HashMap();
    init();
    Set keys = baseIndexRepo.name2indexMap.keySet();
    if (!keys.isEmpty()) {
      Iterator keysIter = keys.iterator();
      while (keysIter.hasNext()) {
        String key = (String) keysIter.next();
        IndexIteratorCachePair iicp = (IndexIteratorCachePair) baseIndexRepo.name2indexMap.get(key);
        createIndexNoQuestionsAsked(iicp.index.getComparator(), key, iicp.index
            .getIndexingStrategy());
      }
    }
    this.defaultOrderBuilder = baseIndexRepo.defaultOrderBuilder;
    this.defaultTypeOrder = baseIndexRepo.defaultTypeOrder;
  }

  /**
   * Initialize data. Called from the constructor.
   */
  private void init() {
    TypeSystemImpl ts = this.typeSystem;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    this.indexArray = new ArrayList[numTypes];
    for (int i = 1; i < numTypes; i++) {
      this.indexArray[i] = new ArrayList();
    }
    this.detectIllegalIndexUpdates = new int[numTypes];
    for (int i = 0; i < this.detectIllegalIndexUpdates.length; i++) {
      this.detectIllegalIndexUpdates[i] = Integer.MIN_VALUE;
    }
  }

  /**
   * Reset all indexes.
   */
  public void flush() {
    if (!this.locked) {
      return;
    }
    int max;
    ArrayList v;
    // The first element is null. This is not good...
    for (int i = 1; i < this.indexArray.length; i++) {
      v = this.indexArray[i];
      max = v.size();
      for (int j = 0; j < max; j++) {
        ((IndexIteratorCachePair) v.get(j)).index.flush();
      }
    }
  }

  public void addFS(int fsRef) {
    ll_addFS(fsRef);
  }

  private IndexIteratorCachePair addNewIndex(FSIndexComparator comparator, int indexType) {
    return addNewIndex(comparator, DEFAULT_INDEX_SIZE, indexType);
  }

  /**
   * This is where the actual index gets created.
   */
  private IndexIteratorCachePair addNewIndex(FSIndexComparator comparator, int initialSize,
      int indexType) {
    final Type type = comparator.getType();
    final int typeCode = ((TypeImpl) type).getCode();
    if (typeCode >= this.indexArray.length) {
      // assert(false);
    }
    final ArrayList indexVector = this.indexArray[typeCode];
    // final int vecLen = indexVector.size();
    FSLeafIndexImpl ind;
    switch (indexType) {
    case FSIndex.SET_INDEX: {
      ind = new FSRBTSetIndex(this.cas, type, indexType);
      break;
    }
    case FSIndex.BAG_INDEX: {
      ind = new FSBagIndex(this.cas, type, initialSize, indexType);
      break;
    }
    case FSIndex.DEFAULT_BAG_INDEX: {
      ind = new FSBagIndex(this.cas, type, initialSize, indexType);
      break;
    }
    default: {
      // SORTED_INDEX is the default. We don't throw any errors, if the
      // code
      // is unknown, we just create a sorted index (with duplicates).
      // ind = new FSRBTIndex(this.cas, type, FSIndex.SORTED_INDEX);
      ind = new FSIntArrayIndex(this.cas, type, initialSize, FSIndex.SORTED_INDEX);
      break;
    }
    }
    // ind = new FSRBTIndex(this.cas, type);
    // ind = new FSVectorIndex(this.cas, initialSize);
    ind.init(comparator);
    IndexIteratorCachePair iicp = new IndexIteratorCachePair();
    iicp.index = ind;
    indexVector.add(iicp);
    return iicp;
  }

  /*
   * private IndexIteratorCachePair addIndex( FSIndexComparator comparator, int initialSize) { final
   * Type type = comparator.getType(); final int typeCode = ((TypeImpl) type).getCode(); final
   * Vector indexVector = this.indexArray[typeCode]; final int vecLen = indexVector.size();
   * FSLeafIndexImpl ind;
   * 
   * for (int i = 0; i < vecLen; i++) { ind = ((IndexIteratorCachePair) indexVector.get(i)).index;
   * if (comparator.equals(ind.getComparator())) { return null; } }
   * 
   * ind = new FSRBTIndex(this.cas, type); // ind = new FSVectorIndex(this.cas, initialSize);
   * ind.init(comparator); IndexIteratorCachePair iicp = new IndexIteratorCachePair(); iicp.index =
   * ind; indexVector.add(iicp); return iicp; }
   */
  // private IndexIteratorCachePair addIndexRecursive(FSIndexComparator
  // comparator) {
  // final FSIndexComparatorImpl compCopy =
  // ((FSIndexComparatorImpl) comparator).copy();
  // return addIndexRec(compCopy);
  // }
  private IndexIteratorCachePair addNewIndexRecursive(FSIndexComparator comparator, int indexType) {
    final FSIndexComparatorImpl compCopy = ((FSIndexComparatorImpl) comparator).copy();
    return addNewIndexRec(compCopy, indexType);
  }

  private static final int findIndex(ArrayList indexes, FSIndexComparator comp) {
    FSIndexComparator indexComp;
    final int max = indexes.size();
    for (int i = 0; i < max; i++) {
      indexComp = ((IndexIteratorCachePair) indexes.get(i)).index.getComparator();
      if (comp.equals(indexComp)) {
        return i;
      }
    }
    return -1;
  }

  /*
   * // Will modify comparator, so call with copy. private IndexIteratorCachePair
   * addIndexRec(FSIndexComparator comp) { FSIndexComparator compCopy; IndexIteratorCachePair cp =
   * this.addIndex(comp); if (cp == null) { return null; // The index already exists. } final Type
   * superType = comp.getType(); final Vector types =
   * this.typeSystem.getDirectlySubsumedTypes(superType); final int max = types.size(); for (int i =
   * 0; i < max; i++) { compCopy = ((FSIndexComparatorImpl)comp).copy(); compCopy.setType((Type)
   * types.get(i)); addIndexRec(compCopy); } return cp; }
   */
  // Will modify comparator, so call with copy.
  private IndexIteratorCachePair addNewIndexRec(FSIndexComparator comparator, int indexType) {
    IndexIteratorCachePair iicp = this.addNewIndex(comparator, indexType);
    if (indexType == FSIndex.DEFAULT_BAG_INDEX) {
      // In this special case, we do not add indeces for subtypes.
      return iicp;
    }
    final Type superType = comparator.getType();
    final Vector types = this.typeSystem.getDirectlySubsumedTypes(superType);
    final int max = types.size();
    FSIndexComparator compCopy;
    for (int i = 0; i < max; i++) {
      compCopy = ((FSIndexComparatorImpl) comparator).copy();
      compCopy.setType((Type) types.get(i));
      addNewIndexRec(compCopy, indexType);
    }
    return iicp;
  }

  private static final ArrayList getAllSubsumedTypes(Type t, TypeSystem ts) {
    ArrayList v = new ArrayList();
    addAllSubsumedTypes(t, ts, v);
    return v;
  }

  private static final void addAllSubsumedTypes(Type t, TypeSystem ts, ArrayList v) {
    v.add(t);
    List sub = ts.getDirectSubtypes(t);
    final int len = sub.size();
    for (int i = 0; i < len; i++) {
      addAllSubsumedTypes((Type) sub.get(i), ts, v);
    }
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#commit()
   */
  public void commit() {
    // Will create the default type order if it doesn't exist at this point.
    getDefaultTypeOrder();
    this.locked = true;
  }

  public LinearTypeOrder getDefaultTypeOrder() {
    if (this.defaultTypeOrder == null) {
      if (this.defaultOrderBuilder == null) {
        this.defaultOrderBuilder = new LinearTypeOrderBuilderImpl(this.typeSystem);
      }
      try {
        this.defaultTypeOrder = this.defaultOrderBuilder.getOrder();
      } catch (CASException e) {
        // Since we're doing this on an existing type names, we can't
        // get here.
      }
    }
    return this.defaultTypeOrder;
  }

  public LinearTypeOrderBuilder getDefaultOrderBuilder() {
    if (this.defaultOrderBuilder == null) {
      this.defaultOrderBuilder = new LinearTypeOrderBuilderImpl(this.typeSystem);
    }
    return this.defaultOrderBuilder;
  }

  void setDefaultTypeOrder(LinearTypeOrder order) {
    this.defaultTypeOrder = order;
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndex(FSIndexComparator, String)
   */
  public boolean createIndex(FSIndexComparator comp, String label, int indexType)
      throws CASAdminException {
    if (this.locked) {
      throw new CASAdminException(CASAdminException.REPOSITORY_LOCKED);
    }
    return createIndexNoQuestionsAsked(comp, label, indexType);
  }

  /**
   * This is public only until the xml specifier format supports specifying index kinds (set, bag
   * etc.).
   * 
   * @param comp
   * @param label
   * @param indexType
   * @return boolean
   */
  public boolean createIndexNoQuestionsAsked(FSIndexComparator comp, String label, int indexType) {
    IndexIteratorCachePair cp = (IndexIteratorCachePair) this.name2indexMap.get(label);
    // Now check if the index already exists.
    if (cp == null) {
      // The name is new.
      cp = this.addNewIndexRecursive(comp, indexType);
      this.name2indexMap.put(label, cp);
      return true;
    }
    // For now, just return false if the label already exists.
    return false;
    // // An index has previously been registered for this name. We need to
    // // compare the types to see if the new addition is compatible with
    // the
    // // pre-existing one. There are three cases: the new type can be a
    // sub-type
    // // of the old one, in which case we don't need to do anything; or,
    // the
    // // new type is a super-type of the old one, in which case we add the
    // new
    // // index while keeping the old one; or, there is no subsumption
    // relation,
    // // in which case we can't add the index.
    // Type oldType = cp.index.getType(); // Get old type from the index.
    // Type newType = comp.getType(); // Get new type from comparator.
    // if (this.typeSystem.subsumes(oldType, newType)) {
    // // We don't need to do anything.
    // return true;
    // } else if (this.typeSystem.subsumes(newType, oldType)) {
    // // Add the index, subsuming the old one.
    // cp = this.addIndexRecursive(comp);
    // // Replace the old index with the new one in the map.
    // this.name2indexMap.put(label, cp);
    // return true;
    // } else {
    // // Can't add index under that name.
    // return false;
    // }
    // }
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#getIndexes()
   */
  public Iterator getIndexes() {
    ArrayList indexList = new ArrayList();
    Iterator it = this.getLabels();
    String label;
    while (it.hasNext()) {
      label = (String) it.next();
      indexList.add(getIndex(label));
    }
    return indexList.iterator();
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#getLabels()
   */
  public Iterator getLabels() {
    return this.name2indexMap.keySet().iterator();
  }

  /**
   * Get the labels for a specific comparator.
   * 
   * @param comp
   *          The comparator.
   * @return An iterator over the labels.
   */
  public Iterator getLabels(FSIndexComparator comp) {
    final ArrayList labels = new ArrayList();
    Iterator it = this.getLabels();
    String label;
    while (it.hasNext()) {
      label = (String) it.next();
      if (((IndexIteratorCachePair) this.name2indexMap.get(label)).index.getComparator().equals(
          comp)) {
        labels.add(label);
      }
    }
    return labels.iterator();
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String, Type)
   */
  public FSIndex getIndex(String label, Type type) {
    IndexIteratorCachePair iicp = (IndexIteratorCachePair) this.name2indexMap.get(label);
    if (iicp == null) {
      return null;
    }
    // Why is this necessary?
    if (type.isArray()) {
      Type componentType = type.getComponentType();
      if (componentType != null && !componentType.isPrimitive()
          && !componentType.getName().equals(CAS.TYPE_NAME_TOP)) {
        return null;
      }
    }
    Type indexType = iicp.index.getType();
    if (!this.typeSystem.subsumes(indexType, type)) {
      CASRuntimeException cre = new CASRuntimeException(CASRuntimeException.TYPE_NOT_IN_INDEX,
          new String[] { label, type.getName(), indexType.getName() });
      throw cre;
    }
    final int typeCode = ((TypeImpl) type).getCode();
    ArrayList inds = this.indexArray[typeCode];
    // Since we found an index for the correct type, find() must return a
    // valid result -- unless this is a special auto-index.
    final int indexCode = findIndex(inds, iicp.index.getComparator());
    if (indexCode < 0) {
      return null;
    }
    // assert((indexCode >= 0) && (indexCode < inds.size()));
    return new IndexImpl((IndexIteratorCachePair) inds.get(indexCode));
    // return ((IndexIteratorCachePair)inds.get(indexCode)).index;
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String)
   */
  public FSIndex getIndex(String label) {
    IndexIteratorCachePair iicp = (IndexIteratorCachePair) this.name2indexMap.get(label);
    if (iicp == null) {
      return null;
    }
    return new IndexImpl(iicp);
    // return ((IndexIteratorCachePair)name2indexMap.get(label)).index;
  }

  public IntPointerIterator getIntIteratorForIndex(String label) {
    IndexImpl index = (IndexImpl) getIndex(label);
    if (index == null) {
      return null;
    }
    return new PointerIterator(index.iicp);
  }

  public IntPointerIterator getIntIteratorForIndex(String label, Type type) {
    IndexImpl index = (IndexImpl) getIndex(label, type);
    if (index == null) {
      return null;
    }
    return new PointerIterator(index.iicp);
  }

  /**
   */
  public int getIndexSize(Type type) {
    final int typeCode = ((TypeImpl) type).getCode();
    final ArrayList indexVector = this.indexArray[typeCode];
    if (indexVector.size() == 0) {
      // No index for this type exists.
      return 0;
    }
    int numFSs = ((IndexIteratorCachePair) indexVector.get(0)).index.size();
    final Vector typeVector = this.typeSystem.getDirectlySubsumedTypes(type);
    final int max = typeVector.size();
    for (int i = 0; i < max; i++) {
      numFSs += getIndexSize((Type) typeVector.get(i));
    }
    return numFSs;
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createComparator()
   */
  public FSIndexComparator createComparator() {
    return new FSIndexComparatorImpl(this.cas);
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#isCommitted()
   */
  public boolean isCommitted() {
    return this.locked;
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndexComparator()
   */
  // public FSIndexComparator createIndexComparator() {
  // return new FSIndexComparatorImpl(this.cas);
  // }
  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndex(org.apache.uima.cas.admin.FSIndexComparator,
   *      java.lang.String)
   */
  public boolean createIndex(FSIndexComparator comp, String label) throws CASAdminException {
    return createIndex(comp, label, FSIndex.SORTED_INDEX);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Serialization support

  /**
   * Return an array containing all FSs in any index. This is intended to be used for serialization.
   * Note that duplicate entries in indexes will appear in the array as many times as they occur in
   * an index. The order in which FSs occur in the array does not reflect the order in which they
   * were added to the repository. This means that set indexes deserialized from this list may
   * contain different but equal elements than the original index.
   */
  public int[] getIndexedFSs() {
    IntVector v = new IntVector();
    IndexIteratorCachePair iicp;
    IntPointerIterator it;
    ArrayList iv, cv;
    // We may need to profile this. If this is a bottleneck, use a different
    // implementation.
    SortedIntSet set;
    int jMax, indStrat;
    // Iterate over all types.
    for (int i = 0; i < this.indexArray.length; i++) {
      iv = this.indexArray[i];
      if (iv == null) {
        // The 0 position is the only one that should be null.
        continue;
      }
      // Iterate over the indexes for the type.
      jMax = iv.size();
      // Create a vector of IICPs. If there is at least one sorted or bag
      // index, pick one arbitrarily and add its FSs (since it contains all
      // FSs that all other indexes for the same type contain). If there are
      // only set indexes, create a set of the FSs in those indexes, since they
      // may all contain different elements (FSs that are duplicates for one
      // index may not be duplicates for a different one).
      cv = new ArrayList();
      for (int j = 0; j < jMax; j++) {
        iicp = (IndexIteratorCachePair) iv.get(j);
        indStrat = iicp.index.getIndexingStrategy();
        if (indStrat == FSIndex.SET_INDEX) {
          cv.add(iicp);
        } else {
          if (cv.size() > 0) {
            cv = new ArrayList();
          }
          cv.add(iicp);
          break;
        }
      }
      if (cv.size() > 0) {
        set = new SortedIntSet();
        for (int k = 0; k < cv.size(); k++) {
          it = ((IndexIteratorCachePair) cv.get(k)).index.refIterator();
          while (it.isValid()) {
            set.add(it.get());
            it.inc();
          }
        }
        for (int k = 0; k < set.size(); k++) {
          v.add(set.get(k));
        }
      }
    }
    return v.toArray();
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#addFS(org.apache.uima.cas.FeatureStructure)
   */
  public void addFS(FeatureStructure fs) {
    addFS(((FeatureStructureImpl) fs).getAddress());
  }

  private void incrementIllegalIndexUpdateDetector(int typeCode) {
    this.detectIllegalIndexUpdates[typeCode]++;
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#removeFS(org.apache.uima.cas.FeatureStructure)
   */
  public void removeFS(FeatureStructure fs) {
    ll_removeFS(this.cas.ll_getFSRef(fs));

    // final int typeCode =
    // this.cas.ll_getFSRefType(this.cas.ll_getFSRef(fs));
    // // final TypeImpl type = (TypeImpl) fs.getType();
    // ArrayList idxList = this.indexArray[typeCode];
    // final int max = idxList.size();
    // incrementIllegalIndexUpdateDetector(typeCode);
    // for (int i = 0; i < max; i++) {
    // ((IndexIteratorCachePair) idxList.get(i)).index.deleteFS(fs);
    // }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createTypeSortOrder()
   */
  public LinearTypeOrderBuilder createTypeSortOrder() {
    LinearTypeOrderBuilder orderBuilder = new LinearTypeOrderBuilderImpl(this.typeSystem);
    if (this.defaultOrderBuilder == null) {
      this.defaultOrderBuilder = orderBuilder;
    }
    return orderBuilder;
  }

  // private static final void bubbleSort(Object[] array, int end)
  // {
  // int comp;
  // Object tmp;
  // for (int i = (end - 1); i >= 0; i--)
  // {
  // for (int j = 1; j <= i; j++)
  // {
  // comp = ((Comparable) array[j - 1]).compareTo(array[j]);
  // if (comp > 0)
  // {
  // tmp = array[j - 1];
  // array[j - 1] = array[j];
  // array[j] = tmp;
  // }
  // }
  // }
  // }

  public LowLevelIndex ll_getIndex(String indexName) {
    return (LowLevelIndex) getIndex(indexName);
  }

  public LowLevelIndex ll_getIndex(String indexName, int typeCode) {
    if (!this.typeSystem.isType(typeCode) || !this.cas.isFSRefType(typeCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.INVALID_INDEX_TYPE);
      e.addArgument(Integer.toString(typeCode));
      throw e;
    }
    return (LowLevelIndex) getIndex(indexName, this.typeSystem.ll_getTypeForCode(typeCode));
  }

  public final void ll_addFS(int fsRef, boolean doChecks) {
    if (doChecks) {
      this.cas.checkFsRef(fsRef);
      this.cas.isFSRefType(this.cas.ll_getFSRefType(fsRef));
    }
    ll_addFS(fsRef);
  }

  public void ll_addFS(int fsRef) {
    // Determine type of FS.
    final int typeCode = this.cas.getHeapValue(fsRef);
    // indicate this type's indexes are being modified
    // in case an iterator is simultaneously active over this type
    incrementIllegalIndexUpdateDetector(typeCode);
    // Get the indexes for the type.
    final ArrayList indexes = this.indexArray[typeCode];
    // Add fsRef to all indexes.
    final int size = indexes.size();
    for (int i = 0; i < size; i++) {
      ((IndexIteratorCachePair) indexes.get(i)).index.insert(fsRef);
    }
    if (size == 0) {
      // lazily create a default bag index for this type
      Type type = this.typeSystem.getType(typeCode);
      String defIndexName = getAutoIndexNameForType(type);
      FSIndexComparator comparator = createComparator();
      comparator.setType(type);
      createIndexNoQuestionsAsked(comparator, defIndexName, FSIndex.DEFAULT_BAG_INDEX);
      assert this.indexArray[typeCode].size() == 1;
      // add the FS to the bag index
      ((IndexIteratorCachePair) this.indexArray[typeCode].get(0)).index.insert(fsRef);
    }
  }

  private static final String getAutoIndexNameForType(Type type) {
    return "_" + type.getName() + "_GeneratedIndex";
  }

  public void ll_removeFS(int fsRef) {
    final int typeCode = this.cas.ll_getFSRefType(fsRef);
    incrementIllegalIndexUpdateDetector(typeCode);
    ArrayList idxList = this.indexArray[typeCode];
    final int max = idxList.size();
    for (int i = 0; i < max; i++) {
      ((IndexIteratorCachePair) idxList.get(i)).index.remove(fsRef);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndexRepository#getAllIndexedFS(org.apache.uima.cas.Type)
   */
  public FSIterator getAllIndexedFS(Type type) {
    List iteratorList = new ArrayList();
    getAllIndexedFS(type, iteratorList);
    return new FSIteratorAggregate(iteratorList);
  }

  private final void getAllIndexedFS(Type type, List iteratorList) {
    // Start by looking for an auto-index. If one exists, no other index exists.
    FSIndex autoIndex = getIndex(getAutoIndexNameForType(type));
    if (autoIndex != null) {
      iteratorList.add(autoIndex.iterator());
      // We found one of the special auto-indexes which don't inherit down the tree. So, we
      // manually need to traverse the inheritance tree to look for more indexes. Note that
      // this is not necessary when we have a regular index
      List subtypes = this.typeSystem.getDirectSubtypes(type);
      for (int i = 0; i < subtypes.size(); i++) {
        getAllIndexedFS((Type) subtypes.get(i), iteratorList);
      }
      return;
    }
    // Attempt to find a non-set index first.
    // If none found, then use the an arbitrary set index if any.
    FSIndex setIndex = null;
    Iterator iter = getLabels();
    while (iter.hasNext()) {
      String label = (String) iter.next();
      FSIndex index = getIndex(label);
      // Ignore auto-indexes at this stage, they're handled above.
      if (index.getIndexingStrategy() == FSIndex.DEFAULT_BAG_INDEX) {
        continue;
      }
      if (this.typeSystem.subsumes(index.getType(), type)) {
        if (index.getIndexingStrategy() != FSIndex.SET_INDEX) {
          iteratorList.add(getIndex(label, type).iterator());
          // Done, found non-set index.
          return;
        }
        setIndex = getIndex(label, type);
      }
    }
    // No sorted or bag index found for this type. If there was a set index,
    // return an iterator for it.
    if (setIndex != null) {
      iteratorList.add(setIndex.iterator());
      return;
    }
    // No index for this type was found at all. Since the auto-indexes are created on demand for
    // each type, there may be gaps in the inheritance chain. So keep descending the inheritance
    // tree looking for relevant indexes.
    List subtypes = this.typeSystem.getDirectSubtypes(type);
    for (int i = 0; i < subtypes.size(); i++) {
      getAllIndexedFS((Type) subtypes.get(i), iteratorList);
    }
  }

}
