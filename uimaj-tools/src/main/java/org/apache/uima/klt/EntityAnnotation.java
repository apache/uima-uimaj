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

package org.apache.uima.klt;

import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.impl.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * An annotation of a span of text that refers to some entity Updated by JCasGen Thu Apr 21 11:20:08
 * EDT 2005 XML source: descriptors/types/hutt.xml
 * 
 * @generated
 */
public class EntityAnnotation extends Annotation {
  public String toString() {
    return "(ENTITY-ANN \"" + getCoveredText() + "\" " + getComponentId() + " "
                    + getClass().getName() + ":" + getMentionType() + ")";
  }

  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCas.getNextIndex();

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
  protected EntityAnnotation() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public EntityAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public EntityAnnotation(JCas jcas) {
    super(jcas);
    readObject();
  }

  public EntityAnnotation(JCas jcas, int begin, int end) {
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
  // * Feature: links

  /**
   * getter for links - gets
   * 
   * @generated
   */
  public FSList getLinks() {
    if (EntityAnnotation_Type.featOkTst && ((EntityAnnotation_Type) jcasType).casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.EntityAnnotation");
    return (FSList) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
                    ((EntityAnnotation_Type) jcasType).casFeatCode_links)));
  }

  /**
   * setter for links - sets
   * 
   * @generated
   */
  public void setLinks(FSList v) {
    if (EntityAnnotation_Type.featOkTst && ((EntityAnnotation_Type) jcasType).casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.EntityAnnotation");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityAnnotation_Type) jcasType).casFeatCode_links,
                    jcasType.ll_cas.ll_getFSRef(v));
  }

  // *--------------*
  // * Feature: componentId

  /**
   * getter for componentId - gets
   * 
   * @generated
   */
  public String getComponentId() {
    if (EntityAnnotation_Type.featOkTst
                    && ((EntityAnnotation_Type) jcasType).casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.EntityAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr,
                    ((EntityAnnotation_Type) jcasType).casFeatCode_componentId);
  }

  /**
   * setter for componentId - sets
   * 
   * @generated
   */
  public void setComponentId(String v) {
    if (EntityAnnotation_Type.featOkTst
                    && ((EntityAnnotation_Type) jcasType).casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.EntityAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr,
                    ((EntityAnnotation_Type) jcasType).casFeatCode_componentId, v);
  }

  // *--------------*
  // * Feature: mentionType

  /**
   * getter for mentionType - gets
   * 
   * @generated
   */
  public String getMentionType() {
    if (EntityAnnotation_Type.featOkTst
                    && ((EntityAnnotation_Type) jcasType).casFeat_mentionType == null)
      JCas.throwFeatMissing("mentionType", "org.apache.uima.klt.EntityAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr,
                    ((EntityAnnotation_Type) jcasType).casFeatCode_mentionType);
  }

  /**
   * setter for mentionType - sets
   * 
   * @generated
   */
  public void setMentionType(String v) {
    if (EntityAnnotation_Type.featOkTst
                    && ((EntityAnnotation_Type) jcasType).casFeat_mentionType == null)
      JCas.throwFeatMissing("mentionType", "org.apache.uima.klt.EntityAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr,
                    ((EntityAnnotation_Type) jcasType).casFeatCode_mentionType, v);
  }
}
