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

/* Apache UIMA v3 - First created by JCasGen Mon Oct 02 16:32:10 EDT 2017 */

package org.apache.uima.jcas.tcas;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;

/**
 * Updated by JCasGen Mon Oct 02 16:32:10 EDT 2017 XML source:
 * C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-document-annotation/src/test/resources/ExampleCas/testTypeSystem_docmetadata.xml
 * 
 * @generated
 */
public class DocMeta extends DocumentAnnotation {

  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static String _TypeName = "org.apache.uima.jcas.tcas.DocMeta";

  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static int typeIndexID = JCasRegistry.register(DocMeta.class);
  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static int type = typeIndexID;

  /**
   * @generated
   * @return index of the type
   */
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /*
   * ******************* Feature Offsets *
   *******************/

  public final static String _FeatName_feat = "feat";
  public final static String _FeatName_feat2 = "feat2";
  public final static String _FeatName_feat3 = "feat3";
  public final static String _FeatName_arraystr = "arraystr";
  public final static String _FeatName_arrayints = "arrayints";
  public final static String _FeatName_arrayFs = "arrayFs";

  /* Feature Adjusted Offsets */
  private final static CallSite _FC_feat = TypeSystemImpl.createCallSite(DocMeta.class, "feat");
  private final static MethodHandle _FH_feat = _FC_feat.dynamicInvoker();
  private final static CallSite _FC_feat2 = TypeSystemImpl.createCallSite(DocMeta.class, "feat2");
  private final static MethodHandle _FH_feat2 = _FC_feat2.dynamicInvoker();
  private final static CallSite _FC_feat3 = TypeSystemImpl.createCallSite(DocMeta.class, "feat3");
  private final static MethodHandle _FH_feat3 = _FC_feat3.dynamicInvoker();
  private final static CallSite _FC_arraystr = TypeSystemImpl.createCallSite(DocMeta.class,
          "arraystr");
  private final static MethodHandle _FH_arraystr = _FC_arraystr.dynamicInvoker();
  private final static CallSite _FC_arrayints = TypeSystemImpl.createCallSite(DocMeta.class,
          "arrayints");
  private final static MethodHandle _FH_arrayints = _FC_arrayints.dynamicInvoker();
  private final static CallSite _FC_arrayFs = TypeSystemImpl.createCallSite(DocMeta.class,
          "arrayFs");
  private final static MethodHandle _FH_arrayFs = _FC_arrayFs.dynamicInvoker();

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected DocMeta() {
    /* intentionally empty block */}

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   * @param casImpl
   *          the CAS this Feature Structure belongs to
   * @param type
   *          the type of this Feature Structure
   */
  public DocMeta(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }

  /**
   * @generated
   * @param jcas
   *          JCas to which this Feature Structure belongs
   */
  public DocMeta(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * @generated
   * @param jcas
   *          JCas to which this Feature Structure belongs
   * @param begin
   *          offset to the begin spot in the SofA
   * @param end
   *          offset to the end spot in the SofA
   */
  public DocMeta(JCas jcas, int begin, int end) {
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
    /* default - does nothing empty block */}

  // *--------------*
  // * Feature: feat

  /**
   * getter for feat - gets
   * 
   * @generated
   * @return value of the feature
   */
  public String getFeat() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_feat));
  }

  /**
   * setter for feat - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setFeat(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_feat), v);
  }

  // *--------------*
  // * Feature: feat2

  /**
   * getter for feat2 - gets
   * 
   * @generated
   * @return value of the feature
   */
  public String getFeat2() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_feat2));
  }

  /**
   * setter for feat2 - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setFeat2(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_feat2), v);
  }

  // *--------------*
  // * Feature: feat3

  /**
   * getter for feat3 - gets
   * 
   * @generated
   * @return value of the feature
   */
  public String getFeat3() {
    return _getStringValueNc(wrapGetIntCatchException(_FH_feat3));
  }

  /**
   * setter for feat3 - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setFeat3(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_feat3), v);
  }

  // *--------------*
  // * Feature: arraystr

  /**
   * getter for arraystr - gets
   * 
   * @generated
   * @return value of the feature
   */
  public StringArray getArraystr() {
    return (StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arraystr)));
  }

  /**
   * setter for arraystr - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setArraystr(StringArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arraystr), v);
  }

  /**
   * indexed getter for arraystr - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public String getArraystr(int i) {
    return ((StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arraystr)))).get(i);
  }

  /**
   * indexed setter for arraystr - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setArraystr(int i, String v) {
    ((StringArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arraystr)))).set(i, v);
  }

  // *--------------*
  // * Feature: arrayints

  /**
   * getter for arrayints - gets
   * 
   * @generated
   * @return value of the feature
   */
  public IntegerArray getArrayints() {
    return (IntegerArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayints)));
  }

  /**
   * setter for arrayints - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setArrayints(IntegerArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arrayints), v);
  }

  /**
   * indexed getter for arrayints - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public int getArrayints(int i) {
    return ((IntegerArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayints)))).get(i);
  }

  /**
   * indexed setter for arrayints - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setArrayints(int i, int v) {
    ((IntegerArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayints)))).set(i, v);
  }

  // *--------------*
  // * Feature: arrayFs

  /**
   * getter for arrayFs - gets
   * 
   * @generated
   * @return value of the feature
   */
  public FSArray getArrayFs() {
    return (FSArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayFs)));
  }

  /**
   * setter for arrayFs - sets
   * 
   * @generated
   * @param v
   *          value to set into the feature
   */
  public void setArrayFs(FSArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_arrayFs), v);
  }

  /**
   * indexed getter for arrayFs - gets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to get
   * @return value of the element at index i
   */
  public Annotation getArrayFs(int i) {
    return (Annotation) (((FSArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayFs))))
            .get(i));
  }

  /**
   * indexed setter for arrayFs - sets an indexed value -
   * 
   * @generated
   * @param i
   *          index in the array to set
   * @param v
   *          value to set into the array
   */
  public void setArrayFs(int i, Annotation v) {
    ((FSArray) (_getFeatureValueNc(wrapGetIntCatchException(_FH_arrayFs)))).set(i, v);
  }
}
