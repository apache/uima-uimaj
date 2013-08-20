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
/* First created by JCasGen Mon Mar 29 06:42:24 MDT 2010 */
package org.apache.uima.fit.examples.tutorial.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Updated by JCasGen Fri Jun 11 20:10:52 MDT 2010 XML source: C:/Users/Philip/Documents
 * /Academic/workspace/uimaFIT-examples/src/main/resources /org/uimafit/examples/TypeSystem.xml
 * 
 * @generated
 */
public class RoomNumber extends Annotation {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCasRegistry.register(RoomNumber.class);

  /**
   * @generated
   * @ordered
   */
  public final static int type = typeIndexID;

  /** @generated */
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected RoomNumber() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public RoomNumber(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public RoomNumber(JCas jcas) {
    super(jcas);
    readObject();
  }

  /** @generated */
  public RoomNumber(JCas jcas, int begin, int end) {
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
  // * Feature: building

  /**
   * getter for building - gets
   * 
   * @generated
   */
  public String getBuilding() {
    if (RoomNumber_Type.featOkTst && ((RoomNumber_Type) jcasType).casFeat_building == null) {
      jcasType.jcas.throwFeatMissing("building",
              "org.apache.uima.fit.examples.tutorial.type.RoomNumber");
    }
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((RoomNumber_Type) jcasType).casFeatCode_building);
  }

  /**
   * setter for building - sets
   * 
   * @generated
   */
  public void setBuilding(String v) {
    if (RoomNumber_Type.featOkTst && ((RoomNumber_Type) jcasType).casFeat_building == null) {
      jcasType.jcas.throwFeatMissing("building",
              "org.apache.uima.fit.examples.tutorial.type.RoomNumber");
    }
    jcasType.ll_cas.ll_setStringValue(addr, ((RoomNumber_Type) jcasType).casFeatCode_building, v);
  }
}
