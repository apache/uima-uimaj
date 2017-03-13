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

package org.apache.uima.collection.metadata;

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * Object containing a filter expression used by the CPE to route CAS's to CasProcessor. A
 * CasProcessor can be configured in the CPE Descriptor to use filtering using an SQL-like WHERE
 * clause: where featurespec [ and featurespec2 ...]. The featurespec can be one of these four forms
 * and meanings:
 * <ul>
 * <li> Feature Process CAS if the Feature is present (e.g. where Person)
 * <li> Feature! Process CAS if the Feature is not present (e.g. where Person!)
 * <li> Feature=value Process CAS if the Feature has this value( e.g. where Person=Bush)
 * <li> Feature!=value Process CAS if the Feature does not have this value (e.g. where Person!=Bush)
 * </ul>
 * 
 * The featurespecs are implicitly connected with and operators and precedence rules are currently
 * not supported.
 * 
 * 
 */
public interface CasProcessorFilter extends MetaDataObject {
  /**
   * Sets filter expression
   * 
   * @param aFilterString -
   *          expression
   */
  public void setFilterString(String aFilterString);

  /**
   * Returns a filter expression as String
   * 
   * @return - filter expression
   */
  public String getFilterString();
}
