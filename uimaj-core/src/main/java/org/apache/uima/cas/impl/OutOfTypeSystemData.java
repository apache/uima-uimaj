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

package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is used by the XCASDeserializer to store feature sturctures that do not fit into the
 * type system of the CAS it is deserializing into. This data can then be passed to the
 * XCASSerializer, which will include it in the XCAS it produces. In this way consumers of an XCAS
 * can safely ignore out-of-typesystem data without destroying it.
 * 
 * 
 */
public class OutOfTypeSystemData {
  /**
   * List of FSData objects for out-of-typesystem FSs.
   */
  List fsList = new ArrayList();

  /**
   * Map from Integer (CAS address) to List of String arrays holding feature names and values for
   * out-of-typesystem features on in-typesystem FSs.
   */
  Map extraFeatureValues = new HashMap();

  /**
   * Map from Integer (CAS address of an FSArray) to List of ArrayElement objects, each of which
   * holds an array index and value (as a string).
   */
  Map arrayElements = new HashMap();

  /**
   * Map used during re-serialization. Stores mapping from out-of-typesystem FS IDs to the actual
   * IDs used in the generated XCAS.
   */
  Map idMap = new HashMap();

  /**
   * For debugging purposes only.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("FeatureStructures\n-----------------\n");
    Iterator it = fsList.iterator();
    while (it.hasNext()) {
      FSData fs = (FSData) it.next();
      buf.append(fs.toString()).append("\n");
    }
    buf.append("\nFeatures\n-----------------\n");
    it = extraFeatureValues.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      Integer id = (Integer) entry.getKey();
      buf.append(id).append(": ");
      List attrs = (List) entry.getValue();
      Iterator it2 = attrs.iterator();
      while (it2.hasNext()) {
        String[] attr = (String[]) it2.next();
        buf.append(attr[0]).append("=").append(attr[1]).append('\n');
      }
    }
    return buf.toString();
  }
}

class ArrayElement {
  int index;

  String value;

  ArrayElement(int index, String value) {
    this.index = index;
    this.value = value;
  }
}

class FSData {
  String id;

  String type;

  String indexRep; // space-separated sequence of index repository numbers

  Map featVals = new HashMap();

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(type).append('[');
    Iterator it = featVals.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      buf.append(entry.getKey()).append('=').append(entry.getValue()).append(',');
    }
    buf.append("](ID=").append(id).append(')');
    return buf.toString();
  }

}
