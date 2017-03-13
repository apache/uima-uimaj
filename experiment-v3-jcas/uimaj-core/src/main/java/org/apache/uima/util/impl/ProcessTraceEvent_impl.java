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

package org.apache.uima.util.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.uima.util.ProcessTraceEvent;

/**
 * Reference implementation of {@link ProcessTraceEvent}.
 * 
 * 
 */
public class ProcessTraceEvent_impl implements ProcessTraceEvent {

  private static final long serialVersionUID = 4275351517280216988L;

  /**
   * Component Name for this event.
   */
  private String mComponentName;

  /**
   * Type of this event.
   */
  private String mType;

  /**
   * Description of this event.
   */
  private String mDescription;

  /**
   * Duration of this event in milliseconds.
   */
  private int mDuration;

  /**
   * Result Message of this event.
   */
  private String mResultMessage;

  /**
   * List of sub-events of this event. (Initialized lazily.)
   */
  private List<ProcessTraceEvent> mSubEvents;

  /**
   * Start time of this event.
   */
  private long mStartTime;

  /**
   * Creates a new ProcessTraceEvent_impl with null property values.
   */
  public ProcessTraceEvent_impl() {
  }

  /**
   * Creates a new ProcessTraceEvent_impl and sets the Component name, type, and description
   * properties.
   * 
   * @param aComponentName
   *          name of Component generating this event
   * @param aType
   *          type of event. Standard event types are defined as constants on the
   *          {@link ProcessTraceEvent} interface, but any string is allowed.
   * @param aDescription
   *          description of event
   */
  public ProcessTraceEvent_impl(String aComponentName, String aType, String aDescription) {
    mComponentName = aComponentName;
    mType = aType;
    mDescription = aDescription;
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#getComponentName()
   */
  public String getComponentName() {
    return mComponentName;
  }

  /**
   * @param aName the component name for this event
   */
  public void setComponentName(String aName) {
    mComponentName = aName;
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#getType()
   */
  public String getType() {
    return mType;
  }

  /**
   * @param aType the type of this event
   */
  public void setType(String aType) {
    mType = aType;
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @param aDescription the description for this event
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#getDuration()
   */
  public int getDuration() {
    return mDuration;
  }

  /**
   * @param aDuration the duration for this event
   */
  public void setDuration(int aDuration) {
    mDuration = aDuration;
  }

  /**
   * @param aAdditionalDuration Adds this to the duration of this event
   */
  public void addToDuration(long aAdditionalDuration) {
    mDuration += aAdditionalDuration;
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#getResultMessage()
   */
  public String getResultMessage() {
    return mResultMessage;
  }

  /**
   * @param aResultMessage the Result Message for this event
   */
  public void setResultMessage(String aResultMessage) {
    mResultMessage = aResultMessage;
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#getSubEvents()
   */
  public List<ProcessTraceEvent> getSubEvents() {
    if (mSubEvents == null) {
      return Collections.emptyList();
    } else {
      return mSubEvents;
    }
  }

  /**
   * @param aEvent Adds this sub-event to this event.
   */
  public void addSubEvent(ProcessTraceEvent aEvent) {
    if (mSubEvents == null) {
      mSubEvents = new ArrayList<ProcessTraceEvent>();
    }
    mSubEvents.add(aEvent);
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#toString()
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    toString(buf, 0);
    return buf.toString();
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#toString(StringBuffer,int)
   */
  public void toString(StringBuffer aBuf, int aIndentLevel) {
    toString(aBuf, aIndentLevel, 0);
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#toString(java.lang.StringBuffer, int, int)
   */
  public void toString(StringBuffer aBuf, int aIndentLevel, int aTotalTime) {
    final DecimalFormat pctFmt = new DecimalFormat("##.##%");

    writeTabs(aIndentLevel, aBuf);
    aBuf.append("Component Name: ").append(getComponentName()).append('\n');
    writeTabs(aIndentLevel, aBuf);
    aBuf.append("Event Type: ").append(getType()).append('\n');
    if (getDescription() != null && getDescription().length() > 0) {
      writeTabs(aIndentLevel, aBuf);
      aBuf.append("Description: ").append(getDescription()).append('\n');
    }

    int duration = getDuration();
    if (duration >= 0) {
      writeTabs(aIndentLevel, aBuf);
      aBuf.append("Duration: ").append(duration).append("ms");
      if (aTotalTime > 0) {
        double pct = (double) duration / aTotalTime;
        String pctStr = pctFmt.format(pct);
        aBuf.append(" (").append(pctStr).append(')');
      }
      aBuf.append('\n');
    }

    if (getResultMessage() != null && getResultMessage().length() > 0) {
      writeTabs(aIndentLevel, aBuf);
      aBuf.append("Result: ").append(getResultMessage()).append('\n');
    }

    List<ProcessTraceEvent> subEvents = getSubEvents();
    if (!subEvents.isEmpty()) {
      writeTabs(aIndentLevel, aBuf);
      aBuf.append("Sub-events:").append('\n');

      for (ProcessTraceEvent evt : subEvents) {
        evt.toString(aBuf, aIndentLevel + 1, aTotalTime);
        aBuf.append('\n');
      }
    }
  }

  /**
   * @see org.apache.uima.util.ProcessTraceEvent#getDurationExcludingSubEvents()
   */
  public int getDurationExcludingSubEvents() {
    int result = getDuration();
    for (ProcessTraceEvent evt : getSubEvents()) {
      result -= evt.getDuration();
    }
    return result;
  }

  public long getStartTime() {
    return mStartTime;
  }

  public void setStartTime(long aStartTime) {
    mStartTime = aStartTime;
  }

  /**
   * Writes tabs to a StringBuffer
   * 
   * @param aNumTabs
   *          number of tabs to print
   * @param aBuf
   *          the buffer to write to
   */
  protected void writeTabs(int aNumTabs, StringBuffer aBuf) {
    for (int i = 0; i < aNumTabs; i++) {
      aBuf.append('\t');
    }
  }
}
