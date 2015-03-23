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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.resource.metadata.TypePriorityList;

/**
 * Reference implementation of {@link TypePriorityList}.
 * 
 * 
 */
public class TypePriorityList_impl extends MetaDataObject_impl implements TypePriorityList {

  static final long serialVersionUID = 4700170375564691096L;

  private List<String> mTypeNames = new ArrayList<String>();

  /**
   * @see TypePriorityList#getTypes()
   * synchronized to prevent concurrent modification exceptions
   */
  public synchronized String[] getTypes() {
    String[] result = new String[mTypeNames.size()];
    mTypeNames.toArray(result);
    return result;
  }

  /**
   * @see TypePriorityList#setTypes(java.lang.String[])
   */
  public synchronized void setTypes(String[] aTypeNames) {
    mTypeNames.clear();
    for (int i = 0; i < aTypeNames.length; i++) {
      mTypeNames.add(aTypeNames[i]);
    }
  }

  /**
   * @see TypePriorityList#addType(java.lang.String)
   */
  public synchronized void addType(String aTypeName) {
    mTypeNames.add(aTypeName);
  }

  /**
   * @see TypePriorityList#removeType(java.lang.String)
   */
  public synchronized void removeType(String aTypeName) {
    mTypeNames.remove(aTypeName);
  }

  /*
   * (non-Javadoc) Special purpose clone method to deal with ArrayList.
   */
  public synchronized Object clone() {
    //surprise: super.clone sets the final field to the same array list as the original
    TypePriorityList_impl clone = (TypePriorityList_impl) super.clone();
    
    clone.mTypeNames = new ArrayList<>();  // because above clone has set it to the == object
    for (String name : mTypeNames) {
      clone.addType(name);
    }

    return clone;
  }

  /**
   * @see MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return new XmlizationInfo("priorityList", new PropertyXmlInfo[] { new PropertyXmlInfo("types",
            null, false, "type") });
  }
}
