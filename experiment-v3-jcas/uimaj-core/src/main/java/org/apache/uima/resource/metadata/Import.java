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

import java.net.URL;

import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.InvalidXMLException;

/**
 * An import declaration. These are currently used to import type systems, indexes, and type
 * priorities. Imports may be by location (relative URL) or name (a Java-style compound name, looked
 * up in the classpath), but not both.
 * 
 * 
 */
public interface Import extends MetaDataObject {

 public static final Import[] EMPTY_IMPORTS = new Import[0];

  /**
   * Gets the name of this import's target.
   * 
   * @return a Java-style compound name which specifies the target of this import. This will be
   *         located by appending ".xml" to the name and searching the classpath.
   */
  public String getName();

  /**
   * Sets the name of this import's target.
   * 
   * @param aName
   *          a Java-style compound name which specifies the target of this import. This will be
   *          located by appending ".xml" to the name and searching the classpath.
   */
  public void setName(String aName);

  /**
   * Gets the location of this import's target.
   * 
   * @return a URI specifying the location of this import's target.
   */
  public String getLocation();

  /**
   * Sets the location of this import's target.
   * 
   * @param aUri
   *          a URI specifying the location of this import's target.
   */
  public void setLocation(String aUri);

  /**
   * Computes the absolute URL for this import, using the relative location or name, whichever is
   * specified by this import object.
   * 
   * @param aResourceManager
   *          resource manager to use to do name lookups
   * 
   * @return the absolute URL for this import
   * 
   * @throws InvalidXMLException
   *           if the import could not be resolved
   */
  public URL findAbsoluteUrl(ResourceManager aResourceManager) throws InvalidXMLException;

}
