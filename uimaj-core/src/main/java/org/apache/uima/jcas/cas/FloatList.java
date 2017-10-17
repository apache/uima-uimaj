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

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;

public abstract class FloatList extends TOP implements CommonList, Iterable<Float> {

	// Never called.
	protected FloatList() {// Disable default constructor
	}

	public FloatList(JCas jcas) {
		super(jcas);
	}

  /**
  * used by generator
  * Make a new AnnotationBase
  * @param c -
  * @param t -
  */

   public FloatList(TypeImpl t, CASImpl c) {
     super(t, c);
   }
   
  public float getNthElement(int i) {
    return ((NonEmptyFloatList) getNonEmptyNthNode(i)).getHead();
  }
  
  
  @Override
  public NonEmptyFloatList createNonEmptyNode() {
    return new NonEmptyFloatList(this._casView.getJCasImpl());
  }
  
  /**
  * pushes item onto front of this list
  * @param item the item to push onto the list
  * @return the new list, with this item as the head value of the first element
  */
 public NonEmptyFloatList push(float item) {
   return new NonEmptyFloatList(_casView.getJCasImpl(), item, this);
 }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Float> iterator() {
    return Collections.emptyIterator();  // overridden by NonEmptyXxList
  }
  
  @Override
  public EmptyFloatList emptyList() {
    return this._casView.emptyFloatList();
  }
  
  /**
   * Create an FloatList from an existing array of Feature Structures
   * @param jcas the JCas to use
   * @param a the array of Floats to populate the list with
   * @return an FloatList, with the elements from the array
   */
  public static FloatList create(JCas jcas, Float[] a) {
    FloatList floatList = jcas.getCasImpl().emptyFloatList();   
    for (int i = a.length - 1; i >= 0; i--) {
      floatList = floatList.push(a[i]);
    }   
    return floatList;
  }
  
  public Stream<Float> stream() {
    return StreamSupport.stream(spliterator(), false);
  }
  
  public boolean contains(float v) {
    FloatList node = this;
    while (node instanceof NonEmptyFloatList) {
      NonEmptyFloatList n = (NonEmptyFloatList) node;
      if (n.getHead() == v) {
        return true;
      }
      node = n.getTail();
    }
    return false;
  }
}
