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

public class NonEmptyFSList extends FSList {

  public final static int typeIndexID = JCasRegistry.register(NonEmptyFSList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected NonEmptyFSList() {
  }

 /* Internal - Constructor used by generator */
  public NonEmptyFSList(int addr, TOP_Type type) {
    super(addr, type);
  }

  public NonEmptyFSList(JCas jcas) {
    super(jcas);
  }
  
  /**
   * @param jcas the JCas create the new Feature Structure in
   * @param item the head item
   * @param tail the tail item
   */
  public NonEmptyFSList(JCas jcas, TOP item, FSList tail) {
    this(jcas);
    setHead(item);
    setTail(tail); 
  }

  // *------------------*
  // * Feature: head
  /* getter for head * */
  public org.apache.uima.jcas.cas.TOP getHead() {
    if (NonEmptyFSList_Type.featOkTst && ((NonEmptyFSList_Type) jcasType).casFeat_head == null)
      this.jcasType.jcas.throwFeatMissing("head", "uima.cas.NonEmptyFSList");
    return (org.apache.uima.jcas.cas.TOP) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas
            .ll_getRefValue(addr, ((NonEmptyFSList_Type) jcasType).casFeatCode_head)));
  }

  /* setter for head * */
  public void setHead(org.apache.uima.jcas.cas.TOP v) {
    if (NonEmptyFSList_Type.featOkTst && ((NonEmptyFSList_Type) jcasType).casFeat_head == null)
      this.jcasType.jcas.throwFeatMissing("head", "uima.cas.NonEmptyFSList");
    jcasType.ll_cas.ll_setRefValue(addr, ((NonEmptyFSList_Type) jcasType).casFeatCode_head,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public FSList getTail() {
    if (NonEmptyFSList_Type.featOkTst && ((NonEmptyFSList_Type) jcasType).casFeat_tail == null)
      this.jcasType.jcas.throwFeatMissing("tail", "uima.cas.NonEmptyFSList");
    return (FSList) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((NonEmptyFSList_Type) jcasType).casFeatCode_tail)));
  }

  /* setter for tail * */
  public void setTail(FSList v) {
    if (NonEmptyFSList_Type.featOkTst && ((NonEmptyFSList_Type) jcasType).casFeat_tail == null)
      this.jcasType.jcas.throwFeatMissing("tail", "uima.cas.NonEmptyFSList");
    jcasType.ll_cas.ll_setRefValue(addr, ((NonEmptyFSList_Type) jcasType).casFeatCode_tail,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  @Override
  public Iterator<TOP> iterator() {
    return new Iterator<TOP>() {

      FSList node = NonEmptyFSList.this;
      
      @Override
      public boolean hasNext() {
        return node instanceof NonEmptyFSList;
      }

      @Override
      public TOP next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        NonEmptyFSList nn = (NonEmptyFSList) node;
        TOP r = nn.getHead();
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
