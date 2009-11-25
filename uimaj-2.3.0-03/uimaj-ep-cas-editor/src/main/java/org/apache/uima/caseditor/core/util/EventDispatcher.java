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

package org.apache.uima.caseditor.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * The EventDispatcher routes received events to the registered handler objects.
 * 
 * @param <K>
 *          the key type
 * @param <E>
 *          the event type
 */
public class EventDispatcher<K, E> {
  private Map<K, IEventHandler<E>> mHandler = new HashMap<K, IEventHandler<E>>();

  private IEventHandler<E> mDefaultHandler;

  /**
   * Initializes the current instance with a default handler.
   * 
   * @param defaultHandler
   *          handles all unkown events, must not be null
   */
  public EventDispatcher(IEventHandler<E> defaultHandler) {
    if (defaultHandler == null) {
      throw new IllegalArgumentException("The defaultHandler must not be null!");
    }

    mDefaultHandler = defaultHandler;
  }

  /**
   * Registers the given handler for the given key.
   * 
   * @param key
   * @param handler
   */
  public void register(K key, IEventHandler<E> handler) {
    mHandler.put(key, handler);
  }

  /**
   * Notifies the registered handler, if there is no handler registered for this key the default
   * handler is notified.
   * 
   * @param key
   *          the key type
   * @param event
   *          the event type
   */
  public void notify(K key, E event) {
    IEventHandler<E> handler = mHandler.get(key);

    if (handler != null) {
      handler.handle(event);
    } else {
      mDefaultHandler.handle(event);
    }
  }
}
