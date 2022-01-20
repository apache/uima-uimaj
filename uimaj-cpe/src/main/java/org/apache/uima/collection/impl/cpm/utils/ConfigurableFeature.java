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

/**
 * The Class ConfigurableFeature.
 */
public class ConfigurableFeature {

  /** The value. */
  private ValuePair value;

  /** The attribute list. */
  private ArrayList attributeList = new ArrayList();

  /**
   * Instantiates a new configurable feature.
   *
   * @param oldV
   *          the old V
   * @param newV
   *          the new V
   */
  public ConfigurableFeature(String oldV, String newV) {
    value = new ValuePair(oldV, newV);
  }

  /**
   * Gets the old feature name.
   *
   * @return the old feature name
   */
  public String getOldFeatureName() {
    return value.getOldV();
  }

  /**
   * Gets the new feature name.
   *
   * @return the new feature name
   */
  public String getNewFeatureName() {
    return value.getNewV();
  }

  /**
   * Adds the attribute.
   *
   * @param value
   *          the value
   */
  public void addAttribute(ValuePair value) {
    attributeList.add(value);
  }

  /**
   * Adds the attributes.
   *
   * @param attList
   *          the att list
   */
  public void addAttributes(ArrayList attList) {
    attributeList = attList;
  }

  /**
   * Gets the old attribute value.
   *
   * @param index
   *          the index
   * @return the old attribute value
   */
  public String getOldAttributeValue(int index) {
    if (index < 0 || attributeList.size() == 0 || attributeList.size() < index) {
      return null;
    }
    return ((ValuePair) attributeList.get(index)).getOldV();
  }

  /**
   * Gets the old attribute value.
   *
   * @param key
   *          the key
   * @return the old attribute value
   */
  public String getOldAttributeValue(String key) {
    for (int i = 0; i < attributeList.size(); i++) {
      if (((ValuePair) attributeList.get(i)).getOldV().equals(key)) {
        return ((ValuePair) attributeList.get(i)).getOldV();
      }
    }
    return null;
  }

  /**
   * Gets the new attribute value.
   *
   * @param index
   *          the index
   * @return the new attribute value
   */
  public String getNewAttributeValue(int index) {
    if (index < 0 || attributeList.size() == 0 || attributeList.size() < index) {
      return null;
    }
    return ((ValuePair) attributeList.get(index)).getNewV();
  }

  /**
   * Gets the new attribute value.
   *
   * @param key
   *          the key
   * @return the new attribute value
   */
  public String getNewAttributeValue(String key) {
    for (int i = 0; i < attributeList.size(); i++) {
      if (((ValuePair) attributeList.get(i)).getOldV().equals(key)) {
        return ((ValuePair) attributeList.get(i)).getNewV();
      }
    }
    return null;
  }

  /**
   * Attribute list size.
   *
   * @return the int
   */
  public int attributeListSize() {
    if (attributeList == null) {
      return 0;
    }
    return attributeList.size();
  }
}
