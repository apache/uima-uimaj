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
 * Contains command line and environment for launching a seperate process.
 * 
 * 
 */
public class Execute {
  private String[] environment;

  private String[] cmdLine;

  
  public Execute() {
    super();
  }

  /**
   * Returns command line as String Array
   * 
   * @return - command line
   */
  public String[] getCmdLine() {
    return cmdLine;
  }

  /**
   * Returns Cas Processor environment
   * 
   * @return - environment
   */
  public String[] getEnvironment() {
    return environment;
  }

  /**
   * Copies Cas Processor command line
   * 
   * @param strings -
   *          command line
   */
  public void setCmdLine(String[] strings) {
    cmdLine = strings;
  }

  /**
   * Copies Cas Processor environment
   * 
   * @param strings
   */
  public void setEnvironment(String[] strings) {
    environment = strings;
  }

}
