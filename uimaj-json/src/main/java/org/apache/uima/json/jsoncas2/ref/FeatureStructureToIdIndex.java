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
package org.apache.uima.json.jsoncas2.ref;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.apache.uima.cas.FeatureStructure;

import com.fasterxml.jackson.databind.DatabindContext;

public class FeatureStructureToIdIndex {
  public static final String KEY = "UIMA.FeatureStructureToIdIndex";

  private Map<FeatureStructure, Integer> fsToIdIndex;
  private Map<Integer, FeatureStructure> idToFsIndex;

  public FeatureStructureToIdIndex() {
    idToFsIndex = new IdentityHashMap<>();
    fsToIdIndex = new IdentityHashMap<>();
  }

  public void put(int aFsId, FeatureStructure aFs) {
    idToFsIndex.put(aFsId, aFs);
    fsToIdIndex.put(aFs, aFsId);
  }

  public OptionalInt get(FeatureStructure aFs) {
    Integer id = fsToIdIndex.get(aFs);
    return id != null ? OptionalInt.of(id) : OptionalInt.empty();
  }

  public Optional<FeatureStructure> get(int aId) {
    return Optional.ofNullable(idToFsIndex.get(aId));
  }

  public static void set(DatabindContext aProvider, FeatureStructureToIdIndex aRefCache) {
    aProvider.setAttribute(KEY, aRefCache);
  }

  public static FeatureStructureToIdIndex get(DatabindContext aProvider) {
    return (FeatureStructureToIdIndex) aProvider.getAttribute(KEY);
  }
}
