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

import org.apache.uima.jcas.impl.JCas;

public class NonEmptyFloatList extends FloatList {

  public final static int typeIndexID = JCas.getNextIndex();
  public final static int type = typeIndexID;
  public           int getTypeIndexID() {return typeIndexID;}

  // Never called.  Disable default constructor
  protected NonEmptyFloatList() {}

  /** Internal - Constructor used by generator */
  public NonEmptyFloatList(int addr, TOP_Type type) {
    super(addr, type);
  }

  public NonEmptyFloatList(JCas jcas) {
    super(jcas);
  }

  //*------------------*
  //* Feature: head
  /** getter for head  * */
  public float getHead() {
    if (NonEmptyFloatList_Type.featOkTst && ((NonEmptyFloatList_Type)jcasType).casFeat_head == null)
          JCas.throwFeatMissing("head", "uima.cas.NonEmptyFloatList");
    return jcasType.ll_cas.ll_getFloatValue(addr, ((NonEmptyFloatList_Type)jcasType).casFeatCode_head);}

  /** setter for head  * */
  public void setHead(float v) {
    if (NonEmptyFloatList_Type.featOkTst && ((NonEmptyFloatList_Type)jcasType).casFeat_head == null)
          JCas.throwFeatMissing("head", "uima.cas.NonEmptyFloatList");
    jcasType.ll_cas.ll_setFloatValue(addr, ((NonEmptyFloatList_Type)jcasType).casFeatCode_head, v);}
  //*------------------*
  //* Feature: tail
  /** getter for tail  * */
  public FloatList getTail() {
    if (NonEmptyFloatList_Type.featOkTst && ((NonEmptyFloatList_Type)jcasType).casFeat_tail == null)
          JCas.throwFeatMissing("tail", "uima.cas.NonEmptyFloatList");
    return (FloatList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((NonEmptyFloatList_Type)jcasType).casFeatCode_tail)));}

  /** setter for tail  * */
  public void setTail(FloatList v) {
    if (NonEmptyFloatList_Type.featOkTst && ((NonEmptyFloatList_Type)jcasType).casFeat_tail == null)
          JCas.throwFeatMissing("tail", "uima.cas.NonEmptyFloatList");
    jcasType.ll_cas.ll_setRefValue(addr, ((NonEmptyFloatList_Type)jcasType).casFeatCode_tail, jcasType.ll_cas.ll_getFSRef(v));}
}
