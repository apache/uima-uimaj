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
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.TOP_Type;

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

  // Never called. Disable default constructor
  protected Annotation() {
  }

 /* Internal - Constructor used by generator */
  public Annotation(int addr, TOP_Type type) {
    super(addr, type);
  }

  public Annotation(JCas jcas) {
    super(jcas);
  }

  // *------------------*
  // * Feature: begin
  // * beginning of span of annotation
  /*
   * getter for begin - gets beginning of span of annotation
   */
  public int getBegin() {
    // not needed - is built in
//    if (Annotation_Type.featOkTst && ((Annotation_Type) jcasType).casFeat_begin == null)
//      this.jcasType.jcas.throwFeatMissing("begin", "uima.tcas.Annotation");
    return ((Annotation_Type)jcasType).getBegin(addr);
//    return jcasType.ll_cas.ll_getIntValue(addr, ((Annotation_Type) jcasType).casFeatCode_begin);
  }

  /*
   * setter for begin - sets beginning of span of annotation
   */
  public void setBegin(int v) {
    // not needed - is built in
//    if (Annotation_Type.featOkTst && ((Annotation_Type) jcasType).casFeat_begin == null)
//      this.jcasType.jcas.throwFeatMissing("begin", "uima.tcas.Annotation");
    jcasType.ll_cas.ll_setIntValue(addr, ((Annotation_Type) jcasType).casFeatCode_begin, v);
  }

  // *------------------*
  // * Feature: end
  // * ending of span of annotation

  /*
   * getter for end - gets ending of span of annotation
   */
  public int getEnd() {
    // not needed - is built in
//    if (Annotation_Type.featOkTst && ((Annotation_Type) jcasType).casFeat_end == null)
//      this.jcasType.jcas.throwFeatMissing("end", "uima.tcas.Annotation");
    return ((Annotation_Type)jcasType).getEnd(addr);
//    return jcasType.ll_cas.ll_getIntValue(addr, ((Annotation_Type) jcasType).casFeatCode_end);
  }

  /*
   * setter for end - sets ending of span of annotation
   */
  public void setEnd(int v) {
    // not needed - is built in
//    if (Annotation_Type.featOkTst && ((Annotation_Type) jcasType).casFeat_end == null)
//      this.jcasType.jcas.throwFeatMissing("end", "uima.tcas.Annotation");
    jcasType.ll_cas.ll_setIntValue(addr, ((Annotation_Type) jcasType).casFeatCode_end, v);
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

    final CAS casView = this.getView();
    final String text = casView.getDocumentText();
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
