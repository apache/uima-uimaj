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

package org.apache.uima.search;

/**
 * Mapping from a CAS feature name to its name in the index. Used inside
 * {@link Style#getAttributeMappings()}.
 */
public interface Mapping {
  /**
   * Gets the name of the feature that should be indexed.
   * 
   * @return the CAS feature name
   */
  public String getFeature();

  /**
   * Sets the name of the feature that should be indexed.
   * 
   * @param aFeature
   *          the CAS feature name
   */
  public void setFeature(String aFeature);

  /**
   * Gets the name that will be used to represent this feature in the index. This determines the
   * name that must be used to query for this feature.
   * 
   * @return the index name for the feature
   */
  public String getIndexName();

  /**
   * Sets the name that will be used to represent this feature in the index. This determines the
   * name that must be used to query for this feature.
   * 
   * @param aIndexName
   *          the index name for the feature
   */
  public void setIndexName(String aIndexName);
}
