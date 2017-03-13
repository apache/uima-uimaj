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


public interface JMSMessagingSpecifier extends MessagingSpecifier {

  public static final String defaultInitialContextFactory = "com.sun.jndi.ldap.LdapCtxFactory";

  /**
   * Returns the hostInitialContextFactory.
   * 
   * @return String
   */
  public String getHostInitialContextFactory();

  /**
   * Returns the hostProviderURL.
   * 
   * @return String
   */
  public String getHostProviderURL();

  /**
   * Returns the targetInitialContextFactory.
   * 
   * @return String
   */
  public String getTargetInitialContextFactory();

  /**
   * Returns the targetProviderURL.
   * 
   * @return String
   */
  public String getTargetProviderURL();

  /**
   * Sets the hostInitialContextFactory.
   * 
   * @param hostInitialContextFactory
   *          The hostInitialContextFactory to set
   */
  public void setHostInitialContextFactory(String hostInitialContextFactory);

  /**
   * Sets the hostProviderURL.
   * 
   * @param hostProviderURL
   *          The hostProviderURL to set
   */
  public void setHostProviderURL(String hostProviderURL);

  /**
   * Sets the targetInitialContextFactory.
   * 
   * @param targetInitialContextFactory
   *          The targetInitialContextFactory to set
   */
  public void setTargetInitialContextFactory(String targetInitialContextFactory);

  /**
   * Sets the targetProviderURL.
   * 
   * @param targetProviderURL
   *          The targetProviderURL to set
   */
  public void setTargetProviderURL(String targetProviderURL);
}
