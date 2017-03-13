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

package org.apache.uima.analysis_engine.metadata;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * Sofa Name mapping is required to connect the output Sofas from one component to the input Sofa of
 * another component.
 * 
 * <p>
 * A <code>SofaMapping</code> object represents mapping of a Sofa name assigned by a component to
 * a Sofa name assigned by an aggregate which could be either an aggregate AE or a CPE. This
 * interface provides methods to set the attributes that define a mapping.
 * 
 * <p>
 * If the component Sofa Name is not set, it defaults to ({@link CAS#NAME_DEFAULT_SOFA}).
 * 
 */

public interface SofaMapping extends MetaDataObject {
  /**
   * Gets the component key. This identifies the component for which this mapping is defined.
   * 
   * @return the key
   */
  public String getComponentKey();

  /**
   * Set the component key. This identifies the component for which this mapping is defined.
   * 
   * @param aComponentKey
   *          the key
   */
  public void setComponentKey(String aComponentKey);

  /**
   * Gets the sofa name assigned by the component.
   * 
   * @return the component's sofa name
   */
  public String getComponentSofaName();

  /**
   * Set the component assigned sofa name.
   * 
   * @param aComponentSofaName
   *          the component's sofa name
   */
  public void setComponentSofaName(String aComponentSofaName);

  /**
   * Get the sofa name assigned by the aggregate.
   * 
   * @return the aggregate's sofa name
   */
  public String getAggregateSofaName();

  /**
   * Set the sofa name assigned by the aggregate.
   * 
   * @param aAggregateSofaName
   *          the aggregate's sofa name
   */
  public void setAggregateSofaName(String aAggregateSofaName);
}
