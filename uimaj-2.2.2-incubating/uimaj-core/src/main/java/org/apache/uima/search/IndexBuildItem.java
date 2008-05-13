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

import java.io.Serializable;

import org.apache.uima.util.XMLizable;

/**
 * An individual item within a {@link IndexBuildSpecification}. Specifies the {@link #getName()}
 * name of a CAS type, a {@link #getIndexRule() index rule}, and an optional
 * {@link #getFilter() filter}.
 * <p>
 * This object implements the {@link XMLizable} interface and can be parsed from an XML
 * representation.
 * 
 * 
 */
public interface IndexBuildItem extends XMLizable, Serializable {

  /**
   * Gets the name of the CAS type for which this item defines the indexing behavior.
   * 
   * @return the CAS type name for this build item
   */
  public String getName();

  /**
   * Sets the name of the CAS type for which this item defines the indexing behavior.
   * 
   * @param aName
   *          the CAS type name for this build item
   */
  public void setName(String aName);

  /**
   * Gets the <code>IndexRule</code> that defines how annotations matching this build item should
   * be indexed.
   * 
   * @return the indexing rule for this build item
   */
  public IndexRule getIndexRule();

  /**
   * Sets the <code>IndexRule</code> that defines how annotations matching this build item should
   * be indexed.
   * 
   * @param aRule
   *          the indexing rule for this build item
   */
  public void setIndexRule(IndexRule aRule);

  /**
   * Gets the <code>Filter</code> that identifies which instances of the named CAS type are
   * governed by the index rule for this item. Filters are optional; if none is specified then this
   * rule applies to all instances of the named CAS type.
   * 
   * @return the Filter for this build item, null if none
   */
  public Filter getFilter();

  /**
   * Sets the <code>Filter</code> that identifies which instances of the named CAS type are
   * governed by the index rule for this item. Filters are optional; if none is specified then this
   * rule applies to all instances of the named CAS type.
   * 
   * @param aFilter
   *          the Filter for this build item, null if none
   */
  public void setFilter(Filter aFilter);
}
