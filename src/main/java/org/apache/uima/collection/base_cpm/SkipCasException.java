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

package org.apache.uima.collection.base_cpm;

/**
 * Exception to notify the CPM to skip a Cas Processor for the current CAS. Skipping means that the
 * CPM will not call CasProcessor's process() method for the current CAS. It allows the CPM to
 * continue processing.
 */
public class SkipCasException extends Exception {
  private static final long serialVersionUID = -1536918949728720979L;

  public SkipCasException(String msg) {
    super(msg);
  }

}
