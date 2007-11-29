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

import java.io.Serializable;

/**
 * The base interface for the value of a feature. Feature values will be implement one of three
 * subinterfaces:
 * <ul>
 * <li>{@link PrimitiveValue} (string, integer, float)</li>
 * <li>{@link ReferenceValue} (reference, via ID, to another feature structure)</li>
 * </ul>
 * <p>
 * Note that arrays are represented as FeatureStructures, not primitive values. Therefore features
 * with array values will be of type {@link ReferenceValue}, where the reference will resolve to an
 * instance of {@link PrimitiveArrayFS} or {@link ReferenceArrayFS}.
 * 
 * 
 */
public interface FeatureValue extends Serializable {
  /**
   * Gets the feature value as a Java object.
   * 
   * @return the feature value.
   */
  public Object get();
}
