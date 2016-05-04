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
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.cas.TOP_Type;

/** 
 * Updated by JCasGen Wed May 04 13:57:58 EDT 2016
 * @generated */
public class Root_Type extends TOP_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Root.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("aa.Root");
 
  /** @generated */
  final Feature casFeat_arrayInt;
  /** @generated */
  final int     casFeatCode_arrayInt;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArrayInt(int addr) {
        if (featOkTst && casFeat_arrayInt == null)
      jcas.throwFeatMissing("arrayInt", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArrayInt(int addr, int v) {
        if (featOkTst && casFeat_arrayInt == null)
      jcas.throwFeatMissing("arrayInt", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayInt, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getArrayInt(int addr, int i) {
        if (featOkTst && casFeat_arrayInt == null)
      jcas.throwFeatMissing("arrayInt", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i);
	return ll_cas.ll_getIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArrayInt(int addr, int i, int v) {
        if (featOkTst && casFeat_arrayInt == null)
      jcas.throwFeatMissing("arrayInt", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i);
    ll_cas.ll_setIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_arrayRef;
  /** @generated */
  final int     casFeatCode_arrayRef;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArrayRef(int addr) {
        if (featOkTst && casFeat_arrayRef == null)
      jcas.throwFeatMissing("arrayRef", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArrayRef(int addr, int v) {
        if (featOkTst && casFeat_arrayRef == null)
      jcas.throwFeatMissing("arrayRef", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayRef, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getArrayRef(int addr, int i) {
        if (featOkTst && casFeat_arrayRef == null)
      jcas.throwFeatMissing("arrayRef", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArrayRef(int addr, int i, int v) {
        if (featOkTst && casFeat_arrayRef == null)
      jcas.throwFeatMissing("arrayRef", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_arrayFloat;
  /** @generated */
  final int     casFeatCode_arrayFloat;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArrayFloat(int addr) {
        if (featOkTst && casFeat_arrayFloat == null)
      jcas.throwFeatMissing("arrayFloat", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArrayFloat(int addr, int v) {
        if (featOkTst && casFeat_arrayFloat == null)
      jcas.throwFeatMissing("arrayFloat", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayFloat, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public float getArrayFloat(int addr, int i) {
        if (featOkTst && casFeat_arrayFloat == null)
      jcas.throwFeatMissing("arrayFloat", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getFloatArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i);
	return ll_cas.ll_getFloatArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArrayFloat(int addr, int i, float v) {
        if (featOkTst && casFeat_arrayFloat == null)
      jcas.throwFeatMissing("arrayFloat", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setFloatArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i);
    ll_cas.ll_setFloatArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_arrayString;
  /** @generated */
  final int     casFeatCode_arrayString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArrayString(int addr) {
        if (featOkTst && casFeat_arrayString == null)
      jcas.throwFeatMissing("arrayString", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArrayString(int addr, int v) {
        if (featOkTst && casFeat_arrayString == null)
      jcas.throwFeatMissing("arrayString", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayString, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getArrayString(int addr, int i) {
        if (featOkTst && casFeat_arrayString == null)
      jcas.throwFeatMissing("arrayString", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i);
	return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArrayString(int addr, int i, String v) {
        if (featOkTst && casFeat_arrayString == null)
      jcas.throwFeatMissing("arrayString", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_plainInt;
  /** @generated */
  final int     casFeatCode_plainInt;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getPlainInt(int addr) {
        if (featOkTst && casFeat_plainInt == null)
      jcas.throwFeatMissing("plainInt", "aa.Root");
    return ll_cas.ll_getIntValue(addr, casFeatCode_plainInt);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPlainInt(int addr, int v) {
        if (featOkTst && casFeat_plainInt == null)
      jcas.throwFeatMissing("plainInt", "aa.Root");
    ll_cas.ll_setIntValue(addr, casFeatCode_plainInt, v);}
    
  
 
  /** @generated */
  final Feature casFeat_plainFloat;
  /** @generated */
  final int     casFeatCode_plainFloat;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public float getPlainFloat(int addr) {
        if (featOkTst && casFeat_plainFloat == null)
      jcas.throwFeatMissing("plainFloat", "aa.Root");
    return ll_cas.ll_getFloatValue(addr, casFeatCode_plainFloat);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPlainFloat(int addr, float v) {
        if (featOkTst && casFeat_plainFloat == null)
      jcas.throwFeatMissing("plainFloat", "aa.Root");
    ll_cas.ll_setFloatValue(addr, casFeatCode_plainFloat, v);}
    
  
 
  /** @generated */
  final Feature casFeat_plainString;
  /** @generated */
  final int     casFeatCode_plainString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPlainString(int addr) {
        if (featOkTst && casFeat_plainString == null)
      jcas.throwFeatMissing("plainString", "aa.Root");
    return ll_cas.ll_getStringValue(addr, casFeatCode_plainString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPlainString(int addr, String v) {
        if (featOkTst && casFeat_plainString == null)
      jcas.throwFeatMissing("plainString", "aa.Root");
    ll_cas.ll_setStringValue(addr, casFeatCode_plainString, v);}
    
  
 
  /** @generated */
  final Feature casFeat_plainRef;
  /** @generated */
  final int     casFeatCode_plainRef;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getPlainRef(int addr) {
        if (featOkTst && casFeat_plainRef == null)
      jcas.throwFeatMissing("plainRef", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_plainRef);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPlainRef(int addr, int v) {
        if (featOkTst && casFeat_plainRef == null)
      jcas.throwFeatMissing("plainRef", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_plainRef, v);}
    
  
 
  /** @generated */
  final Feature casFeat_plainLong;
  /** @generated */
  final int     casFeatCode_plainLong;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public long getPlainLong(int addr) {
        if (featOkTst && casFeat_plainLong == null)
      jcas.throwFeatMissing("plainLong", "aa.Root");
    return ll_cas.ll_getLongValue(addr, casFeatCode_plainLong);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPlainLong(int addr, long v) {
        if (featOkTst && casFeat_plainLong == null)
      jcas.throwFeatMissing("plainLong", "aa.Root");
    ll_cas.ll_setLongValue(addr, casFeatCode_plainLong, v);}
    
  
 
  /** @generated */
  final Feature casFeat_plainDouble;
  /** @generated */
  final int     casFeatCode_plainDouble;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getPlainDouble(int addr) {
        if (featOkTst && casFeat_plainDouble == null)
      jcas.throwFeatMissing("plainDouble", "aa.Root");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_plainDouble);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPlainDouble(int addr, double v) {
        if (featOkTst && casFeat_plainDouble == null)
      jcas.throwFeatMissing("plainDouble", "aa.Root");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_plainDouble, v);}
    
  
 
  /** @generated */
  final Feature casFeat_arrayLong;
  /** @generated */
  final int     casFeatCode_arrayLong;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArrayLong(int addr) {
        if (featOkTst && casFeat_arrayLong == null)
      jcas.throwFeatMissing("arrayLong", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayLong);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArrayLong(int addr, int v) {
        if (featOkTst && casFeat_arrayLong == null)
      jcas.throwFeatMissing("arrayLong", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayLong, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public long getArrayLong(int addr, int i) {
        if (featOkTst && casFeat_arrayLong == null)
      jcas.throwFeatMissing("arrayLong", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getLongArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayLong), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayLong), i);
	return ll_cas.ll_getLongArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayLong), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArrayLong(int addr, int i, long v) {
        if (featOkTst && casFeat_arrayLong == null)
      jcas.throwFeatMissing("arrayLong", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setLongArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayLong), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayLong), i);
    ll_cas.ll_setLongArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayLong), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_arrayDouble;
  /** @generated */
  final int     casFeatCode_arrayDouble;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArrayDouble(int addr) {
        if (featOkTst && casFeat_arrayDouble == null)
      jcas.throwFeatMissing("arrayDouble", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayDouble);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArrayDouble(int addr, int v) {
        if (featOkTst && casFeat_arrayDouble == null)
      jcas.throwFeatMissing("arrayDouble", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayDouble, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public double getArrayDouble(int addr, int i) {
        if (featOkTst && casFeat_arrayDouble == null)
      jcas.throwFeatMissing("arrayDouble", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getDoubleArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayDouble), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayDouble), i);
	return ll_cas.ll_getDoubleArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayDouble), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArrayDouble(int addr, int i, double v) {
        if (featOkTst && casFeat_arrayDouble == null)
      jcas.throwFeatMissing("arrayDouble", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setDoubleArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayDouble), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayDouble), i);
    ll_cas.ll_setDoubleArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayDouble), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Root_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_arrayInt = jcas.getRequiredFeatureDE(casType, "arrayInt", "uima.cas.IntegerArray", featOkTst);
    casFeatCode_arrayInt  = (null == casFeat_arrayInt) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arrayInt).getCode();

 
    casFeat_arrayRef = jcas.getRequiredFeatureDE(casType, "arrayRef", "uima.cas.FSArray", featOkTst);
    casFeatCode_arrayRef  = (null == casFeat_arrayRef) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arrayRef).getCode();

 
    casFeat_arrayFloat = jcas.getRequiredFeatureDE(casType, "arrayFloat", "uima.cas.FloatArray", featOkTst);
    casFeatCode_arrayFloat  = (null == casFeat_arrayFloat) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arrayFloat).getCode();

 
    casFeat_arrayString = jcas.getRequiredFeatureDE(casType, "arrayString", "uima.cas.StringArray", featOkTst);
    casFeatCode_arrayString  = (null == casFeat_arrayString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arrayString).getCode();

 
    casFeat_plainInt = jcas.getRequiredFeatureDE(casType, "plainInt", "uima.cas.Integer", featOkTst);
    casFeatCode_plainInt  = (null == casFeat_plainInt) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_plainInt).getCode();

 
    casFeat_plainFloat = jcas.getRequiredFeatureDE(casType, "plainFloat", "uima.cas.Float", featOkTst);
    casFeatCode_plainFloat  = (null == casFeat_plainFloat) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_plainFloat).getCode();

 
    casFeat_plainString = jcas.getRequiredFeatureDE(casType, "plainString", "uima.cas.String", featOkTst);
    casFeatCode_plainString  = (null == casFeat_plainString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_plainString).getCode();

 
    casFeat_plainRef = jcas.getRequiredFeatureDE(casType, "plainRef", "aa.Root", featOkTst);
    casFeatCode_plainRef  = (null == casFeat_plainRef) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_plainRef).getCode();

 
    casFeat_plainLong = jcas.getRequiredFeatureDE(casType, "plainLong", "uima.cas.Long", featOkTst);
    casFeatCode_plainLong  = (null == casFeat_plainLong) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_plainLong).getCode();

 
    casFeat_plainDouble = jcas.getRequiredFeatureDE(casType, "plainDouble", "uima.cas.Double", featOkTst);
    casFeatCode_plainDouble  = (null == casFeat_plainDouble) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_plainDouble).getCode();

 
    casFeat_arrayLong = jcas.getRequiredFeatureDE(casType, "arrayLong", "uima.cas.LongArray", featOkTst);
    casFeatCode_arrayLong  = (null == casFeat_arrayLong) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arrayLong).getCode();

 
    casFeat_arrayDouble = jcas.getRequiredFeatureDE(casType, "arrayDouble", "uima.cas.DoubleArray", featOkTst);
    casFeatCode_arrayDouble  = (null == casFeat_arrayDouble) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arrayDouble).getCode();

  }
}



    