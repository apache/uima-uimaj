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

package org.apache.uima.cas_data;

/**
 * A subtype of FeatureStructure that represents an array of primitive values (Strings, int, or
 * floats).
 * 
 * 
 */
public interface PrimitiveArrayFS extends FeatureStructure {
  /**
   * Get the size of this array.
   * 
   * @return the size
   */
  public int size();

  /**
   * Gets this value as an integer array.
   * 
   * @return integer array value, empty array if value is not an array
   */
  public int[] toIntArray();

  /**
   * Gets this value as an float array.
   * 
   * @return float array value, empty array if value is not an array
   */
  public float[] toFloatArray();

  /**
   * Gets this value as an string array.
   * 
   * @return string array value, empty array if value is not an array
   */
  public String[] toStringArray();
}
