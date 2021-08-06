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

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.json.jsoncas2.model.FeatureStructures;

import com.fasterxml.jackson.databind.DatabindContext;

public class FeatureStructureToViewIndex {
  public static final String FS_VIEW_CACHE = "UIMA.FeatureStructureToViewIndex";

  private final FeatureStructures featureStructures;

  private Map<FeatureStructure, Set<String>> fsToViewsCache;

  public FeatureStructureToViewIndex() {
    featureStructures = null;
    fsToViewsCache = new IdentityHashMap<>();
  }

  public FeatureStructureToViewIndex(FeatureStructures aFeatureStructures) {
    featureStructures = aFeatureStructures;
  }

  public Set<String> getViewsContainingFs(FeatureStructure aFS) {
    if (fsToViewsCache == null) {
      fsToViewsCache = new IdentityHashMap<>();
      featureStructures.iterator().next().getCAS().getViewIterator().forEachRemaining(view -> {
        for (FeatureStructure fs : view.select()) {
          fsToViewsCache.computeIfAbsent(fs, _fs -> new HashSet<>()).add(view.getViewName());
        }
      });
    }

    return fsToViewsCache.get(aFS);
  }

  public void assignFsToView(FeatureStructure aFs, String aView) {
    fsToViewsCache.computeIfAbsent(aFs, _fs -> new HashSet<>()).add(aView);
  }

  public static void set(DatabindContext aProvider, FeatureStructureToViewIndex aRefCache) {
    aProvider.setAttribute(FS_VIEW_CACHE, aRefCache);
  }

  public static FeatureStructureToViewIndex get(DatabindContext aProvider) {
    return (FeatureStructureToViewIndex) aProvider.getAttribute(FS_VIEW_CACHE);
  }
}
