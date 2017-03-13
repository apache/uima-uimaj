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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.cas.TypeSystem;

/**
 * Implements a globally shared weak-reference map between
 *   types &amp; features to the corresponding Full Feature name
 * Used to avoid creating new full feature names when compiling
 *   result feature specs.
 * Indexable for features via a 2 step index: typeName and shortFeatName
 * 
 * NOTE: this static table ends up holding on to string representations of all types,
 * all features, and all valid full feature names; there's no "cleanup".
 *
 */
public class RsFullFeatNames {
    
  private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> typeName2TypeFeats = 
      new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();  
  
  public static String getFullFeatName(String typeName, String shortFeatName) {
    
    ConcurrentHashMap<String, String> tf = typeName2TypeFeats.get(typeName), tfOther;
    if (null == tf) {
      tfOther = typeName2TypeFeats.putIfAbsent(typeName, tf = new ConcurrentHashMap<String, String>());
      tf = (tfOther != null) ? tfOther : tf; 
    }
    String s = tf.get(shortFeatName), otherString;
    if (null == s) {
      otherString = tf.putIfAbsent(shortFeatName, s = makeFullFeatName(typeName, shortFeatName));
      s = (otherString != null) ? otherString : s;
    }
    return s;
  }
  
  private static String makeFullFeatName(String typeName, String shortFeatName) {
    StringBuilder sb = new StringBuilder(typeName.length() + 1 + shortFeatName.length());
    return sb.append(typeName).append(TypeSystem.FEATURE_SEPARATOR).append(shortFeatName).toString();
  }

}
