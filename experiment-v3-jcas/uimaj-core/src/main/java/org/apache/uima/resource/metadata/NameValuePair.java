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
 * A type of <code>MetaDataObject</code> that stores a name (String) and value (Object).
 * 
 * 
 */
public interface NameValuePair extends MetaDataObject {

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName();

  /**
   * Sets the name.
   * 
   * @param aName
   *          a name
   */
  public void setName(String aName);

  /**
   * Gets the value.
   * 
   * @return the value
   */
  public Object getValue();

  /**
   * Sets the value.
   * 
   * @param aValue
   *          a value
   */
  public void setValue(Object aValue);

}
