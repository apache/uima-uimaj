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

package example;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * A Personal Title. Updated by JCasGen Mon May 23 17:48:43 EDT 2005 XML source:
 * c:\a\eclipse\301jxe\jedii_examples\descriptors\analysis_engine\NamesAndPersonTitles_TAE.xml
 * 
 * @generated
 */
public class PersonTitle extends Annotation {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCasRegistry.register(PersonTitle.class);

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
  protected PersonTitle() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public PersonTitle(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public PersonTitle(JCas jcas) {
    super(jcas);
    readObject();
  }

  public PersonTitle(JCas jcas, int begin, int end) {
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
  // * Feature: Kind

  /**
   * getter for Kind - gets The kind of title - Civilian, Military, or Government.
   * 
   * @generated
   */
  public String getKind() {
    if (PersonTitle_Type.featOkTst && ((PersonTitle_Type) jcasType).casFeat_Kind == null)
      this.jcasType.jcas.throwFeatMissing("Kind", "example.PersonTitle");
    return jcasType.ll_cas.ll_getStringValue(addr, ((PersonTitle_Type) jcasType).casFeatCode_Kind);
  }

  /**
   * setter for Kind - sets The kind of title - Civilian, Military, or Government.
   * 
   * @generated
   */
  public void setKind(String v) {
    if (PersonTitle_Type.featOkTst && ((PersonTitle_Type) jcasType).casFeat_Kind == null)
      this.jcasType.jcas.throwFeatMissing("Kind", "example.PersonTitle");
    jcasType.ll_cas.ll_setStringValue(addr, ((PersonTitle_Type) jcasType).casFeatCode_Kind, v);
  }
}
