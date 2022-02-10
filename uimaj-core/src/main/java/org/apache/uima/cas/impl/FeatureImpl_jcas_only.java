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

/**
 * The implementation of jcas-only features in the type system. JCas-only features are not real
 * features in the type system, but are features inferred by the existence of JCas class information
 * They cannot be set or referenced, but exist in order to properly set up the "offsets" for the
 * JCas class, so that that same JCas class can be used with a different type system which **does**
 * define that feature.
 */
public class FeatureImpl_jcas_only extends FeatureImpl {

  FeatureImpl_jcas_only(String shortName, TypeImpl rangeType) {
    super(null, shortName, rangeType, null, false, null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format("FeatureImpl_jcas_only [%s, range:=%s]", getShortName(), getRangeImpl());
  }

}
