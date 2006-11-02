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

package org.apache.vinci.transport;

/**
 * Object for storing each key/value within a frame.  Also returned by the simple Frame
 * positional accessor method "getKeyValuePair(int)".
 *
 * KeyValuePair can be considered immutable unless you use any of the VinciFrame.fset(*) 
 * methods, which may modify the value component.
 *
 * Generally you shouldn't have to work with KeyValuePair objects since the recommended approach
 * to accessing Frame contents is through declarative as opposed to positional accessors.  
 */

public final class KeyValuePair {
  final String   key;
  FrameComponent value; // Frame or FrameLeaf

  /**
   * @pre mykey != null
   * @pre myvalue != null
   */
  public KeyValuePair(String mykey, FrameComponent myvalue) {
    this.key = mykey;
    this.value = myvalue;
  }

  public FrameComponent getValue() {
    return value;
  }

  public String getKey() {
    return key;
  }

  public boolean isValueALeaf() {
    return value instanceof FrameLeaf;
  }

  // Convenient methods for accesing value as a specific type.
  /**
   * @pre value instanceof FrameLeaf
   */
  public FrameLeaf getValueAsLeaf() {
    return (FrameLeaf) value;
  }

  public String getValueAsString() {
    return value.toString();
  }

  /**
   * @pre value instanceof Frame
   */
  public Frame getValueAsFrame() {
    return (Frame) value;
  }
}
