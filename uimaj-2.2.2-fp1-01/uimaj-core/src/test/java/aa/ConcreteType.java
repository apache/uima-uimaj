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

package aa;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * Updated by JCasGen Tue Feb 21 14:56:04 EST 2006 XML source:
 * C:/a/Eclipse/3.1/j4/jedii_jcas_tests/testTypes.xml
 * 
 * @generated
 */
public class ConcreteType extends AbstractType {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = org.apache.uima.jcas.JCasRegistry.register(ConcreteType.class);

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
  protected ConcreteType() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public ConcreteType(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public ConcreteType(JCas jcas) {
    super(jcas);
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
  // * Feature: concreteString

  /**
   * getter for concreteString - gets
   * 
   * @generated
   */
  public String getConcreteString() {
    if (ConcreteType_Type.featOkTst
            && ((ConcreteType_Type) jcasType).casFeat_concreteString == null)
      this.jcasType.jcas.throwFeatMissing("concreteString", "aa.ConcreteType");
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((ConcreteType_Type) jcasType).casFeatCode_concreteString);
  }

  /**
   * setter for concreteString - sets
   * 
   * @generated
   */
  public void setConcreteString(String v) {
    if (ConcreteType_Type.featOkTst
            && ((ConcreteType_Type) jcasType).casFeat_concreteString == null)
      this.jcasType.jcas.throwFeatMissing("concreteString", "aa.ConcreteType");
    jcasType.ll_cas.ll_setStringValue(addr,
            ((ConcreteType_Type) jcasType).casFeatCode_concreteString, v);
  }
}
