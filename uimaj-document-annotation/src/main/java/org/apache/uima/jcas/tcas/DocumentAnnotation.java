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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * The JCas class definition for the CAS <code>DocumentAnnotation</code> type. When text CASs are
 * created, one instance of this type is created and made accessible via a call to the
 * {@link JCas#getDocumentAnnotationFs()} method. It is also a subtype of {@link Annotation} and
 * therefore would appear as one of the annotations that an iterator over all the annotations would
 * return.
 */
public class DocumentAnnotation extends Annotation {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_DOCUMENT_ANNOTATION;
  public final static String _FeatName_language = "language";
  
  public final static int typeIndexID = JCasRegistry.register(DocumentAnnotation.class);

  public final static int type = typeIndexID;

  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  private final static CallSite _FC_language = TypeSystemImpl.createCallSite(DocumentAnnotation.class, "language");
  private final static MethodHandle _FH_language = _FC_language.dynamicInvoker();
        
  // Never called. Disable default constructor
  protected DocumentAnnotation() {
  }

  public DocumentAnnotation(JCas jcas) {
    super(jcas);
  }

  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public DocumentAnnotation(TypeImpl t, CASImpl c) {
    super(t, c);
  }

  // *------------------*
  // * Feature: language
  /**
   * getter for language
   * @return the language
   */
  public String getLanguage() { return _getStringValueNc(wrapGetIntCatchException(_FH_language)); }

  /**
   * setter for language
   * @param v the language
   */
  public void setLanguage(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_language), v);
  }
}
