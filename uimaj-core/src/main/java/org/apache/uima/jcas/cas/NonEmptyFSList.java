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

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyFSList<T extends TOP> extends FSList<T> implements NonEmptyList {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_NON_EMPTY_FS_LIST;

  public final static int typeIndexID = JCasRegistry.register(NonEmptyFSList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  public static final String _FeatName_head = "head";
  public static final String _FeatName_tail = "tail";
  
//  public static final int _FI_head = TypeSystemImpl.getAdjustedFeatureOffset("head");
//  public static final int _FI_tail = TypeSystemImpl.getAdjustedFeatureOffset("tail");
  private final static CallSite _FC_head = TypeSystemImpl.createCallSiteForBuiltIn(NonEmptyFSList.class, "head");
  private final static MethodHandle _FH_head = _FC_head.dynamicInvoker();
  private final static CallSite _FC_tail = TypeSystemImpl.createCallSiteForBuiltIn(NonEmptyFSList.class, "tail");
  private final static MethodHandle _FH_tail = _FC_tail.dynamicInvoker();
  
  
//  /* local data */
//  private TOP _F_head;
//  private FSList _F_tail;
  
  // might be called to produce removed marker
  public NonEmptyFSList() {
    super();
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
  public NonEmptyFSList(JCas jcas, T head, FSList<?> tail) {
    this(jcas);
    setHead(head);
    setTail(tail);
  }

  /**
   * Generate a NonEmpty node with the specified head with the empty node as the tail
   * @param jcas -
   * @param head -
   */
  public NonEmptyFSList(JCas jcas, T head) {
    this(jcas, head, jcas.getCasImpl().emptyFSList());
  }
  
  // *------------------*
  // * Feature: head
  // return type is TOP for backwards compatibility with v2
  /* getter for head * */
  public T getHead() { return (T) _getFeatureValueNc(wrapGetIntCatchException(_FH_head)); }

  /* setter for head * */
  // arg type is TOP for backwards compatibility with v2
  public void setHead(T vt) {
    if (vt != null && _casView.getBaseCAS() != vt._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, vt, vt._casView, _casView);
    }
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_head), vt); }

//  public void _setHeadNcNj(TOP v) { _F_head = v; }
  
  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public FSList<T> getTail() { return (FSList<T>) _getFeatureValueNc(wrapGetIntCatchException(_FH_tail)); }

  /* setter for tail * */
  public void setTail(FSList v) {
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView, _casView);
    }
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_tail), v); }
   
  @Override
  public void setTail(CommonList v) {
    setTail((FSList) v);
  }
  
  public T getNthElement(int i) {
    return ((NonEmptyFSList<T>)getNonEmptyNthNode(i)).getHead();
  }

  /**
   * inserts the new item as a new NonEmpty FSList node following this item
   * @param item to be inserted
   * @return the NonEmptyFSList node created  
   */
  public NonEmptyFSList<T> add(T item) {
    FSList<T> tail = getTail();
    NonEmptyFSList<T> node = tail.push(item);
    setTail(node);
    return node;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {

      FSList<T> node = NonEmptyFSList.this;
      
      @Override
      public boolean hasNext() {
        return node instanceof NonEmptyFSList;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        NonEmptyFSList<T> nn = (NonEmptyFSList<T>)node; 
        T element = nn.getHead();
        node = nn.getTail();
        return element;
      }
      
    };
  }
}
