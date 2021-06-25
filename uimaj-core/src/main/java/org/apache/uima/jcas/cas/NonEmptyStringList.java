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

public class NonEmptyStringList extends StringList implements Iterable<String>, NonEmptyList {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_NON_EMPTY_STRING_LIST;

  public final static int typeIndexID = JCasRegistry.register(NonEmptyStringList.class);

  public final static int type = typeIndexID;

  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  public static final String _FeatName_head = "head";
  public static final String _FeatName_tail = "tail";

  // public static final int _FI_head = TypeSystemImpl.getAdjustedFeatureOffset("head");
  // public static final int _FI_tail = TypeSystemImpl.getAdjustedFeatureOffset("tail");
  private final static CallSite _FC_head = TypeSystemImpl
          .createCallSiteForBuiltIn(NonEmptyStringList.class, "head");
  private final static MethodHandle _FH_head = _FC_head.dynamicInvoker();
  private final static CallSite _FC_tail = TypeSystemImpl
          .createCallSiteForBuiltIn(NonEmptyStringList.class, "tail");
  private final static MethodHandle _FH_tail = _FC_tail.dynamicInvoker();

  // /* local data */
  // private String _F_head;
  // private StringList _F_tail;

  // Never called. Disable default constructor
  protected NonEmptyStringList() {
  }

  public NonEmptyStringList(JCas jcas) {
    super(jcas);
  }

  /**
   * used by generator Make a new AnnotationBase
   * 
   * @param c
   *          -
   * @param t
   *          -
   */

  public NonEmptyStringList(TypeImpl t, CASImpl c) {
    super(t, c);
  }

  /**
   * Generate a NonEmpty node with the specified head and tail
   * 
   * @param jcas
   *          -
   * @param head
   *          -
   * @param tail
   *          -
   */
  public NonEmptyStringList(JCas jcas, String head, StringList tail) {
    this(jcas);
    setHead(head);
    setTail(tail);
  }

  /**
   * Generate a NonEmpty node with the specified head with the empty node as the tail
   * 
   * @param jcas
   *          -
   * @param head
   *          -
   */
  public NonEmptyStringList(JCas jcas, String head) {
    this(jcas, head, jcas.getCasImpl().emptyStringList());
  }

  // *------------------*
  // * Feature: head
  /* getter for head * */
  public String getHead() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_head));
  }

  /* setter for head * */
  public void setHead(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_head), v);
  }

  // public void _setHeadNcNj(String v) {wrapGetIntCatchException(_FH_head) = v;};

  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public StringList getTail() {
    return (StringList) _getFeatureValueNc(wrapGetIntCatchException(_FH_tail));
  }

  /* setter for tail * */
  public void setTail(StringList v) {
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /**
       * Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list
       * element in a different CAS {2}.
       */
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView,
              _casView);
    }
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_tail), v);
  }

  @Override
  public void setTail(CommonList v) {
    setTail((StringList) v);
  }

  // public void _setTailNcNj(StringList v) { wrapGetIntCatchException(_FH_tail) = v; }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.cas.CommonList#get_headAsString()
   */
  @Override
  public String get_headAsString() {
    return ((NonEmptyStringList) this).getHead();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.cas.CommonList#set_headFromString(java.lang.String)
   */
  @Override
  public void set_headFromString(String v) {
    setHead(v);
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
        String element = nn.getHead();
        node = nn.getTail();
        return element;
      }

    };
  }
}
