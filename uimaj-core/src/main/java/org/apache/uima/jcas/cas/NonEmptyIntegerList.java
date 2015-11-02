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

import org.apache.uima.cas.CAS;
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
  
  // Offsets to use in accessing features
  //   For built-ins, these are static constants
  
  private final static int head_featCode;
  private final static int tail_featCode;
  
  static {
    TypeSystemImpl tsi = TypeSystemImpl.staticTsi;
    TypeImpl listType = tsi.getType(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST);
    
    // makes assumption that all uima lists have the same head/tail offsets
    head_featCode = listType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD).getCode();
    tail_featCode = listType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL).getCode();
  }

  /* local data */
  private int _F_head;
  private IntegerList _F_tail;
  
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
  public int getHead() { return _F_head; }

  /* setter for head * */
  public void setHead(int v) {
    _F_head = v;  
    // no corruption check - can't be a key
    _casView.maybeLogUpdate(this, head_featCode);
  }
  
  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public IntegerList getTail() { return _F_tail; }

  /* setter for tail * */
  public void setTail(CommonList v) {
    assert(v instanceof IntegerList);
    _F_tail = (IntegerList) v;
    // no corruption check - can't be a key
    _casView.maybeLogUpdate(this, tail_featCode);
  }
  
  public void setHead(List<String> stringValues, int i) {
    setHead(Integer.parseInt(stringValues.get(i)));
  }

}
