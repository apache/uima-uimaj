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

import java.util.HashMap;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.cpm.engine.CPMEngine;
import org.apache.uima.util.Level;

/**
 * Facilitates cleaning up resources associated with chunking/sequencing logic.
 * 
 */
public class ExpirationTimer extends Thread {
  private final long timeOut;

  private final HashMap map;

  private final String key;

  CPMEngine cpm = null;

  /**
   * Constructs a Timer that expires after a given interval. It keeps the map from growing
   * indefinitely. Its main purpose is to remove entries from a given map using a provided key.
   */
  public ExpirationTimer(long aTimeout, HashMap aMap, String aKey, CPMEngine aCpm) {
    super();
    timeOut = aTimeout;
    map = aMap;
    key = aKey;
    cpm = aCpm;
  }

  /**
   * Sleeps until a given timeout occurs. When awaken this timer deletes an entry in the shared
   * HashMap using provided key. The map holds docId's that have been split into chunks.
   * 
   */
  public void run() {
    try {
      Thread.sleep(timeOut);
    } catch (InterruptedException e) {
    }

    if (map.containsKey(key)) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_timer_expired__FINEST",
                new Object[] { Thread.currentThread().getName(), key, String.valueOf(map.size()) });
      }
      synchronized (map) {
        map.remove(key);
      }
    }
  }

}
