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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * represents the updateable list of features, each with a particular language spec
 * a given feature only appears once in the list, with the union of all languages 
 */
public class RsFeats implements Iterable<RsFeat> {
  List<RsFeat> features = null; 
  
  RsFeats() {}
  
  /**
   * copies into a new feature list, shares the languages
   * @param other
   */
  RsFeats(RsFeats other) {
    if (other.features == null) {
      features = null;
      return;
    }      
    features = new ArrayList<RsFeat>(other.features.size());
    for (RsFeat f : other.features){
      features.add(new RsFeat(f));    
    }
  }
  
  int size() {
    return (features == null) ? 0 : features.size();
  }
  
  /**
   * ASSUMES feat not exist in features already 
   * @param feat
   */
  void add(String shortFeatName, Object languages) {
    String[] saLangs;
    RsLangs  rsLangs;
    RsFeat feat;
    if (languages instanceof String[]) {
      saLangs = (String[])languages;
      feat = new RsFeat(shortFeatName, saLangs);
    } else { 
      rsLangs = (RsLangs)languages;
      rsLangs.setShared();
      feat = new RsFeat(shortFeatName, rsLangs);
    }
    if (null == features) {
      features = new ArrayList<RsFeat>(1);
    }
    features.add(feat);
  }
  
  /**
   * Assume features != null
   * remove a feature, regardless of language(s)
   * @param shortFeatName
   */
  void remove(String typeName, String shortFeatName) {
    for (Iterator<RsFeat> it = features.iterator(); it.hasNext();) {
      if (shortFeatName.equals(it.next().shortFeatName)) {
        it.remove();
        return;
      }
    }
  }
  
  boolean contains(String typeName, String shortFeatName) {
    if (null == features || features.size() == 0) {
      return false;
    }
    return null != get(shortFeatName);
  }
  
  /**
   * linear search in list for short feat name
   * @param shortFeatName - canonicalized short feature name
   * @return the RsFeat or null
   */
  RsFeat get(String shortFeatName) {
    for (RsFeat r : features) {
      if (r.shortFeatName.equals(shortFeatName)) { 
        return r;
      }
    }
    return null;
  }

  public Iterator<RsFeat> iterator() {
    return (null == features) ? nullIterator : features.iterator();
  }
  
  final static Iterator<RsFeat> nullIterator = new Iterator<RsFeat>() {

    public boolean hasNext() {
      return false;
    }

    public RsFeat next() {
      return null;
    }

    public void remove() {
    }
  };
}
