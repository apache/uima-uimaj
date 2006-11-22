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

import java.util.ArrayList;

public class FeatureMap {
  ArrayList entities = null;

  public FeatureMap() {
    entities = new ArrayList();
  }

  public void add(ConfigurableFeature entity) {
    entities.add(entity);
  }

  public ConfigurableFeature get(int index) {
    if (entities.size() < index) {
      return null;
    }

    return (ConfigurableFeature) entities.get(index);
  }

  public ConfigurableFeature get(String key) {
    for (int i = 0; i < entities.size(); i++) {
      if (((ConfigurableFeature) entities.get(i)).getOldFeatureName().equals(key))
        return (ConfigurableFeature) entities.get(i);
    }
    return null;
  }

  public boolean contains(String key) {
    for (int i = 0; i < entities.size(); i++) {
      if (((ConfigurableFeature) entities.get(i)).getOldFeatureName().equals(key))
        return true;
    }
    return false;
  }

  public int size() {
    return entities.size();
  }
}
