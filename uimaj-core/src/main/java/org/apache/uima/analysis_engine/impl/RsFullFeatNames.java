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

package org.apache.uima.analysis_engine.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.uima.cas.TypeSystem;

/**
 * Implements a globally shared weak-reference map between
 *   types & features to the corresponding Full Feature name
 * Used to avoid creating new full feature names when compiling
 *   result feature specs.
 * Indexable for features via a 2 step index: typeName (weak) and shortFeatName
 *
 */
public class RsFullFeatNames {
  
  private static class TypeFeats {
    private Map<String, String> short2Full = null;  // null till used 
  }
  
  private static final Map<String, TypeFeats> typeName2TypeFeats = new WeakHashMap<String, TypeFeats>(); 
  
  
  public static String getFullFeatName(String typeName, String shortFeatName) {
    synchronized (typeName2TypeFeats) {
      TypeFeats tf = typeName2TypeFeats.get(typeName);
      if (null == tf) {
        tf = new TypeFeats();
        typeName2TypeFeats.put(typeName, tf);
      }
      if (null == tf.short2Full) {
        tf.short2Full = new HashMap<String, String>(3);
      } else {
        String s = tf.short2Full.get(shortFeatName);
        if (null != s) {
          return s;
        }
      }
      String fullFeatName = makeFullFeatName(typeName, shortFeatName);
      tf.short2Full.put(shortFeatName, fullFeatName);
      return fullFeatName;
    } 
  }
  
  private static String makeFullFeatName(String typeName, String shortFeatName) {
    StringBuilder sb = new StringBuilder(typeName.length() + 1 + shortFeatName.length());
    return sb.append(typeName).append(TypeSystem.FEATURE_SEPARATOR).append(shortFeatName).toString();
  }

}
