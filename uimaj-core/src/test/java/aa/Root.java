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


/* First created by JCasGen Wed May 04 13:57:58 EDT 2016 */
package aa;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.StringArray;


/** 
 * Updated by JCasGen Wed May 04 13:57:58 EDT 2016
 * XML source: C:/au/svnCheckouts/trunk/uimaj280/uimaj/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class Root extends TOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Root.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Root() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Root(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Root(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: arrayInt

  /** getter for arrayInt - gets 
   * @generated
   * @return value of the feature 
   */
  public IntegerArray getArrayInt() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayInt == null)
      jcasType.jcas.throwFeatMissing("arrayInt", "aa.Root");
    return (IntegerArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayInt)));}
    
  /** setter for arrayInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayInt(IntegerArray v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayInt == null)
      jcasType.jcas.throwFeatMissing("arrayInt", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayInt, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arrayInt - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public int getArrayInt(int i) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayInt == null)
      jcasType.jcas.throwFeatMissing("arrayInt", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayInt), i);
    return jcasType.ll_cas.ll_getIntArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayInt), i);}

  /** indexed setter for arrayInt - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayInt(int i, int v) { 
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayInt == null)
      jcasType.jcas.throwFeatMissing("arrayInt", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayInt), i);
    jcasType.ll_cas.ll_setIntArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayInt), i, v);}
   
    
  //*--------------*
  //* Feature: arrayRef

  /** getter for arrayRef - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getArrayRef() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayRef == null)
      jcasType.jcas.throwFeatMissing("arrayRef", "aa.Root");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayRef)));}
    
  /** setter for arrayRef - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayRef(FSArray v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayRef == null)
      jcasType.jcas.throwFeatMissing("arrayRef", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayRef, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arrayRef - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public TOP getArrayRef(int i) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayRef == null)
      jcasType.jcas.throwFeatMissing("arrayRef", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayRef), i);
    return (TOP)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayRef), i)));}

  /** indexed setter for arrayRef - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayRef(int i, TOP v) { 
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayRef == null)
      jcasType.jcas.throwFeatMissing("arrayRef", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayRef), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayRef), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: arrayFloat

  /** getter for arrayFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public FloatArray getArrayFloat() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayFloat == null)
      jcasType.jcas.throwFeatMissing("arrayFloat", "aa.Root");
    return (FloatArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayFloat)));}
    
  /** setter for arrayFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayFloat(FloatArray v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayFloat == null)
      jcasType.jcas.throwFeatMissing("arrayFloat", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayFloat, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arrayFloat - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public float getArrayFloat(int i) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayFloat == null)
      jcasType.jcas.throwFeatMissing("arrayFloat", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayFloat), i);
    return jcasType.ll_cas.ll_getFloatArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayFloat), i);}

  /** indexed setter for arrayFloat - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayFloat(int i, float v) { 
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayFloat == null)
      jcasType.jcas.throwFeatMissing("arrayFloat", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayFloat), i);
    jcasType.ll_cas.ll_setFloatArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayFloat), i, v);}
   
    
  //*--------------*
  //* Feature: arrayString

  /** getter for arrayString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getArrayString() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayString == null)
      jcasType.jcas.throwFeatMissing("arrayString", "aa.Root");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayString)));}
    
  /** setter for arrayString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayString(StringArray v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayString == null)
      jcasType.jcas.throwFeatMissing("arrayString", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayString, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arrayString - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getArrayString(int i) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayString == null)
      jcasType.jcas.throwFeatMissing("arrayString", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayString), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayString), i);}

  /** indexed setter for arrayString - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayString(int i, String v) { 
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayString == null)
      jcasType.jcas.throwFeatMissing("arrayString", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayString), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayString), i, v);}
   
    
  //*--------------*
  //* Feature: plainInt

  /** getter for plainInt - gets 
   * @generated
   * @return value of the feature 
   */
  public int getPlainInt() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainInt == null)
      jcasType.jcas.throwFeatMissing("plainInt", "aa.Root");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Root_Type)jcasType).casFeatCode_plainInt);}
    
  /** setter for plainInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainInt(int v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainInt == null)
      jcasType.jcas.throwFeatMissing("plainInt", "aa.Root");
    jcasType.ll_cas.ll_setIntValue(addr, ((Root_Type)jcasType).casFeatCode_plainInt, v);}    
   
    
  //*--------------*
  //* Feature: plainFloat

  /** getter for plainFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public float getPlainFloat() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainFloat == null)
      jcasType.jcas.throwFeatMissing("plainFloat", "aa.Root");
    return jcasType.ll_cas.ll_getFloatValue(addr, ((Root_Type)jcasType).casFeatCode_plainFloat);}
    
  /** setter for plainFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainFloat(float v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainFloat == null)
      jcasType.jcas.throwFeatMissing("plainFloat", "aa.Root");
    jcasType.ll_cas.ll_setFloatValue(addr, ((Root_Type)jcasType).casFeatCode_plainFloat, v);}    
   
    
  //*--------------*
  //* Feature: plainString

  /** getter for plainString - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPlainString() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainString == null)
      jcasType.jcas.throwFeatMissing("plainString", "aa.Root");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Root_Type)jcasType).casFeatCode_plainString);}
    
  /** setter for plainString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainString(String v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainString == null)
      jcasType.jcas.throwFeatMissing("plainString", "aa.Root");
    jcasType.ll_cas.ll_setStringValue(addr, ((Root_Type)jcasType).casFeatCode_plainString, v);}    
   
    
  //*--------------*
  //* Feature: plainRef

  /** getter for plainRef - gets TokenType testMissingImport;
   * @generated
   * @return value of the feature 
   */
  public Root getPlainRef() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainRef == null)
      jcasType.jcas.throwFeatMissing("plainRef", "aa.Root");
    return (Root)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_plainRef)));}
    
  /** setter for plainRef - sets TokenType testMissingImport; 
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainRef(Root v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainRef == null)
      jcasType.jcas.throwFeatMissing("plainRef", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type)jcasType).casFeatCode_plainRef, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: plainLong

  /** getter for plainLong - gets 
   * @generated
   * @return value of the feature 
   */
  public long getPlainLong() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainLong == null)
      jcasType.jcas.throwFeatMissing("plainLong", "aa.Root");
    return jcasType.ll_cas.ll_getLongValue(addr, ((Root_Type)jcasType).casFeatCode_plainLong);}
    
  /** setter for plainLong - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainLong(long v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainLong == null)
      jcasType.jcas.throwFeatMissing("plainLong", "aa.Root");
    jcasType.ll_cas.ll_setLongValue(addr, ((Root_Type)jcasType).casFeatCode_plainLong, v);}    
   
    
  //*--------------*
  //* Feature: plainDouble

  /** getter for plainDouble - gets 
   * @generated
   * @return value of the feature 
   */
  public double getPlainDouble() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainDouble == null)
      jcasType.jcas.throwFeatMissing("plainDouble", "aa.Root");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((Root_Type)jcasType).casFeatCode_plainDouble);}
    
  /** setter for plainDouble - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainDouble(double v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_plainDouble == null)
      jcasType.jcas.throwFeatMissing("plainDouble", "aa.Root");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((Root_Type)jcasType).casFeatCode_plainDouble, v);}    
   
    
  //*--------------*
  //* Feature: arrayLong

  /** getter for arrayLong - gets 
   * @generated
   * @return value of the feature 
   */
  public LongArray getArrayLong() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayLong == null)
      jcasType.jcas.throwFeatMissing("arrayLong", "aa.Root");
    return (LongArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayLong)));}
    
  /** setter for arrayLong - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayLong(LongArray v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayLong == null)
      jcasType.jcas.throwFeatMissing("arrayLong", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayLong, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arrayLong - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public long getArrayLong(int i) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayLong == null)
      jcasType.jcas.throwFeatMissing("arrayLong", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayLong), i);
    return jcasType.ll_cas.ll_getLongArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayLong), i);}

  /** indexed setter for arrayLong - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayLong(int i, long v) { 
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayLong == null)
      jcasType.jcas.throwFeatMissing("arrayLong", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayLong), i);
    jcasType.ll_cas.ll_setLongArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayLong), i, v);}
   
    
  //*--------------*
  //* Feature: arrayDouble

  /** getter for arrayDouble - gets 
   * @generated
   * @return value of the feature 
   */
  public DoubleArray getArrayDouble() {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayDouble == null)
      jcasType.jcas.throwFeatMissing("arrayDouble", "aa.Root");
    return (DoubleArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayDouble)));}
    
  /** setter for arrayDouble - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayDouble(DoubleArray v) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayDouble == null)
      jcasType.jcas.throwFeatMissing("arrayDouble", "aa.Root");
    jcasType.ll_cas.ll_setRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayDouble, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arrayDouble - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public double getArrayDouble(int i) {
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayDouble == null)
      jcasType.jcas.throwFeatMissing("arrayDouble", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayDouble), i);
    return jcasType.ll_cas.ll_getDoubleArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayDouble), i);}

  /** indexed setter for arrayDouble - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayDouble(int i, double v) { 
    if (Root_Type.featOkTst && ((Root_Type)jcasType).casFeat_arrayDouble == null)
      jcasType.jcas.throwFeatMissing("arrayDouble", "aa.Root");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayDouble), i);
    jcasType.ll_cas.ll_setDoubleArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Root_Type)jcasType).casFeatCode_arrayDouble), i, v);}
  }

    