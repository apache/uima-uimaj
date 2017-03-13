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
package org.apache.uima.util;

import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.resource.ResourceConfigurationException;

/*
 * Test POJO access via the UimaContextHolder
 * Should work when invoked by a child thread, 
 * but fail when invoked by a separate process via main 
 */
public class UimaContextHolderTest implements Runnable {

  public String result = null;
  public long threadId = 0;
  static String nocontextError = "ERROR: No UimaContext accessible";

  public String testSettings() throws ResourceConfigurationException {
    threadId  = Thread.currentThread().getId();
    UimaContext uimaContext = UimaContextHolder.getContext();
    if (uimaContext == null) {
      return nocontextError;
    }
    return uimaContext.getSharedSettingValue("context-holder");
  }

  @Override
  public void run() {
    try {
      result  = testSettings();
    } catch (ResourceConfigurationException e) {
      e.printStackTrace();
    }
  }

  /*
   * When invoked as an independent thread should report no UimaContext available
   */
  public static void main(String[] args) {
    UimaContextHolderTest contextHolder = new UimaContextHolderTest();
    try {
      String result = contextHolder.testSettings();
      if (!result.equals(nocontextError)) {
        System.exit(99);
      }
    } catch (ResourceConfigurationException e) {
      System.exit(98);
    }
  }
}
