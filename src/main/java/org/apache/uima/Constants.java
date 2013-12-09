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

package org.apache.uima;

/**
 * Some constants used by the UIMA framework.
 * 
 * 
 */
public abstract class Constants {
  /**
   * A constant indicating the name of the "SOAP" protocol for service communication.
   */
  public static final String PROTOCOL_SOAP = "SOAP";

  /**
   * A constant indicating the name of the "SOAP with Attachments" protocol for service
   * communication.
   */
  public static final String PROTOCOL_SOAP_WITH_ATTACHMENTS = "SOAPwithAttachments";

  /**
   * A constant indicating the name of the "VINCI" protocol for service communication.
   */
  public static final String PROTOCOL_VINCI = "Vinci";

  /**
   * A constant indicating the name of the "VINCI" protocol, using binary CAS serialization, for
   * service communication.
   */
  public static final String PROTOCOL_VINCI_BINARY_CAS = "VinciBinaryCAS";

  /**
   * A constant indicating the name of the "MQ" protocol for service communication.
   */
  public static final String PROTOCOL_MQ = "MQ";

  /**
   * A constant indicating the name of the "Mail" protocol for service communication.
   */
  public static final String PROTOCOL_MAIL = "MAIL";

  /**
   * A constant indicating the name of the "JMS" protocol for service communication.
   */
  public static final String PROTOCOL_JMS = "JMS";
  
  /**
   * Name of the UIMA Java framework, to be used in &lt;frameworkImplementation&gt; 
   * element of descriptors.
   */
  public static final String JAVA_FRAMEWORK_NAME = "org.apache.uima.java";
  
  /**
   * Name of the UIMA C++ framework, to be used in &lt;frameworkImplementation&gt; 
   * element of descriptors.
   */
  public static final String CPP_FRAMEWORK_NAME = "org.apache.uima.cpp";
}
