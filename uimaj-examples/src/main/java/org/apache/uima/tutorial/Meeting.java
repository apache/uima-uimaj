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
public class Meeting extends Annotation {

    /**
   * @generated
   * @ordered
   */
    public static final int typeIndexID = JCasRegistry.register(Meeting.class);

    /**
   * @generated
   * @ordered
   */
    public static final int type = typeIndexID;

    /** @generated */
    public int getTypeIndexID() {
        return typeIndexID;
    }

    public static final int _FI_room = TypeSystemImpl.getAdjustedFeatureOffset("room");

    public static final int _FI_date = TypeSystemImpl.getAdjustedFeatureOffset("date");

    public static final int _FI_startTime = TypeSystemImpl.getAdjustedFeatureOffset("startTime");

    public static final int _FI_endTime = TypeSystemImpl.getAdjustedFeatureOffset("endTime");

    /**
   * Never called. Disable default constructor
   *
   * @generated
   */
    protected  Meeting() {
    }

    /**
   * Internal - constructor used by generator
   *
   * @generated
   */
    public  Meeting(TypeImpl type, CASImpl casImpl) {
        super(type, casImpl);
        readObject();
    }

    /** @generated */
    public  Meeting(JCas jcas) {
        super(jcas);
        readObject();
    }

    public  Meeting(JCas jcas, int begin, int end) {
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
    }

    // *--------------*
    // * Feature: room
    /**
   * getter for room - gets
   *
   * @generated
   */
    public RoomNumber getRoom() {
        return (RoomNumber) (_getFeatureValueNc(_FI_room));
    }

    /**
   * setter for room - sets
   *
   * @generated
   */
    public void setRoom(RoomNumber v) {
        _setFeatureValueNcWj(_FI_room, v);
    }

    // *--------------*
    // * Feature: date
    /**
   * getter for date - gets
   *
   * @generated
   */
    public DateAnnot getDate() {
        return (DateAnnot) (_getFeatureValueNc(_FI_date));
    }

    /**
   * setter for date - sets
   *
   * @generated
   */
    public void setDate(DateAnnot v) {
        _setFeatureValueNcWj(_FI_date, v);
    }

    // *--------------*
    // * Feature: startTime
    /**
   * getter for startTime - gets
   *
   * @generated
   */
    public TimeAnnot getStartTime() {
        return (TimeAnnot) (_getFeatureValueNc(_FI_startTime));
    }

    /**
   * setter for startTime - sets
   *
   * @generated
   */
    public void setStartTime(TimeAnnot v) {
        _setFeatureValueNcWj(_FI_startTime, v);
    }

    // *--------------*
    // * Feature: endTime
    /**
   * getter for endTime - gets
   *
   * @generated
   */
    public TimeAnnot getEndTime() {
        return (TimeAnnot) (_getFeatureValueNc(_FI_endTime));
    }

    /**
   * setter for endTime - sets
   *
   * @generated
   */
    public void setEndTime(TimeAnnot v) {
        _setFeatureValueNcWj(_FI_endTime, v);
    }

    /** Custom constructor taking all parameters */
    public  Meeting(JCas jcas, int start, int end, RoomNumber room, DateAnnot date, TimeAnnot startTime, TimeAnnot endTime) {
        this(jcas, start, end);
        setRoom(room);
        setDate(date);
        setStartTime(startTime);
        setEndTime(endTime);
    }

    public String toString() {
        return "Meeting in " + getRoom().getCoveredText() + " on " + getDate().getCoveredText() + ", " + getStartTime().getCoveredText() + " - " + getEndTime().getCoveredText();
    }
}
