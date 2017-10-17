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

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.CommonArrayFS;

/**
 * This interface is implemented by arrays of non-FeatureStructure components 
 *   boolean, byte, short, int, long, float, double, String, JavaObject
 * Internal Use Only.
 */
public interface CommonPrimitiveArray<T> extends CommonArrayFS<T> {
  
  /**
   * Internal Use Only.
   * Set an array value from a string representation
   * NOTE: does **not** log the change for delta cas; this should only be used by 
   *       internal deserializers
   * @param i the index
   * @param v the value to set at the above index
   */
  void setArrayValueFromString(int i, String v);
}
