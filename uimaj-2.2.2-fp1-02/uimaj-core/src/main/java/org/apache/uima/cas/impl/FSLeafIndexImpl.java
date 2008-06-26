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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntPointerIterator;

/**
 * Class comment for FSLeafIndexImpl.java goes here.
 * 
 * 
 */
public abstract class FSLeafIndexImpl implements IntComparator, FSIndex, FSIndexImpl {

  private final int indexType;

  // A reference to the low-level CAS.
  protected CASImpl lowLevelCAS;

  private static final int STRING_CODE = 0;

  private static final int FLOAT_CODE = 1;

  private static final int INT_CODE = 2;

  private static final int TYPE_ORDER_CODE = 3;

  private static final int BOOLEAN_CODE = 4;

  private static final int BYTE_CODE = 5;

  private static final int SHORT_CODE = 6;

  private static final int LONG_CODE = 7;

  private static final int DOUBLE_CODE = 8;

  private FSIndexComparatorImpl comparator;

  private boolean isInitialized = false;

  // For each key, the int code of the type of that key.
  private int[] keyType;

  // For each feature key, the feature offset.
  private int[] keyOffset;

  // An array of the type orders used. This array is dense, most values will
  // be null.
  private LinearTypeOrder[] typeOrder;

  // For each key, the comparison to use.
  private int[] keyComp;

  // The number of keys.
  private int numKeys;

  private Type type; // The type of this

  // never called
  // declared private to block external calls
  private FSLeafIndexImpl() {
    super();
    this.indexType = 0; // must do because it's final
  }

  /**
   * Constructor for FSLeafIndexImpl.
   */
  protected FSLeafIndexImpl(CASImpl cas, Type type, int indexType) {
    super();
    this.indexType = indexType;
    this.lowLevelCAS = cas;
    this.type = type;
  }

  abstract boolean insert(int fs);

  abstract void remove(int fs);

  // public abstract IntListIterator iterator();

  // public abstract ComparableIntIterator iterator(IntComparator comp);

  public abstract ComparableIntPointerIterator pointerIterator(IntComparator comp,
          int[] detectIllegalIndexUpdates, int typeCode);

  public FSIndexComparator getComparator() {
    return this.comparator;
  }

  IntComparator getIntComparator() {
    return this;
  }

  public int getIndexingStrategy() {
    return this.indexType;
  }

  boolean init(FSIndexComparator comp) {
    if (this.isInitialized) {
      return false;
    }
    FSIndexComparatorImpl comp1 = (FSIndexComparatorImpl) comp;
    this.comparator = comp1.copy();
    if (!this.comparator.isValid()) {
      return false;
    }
    final int nKeys = this.comparator.getNumberOfKeys();
    // Initialize the comparator info.
    this.keyType = new int[nKeys];
    this.keyOffset = new int[nKeys];
    this.keyComp = new int[nKeys];
    this.typeOrder = new LinearTypeOrder[nKeys];
    Feature keyFeature;
    for (int i = 0; i < nKeys; i++) {
      switch (comp.getKeyType(i)) {
        case FSIndexComparator.FEATURE_KEY: {
          keyFeature = this.comparator.getKeyFeature(i);
          this.keyType[i] = getKeyCode(keyFeature);
          this.keyOffset[i] = getFeatureOffset(keyFeature);
          this.keyComp[i] = this.comparator.getKeyComparator(i);
          break;
        }
        case FSIndexComparator.TYPE_ORDER_KEY: {
          this.keyType[i] = TYPE_ORDER_CODE;
          this.keyComp[i] = this.comparator.getKeyComparator(i);
          this.typeOrder[i] = this.comparator.getKeyTypeOrder(i);
          this.keyOffset[i] = 0;
          break;
        }
        default: {
          // This is an internal error.
          throw new RuntimeException("Assertion failed.");
        }
      }
    }
    this.numKeys = nKeys;
    this.isInitialized = true;
    return true;
  }

  private static final int getKeyCode(Feature feat) {
    String typeName = feat.getRange().getName();
    if (typeName.equals(CAS.TYPE_NAME_STRING)) {
      return STRING_CODE;
    }
    if (typeName.equals(CAS.TYPE_NAME_FLOAT)) {
      return FLOAT_CODE;
    }
    if (typeName.equals(CAS.TYPE_NAME_BOOLEAN)) {
      return BOOLEAN_CODE;
    }
    if (typeName.equals(CAS.TYPE_NAME_BYTE)) {
      return BYTE_CODE;
    }
    if (typeName.equals(CAS.TYPE_NAME_SHORT)) {
      return SHORT_CODE;
    }
    if (typeName.equals(CAS.TYPE_NAME_LONG)) {
      return LONG_CODE;
    }
    if (typeName.equals(CAS.TYPE_NAME_DOUBLE)) {
      return DOUBLE_CODE;
    }
    // This is defaulty and not nice. We rely on the checking in
    // FSIndexComparatorImpl to make sure that only correct types get
    // through.
    return INT_CODE;
  }

  private final int getFeatureOffset(Feature feat) {
    return this.comparator.getLowLevelCAS().getFeatureOffset(((FeatureImpl) feat).getCode());
  }

  public int ll_compare(int ref1, int ref2) {
    return this.compare(ref1, ref2);
  }

  public int compare(int fs1, int fs2) {
    int[] heap = this.lowLevelCAS.getHeap().heap;
    int val1, val2;
    int compVal;
    float float1, float2;
    String string1, string2;
    for (int i = 0; i < this.numKeys; i++) {
      val1 = heap[fs1 + this.keyOffset[i]];
      val2 = heap[fs2 + this.keyOffset[i]];
      switch (this.keyType[i]) {
        case STRING_CODE: {
          // System.out.println("Comparing string codes " + val1 + " and "
          // + val2);
          // System.out.println(
          // "Strings: "
          // + lowLevelCAS.getStringForCode(val1)
          // + ", "
          // + lowLevelCAS.getStringForCode(val2));
          // if (lowLevelCAS.getStringForCode(val1) == null) {
          // System.out.println("Value for " + val1 + " is <null>.");
          // }
          // if (lowLevelCAS.getStringForCode(val2) == null) {
          // System.out.println("Value for " + val2 + " is <null>.");
          // }
          string1 = this.lowLevelCAS.getStringForCode(val1);
          string2 = this.lowLevelCAS.getStringForCode(val2);
          if (string1 == null) {
            if (string2 == null) {
              compVal = 0;
            } else {
              compVal = -1;
            }
          } else {
            if (string2 == null) {
              compVal = 1;
            } else {
              compVal = string1.compareTo(string2);
            }
          }

          if (compVal != 0) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return compVal;
            }
            return -compVal;
          }
          break;
        }
        case FLOAT_CODE: {
          float1 = CASImpl.int2float(val1);
          float2 = CASImpl.int2float(val2);
          if (float1 < float2) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          } else if (float1 > float2) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return 1;
            }
            return -1;
          }
          break;
        }
        case TYPE_ORDER_CODE: {
          if (val1 == val2) {
            break;
          }
          if (this.typeOrder[i].lessThan(val1, val2)) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          }
          if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
            return 1;
          }
          return -1;
        }
        case LONG_CODE: {
          long long1 = this.lowLevelCAS.getLongHeap().getHeapValue(val1);
          long long2 = this.lowLevelCAS.getLongHeap().getHeapValue(val2);
          if (long1 < long2) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          } else if (long1 > long2) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return 1;
            }
            return -1;
          }
          break;
        }
        case DOUBLE_CODE: {
          double double1 = Double.longBitsToDouble(this.lowLevelCAS.getLongHeap().getHeapValue(val1));
          double double2 = Double.longBitsToDouble(this.lowLevelCAS.getLongHeap().getHeapValue(val2));
          if (double1 < double2) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          } else if (double1 > double2) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return 1;
            }
            return -1;
          }
          break;
        }

        default: { // Compare the int values directly.
          // boolean compare done here as well.
          // byte compare done here as well.
          // short compare done here as well.
          if (val1 < val2) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          } else if (val1 > val2) {
            if (this.keyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return 1;
            }
            return -1;

          }
          break;
        }
      }
    }
    // FSs are identical as far as this comparator goes.
    return 0;
  }

  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return this.comparator.equals(o);
    // if (o instanceof FSIndexComparator) {
    // return this.comparator.equals(o);
    // } else if (o instanceof FSVectorIndex) {
    // return this.comparator.equals(((FSLeafIndexImpl)o).comparator);
    // } else {
    // return false;
    // }
  }

  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see org.apache.uima.cas.FSIndex#compare(FeatureStructure, FeatureStructure)
   */
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    return compare(((FeatureStructureImpl) fs1).getAddress(), ((FeatureStructureImpl) fs2)
            .getAddress());
  }

  /**
   * @see org.apache.uima.cas.FSIndex#getType()
   */
  public Type getType() {
    return this.type;
  }

  protected abstract IntPointerIterator refIterator();

  public IntPointerIterator getIntIterator() {
    return refIterator();
  }

  protected abstract IntPointerIterator refIterator(int fsCode);

  // these next two are never called (maybe)
  // because the object this method is called on is
  // never this object, but instead the FSIndexRepositoryImpl.IndexImpl object
  //
  // It would be good to refactor this so that this confusion is eliminated,
  // perhaps by not having this class implement FSIndex

  /**
   * @see org.apache.uima.cas.FSIndex#iterator()
   */
  public FSIterator iterator() {
    System.out.println(this.getClass().getName());
    return new FSIteratorWrapper(refIterator(), this.lowLevelCAS);
  }

  /**
   * @see org.apache.uima.cas.FSIndex#iterator(FeatureStructure)
   */
  public FSIterator iterator(FeatureStructure fs) {
    return new FSIteratorWrapper(refIterator(((FeatureStructureImpl) fs).getAddress()),
            this.lowLevelCAS);
  }

  /**
   * Method deleteFS.
   * 
   * @param fs
   */
  public abstract void deleteFS(FeatureStructure fs);

  public LowLevelIterator ll_iterator(boolean ambiguous) {
    if (ambiguous) {
      return this.ll_iterator();
    }

    return null;
  }

}
