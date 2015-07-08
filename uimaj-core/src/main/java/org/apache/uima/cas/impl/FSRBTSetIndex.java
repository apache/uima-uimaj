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

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.ComparableIntIterator;
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.rb_trees.CompIntArrayRBT;

/**
 * Used for UIMA FS Set Indexes 
 * 
 * Uses CompIntArrayRBT red black tree to hold items
 * 
 * Same as FSRBTIndex, but duplicates are not inserted.
 * 
 * @param <T> the Java cover class type for this index, passed along to (wrapped) iterators producing Java cover classes
 *
 */
// internal use only
public class FSRBTSetIndex<T extends FeatureStructure> extends FSLeafIndexImpl<T> {

  CompIntArrayRBT tree;

  /**
   * Constructor for FSRBTIndex.
   * 
   * @param cas -
   * @param type -
   * @param indexType -
   */
  public FSRBTSetIndex(CASImpl cas, Type type, int indexType) {
    super(cas, type, indexType);
    // We can only initialize the tree after we got the comparator.
    this.tree = null;
  }

  boolean init(FSIndexComparator comp) {
    boolean rc = super.init(comp);
    this.tree = new CompIntArrayRBT(this);
    return rc;
  }

  public void flush() {
    this.tree.flush();
//    this.tree = new CompIntArrayRBT(this);  // not this way - iterators are holding on to references to the old tree...
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#insert(int)
   */
  boolean insert(int fs) {
    this.tree.insertKey(fs);
    return true;
  }
  
  boolean insert(int fs, int count) {
    return insert(fs);
  }

  public FeatureStructure find(FeatureStructure fs) {
    LowLevelCAS llc = fs.getCAS().getLowLevelCAS();
    final int addr = llc.ll_getFSRef(fs);
    final int resultAddr = this.tree.getKeyForNode(this.tree.findKey(addr));
    if (resultAddr > 0) {
      return llc.ll_getFSForRef(resultAddr);
    }
    return null;
  }

  public IntPointerIterator refIterator() {
    return this.tree.pointerIterator(this, null, this);
  }

  ComparableIntIterator refIterator(IntComparator comp) {
    return (ComparableIntIterator) this.tree.pointerIterator(this, null, comp);
  }

  public ComparableIntPointerIterator<T> pointerIterator(
      IntComparator comp, int[] detectIllegalIndexUpdates, int typeCode) {
    return this.tree.pointerIterator(this, detectIllegalIndexUpdates, comp);
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#refIterator(int)
   */
  protected IntPointerIterator refIterator(int fsCode) {
    ComparableIntPointerIterator<T> it = this.tree.pointerIterator(this, null, null);
    it.moveTo(fsCode);
    return it;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
   * @param fs feature structure
   * @return true if the set contains the feature structure
   */
  public boolean contains(FeatureStructure fs) {
    return ll_contains(((FeatureStructureImpl) fs).getAddress());
  }
  
  boolean ll_contains(int fsAddr) {
    return this.tree.containsKey(fsAddr);
  }

  /**
   * @see org.apache.uima.cas.FSIndex#size()
   */
  public int size() {
    return this.tree.size();
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#deleteFS(org.apache.uima.cas.FeatureStructure)
   */
  public void deleteFS(FeatureStructure fs) {
    final int addr = ((FeatureStructureImpl) fs).getAddress();
    this.tree.deleteKey(addr);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIndex#ll_iterator()
   */
  public LowLevelIterator ll_iterator() {
    return (LowLevelIterator) this.tree.pointerIterator(this, null, this);
  }

  /*
   * This code is written to remove the exact fs, not just one which matches equal to the argument
   * 
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#remove(int)
   */
  @Override
  boolean remove(int fs) {
    return this.tree.deleteKey(fs);
  }

  @Override
  protected void bulkAddTo(IntVector v) {
    throw new UnsupportedOperationException();
  }
  
  // For testing only
  public void setTree(CompIntArrayRBT compIntArrayRBT) {
    this.tree = compIntArrayRBT;
  }

}
