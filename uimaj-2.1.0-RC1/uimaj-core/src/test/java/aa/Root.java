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
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * Updated by JCasGen Tue Feb 21 14:56:04 EST 2006 XML source:
 * C:/a/Eclipse/3.1/j4/jedii_jcas_tests/testTypes.xml
 * 
 * @generated
 */
public class Root extends TOP {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = org.apache.uima.jcas.JCasRegistry.register(Root.class);

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
  protected Root() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public Root(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public Root(JCas jcas) {
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
  // * Feature: arrayInt

  /**
   * getter for arrayInt - gets
   * 
   * @generated
   */
  public IntegerArray getArrayInt() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayInt == null)
      this.jcasType.jcas.throwFeatMissing("arrayInt", "aa.Root");
    return (IntegerArray) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayInt)));
  }

  /**
   * setter for arrayInt - sets
   * 
   * @generated
   */
  public void setArrayInt(IntegerArray v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayInt == null)
      this.jcasType.jcas.throwFeatMissing("arrayInt", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type) jcasType).casFeatCode_arrayInt,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  /**
   * indexed getter for arrayInt - gets an indexed value -
   * 
   * @generated
   */
  public int getArrayInt(int i) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayInt == null)
      this.jcasType.jcas.throwFeatMissing("arrayInt", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayInt), i);
    return jcasType.ll_cas.ll_getIntArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayInt), i);
  }

  /**
   * indexed setter for arrayInt - sets an indexed value -
   * 
   * @generated
   */
  public void setArrayInt(int i, int v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayInt == null)
      this.jcasType.jcas.throwFeatMissing("arrayInt", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayInt), i);
    jcasType.ll_cas.ll_setIntArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayInt), i, v);
  }

  // *--------------*
  // * Feature: arrayRef

  /**
   * getter for arrayRef - gets
   * 
   * @generated
   */
  public FSArray getArrayRef() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayRef == null)
      this.jcasType.jcas.throwFeatMissing("arrayRef", "aa.Root");
    return (FSArray) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayRef)));
  }

  /**
   * setter for arrayRef - sets
   * 
   * @generated
   */
  public void setArrayRef(FSArray v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayRef == null)
      this.jcasType.jcas.throwFeatMissing("arrayRef", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type) jcasType).casFeatCode_arrayRef,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  /**
   * indexed getter for arrayRef - gets an indexed value -
   * 
   * @generated
   */
  public TOP getArrayRef(int i) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayRef == null)
      this.jcasType.jcas.throwFeatMissing("arrayRef", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayRef), i);
    return (TOP) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(
            jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type) jcasType).casFeatCode_arrayRef), i)));
  }

  /**
   * indexed setter for arrayRef - sets an indexed value -
   * 
   * @generated
   */
  public void setArrayRef(int i, TOP v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayRef == null)
      this.jcasType.jcas.throwFeatMissing("arrayRef", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayRef), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayRef), i, jcasType.ll_cas.ll_getFSRef(v));
  }

  // *--------------*
  // * Feature: arrayFloat

  /**
   * getter for arrayFloat - gets
   * 
   * @generated
   */
  public FloatArray getArrayFloat() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayFloat == null)
      this.jcasType.jcas.throwFeatMissing("arrayFloat", "aa.Root");
    return (FloatArray) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayFloat)));
  }

  /**
   * setter for arrayFloat - sets
   * 
   * @generated
   */
  public void setArrayFloat(FloatArray v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayFloat == null)
      this.jcasType.jcas.throwFeatMissing("arrayFloat", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type) jcasType).casFeatCode_arrayFloat,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  /**
   * indexed getter for arrayFloat - gets an indexed value -
   * 
   * @generated
   */
  public float getArrayFloat(int i) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayFloat == null)
      this.jcasType.jcas.throwFeatMissing("arrayFloat", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayFloat), i);
    return jcasType.ll_cas.ll_getFloatArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayFloat), i);
  }

  /**
   * indexed setter for arrayFloat - sets an indexed value -
   * 
   * @generated
   */
  public void setArrayFloat(int i, float v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayFloat == null)
      this.jcasType.jcas.throwFeatMissing("arrayFloat", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayFloat), i);
    jcasType.ll_cas.ll_setFloatArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayFloat), i, v);
  }

  // *--------------*
  // * Feature: arrayString

  /**
   * getter for arrayString - gets
   * 
   * @generated
   */
  public StringArray getArrayString() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayString == null)
      this.jcasType.jcas.throwFeatMissing("arrayString", "aa.Root");
    return (StringArray) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayString)));
  }

  /**
   * setter for arrayString - sets
   * 
   * @generated
   */
  public void setArrayString(StringArray v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayString == null)
      this.jcasType.jcas.throwFeatMissing("arrayString", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type) jcasType).casFeatCode_arrayString,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  /**
   * indexed getter for arrayString - gets an indexed value -
   * 
   * @generated
   */
  public String getArrayString(int i) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayString == null)
      this.jcasType.jcas.throwFeatMissing("arrayString", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayString), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayString), i);
  }

  /**
   * indexed setter for arrayString - sets an indexed value -
   * 
   * @generated
   */
  public void setArrayString(int i, String v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_arrayString == null)
      this.jcasType.jcas.throwFeatMissing("arrayString", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayString), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_arrayString), i, v);
  }

  // *--------------*
  // * Feature: plainInt

  /**
   * getter for plainInt - gets
   * 
   * @generated
   */
  public int getPlainInt() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_plainInt == null)
      this.jcasType.jcas.throwFeatMissing("plainInt", "aa.Root");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Root_Type) jcasType).casFeatCode_plainInt);
  }

  /**
   * setter for plainInt - sets
   * 
   * @generated
   */
  public void setPlainInt(int v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_plainInt == null)
      this.jcasType.jcas.throwFeatMissing("plainInt", "aa.Root");
    jcasType.ll_cas.ll_setIntValue(addr, ((Root_Type) jcasType).casFeatCode_plainInt, v);
  }

  // *--------------*
  // * Feature: plainFloat

  /**
   * getter for plainFloat - gets
   * 
   * @generated
   */
  public float getPlainFloat() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_plainFloat == null)
      this.jcasType.jcas.throwFeatMissing("plainFloat", "aa.Root");
    return jcasType.ll_cas.ll_getFloatValue(addr, ((Root_Type) jcasType).casFeatCode_plainFloat);
  }

  /**
   * setter for plainFloat - sets
   * 
   * @generated
   */
  public void setPlainFloat(float v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_plainFloat == null)
      this.jcasType.jcas.throwFeatMissing("plainFloat", "aa.Root");
    jcasType.ll_cas.ll_setFloatValue(addr, ((Root_Type) jcasType).casFeatCode_plainFloat, v);
  }

  // *--------------*
  // * Feature: plainString

  /**
   * getter for plainString - gets
   * 
   * @generated
   */
  public String getPlainString() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_plainString == null)
      this.jcasType.jcas.throwFeatMissing("plainString", "aa.Root");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Root_Type) jcasType).casFeatCode_plainString);
  }

  /**
   * setter for plainString - sets
   * 
   * @generated
   */
  public void setPlainString(String v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_plainString == null)
      this.jcasType.jcas.throwFeatMissing("plainString", "aa.Root");
    jcasType.ll_cas.ll_setStringValue(addr, ((Root_Type) jcasType).casFeatCode_plainString, v);
  }

  // *--------------*
  // * Feature: plainRef

  /**
   * getter for plainRef - gets
   * 
   * @generated
   */
  public Root getPlainRef() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_plainRef == null)
      this.jcasType.jcas.throwFeatMissing("plainRef", "aa.Root");
    return (Root) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Root_Type) jcasType).casFeatCode_plainRef)));
  }

  /**
   * setter for plainRef - sets
   * 
   * @generated
   */
  public void setPlainRef(Root v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_plainRef == null)
      this.jcasType.jcas.throwFeatMissing("plainRef", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type) jcasType).casFeatCode_plainRef,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  // *--------------*
  // * Feature: concreteString

  /**
   * getter for concreteString - gets
   * 
   * @generated
   */
  public String getConcreteString() {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_concreteString == null)
      this.jcasType.jcas.throwFeatMissing("concreteString", "aa.Root");
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((Root_Type) jcasType).casFeatCode_concreteString);
  }

  /**
   * setter for concreteString - sets
   * 
   * @generated
   */
  public void setConcreteString(String v) {
    if (Root_Type.featOkTst && ((Root_Type) jcasType).casFeat_concreteString == null)
      this.jcasType.jcas.throwFeatMissing("concreteString", "aa.Root");
    jcasType.ll_cas.ll_setStringValue(addr, ((Root_Type) jcasType).casFeatCode_concreteString, v);
  }
}
