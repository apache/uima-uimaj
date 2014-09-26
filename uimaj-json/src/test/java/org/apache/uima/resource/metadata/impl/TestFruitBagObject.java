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

/**
 * A simple concrete MetaDataObject implementation for use in testing the MetaDataObject_impl class.
 */
public class TestFruitBagObject extends MetaDataObject_impl {

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("fruitBag",
          new PropertyXmlInfo[] { new PropertyXmlInfo("fruits") });

  private TestFruitObject[] mFruits;
  
  public TestFruitBagObject() {
    //do nothing
  }

  public TestFruitObject[] getFruits() {
    return mFruits;
  }

  public void setFruits(TestFruitObject[] aFruits) {
    mFruits = aFruits;
  }

//  /**
//   * For testing purposes - a hardcoded attribute set. Should be compared with the results of
//   * {@link #listAttributes()}.
//   */
//  static public Set<NameClassPair> getAttributeSet() {
//    Set<NameClassPair> result = new HashSet<NameClassPair>();
//    result.add(new NameClassPair("fruits", TestFruitObject[].class.getName()));
//    return result;
//  }
  
  static public Set<MetaDataAttr> getMetaDataAttrSet() {
    Set<MetaDataAttr> result = new HashSet<MetaDataAttr>();
    try {
    result.add(new MetaDataAttr("fruits", 
        TestFruitBagObject.class.getDeclaredMethod("getFruits"), 
        TestFruitBagObject.class.getDeclaredMethod("setFruits", TestFruitObject[].class),
        TestFruitObject[].class));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

}
