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

package org.apache.uima.collection.impl.cpm.utils;

/**
 * Convenience class that is used to hold metadata associated with chunking and sequencing of
 * documents. It allows the OutputQueue to manage sequencing of chunks destined for the CasConsumer.
 */
public class ChunkMetadata {
  public static final String SEQUENCE = "sequenceNumber";

  public static final String DOCUMENTID = "documentId";

  public static final String ISCOMPLETED = "isCompleted";

  public static final String DOCUMENTURL = "url";

  public static final String THROTTLEID = "throttleID";

  private String docId;

  private int sequence;

  private boolean last = true;

  private boolean timedOut = false;

  private String url;

  private String throttleID;

  
  public ChunkMetadata(String aDocId, int aSequence, boolean aLast) {
    docId = aDocId;
    sequence = aSequence;
    last = aLast;
  }

  public boolean isLast() {
    return last;
  }

  public int getSequence() {
    return sequence;
  }

  public boolean isOneOfMany() {
    return sequence == 0 ? false : true;
  }

  public String getDocId() {
    return docId;
  }

  public String getThrottleID() {
    return throttleID;
  }

  public String getURL() {
    return url;
  }

  /**
   * @return true if timed out
   */
  public boolean isTimedOut() {
    return timedOut;
  }

  /**
   * @param b
   */
  public void setTimedOut(boolean b) {
    timedOut = b;
  }

  public void setThrottleID(String aThrottleID) {
    throttleID = aThrottleID;
  }

  public void setURL(String aURL) {
    url = aURL;
  }
}
