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

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.StringArrayFS;

public class DebugNameValuePair {

  /** Name */
  private String mName;

  /** Value */
  private Object mValue;

  /**
   * Creates a new <code>NameValuePair_impl</code> with the specified name and value.
   * 
   * @param aName
   *          a name
   * @param aValue
   *          a value
   */
  public DebugNameValuePair(String aName, Object aValue) {
    setName(aName);
    setValue(aValue);
  }

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName() {
    return mName;
  }

  /**
   * Sets the name.
   * 
   * @param aName
   *          a name
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * Gets the value.
   * 
   * @return the value
   */
  public Object getValue() {
    return mValue;
  }

  /**
   * Sets the value.
   * 
   * @param aValue
   *          a value
   */
  public void setValue(Object aValue) {
    mValue = aValue;
  }

  public String toString() {
    Object v = getValue();
    if (v instanceof StringArrayFS)
      v = "StringArrayFS[" + ((StringArrayFS) v).size() + "]";
    else if (v instanceof FloatArrayFS)
      v = "FloatArrayFS[" + ((FloatArrayFS) v).size() + "]";
    else if (v instanceof IntArrayFS)
      v = "IntArrayFS[" + ((IntArrayFS) v).size() + "]";
    else if (v instanceof ArrayFS)
      v = "ArrayFS[" + ((ArrayFS) v).size() + "]";
    return getName() + ": " + v;
  }

}
