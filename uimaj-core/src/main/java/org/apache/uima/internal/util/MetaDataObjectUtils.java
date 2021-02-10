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
package org.apache.uima.internal.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.resource.metadata.MetaDataObject;

public class MetaDataObjectUtils {
  public static <K, V extends MetaDataObject> Map<K, V> cloneMap(Map<K, V> input) {
    if (input == null) {
      return null;
    }

    Map<K, V> copy = new LinkedHashMap<>();
    for (Entry<K, V> e : input.entrySet()) {
      V copiedValue = e.getValue();
      if (copiedValue instanceof MetaDataObject) {
        copiedValue = (V) ((MetaDataObject) copiedValue).clone();
      }
      copy.put(e.getKey(), copiedValue);
    }
    return copy;
  }
}
