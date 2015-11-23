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
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyFloatList extends FloatList implements NonEmptyList {

  public final static int typeIndexID = JCasRegistry.register(NonEmptyFloatList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  public static final int _FI_head = JCasRegistry.registerFeature(typeIndexID);
  public static final int _FI_tail = JCasRegistry.registerFeature(typeIndexID);
  
  /* local data */
  private float _F_head;
  private FloatList _F_tail;
  
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

  // *------------------*
  // * Feature: head
  /* getter for head * */
  public float getHead() { return _F_head; }

  /* setter for head * */
  public void setHead(float v) {
    _F_head = v;  
    // no corruption check - a list element can't be a key
    _casView.maybeLogUpdateJFRI(this, _FI_head);
  }

  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public FloatList getTail() { return _F_tail; }

  /* setter for tail * */
  public void setTail(FloatList v) {
    _F_tail = v;
    // no corruption check - can't be a key
    _casView.maybeLogUpdateJFRI(this, _FI_tail);
  }

  public void setTail(CommonList v) {
    setTail((FloatList) v);
  }
  
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
    
}
