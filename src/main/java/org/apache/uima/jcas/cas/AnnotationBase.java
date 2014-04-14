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

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * the JCas class model for the CAS type uima.cas.AnnotationBase. 
 * The AnnotationBase type defines one system-used feature which 
 * specifies for an annotation the subject of analysis (Sofa) to which it refers. 
 * Various annotation types (including the built-in uima.tcas.Annotation)
 * may be defined as subtypes of this type.
 * 
 * uima.tcas.Annotation is a subtype of this type, appropriate for
 * Subjects of Analysis which are text strings.  Other (not-built-in)
 * subtypes may be defined for other kinds of Subjects of Analysis.  For instance
 * an audio sample Subject of Analysis might define a start and end position as time points 
 * in the stream.  An image Subject of Analysis might define rectangular coordiantes
 * describing a sub-area of the image.
 * 
 * If you are defining a type which needs a reference to the Subject of Analysis
 * (which is view-specific),
 * it should be a subtype of this base type.
 */
public class AnnotationBase extends org.apache.uima.jcas.cas.TOP implements AnnotationBaseFS {

  public final static int typeIndexID = JCasRegistry.register(AnnotationBase.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected AnnotationBase() {
  }

 /* Internal - Constructor used by generator */
  public AnnotationBase(int addr, TOP_Type type) {
    super(addr, type);
  }

  public AnnotationBase(JCas jcas) {
    super(jcas);
  }

  // *------------------*
  // * Feature: sofa
  // * Sofa reference of the annotation
  /*
   * getter for sofa - gets Sofaref for annotation
   */
  public SofaFS getSofa() {
    if (AnnotationBase_Type.featOkTst && ((AnnotationBase_Type) jcasType).casFeat_sofa == null) {
      // https://issues.apache.org/jira/browse/UIMA-2384
      this.jcasType.jcas.throwFeatMissing("sofa", this.getClass().getName());
    }
    return (SofaFS) jcasType.ll_cas.ll_getFSForRef(
            jcasType.ll_cas.ll_getRefValue(addr, ((AnnotationBase_Type)jcasType).casFeatCode_sofa));
  }

  public CAS getView() {
    return this.jcasType.casImpl.ll_getSofaCasView(addr);
  }

}
