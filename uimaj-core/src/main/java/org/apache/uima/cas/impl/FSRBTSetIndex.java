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
import org.apache.uima.internal.util.rb_trees.CompIntArrayRBT;

/**
 * Same as FSRBTIndex, but duplicates are not inserted.
 * 
 * 
 */
class FSRBTSetIndex extends FSLeafIndexImpl {

  private CompIntArrayRBT tree;

  /**
   * Constructor for FSRBTIndex.
   * 
   * @param cas
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
    this.tree = new CompIntArrayRBT(this);
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#insert(int)
   */
  boolean insert(int fs) {
    this.tree.insertKey(fs);
    return true;
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
    return this.tree.pointerIterator();
  }

  ComparableIntIterator refIterator(IntComparator comp) {
    return this.tree.iterator(comp);
  }

  public ComparableIntPointerIterator pointerIterator(IntComparator comp,
          int[] detectIllegalIndexUpdates, int typeCode) {
    return this.tree.pointerIterator(comp, detectIllegalIndexUpdates, typeCode);
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#refIterator(int)
   */
  protected IntPointerIterator refIterator(int fsCode) {
    return this.tree.pointerIterator(fsCode);
  }

  /**
   * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
   */
  public boolean contains(FeatureStructure fs) {
    return this.tree.containsKey(((FeatureStructureImpl) fs).getAddress());
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
    return new LowLevelIteratorWrapper(this.tree.pointerIterator(), this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#remove(int)
   */
  void remove(int fs) {
    this.tree.deleteKey(fs);
  }

}
