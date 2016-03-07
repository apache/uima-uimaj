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

import java.util.List;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyIntegerList extends IntegerList implements NonEmptyList {

  public final static int typeIndexID = JCasRegistry.register(NonEmptyIntegerList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }
  
  public final static int _FI_head = TypeSystemImpl.getAdjustedFeatureOffset("head");
  public final static int _FI_tail = TypeSystemImpl.getAdjustedFeatureOffset("tail");

//  /* local data */
//  private int _F_head;
//  private IntegerList _F_tail;
  
  // Never called. Disable default constructor
  protected NonEmptyIntegerList() {
  }

  public NonEmptyIntegerList(JCas jcas) {
    super(jcas);
  }
  
  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public NonEmptyIntegerList(TypeImpl t, CASImpl c) {
    super(t, c);
  }


  // *------------------*
  // * Feature: head
  /* getter for head * */
  public int getHead() { return _getIntValueNc(_FI_head); }

  /* setter for head * */
  public void setHead(int v) {
    _setIntValueNfc(_getFeatFromAdjOffset(_FI_head, true), v);
  }

//  public void _setHeadNcNj(int v) { _FI_head = v;}
  
  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public IntegerList getTail() { return (IntegerList) _getFeatureValueNc(_FI_tail); }

  /* setter for tail * */
  public void setTail(IntegerList v) { _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_tail, false), v); }
  
  public void setTail(CommonList v) { setTail((IntegerList)v); }
    
  public void setHead(List<String> stringValues, int i) {
    setHead(Integer.parseInt(stringValues.get(i)));
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#get_headAsString()
   */
  @Override
  public String get_headAsString() {
    return Integer.toString(((NonEmptyIntegerList)this).getHead());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#set_headFromString(java.lang.String)
   */
  @Override
  public void set_headFromString(String v) {
    setHead(Integer.parseInt(v));
  }  

  
}
