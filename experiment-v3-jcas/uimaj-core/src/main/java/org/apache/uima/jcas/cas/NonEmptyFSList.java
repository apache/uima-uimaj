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
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyFSList extends FSList implements Iterable<TOP>, NonEmptyList {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = "org.apache.uima.jcas.cas.NonEmptyFSList";

  public final static int typeIndexID = JCasRegistry.register(NonEmptyFSList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  public static final int _FI_head = TypeSystemImpl.getAdjustedFeatureOffset("head");
  public static final int _FI_tail = TypeSystemImpl.getAdjustedFeatureOffset("tail");

//  /* local data */
//  private TOP _F_head;
//  private FSList _F_tail;
  
  // Never called. Disable default constructor
  protected NonEmptyFSList() {
  }

  public NonEmptyFSList(JCas jcas) {
    super(jcas);
  }

  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public NonEmptyFSList(TypeImpl t, CASImpl c) {
    super(t, c);
  }
  
  /**
   * Generate a NonEmpty node with the specified head and tail
   * @param jcas -
   * @param head -
   * @param tail -
   */
  public NonEmptyFSList(JCas jcas, TOP head, CommonList tail) {
    this(jcas);
    setHead(head);
    setTail(tail);
  }

  /**
   * Generate a NonEmpty node with the specified head with the empty node as the tail
   * @param jcas -
   * @param head -
   */
  public NonEmptyFSList(JCas jcas, TOP head) {
    this(jcas, head, jcas.getCasImpl().getEmptyFSList());
  }
  
  // *------------------*
  // * Feature: head
  /* getter for head * */
  public TOP getHead() { return _getFeatureValueNc(_FI_head); }

  /* setter for head * */
  public void setHead(FeatureStructure v) {
    TOP vt = (TOP) v;
    if (vt != null && _casView.getBaseCAS() != vt._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, vt, vt._casView, _casView);
    }
    _setFeatureValueNcWj(_FI_head, v); }

//  public void _setHeadNcNj(TOP v) { _F_head = v; }
  
  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public FSList getTail() { return (FSList) _getFeatureValueNc(_FI_tail); }

  /* setter for tail * */
  public void setTail(FSList v) {
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView, _casView);
    }
    _setFeatureValueNcWj(_FI_tail, v); }
   
  @Override
  public void setTail(CommonList v) {
    setTail((FSList) v);
  }
  
  public TOP getNthElement(int i) {
    return ((NonEmptyFSList)getNonEmptyNthNode(i)).getHead();
  }

  /**
   * inserts the new item as a new NonEmpty FSList node following this item
   * @param item to be inserted
   * @return the NonEmptyFSList node created  
   */
  public NonEmptyFSList add(FeatureStructure item) {
    FSList tail = getTail();
    NonEmptyFSList node = tail.push((TOP) item);
    setTail(node);
    return node;
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
        NonEmptyFSList nn = (NonEmptyFSList)node; 
        TOP element = nn.getHead();
        node = nn.getTail();
        return element;
      }
      
    };
  }
}
