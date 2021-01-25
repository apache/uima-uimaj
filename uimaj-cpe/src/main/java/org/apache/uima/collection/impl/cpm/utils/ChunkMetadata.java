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
  
  /** The Constant SEQUENCE. */
  public static final String SEQUENCE = "sequenceNumber";

  /** The Constant DOCUMENTID. */
  public static final String DOCUMENTID = "documentId";

  /** The Constant ISCOMPLETED. */
  public static final String ISCOMPLETED = "isCompleted";

  /** The Constant DOCUMENTURL. */
  public static final String DOCUMENTURL = "url";

  /** The Constant THROTTLEID. */
  public static final String THROTTLEID = "throttleID";

  /** The doc id. */
  private String docId;

  /** The sequence. */
  private int sequence;

  /** The last. */
  private boolean last = true;

  /** The timed out. */
  private boolean timedOut = false;

  /** The url. */
  private String url;

  /** The throttle ID. */
  private String throttleID;

  
  /**
   * Instantiates a new chunk metadata.
   *
   * @param aDocId the a doc id
   * @param aSequence the a sequence
   * @param aLast the a last
   */
  public ChunkMetadata(String aDocId, int aSequence, boolean aLast) {
    docId = aDocId;
    sequence = aSequence;
    last = aLast;
  }

  /**
   * Checks if is last.
   *
   * @return true, if is last
   */
  public boolean isLast() {
    return last;
  }

  /**
   * Gets the sequence.
   *
   * @return the sequence
   */
  public int getSequence() {
    return sequence;
  }

  /**
   * Checks if is one of many.
   *
   * @return true, if is one of many
   */
  public boolean isOneOfMany() {
    return sequence == 0 ? false : true;
  }

  /**
   * Gets the doc id.
   *
   * @return the doc id
   */
  public String getDocId() {
    return docId;
  }

  /**
   * Gets the throttle ID.
   *
   * @return the throttle ID
   */
  public String getThrottleID() {
    return throttleID;
  }

  /**
   * Gets the url.
   *
   * @return the url
   */
  public String getURL() {
    return url;
  }

  /**
   * Checks if is timed out.
   *
   * @return true if timed out
   */
  public boolean isTimedOut() {
    return timedOut;
  }

  /**
   * Sets the timed out.
   *
   * @param b true means timed out
   */
  public void setTimedOut(boolean b) {
    timedOut = b;
  }

  /**
   * Sets the throttle ID.
   *
   * @param aThrottleID the new throttle ID
   */
  public void setThrottleID(String aThrottleID) {
    throttleID = aThrottleID;
  }

  /**
   * Sets the url.
   *
   * @param aURL the new url
   */
  public void setURL(String aURL) {
    url = aURL;
  }
}
