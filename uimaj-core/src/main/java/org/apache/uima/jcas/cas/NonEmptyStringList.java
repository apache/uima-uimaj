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

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyStringList extends StringList implements NonEmptyList {

  public final static int typeIndexID = JCasRegistry.register(NonEmptyStringList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  public static final int _FI_head = TypeSystemImpl.getAdjustedFeatureOffset("head");
  public static final int _FI_tail = TypeSystemImpl.getAdjustedFeatureOffset("tail");
  
//  /* local data */
//  private String _F_head;
//  private StringList _F_tail;
  
  // Never called. Disable default constructor
  protected NonEmptyStringList() {
  }

  public NonEmptyStringList(JCas jcas) {
    super(jcas);
  }

  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public NonEmptyStringList(TypeImpl t, CASImpl c) {
    super(t, c);
  }
  
// *------------------*
  // * Feature: head
  /* getter for head * */
  public String getHead() { return _getStringValueNc(_FI_head); }

  /* setter for head * */
  public void setHead(String v) {
    _setStringValueNfc(_getFeatFromAdjOffset(_FI_head, false), v);
  }
  
//  public void _setHeadNcNj(String v) {_FI_head = v;};

  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public StringList getTail() { return (StringList) _getFeatureValueNc(_FI_tail); }

  /* setter for tail * */
  public void setTail(StringList v) { _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_tail, false), v); }
  
  public void setTail(CommonList v) {
    setTail((StringList)v);
  }
  
//  public void _setTailNcNj(StringList v) { _FI_tail = v; }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#get_headAsString()
   */
  @Override
  public String get_headAsString() {
    return ((NonEmptyStringList)this).getHead();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#set_headFromString(java.lang.String)
   */
  @Override
  public void set_headFromString(String v) {
    setHead(v);
  }
  
}
