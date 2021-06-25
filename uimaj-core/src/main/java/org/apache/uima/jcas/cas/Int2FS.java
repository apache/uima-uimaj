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

/* Apache UIMA v3 - First created by JCasGen Fri Jan 20 11:55:59 EST 2017 */

package org.apache.uima.jcas.cas;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.UimaSerializableFSs;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.util.IntEntry;
import org.apache.uima.util.impl.Constants;

/**
 * A map from ints to Feature Structures
 * 
 * Is Pear aware - stores non-pear versions but may return pear version in pear contexts
 */
public class Int2FS<T extends TOP> extends TOP implements UimaSerializableFSs, Cloneable {

  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static String _TypeName = "org.apache.uima.jcas.cas.Int2FS";

  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static int typeIndexID = JCasRegistry.register(Int2FS.class);
  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static int type = typeIndexID;

  /**
   * @generated
   * @return index of the type
   */
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

//@formatter:off
  /** 
   * lifecycle   
   *   - starts as empty array list   
   *   - becomes non-empty when updated (add)       
   *   -- used from that point on. 
   */
//@formatter:on
  private boolean isPendingInit = false;
  private boolean isSaveNeeded = false;

  private final Int2ObjHashMap<TOP, T> int2FS; // not set here to allow initial size version

//@formatter:off
  /* *******************
   *   Feature Offsets *
   * *******************/ 
//@formatter:on
  public final static String _FeatName_fsArray = "fsArray";

  /* Feature Adjusted Offsets */
  private final static CallSite _FC_fsArray = TypeSystemImpl.createCallSiteForBuiltIn(Int2FS.class,
          "fsArray");
  private final static MethodHandle _FH_fsArray = _FC_fsArray.dynamicInvoker();

  public final static String _FeatName_intArray = "intArray";

  /* Feature Adjusted Offsets */
  private final static CallSite _FC_intArray = TypeSystemImpl.createCallSiteForBuiltIn(Int2FS.class,
          "intArray");
  private final static MethodHandle _FH_intArray = _FC_intArray.dynamicInvoker();

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected Int2FS() {
    int2FS = null;
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   * @param casImpl
   *          the CAS this Feature Structure belongs to
   * @param type
   *          the type of this Feature Structure
   */
  public Int2FS(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    int2FS = new Int2ObjHashMap<>(TOP.class);

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }

  /**
   * @generated
   * @param jcas
   *          JCas to which this Feature Structure belongs
   */
  public Int2FS(JCas jcas) {
    super(jcas);
    int2FS = new Int2ObjHashMap<>(TOP.class);

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }

  /**
   * Make a new Int2FS map with an initial capacity.
   *
   * @param jcas
   *          The JCas
   * @param length
   *          initial size
   */
  public Int2FS(JCas jcas, int length) {
    super(jcas);
    _casView.validateArraySize(length);
    int2FS = new Int2ObjHashMap<>(TOP.class, length);

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }

  // *--------------*
  // * Feature: fsArray

  /**
   * getter for fsArray - internal use
   * 
   * @generated
   * @return value of the feature
   */
  private FSArray<T> getFsArray() {
    return (FSArray<T>) (_getFeatureValueNc(wrapGetIntCatchException(_FH_fsArray)));
  }

  /**
   * setter for fsArray - internal use
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  private void setFsArray(FSArray<T> v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_fsArray), v);
  }

  /**
   * getter for intArray - internal use
   * 
   * @generated
   * @return value of the feature
   */
  private IntegerArray getIntArray() {
    return (IntegerArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_intArray)));
  }

  /**
   * setter for intArray - sets internal use
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  private void setIntArray(IntegerArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_intArray), v);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaSerializable#_init_from_cas_data()
   */
  @Override
  public void _init_from_cas_data() {
    isPendingInit = true;
  }

  private void maybeLazyInit() {
    if (isPendingInit) {
      lazyInit();
    }
  }

  private void lazyInit() {
    isPendingInit = false;
    int2FS.clear();
    FSArray<T> a = getFsArray();
    IntegerArray ii = getIntArray();
    int size = ii.size();

    for (int i = 0; i < size; i++) {
      int2FS.put(ii.get(i), a.get(i));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaSerializable#_save_to_cas_data()
   */
  @Override
  public void _save_to_cas_data() {
    if (isSaveNeeded) {
      isSaveNeeded = false;
      FSArray<T> fsa = getFsArray();
      IntegerArray ia = getIntArray();
      int newSize = int2FS.size();
      if (fsa == null || fsa.size() != newSize) {
        fsa = new FSArray<>(_casView.getJCasImpl(), newSize);
        setFsArray(fsa);
        ia = new IntegerArray(_casView.getJCasImpl(), newSize);
        setIntArray(ia);
      }

      // using element by element instead of bulk operations
      // in case fsa was preallocated and right size, may need journaling

      Iterator<IntEntry<T>> it = int2FS.iterator();
      int[] i = new int[1];

      FSArray<T> fsa_final = fsa;
      IntegerArray ia_final = ia;

      it.forEachRemaining(e -> {
        ia_final.set(i[0], e.getKey());
        fsa_final.set_without_PEAR_conversion(i[0], (T) e.getValue());
        i[0]++;
      });
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaSerializable#_superClone()
   */
  @Override
  public FeatureStructureImplC _superClone() {
    return clone();
  } // enable common clone

  private TOP[] gta() {
    FSArray<T> fsa = getFsArray();
    return (null == fsa) ? Constants.EMPTY_TOP_ARRAY : fsa._getTheArray();
  }

  // private int[] gtia() {
  // IntegerArray ia = getIntArray();
  // return (null == ia)
  // ? Constants.EMPTY_INT_ARRAY
  // : ia._getTheArray();
  // }

  // no non-default equals and hashcode - is very expensive

  public Collection<T> values() {
    if (isSaveNeeded) {
      return (Collection<T>) Arrays.asList(int2FS.valuesArray());
    }
    return (Collection<T>) Arrays.asList(gta().clone());
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final int maxLen = 10;
    return "int2FS [isPendingInit=" + isPendingInit + ", isSaveNeeded=" + isSaveNeeded + ", int2FS="
            + (int2FS != null ? toString(int2FS, maxLen) : null) + "]";
  }

  /**
   * To string.
   *
   * @param collection
   *          the collection
   * @param maxLen
   *          the max len
   * @return the string
   */
  private String toString(Int2ObjHashMap<TOP, T> collection, int maxLen) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    int i = 0;
    for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(iterator.next());
    }
    builder.append("]");
    return builder.toString();
  }

  public int size() {
    return isSaveNeeded ? int2FS.size() : gta().length;
  }

  /**
   * Checks if is empty.
   *
   * @return true, if is empty
   * @see java.util.HashSet#isEmpty()
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  public boolean containsKey(Object key) {
    maybeLazyInit();
    return int2FS.containsKey((int) key);
  }

  public boolean containsValue(Object value) {
    if (!(value instanceof TOP)) {
      return false;
    }
    return values().contains(value);
  }

  public T get(int key) {
    maybeLazyInit();
    return int2FS.get(key);
  }

  public T put(int key, T value) {
    maybeLazyInit();
    isSaveNeeded = true;
    return (T) int2FS.put(key, value);

  }

  public T remove(int key) {
    maybeLazyInit();
    isSaveNeeded = true;
    return (T) int2FS.remove(key);
  }

  public void clear() {
    if (size() == 0) {
      return;
    }
    maybeLazyInit();
    isSaveNeeded = true;
    int2FS.clear();
  }

  public Iterator<IntEntry<T>> iterator() {
    maybeLazyInit();
    return int2FS.iterator();
  }

}
