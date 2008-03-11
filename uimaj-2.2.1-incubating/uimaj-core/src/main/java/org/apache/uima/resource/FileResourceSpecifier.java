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
 * A type of <code>ResourceSpecifier</code> that locates a resource file using its URL.
 * 
 * 
 */
public interface FileResourceSpecifier extends ResourceSpecifier {

  /**
   * Retrieves the URL of the file.
   * 
   * @return a URL string
   */
  public String getFileUrl();

  /**
   * Gets the file name for the local cache of a remote resource file. This is optional.
   * 
   * @return the file name of the local cache, <code>null</code> if none.
   */
  public String getLocalCache();

  /**
   * Sets the URL of the file.
   * 
   * @param aUrl
   *          a URL string
   */
  public void setFileUrl(String aUrl);

  /**
   * Sets the file name for the local cache of a remote resource file. This is optional.
   * 
   * @param aFileName
   *          file name of the local cache, <code>null</code> if none.
   */
  public void setLocalCache(String aFileName);
}
