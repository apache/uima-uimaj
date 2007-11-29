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

package org.apache.uima.examples.opennlp;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Syntax annotation, typically created by a parser. Updated by JCasGen Fri Dec 02 14:22:24 EST 2005
 * XML source:
 * c:/workspace/uimaj-examples/opennlp/src/org/apache/uima/examples/opennlp/annotator/OpenNLPExampleTypes.xml
 * 
 * @generated
 */
public class SyntaxAnnotation extends Annotation {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCasRegistry.register(SyntaxAnnotation.class);

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
  protected SyntaxAnnotation() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public SyntaxAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public SyntaxAnnotation(JCas jcas) {
    super(jcas);
    readObject();
  }

  public SyntaxAnnotation(JCas jcas, int begin, int end) {
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
  // * Feature: componentId

  /**
   * getter for componentId - gets Identifier of the annotator that created this annotation.
   * 
   * @generated
   */
  public String getComponentId() {
    if (SyntaxAnnotation_Type.featOkTst
            && ((SyntaxAnnotation_Type) jcasType).casFeat_componentId == null)
      this.jcasType.jcas.throwFeatMissing("componentId", "org.apache.uima.examples.opennlp.SyntaxAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((SyntaxAnnotation_Type) jcasType).casFeatCode_componentId);
  }

  /**
   * setter for componentId - sets Identifier of the annotator that created this annotation.
   * 
   * @generated
   */
  public void setComponentId(String v) {
    if (SyntaxAnnotation_Type.featOkTst
            && ((SyntaxAnnotation_Type) jcasType).casFeat_componentId == null)
      this.jcasType.jcas.throwFeatMissing("componentId", "org.apache.uima.examples.opennlp.SyntaxAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr,
            ((SyntaxAnnotation_Type) jcasType).casFeatCode_componentId, v);
  }
}
