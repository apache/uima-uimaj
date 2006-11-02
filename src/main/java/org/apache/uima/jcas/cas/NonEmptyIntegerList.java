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

public class NonEmptyIntegerList extends IntegerList {

  public final static int typeIndexID = JCas.getNextIndex();
  public final static int type = typeIndexID;
  public           int getTypeIndexID() {return typeIndexID;}

  // Never called.  Disable default constructor
  protected NonEmptyIntegerList() {}

  /** Internal - Constructor used by generator */
  public NonEmptyIntegerList(int addr, TOP_Type type) {
    super(addr, type);
  }

  public NonEmptyIntegerList(JCas jcas) {
    super(jcas);
  }

  //*------------------*
  //* Feature: head
  /** getter for head  * */
  public int getHead() {
    if (NonEmptyIntegerList_Type.featOkTst && ((NonEmptyIntegerList_Type)jcasType).casFeat_head == null)
          JCas.throwFeatMissing("head", "uima.cas.NonEmptyIntegerList");
    return jcasType.ll_cas.ll_getIntValue(addr, ((NonEmptyIntegerList_Type)jcasType).casFeatCode_head);}

  /** setter for head  * */
  public void setHead(int v) {
    if (NonEmptyIntegerList_Type.featOkTst && ((NonEmptyIntegerList_Type)jcasType).casFeat_head == null)
          JCas.throwFeatMissing("head", "uima.cas.NonEmptyIntegerList");
    jcasType.ll_cas.ll_setIntValue(addr, ((NonEmptyIntegerList_Type)jcasType).casFeatCode_head, v);}
  //*------------------*
  //* Feature: tail
  /** getter for tail  * */
  public IntegerList getTail() {
    if (NonEmptyIntegerList_Type.featOkTst && ((NonEmptyIntegerList_Type)jcasType).casFeat_tail == null)
          JCas.throwFeatMissing("tail", "uima.cas.NonEmptyIntegerList");
    return (IntegerList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((NonEmptyIntegerList_Type)jcasType).casFeatCode_tail)));}

  /** setter for tail  * */
  public void setTail(IntegerList v) {
    if (NonEmptyIntegerList_Type.featOkTst && ((NonEmptyIntegerList_Type)jcasType).casFeat_tail == null)
          JCas.throwFeatMissing("tail", "uima.cas.NonEmptyIntegerList");
    jcasType.ll_cas.ll_setRefValue(addr, ((NonEmptyIntegerList_Type)jcasType).casFeatCode_tail, jcasType.ll_cas.ll_getFSRef(v));}
}
