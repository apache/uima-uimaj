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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.AnnotationBase;

/**
 * the JCas class model for the CAS type uima.cas.Annotation. It defines two integer valued features
 * indicating the begin and end of the span being annotated. There is also a method to retrieve the
 * spanned text as a string.
 */
public class Annotation extends AnnotationBase implements AnnotationFS {

  public final static int typeIndexID = JCasRegistry.register(Annotation.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }
  
  public final static int _FI_begin = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_end = JCasRegistry.registerFeature(typeIndexID);
  
  /* local data */
  private int _F_begin;
  private int _F_end;

  // Never called. Disable default constructor
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
  public int getBegin() { return _F_begin; }

  /*
   * setter for begin - sets beginning of span of annotation
   */
  public void setBegin(int v) { 
    _casView.setWithCheckAndJournalJFRI(this, _FI_begin , () -> _F_begin = v);
  }

  // *------------------*
  // * Feature: end
  // * ending of span of annotation

  /*
   * getter for end - gets ending of span of annotation
   */
  public int getEnd() { return _F_end; }

  /*
   * setter for end - sets ending of span of annotation
   */
  public void setEnd(int v) {
    _casView.setWithCheckAndJournalJFRI(this,  _FI_end,  () -> _F_end = v);
  }

  /**
   * Constructor with begin and end passed as arguments
   * @param jcas JCas
   * @param begin begin offset
   * @param end   end offset
   */
  public Annotation(JCas jcas, int begin, int end) {
    this(jcas); // forward to constructor
    this.setBegin(begin);
    this.setEnd(end);
  }

  /**
   * @see org.apache.uima.cas.text.AnnotationFS#getCoveredText()
   */
  public String getCoveredText() {

    final String text = _casView.getDocumentText();
    if (text == null) {
      return null;
    }
    return text.substring(getBegin(), getEnd());
  }

  /**
   * @deprecated
   * @see Annotation#getBegin()
   * @return the Annotation "begin" feature value
   */
  @Deprecated
  public int getStart() {
    return getBegin();
  }
  
}
