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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.internal.util.Pair;
import org.apache.uima.jcas.cas.TOP;

/**
 * This class is used by the XCASDeserializer to store feature structures that do not fit into the
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
  final List<FSData> fsList = new ArrayList<>();

  /**
   * Map from Feature Structure to List of String arrays holding feature names and values for
   * out-of-typesystem features on in-typesystem FSs.
   */
  final Map<TOP, List<Pair<String, Object>>> extraFeatureValues = new IdentityHashMap<>();

  /**
   * Map from FSArray instances to List of ArrayElement objects, each of which holds an array index
   * and value (as a string). key: FSArray instance represented as an xmiId
   */
  final Map<TOP, List<ArrayElement>> arrayElements = new IdentityHashMap<>();

  /**
   * Map used during re-serialization. Stores mapping from out-of-typesystem FS IDs to the actual
   * IDs used in the generated XCAS.
   */
  final Map<String, String> idMap = new HashMap<>();

  /**
   * For debugging purposes only.
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("FeatureStructures\n-----------------\n");

    for (FSData fs : fsList) {
      buf.append(fs.toString()).append('\n');
    }
    buf.append("\nFeatures\n-----------------\n");
    for (Map.Entry<TOP, List<Pair<String, Object>>> entry : extraFeatureValues.entrySet()) {
      TOP id = entry.getKey();
      buf.append(id._id).append(": ");
      for (Pair<String, Object> p : entry.getValue()) {
        TOP fs = (p.u instanceof TOP) ? (TOP) p.u : null;
        String sv = (p.u instanceof String) ? (String) p.u : fs.toShortString();
        buf.append(p.t).append('=').append(sv).append('\n');
      }
    }
    return buf.toString();
  }
}

/****************************************************************
 * W A R N I N G Not an Inner Class ! !
 ****************************************************************/
class ArrayElement {
  int index;

  String value;

  ArrayElement(int index, String value) {
    this.index = index;
    this.value = value;
  }
}

/****************************************************************
 * W A R N I N G Not an Inner Class ! !
 ****************************************************************/
class FSData {
  String id;

  String type;

  String indexRep; // space-separated sequence of index repository numbers

  /** map from feature name to value which is a string or a ref to a not-out-of-type-system FS */
  Map<String, Object> featVals = new HashMap<>();

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(type).append('[');
    Iterator<Map.Entry<String, Object>> it = featVals.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Object> entry = it.next();
      Object v = entry.getValue();
      if (v instanceof TOP) {
        TOP fs = (TOP) v;
        v = "FS:" + fs.toShortString();
      }
      buf.append(entry.getKey()).append('=').append(v).append(',');
    }
    buf.append("](ID=").append(id).append(')');
    return buf.toString();
  }

}
