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
 * Updated by JCasGen Sun Oct 08 19:34:17 EDT 2017 XML source:
 * C:/au/svnCheckouts/uv3/trunk/uimaj-v3/uimaj-examples/src/main/descriptors/tutorial/ex6/TutorialTypeSystem.xml
 * 
 * @generated
 */
public class Meeting extends Annotation {

  /**
   * @generated
   * @ordered
   */
  @SuppressWarnings("hiding")
  public final static String _TypeName = "org.apache.uima.tutorial.Meeting";

  /**
   * The Constant typeIndexID.
   *
   * @generated
   * @ordered
   */
  public static final int typeIndexID = JCasRegistry.register(Meeting.class);

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
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /*
   * ******************* Feature Offsets *
   *******************/

  public final static String _FeatName_room = "room";
  public final static String _FeatName_date = "date";
  public final static String _FeatName_startTime = "startTime";
  public final static String _FeatName_endTime = "endTime";

  /* Feature Adjusted Offsets */
  private final static CallSite _FC_room = TypeSystemImpl.createCallSite(Meeting.class, "room");
  private final static MethodHandle _FH_room = _FC_room.dynamicInvoker();
  private final static CallSite _FC_date = TypeSystemImpl.createCallSite(Meeting.class, "date");
  private final static MethodHandle _FH_date = _FC_date.dynamicInvoker();
  private final static CallSite _FC_startTime = TypeSystemImpl.createCallSite(Meeting.class,
          "startTime");
  private final static MethodHandle _FH_startTime = _FC_startTime.dynamicInvoker();
  private final static CallSite _FC_endTime = TypeSystemImpl.createCallSite(Meeting.class,
          "endTime");
  private final static MethodHandle _FH_endTime = _FC_endTime.dynamicInvoker();

  /**
   * Never called. Disable default constructor
   *
   * @generated
   */
  protected Meeting() {
    /* intentionally empty block */}

  /**
   * Internal - constructor used by generator.
   *
   * @param type
   *          the type
   * @param casImpl
   *          the cas impl
   * @generated
   */
  public Meeting(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }

  /**
   * Instantiates a new meeting.
   *
   * @param jcas
   *          the jcas
   * @generated
   */
  public Meeting(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * Instantiates a new meeting.
   *
   * @param jcas
   *          the jcas
   * @param begin
   *          the begin
   * @param end
   *          the end
   */
  public Meeting(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->*
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

  // *--------------*
  // * Feature: room
  /**
   * getter for room - gets.
   *
   * @return the room
   * @generated
   */
  public RoomNumber getRoom() {
    return (RoomNumber) (_getFeatureValueNc(wrapGetIntCatchException(_FH_room)));
  }

  /**
   * setter for room - sets.
   *
   * @param v
   *          the new room
   * @generated
   */
  public void setRoom(RoomNumber v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_room), v);
  }

  // *--------------*
  // * Feature: date
  /**
   * getter for date - gets.
   *
   * @return the date
   * @generated
   */
  public DateAnnot getDate() {
    return (DateAnnot) (_getFeatureValueNc(wrapGetIntCatchException(_FH_date)));
  }

  /**
   * setter for date - sets.
   *
   * @param v
   *          the new date
   * @generated
   */
  public void setDate(DateAnnot v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_date), v);
  }

  // *--------------*
  // * Feature: startTime
  /**
   * getter for startTime - gets.
   *
   * @return the start time
   * @generated
   */
  public TimeAnnot getStartTime() {
    return (TimeAnnot) (_getFeatureValueNc(wrapGetIntCatchException(_FH_startTime)));
  }

  /**
   * setter for startTime - sets.
   *
   * @param v
   *          the new start time
   * @generated
   */
  public void setStartTime(TimeAnnot v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_startTime), v);
  }

  // *--------------*
  // * Feature: endTime
  /**
   * getter for endTime - gets.
   *
   * @return the end time
   * @generated
   */
  public TimeAnnot getEndTime() {
    return (TimeAnnot) (_getFeatureValueNc(wrapGetIntCatchException(_FH_endTime)));
  }

  /**
   * setter for endTime - sets.
   *
   * @param v
   *          the new end time
   * @generated
   */
  public void setEndTime(TimeAnnot v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_endTime), v);
  }

  /**
   * Custom constructor taking all parameters.
   *
   * @param jcas
   *          the jcas
   * @param start
   *          the start
   * @param end
   *          the end
   * @param room
   *          the room
   * @param date
   *          the date
   * @param startTime
   *          the start time
   * @param endTime
   *          the end time
   */
  public Meeting(JCas jcas, int start, int end, RoomNumber room, DateAnnot date,
          TimeAnnot startTime, TimeAnnot endTime) {
    this(jcas, start, end);
    setRoom(room);
    setDate(date);
    setStartTime(startTime);
    setEndTime(endTime);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.FeatureStructureImplC#toString()
   */
  @Override
  public String toString() {
    return "Meeting in " + getRoom().getCoveredText() + " on " + getDate().getCoveredText() + ", "
            + getStartTime().getCoveredText() + " - " + getEndTime().getCoveredText();
  }
}
