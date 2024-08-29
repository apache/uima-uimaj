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

import static java.util.Spliterator.ORDERED;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * Java Class model for Cas FSArray type extends FeatureStructure for backwards compatibility when
 * using FSArray with no typing.
 */
public final class FSArray<T extends FeatureStructure> extends TOP
        implements ArrayFSImpl<T>, Iterable<T>, SelectViaCopyToArray<T> {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public static final String _TypeName = CAS.TYPE_NAME_FS_ARRAY;

  /**
   * each cover class when loaded sets an index. used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public static final int typeIndexID = JCasRegistry.register(FSArray.class);

  public static final int type = typeIndexID;

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

  private final TOP[] theArray;

  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private FSArray() {
    theArray = null;
  }

  /**
   * Make a new FSArray of given size
   * 
   * @param jcas
   *          The JCas
   * @param length
   *          The number of elements in the new array
   */
  public FSArray(JCas jcas, int length) {
    super(jcas);
    _casView.validateArraySize(length);
    theArray = new TOP[length];

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2Size_arrays(length);
    }
  }

  /**
   * used by generator Make a new FSArray of given size
   * 
   * @param c
   *          -
   * @param t
   *          -
   * @param length
   *          the length of the array
   */
  public FSArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);
    _casView.validateArraySize(length);
    theArray = new TOP[length];

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2Size_arrays(length);
    }
  }

  /**
   * Constructs an instance of a subtype of FSArray having the component type clazz Note: the array
   * with this component type must already be specified in the type system declaration as a feature
   * whose range is FSArray with the specified elementType.
   * 
   * @param clazz
   *          - the FSArray's element's class
   * @param jcas
   *          -
   * @param length
   *          -
   */
  public FSArray(Class<? extends TOP> clazz, JCas jcas, int length) {
    this((TypeImpl) jcas.getCasType(clazz), jcas.getCasImpl(), length);
  }

  /** return the indexed value from the corresponding Cas FSArray as a Java Model object. */
  @Override
  public <U extends FeatureStructure> U get(int i) {
    return (U) _maybeGetPearFs(theArray[i]);
  }

  // internal use
  TOP get_without_PEAR_conversion(int i) {
    return theArray[i];
  }

  /** updates the Cas, setting the indexed value with the corresponding Cas FeatureStructure. */
  @Override
  public void set(int i, T av) {
    TOP v = (TOP) av;
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /**
       * Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list
       * element in a different CAS {2}.
       */
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView,
              _casView);
    }
    theArray[i] = _maybeGetBaseForPearFs(v);
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  // internal use
  void set_without_PEAR_conversion(int i, TOP v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /** return the size of the array. */
  @Override
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   */
  @Override
  public <U extends FeatureStructure> void copyFromArray(U[] src, int srcPos, int destPos,
          int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 || srcEnd > src.length || destEnd > size()) {
      throw new ArrayIndexOutOfBoundsException(
              String.format("FSArray.copyFromArray, srcPos: %,d destPos: %,d length: %,d", srcPos,
                      destPos, length));
    }

    // doing this element by element to get pear conversions done if needed, and
    // to get journaling done
    for (; srcPos < srcEnd && destPos < destEnd;) {
      set(destPos++, (T) src[srcPos++]);
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   */
  @Override
  public <U extends FeatureStructure> void copyToArray(int srcPos, U[] dest, int destPos,
          int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 || srcEnd > size() || destEnd > dest.length) {
      throw new ArrayIndexOutOfBoundsException(
              String.format("FSArray.copyToArray, srcPos: %,d destPos: %,d length: %,d", srcPos,
                      destPos, length));
    }
    for (; srcPos < srcEnd && destPos < destEnd;) {
      dest[destPos++] = (U) _maybeGetPearFs(get(srcPos++));
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  @Override
  public FeatureStructure[] toArray() {
    FeatureStructure[] r = new FeatureStructure[size()];
    copyToArray(0, r, 0, size());
    return r;
  }

  @Override
  public FeatureStructure[] _toArrayForSelect() {
    return toArray();
  }

  /**
   * Not supported, will throw UnsupportedOperationException
   */
  @Override
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    throw new UnsupportedOperationException();
  }

  /**
   * Copies an array of Feature Structures to an Array of Strings. The strings are the "toString()"
   * representation of the feature structures,
   * 
   * @param srcPos
   *          The index of the first element to copy.
   * @param dest
   *          The array to copy to.
   * @param destPos
   *          Where to start copying into <code>dest</code>.
   * @param length
   *          The number of elements to copy.
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>srcPos &lt; 0</code> or <code>length &gt; size()</code> or
   *              <code>destPos + length &gt; destArray.length</code>.
   */
  @Override
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, srcPos, length);
    for (int i = 0; i < length; i++) {
      FeatureStructure fs = _maybeGetPearFs(theArray[i + srcPos]);
      dest[i + destPos] = (fs == null) ? null : fs.toString();
    }
  }

  // internal use
  // used by serializers, other impls (e.g. FSHashSet)
  // no conversion to Pear trampolines done
  public TOP[] _getTheArray() {
    return theArray;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   * no conversion to Pear trampolines done
   */
  @Override
  public void copyValuesFrom(CommonArrayFS<T> v) {
    FSArray<T> bv = (FSArray<T>) v;
    System.arraycopy(bv.theArray, 0, theArray, 0, theArray.length);
    _casView.maybeLogArrayUpdates(this, 0, size());
  }

  /**
   * Convenience - create a FSArray from an existing FeatureStructure[]
   * 
   * @param jcas
   *          -
   * @param a
   *          -
   * @param <U>
   *          the element type of the FSArray, subtype of FeatureStructure
   * @return -
   */
  public static <U extends FeatureStructure> FSArray<U> create(JCas jcas, FeatureStructure[] a) {
    FSArray<U> fsa = new FSArray<>(jcas, a.length);
    fsa.copyFromArray(a, 0, 0, a.length);
    return fsa;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return (T) get(i++); // does trampoline conversion
      }
    };
  }

  @Override
  public Spliterator<T> spliterator() {
    return Spliterators.spliterator(iterator(), size(), ORDERED);
  }

  public Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  public boolean contains(Object o) {
    if (null == o) {
      for (TOP e : theArray) {
        if (e == null) {
          return true;
        }
      }
      return false;
    }

    if (!(o instanceof TOP)) {
      return false;
    }
    TOP item = (TOP) o;
    for (TOP e : theArray) {
      if (item.equals(e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public <U extends TOP> U[] toArray(U[] a) {
    final int sz = size();
    if (a.length < sz) {
      @SuppressWarnings("unchecked")
      U[] copy = (U[]) Array.newInstance(a.getClass().getComponentType(), sz);
      copyToArray(0, copy, 0, sz);
      return copy;
    }

    copyToArray(0, a, 0, size());
    return a;
  }

}
