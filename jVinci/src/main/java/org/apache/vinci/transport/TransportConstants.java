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

package org.apache.vinci.transport;

/**
 * This class contains all frame related constants used by the Vinci transport layer.
 */
public class TransportConstants {
  /**
   * Class not to be instantiated.
   */
  private TransportConstants() {
  }

  public static final String VINCI_NAMESPACE     = "vinci:";
  public static final String VINCI_NAMESPACE_URI = "http://vinci.almaden.ibm.com/FrameSpec/";
  // ^^ This transport library naively assumes that the Vinci namepsace is ALWAYS
  // indicated by the "vinci:" tag prefix, even though XML requires the namespace
  // be defined by the prefix "xmlns" definition. 

  public static final String PCDATA_KEY          = "";
  public static final String ERROR_KEY           = VINCI_NAMESPACE + "ERROR";
  public static final String COMMAND_KEY         = VINCI_NAMESPACE + "COMMAND";
  public static final String SHUTDOWN_KEY        = VINCI_NAMESPACE + "SHUTDOWN";
  public static final String PING_KEY            = VINCI_NAMESPACE + "PING";
  public static final String STATUS_KEY          = VINCI_NAMESPACE + "STATUS";

  public static final String TRUE_VALUE          = "true";
  public static final String FALSE_VALUE         = "false";
  public static final String OK_VALUE            = "ok";
}
