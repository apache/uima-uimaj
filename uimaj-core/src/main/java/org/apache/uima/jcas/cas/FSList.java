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

import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.SelectFSs_impl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;

/**
 * 
 * T extends TOP, v2 already mandated TOP for set/get
 * 
 * @param <T>
 *          the type of the elements in the list
 */
public abstract class FSList<T extends TOP> extends TOP implements CommonList, Iterable<T> {

  // for removed markers
  protected FSList() {// Disable default constructor

  }

  public FSList(JCas jcas) {
    super(jcas);
  }

  /**
   * used by generator Make a new AnnotationBase
   * 
   * @param c
   *          -
   * @param t
   *          -
   */

  public FSList(TypeImpl t, CASImpl c) {
    super(t, c);
  }

  public T getNthElement(int i) {
    FSList<T> node = (FSList<T>) getNthNode(i);
    if (node instanceof EmptyFSList) {
      throw new CASRuntimeException(CASRuntimeException.JCAS_GET_NTH_PAST_END, Integer.toString(i));
    }
    return ((NonEmptyFSList<T>) node).getHead();
  }

  @Override
  public NonEmptyFSList<T> createNonEmptyNode() {
    return new NonEmptyFSList<>(_casView.getJCasImpl());
  }

  @Override
  public NonEmptyFSList<T> pushNode() {
    NonEmptyFSList<T> n = createNonEmptyNode();
    n.setTail(this);
    return n;
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   */
  public <U extends T> SelectFSs<U> select() {
    return new SelectFSs_impl<>(this);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param filterByType
   *          only includes elements of this type
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   * @throws IllegalArgumentException
   *           if no type is specified.
   */
  public <U extends T> SelectFSs<U> select(Type filterByType) {
    return new SelectFSs_impl<>(this).type(filterByType);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param filterByType
   *          only includes elements of this JCas class
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   * @throws IllegalArgumentException
   *           if no type is specified.
   */
  public <U extends T> SelectFSs<U> select(Class<U> filterByType) {
    return new SelectFSs_impl<>(this).type(filterByType);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param filterByType
   *          only includes elements of this JCas class's type
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   */
  public <U extends T> SelectFSs<U> select(int filterByType) {
    return new SelectFSs_impl<>(this).type(filterByType);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.
   * 
   * @param filterByType
   *          only includes elements of this type (fully qualified type name)
   * @param <U>
   *          generic type being selected
   * @return a new instance of SelectFSs
   * @throws IllegalArgumentException
   *           if no type is specified.
   */
  public <U extends T> SelectFSs<U> select(String filterByType) {
    return new SelectFSs_impl<>(this).type(filterByType);
  }

  /**
   * Create an FSList from an existing array of Feature Structures
   * 
   * @param jcas
   *          the JCas to use
   * @param a
   *          the array of Feature Structures to populate the list with
   * @param <U>
   *          the type of FeatureStructures being stored in the FSList being created
   * @param <E>
   *          the type of the array argument
   * @return an FSList, with the elements from the array
   */
  public static <U extends TOP, E extends FeatureStructure> FSList<U> create(JCas jcas, E[] a) {
    FSList<U> fsl = jcas.getCasImpl().emptyFSList();
    for (int i = a.length - 1; i >= 0; i--) {
      fsl = fsl.push((U) a[i]);
    }
    return fsl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return Collections.<T> emptyIterator(); // overridden by NonEmptyFSList
  }

  /**
   * pushes item onto front of this list
   * 
   * @param item
   *          the item to push onto the list
   * @return the new list, with this item as the head value of the first element
   */
  public NonEmptyFSList<T> push(T item) {
    return new NonEmptyFSList<>(_casView.getJCasImpl(), item, this);
  }

  /**
   * @return a stream over this FSList
   */
  public Stream<T> stream() {
    return (Stream<T>) StreamSupport.stream(spliterator(), false);
  }

  @Override
  public EmptyFSList emptyList() {
    return _casView.emptyFSList();
  }

  public boolean contains(T v) {
    FSList<T> node = this;
    while (node instanceof NonEmptyFSList) {
      NonEmptyFSList<T> n = (NonEmptyFSList<T>) node;
      if (n.getHead() == v) {
        return true;
      }
      node = n.getTail();
    }
    return false;
  }
}
