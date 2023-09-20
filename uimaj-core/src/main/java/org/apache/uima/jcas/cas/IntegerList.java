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

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;

public abstract class IntegerList extends TOP implements CommonList, Iterable<Integer> {

  public static OfInt EMPTY_INT_ITERATOR = new OfInt() {

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public int nextInt() {
      throw new NoSuchElementException();
    }
  };

  // Never called.
  protected IntegerList() { // Disable default constructor
  }

  public IntegerList(JCas jcas) {
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

  public IntegerList(TypeImpl t, CASImpl c) {
    super(t, c);
  }

  public int getNthElement(int i) {
    return ((NonEmptyIntegerList) getNonEmptyNthNode(i)).getHead();
  }

  @Override
  public NonEmptyIntegerList createNonEmptyNode() {
    NonEmptyIntegerList node = new NonEmptyIntegerList(
            _casView.getTypeSystemImpl().intNeListType, _casView);
    return node;
  }

  @Override
  public NonEmptyIntegerList pushNode() {
    NonEmptyIntegerList n = createNonEmptyNode();
    n.setTail(this);
    return n;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator() overridden by NonEmptyIntegerList
   */
  @Override
  public OfInt iterator() {
    return EMPTY_INT_ITERATOR;
  }

  /**
   * pushes item onto front of this list
   * 
   * @param item
   *          the item to push onto the list
   * @return the new list, with this item as the head value of the first element
   */
  public NonEmptyIntegerList push(int item) {
    return new NonEmptyIntegerList(_casView.getJCasImpl(), item, this);
  }

  @Override
  public EmptyIntegerList emptyList() {
    return _casView.emptyIntegerList();
  }

  /**
   * Create an IntegerList from an existing array of ints
   * 
   * @param jcas
   *          the JCas to use
   * @param a
   *          the array of ints to populate the list with
   * @return an IntegerList, with the elements from the array
   */
  public static IntegerList create(JCas jcas, int[] a) {
    IntegerList integerList = jcas.getCasImpl().emptyIntegerList();
    for (int i = a.length - 1; i >= 0; i--) {
      integerList = integerList.push(a[i]);
    }
    return integerList;
  }

  public Stream<Integer> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  @Override
  public Spliterator.OfInt spliterator() {
    return Spliterators.spliterator(iterator(), Long.MAX_VALUE, 0);
  }

  public boolean contains(int v) {
    IntegerList node = this;
    while (node instanceof NonEmptyIntegerList) {
      NonEmptyIntegerList n = (NonEmptyIntegerList) node;
      if (n.getHead() == v) {
        return true;
      }
      node = n.getTail();
    }
    return false;
  }
}
