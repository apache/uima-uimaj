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

package org.apache.uima.resource;


public interface MailMessagingSpecifier extends MessagingSpecifier {

  /**
   * Returns the hostImapPort.
   * 
   * @return int
   */
  public int getHostImapPort();

  /**
   * Returns the hostSmtpPort.
   * 
   * @return int
   */
  public int getHostSmtpPort();

  /**
   * Returns the targetImapPort.
   * 
   * @return int
   */
  public int getTargetImapPort();

  /**
   * Returns the targetSmtpPort.
   * 
   * @return int
   */
  public int getTargetSmtpPort();

  /**
   * Sets the hostImapPort.
   * 
   * @param hostImapPort
   *          The hostImapPort to set
   */
  public void setHostImapPort(int hostImapPort);

  /**
   * Sets the hostSmtpPort.
   * 
   * @param hostSmtpPort
   *          The hostSmtpPort to set
   */
  public void setHostSmtpPort(int hostSmtpPort);

  /**
   * Sets the targetImapPort.
   * 
   * @param targetImapPort
   *          The targetImapPort to set
   */
  public void setTargetImapPort(int targetImapPort);

  /**
   * Sets the targetSmtpPort.
   * 
   * @param targetSmtpPort
   *          The targetSmtpPort to set
   */
  public void setTargetSmtpPort(int targetSmtpPort);
}
