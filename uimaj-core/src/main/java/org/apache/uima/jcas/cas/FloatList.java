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
    NonEmptyFloatList node = new NonEmptyFloatList(this._casView.getTypeSystemImpl().floatNeListType, this._casView);
    return node;
  }
  
  /**
  * pushes item onto front of this list
  * @param item the item to push onto the list
  * @return the new list, with this item as the head value of the first element
  */
 public NonEmptyFloatList push(float item) {
   return new NonEmptyFloatList(_casView.getExistingJCas(), item, this);
 }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Float> iterator() {
    return Collections.emptyIterator();  // overridden by NonEmptyXxList
  }
}
