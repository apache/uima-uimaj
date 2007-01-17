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

package org.apache.uima.tutorial;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Updated by JCasGen Mon Nov 29 15:02:38 EST 2004 XML source: C:/Program
 * Files/apache/uima/examples/descriptors/tutorial/ex6/TutorialTypeSystem.xml
 * 
 * @generated
 */
public class UimaAcronym extends Annotation {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCasRegistry.register(UimaAcronym.class);

  /**
   * @generated
   * @ordered
   */
  public final static int type = typeIndexID;

  /** @generated */
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected UimaAcronym() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public UimaAcronym(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public UimaAcronym(JCas jcas) {
    super(jcas);
    readObject();
  }

  public UimaAcronym(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

  // *--------------*
  // * Feature: expandedForm

  /**
   * getter for expandedForm - gets
   * 
   * @generated
   */
  public String getExpandedForm() {
    if (UimaAcronym_Type.featOkTst && ((UimaAcronym_Type) jcasType).casFeat_expandedForm == null)
      this.jcasType.jcas.throwFeatMissing("expandedForm", "org.apache.uima.tutorial.UimaAcronym");
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((UimaAcronym_Type) jcasType).casFeatCode_expandedForm);
  }

  /**
   * setter for expandedForm - sets
   * 
   * @generated
   */
  public void setExpandedForm(String v) {
    if (UimaAcronym_Type.featOkTst && ((UimaAcronym_Type) jcasType).casFeat_expandedForm == null)
      this.jcasType.jcas.throwFeatMissing("expandedForm", "org.apache.uima.tutorial.UimaAcronym");
    jcasType.ll_cas.ll_setStringValue(addr, ((UimaAcronym_Type) jcasType).casFeatCode_expandedForm,
            v);
  }

  /** Custom constructor taking all parameters */
  public UimaAcronym(JCas jcas, int start, int end, String expandedForm) {
    super(jcas, start, end);
    setExpandedForm(expandedForm);
  }
}
