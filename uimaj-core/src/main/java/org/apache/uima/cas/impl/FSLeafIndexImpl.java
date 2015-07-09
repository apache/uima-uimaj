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

import java.util.Comparator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.internal.util.IntVector;

/**
 * The common (among all index kinds - set, sorted, bag) info for an index
 * Subtypes define the actual index repository (integers indexing the CAS) for each kind.
 * 
 * @param <T> the Java cover class type for this index, passed along to (wrapped) iterators producing Java cover classes
 */
public abstract class FSLeafIndexImpl<T extends FeatureStructure> implements Comparator<T>, IntComparator, FSIndexImpl {

  private final int indexType;  // Sorted, Set, Bag, Default-bag, etc.

  // A reference to the low-level CAS.
  final protected CASImpl lowLevelCAS;

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

  final private Type type; // The type of this
  
  final private int typeCode;

  
  
  @Override
  public String toString() {
    String kind;
    switch (indexType) {
    case 0:
      kind = "Sorted";
      break;
    case 1:
      kind = "Set";
      break;
    case 2:
      kind = "Bag";
      break;
    case 3:
      kind = "Default Bag";
      break;
    default:
      kind = "Invalid";
    }
    return "FSLeafIndexImpl ["  
//        + "indexType=" + indexType + ", comparator=" + comparator + ", keyType="
//        + Arrays.toString(keyType) + ", keyOffset=" + Arrays.toString(keyOffset) + ", typeOrder="
//        + Arrays.toString(typeOrder) + ", keyComp=" + Arrays.toString(keyComp) + ", numKeys=" + numKeys 
        + "type=" + type + ", kind=" + kind + "]";
  }

  // never called
  // declared private to block external calls
  @SuppressWarnings("unused")
  private FSLeafIndexImpl() {
    this.indexType = 0; // must do because it's final
    this.lowLevelCAS = null;
    this.type = null;
    this.typeCode = 0;
  }

  /**
   * Constructor for FSLeafIndexImpl.
   * @param cas -
   * @param type -
   * @param indexType -
   */
  protected FSLeafIndexImpl(CASImpl cas, Type type, int indexType) {
    super();
    this.indexType = indexType;
    this.lowLevelCAS = cas;
    this.type = type;
    this.typeCode = ((TypeImpl)type).getCode();
  }

  abstract boolean insert(int fs);
  
  abstract boolean insert(int fs, int count);  // for bulk addback

  /**
   * @param fs - the Feature Structure to be removed.
   * Only this exact Feature Structure is removed (this is a stronger test than, for example,
   * what moveTo(fs) does, where the fs in that case is used as a template).  
   * It is not an error if this exact Feature Structure is not in an index.
   * @return true if something was removed, false if not found
   */
  abstract boolean remove(int fs);

  // public abstract IntListIterator iterator();

  // public abstract ComparableIntIterator iterator(IntComparator comp);

  public abstract ComparableIntPointerIterator<T> pointerIterator(IntComparator comp,
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

  /**
   * Note: may return other than -1 , 0, and 1  (e.g., might return -6)
   * @param fs1 -
   * @param fs2 -
   * @return 0 if equal, &lt; 0 if fs1 &lt; fs2, &gt; 0 if fs1 &gt; fs2
   */
  public int ll_compare(int fs1, int fs2) {
    return this.compare(fs1, fs2);
  }

  /**
   * Note: may return other than -1 , 0, and 1  (e.g., might return -6)
   * @param fs1 -
   * @param fs2 -
   * @return 0 if equal, &lt; 0 if fs1 &lt; fs2, &gt; 0 if fs1 &gt; fs2
   */
  public int compare(int fs1, int fs2) {
    final int[] heap = this.lowLevelCAS.getHeap().heap;
    final int[] localKeyType = this.keyType;
    final int[] localKeyOffset = this.keyOffset;
    final int[] localKeyComp = this.keyComp;
    final int localNumKeys = this.numKeys;
    for (int i = 0; i < localNumKeys; i++) {
      final int val1 = heap[fs1 + localKeyOffset[i]];
      final int val2 = heap[fs2 + localKeyOffset[i]];
      switch (localKeyType[i]) {
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
          final String string1 = this.lowLevelCAS.getStringForCode(val1);
          final String string2 = this.lowLevelCAS.getStringForCode(val2);
          int compVal;
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
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return compVal;
            }
            return -compVal;
          }
          break;
        }
        case FLOAT_CODE: {
          final float float1 = CASImpl.int2float(val1);
          final float float2 = CASImpl.int2float(val2);
          if (float1 < float2) {
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          } else if (float1 > float2) {
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
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
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          }
          if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
            return 1;
          }
          return -1;
        }
        case LONG_CODE: {
          final long long1 = this.lowLevelCAS.getLongHeap().getHeapValue(val1);
          final long long2 = this.lowLevelCAS.getLongHeap().getHeapValue(val2);
          if (long1 < long2) {
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          } else if (long1 > long2) {
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return 1;
            }
            return -1;
          }
          break;
        }
        case DOUBLE_CODE: {
          final double double1 = Double.longBitsToDouble(this.lowLevelCAS.getLongHeap().getHeapValue(val1));
          final double double2 = Double.longBitsToDouble(this.lowLevelCAS.getLongHeap().getHeapValue(val2));
          if (double1 < double2) {
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          } else if (double1 > double2) {
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
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
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
              return -1;
            }
            return 1;
          } else if (val1 > val2) {
            if (localKeyComp[i] == FSIndexComparator.STANDARD_COMPARE) {
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

  // Eclipse says this method is never called by uimaj-core methods 9-2009
//  public final boolean equals(Object o) {
//    if (this == o) {
//      return true;
//    }
//    return this.comparator.equals(o);
//    // if (o instanceof FSIndexComparator) {
//    // return this.comparator.equals(o);
//    // } else if (o instanceof FSVectorIndex) {
//    // return this.comparator.equals(((FSLeafIndexImpl)o).comparator);
//    // } else {
//    // return false;
//    // }
//  }
  
  

//  public int hashCode() {
//    // if this throws exception, then Eclipse debugger fails to show data in inspector
//    // throw new UnsupportedOperationException(); 
//    return 0;
//  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((comparator == null) ? 0 : comparator.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    FSLeafIndexImpl<?> other = (FSLeafIndexImpl<?>) obj;
    if (comparator == null) {
      if (other.comparator != null)
        return false;
    } else if (!comparator.equals(other.comparator))
      return false;
    return true;
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
   * @return The type of feature structures in this index.
   */
  public Type getType() {
    return this.type;
  }
  
  int getTypeCode() {
    return this.typeCode;
  }

  protected abstract IntPointerIterator refIterator();

  public IntPointerIterator getIntIterator() {
    return refIterator();
  }

  /**
   * For serialization: get all the items in this index and bulk add to an IntVector
   * @param v the set of items to add
   */
  protected abstract void bulkAddTo(IntVector v);
  
  protected abstract IntPointerIterator refIterator(int fsCode);

  // these next two are never called (maybe)
  // because the object this method is called on is
  // never this object, but instead the FSIndexRepositoryImpl.IndexImpl object
  //
  // It would be good to refactor this so that this confusion is eliminated,
  // perhaps by not having this class implement FSIndex

//  /**
//   * @see org.apache.uima.cas.FSIndex#iterator()
//   */
//  public FSIterator<T> iterator() {
//    return new FSIteratorWrapper<T>(refIterator(), this.lowLevelCAS);
//  }

  /**
   * @see org.apache.uima.cas.FSIndex#iterator(FeatureStructure)
   * 
   * This has no callers, and is probably not used.
   * The iterator it produces is only over one leaf index and
   * doesn't include Concurrent Modification Exception testing
   * @param fs -
   * @return -
   */
  public FSIterator<T> iterator(FeatureStructure fs) {
    return new FSIteratorWrapper<T>(refIterator(((FeatureStructureImpl) fs).getAddress()),
            this.lowLevelCAS);
  }

  /**
   * Method deleteFS.
   * 
   * @param fs -
   */
  public abstract void deleteFS(FeatureStructure fs);

  public LowLevelIterator ll_iterator(boolean ambiguous) {
    if (ambiguous) {
      return this.ll_iterator();
    }

    return null;
  }

  public LowLevelIterator ll_rootIterator() {
      return this.ll_iterator();
  }

//  @Override
//  public FSIndex<T> withSnapshotIterators() {
//    // should never be called
//    // this is an artifact of the fact that FSLeafIndexImpl implements FSIndex interface
//    //   which seems incorrect?
//    throw new UnsupportedOperationException();
//  }

}
