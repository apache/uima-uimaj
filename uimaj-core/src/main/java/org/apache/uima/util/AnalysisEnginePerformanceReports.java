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

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class that is useful for generating an Analysis Engine performance report from a
 * {@link ProcessTrace} object.
 * 
 * 
 */
public class AnalysisEnginePerformanceReports {

  private ProcessTrace mProcessTrace;

  private Map<String, Integer> mAnnotatorAnalysisTimes = new HashMap<String, Integer>();

  private int mAnalysisTime = 0;

  private int mFrameworkOverhead = 0;

  private int mServiceWrapperOverhead = 0;

  private int mServiceCallOverhead = 0;

  private int mTotalTime = 0;

  private NumberFormat pctFormat;

  public AnalysisEnginePerformanceReports(ProcessTrace aProcessTrace) {
    mProcessTrace = aProcessTrace;

    pctFormat = NumberFormat.getPercentInstance();
    pctFormat.setMaximumFractionDigits(2);

    for (ProcessTraceEvent evt : aProcessTrace.getEvents()) {
      if (ProcessTraceEvent.ANALYSIS_ENGINE.equals(evt.getType())
              || ProcessTraceEvent.SERVICE_CALL.equals(evt.getType())) {
        mTotalTime += evt.getDuration();
      }
      addEventData(evt);
    }
  }

  protected void addEventData(ProcessTraceEvent aEvent) {
    // add results to report
    if (ProcessTraceEvent.ANALYSIS.equals(aEvent.getType())) {
      mAnalysisTime += aEvent.getDuration();
      String componentName = aEvent.getComponentName();
      mAnnotatorAnalysisTimes.put(componentName, aEvent.getDuration());
    } else if (ProcessTraceEvent.ANALYSIS_ENGINE.equals(aEvent.getType())) {
      // framework overhead is difference between this event's duration and
      // combined duration of all ANALYSIS or ANALYSIS_ENGINE subevents
      final String[] subEventTypes = new String[] { ProcessTraceEvent.ANALYSIS,
          ProcessTraceEvent.ANALYSIS_ENGINE };
      int duration = aEvent.getDuration();
      int subEventDuration = getSubEventDuration(aEvent, subEventTypes);
      if (subEventDuration > 0) {
        mFrameworkOverhead += (duration - subEventDuration);
      }
    } else if (ProcessTraceEvent.SERVICE.equals(aEvent.getType())) {
      // service wrapper overhead is difference between this event's duration
      // and duration of contained ANALYSIS or ANALYSIS_ENGINE subevents
      final String[] subEventTypes = new String[] { ProcessTraceEvent.ANALYSIS,
          ProcessTraceEvent.ANALYSIS_ENGINE };
      int duration = aEvent.getDuration();
      int subEventDuration = getSubEventDuration(aEvent, subEventTypes);
      if (subEventDuration > 0) {
        mServiceWrapperOverhead += (duration - subEventDuration);
      }
    } else if (ProcessTraceEvent.SERVICE_CALL.equals(aEvent.getType())) {
      // service call overhead is difference between this event's duration
      // and duration of contained SERVICE, ANALYSIS or ANALYSIS_ENGINE subevents
      final String[] subEventTypes = new String[] { ProcessTraceEvent.SERVICE,
          ProcessTraceEvent.ANALYSIS, ProcessTraceEvent.ANALYSIS_ENGINE };
      int duration = aEvent.getDuration();
      int subEventDuration = getSubEventDuration(aEvent, subEventTypes);
      if (subEventDuration > 0) {
        mServiceCallOverhead += (duration - subEventDuration);
      }
    }

    for (ProcessTraceEvent subEvt : aEvent.getSubEvents()) {
      addEventData(subEvt);
    }
  }

  public int getTotalTime() {
    return mTotalTime;
  }

  public int getAnalysisTime() {
    return mAnalysisTime;
  }

  public int getFrameworkOverhead() {
    return mFrameworkOverhead;
  }

  public int getServiceWrapperOverhead() {
    return mServiceWrapperOverhead;
  }

  public int getServiceCallOverhead() {
    return mServiceCallOverhead;
  }

  public String getFullReport() {
    return mProcessTrace.toString();
  }

  public String toString() {
    int total = getTotalTime();
    int analysis = getAnalysisTime();
    int frameworkOver = getFrameworkOverhead();
    int serviceWrapperOver = getServiceWrapperOverhead();
    int serviceCallOver = getServiceCallOverhead();

    StringBuffer buf = new StringBuffer();
    buf.append("Total Analysis Engine Time: " + total + "ms\n");
    if (analysis > 0) {
      buf.append("Annotator Time: " + getAnalysisTime() + "ms (" + toPct(analysis, total) + ")\n");
    } else {
      buf.append("Analysis: <10ms\n");
    }
    if (frameworkOver > 0) {
      buf.append("Framework Overhead: " + frameworkOver + "ms (" + toPct(frameworkOver, total)
              + ")\n");
    } else {
      buf.append("Framework Overhead: <10ms\n");
    }
    if (serviceCallOver > 0) {
      buf.append("Service Wrapper Overhead: " + serviceWrapperOver + "ms ("
              + toPct(serviceWrapperOver, total) + ")\n");
      buf.append("Service Call Overhead: " + serviceCallOver + "ms ("
              + toPct(serviceCallOver, total) + ")\n");
    }

    return buf.toString();
  }

  /**
   * Convert to percent string - to two decimal places
   */
  private String toPct(long numerator, long denomenator) {
    return pctFormat.format(((double) numerator) / denomenator);
  }

  /**
   * Gets the combined duration of all sub-events of certain types. Will recurse into events that
   * don't have the correct type but will not recurse inside a matching event (to avoid
   * double-counting of any times).
   * 
   * @param aEvent
   *          event whose subevents will be examined
   * @param aEventTypes
   *          array of event types in which we are interested
   * 
   * @return sum of the durations of sub-events of <code>aEvent</code> whose type is a member of
   *         <code>aEventTypes</code>.
   */
  private int getSubEventDuration(ProcessTraceEvent aEvent, String[] aEventTypes) {
    int duration = 0;
    List<ProcessTraceEvent> subEvents = aEvent.getSubEvents();
    Iterator<ProcessTraceEvent> it = subEvents.iterator();
    whileLoop: while (it.hasNext()) {
      ProcessTraceEvent evt = it.next();
      String type = evt.getType();
      for (int i = 0; i < aEventTypes.length; i++) {
        if (aEventTypes[i].equals(type)) {
          duration += evt.getDuration();
          continue whileLoop;
        }
      }
      // call recursively on subevents
      duration += getSubEventDuration(evt, aEventTypes);
    }

    return duration;
  }
}
