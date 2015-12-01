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


/* First created by JCasGen Wed May 23 14:54:19 EDT 2012 */



/* Apache UIMA v3 - First created by JCasGen Tue Dec 01 15:07:57 EST 2015 */

package org.apache.uima.testTypeSystem_arrays;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;


/**
 * Updated by JCasGen Tue Dec 01 15:07:57 EST 2015
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-core/src/test/resources/ExampleCas/testTypeSystem_arrays.xml
 * @generated */
public class OfStrings extends Annotation {
  /** @generated
   * @ordered
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(OfStrings.class);
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


  /* *****************
   *    Local Data   *
   * *****************/

  /* Register Features */
  public final static int _FI_f1Strings = JCasRegistry.registerFeature(typeIndexID);


  private StringArray _F_f1Strings;  //

  /** Never called.  Disable default constructor
   * @generated */
  protected OfStrings() {/* intentionally empty block */}

  /** Internal - constructor used by generator
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure
   */
  public OfStrings(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   */
  public OfStrings(JCas jcas) {
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
  //* Feature: f1Strings

  /** getter for f1Strings - gets
   * @generated
   * @return value of the feature
   */
  public StringArray getF1Strings() { return _F_f1Strings;}

  /** setter for f1Strings - sets
   * @generated
   * @param v value to set into the feature
   */
  public void setF1Strings(StringArray v) {

      _casView.setWithJournalJFRI(this, _FI_f1Strings, () -> _F_f1Strings = v);
      }

  /** indexed getter for f1Strings - gets an indexed value -
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i
   */
  public String getF1Strings(int i) {
     return getF1Strings().get(i);}

  /** indexed setter for f1Strings - sets an indexed value -
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array
   */
  public void setF1Strings(int i, String v) {
    getF1Strings().set(i, v);}
  }


    