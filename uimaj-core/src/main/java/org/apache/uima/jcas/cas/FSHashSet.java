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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;

import org.apache.uima.UimaSerializableFSs;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * A HashSet type containing Feature Structures,
 *   - Has all the methods of HashSet
 *   - Implements the select(...) APIs 
 *   
 * Implementation notes:
 *   - Uses UimaSerializable APIs *       
 */
public final class FSHashSet <T extends FeatureStructure> extends TOP implements 
                                 UimaSerializableFSs, SelectViaCopyToArray, 
                                 Set<T>, RandomAccess, Cloneable {

  /**
   * each cover class when loaded sets an index. used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(FSHashSet.class);

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
  private final HashSet<T> fsHashSet;
   
  public static final int _FI_fsArray = TypeSystemImpl.getAdjustedFeatureOffset("fsArray");
  
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private FSHashSet() {
    fsHashSet = null;
  }

  /**
   * Make a new ArrayList 
   * @param jcas The JCas
   */
  public FSHashSet(JCas jcas) {
    super(jcas);
    fsHashSet = new HashSet<>();

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }

  /**
   * Make a new ArrayList with an initial size 
   * @param jcas The JCas
   * @param length initial size
   */
  public FSHashSet(JCas jcas, int length) {
    super(jcas);
    _casView.validateArraySize(length);
    fsHashSet = new HashSet<>(length);

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
  public FSHashSet(TypeImpl t, CASImpl c) {
    super(t, c);  
    
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    fsHashSet = new HashSet<>();
  }

  // *------------------*
  // * Feature: fsArray
  /* getter for fsArray */
  private FSArray getFsArray() { return (FSArray) _getFeatureValueNc(_FI_fsArray); }

  /* setter for fsArray */
  private void setFsArray(FSArray v) {
    _setFeatureValueNcWj(_FI_fsArray, v); }
  
  public void _init_from_cas_data() {
    fsHashSet.clear();
    FSArray a = getFsArray();
    if (a != null) {
      fsHashSet.addAll((Collection<? extends T>) Arrays.asList(a));
    }
  }
  
  public void _save_to_cas_data() {
    FSArray a = getFsArray();
    if (a == null || a.size() != fsHashSet.size()) {
      a = new FSArray(_casView.getExistingJCas(), fsHashSet.size());
      setFsArray(a);
    }
    fsHashSet.toArray(a._getTheArray());
  }

  public FeatureStructure[] _toArrayForSelect() { return (FeatureStructure[]) toArray(); }

  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_superClone()
   */
  @Override
  public FeatureStructureImplC _superClone() { return clone();}  // enable common clone
  
  
  /**
   * @param o
   * @return
   * @see java.util.AbstractSet#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    return fsHashSet.equals(o);
  }

  /**
   * @return
   * @see java.util.AbstractSet#hashCode()
   */
  public int hashCode() {
    return fsHashSet.hashCode();
  }

  /**
   * @return
   * @see java.util.AbstractCollection#toArray()
   */
  public FeatureStructure[] toArray() {
    return (FeatureStructure[]) fsHashSet.toArray();
  }

  /**
   * @param c
   * @return
   * @see java.util.AbstractSet#removeAll(java.util.Collection)
   */
  public boolean removeAll(Collection<?> c) {
    return fsHashSet.removeAll(c);
  }

  /**
   * @param a
   * @return
   * @see java.util.AbstractCollection#toArray(java.lang.Object[])
   */
  public <T> T[] toArray(T[] a) {
    return fsHashSet.toArray(a);
  }

  /**
   * @return
   * @see java.util.HashSet#iterator()
   */
  public Iterator<T> iterator() {
    return fsHashSet.iterator();
  }

  /**
   * @return
   * @see java.util.HashSet#size()
   */
  public int size() {
    return fsHashSet.size();
  }

  /**
   * @return
   * @see java.util.HashSet#isEmpty()
   */
  public boolean isEmpty() {
    return fsHashSet.isEmpty();
  }

  /**
   * @param o
   * @return
   * @see java.util.HashSet#contains(java.lang.Object)
   */
  public boolean contains(Object o) {
    return fsHashSet.contains(o);
  }

  /**
   * @param e
   * @return
   * @see java.util.HashSet#add(java.lang.Object)
   */
  public boolean add(T e) {
    return fsHashSet.add(e);
  }

  /**
   * @param o
   * @return
   * @see java.util.HashSet#remove(java.lang.Object)
   */
  public boolean remove(Object o) {
    return fsHashSet.remove(o);
  }

  /**
   * 
   * @see java.util.HashSet#clear()
   */
  public void clear() {
    fsHashSet.clear();
  }




  /**
   * @param c
   * @return
   * @see java.util.AbstractCollection#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection<?> c) {
    return fsHashSet.containsAll(c);
  }

  /**
   * @param c
   * @return
   * @see java.util.AbstractCollection#addAll(java.util.Collection)
   */
  public boolean addAll(Collection<? extends T> c) {
    return fsHashSet.addAll(c);
  }

  /**
   * @return
   * @see java.util.HashSet#spliterator()
   */
  public Spliterator<T> spliterator() {
    return fsHashSet.spliterator();
  }

  /**
   * @param c
   * @return
   * @see java.util.AbstractCollection#retainAll(java.util.Collection)
   */
  public boolean retainAll(Collection<?> c) {
    return fsHashSet.retainAll(c);
  }

  /**
   * @return
   * @see java.util.AbstractCollection#toString()
   */
  public String toString() {
    return fsHashSet.toString();
  }
    
  
}
