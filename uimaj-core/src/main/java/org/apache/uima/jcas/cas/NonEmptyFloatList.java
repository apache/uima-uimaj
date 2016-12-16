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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyFloatList extends FloatList implements NonEmptyList {

  public final static int typeIndexID = JCasRegistry.register(NonEmptyFloatList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  public static final int _FI_head = TypeSystemImpl.getAdjustedFeatureOffset("head");
  public static final int _FI_tail = TypeSystemImpl.getAdjustedFeatureOffset("tail");
  
  /* local data */
//  private float _F_head;
//  private FloatList _F_tail;
  
  // Never called. Disable default constructor
  protected NonEmptyFloatList() {
  }

  public NonEmptyFloatList(JCas jcas) {
    super(jcas);
  }

  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public NonEmptyFloatList(TypeImpl t, CASImpl c) {
    super(t, c);
  }

  /**
   * Generate a NonEmpty node with the specified head and tail
   * @param jcas -
   * @param v -
   * @param tail -
   */
  public NonEmptyFloatList(JCas jcas, float v, CommonList tail) {
    this(jcas);
    setHead(v);
    setTail(tail);
  }
  
  /**
   * Generate a NonEmpty node with the specified head with the empty node as the tail
   * @param jcas -
   * @param v -
   */
  public NonEmptyFloatList(JCas jcas, float v) {
    this(jcas, v, jcas.getCasImpl().getEmptyFloatList());
  }
  
  // *------------------*
  // * Feature: head
  /* getter for head * */
  public float getHead() { return _getFloatValueNc(_FI_head); }

  /* setter for head * */
  public void setHead(float v) {
    this._setFloatValueNfc(_FI_head, v);
  }

//  public void _setHeadNcNj(float v) { setFloatValueNcNj(_getFeat(_FI_head), v); }
  
  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public FloatList getTail() { return (FloatList) _getFeatureValueNc(_FI_tail); }

  /* setter for tail * */
  public void setTail(FloatList v) {
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView, _casView);
    }
    _setFeatureValueNcWj(_FI_tail, v); 
  }

  public void setTail(CommonList v) { setTail((FloatList) v); }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#get_headAsString()
   */
  @Override
  public String get_headAsString() {
    return Float.toString(((NonEmptyFloatList)this).getHead());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#set_headAsString(java.lang.String)
   */
  @Override
  public void set_headFromString(String v) {
    setHead(Float.parseFloat(v));
  }
   
  @Override
  public EmptyFloatList getEmptyList() {
    return this._casView.getEmptyFloatList();
  }
  
  @Override
  public Iterator<Float> iterator() {
    return new Iterator<Float>() {

      FloatList node = NonEmptyFloatList.this;
      
      @Override
      public boolean hasNext() {
        return node instanceof NonEmptyFloatList;
      }

      @Override
      public Float next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        NonEmptyFloatList nn = (NonEmptyFloatList)node; 
        Float element = nn.getHead();
        node = nn.getTail();
        return element;
      }
      
    };
  }

}
