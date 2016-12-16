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

package org.apache.uima.jcas.cas;

import java.util.Arrays;
import java.util.PrimitiveIterator.OfInt;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.uima.List_of_ints;
import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * An ArrayList type containing ints, for UIMA
 *   - implements a subset of the List API, Iterable&lt;Integer&gt;, IntListIterator.
 *   - it is adjustable, like ArrayList
 *   
 * Implementation notes:
 *   - Uses UimaSerializable APIs
 *   - two implementations of the array list:
 *     -- one uses the original IntegerArray, via a variant of the asList wrapper that returns ints
 *     -- This is used until an add or remove operation that changes the size.
 *       --- switches to IntVector, resetting the original IntegerArray to null
 *       
 *   - This enables operation without creating the Java Object in use cases of deserializing and
 *     referencing when updating is not being used.    
 */
public final class IntegerArrayList extends TOP implements 
                                 Iterable<Integer>,
                                 UimaSerializable, CommonArray, 
                                 RandomAccess, Cloneable {

  /**
   * each cover class when loaded sets an index. used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(IntegerArrayList.class);

  public final static int type = typeIndexID;

  /**
   * used to obtain reference to the _Type instance
   * 
   * @return the type array index
   */
  // can't be factored - refs locally defined field
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * lifecycle
   *   - starts as empty array list
   *   - becomes non-empty when updated (add)
   *       -- used from that point on
   */
  private final IntVector intArrayList;
  
  /**
   * lifecycle
   *   - starts as the empty list
   *   - set when _init_from_cas_data()
   *   - set to null when update (add/remove) happens
   */
  private List_of_ints intArrayAsList = List_of_ints.EMPTY_LIST();
  
  public static final int _FI_intArray = TypeSystemImpl.getAdjustedFeatureOffset("intArray");
  
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private IntegerArrayList() {
    intArrayList = null;
  }

  /**
   * Make a new ArrayList 
   * @param jcas The JCas
   */
  public IntegerArrayList(JCas jcas) {
    super(jcas);
    intArrayList = new IntVector();
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }

  /**
   * Make a new ArrayList with an initial size 
   * @param jcas The JCas
   * @param length initial size
   */
  public IntegerArrayList(JCas jcas, int length) {
    super(jcas);
    _casView.validateArraySize(length);
    intArrayList = new IntVector(length);

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }
  
  /**
   * used by generator
   * Make a new FSArrayList
   * @param c -
   * @param t -
   */
  public IntegerArrayList(TypeImpl t, CASImpl c) {
    super(t, c);  
    
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    intArrayList = new IntVector();
  }

  // *------------------*
  // * Feature: fsArray
  /* getter for fsArray */
  private IntegerArray getIntegerArray() { return (IntegerArray) _getFeatureValueNc(_FI_intArray); }

  /* setter for fsArray */
  private void setIntegerArray(IntegerArray v) {
    _setFeatureValueNcWj(_FI_intArray, v); }

  private void maybeStartUsingIntegerArrayList() {
    if (intArrayAsList != null) {
      intArrayList.removeAllElements();
      intArrayList.copyFromArray(intArrayAsList.toArrayMinCopy(), 0, 0, size()); 
      intArrayAsList = null;  // stop using this one
      setIntegerArray(null);  // clear
    }
  }
  
  @Override
  public void _init_from_cas_data() {
    
  }
  
  @Override
  public void _save_to_cas_data() {
    if (null != intArrayAsList) {
      return;  // nothing to do
    }
    IntegerArray ia = getIntegerArray();
    final int size = intArrayList.size();
    if (ia == null || ia.size() != size) {
      ia = new IntegerArray(_casView.getExistingJCas(), size);
      setIntegerArray(ia);
    }
    ia.copyFromArray(intArrayList.getArray(), 0,  0, size());
    intArrayAsList = List_of_ints.newInstance(getIntegerArray()._getTheArray());
  }
  
  /**
   * @param i -
   * @return the indexed value from the corresponding Cas IntegerArray as a Java Model object.
   */
  public int get(int i) {
    return (null == intArrayAsList)
      ? intArrayList.get(i)
      : intArrayAsList.get(i);
  }

  /**
   * updates the i-th value of the FSArrayList
   * @param i -
   * @param v -
   */
  public void set(int i, int v) {
    if (null == intArrayAsList) {
      intArrayList.set(i, v);
    } else {
      intArrayAsList.set(i, v);
    }
  }
  
  /** return the size of the array. */
  @Override
  public int size() {
    return (null == intArrayAsList) 
        ? intArrayList.size()
        : intArrayAsList.size();
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   * @param src -
   * @param srcPos -
   * @param destPos -
   * @param length -
   */
  public void copyFromArray(int[] src, int srcPos, int destPos, int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 ||
        srcEnd > src.length ||
        destEnd > size()) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("FSArrayList.copyFromArray, srcPos: %,d destPos: %,d length: %,d",  srcPos, destPos, length));
    }
    if (null == intArrayAsList) {
      intArrayList.copyFromArray(src, srcPos, destPos, length);
    } else {
      intArrayAsList.copyFromArray(src, srcPos, destPos, length);
    }      
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   * @param srcPos -
   * @param dest -
   * @param destPos -
   * @param length -
   */
  public void copyToArray(int srcPos, int[] dest, int destPos, int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 ||
        srcEnd > size() ||
        destEnd > dest.length) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("FSArrayList.copyToArray, srcPos: %,d destPos: %,d length: %,d",  srcPos, destPos, length));
    }
    if (null == intArrayAsList) {
      intArrayList.copyToArray(srcPos, dest, destPos, length);
    } else {
      intArrayAsList.copyToArray(srcPos, dest, destPos, length);
    }          
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   * @return -
   */
  public int[] toArray() {
    int[] r = new int[size()];
    copyToArray(0, r, 0, size());
    return r;
  }

  /**
   * Not supported, will throw UnsupportedOperationException
   * @param src -
   * @param srcPos -
   * @param destPos -
   * @param length -
   */
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    throw new UnsupportedOperationException();
  }
    
  /**
   * Copies an array of ints to an Array of Strings.
   * 
   * @param srcPos
   *                The index of the first element to copy.
   * @param dest
   *                The array to copy to.
   * @param destPos
   *                Where to start copying into <code>dest</code>.
   * @param length
   *                The number of elements to copy.
   * @exception ArrayIndexOutOfBoundsException
   *                    If <code>srcPos &lt; 0</code> or
   *                    <code>length &gt; size()</code> or
   *                    <code>destPos + length &gt; destArray.length</code>.
   */
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(size(), srcPos, length);
    for (int i = 0; i < length; i++) {
      dest[i + destPos] = Integer.toBinaryString(get(srcPos + i));
    }
  }

  @Override
  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
  
  /* 
   * 
   * (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArray v) {
    clear();
    Spliterator.OfInt si;
    
    if (v instanceof IntegerArrayList) {
      si = ((IntegerArrayList) v).spliterator();
    } else if (v instanceof IntegerArray) {
      si = ((IntegerArray) v).spliterator();
    } else {
      throw new ClassCastException("argument must be of class IntegerArray or IntegerArrayList");
    }
      
    si.forEachRemaining((int i) -> add(i));      
  }
  
  /**
   * Convenience - create a FSArrayList from an existing FeatureStructure[]
   * @param jcas -
   * @param a -
   * @return -
   */
  public static IntegerArrayList create(JCas jcas, int[] a) {
    IntegerArrayList ial = new IntegerArrayList(jcas, a.length);
    ial.copyFromArray(a, 0, 0, a.length);
    return ial;
  }
  
  @Override
  public FeatureStructureImplC _superClone() {return clone();}  // enable common clone
  
  /**
   * @return -
   * @see java.util.ArrayList#isEmpty()
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * @param i -
   * @return -
   */
  public boolean contains(int i) {
    return indexOf(i) != -1;
  }

  /**
   * @param i -
   * @return -
   * @see java.util.ArrayList#indexOf(java.lang.Object)
   */
  public int indexOf(int i) {
    if (null == intArrayAsList) {
      return intArrayList.indexOf(i);
    }
    return intArrayAsList.indexOf(i);
  }

  /**
   * @param i -
   * @return -
   * @see java.util.ArrayList#lastIndexOf(java.lang.Object)
   */
  public int lastIndexOf(int i) {
    if (null == intArrayAsList) {
      return intArrayList.lastIndexOf(i);
    }
    return intArrayAsList.lastIndexOf(i);
  }

  /**
   * @param a -
   * @return -
   * @see java.util.ArrayList#toArray(java.lang.Object[])
   */
  public int[] toArray(int[] a) {
    return (null == intArrayAsList) 
        ? intArrayList.toArray()
        : intArrayAsList.toArray();
  }

  /**
   * @return -
   * @see java.util.AbstractCollection#toString()
   */
  @Override
  public String toString() {
    return String.format("IntegerArrayList[size: %,d]", size());
  }

  /**
   * @param e -
   * @return true
   * @see java.util.ArrayList#add(java.lang.Object)
   */
  public boolean add(int e) {
    maybeStartUsingIntegerArrayList();
    intArrayList.add(e);
    return true;
  }

  /**
   * @param o -
   * @return  true if all elements are the same, and in same order, and same number
   * @see java.util.AbstractList#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof IntegerArrayList) {
      IntegerArrayList other = (IntegerArrayList) o;
      if (size() == other.size()) {
        for (int i = size() - 1; i >= 0; i--) {
          if (get(i) != other.get(i)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  /**
   * @param index -
   * @param element -
   * @see java.util.ArrayList#add(int, java.lang.Object)
   */
  public void add(int index, int element) {
    maybeStartUsingIntegerArrayList();
    intArrayList.add(index, element);
  }

  /**
   * @param index -
   * @return -
   * @see java.util.ArrayList#remove(int)
   */
  public int removeAt(int index) {
    maybeStartUsingIntegerArrayList();
    return intArrayList.remove(index);
  }

  /**
   * @param o - locate and if found remove this object
   * @return true if removed
   * @see java.util.ArrayList#remove(java.lang.Object)
   */
  public boolean remove(int o) {
    maybeStartUsingIntegerArrayList();
    int pos = intArrayList.indexOf(o);
    if (pos >= 0) {
      intArrayList.remove(pos);
      return true;
    }
    return false;
  }

  /**
   * @return -
   * @see java.util.AbstractList#hashCode()
   */
  @Override
  public int hashCode() {
    int hc = 1;
    final int prime = 31;
    for (int i = size() - 1; i >= 0; i++) {
      hc = hc * prime + i;
      hc = hc * prime + get(i);
    }
    return hc;
  }

  /**
   * 
   * @see java.util.ArrayList#clear()
   */
  public void clear() {
    maybeStartUsingIntegerArrayList();
    intArrayList.removeAllElements();
  }

  /**
   * @return -
   * @see java.util.ArrayList#iterator()
   */
  @Override
  public OfInt iterator() {
    return (null == intArrayAsList) 
        ? intArrayList.iterator()
        : intArrayAsList.iterator();
  }
  
  public IntListIterator intListIterator() {
    return (null == intArrayAsList) 
        ? intArrayList.intListIterator()
        : intArrayAsList.intListIterator();
  }

  public void sort() {
    if (null == intArrayAsList) {
      intArrayList.sort();
    } else {
      intArrayAsList.sort();
    }
  }
  
  public Spliterator.OfInt spliterator() {
    return (null == intArrayAsList) 
        ? Arrays.spliterator(intArrayList.toIntArray())
        : Arrays.spliterator(getIntegerArray()._getTheArray());
  }

  /**
   * @return a stream over the integers
   */
  public IntStream stream() {
    return StreamSupport.intStream(spliterator(), false);
  }
  
}
