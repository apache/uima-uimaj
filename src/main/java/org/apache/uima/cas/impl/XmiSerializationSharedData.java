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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.internal.util.rb_trees.RedBlackTree;

/**
 * Holds information that is shared between the XmiCasSerializer and the XmiCasDeserializer. This
 * allows consistency of XMI IDs across serializations, and also provides the ability to filter out
 * some FSs during serialization (e.g. to send to a service) and then reintegrate those FSs during
 * the next deserialization.
 * 
 */
public class XmiSerializationSharedData {
  /**
   * A map from FeatureStructure address to XMI ID. This is built during deserialization, then used
   * by the next serialization to ensure consistent IDs.
   */
  private RedBlackTree fsAddrToXmiIdMap = new RedBlackTree();

  /**
   * The maximum XMI ID used in the serialization. Used to generate unique IDs if needed.
   */
  private int maxXmiId = 0;

  void addIdMapping(int fsAddr, int xmiId) {
    fsAddrToXmiIdMap.put(fsAddr, Integer.toString(xmiId));
    if (xmiId > maxXmiId)
      maxXmiId = xmiId;
  }

  String getXmiId(int fsAddr) {
    // see if we already have a mapping
    String xmiId = (String) fsAddrToXmiIdMap.get(fsAddr);
    if (xmiId != null) {
      return xmiId;
    } else // no mapping for this FS. Generate a unique ID
    {
      // to be sure we get a unique Id, increment maxXmiId and use that
      String idStr = Integer.toString(++maxXmiId);
      fsAddrToXmiIdMap.put(fsAddr, idStr);
      return idStr;
    }
  }

  public void clearIdMap() {
    fsAddrToXmiIdMap.clear();
  }

  void checkForDups() {
    Set ids = new HashSet();
    Iterator iter = fsAddrToXmiIdMap.iterator();
    while (iter.hasNext()) {
      String xmiId = (String) iter.next();
      if (!ids.add(xmiId)) {
        throw new RuntimeException("Duplicate ID " + xmiId + "!");
      }
    }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    int[] keys = fsAddrToXmiIdMap.keySet();
    for (int i = 0; i < keys.length; i++) {
      buf.append(keys[i]).append(": ").append(fsAddrToXmiIdMap.get(keys[i])).append('\n');
    }
    return buf.toString();
  }
}
