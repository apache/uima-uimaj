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

package org.apache.uima.resource.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.resource.RelativePathResolver;

/**
 * Reference implementation of {@link RelativePathResolver}.
 */
public class RelativePathResolver_impl implements RelativePathResolver {

  /** Data path as a string. */
  @Deprecated(since = "3.6.0")
  private List<String> mDataPath;

  /** Array of base URLs parsed from the data path. */
  private List<URL> mBaseUrls;

  /** ClassLoader to fall back on if resource not in data path. */
  private ClassLoader mClassLoader;

  public RelativePathResolver_impl() {
    this(MethodHandles.lookup().lookupClass().getClassLoader());
  }

  public RelativePathResolver_impl(ClassLoader aClassLoader) {
    // initialize data path based on uima.datapath System property; if not
    // present fall back on user.dir
    String dataPath = null;
    try {
      dataPath = System.getProperty(UIMA_DATAPATH_PROP);
    } catch (SecurityException e) {
      // can't access system properties
    }

    if (dataPath == null) {
      try {
        dataPath = System.getProperty("user.dir");
      } catch (SecurityException e) {
        // can't access system properties
      }
    }

    if (dataPath == null) {
      // no path info found - use empty data path
      dataPath = "";
    }

    try {
      setDataPath(dataPath);
    } catch (MalformedURLException e) {
      // initialize to empty path
      mDataPath = null;
      mBaseUrls = null;
    }

    mClassLoader = aClassLoader;
  }

  @Override
  @Deprecated
  public String getDataPath() {
    if (mDataPath == null) {
      return "";
    }

    var pathSepChar = System.getProperty("path.separator");
    return mDataPath.stream().collect(joining(pathSepChar));
  }

  @Deprecated
  @Override
  public List<String> getDataPathElements() {
    if (mDataPath == null) {
      return emptyList();
    }

    return unmodifiableList(mDataPath);
  }

  @Override
  public List<URL> getDataPathUrls() {
    if (mBaseUrls == null) {
      return emptyList();
    }

    return unmodifiableList(mBaseUrls);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setDataPathElements(File... aElements) throws MalformedURLException {
    if (aElements == null) {
      mDataPath = null;
      mBaseUrls = null;
      return;
    }

    var baseUrls = new ArrayList<URL>(aElements.length);
    for (var path : aElements) {
      // Note, this URL can contain space characters if there were spaces in the
      // datapath. This may not be ideal but we're keeping that behavior for
      // backwards compatibility. Some components relied on this (e.g. by calling
      // URL.getFile() and expecting it to be a valid file name).
      baseUrls.add(path.toURL());
    }

    mDataPath = stream(aElements) //
            .map(File::getPath) //
            .map(s -> s.replace(File.separator, "/")) //
            .toList();
    mBaseUrls = baseUrls;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setDataPathElements(String... aElements) throws MalformedURLException {
    if (aElements == null) {
      mDataPath = null;
      mBaseUrls = null;
      return;
    }

    var dataPath = new ArrayList<String>(aElements.length);

    var baseUrls = new ArrayList<URL>(aElements.length);
    for (var element : aElements) {
      try {
        var url = new URL(element);
        baseUrls.add(url);
        var protocol = url.getProtocol();
        if ((protocol != null) && protocol.equalsIgnoreCase("file")) {
          dataPath.add(new File(URLDecoder.decode(url.getPath(), UTF_8)).getPath());
        }
      } catch (MalformedURLException e) {
        // Note, this URL can contain space characters if there were spaces in the
        // datapath. This may not be ideal but we're keeping that behavior for
        // backwards compatibility. Some components relied on this (e.g. by calling
        // URL.getFile() and expecting it to be a valid file name).
        baseUrls.add(new File(element).toURL());
        dataPath.add(element);
      }
    }

    mDataPath = dataPath;
    mBaseUrls = baseUrls;
  }

  @Override
  public void setDataPathElements(URL... aElements) {
    if (aElements == null) {
      mDataPath = null;
      mBaseUrls = null;
      return;
    }

    var dataPath = new ArrayList<String>(aElements.length);
    for (var url : aElements) {
      var protocol = url.getProtocol();
      if ((protocol == null) || !protocol.equalsIgnoreCase("file")) {
        continue;
      }
      dataPath.add(new File(URLDecoder.decode(url.getPath(), UTF_8)).getPath());
    }

    mDataPath = dataPath;
    mBaseUrls = asList(aElements);
  }

  @Override
  @Deprecated
  public void setDataPath(String aPath) throws MalformedURLException {
    var urls = new ArrayList<URL>();
    var paths = new ArrayList<String>();

    // tokenize based on path.separator system property
    var pathSepChar = System.getProperty("path.separator");
    var tokenizer = new StringTokenizer(aPath, pathSepChar);
    while (tokenizer.hasMoreTokens()) {
      var tok = tokenizer.nextToken();
      paths.add(tok);
      // Note, this URL can contain space characters if there were spaces in the
      // datapath. This may not be ideal but we're keeping that behavior for
      // backwards compatibility. Some components relied on this (e.g. by calling
      // URL.getFile() and expecting it to be a valid file name).
      urls.add(new File(tok).toURL());
    }

    mDataPath = paths;
    mBaseUrls = urls;
  }

  @Override
  public URL resolveRelativePath(String aPathOrUrl) {
    // check if an URL was passed in - if so, we fall back to the old logic which is a bit odd
    // because for relative URLs, it basically discards the protocol. This is behavior we may
    // want to change on the next major release... e.g. to require that relative paths are
    // always specified without a protocol.
    try {
      var url = new URL(aPathOrUrl);
      return resolveRelativePath(url);
    } catch (MalformedURLException e) {
      // ignore and move on
    }

    // try each base URL
    for (var baseUrl : mBaseUrls) {
      try {
        var absUrl = new URL(baseUrl, aPathOrUrl);
        // if file exists here, return this URL
        if (fileExistsAtUrl(absUrl)) {
          return absUrl;
        }
      } catch (MalformedURLException e) {
        // ignore and move on to next base URL
      }
    }

    // fallback on classloader
    URL absURL = null;
    if (mClassLoader != null) {
      absURL = mClassLoader.getResource(aPathOrUrl);
    }

    // fallback on TCCL
    if (absURL == null) {
      var tccl = Thread.currentThread().getContextClassLoader();
      absURL = tccl.getResource(aPathOrUrl);
    }

    // if no ClassLoader specified (could be the bootstrap classloader), try the system classloader
    if (absURL == null && mClassLoader == null) {
      absURL = ClassLoader.getSystemClassLoader().getResource(aPathOrUrl);
    }

    return absURL;
  }

  @Deprecated
  @Override
  public URL resolveRelativePath(URL aUrl) {
    // try each base URL
    for (var baseUrl : mBaseUrls) {
      try {
        var absUrl = new URL(baseUrl, aUrl.toString());
        // if file exists here, return this URL
        if (fileExistsAtUrl(absUrl)) {
          return absUrl;
        }
      } catch (MalformedURLException e) {
        // ignore and move on to next base URL
      }
    }

    // check if an absolute URL was passed in
    if (aUrl.getPath().startsWith("/") && fileExistsAtUrl(aUrl)) {
      return aUrl;
    }

    // fallback on classloader
    var f = aUrl.getFile();
    URL absURL = null;
    if (mClassLoader != null) {
      absURL = mClassLoader.getResource(f);
    }

    // fallback on TCCL
    if (absURL == null) {
      var tccl = Thread.currentThread().getContextClassLoader();
      absURL = tccl.getResource(f);
    }

    // if no ClassLoader specified (could be the bootstrap classloader), try the system classloader
    if (absURL == null && mClassLoader == null) {
      absURL = ClassLoader.getSystemClassLoader().getResource(f);
    }

    return absURL;
  }

  /**
   * @see org.apache.uima.resource.RelativePathResolver#setPathResolverClassLoader(java.lang.ClassLoader)
   */
  @Override
  public void setPathResolverClassLoader(ClassLoader aClassLoader) {
    mClassLoader = aClassLoader;
  }

  /*
   * Utility method that checks to see if a file exists at the specified URL.
   */
  protected boolean fileExistsAtUrl(URL aUrl) {
    if (aUrl == null) {
      return false;
    }

    if ("file".equals(aUrl.getProtocol())) {
      return new File(aUrl.getPath()).exists();
    }

    try {
      // Ensure that we actually always check the resource for existence. In case of a JAR URL,
      // this is also important to ensure that the ZIP/JAR file is closed again.
      var connection = aUrl.openConnection();
      connection.setDefaultUseCaches(false);
      try (var testStream = connection.getInputStream()) {
        return true;
      }
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * @return the base URLs that were parsed from the data path.
   * @deprecated Use {@link #getDataPathUrls()} instead.
   */
  @Deprecated
  protected URL[] getBaseUrls() {
    return mBaseUrls.toArray(URL[]::new);
  }
}
