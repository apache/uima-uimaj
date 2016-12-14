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

import java.util.AbstractList;
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
 *
 * @param <T> the generic type
 */
public final class FSArrayList <T extends TOP> extends TOP implements 
                                 UimaSerializableFSs, CommonArray, CommonArrayFS, SelectViaCopyToArray, 
                                 List<T>, RandomAccess, Cloneable {

  /** The Constant EMPTY_LIST. */
  private final static List<? extends TOP> EMPTY_LIST = Arrays.asList(Constants.EMPTY_TOP_ARRAY);
  /**
   * each cover class when loaded sets an index. used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(FSArrayList.class);

  /** The Constant type. */
  public final static int type = typeIndexID;

  /**
   * used to obtain reference to the _Type instance.
   *
   * @return the type array index
   */
  // can't be factored - refs locally defined field
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /** lifecycle   - starts as empty array list   - becomes non-empty when updated (add)       -- used from that point on. */
  private final List<T> fsArrayList;
  
  /** lifecycle   - starts as the empty list   - set when _init_from_cas_data()   - set to null when update (add/remove) happens. */
  private List<T> fsArrayAsList = (List<T>) EMPTY_LIST;
  
  /** The Constant _FI_fsArray. */
  public static final int _FI_fsArray = TypeSystemImpl.getAdjustedFeatureOffset("fsArray");
  
  /**
   * Instantiates a new FS array list.
   */
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private FSArrayList() {
    fsArrayList = null;
  }

  /**
   * Make a new ArrayList .
   *
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
   * Make a new ArrayList with an initial size .
   *
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
   * Make a new FSArrayList.
   *
   * @param t -
   * @param c -
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
  /**
   * Gets the fs array.
   *
   * @return the fs array
   */
  /* getter for fsArray */
  private FSArray getFsArray() { return (FSArray) _getFeatureValueNc(_FI_fsArray); }

  /**
   * Sets the fs array.
   *
   * @param v the new fs array
   */
  /* setter for fsArray */
  private void setFsArray(FSArray v) {
    _setFeatureValueNcWj(_FI_fsArray, v); }

  /**
   * Maybe start using array list.
   */
  private void maybeStartUsingArrayList() {
    if (fsArrayAsList != null) {
      fsArrayList.clear();
      fsArrayList.addAll(fsArrayAsList);
      fsArrayAsList = null;  // stop using this one
    }
  }
  
  /**
   * Gta.
   *
   * @return the TO p[]
   */
  private TOP[] gta() {
    FSArray fsa = getFsArray();
    if (null == fsa) {
      return Constants.EMPTY_TOP_ARRAY;
    }
    return fsa._getTheArray();
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_init_from_cas_data()
   */
  @Override
  public void _init_from_cas_data() {
    // special handling to have getter and setter honor pear trampolines
//    fsArrayAsList = (List<T>) Arrays.asList(gta());
    final FSArray fsa = getFsArray();
    if (null == fsa) {
      fsArrayAsList = Collections.emptyList();
    } else {
    
      fsArrayAsList = new AbstractList<T>() {
        int i = 0;
        @Override
        public T get(int index) {
          return (T) fsa.get(i);
        }
  
        @Override
        public int size() {
          return fsa.size();
        }      
      };
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_save_to_cas_data()
   */
  @Override
  public void _save_to_cas_data() {
    // if fsArraysAsList is not null, then the cas data form is still valid, do nothing
    if (null != fsArrayAsList) {
      return;
    }
    
    // reallocate fsArray if wrong size
    final int sz = size();
    FSArray fsa = getFsArray();
    if (fsa == null || fsa.size() != sz) {
      setFsArray(fsa = new FSArray(_casView.getExistingJCas(), sz));
    }
    
    // using element by element instead of bulk operations to
    //   pick up any pear trampoline conversion and 
    //   in case fsa was preallocated and right size, may need journaling
    int i = 0;
    for (TOP fs : fsArrayList) {
      TOP currentValue = fsa.get(i);
      if (currentValue != fs) {
        fsa.set(i, fs); // done this way to record for journaling for delta CAS
      }
      i++;
    }
  }
  
  /**
   * Gl.
   *
   * @return the list
   */
  private List<T> gl () {
    return (null == fsArrayAsList) 
      ? fsArrayList
      : fsArrayAsList;
  }
  
  /* (non-Javadoc)
   * @see java.util.List#get(int)
   */
  @Override
  public T get(int i) {
    return gl().get(i);
  }

  /**
   * updates the i-th value of the FSArrayList.
   *
   * @param i the i
   * @param v the v
   * @return the t
   */
  @Override
  public T set(int i, T v) {
    
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView, _casView);
    }
    return gl().set(i, v);
  }
  
  /**
   *  return the size of the array.
   *
   * @return the int
   */
  @Override
  public int size() {
    return gl().size();
  }

  /**
   * Copy from array.
   *
   * @param src -
   * @param srcPos -
   * @param destPos -
   * @param length -
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
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
   * Copy to array.
   *
   * @param srcPos -
   * @param dest -
   * @param destPos -
   * @param length -
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
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
      dest[destPos++] = get(srcPos++);
    }
  }

  /**
   * Note: doesn't convert to pear trampolines!.
   *
   * @return the feature structure[]
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  @Override
  public T[] toArray() {
    T[] r = (T[]) new TOP[size()];
    copyToArray(0, r, 0, size());
    return r;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.SelectViaCopyToArray#_toArrayForSelect()
   */
  @Override
  public FeatureStructure[] _toArrayForSelect() { return toArray(); }

  /**
   * Not supported, will throw UnsupportedOperationException.
   *
   * @param src the src
   * @param srcPos the src pos
   * @param destPos the dest pos
   * @param length the length
   */
  @Override
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
  @Override
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(size(), srcPos, length);
    int i = 0;
    for (T fs : this) {
      dest[i + destPos] = (fs == null) ? null : fs.toString();
      i++;
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#toStringArray()
   */
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
   * Convenience - create a FSArrayList from an existing FeatureStructure[].
   *
   * @param <N> generic type of returned FS
   * @param jcas -
   * @param a -
   * @return -
   */
  public static <N extends TOP> FSArrayList<N> create(JCas jcas, N[] a) {
    FSArrayList<N> fsa = new FSArrayList<>(jcas, a.length);
    fsa.copyFromArray(a, 0, 0, a.length);
    return fsa;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_superClone()
   */
  @Override
  public FeatureStructureImplC _superClone() {return clone();}  // enable common clone
  
  /**
   * Contains all.
   *
   * @param c -
   * @return -
   * @see java.util.AbstractCollection#containsAll(java.util.Collection)
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    return gl().containsAll(c);
  }

  /**
   * Checks if is empty.
   *
   * @return -
   * @see java.util.ArrayList#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return gl().isEmpty();
  }

  /**
   * Contains.
   *
   * @param o -
   * @return -
   * @see java.util.ArrayList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Object o) {
    if (!(o instanceof TOP)) return false;
    TOP fs = (TOP) o;    
    return gl().contains(fs);
  }

  /**
   * Index of.
   *
   * @param o -
   * @return -
   * @see java.util.ArrayList#indexOf(java.lang.Object)
   */
  @Override
  public int indexOf(Object o) {
    if (!(o instanceof TOP)) return -1;
    TOP fs = (TOP) o;    
    return gl().indexOf(fs);
  }

  /**
   * Last index of.
   *
   * @param o -
   * @return -
   * @see java.util.ArrayList#lastIndexOf(java.lang.Object)
   */
  @Override
  public int lastIndexOf(Object o) {
    if (!(o instanceof TOP)) return -1;
    TOP fs = (TOP) o;    
    return gl().lastIndexOf(fs);
  }

  /**
   * To array.
   *
   * @param <T> the generic type
   * @param a -
   * @return -
   * @see java.util.ArrayList#toArray(java.lang.Object[])
   */
  @Override
  public <T> T[] toArray(T[] a) {
    return gl().toArray(a);
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final int maxLen = 10;
    return "FSArrayList [size="
        + size()
        + ", fsArrayList="
        + (fsArrayList != null ? fsArrayList.subList(0, Math.min(fsArrayList.size(), maxLen))
            : null)
        + ", fsArrayAsList=" + (fsArrayAsList != null
            ? fsArrayAsList.subList(0, Math.min(fsArrayAsList.size(), maxLen)) : null)
        + "]";
  }

  /**
   * Adds the.
   *
   * @param e -
   * @return -
   * @see java.util.ArrayList#add(java.lang.Object)
   */
  @Override
  public boolean add(T e) {
    maybeStartUsingArrayList();
    return fsArrayList.add(e);
  }

  /**
   * equals means equal items, same order.
   *
   * @param o -
   * @return -
   * @see java.util.AbstractList#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FSArrayList)) return false;
    FSArrayList<T> other = (FSArrayList<T>) o;
    if (size() != other.size()) return false;
    
    Iterator<T> it_other = other.iterator();
    for (T item : this) {
      if (!item.equals(it_other.next())) return false;
    }
    return true;
  }

  /**
   * Adds the.
   *
   * @param index -
   * @param element -
   * @see java.util.ArrayList#add(int, java.lang.Object)
   */
  @Override
  public void add(int index, T element) {
    maybeStartUsingArrayList();
    fsArrayList.add(index, element);
  }

  /**
   * Removes the.
   *
   * @param index -
   * @return -
   * @see java.util.ArrayList#remove(int)
   */
  @Override
  public T remove(int index) {
    maybeStartUsingArrayList();
    return fsArrayList.remove(index);
  }

  /**
   * Removes the.
   *
   * @param o -
   * @return -
   * @see java.util.ArrayList#remove(java.lang.Object)
   */
  @Override
  public boolean remove(Object o) {
    maybeStartUsingArrayList();
    return fsArrayList.remove(o);
  }

  /**
   * want hashcode to depend only on equal items, regardless of what format.
   *
   * @return -
   * @see java.util.AbstractList#hashCode()
   */
  @Override
  public int hashCode() {
    int hc = 1;
    final int prime = 31;
    for (T item : this) {
      hc = hc * prime + item.hashCode();
    }
    return hc;
  }

  /**
   * Clear.
   *
   * @see java.util.ArrayList#clear()
   */
  @Override
  public void clear() {
    maybeStartUsingArrayList();
    fsArrayList.clear();
  }

  /**
   * Adds the all.
   *
   * @param c -
   * @return -
   * @see java.util.ArrayList#addAll(java.util.Collection)
   */
  @Override
  public boolean addAll(Collection<? extends T> c) {
    maybeStartUsingArrayList();
    return fsArrayList.addAll(c);
  }

  /**
   * Adds the all.
   *
   * @param index -
   * @param c -
   * @return -
   * @see java.util.ArrayList#addAll(int, java.util.Collection)
   */
  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    maybeStartUsingArrayList();
    return fsArrayList.addAll(index, c);
  }

  /**
   * Removes the all.
   *
   * @param c -
   * @return -
   * @see java.util.ArrayList#removeAll(java.util.Collection)
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    maybeStartUsingArrayList();
    return fsArrayList.removeAll(c);
  }

  /**
   * Retain all.
   *
   * @param c -
   * @return -
   * @see java.util.ArrayList#retainAll(java.util.Collection)
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    maybeStartUsingArrayList();
    return fsArrayList.retainAll(c);
  }

  /**
   * Stream.
   *
   * @return -
   * @see java.util.Collection#stream()
   */
  @Override
  public Stream<T> stream() {
    return gl().stream();
  }

  /**
   * Parallel stream.
   *
   * @return -
   * @see java.util.Collection#parallelStream()
   */
  @Override
  public Stream<T> parallelStream() {
    return gl().parallelStream();
  }

  /**
   * List iterator.
   *
   * @param index -
   * @return -
   * @see java.util.ArrayList#listIterator(int)
   */
  @Override
  public ListIterator<T> listIterator(int index) {
    return gl().listIterator(index);
  }

  /**
   * List iterator.
   *
   * @return -
   * @see java.util.ArrayList#listIterator()
   */
  @Override
  public ListIterator<T> listIterator() {
    return gl().listIterator();
  }

  /**
   * Iterator.
   *
   * @return -
   * @see java.util.ArrayList#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return gl().iterator();
  }

  /**
   * Sub list.
   *
   * @param fromIndex -
   * @param toIndex -
   * @return -
   * @see java.util.ArrayList#subList(int, int)
   */
  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return gl().subList(fromIndex, toIndex);
  }

  /**
   * For each.
   *
   * @param action -
   * @see java.util.ArrayList#forEach(java.util.function.Consumer)
   */
  @Override
  public void forEach(Consumer<? super T> action) {
    gl().forEach(action);
  }

  /**
   * Spliterator.
   *
   * @return -
   * @see java.util.ArrayList#spliterator()
   */
  @Override
  public Spliterator<T> spliterator() {
    return gl().spliterator();
  }

  /**
   * Removes the if.
   *
   * @param filter -
   * @return -
   * @see java.util.ArrayList#removeIf(java.util.function.Predicate)
   */
  @Override
  public boolean removeIf(Predicate<? super T> filter) {
    maybeStartUsingArrayList();
    return fsArrayList.removeIf(filter);
  }

  /**
   * Replace all.
   *
   * @param operator -
   * @see java.util.ArrayList#replaceAll(java.util.function.UnaryOperator)
   */
  @Override
  public void replaceAll(UnaryOperator<T> operator) {
    maybeStartUsingArrayList();
    fsArrayList.replaceAll(operator);
  }

  /**
   * Sort.
   *
   * @param c -
   * @see java.util.ArrayList#sort(java.util.Comparator)
   */
  @Override
  public void sort(Comparator<? super T> c) {
    gl().sort(c);
  }
     
}
