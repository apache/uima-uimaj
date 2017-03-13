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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;


/**
 * Updated by JCasGen Mon Nov 29 15:02:38 EST 2004 XML source: C:/Program
 * Files/apache/uima/examples/descriptors/tutorial/ex6/TutorialTypeSystem.xml
 *
 * @generated
 */
public class DateTimeAnnot extends Annotation {

    /**
     * The Constant typeIndexID.
     *
     * @generated 
     * @ordered 
     */
    public static final int typeIndexID = JCasRegistry.register(DateTimeAnnot.class);

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
    public int getTypeIndexID() {
        return typeIndexID;
    }

    /** The Constant _FI_shortDateString. */
    public static final int _FI_shortDateString = TypeSystemImpl.getAdjustedFeatureOffset("shortDateString");

    /**
   * Never called. Disable default constructor
   *
   * @generated
   */
    protected  DateTimeAnnot() {
    }

    /**
     * Internal - constructor used by generator.
     *
     * @param type the type
     * @param casImpl the cas impl
     * @generated 
     */
    public  DateTimeAnnot(TypeImpl type, CASImpl casImpl) {
        super(type, casImpl);
        readObject();
    }

    /**
     * Instantiates a new date time annot.
     *
     * @param jcas the jcas
     * @generated 
     */
    public  DateTimeAnnot(JCas jcas) {
        super(jcas);
        readObject();
    }

    /**
     * Instantiates a new date time annot.
     *
     * @param jcas the jcas
     * @param begin the begin
     * @param end the end
     */
    public  DateTimeAnnot(JCas jcas, int begin, int end) {
        super(jcas);
        setBegin(begin);
        setEnd(end);
        readObject();
    }

    /**
     * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->.
     *
     * @generated modifiable
     */
    private void readObject() {
    }

    // *--------------*
    // * Feature: shortDateString
    /**
     * getter for shortDateString - gets.
     *
     * @return the short date string
     * @generated 
     */
    public String getShortDateString() {
        return _getStringValueNc(_FI_shortDateString);
    }

    /**
     * setter for shortDateString - sets.
     *
     * @param v the new short date string
     * @generated 
     */
    public void setShortDateString(String v) {
        _setStringValueNfc(_FI_shortDateString, v);
    }
}
