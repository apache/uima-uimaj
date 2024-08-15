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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Used by the resource manager to resolve relative URLs to absolute URLs.
 */
public interface RelativePathResolver {

  String UIMA_DATAPATH_PROP = "uima.datapath";

  /**
   * Gets the data path used to resolve relative paths. More than one directory may be specified by
   * separating them with the System <code>path.separator</code> character (; on windows, : on
   * UNIX). Elements of this path may be absolute or relative file paths.
   * <p>
   * <b>Note:</b> This method will only return file paths. If any non-file URLs have been added to
   * the data path e.g via {@link #setDataPathElements(URL...)}, these will not be included. Use
   * {@link #getDataPathUrls()} to get a full list.
   * 
   * @return the data path
   * @deprecated Use {@link #getDataPathElements} instead.
   */
  @Deprecated(since = "3.3.0")
  String getDataPath();

  /**
   * Gets the data path used to resolve relative paths. Elements of this path may be absolute or
   * relative file paths.
   * <p>
   * <b>Note:</b> This method will only return file paths. If any non-file URLs have been added to
   * the data path e.g via {@link #setDataPathElements(URL...)}, these will not be included. Use
   * {@link #getDataPathUrls()} to get a full list.
   * 
   * @return the data path
   * @deprecated Use {@link #getDataPathUrls()} instead.
   */
  @Deprecated(since = "3.6.0")
  List<String> getDataPathElements();

  /**
   * Gets the data path used to resolve relative paths. Elements of this path may be absolute or
   * relative URLs.
   * 
   * @return the data path
   */
  List<URL> getDataPathUrls();

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
   * @deprecated Use {@link #setDataPathElements} instead.
   */
  @Deprecated(since = "3.3.0")
  void setDataPath(String aPath) throws MalformedURLException;

  /**
   * Sets the data path elements used to resolve relative paths. Elements of this path may be URLs
   * or absolute or relative file paths.
   * 
   * @param aElements
   *          the data path elements
   * 
   * @throws MalformedURLException
   *           if a file path could not be converted to a URL
   */
  void setDataPathElements(String... aElements) throws MalformedURLException;

  /**
   * Sets the data path elements used to resolve relative paths. Elements of this path may be
   * absolute or relative file paths.
   * 
   * @param aElements
   *          the data path elements
   * 
   * @throws MalformedURLException
   *           if a file path could not be converted to a URL
   */
  void setDataPathElements(File... aElements) throws MalformedURLException;

  /**
   * Sets the data path elements used to resolve relative paths. Elements of this path may be
   * absolute or relative URLs.
   * 
   * @param aURLs
   *          the data path aURLs
   */
  void setDataPathElements(URL... aURLs);

  /**
   * Resolves an URLrelative to each element of the data path, sequentially starting with the first
   * element. If this results in an absolute URL at which a file actually exists, that absolute URL
   * is returned. If no file could be found, <code>null</code> is returned.
   * 
   * @param aUrl
   *          the URL to be resolved (if an absolute URL is specified, it will be returned
   *          unmodified if a file actually exists at the URL; otherwise <code>null</code> will be
   *          returned).
   * 
   * @return the absolute URL at which the file exists, <code>null</code> it none could be found.
   * @deprecated Use {@link #resolveRelativePath(String)} instead.
   */
  @Deprecated
  URL resolveRelativePath(URL aUrl);

  /**
   * Resolves a path relative to each element of the data path, sequentially starting with the first
   * element. If this results in an absolute URL at which a file actually exists, that absolute URL
   * is returned. If no file could be found, <code>null</code> is returned.
   * <p>
   * <b>Note:</b> For backwards compatibility, it is still allowed to specify relative URLs here
   * such as {@code file:foo/foo.txt} and this will effectively work as if {@code foo/foo.txt} had
   * been specified. The protocol is simply discarded. Future versions will no longer accept a
   * protocol in relative paths.
   * 
   * @param aPathOrUrl
   *          the path/URL to be resolved (if an absolute URL is specified, it will be returned
   *          unmodified if a file actually exists at the URL; otherwise <code>null</code> will be
   *          returned).
   * 
   * @return the absolute URL at which the file exists, <code>null</code> it none could be found.
   */
  URL resolveRelativePath(String aPathOrUrl);

  /**
   * Sets the ClassLoader that should be used to resolve the resources.
   * 
   * @param aClassLoader
   *          the ClassLoader that should be used to resolve the resources.
   */
  void setPathResolverClassLoader(ClassLoader aClassLoader);
}
