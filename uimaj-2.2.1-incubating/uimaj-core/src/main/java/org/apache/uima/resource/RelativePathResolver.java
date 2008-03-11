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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Used by the resource manager to resolve relative URLs to absolute URLs.
 * 
 * 
 */
public interface RelativePathResolver {

  static final String UIMA_DATAPATH_PROP = "uima.datapath";

  /**
   * Gets the data path used to resolve relative paths. More than one directory may be specified by
   * separating them with the System <code>path.separator</code> character (; on windows, : on
   * UNIX). Elements of this path may be absolute or relative file paths.
   * 
   * @return the data path
   */
  public String getDataPath();

  /**
   * Sets the data path used to resolve relative paths. More than one directory may be specified by
   * separating them with the System <code>path.separator</code> character (; on windows, : on
   * UNIX). Elements of this path may be absolute or relative file paths.
   * 
   * @param aPath
   *          the data path
   * 
   * @throws MalformedURLException
   *           if a file path could not be converted to a URL
   */
  public void setDataPath(String aPath) throws MalformedURLException;

  /**
   * Resolves a relative URL to an absolute URL. This will attempt to resolve the URL relative to
   * each element of the data path, sequentially starting with the first element. If this results in
   * an absolute URL at which a file actually exists, that absolute URL is returned. If no file
   * could be found, <code>null</code> is returned.
   * 
   * @param aRelativeUrl
   *          the relative URL to be resolved (if an absolute URL is specified, it will be returned
   *          unmodified if a file actually exists at the URL; otherwise <code>null</code> will be
   *          returned).
   * 
   * @return the absolute URL at which the file exists, <code>null</code> it none could be found.
   */
  public URL resolveRelativePath(URL aRelativeUrl);

  /**
   * Sets the ClassLoader that should be used to resolve the resources.
   * 
   * @param aClassLoader
   *          the ClassLoader that should be used to resolve the resources.
   */
  public void setPathResolverClassLoader(ClassLoader aClassLoader);

}
