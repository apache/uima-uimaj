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

package org.apache.uima.resource.metadata;

/**
 * Declares a relative priority between CAS types. This object contains a single list of types. One
 * or more <code>TypePriorityList</code> objects can be grouped together to define a complete
 * <code>TypePriorities</code> declaration for a type system.
 * <p>
 * This object implements <code>MetaDataObject</code>, which implements
 * {@link org.apache.uima.util.XMLizable}, so it can be serialized to and deserialized from an XML
 * element.
 * 
 * @see TypePriorities
 * 
 */
public interface TypePriorityList extends MetaDataObject {

  public final static TypePriorityList[] EMPTY_TYPE_PRIORITY_LISTS = new TypePriorityList[0];
  /**
   * Gets the type names, in order of their priority.
   * 
   * @return an array of type names, in order of their priority
   */
  public String[] getTypes();

  /**
   * Sets the type names, in order of their priority.
   * 
   * @param aTypeNames
   *          an array type names, in order of their priority
   */
  public void setTypes(String[] aTypeNames);

  /**
   * Adds a type at the end of the priority list.
   * 
   * @param aTypeName
   *          the type name to add
   */
  public void addType(String aTypeName);

  /**
   * Removes a type from the priority list.
   * 
   * @param aTypeName
   *          the type name to remove
   */
  public void removeType(String aTypeName);
}
