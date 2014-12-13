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

package org.apache.uima.internal.util;

import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.ProcessTrace;

/**
 * Reference implementation of {@link InstrumentationFacility}.
 * 
 * 
 */
public class InstrumentationFacility_impl implements InstrumentationFacility {

  /**
   * Creates a new InstrumentationFacility_impl.
   * 
   * @param aProcessTrace
   *          the process trace object in which to record instrumentation information.
   */
  public InstrumentationFacility_impl(ProcessTrace aProcessTrace) {
    mProcessTrace = aProcessTrace;
  }

  /**
   * @see org.apache.uima.util.InstrumentationFacility#startEvent(java.lang.String,
   *      java.lang.String, java.lang.String)
   */
  public void startEvent(String aComponentName, String aEventType, String aDescription) {
    if (mProcessTrace != null) {
      mProcessTrace.startEvent(aComponentName, aEventType, aDescription);
    }
  }

  /**
   * @see org.apache.uima.util.InstrumentationFacility#endEvent(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  public void endEvent(String aComponentName, String aEventType, String aResultMessage) {
    if (mProcessTrace != null) {
      mProcessTrace.endEvent(aComponentName, aEventType, aResultMessage);
    }
  }

  /**
   * @see org.apache.uima.util.InstrumentationFacility#addEvent(java.lang.String, java.lang.String,
   *      java.lang.String, int, java.lang.String)
   */
  public void addEvent(String aResourceName, String aType, String aDescription, int aDuration,
          String aResultMsg) {
    if (mProcessTrace != null) {
      mProcessTrace.addEvent(aResourceName, aType, aDescription, aDuration, aResultMsg);
    }
  }

  /**
   * Sets the process trace object wrapped by this instrumentation facility. This is not part of the
   * InstrumentationFacility interface.
   * @param aProcessTrace -
   */
  public void setProcessTrace(ProcessTrace aProcessTrace) {
    mProcessTrace = aProcessTrace;
  }

  /**
   * The process trace object wrapped by this instrumentation facility.
   */
  private ProcessTrace mProcessTrace;
}
