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

package org.apache.uima.util;

import org.apache.uima.UIMA_IllegalStateException;

/**
 * The <code>InstrumentationFacility</code> interface defines a standard way for UIMA components
 * to be instrumented for performance evaluation. The UIMA framework will provide each UIMA
 * component with access to an implementation of this interface.
 * 
 * This interface is under development. In its current form, it basically just provides a write-only
 * interface to a {@link ProcessTrace} object.
 * 
 * 
 */
public interface InstrumentationFacility {
  /**
   * Records the start of an event. The event will be ended when there is a corresponding call to
   * {@link #endEvent(String,String,String)} with the same component name and event type. The
   * duration of the event will be automatically computed from the difference in time between the
   * start and end.
   * 
   * @param aComponentName
   *          name of the component generating the event
   * @param aEventType
   *          type of the event. Standard types are defined as constants on the
   *          {@link ProcessTraceEvent} interface, but any string may be used.
   * @param aDescription
   *          description of the event
   */
  public void startEvent(String aComponentName, String aEventType, String aDescription);

  /**
   * Records the end of an event. The event is identified by the component name and type. If there
   * is no open event that matches those values, a <code>UIMA_IllegalStateException</code> will be
   * thrown.
   * 
   * @param aComponentName
   *          name of the component generating the event
   * @param aEventType
   *          type of the event. Standard types are defined as constants on the
   *          {@link ProcessTraceEvent} interface, but any string may be used.
   * @param aResultMessage
   *          describes the result of the event
   * 
   * @throws UIMA_IllegalStateException
   *           if there is no open event matching the <code>aComponentName</code> and
   *           <code>aEventType</code> arguments.
   */
  public void endEvent(String aComponentName, String aEventType, String aResultMessage);

  /**
   * Records a completed event with the specified parameters.
   * 
   * @param aResourceName
   *          name of the component generating the event
   * @param aType
   *          type of the event. Standard types are defined as constants on the
   *          {@link ProcessTraceEvent} interface, but any string may be used.
   * @param aDescription
   *          description of the event
   * @param aDuration
   *          duration of the event in milliseconds
   * @param aResultMsg
   *          result message of event
   */
  public void addEvent(String aResourceName, String aType, String aDescription, int aDuration,
          String aResultMsg);
}
