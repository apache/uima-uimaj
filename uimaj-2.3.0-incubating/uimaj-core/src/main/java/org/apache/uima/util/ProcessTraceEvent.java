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

import java.util.List;

/**
 * Captures information, including timing, about an event that occurred during processing. Events
 * may have sub-events that further break down the steps involved in a complex process.
 * 
 * 
 */
public interface ProcessTraceEvent extends java.io.Serializable {

  /**
   * Retrieves the name of the component that is performing this event.
   * 
   * @return the component name
   */
  public String getComponentName();

  /**
   * Gets the type of event. Standard values for this property are defined as constants on this
   * interface, but any String is allowed.
   * 
   * @return the event type
   */
  public String getType();

  /**
   * Retrieves the description of this event.
   * 
   * @return the event Description
   */
  public String getDescription();

  /**
   * Gets the duration of this event.
   * 
   * @return the duration of this event, in milliseconds.
   */
  public int getDuration();

  /**
   * Retrieves the result message of this event.
   * 
   * @return the event's result message
   */
  public String getResultMessage();

  /**
   * Gets the sub-events of this event.
   * 
   * @return a List containing other <code>ProcessTraceEvent</code> objects
   */
  public List<ProcessTraceEvent> getSubEvents();

  /**
   * Gets the duration of this event, minus the sum of the durations of its direct sub-events.
   * 
   * @return the duration of this event in milliseconds, excluding the time spent in its sub-events
   */
  public int getDurationExcludingSubEvents();

  /**
   * Generates a user-readable representation of this event and its sub-events.
   * 
   * @return the String representation of this event and its sub-events
   */
  public String toString();

  /**
   * Generates a user-readable representation of this event and its subevents, using the given
   * indentation level and writing to a StringBuffer. This is useful for writing nested events.
   * 
   * @param aBuf
   *          string buffer to add to
   * @param aIndentLevel
   *          indentation level
   */
  public void toString(StringBuffer aBuf, int aIndentLevel);

  /**
   * Generates a user-readable representation of this event and its subevents, using the given
   * indentation level and writing to a StringBuffer. Also, if the total time for all events is
   * known, this method will print the percentage of time used by this event and its subevents.
   * 
   * @param aBuf
   *          string buffer to add to
   * @param aIndentLevel
   *          indentation level
   * @param aTotalTime
   *          total time, used to calculate percentags. If not known, pass 0.
   */
  public void toString(StringBuffer aBuf, int aIndentLevel, int aTotalTime);

  /**
   * Constant for the ANALYSIS_ENGINE event type. This represents the time spent in the Analysis
   * Engine, including the annotator's analysis and framework overhead.
   */
  public static final String ANALYSIS_ENGINE = "ANALYSIS_ENGINE";

  /**
   * Constant for the ANALYSIS event type. This represents the actual analysis performed by an
   * annotator.
   */
  public static final String ANALYSIS = "ANALYSIS";

  /**
   * Constant for the SERVICE event type. This represents the total time spent in execution of a
   * remote service (not including communication and marshalling/unmarshalling overhead).
   */
  public static final String SERVICE = "SERVICE";

  /**
   * Constant for the SERVICE_CALL event type. This represents the total time spent making a call on
   * a remote service, including marshalling and unmarshalling.
   */
  public static final String SERVICE_CALL = "SERVICE_CALL";
}
