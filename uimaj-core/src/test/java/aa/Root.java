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
// @formatter:off
/* Apache UIMA v3 - First created by JCasGen Sun Oct 08 19:06:27 EDT 2017 */

package aa;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Sun Oct 08 19:06:27 EDT 2017
 * XML source: C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-core/src/test/java/org/apache/uima/jcas/test/generatedx.xml
 * @generated */
public class Root extends TOP {
 
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "aa.Root";
  
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
 
 
  /* *******************
   *   Feature Offsets *
   * *******************/ 
   
  public final static String _FeatName_arrayInt = "arrayInt";
  public final static String _FeatName_arrayRef = "arrayRef";
  public final static String _FeatName_arrayFloat = "arrayFloat";
  public final static String _FeatName_arrayString = "arrayString";
  public final static String _FeatName_plainInt = "plainInt";
  public final static String _FeatName_plainFloat = "plainFloat";
  public final static String _FeatName_plainString = "plainString";
  public final static String _FeatName_plainRef = "plainRef";
  public final static String _FeatName_plainLong = "plainLong";
  public final static String _FeatName_plainDouble = "plainDouble";
  public final static String _FeatName_arrayLong = "arrayLong";
  public final static String _FeatName_arrayDouble = "arrayDouble";


  /* Feature Adjusted Offsets */
  private final static CallSite _FC_arrayInt = TypeSystemImpl.createCallSite(Root.class, "arrayInt");
  private final static MethodHandle _FH_arrayInt = _FC_arrayInt.dynamicInvoker();
  private final static CallSite _FC_arrayRef = TypeSystemImpl.createCallSite(Root.class, "arrayRef");
  private final static MethodHandle _FH_arrayRef = _FC_arrayRef.dynamicInvoker();
  private final static CallSite _FC_arrayFloat = TypeSystemImpl.createCallSite(Root.class, "arrayFloat");
  private final static MethodHandle _FH_arrayFloat = _FC_arrayFloat.dynamicInvoker();
  private final static CallSite _FC_arrayString = TypeSystemImpl.createCallSite(Root.class, "arrayString");
  private final static MethodHandle _FH_arrayString = _FC_arrayString.dynamicInvoker();
  private final static CallSite _FC_plainInt = TypeSystemImpl.createCallSite(Root.class, "plainInt");
  private final static MethodHandle _FH_plainInt = _FC_plainInt.dynamicInvoker();
  private final static CallSite _FC_plainFloat = TypeSystemImpl.createCallSite(Root.class, "plainFloat");
  private final static MethodHandle _FH_plainFloat = _FC_plainFloat.dynamicInvoker();
  private final static CallSite _FC_plainString = TypeSystemImpl.createCallSite(Root.class, "plainString");
  private final static MethodHandle _FH_plainString = _FC_plainString.dynamicInvoker();
  private final static CallSite _FC_plainRef = TypeSystemImpl.createCallSite(Root.class, "plainRef");
  private final static MethodHandle _FH_plainRef = _FC_plainRef.dynamicInvoker();
  private final static CallSite _FC_plainLong = TypeSystemImpl.createCallSite(Root.class, "plainLong");
  private final static MethodHandle _FH_plainLong = _FC_plainLong.dynamicInvoker();
  private final static CallSite _FC_plainDouble = TypeSystemImpl.createCallSite(Root.class, "plainDouble");
  private final static MethodHandle _FH_plainDouble = _FC_plainDouble.dynamicInvoker();
  private final static CallSite _FC_arrayLong = TypeSystemImpl.createCallSite(Root.class, "arrayLong");
  private final static MethodHandle _FH_arrayLong = _FC_arrayLong.dynamicInvoker();
  private final static CallSite _FC_arrayDouble = TypeSystemImpl.createCallSite(Root.class, "arrayDouble");
  private final static MethodHandle _FH_arrayDouble = _FC_arrayDouble.dynamicInvoker();

   
  /** Never called.  Disable default constructor
   * @generated */
  protected Root() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public Root(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
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
  public IntegerArray getArrayInt() { return (IntegerArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayInt)));}
    
  /** setter for arrayInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayInt(IntegerArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arrayInt), v);
  }    
    
    
  /** indexed getter for arrayInt - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public int getArrayInt(int i) {
     return ((IntegerArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayInt)))).get(i);} 

  /** indexed setter for arrayInt - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayInt(int i, int v) {
    ((IntegerArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayInt)))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: arrayRef

  /** getter for arrayRef - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getArrayRef() { return (FSArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayRef)));}
    
  /** setter for arrayRef - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayRef(FSArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arrayRef), v);
  }    
    
    
  /** indexed getter for arrayRef - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public TOP getArrayRef(int i) {
     return (TOP)(((FSArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayRef)))).get(i));} 

  /** indexed setter for arrayRef - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayRef(int i, TOP v) {
    ((FSArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayRef)))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: arrayFloat

  /** getter for arrayFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public FloatArray getArrayFloat() { return (FloatArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayFloat)));}
    
  /** setter for arrayFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayFloat(FloatArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arrayFloat), v);
  }    
    
    
  /** indexed getter for arrayFloat - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public float getArrayFloat(int i) {
     return ((FloatArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayFloat)))).get(i);} 

  /** indexed setter for arrayFloat - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayFloat(int i, float v) {
    ((FloatArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayFloat)))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: arrayString

  /** getter for arrayString - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getArrayString() { return (StringArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayString)));}
    
  /** setter for arrayString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayString(StringArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arrayString), v);
  }    
    
    
  /** indexed getter for arrayString - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getArrayString(int i) {
     return ((StringArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayString)))).get(i);} 

  /** indexed setter for arrayString - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayString(int i, String v) {
    ((StringArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayString)))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: plainInt

  /** getter for plainInt - gets 
   * @generated
   * @return value of the feature 
   */
  public int getPlainInt() { return _getIntValueNc(wrapGetIntCatchException(_FH_plainInt));}
    
  /** setter for plainInt - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainInt(int v) {
    _setIntValueNfc(wrapGetIntCatchException(_FH_plainInt), v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainFloat

  /** getter for plainFloat - gets 
   * @generated
   * @return value of the feature 
   */
  public float getPlainFloat() { return _getFloatValueNc(wrapGetIntCatchException(_FH_plainFloat));}
    
  /** setter for plainFloat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainFloat(float v) {
    _setFloatValueNfc(wrapGetIntCatchException(_FH_plainFloat), v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainString

  /** getter for plainString - gets 
   * @generated
   * @return value of the feature 
   */
  public String getPlainString() { return _getStringValueNc(wrapGetIntCatchException(_FH_plainString));}
    
  /** setter for plainString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainString(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_plainString), v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainRef

  /** getter for plainRef - gets TokenType testMissingImport;
   * @generated
   * @return value of the feature 
   */
  public Root getPlainRef() { return (Root)(_getFeatureValueNc(wrapGetIntCatchException(_FH_plainRef)));}
    
  /** setter for plainRef - sets TokenType testMissingImport; 
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainRef(Root v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_plainRef), v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainLong

  /** getter for plainLong - gets 
   * @generated
   * @return value of the feature 
   */
  public long getPlainLong() { return _getLongValueNc(wrapGetIntCatchException(_FH_plainLong));}
    
  /** setter for plainLong - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainLong(long v) {
    _setLongValueNfc(wrapGetIntCatchException(_FH_plainLong), v);
  }    
    
   
    
  //*--------------*
  //* Feature: plainDouble

  /** getter for plainDouble - gets 
   * @generated
   * @return value of the feature 
   */
  public double getPlainDouble() { return _getDoubleValueNc(wrapGetIntCatchException(_FH_plainDouble));}
    
  /** setter for plainDouble - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPlainDouble(double v) {
    _setDoubleValueNfc(wrapGetIntCatchException(_FH_plainDouble), v);
  }    
    
   
    
  //*--------------*
  //* Feature: arrayLong

  /** getter for arrayLong - gets 
   * @generated
   * @return value of the feature 
   */
  public LongArray getArrayLong() { return (LongArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayLong)));}
    
  /** setter for arrayLong - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayLong(LongArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arrayLong), v);
  }    
    
    
  /** indexed getter for arrayLong - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public long getArrayLong(int i) {
     return ((LongArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayLong)))).get(i);} 

  /** indexed setter for arrayLong - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayLong(int i, long v) {
    ((LongArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayLong)))).set(i, v);
  }  
   
    
  //*--------------*
  //* Feature: arrayDouble

  /** getter for arrayDouble - gets 
   * @generated
   * @return value of the feature 
   */
  public DoubleArray getArrayDouble() { return (DoubleArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayDouble)));}
    
  /** setter for arrayDouble - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayDouble(DoubleArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arrayDouble), v);
  }    
    
    
  /** indexed getter for arrayDouble - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public double getArrayDouble(int i) {
     return ((DoubleArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayDouble)))).get(i);} 

  /** indexed setter for arrayDouble - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayDouble(int i, double v) {
    ((DoubleArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayDouble)))).set(i, v);
  }  
  }
