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

package org.apache.uima.resource;

/**
 * A type of <code>ResourceSpecifier</code> that locates a resource file using its URL, where the
 * URL depends on an ISO language identifier. An example of this type of resource is a dictionary
 * with a separate data file for each language.
 * <p>
 * Instead of a single URL, this specifier defines a URL prefix and a URL suffix. The ISO language
 * identifier is then placed between the prefix and suffix to form the complete URL of the file. If
 * that file does not exist, more general language identifiers will be tried. For example, if there
 * is no resource for <code>en-US</code>, the resource for <code>en</code> will be used
 * instead.
 * 
 * 
 */
public interface FileLanguageResourceSpecifier extends ResourceSpecifier {

  /**
   * Retrieves the URL prefix.
   * 
   * @return the URL prefix
   */
  public String getFileUrlPrefix();

  /**
   * Retrieves the URL suffix.
   * 
   * @return the URL suffix
   */
  public String getFileUrlSuffix();

  /**
   * Sets the URL prefix.
   * 
   * @param aPrefix
   *          the URL prefix
   */
  public void setFileUrlPrefix(String aPrefix);

  /**
   * Sets the URL suffix.
   * 
   * @param aSuffix
   *          the URL suffix
   */
  public void setFileUrlSuffix(String aSuffix);
}
