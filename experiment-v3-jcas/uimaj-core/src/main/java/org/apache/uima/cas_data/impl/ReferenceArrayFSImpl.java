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

import org.apache.uima.cas_data.ReferenceArrayFS;

/**
 * 
 * 
 */
public class ReferenceArrayFSImpl extends FeatureStructureImpl implements ReferenceArrayFS {
  
  private static final long serialVersionUID = -1748415737830294866L;

  private String[] mIdRefs;

  public ReferenceArrayFSImpl(String[] aIdRefs) {
    mIdRefs = aIdRefs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.ReferenceArrayFS#size()
   */
  public int size() {
    return mIdRefs.length;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas_data.ReferenceArrayFS#getIdRefArray()
   */
  public String[] getIdRefArray() {
    return mIdRefs;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append('\n').append(getType()).append('\n');
    if (getId() != null) {
      buf.append("ID = ").append(getId()).append('\n');
    }
    buf.append('[');
    int size = size();
    for (int i = 0; i < size; i++) {
      buf.append(mIdRefs[i]);
      if (i < size - 1) {
        buf.append(',');
      }
    }
    buf.append("]\n");
    return buf.toString();
  }

}
