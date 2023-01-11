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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.util.impl.Constants;

/**
 * Reference implementation of {@link RelativePathResolver}.
 */
public class RelativePathResolver_impl implements RelativePathResolver {

  /** Data path as a string. */
  private List<String> mDataPath;

  /** Array of base URLs parsed from the data path. */
  private URL[] mBaseUrls;

  /** ClassLoader to fall back on if resource not in data path. */
  private ClassLoader mClassLoader;

  public RelativePathResolver_impl() {
    this(null);
    mClassLoader = getClass().getClassLoader(); // default value, maybe overridden by
                                                // setPathResolverClassLoader
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
      mDataPath = emptyList();
      mBaseUrls = Constants.EMPTY_URL_ARRAY;
    }
    mClassLoader = aClassLoader;
  }

  @Override
  @Deprecated
  public String getDataPath() {
    String pathSepChar = System.getProperty("path.separator");
    return mDataPath.stream().collect(joining(pathSepChar));
  }

  @Override
  public List<String> getDataPathElements() {
    return mDataPath;
  }

  @Override
  public void setDataPathElements(File... aPaths) throws MalformedURLException {
    if (aPaths == null) {
      mDataPath = emptyList();
      mBaseUrls = Constants.EMPTY_URL_ARRAY;
      return;
    }

    mDataPath = unmodifiableList(Arrays.stream(aPaths) //
            .map(File::getPath) //
            .map(s -> s.replace(File.separator, "/")) //
            .collect(toList()));
    mBaseUrls = new URL[aPaths.length];
    for (int i = 0; i < aPaths.length; i++) {
      // Note, this URL can contain space characters if there were spaces in the
      // datapath. This may not be ideal but we're keeping that behavior for
      // backwards compatibility. Some components relied on this (e.g. by calling
      // URL.getFile() and expecting it to be a valid file name).
      mBaseUrls[i] = aPaths[i].toURL();
    }
  }

  @Override
  public void setDataPathElements(String... aPaths) throws MalformedURLException {
    if (aPaths == null) {
      mDataPath = null;
      mBaseUrls = null;
      return;
    }

    mDataPath = unmodifiableList(Arrays.stream(aPaths).collect(toList()));
    mBaseUrls = new URL[aPaths.length];
    for (int i = 0; i < aPaths.length; i++) {
      // Note, this URL can contain space characters if there were spaces in the
      // datapath. This may not be ideal but we're keeping that behavior for
      // backwards compatibility. Some components relied on this (e.g. by calling
      // URL.getFile() and expecting it to be a valid file name).
      mBaseUrls[i] = new File(aPaths[i]).toURL();
    }
  }

  @Override
  @Deprecated
  public void setDataPath(String aPath) throws MalformedURLException {
    List<URL> urls = new ArrayList<>();
    List<String> paths = new ArrayList<>();

    // tokenize based on path.separator system property
    String pathSepChar = System.getProperty("path.separator");
    StringTokenizer tokenizer = new StringTokenizer(aPath, pathSepChar);
    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken();
      paths.add(tok);
      URL url = new File(tok).toURL();
      urls.add(url);
      // Note, this URL can contain space characters if there were spaces in the
      // datapath. This may not be ideal but we're keeping that behavior for
      // backwards compatibility. Some components relied on this (e.g. by calling
      // URL.getFile() and expecting it to be a valid file name).
    }
    mBaseUrls = urls.toArray(new URL[urls.size()]);
    mDataPath = unmodifiableList(paths);
  }

  @Override
  public URL resolveRelativePath(URL aRelativeUrl) {
    // try each base URL
    URL[] baseUrls = getBaseUrls();
    for (int i = 0; i < baseUrls.length; i++) {
      try {
        URL absUrl = new URL(baseUrls[i], aRelativeUrl.toString());
        // if file exists here, return this URL
        if (fileExistsAtUrl(absUrl)) {
          return absUrl;
        }
      } catch (MalformedURLException e) {
        // ignore and move on to next base URL
      }
    }

    // check if an absolute URL was passed in
    if (aRelativeUrl.getPath().startsWith("/") && fileExistsAtUrl(aRelativeUrl)) {
      return aRelativeUrl;
    }

    // fallback on classloader
    String f = aRelativeUrl.getFile();
    URL absURL = null;
    if (mClassLoader != null) {
      absURL = mClassLoader.getResource(f);
    } 
    
    // fallback on TCCL
    if (absURL == null) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
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
    // set ClassLoader
    mClassLoader = aClassLoader;
  }

  /*
   * Utility method that checks to see if a file exists at the specified URL.
   */
  protected boolean fileExistsAtUrl(URL aUrl) {
    try (InputStream testStream = aUrl.openStream()) {
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * @return the base URLs that were parsed from the data path.
   */
  protected URL[] getBaseUrls() {
    return mBaseUrls;
  }
}
