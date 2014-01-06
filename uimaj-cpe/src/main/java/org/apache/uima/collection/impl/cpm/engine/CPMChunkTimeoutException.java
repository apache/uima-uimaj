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


public class CPMChunkTimeoutException extends ResourceProcessException {
  private static final long serialVersionUID = 358067081843640078L;

  private String documentURL = null;

  private String throttleID = null;

  private long docID = 0L;

  /**
   * 
   * @param aDocumentId the document ID
   * @param aThrottleID tbd
   * @param aDocumentURL document URL
   */
  public CPMChunkTimeoutException(long aDocumentId, String aThrottleID, String aDocumentURL) {
    super();
    docID = aDocumentId;
    throttleID = aThrottleID;
    documentURL = aDocumentURL;
  }

  public String getDocumentURL() {
    return documentURL;
  }

  /**
   * @return the docID
   */
  public long getDocID() {
    return docID;
  }

  /**
   * @return the ThrottleID
   */
  public String getThrottleID() {
    return throttleID;
  }

}
