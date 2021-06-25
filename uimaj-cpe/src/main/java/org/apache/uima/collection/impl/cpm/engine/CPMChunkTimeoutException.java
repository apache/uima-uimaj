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

package org.apache.uima.collection.impl.cpm.engine;

import org.apache.uima.resource.ResourceProcessException;



/**
 * The Class CPMChunkTimeoutException.
 */
public class CPMChunkTimeoutException extends ResourceProcessException {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 358067081843640078L;

  /** The document URL. */
  private String documentURL = null;

  /** The throttle ID. */
  private String throttleID = null;

  /** The doc ID. */
  private long docID = 0L;

  /**
   * Instantiates a new CPM chunk timeout exception.
   *
   * @param aDocumentId the document ID
   * @param aThrottleID tbd
   * @param aDocumentURL document URL
   */
  public CPMChunkTimeoutException(long aDocumentId, String aThrottleID, String aDocumentURL) {
    docID = aDocumentId;
    throttleID = aThrottleID;
    documentURL = aDocumentURL;
  }

  /**
   * Gets the document URL.
   *
   * @return the document URL
   */
  public String getDocumentURL() {
    return documentURL;
  }

  /**
   * Gets the doc ID.
   *
   * @return the docID
   */
  public long getDocID() {
    return docID;
  }

  /**
   * Gets the throttle ID.
   *
   * @return the ThrottleID
   */
  public String getThrottleID() {
    return throttleID;
  }

}
