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
package org.apache.uima.tutorial;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Sun Oct 08 19:34:17 EDT 2017
 * XML source: C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-examples/src/main/descriptors/tutorial/ex6/TutorialTypeSystem.xml
 * @generated */
public class UimaAcronym extends Annotation {

  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "org.apache.uima.tutorial.UimaAcronym";
  
    /**
     * The Constant typeIndexID.
     *
     * @generated 
     * @ordered 
     */
    public static final int typeIndexID = JCasRegistry.register(UimaAcronym.class);

    /**
     * The Constant type.
     *
     * @generated 
     * @ordered 
     */
    public static final int type = typeIndexID;

    /**
     * Gets the type index ID.
     *
     * @return the type index ID
     * @generated 
     */
    @Override
    public int getTypeIndexID() {return typeIndexID;}
 
 
  /* *******************
   *   Feature Offsets *
   * *******************/ 
   
  public final static String _FeatName_expandedForm = "expandedForm";


  /* Feature Adjusted Offsets */
  private final static CallSite _FC_expandedForm = TypeSystemImpl.createCallSite(UimaAcronym.class, "expandedForm");
  private final static MethodHandle _FH_expandedForm = _FC_expandedForm.dynamicInvoker();

   
  /**
   * Never called. Disable default constructor
   *
   * @generated
   */
    protected  UimaAcronym() {/* intentionally empty block */}
    
    /**
     * Internal - constructor used by generator.
     *
     * @param type the type
     * @param casImpl the cas impl
     * @generated 
     */
    public  UimaAcronym(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }
  
    /**
     * Instantiates a new uima acronym.
     *
     * @param jcas the jcas
     * @generated 
     */
    public  UimaAcronym(JCas jcas) {
    super(jcas);
    readObject();   
  } 


    /**
     * Instantiates a new uima acronym.
     *
     * @param jcas the jcas
     * @param begin the begin
     * @param end the end
     */
    public  UimaAcronym(JCas jcas, int begin, int end) {
        super(jcas);
        setBegin(begin);
        setEnd(end);
        readObject();
    }

  /** 
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->*
   * @generated modifiable 
   */
    private void readObject() {
    }

    // *--------------*
    // * Feature: expandedForm
    /**
     * getter for expandedForm - gets.
     *
     * @return the expanded form
     * @generated 
     */
    public String getExpandedForm() { return _getStringValueNc(wrapGetIntCatchException(_FH_expandedForm));}
    
    /**
     * setter for expandedForm - sets.
     *
     * @param v the new expanded form
     * @generated 
     */
    public void setExpandedForm(String v) {
    _setStringValueNfc(wrapGetIntCatchException(_FH_expandedForm), v);
  }    
    
        /**
     *  Custom constructor taking all parameters.
     *
     * @param jcas the jcas
     * @param start the start
     * @param end the end
     * @param expandedForm the expanded form
     */
    public  UimaAcronym(JCas jcas, int start, int end, String expandedForm) {
        super(jcas, start, end);
        setExpandedForm(expandedForm);
    }
}
