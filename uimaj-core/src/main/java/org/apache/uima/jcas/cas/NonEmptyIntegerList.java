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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfInt;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyIntegerList extends IntegerList implements NonEmptyList {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST;

  public final static int typeIndexID = JCasRegistry.register(NonEmptyIntegerList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }
  
  public static final String _FeatName_head = "head";
  public static final String _FeatName_tail = "tail";

//  public final static int _FI_head = TypeSystemImpl.getAdjustedFeatureOffset("head");
//  public final static int _FI_tail = TypeSystemImpl.getAdjustedFeatureOffset("tail");
  private final static CallSite _FC_head = TypeSystemImpl.createCallSiteForBuiltIn(NonEmptyIntegerList.class, "head");
  private final static MethodHandle _FH_head = _FC_head.dynamicInvoker();
  private final static CallSite _FC_tail = TypeSystemImpl.createCallSiteForBuiltIn(NonEmptyIntegerList.class, "tail");
  private final static MethodHandle _FH_tail = _FC_tail.dynamicInvoker();


//  /* local data */
//  private int _F_head;
//  private IntegerList _F_tail;
  
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

  /**
   * Generate a NonEmpty node with the specified head and tail
   * @param jcas -
   * @param v -
   * @param tail -
   */
  public NonEmptyIntegerList(JCas jcas, int v, IntegerList tail) {
    this(jcas);
    setHead(v);
    setTail(tail);
  }
  
  /**
   * Generate a NonEmpty node with the specified head with the empty node as the tail
   * @param jcas -
   * @param v -
   */
  public NonEmptyIntegerList(JCas jcas, int v) {
    this(jcas, v, jcas.getCasImpl().emptyIntegerList());
  }
  
  // *------------------*
  // * Feature: head
  /* getter for head * */
  public int getHead() { return _getIntValueNc(wrapGetIntCatchException(_FH_head)); }

  /* setter for head * */
  public void setHead(int v) {
    _setIntValueNfc(wrapGetIntCatchException(_FH_head), v);
  }

//  public void _setHeadNcNj(int v) { wrapGetIntCatchException(_FH_head) = v;}
  
  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public IntegerList getTail() { return (IntegerList) _getFeatureValueNc(wrapGetIntCatchException(_FH_tail)); }

  /* setter for tail * */
  public void setTail(IntegerList v) {
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView, _casView);
    }
  _setFeatureValueNcWj(wrapGetIntCatchException(_FH_tail), v); 
  }
  
  @Override
  public void setTail(CommonList v) {setTail((IntegerList)v);}
    
  public void setHead(List<String> stringValues, int i) {
    setHead(Integer.parseInt(stringValues.get(i)));
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#get_headAsString()
   */
  @Override
  public String get_headAsString() {
    return Integer.toString(((NonEmptyIntegerList)this).getHead());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#set_headFromString(java.lang.String)
   */
  @Override
  public void set_headFromString(String v) {
    setHead(Integer.parseInt(v));
  }  
  
  @Override
  public OfInt iterator() {
    return new OfInt() {

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
        NonEmptyIntegerList nn = (NonEmptyIntegerList)node; 
        Integer element = nn.getHead();
        node = nn.getTail();
        return element;
      }

      @Override
      public int nextInt() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        NonEmptyIntegerList nn = (NonEmptyIntegerList)node; 
        int element = nn.getHead();
        node = nn.getTail();
        return element;
      }
      
    };
  }
}
