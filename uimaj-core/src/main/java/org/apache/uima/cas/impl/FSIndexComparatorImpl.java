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

import java.util.Vector;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.internal.util.IntVector;

public class FSIndexComparatorImpl implements FSIndexComparator {

  private Type type;

  private Vector<Object> keyVector;

  private IntVector compVector;

  private IntVector keyTypeVector;

  private TypeSystem ts;

  private CASImpl cas;

  @SuppressWarnings("unused")
  private FSIndexComparatorImpl() {
    super();
  }

  // Public only for testing purposes.
  public FSIndexComparatorImpl(CASImpl cas) {
    super();
    this.keyVector = new Vector<Object>();
    this.compVector = new IntVector();
    this.keyTypeVector = new IntVector();
    this.type = null;
    this.ts = cas.getTypeSystem();
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
    final int rc = this.keyVector.size();
    this.keyVector.add(feat);
    this.compVector.add(compareKey);
    this.keyTypeVector.add(FEATURE_KEY);
    return rc;
  }

  public int addKey(LinearTypeOrder typeOrder, int compareKey) {
    final int rc = this.keyVector.size();
    this.compVector.add(compareKey);
    this.keyVector.add(typeOrder);
    this.keyTypeVector.add(TYPE_ORDER_KEY);
    return rc;
  }

  public int getKeyType(int key) {
    return this.keyTypeVector.get(key);
  }

  public int getNumberOfKeys() {
    return this.keyVector.size();
  }

  public Feature getKeyFeature(int key) {
    if (this.keyTypeVector.get(key) == FEATURE_KEY) {
      return (Feature) this.keyVector.get(key);
    }
    return null;
  }

  public LinearTypeOrder getKeyTypeOrder(int key) {
    if (this.keyTypeVector.get(key) == TYPE_ORDER_KEY) {
      return (LinearTypeOrder) this.keyVector.get(key);
    }
    return null;
  }

  public int getKeyComparator(int key) {
    return this.compVector.get(key);
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
      if (this.keyTypeVector.get(i) != FEATURE_KEY) {
        continue;
      }
      feat = (Feature) this.keyVector.get(i);
      // if (feat.getTypeSystem() != ts) {
      // return false;
      // }
      if (!this.ts.subsumes(feat.getDomain(), this.type)) {
        return false;
      }
    }
    return true;
  }


  synchronized FSIndexComparatorImpl copy() {
    FSIndexComparatorImpl copy = new FSIndexComparatorImpl(this.cas);
    copy.type = this.type;
    final int max = this.getNumberOfKeys();
    copy.compVector.add(this.compVector.getArray(), 0, this.compVector.size());
    copy.keyTypeVector.add(this.keyTypeVector.getArray(), 0, this.keyTypeVector.size());
    for (int i = 0; i < max; i++) {
      copy.keyVector.add(this.keyVector.get(i));
//      copy.compVector.add(this.compVector.get(i));
//      copy.keyTypeVector.add(this.keyTypeVector.get(i));
    }
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
