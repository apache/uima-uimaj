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
 * Represents an allowed value for an "enumerated" type.
 * 
 * @see TypeDescription
 * 
 * 
 */
public interface AllowedValue extends MetaDataObject {

  /**
   * Gets the allowed value.
   * 
   * @return the allowed value string
   */
  public String getString();

  /**
   * Sets the allowed value.
   * 
   * @param aString
   *          the allowed value string
   */
  public void setString(String aString);

  /**
   * Gets the verbose description of this allowed value.
   * 
   * @return the description of this allowed value
   */
  public String getDescription();

  /**
   * Sets the verbose description of this allowed value.
   * 
   * @param aDescription
   *          the description of this allowed value
   */
  public void setDescription(String aDescription);
}
