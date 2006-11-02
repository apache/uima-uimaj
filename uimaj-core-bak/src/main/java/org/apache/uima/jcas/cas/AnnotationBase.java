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
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.impl.AnnotationImpl;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TCASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.impl.JCas;
/**
 * the JCas class model for the CAS type uima.cas.Annotation.
 * It defines two integer valued features indicating the begin and end of the
 * span being annotated.  There is also a method to retrieve the spanned
 * text as a string.
 */
public class AnnotationBase extends org.apache.uima.jcas.cas.TOP implements AnnotationBaseFS {

  public final static int typeIndexID = JCas.getNextIndex();
  public final static int type = typeIndexID;
  public           int getTypeIndexID() {return typeIndexID;}

  // Never called.  Disable default constructor
  protected AnnotationBase() {}

  /** Internal - Constructor used by generator */
  public AnnotationBase(int addr, TOP_Type type) {
    super(addr, type);
  }
  
  public AnnotationBase(JCas jcas) {
    super(jcas);
  }

  //*------------------*
  //* Feature: sofa
  //* Sofa reference of the annotation
  /** getter for sofa - gets Sofaref for annotation
  * */
  public SofaFS getSofa() {
    if (AnnotationBase_Type.featOkTst && ((AnnotationBase_Type)jcasType).casFeat_sofa == null)
          JCas.throwFeatMissing("sofa", "uima.tcas.Annotation");
    return (SofaFS)jcasType.ll_cas.ll_getFSForRef(addr);}

  
  public CAS getView() {return this.jcasType.casImpl.ll_getSofaCasView(addr); }

}
