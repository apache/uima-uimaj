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

public class NonEmptyStringList extends StringList {

  public final static int typeIndexID = JCasRegistry.register(NonEmptyStringList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected NonEmptyStringList() {
  }

 /* Internal - Constructor used by generator */
  public NonEmptyStringList(int addr, TOP_Type type) {
    super(addr, type);
  }

  public NonEmptyStringList(JCas jcas) {
    super(jcas);
  }
  
  /**
   * @param jcas the JCas create the new Feature Structure in
   * @param s the head item
   * @param tail the tail item
   */
  public NonEmptyStringList(JCas jcas, String s, StringList tail) {
    this(jcas);
    setHead(s);
    setTail(tail);
  }

  // *------------------*
  // * Feature: head
  /* getter for head * */
  public String getHead() {
    if (NonEmptyStringList_Type.featOkTst
            && ((NonEmptyStringList_Type) jcasType).casFeat_head == null)
      this.jcasType.jcas.throwFeatMissing("head", "uima.cas.NonEmptyStringList");
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((NonEmptyStringList_Type) jcasType).casFeatCode_head);
  }

  /* setter for head * */
  public void setHead(String v) {
    if (NonEmptyStringList_Type.featOkTst
            && ((NonEmptyStringList_Type) jcasType).casFeat_head == null)
      this.jcasType.jcas.throwFeatMissing("head", "uima.cas.NonEmptyStringList");
    jcasType.ll_cas.ll_setStringValue(addr, ((NonEmptyStringList_Type) jcasType).casFeatCode_head,
            v);
  }

  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public StringList getTail() {
    if (NonEmptyStringList_Type.featOkTst
            && ((NonEmptyStringList_Type) jcasType).casFeat_tail == null)
      this.jcasType.jcas.throwFeatMissing("tail", "uima.cas.NonEmptyStringList");
    return (StringList) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((NonEmptyStringList_Type) jcasType).casFeatCode_tail)));
  }

  /* setter for tail * */
  public void setTail(StringList v) {
    if (NonEmptyStringList_Type.featOkTst
            && ((NonEmptyStringList_Type) jcasType).casFeat_tail == null)
      this.jcasType.jcas.throwFeatMissing("tail", "uima.cas.NonEmptyStringList");
    jcasType.ll_cas.ll_setRefValue(addr, ((NonEmptyStringList_Type) jcasType).casFeatCode_tail,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  @Override
  public Iterator<String> iterator() {
    return new Iterator<String>() {

      StringList node = NonEmptyStringList.this;
      
      @Override
      public boolean hasNext() {
        return node instanceof NonEmptyStringList;
      }

      @Override
      public String next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        NonEmptyStringList nn = (NonEmptyStringList) node;
        String r = nn.getHead();
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
