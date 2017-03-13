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

package org.apache.uima.resource.metadata.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.util.NameClassPair;

/**
 * A simple concrete MetaDataObject implementation for use in testing the MetaDataObject_impl class.
 */
public class TestFruitObject extends MetaDataObject_impl {
  
  private static final long serialVersionUID = 1L;

  public TestFruitObject() {
    //do nothing
  }

  public String getName() {
    return mName;
  }

  public void setName(String aName) {
    mName = aName;
  }

  public String getColor() {
    return mColor;
  }

  public void setColor(String aColor) {
    mColor = aColor;
  }

  public float getAvgWeightLbs() {
    return mAvgWeightLbs;
  }

  public void setAvgWeightLbs(float aAvgWeightLbs) {
    mAvgWeightLbs = aAvgWeightLbs;
  }

  public int getAvgCostCents() {
    return mAvgCostCents;
  }

  public void setAvgCostCents(int aAvgCostCents) {
    mAvgCostCents = aAvgCostCents;
  }

  public boolean isCitrus() {
    return mCitrus;
  }

  public void setCitrus(boolean aCitrus) {
    mCitrus = aCitrus;
  }

  public String[] getCommonUses() {
    return mCommonUses;
  }

  public void setCommonUses(String[] aCommonUses) {
    mCommonUses = aCommonUses;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("fruit",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name"), new PropertyXmlInfo("color"),
              new PropertyXmlInfo("avgWeightLbs"), new PropertyXmlInfo("avgCostCents"),
              new PropertyXmlInfo("citrus"), new PropertyXmlInfo("commonUses"), });

  /**
   * For testing purposes - a hardcoded attribute set. Should be compared with the results of
   * {@link #listAttributes()}.
   */
  static public Set<NameClassPair> getAttributeSet() {
    HashSet<NameClassPair> result = new HashSet<NameClassPair>();
    result.add(new NameClassPair("name", String.class.getName()));
    result.add(new NameClassPair("color", String.class.getName()));
    result.add(new NameClassPair("avgWeightLbs", Float.class.getName()));
    result.add(new NameClassPair("avgCostCents", Integer.class.getName()));
    result.add(new NameClassPair("citrus", Boolean.class.getName()));
    result.add(new NameClassPair("commonUses", String[].class.getName()));
    return result;
  }
  
  static public Set<MetaDataAttr> getMetaDataAttrSet() {
    HashSet<MetaDataAttr> result = new HashSet<MetaDataAttr>();
    try {
    result.add(new MetaDataAttr(
        "name", 
        TestFruitObject.class.getDeclaredMethod("getName"),
        TestFruitObject.class.getDeclaredMethod("setName", String.class), 
        String.class));
    result.add(new MetaDataAttr(
        "avgWeightLbs", 
        TestFruitObject.class.getDeclaredMethod("getAvgWeightLbs"),
        TestFruitObject.class.getDeclaredMethod("setAvgWeightLbs", float.class),
        Float.class));
    result.add(new MetaDataAttr(
        "color", 
        TestFruitObject.class.getDeclaredMethod("getColor"),
        TestFruitObject.class.getDeclaredMethod("setColor", String.class), 
        String.class));
    result.add(new MetaDataAttr(
        "avgCostCents", 
        TestFruitObject.class.getDeclaredMethod("getAvgCostCents"),
        TestFruitObject.class.getDeclaredMethod("setAvgCostCents", int.class), 
        Integer.class));
    result.add(new MetaDataAttr(
        "citrus", 
        TestFruitObject.class.getDeclaredMethod("isCitrus"),
        TestFruitObject.class.getDeclaredMethod("setCitrus", boolean.class), 
        Boolean.class));
    result.add(new MetaDataAttr(
        "commonUses", 
        TestFruitObject.class.getDeclaredMethod("getCommonUses"),
        TestFruitObject.class.getDeclaredMethod("setCommonUses", String[].class), 
        String[].class));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private String mName;

  private String mColor;

  private float mAvgWeightLbs;

  private int mAvgCostCents;

  private boolean mCitrus;

  private String[] mCommonUses;

}
