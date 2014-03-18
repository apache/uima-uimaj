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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A concurrent map supporting a value-creating get.
 * There's a small window where the value producing function could be called multiple times
 * on different threads, but the first one will be used and the others thrown away.
 */
public class ConcurrentHashMapWithProducer<K, V> extends ConcurrentHashMap<K, V> {  

  private static final long serialVersionUID = 1L;

  public ConcurrentHashMapWithProducer() {
    super();
  }

  public ConcurrentHashMapWithProducer(int initialCapacity, float loadFactor, int concurrencyLevel) {
    super(initialCapacity, loadFactor, concurrencyLevel);
  }


  public ConcurrentHashMapWithProducer(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }


  public ConcurrentHashMapWithProducer(int initialCapacity) {
    super(initialCapacity);
  }


  public ConcurrentHashMapWithProducer(Map<? extends K, ? extends V> m) {
    super(m);
  }

  public V get(K key, Callable<V> valueProducer) throws Exception {
    V value = get(key), otherValue = null;
    if (null == value) {
      otherValue = putIfAbsent(key,  value = valueProducer.call());
      value = (otherValue != null) ? otherValue : value;
    }
    return value;
  }
}
