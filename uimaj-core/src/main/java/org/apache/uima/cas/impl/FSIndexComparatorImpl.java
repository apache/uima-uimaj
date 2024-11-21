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
import org.apache.uima.internal.util.IntVector;

// @formatter:off
/**
 * Specifies the comparison to be used for an index, in terms of 
 * - the keys and the typeorder, in an order 
 * - the standard/reverse ordering
 */
// @formatter:on
public class FSIndexComparatorImpl implements FSIndexComparator {

  private Type type;

  // the Feature or Linear Type Order, as an ordered collection, one per key
  private final List<Object> keySpecs; // Feature or LinearTypeOrder

  // Standard or Reverse
  private final IntVector directions;

  // // FEATURE_KEY or TYPE_ORDER_KEY
  // private IntVector keyTypeVector;

  // Public only for testing purposes.
  public FSIndexComparatorImpl() {
    type = null;
    keySpecs = new ArrayList<>();
    directions = new IntVector();
  }

  private FSIndexComparatorImpl(Type type, List<Object> keySpecs, IntVector directions) {
    this.type = type;
    this.keySpecs = keySpecs;
    this.directions = directions;
  }

  private boolean checkType(Type t) {
    return t.isPrimitive();
  }

  @Override
  public void setType(Type type) {
    this.type = type;
  }

  @Override
  public Type getType() {
    return type;
  }

  int getTypeCode() {
    return ((TypeImpl) type).getCode();
  }

  @Override
  public int addKey(Feature feat, int compareKey) {
    if (!checkType(feat.getRange())) {
      return -1;
    }
    final int rc = keySpecs.size();
    keySpecs.add(feat);
    directions.add(compareKey);
    return rc;
  }

  @Override
  public int addKey(LinearTypeOrder typeOrder, int compareKey) {
    final int rc = keySpecs.size();
    keySpecs.add(typeOrder);
    directions.add(compareKey);
    return rc;
  }

  @Override
  public int getKeyType(int key) {
    return (keySpecs.get(key) instanceof Feature) ? FEATURE_KEY : TYPE_ORDER_KEY;
  }

  @Override
  public int getNumberOfKeys() {
    return keySpecs.size();
  }

  @Override
  public FeatureImpl getKeyFeature(int key) {
    if (getKeyType(key) == FEATURE_KEY) {
      return (FeatureImpl) keySpecs.get(key);
    }
    return null;
  }

  public LinearTypeOrder getKeyTypeOrder(int key) {
    if (getKeyType(key) == TYPE_ORDER_KEY) {
      return (LinearTypeOrder) keySpecs.get(key);
    }
    return null;
  }

  @Override
  public int getKeyComparator(int key) {
    return directions.get(key);
  }

  /**
   * Equals including the type of the comparator
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FSIndexComparatorImpl)) {
      return false;
    }
    FSIndexComparatorImpl comp = (FSIndexComparatorImpl) o;
    if (type != comp.type) {
      return false;
    }
    return equalsWithoutType(comp);
  }

  /**
   * Compare two comparators, ignoring the type
   * 
   * @param comp
   *          the other comparator to compare to
   * @return true if they're the same comparator
   */
  boolean equalsWithoutType(FSIndexComparatorImpl comp) {
    final int max = getNumberOfKeys();
    if (max != comp.getNumberOfKeys()) {
      return false;
    }
    for (int i = 0; i < max; i++) {
      Object keySpec1 = keySpecs.get(i);
      Object keySpec2 = comp.keySpecs.get(i);
      if (keySpec1 instanceof LinearTypeOrder) {
        // equals compares the type codes in the ordered arrays for ==
        if (!(((LinearTypeOrder) keySpec1).equals(keySpec2))) {
          return false;
        }
      } else {
        FeatureImpl f1 = (FeatureImpl) keySpec1;
        FeatureImpl f2 = (FeatureImpl) keySpec2;
        boolean featimpl_match = f1.equals(f2) // this compares
                                               // shortName,
                                               // multiplerefs allowed
                                               // highest defining type
                                               // range type name
                // also need to confirm offsets are the same
                && f1.getOffset() == f2.getOffset()
                && f1.getAdjustedOffset() == f2.getAdjustedOffset()
                && directions.get(i) == comp.directions.get(i);

        if (!featimpl_match) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 31 : type.hashCode());
    final int max = getNumberOfKeys();
    for (int i = 0; i < max; i++) {
      Object o = keySpecs.get(i); // LinearTypeOrder or feature
      if (o instanceof LinearTypeOrder) {
        result = prime * result + ((LinearTypeOrderBuilderImpl.TotalTypeOrder) o).hashCode();
      } else {
        FeatureImpl f = (FeatureImpl) o;
        result = prime * result + f.hashCode(); // only shortName,
                                                // multiplerefs allowed
                                                // highest defining type
                                                // range type name
        result = prime * result + f.getOffset();
        result = prime * result + f.getAdjustedOffset();
        result = prime * result + directions.get(i);
      }
    }
    return result;
  }

  @Override
  public boolean isValid() {
    if (type == null) {
      return false;
    }
    final int max = getNumberOfKeys();
    Feature feat;
    for (int i = 0; i < max; i++) {
      if (getKeyType(i) != FEATURE_KEY) {
        continue;
      }
      feat = (Feature) keySpecs.get(i);
      // if (feat.getTypeSystem() != ts) {
      // return false;
      // }
      if (!((TypeImpl) feat.getDomain()).subsumes((TypeImpl) type)) {
        return false;
      }
    }
    return true;
  }

  public synchronized FSIndexComparatorImpl copy() {
    return new FSIndexComparatorImpl(type, keySpecs, directions);
  }

  /**
   * Compares two FSIndexComparator instances.
   * 
   * The code to compare two FSs is in the compare method of FsIndex_singletype.
   * 
   * @see java.lang.Comparable#compareTo(Object)
   */
  @Override
  public int compareTo(FSIndexComparator o) {
    FSIndexComparator comp = o;
    final int thisSize = getNumberOfKeys();
    final int compSize = comp.getNumberOfKeys();
    int i = 0;
    int feat1, feat2;
    while ((i < thisSize) && (i < compSize)) {
      feat1 = getKeyFeature(i).getCode();
      feat2 = ((FeatureImpl) comp.getKeyFeature(i)).getCode();
      if (feat1 < feat2) {
        return -1;
      } else if (feat1 > feat2) {
        return 1;
      } else {
        if (getKeyComparator(i) < comp.getKeyComparator(i)) {
          return -1;
        } else if (getKeyComparator(i) > comp.getKeyComparator(i)) {
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
