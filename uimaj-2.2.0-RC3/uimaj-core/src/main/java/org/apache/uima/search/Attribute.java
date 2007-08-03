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
 * A simple name, value pair that is specified as part of a {@link Style} to further define its
 * behavior.
 * <p>
 * This object implements the {@link XMLizable} interface and can be parsed from an XML
 * representation.
 * 
 * 
 */
public interface Attribute extends XMLizable, Serializable {

  /**
   * Gets the name of this attribute.
   * 
   * @return the name of this attribute
   */
  public String getName();

  /**
   * Sets the name of this attribute.
   * 
   * @param aName
   *          the name of this attribute
   */
  public void setName(String aName);

  /**
   * Gets the value of this attribute.
   * 
   * @return the value of this attribute
   */
  public String getValue();

  /**
   * Sets the value of this attribute.
   * 
   * @param aValue
   *          the value of this attribute
   */
  public void setValue(String aValue);
}
