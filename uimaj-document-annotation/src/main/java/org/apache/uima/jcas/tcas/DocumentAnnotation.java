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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * The JCas class definition for the CAS <code>DocumentAnnotation</code> type. When text CASs are
 * created, one instance of this type is created and made accessible via a call to the
 * {@link JCas#getDocumentAnnotationFs()} method. It is also a subtype of {@link Annotation} and
 * therefore would appear as one of the annotations that an iterator over all the annotations would
 * return.
 */
public class DocumentAnnotation extends Annotation {

  public final static int typeIndexID = JCasRegistry.register(DocumentAnnotation.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected DocumentAnnotation() {
  }

  /** Internal - Constructor used by generator */
  public DocumentAnnotation(int addr, TOP_Type type) {
    super(addr, type);
  }

  public DocumentAnnotation(JCas jcas) {
    super(jcas);
  }

  // *------------------*
  // * Feature: language
  /**
   * getter for language
   */
  public String getLanguage() {
    if (DocumentAnnotation_Type.featOkTst
            && ((DocumentAnnotation_Type) jcasType).casFeat_language == null)
      this.jcasType.jcas.throwFeatMissing("language", "uima.tcas.DocumentAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((DocumentAnnotation_Type) jcasType).casFeatCode_language);
  }

  /**
   * setter for language
   */
  public void setLanguage(String v) {
    if (DocumentAnnotation_Type.featOkTst
            && ((DocumentAnnotation_Type) jcasType).casFeat_language == null)
      this.jcasType.jcas.throwFeatMissing("language", "uima.tcas.DocumentAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr,
            ((DocumentAnnotation_Type) jcasType).casFeatCode_language, v);
  }
}
