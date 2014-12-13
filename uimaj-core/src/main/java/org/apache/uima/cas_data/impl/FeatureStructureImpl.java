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

package org.apache.uima.cas_data.impl;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.cas_data.FeatureValue;


public class FeatureStructureImpl implements FeatureStructure {
  
  private static final long serialVersionUID = -1828536763993413045L;

  private Map<String, FeatureValue> mFeatureMap;

  private String mFSType = null;

  private String mId = null;

  private int[] mIndexed = new int[0];

  public FeatureStructureImpl() {
    mFeatureMap = new TreeMap<String, FeatureValue>();
  }

  public String getType() {
    return mFSType;
  }

  public void setType(String aType) {
    mFSType = aType;
  }

  public String[] getFeatureNames() {
    Set<String> aSet = mFeatureMap.keySet();
    String[] featureNames = new String[aSet.size()];
    aSet.toArray(featureNames);
    return featureNames;
  }

  public FeatureValue getFeatureValue(String aFeatureName) {
    return mFeatureMap.get(aFeatureName);
  }

  public void setFeatureValue(String aFeatureType, FeatureValue aValue) {
    mFeatureMap.put(aFeatureType, aValue);
  }

  public Object get() {
    return this;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append('\n').append(getType()).append('\n');
    if (getId() != null) {
      buf.append("ID = ").append(getId()).append('\n');
    }
    int[] indexed = getIndexed();
    if (indexed.length > 0) {
      buf.append("indexed = ").append(indexed[0]);
      for (int i = 1; i < indexed.length; i++) {
        buf.append(' ').append(indexed[i]);
      }
      buf.append('\n');
    }
    String[] featNames = getFeatureNames();
    for (int i = 0; i < featNames.length; i++) {
      buf.append(featNames[i]).append(" = ").append(getFeatureValue(featNames[i])).append('\n');
    }
    return buf.toString();
  }

  /**
   * @return an ID string
   */
  public String getId() {
    return mId;
  }

  /**
   * @return true if it is indexed
   * @deprecated
   */
  @Deprecated
  public boolean isIndexed() {
    return mIndexed.length > 0;
  }

  /**
   * @param string -
   */
  public void setId(String string) {
    mId = string;
  }

  /**
   * @param b -
   * @deprecated
   */
  @Deprecated
  public void setIndexed(boolean b) {
    mIndexed = new int[] { 1 }; // index in first index repository for backwards compatibility
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.FeatureStructure#getIndexed()
   */
  public int[] getIndexed() {
    return mIndexed;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.FeatureStructure#setIndexed(int[])
   */
  public void setIndexed(int[] aIndexed) {
    if (aIndexed == null)
      mIndexed = new int[0];
    else
      mIndexed = aIndexed;
  }

}
