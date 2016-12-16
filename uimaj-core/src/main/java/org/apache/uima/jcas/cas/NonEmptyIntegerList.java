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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyIntegerList extends IntegerList implements Iterable<Integer> {

  public final static int typeIndexID = JCasRegistry.register(NonEmptyIntegerList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected NonEmptyIntegerList() {
  }

 /* Internal - Constructor used by generator */
  public NonEmptyIntegerList(int addr, TOP_Type type) {
    super(addr, type);
  }

  public NonEmptyIntegerList(JCas jcas) {
    super(jcas);
  }
  
  /**
   * @param jcas the jcas to create this Feature Structure in
   * @param i the head value
   * @param tail the tail
   */
  public NonEmptyIntegerList(JCas jcas, int i, IntegerList tail) {
    this(jcas);
    setHead(i);
    setTail(tail);
  }

  // *------------------*
  // * Feature: head
  /* getter for head * */
  public int getHead() {
    if (NonEmptyIntegerList_Type.featOkTst
            && ((NonEmptyIntegerList_Type) jcasType).casFeat_head == null)
      this.jcasType.jcas.throwFeatMissing("head", "uima.cas.NonEmptyIntegerList");
    return jcasType.ll_cas.ll_getIntValue(addr,
            ((NonEmptyIntegerList_Type) jcasType).casFeatCode_head);
  }

  /* setter for head * */
  public void setHead(int v) {
    if (NonEmptyIntegerList_Type.featOkTst
            && ((NonEmptyIntegerList_Type) jcasType).casFeat_head == null)
      this.jcasType.jcas.throwFeatMissing("head", "uima.cas.NonEmptyIntegerList");
    jcasType.ll_cas.ll_setIntValue(addr, ((NonEmptyIntegerList_Type) jcasType).casFeatCode_head, v);
  }

  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public IntegerList getTail() {
    if (NonEmptyIntegerList_Type.featOkTst
            && ((NonEmptyIntegerList_Type) jcasType).casFeat_tail == null)
      this.jcasType.jcas.throwFeatMissing("tail", "uima.cas.NonEmptyIntegerList");
    return (IntegerList) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((NonEmptyIntegerList_Type) jcasType).casFeatCode_tail)));
  }

  /* setter for tail * */
  public void setTail(IntegerList v) {
    if (NonEmptyIntegerList_Type.featOkTst
            && ((NonEmptyIntegerList_Type) jcasType).casFeat_tail == null)
      this.jcasType.jcas.throwFeatMissing("tail", "uima.cas.NonEmptyIntegerList");
    jcasType.ll_cas.ll_setRefValue(addr, ((NonEmptyIntegerList_Type) jcasType).casFeatCode_tail,
            jcasType.ll_cas.ll_getFSRef(v));
  }
  
  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {

      IntegerList node = NonEmptyIntegerList.this;
      
      @Override
      public boolean hasNext() {
        return node instanceof NonEmptyIntegerList;
      }

      @Override
      public Integer next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        NonEmptyIntegerList nn = (NonEmptyIntegerList) node;
        Integer r = nn.getHead();
        node = nn.getTail();
        return r;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
      
  }

}
