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
import java.util.Iterator;
import java.util.Map;

/**
 * This object holds the set of RsTypes for a result spec
 * There is one instance of this per ResultSpecification_impl
 */
public class RsTypesMap implements Iterable<RsType> {
    
  private final Map<String, RsType> types;  
  
  RsTypesMap() {
    types = new HashMap<String, RsType>();
  }
  
  /**
   * cloning constructor - clones its arg
   * @param src
   */
  RsTypesMap(RsTypesMap src) {
    types = new HashMap<String, RsType>(src.types);
    for (Map.Entry<String, RsType> e : types.entrySet()) {
      e.setValue(new RsType(e.getValue()));  // copy
    }
  }

  /**
   * add a type (not a type:feat) 
   * @param typeName
   * @param isAllFeat
   * @param languages
   * @param replace
   */
  void add(String typeName, boolean isAllFeat, String[] languages, boolean replace) {
    RsType t = types.get(typeName);
    if (null == t) {
      t = new RsType(typeName);
      types.put(typeName, t);
    }
    if (isAllFeat) {
      if (!t.isAllFeatures) {
        replace = true;  // if setting for the 1st time, replace the x-unspec which is the default
      }
      t.isAllFeatures = true;
      t.languagesAllFeat = addLanguages(t.languagesAllFeat, languages, replace);
    } else {
      if (!t.isSpecified) {
        replace = true;
      }
      t.isSpecified = true;
      t.languagesNotAllFeat = addLanguages(t.languagesNotAllFeat, languages, replace);
    }
  }
  
  void add(String typeName, boolean isAllFeat, RsLangs rslangs, boolean replace) {
    RsType t = types.get(typeName);
    if (null == t) {
      t = new RsType(typeName);
      types.put(typeName, t);
    }
    if (isAllFeat) {
      if (!t.isAllFeatures) {
        replace = true;
      }
      t.isAllFeatures = true;
      t.languagesAllFeat = addLanguages(t.languagesAllFeat, rslangs, replace);
    } else {
      if (!t.isSpecified) {
        replace = true;
      }
      t.isSpecified = true;
      t.languagesNotAllFeat = addLanguages(t.languagesNotAllFeat, rslangs, replace);
    }
  }
 
  
  /**
   * add a feature (not a plain type)
   *   If feature exists, augments (union) its languages or replaces it
   * @param typeName
   * @param featName
   * @param languages
   * @param replace
   */

  void add(String typeName, String shortFeatName, Object rslangs, boolean replace) {
    RsType t = types.get(typeName);
    if (null == t) {
      t = new RsType(typeName);
      types.put(typeName, t);
    }
    RsFeats feats = t.features;
    RsFeat feat = null;
    if (null == feats) {
      feats = new RsFeats();
      t.features = feats;
    } else {
      feat = feats.get(shortFeatName);
    }
    if (null == feat) {
      feats.add(shortFeatName, rslangs);
    } else {
      feat.languages = addLanguages(feat.languages, rslangs, replace);
    }
  }
  
  RsLangs addLanguages(RsLangs existing, Object langs, boolean replace) {
    String[] saLangs = null;
    RsLangs  rsLangs = null;
    if (langs instanceof String[]) {
      saLangs = (String[])langs;
    } else {
      rsLangs = (RsLangs)langs;
    }
   
    boolean noLangs = (null == langs) ? true 
        : (null != saLangs) ? (0 == ((String[])langs).length)
        : RsLangs.isEmpty(rsLangs);        
    
    if (noLangs) {
      if (replace) {
        return null;
      }
      return existing;
    }
    if (null == existing && !replace) { // existing is x-unspecified
      return null;                      // subsumes everything
    }
    if (replace) {
      if (saLangs != null) {
        return RsLangs.createOrNull(saLangs);
      } else {
        rsLangs.setShared();
        return rsLangs;
      }
    }
    // not replace, and existing
    existing = (null != saLangs) ? RsLangs.addAll(existing, saLangs)
                                 : RsLangs.addAll(existing, rsLangs);
    return existing;
  }
  
  RsLangs addLanguages(RsLangs existing, RsLangs rslangs, boolean replace) {
    if (RsLangs.isEmpty(rslangs)) {
      if (replace) {
        return null;
      }
      return existing;
    }
    if (null == existing && !replace) {  // existing is x-unspecified
      return null;
    }
    if (replace) {
      rslangs.setShared();
      return rslangs;
    }
    existing = RsLangs.addAll(existing, rslangs);
    return existing;
  }
  
      
  /** 
   * Remove a type, regardless of languages
   * NOTE: doesn't remove type:feature entries associated with that type
   * @param type
   */
  void remove(String type) {
    RsType t = types.get(type);
    if (null != t) {
      if (null == t.features || t.features.size() == 0) {
        types.remove(type);
        return;
      }
      t.isAllFeatures = false;
      t.isSpecified = false;
      t.languagesAllFeat = null;
      t.languagesNotAllFeat = null;
    }
  }
  
  /**
   * remove a feature, regardless of languages
   * If all features are removed, null out the rsFeats slot.
   * If all features are removed, and no type instance, remove the type also.
   * @param typeName
   * @param feature Short Name
   */
  void remove(String type, String feature) {
    RsType t = types.get(type);
    if (null != t) {
      if (null != t.features) {
        t.features.remove(type, feature);
        if (0 == t.features.size()) {
          t.features = null;
          if (!t.isAllFeatures && !t.isSpecified) {
            types.remove(type);
          }
        }
      }
    }
  }
  
  RsType getRsType(String typeName) {
    return types.get(typeName);
  }
  
  RsFeat get(String typeName, String shortFeatName) {
    RsType t = types.get(typeName);
    if (t == null || t.features == null) {
      return null;
    }
    return t.features.get(shortFeatName);
  }  
  
  int nbrOfTypes() {
    return types.size();
  }

  public Iterator<RsType> iterator() {
    return types.values().iterator();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((types == null) ? 0 : types.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RsTypesMap other = (RsTypesMap) obj;
    if (types == null) {
      if (other.types != null) {
        return false;
      }
    } else if (!types.equals(other.types)) {
      return false;
    }
    return true;
  }

}
