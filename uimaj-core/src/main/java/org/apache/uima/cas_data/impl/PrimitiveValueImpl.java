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

import org.apache.uima.cas_data.PrimitiveValue;


public class PrimitiveValueImpl implements PrimitiveValue {
  
  private static final long serialVersionUID = -5889249846359051538L;

  private Object aValueObject = null;

  public PrimitiveValueImpl(String aValue) {
    aValueObject = aValue;
  }

  public PrimitiveValueImpl(int aValue) {
    aValueObject = Integer.valueOf(aValue);
  }

  public PrimitiveValueImpl(float aValue) {
    aValueObject = Float.valueOf(aValue);
  }

  public String toString() {
    if (aValueObject == null)
      return "";
    else
      return aValueObject.toString();
  }

  public int toInt() {
    if (aValueObject instanceof Integer)
      return ((Integer) aValueObject).intValue();
    else if (aValueObject instanceof String) {
      try {
        return Integer.parseInt((String) aValueObject);
      } catch (NumberFormatException e) {
        // the string doesn't parse as an integer. Return 0 as per the contract
        // stated by the PrimitiveValue interface.
        return 0;
      }
    } else
      return 0;
  }

  public float toFloat() {
    if (aValueObject instanceof Float)
      return ((Float) aValueObject).floatValue();
    else if (aValueObject instanceof String) {
      try {
        return Float.parseFloat((String) aValueObject);
      } catch (NumberFormatException e) {
        // the string doesn't parse as an float. Return 0 as per the contract
        // stated by the PrimitiveValue interface.
        return 0;
      }
    } else
      return 0;
  }

  public Object get() {
    return aValueObject;
  }

}
