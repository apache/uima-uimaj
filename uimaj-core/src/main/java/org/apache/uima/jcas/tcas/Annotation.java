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

package org.apache.uima.jcas.tcas;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.util.function.IntPredicate;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.impl.AnnotationImpl;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.AnnotationBase;

/**
 * the JCas class model for the CAS type uima.cas.Annotation. It defines two integer valued features
 * indicating the begin and end of the span being annotated. There is also a method to retrieve the
 * spanned text as a string.
 */
public class Annotation extends AnnotationBase implements AnnotationImpl {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_ANNOTATION;
  public final static String _FeatName_begin = "begin";
  public final static String _FeatName_end = "end";

  public final static int typeIndexID = JCasRegistry.register(Annotation.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }
  
  private final static CallSite _FC_begin = TypeSystemImpl.createCallSite(Annotation.class, "begin");
  private final static MethodHandle _FH_begin = _FC_begin.dynamicInvoker();
  private final static CallSite _FC_end = TypeSystemImpl.createCallSite(Annotation.class, "end");
  private final static MethodHandle _FH_end = _FC_end.dynamicInvoker();
  
  // hard code for performance, and because is likely to be right
//  private final static int BEGIN_OFFSET = 0;
//  private final static int END_OFFSET = 1;
  
//  static {
//    _FC_begin.setTarget(MethodHandles.constant(int.class, TypeSystemImpl.getAdjustedFeatureOffset("begin")));
//  }
  
  
//  private final static int _FI_begin = TypeSystemImpl.getAdjustedFeatureOffset("begin");
//  private final static int _FI_end   = TypeSystemImpl.getAdjustedFeatureOffset("end");
  
//  /* local data */
//  private int _F_begin;
//  private int _F_end;

  // Never called. Disable default constructor
  @Deprecated
  @SuppressWarnings("deprecation")
  protected Annotation() {
  }

  public Annotation(JCas jcas) {
    super(jcas);
  }
  
  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public Annotation(TypeImpl t, CASImpl c) {
    super(t, c);
  }


  // *------------------*
  // * Feature: begin
  // * beginning of span of annotation
  /*
   * getter for begin - gets beginning of span of annotation
   */
//  public int getBegin() { return _F_begin; }
  public final int getBegin() { 
    try {
      return _getIntValueNc((int)_FH_begin.invokeExact());
    } catch (Throwable e) {
      throw new RuntimeException(e);  // never happen
    }
//  public final int getBegin() { return _getIntValueNc(BEGIN_OFFSET); //wrapGetIntCatchException(_FH_begin)); 
  }
  
  /*
   * setter for begin - sets beginning of span of annotation
   */
//  public final void setBegin(int v) { _setIntValueNfcCJ(BEGIN_OFFSET, v);
  public final void setBegin(int v) {
    try {
      _setIntValueNfcCJ((int)_FH_begin.invokeExact(), v);
    } catch (Throwable e) {
      throw new RuntimeException(e);  // never happen
    }
  }
  
  // *------------------*
  // * Feature: end
  // * ending of span of annotation

  /*
   * getter for end - gets ending of span of annotation
   */
  public final int getEnd() {
    try {
      return this._getIntValueNc((int) _FH_end.invokeExact());
    } catch (Throwable e) {
      throw new RuntimeException(e);  // never happen
    }  
//    return this._getIntValueNc(END_OFFSET);
  }
  
  /*
   * setter for end - sets ending of span of annotation
   */
  public final void setEnd(int v) {
    try {
      this._setIntValueNfc((int) _FH_end.invokeExact(),  v);
    } catch (Throwable e) {
      throw new RuntimeException(e);  // never happen
    }
//    this._setIntValueNfc(END_OFFSET,  v);
  }
  
  /**
   * Constructor with begin and end passed as arguments
   * @param jcas JCas
   * @param begin begin offset
   * @param end   end offset
   */
  public Annotation(JCas jcas, int begin, int end) {
    super(jcas); // forward to constructor
    this.setBegin(begin);
    this.setEnd(end);
  }

  /**
   * @see org.apache.uima.cas.text.AnnotationFS#getCoveredText()
   * @return -
   */
  public String getCoveredText() {

    final String text = _casView.getDocumentText();
    if (text == null) {
      return null;
    }
    return text.substring(getBegin(), getEnd());
  }

  /**
   * @deprecated Use {@link #getBegin} instead.
   * @return the Annotation "begin" feature value
   */
  @Deprecated
  public int getStart() {
    return getBegin();
  }
  
  /**
   * Compare two annotations, no type order
   * @param other -
   * @return -
   */
  public final int compareAnnotation(Annotation other) {
    try {
      final int b = (int) _FH_begin.invokeExact();
      int result = Integer.compare(_getIntValueNc(b), other._getIntValueNc(b));
  //    int result = Integer.compare(_getIntValueNc(BEGIN_OFFSET), other._getIntValueNc(BEGIN_OFFSET));
      if (result != 0) return result;
  
      final int e = (int) _FH_end.invokeExact();
      result = Integer.compare(_getIntValueNc(e), other._getIntValueNc(e));
  //    result = Integer.compare(_getIntValueNc(END_OFFSET), other._getIntValueNc(END_OFFSET));
      return (result == 0) ? 0 : -result;  // reverse compare on "end" value
    } catch (Throwable e) {
      throw new RuntimeException(e);  // never happen
    }
  }
  
  /**
   * Compare two annotations incl type order
   * @param other -
   * @param lto -
   * @return -
   */
  public final int compareAnnotation(Annotation other, LinearTypeOrder lto) {
    int result = compareAnnotation(other);
    if (result != 0) return result;
    
    return lto.compare(this, other);
  }


  /**
   * Compare two annotations, with id compare
   * @param other -
   * @return -
   */
  public final int compareAnnotationWithId(Annotation other) {
    int result = compareAnnotation(other);
    if (result != 0) return result;    
    return Integer.compare(_id,  other._id);
  }
  
  /**
   * Compare two annotations, with type order, with id compare
   * @param other -
   * @param lto -
   * @return -
   */
  public final int compareAnnotationWithId(Annotation other, LinearTypeOrder lto) {
    int result = compareAnnotation(other, lto);
    if (result != 0) return result;    
    return Integer.compare(_id,  other._id);
  }

  @Override
  public void trim(IntPredicate aIsTrimChar) {
    int begin = getBegin();
    int end = getEnd();
    String text = _casView.getDocumentText();
      
    // If the span is empty, or there is no text, there is nothing to trim
    if (begin == end || text == null) {
      return;
    }
    
    final int saved_begin = begin;
    final int saved_end = end;
    // First we trim at the end. If a trimmed span is empty, we want to return the original 
    // begin as the begin/end of the trimmed span
    int backwardsSeekingCodepoint;
    while (
              (end > 0)
              && end > begin
              && aIsTrimChar.test(backwardsSeekingCodepoint = text.codePointBefore(end))
    ) {
      end -= Character.charCount(backwardsSeekingCodepoint);
    }
    
    // Then, trim at the start
    int forwardSeekingCodepoint;
    while (
              (begin < (text.length() - 1))
              && begin < end
              && aIsTrimChar.test(forwardSeekingCodepoint = text.codePointAt(begin))
    ) {
      begin += Character.charCount(forwardSeekingCodepoint);
    }
      
    if (saved_begin != begin || saved_end != end) {
      int final_begin = begin;
      int final_end = end;
    
       _casView.protectIndexes(() -> {
          setBegin(final_begin);
          setEnd(final_end);
       });
    }
  }
}
