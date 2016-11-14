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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.apache.uima.UimaSerializableFSs;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.util.impl.Constants;

/**
 * An ArrayList type containing Feature Structures, for UIMA
 *   - Has all the methods of List
 *   - Implements the select(...) APIs 
 *   
 * Implementation notes:
 *   - Uses UimaSerializable APIs
 *   - two implementations of the array list:
 *     -- one uses the original FSArray, via an asList wrapper
 *     -- This is used until an add or remove operation;
 *       --- switches to ArrayList, resetting the original FSArray to null
 *       
 *   - This enables operation without creating the Java Object in use cases of deserializing and
 *     referencing when updating is not being used.  
 */
public final class FSArrayList <T extends TOP> extends TOP implements 
                                 UimaSerializableFSs, CommonArray, CommonArrayFS, SelectViaCopyToArray, 
                                 List<T>, RandomAccess, Cloneable {

  private final static List<? extends TOP> EMPTY_LIST = (List<? extends TOP>) Arrays.asList(Constants.EMPTY_TOP_ARRAY);
  /**
   * each cover class when loaded sets an index. used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(FSArrayList.class);

  public final static int type = typeIndexID;

  /**
   * used to obtain reference to the _Type instance
   * 
   * @return the type array index
   */
  // can't be factored - refs locally defined field
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * lifecycle
   *   - starts as empty array list
   *   - becomes non-empty when updated (add)
   *       -- used from that point on
   */
  private final List<T> fsArrayList;
  
  /**
   * lifecycle
   *   - starts as the empty list
   *   - set when _init_from_cas_data()
   *   - set to null when update (add/remove) happens
   */
  private List<T> fsArrayAsList = (List<T>) EMPTY_LIST;
  
  public static final int _FI_fsArray = TypeSystemImpl.getAdjustedFeatureOffset("fsArray");
  
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private FSArrayList() {
    fsArrayList = null;
  }

  /**
   * Make a new ArrayList 
   * @param jcas The JCas
   */
  public FSArrayList(JCas jcas) {
    super(jcas);
    fsArrayList = new ArrayList<>();

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }

  /**
   * Make a new ArrayList with an initial size 
   * @param jcas The JCas
   * @param length initial size
   */
  public FSArrayList(JCas jcas, int length) {
    super(jcas);
    _casView.validateArraySize(length);
    fsArrayList = new ArrayList<>(length);

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
  public FSArrayList(TypeImpl t, CASImpl c) {
    super(t, c);  
    
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    fsArrayList = new ArrayList<>();
  }

  // *------------------*
  // * Feature: fsArray
  /* getter for fsArray */
  private FSArray getFsArray() { return (FSArray) _getFeatureValueNc(_FI_fsArray); }

  /* setter for fsArray */
  private void setFsArray(FSArray v) {
    _setFeatureValueNcWj(_FI_fsArray, v); }

  private void maybeStartUsingArrayList() {
    if (fsArrayAsList != null) {
      fsArrayList.clear();
      fsArrayList.addAll(fsArrayAsList);
      fsArrayAsList = null;  // stop using this one
      setFsArray(null);      // clear
    }
  }
  
  public void _init_from_cas_data() {
    fsArrayAsList = (List<T>) Arrays.asList(getFsArray());
  }
  
  public void _save_to_cas_data() {
    if (getFsArray() == null) {
      FSArray a = new FSArray(_casView.getExistingJCas(), fsArrayList.size());
      setFsArray(a);
      fsArrayList.toArray(a._getTheArray());
    }
  }
  
  private List<T> gl () {
    return (null == fsArrayAsList) 
      ? fsArrayList
      : fsArrayAsList;
  }
  
  /** return the indexed value from the corresponding Cas FSArray as a Java Model object. */
  public T get(int i) {
    return (T) _maybeGetPearFs(gl().get(i));
  }

  /**
   * updates the i-th value of the FSArrayList
   */
  public T set(int i, T v) {
    
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView, _casView);
    }
    return gl().set(i, _maybeGetBaseForPearFs(v));
  }
  
  /** return the size of the array. */
  public int size() {
    return gl().size();
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   * @param src -
   * @param srcPos -
   * @param destPos -
   * @param length -
   */
  public void copyFromArray(T[] src, int srcPos, int destPos, int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 ||
        srcEnd > src.length ||
        destEnd > size()) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("FSArrayList.copyFromArray, srcPos: %,d destPos: %,d length: %,d",  srcPos, destPos, length));
    }
    for (;srcPos < srcEnd && destPos < destEnd;) {
      set(destPos++, src[srcPos++]);
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   * @param srcPos -
   * @param dest -
   * @param destPos -
   * @param length -
   */
  public void copyToArray(int srcPos, FeatureStructure[] dest, int destPos, int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 ||
        srcEnd > size() ||
        destEnd > dest.length) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("FSArrayList.copyToArray, srcPos: %,d destPos: %,d length: %,d",  srcPos, destPos, length));
    }
    for (;srcPos < srcEnd && destPos < destEnd;) {
      dest[destPos++] = _maybeGetPearFs(get(srcPos++));
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public FeatureStructure[] toArray() {
    FeatureStructure[] r = new FeatureStructure[size()];
    copyToArray(0, r, 0, size());
    return r;
  }

  public FeatureStructure[] _toArrayForSelect() { return toArray(); }

  /**
   * Not supported, will throw UnsupportedOperationException
   */
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    throw new UnsupportedOperationException();
  }
    
  /**
   * Copies an array of Feature Structures to an Array of Strings.
   * The strings are the "toString()" representation of the feature structures, 
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
      FeatureStructure fs = _maybeGetPearFs(get(i + srcPos));
      dest[i + destPos] = (fs == null) ? null : fs.toString();
    }
  }

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
   * no conversion to Pear trampolines done
   */
  @Override
  public void copyValuesFrom(CommonArray v) {
    T[] a;
    
    if (v instanceof FSArrayList) {
      a = (T[]) ((FSArrayList<T>)v).toArray();
    } else if (v instanceof FSArray) {
      a = (T[]) ((FSArray)v)._getTheArray();
    } else {
      throw new ClassCastException("argument must be of class FSArray or FSArrayList");
    } 
    copyFromArray(a, 0, 0, a.length);
  }
  
  /**
   * Convenience - create a FSArrayList from an existing FeatureStructure[]
   * @param jcas -
   * @param a -
   * @param <N> generic type of returned FS
   * @return -
   */
  public static <N extends TOP> FSArrayList<N> create(JCas jcas, N[] a) {
    FSArrayList<N> fsa = new FSArrayList<>(jcas, a.length);
    fsa.copyFromArray(a, 0, 0, a.length);
    return fsa;
  }
  
  public FeatureStructureImplC _superClone() {return clone();}  // enable common clone
  
  /**
   * @param c -
   * @return -
   * @see java.util.AbstractCollection#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection<?> c) {
    return gl().containsAll(c);
  }

  /**
   * @return -
   * @see java.util.ArrayList#isEmpty()
   */
  public boolean isEmpty() {
    return gl().isEmpty();
  }

  /**
   * @param o -
   * @return -
   * @see java.util.ArrayList#contains(java.lang.Object)
   */
  public boolean contains(Object o) {
    return gl().contains(o);
  }

  /**
   * @param o -
   * @return -
   * @see java.util.ArrayList#indexOf(java.lang.Object)
   */
  public int indexOf(Object o) {
    return gl().indexOf(o);
  }

  /**
   * @param o -
   * @return -
   * @see java.util.ArrayList#lastIndexOf(java.lang.Object)
   */
  public int lastIndexOf(Object o) {
    return gl().lastIndexOf(o);
  }

  /**
   * @param a -
   * @return -
   * @see java.util.ArrayList#toArray(java.lang.Object[])
   */
  public <T> T[] toArray(T[] a) {
    return gl().toArray(a);
  }

  /**
   * @return -
   * @see java.util.AbstractCollection#toString()
   */
  public String toString() {
    return gl().toString();
  }

  /**
   * @param e -
   * @return -
   * @see java.util.ArrayList#add(java.lang.Object)
   */
  public boolean add(T e) {
    maybeStartUsingArrayList();
    return fsArrayList.add(e);
  }

  /**
   * want equals to mean equal items, regardless of what format
   * @param o -
   * @return -
   * @see java.util.AbstractList#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (o instanceof FSArrayList) {
      FSArrayList other = (FSArrayList) o;
      if (size() == other.size()) {
        for (int i = size() - 1; i >= 0; i--) {
          if (!get(i).equals(other.get(i))) {
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
  public void add(int index, T element) {
    maybeStartUsingArrayList();
    fsArrayList.add(index, element);
  }

  /**
   * @param index -
   * @return -
   * @see java.util.ArrayList#remove(int)
   */
  public T remove(int index) {
    maybeStartUsingArrayList();
    return fsArrayList.remove(index);
  }

  /**
   * @param o -
   * @return -
   * @see java.util.ArrayList#remove(java.lang.Object)
   */
  public boolean remove(Object o) {
    maybeStartUsingArrayList();
    return fsArrayList.remove(o);
  }

  /**
   * want hashcode to depend only on equal items, regardless of what format
   * @return -
   * @see java.util.AbstractList#hashCode()
   */
  public int hashCode() {
    int hc = 1;
    final int prime = 31;
    for (int i = size() - 1; i >= 0; i++) {
      hc = hc * prime + i;
      hc = hc * prime + get(i).hashCode();
    }
    return hc;
  }

  /**
   * 
   * @see java.util.ArrayList#clear()
   */
  public void clear() {
    maybeStartUsingArrayList();
    fsArrayList.clear();
  }

  /**
   * @param c -
   * @return -
   * @see java.util.ArrayList#addAll(java.util.Collection)
   */
  public boolean addAll(Collection<? extends T> c) {
    maybeStartUsingArrayList();
    return fsArrayList.addAll(c);
  }

  /**
   * @param index -
   * @param c -
   * @return -
   * @see java.util.ArrayList#addAll(int, java.util.Collection)
   */
  public boolean addAll(int index, Collection<? extends T> c) {
    maybeStartUsingArrayList();
    return fsArrayList.addAll(index, c);
  }

  /**
   * @param c -
   * @return -
   * @see java.util.ArrayList#removeAll(java.util.Collection)
   */
  public boolean removeAll(Collection<?> c) {
    maybeStartUsingArrayList();
    return fsArrayList.removeAll(c);
  }

  /**
   * @param c -
   * @return -
   * @see java.util.ArrayList#retainAll(java.util.Collection)
   */
  public boolean retainAll(Collection<?> c) {
    maybeStartUsingArrayList();
    return fsArrayList.retainAll(c);
  }

  /**
   * @return -
   * @see java.util.Collection#stream()
   */
  public Stream<T> stream() {
    return gl().stream();
  }

  /**
   * @return -
   * @see java.util.Collection#parallelStream()
   */
  public Stream<T> parallelStream() {
    return gl().parallelStream();
  }

  /**
   * @param index -
   * @return -
   * @see java.util.ArrayList#listIterator(int)
   */
  public ListIterator<T> listIterator(int index) {
    return gl().listIterator(index);
  }

  /**
   * @return -
   * @see java.util.ArrayList#listIterator()
   */
  public ListIterator<T> listIterator() {
    return gl().listIterator();
  }

  /**
   * @return -
   * @see java.util.ArrayList#iterator()
   */
  public Iterator<T> iterator() {
    return gl().iterator();
  }

  /**
   * @param fromIndex -
   * @param toIndex -
   * @return -
   * @see java.util.ArrayList#subList(int, int)
   */
  public List<T> subList(int fromIndex, int toIndex) {
    return gl().subList(fromIndex, toIndex);
  }

  /**
   * @param action -
   * @see java.util.ArrayList#forEach(java.util.function.Consumer)
   */
  public void forEach(Consumer<? super T> action) {
    gl().forEach(action);
  }

  /**
   * @return -
   * @see java.util.ArrayList#spliterator()
   */
  public Spliterator<T> spliterator() {
    return gl().spliterator();
  }

  /**
   * @param filter -
   * @return -
   * @see java.util.ArrayList#removeIf(java.util.function.Predicate)
   */
  public boolean removeIf(Predicate<? super T> filter) {
    maybeStartUsingArrayList();
    return fsArrayList.removeIf(filter);
  }

  /**
   * @param operator -
   * @see java.util.ArrayList#replaceAll(java.util.function.UnaryOperator)
   */
  public void replaceAll(UnaryOperator<T> operator) {
    maybeStartUsingArrayList();
    fsArrayList.replaceAll(operator);
  }

  /**
   * @param c -
   * @see java.util.ArrayList#sort(java.util.Comparator)
   */
  public void sort(Comparator<? super T> c) {
    gl().sort(c);
  }
     
}
