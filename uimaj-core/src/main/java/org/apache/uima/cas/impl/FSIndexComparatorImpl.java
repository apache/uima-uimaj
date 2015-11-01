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
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.LinearTypeOrder;

/**
 * Specifies the comparison to be used for an index, in terms of
 *   - the keys and the typeorder, in an order
 *   - the standard/reverse ordering
 *
 *   
 */
public class FSIndexComparatorImpl implements FSIndexComparator {

  private Type type;

  // the Feature or Linear Type Order, as an ordered collection, one per key
  private final List<Object> keySpecs = new ArrayList<Object>();  // Feature or LinearTypeOrder

  // Standard or Reverse
  private final List<Integer> directions = new ArrayList<Integer>();

//  // FEATURE_KEY or TYPE_ORDER_KEY
//  private IntVector keyTypeVector;

  private final CASImpl cas;

  @SuppressWarnings("unused")
  private FSIndexComparatorImpl() {
    this.cas = null;
  }

  // Public only for testing purposes.
  public FSIndexComparatorImpl(CASImpl cas) {
    this.type = null;
    this.cas = cas;
  }

  private boolean checkType(Type t) {
    return t.isPrimitive();
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Type getType() {
    return this.type;
  }
  
  int getTypeCode() {
    return ((TypeImpl)this.type).getCode();
  }

  public int addKey(Feature feat, int compareKey) {
    if (!checkType(feat.getRange())) {
      return -1;
    }
    final int rc = this.keySpecs.size();
    this.keySpecs.add(feat);
    this.directions.add(compareKey);
    return rc;
  }

  public int addKey(LinearTypeOrder typeOrder, int compareKey) {
    final int rc = this.keySpecs.size();
    this.keySpecs.add(typeOrder);
    this.directions.add(compareKey);
    return rc;
  }

  public int getKeyType(int key) {
    return (this.keySpecs.get(key) instanceof Feature) 
        ? FEATURE_KEY 
        : TYPE_ORDER_KEY;
  }

  public int getNumberOfKeys() {
    return this.keySpecs.size();
  }

  public FeatureImpl getKeyFeature(int key) {
    if (getKeyType(key) == FEATURE_KEY) {
      return (FeatureImpl) this.keySpecs.get(key);
    }
    return null;
  }

  public LinearTypeOrder getKeyTypeOrder(int key) {
    if (getKeyType(key)  == TYPE_ORDER_KEY) {
      return (LinearTypeOrder) this.keySpecs.get(key);
    }
    return null;
  }

  public int getKeyComparator(int key) {
    return this.directions.get(key);
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FSIndexComparator)) {
      return false;
    }
    FSIndexComparator comp = (FSIndexComparator) o;
    final int max = this.getNumberOfKeys();
    if (max != comp.getNumberOfKeys()) {
      return false;
    }
    for (int i = 0; i < max; i++) {
      if ((this.getKeyFeature(i) != comp.getKeyFeature(i)) || 
          (this.getKeyComparator(i) != comp.getKeyComparator(i))) {
        return false;
      }
    }
    return true;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    final int max = this.getNumberOfKeys();
    for (int i = 0; i < max; i++) {
      Feature f = this.getKeyFeature(i);
      result = prime * result + ((f == null) ? 31 : f.hashCode());
      result = prime * result + this.getKeyComparator(i);
    }
    return result;
  }
  
  CASImpl getLowLevelCAS() {
    return this.cas;
  }

  public boolean isValid() {
    if (this.type == null) {
      return false;
    }
    final int max = this.getNumberOfKeys();
    Feature feat;
    for (int i = 0; i < max; i++) {
      if (getKeyType(i) != FEATURE_KEY) {
        continue;
      }
      feat = (Feature) this.keySpecs.get(i);
      // if (feat.getTypeSystem() != ts) {
      // return false;
      // }
      if (!((TypeImpl) feat.getDomain()).subsumes((TypeImpl) this.type)) {
        return false;
      }
    }
    return true;
  }


  synchronized FSIndexComparatorImpl copy() {
    FSIndexComparatorImpl copy = new FSIndexComparatorImpl(this.cas);
    copy.type = this.type;
    copy.directions.addAll(this.directions);
    copy.keySpecs.addAll(this.keySpecs);
    return copy;
  }

  /**
   * Compares two FSIndexComparator instances.
   * 
   * The code to compare two FSs is in the compare method of FSLeafIndexImpl.
   * 
   * @see java.lang.Comparable#compareTo(Object)
   */
  public int compareTo(FSIndexComparator o) {
    FSIndexComparator comp = o;
    final int thisSize = this.getNumberOfKeys();
    final int compSize = comp.getNumberOfKeys();
    int i = 0;
    int feat1, feat2;
    while ((i < thisSize) && (i < compSize)) {
      feat1 = ((FeatureImpl) this.getKeyFeature(i)).getCode();
      feat2 = ((FeatureImpl) comp.getKeyFeature(i)).getCode();
      if (feat1 < feat2) {
        return -1;
      } else if (feat1 > feat2) {
        return 1;
      } else {
        if (this.getKeyComparator(i) < comp.getKeyComparator(i)) {
          return -1;
        } else if (this.getKeyComparator(i) > comp.getKeyComparator(i)) {
          return 1;
        }
      }
    }
    // If the comparators are not the same size, the shorter one is smaller.
    if (i < thisSize) {
      return 1;
    } else if (i < compSize) {
      return -1;
    }
    // They're equal.
    return 0;
  }


}
