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

import org.apache.uima.UIMA_IllegalStateException;

/**
 * A <code>ProcessTrace</code> object keeps a record of events that have occurred and information,
 * such as timing, about those events.
 * <p>
 * Each event is represented by a {@link ProcessTraceEvent} object. Events may have sub-events, so a
 * ProcessTrace is really a forest of events, which provides a useful description of where time is
 * spent during a process involving several components.
 * 
 * 
 */
public interface ProcessTrace extends java.io.Serializable {

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
   * Adds an event with the specified parameters to this <code>ProcessTrace</code>.
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

  /**
   * Adds a completed event object to this <code>ProcessTrace</code>. This method is useful for
   * copying events from one ProcessTrace into another.
   * 
   * @param aEvent
   *          the event object to be added to this <code>ProcessTrace</code>
   */
  public void addEvent(ProcessTraceEvent aEvent);

  /**
   * Adds a list of completed event objects to this <code>ProcessTrace</code>. This method is
   * useful for copying events from one ProcessTrace into another.
   * 
   * @param aEventList
   *          a List of event object to be added to this <code>ProcessTrace</code>
   */
  public void addAll(List<ProcessTraceEvent> aEventList);

  /**
   * Gets a list of {@link ProcessTraceEvent}s, in the order in which they were created. This is
   * generally chronological order.
   * 
   * @return an unmodifiable List of {@link ProcessTraceEvent}s
   */
  public List<ProcessTraceEvent> getEvents();

  /**
   * Gets all events that have the given Component name.
   * 
   * @param aComponentName
   *          the component name to look for
   * @param aRecurseWithinMatch
   *          if true, all events with the given component name will be returned. If false, this
   *          method will not recurse into the sub-events of a matching event.
   * 
   * @return a List of ProcessTraceEvents having the given component name
   */
  public List<ProcessTraceEvent> getEventsByComponentName(String aComponentName, boolean aRecurseWithinMatch);

  /**
   * Gets all events that have the given type
   * 
   * @param aType
   *          the type of event to look for
   * @param aRecurseWithinMatch
   *          if true, all events with the given component name will be returned. If false, this
   *          method will not recurse into the sub-events of a matching event.
   * 
   * @return a List of ProcessTraceEvents having the given type
   */
  public List<ProcessTraceEvent> getEventsByType(String aType, boolean aRecurseWithinMatch);

  /**
   * Get a specified event.
   * 
   * @param aComponentName
   *          name of component producing desired event
   * @param aType
   *          type of desired event
   * 
   * @return the first ProcessTraceEvent matching the parameters, <code>null</code> if there is no
   *         such event.
   */
  public ProcessTraceEvent getEvent(String aComponentName, String aType);

  /**
   * Resets this <code>ProcessTrace</code> by removing all events.
   */
  public void clear();

  /**
   * Aggregates the information in another <code>ProcessTrace</code> with this one. Events that
   * exist in both ProcessTraces will have their durations added together. This method is useful for
   * collecting aggregate performance statistics for collection processing.
   * 
   * @param aProcessTrace
   *          the Process Trace object whose information will be combined with the information in
   *          this object
   */
  public void aggregate(ProcessTrace aProcessTrace);

  /**
   * Generates a user-readable representation of all events in this <code>ProcessTrace</code>.
   * 
   * @return the String representation of all events in this <code>ProcessTrace</code>.
   */
  public String toString();

}
