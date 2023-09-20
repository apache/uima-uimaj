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

import java.util.Collection;
import java.util.List;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.CopyOnWriteObjHashSet;
import org.apache.uima.internal.util.ObjHashSet;
import org.apache.uima.jcas.cas.TOP;

/**
 * Used for UIMA FS Bag Indexes Uses ObjHashSet to hold instances of FeatureStructures
 * 
 * @param <T>
 *          the Java cover class type for this index, passed along to (wrapped) iterators producing
 *          Java cover classes NOTE: V3 doesn't support ALLOW_DUP_ADD_TO_INDEXES
 */
public class FsIndex_bag<T extends FeatureStructure> extends FsIndex_singletype<T> {

  // // package private
  // final static boolean USE_POSITIVE_INT_SET = !FSIndexRepositoryImpl.IS_ALLOW_DUP_ADD_2_INDEXES;

  // The index
  final private ObjHashSet<TOP> index;

  // /**
  // * Copy on write, initially null
  // * Iterator creation initializes (if not null).
  // * Modification to index:
  // * call cow.makeCopy();
  // * set cow = null
  // * do the modification
  // * index clear/flush - set to null;
  // */
  // private WeakReference<CopyOnWriteObjHashSet<TOP>> cow = null;

  FsIndex_bag(CASImpl cas, Type type, int initialSize, int indexType,
          FSIndexComparator comparatorForIndexSpecs) {
    super(cas, type, indexType, cleanUpComparator(comparatorForIndexSpecs, cas));
    index = new ObjHashSet<>(initialSize, TOP.class, TOP._singleton);
  }

  /**
   * Substitutes an empty comparator if one is specified - may not be needed
   * 
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#init(org.apache.uima.cas.admin.FSIndexComparator)
   */
  private static FSIndexComparator cleanUpComparator(FSIndexComparator comp, CASImpl casImpl) {
    // The comparator for a bag index must be empty, except for the type. If
    // it isn't, we create an empty one.
    FSIndexComparator newComp;
    if (comp.getNumberOfKeys() > 0) {
      newComp = new FSIndexComparatorImpl();
      newComp.setType(comp.getType());
    } else {
      newComp = comp;
    }
    return newComp;
  }

  @Override
  public void flush() {
    super.flush();
    index.clear();
  }

  @Override
  public final void insert(T fs) {
    assertFsTypeMatchesIndexType(fs, "insert");
    maybeCopy();
    index.add((TOP) fs);
  }

  // @SuppressWarnings("unchecked") // unused 1/2016
  // public final boolean insert(int fs) {
  // return insert((T) casImpl.getFsFromId_checked(fs));
  // }

  /**
   * Override the super impl which uses comparators. For bag indexes, compare equal only if
   * identical addresses
   */
  @Override
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    return (fs1 == fs2) ? 0 : (fs1._id() < fs2._id()) ? -1 : 1;
  }

  // @formatter:off
  /*
   * // Do binary search on index. 
   * private final int binarySearch(int [] array, int ele, int start, int end) { 
   *   --end;  // Make end a legal value. 
   *   int i; // Current position 
   *   int comp; // Compare value 
   *   while (start <= end) { 
   *     i = (start + end) / 2; 
   *     comp = compare(ele, array[i]); 
   *     if (comp ==  0) { 
   *       return i; 
   *     } 
   *     if (start == end) {
   *       if (comp < 0) {
   *         return (-i)-1;
   *       } else { // comp > 0 
   *         return (-i)-2; // (-(i+1))-1
   *       } 
   *     } 
   *     if (comp < 0) {
   *       end = i-1; 
   *     } else { // comp > 0 
   *       start = i+1;
   *     } 
   *   } //This means that the input span is empty. 
   *   return (-start)-1; 
   * }
   */
  // @formatter:on

  // public FsIterator_bag<T>SIteratorSingleType_ImplBase<T> createFSIterator(int[]
  // detectIllegalIndexUpdates, int typeCode) {
  // return new FsIterator_bag<T>(this, detectIllegalIndexUpdates, typeCode);
  // }

  /**
   * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
   * @param fs
   *          A Feature Structure used a template to match for equality with the FSs in the index.
   * @return <code>true</code> if the index contains such an element.
   */
  @Override
  public boolean contains(FeatureStructure fs) {
    return index.contains(fs);
  }

  boolean ll_contains(int fsAddr) {
    return contains(casImpl.getFsFromId_checked(fsAddr));
  }

  /**
   * This is a silly method for bag indexes in V3, since dupl add to indexes is not allowed.
   * 
   * @param fs
   *          -
   * @return null or the original fs if the fs is in the index
   */
  @Override
  public T find(FeatureStructure fs) {
    final int resultAddr = index.find((TOP) fs);
    if (resultAddr >= 0) {
      return (T) fs;
    }
    // Not found.
    return null;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#size()
   */
  @Override
  public int size() {
    return index.size();
  }

  /**
   * only for backwards compatibility
   * 
   */
  @Override
  public boolean deleteFS(T fs) {
    assertFsTypeMatchesIndexType(fs, "deleteFS");
    maybeCopy();
    return index.remove(fs);
  }

  // @Override
  // public boolean remove(int fsRef) {
  // return deleteFS((T) casImpl.getFsFromId_checked(fsRef));
  // }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void bulkAddTo(List<T> fss) {
    fss.addAll((Collection<? extends T>) index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.FsIndex_singletype#iterator(boolean, boolean) both orderNotNeeded
   * and ignoreType are ignored for bag indexes.
   */
  @Override
  public LowLevelIterator<T> iterator(boolean orderNotNeeded, boolean ignoreType) {
    CopyOnWriteIndexPart<T> cow_index_wrapper = getNonNullCow();
    return casImpl.inPearContext() ? new FsIterator_bag_pear<>(this, type, cow_index_wrapper)
            : new FsIterator_bag<>(this, type, cow_index_wrapper);
  }

  @Override
  protected CopyOnWriteIndexPart<T> createCopyOnWriteIndexPart() {
    if (CASImpl.traceCow) {
      casImpl.traceCowCopy(this);
    }
    return new CopyOnWriteObjHashSet(index);
  }

  @Override
  public int ll_maxAnnotSpan() {
    return Integer.MAX_VALUE;
  }

  @Override
  public LowLevelIterator<T> iterator() {
    return iterator(!IS_ORDERED, !IS_TYPE_ORDER);
  }

  // ObjHashSet<TOP> getObjHashSet() {
  // return index;
  // }

  // private void maybeCopy() {
  // if (cow != null) {
  // CopyOnWriteObjHashSet<TOP> v = cow.get();
  // if (v != null) {
  // v.makeCopy();
  // }
  // cow = null;
  // }
  // }

  // /**
  // * Called when iterator created, and when a reset concur mod happens
  // * @return cow to use in iterator
  // */
  // public CopyOnWriteObjHashSet<TOP> getNonNullCow() {
  // if (cow != null) {
  // CopyOnWriteObjHashSet<TOP> n = cow.get();
  // if (n != null) {
  // return n;
  // }
  // }
  //
  // // null means index updated since iterator was created, need to make new cow and use it
  // CopyOnWriteObjHashSet<TOP> n = new CopyOnWriteObjHashSet<TOP>(index);
  // cow = new WeakReference<>(n);
  // return n;
  // }

}
