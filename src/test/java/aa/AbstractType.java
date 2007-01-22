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
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * Updated by JCasGen Tue Feb 21 14:56:04 EST 2006 XML source:
 * C:/a/Eclipse/3.1/j4/jedii_jcas_tests/testTypes.xml
 * 
 * @generated
 */
public class AbstractType extends TOP {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = org.apache.uima.jcas.JCasRegistry.register(AbstractType.class);

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
  protected AbstractType() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public AbstractType(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public AbstractType(JCas jcas) {
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
  // * Feature: abstractInt

  /**
   * getter for abstractInt - gets
   * 
   * @generated
   */
  public int getAbstractInt() {
    if (AbstractType_Type.featOkTst && ((AbstractType_Type) jcasType).casFeat_abstractInt == null)
      this.jcasType.jcas.throwFeatMissing("abstractInt", "aa.AbstractType");
    return jcasType.ll_cas.ll_getIntValue(addr,
            ((AbstractType_Type) jcasType).casFeatCode_abstractInt);
  }

  /**
   * setter for abstractInt - sets
   * 
   * @generated
   */
  public void setAbstractInt(int v) {
    if (AbstractType_Type.featOkTst && ((AbstractType_Type) jcasType).casFeat_abstractInt == null)
      this.jcasType.jcas.throwFeatMissing("abstractInt", "aa.AbstractType");
    jcasType.ll_cas.ll_setIntValue(addr, ((AbstractType_Type) jcasType).casFeatCode_abstractInt, v);
  }
}
