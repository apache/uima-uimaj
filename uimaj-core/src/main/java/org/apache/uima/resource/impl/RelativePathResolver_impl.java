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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.resource.RelativePathResolver;

/**
 * Reference implementation of {@link RelativePathResolver}.
 * 
 * 
 */
public class RelativePathResolver_impl implements RelativePathResolver {

  /** Data path as a string. */
  private String mDataPath;

  /** Array of base URLs parsed from the data path. */
  private URL[] mBaseUrls;

  /** ClassLoader to fall back on if resource not in data path. */
  private ClassLoader mClassLoader;

  public RelativePathResolver_impl() {
    this(null);
    mClassLoader = getClass().getClassLoader();
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
      mDataPath = "";
      mBaseUrls = new URL[0];
    }
    mClassLoader = aClassLoader;
  }

  /**
   * @see org.apache.uima.resource.RelativePathResolver#getDataPath()
   */
  public String getDataPath() {
    return mDataPath;
  }

  /**
   * @see org.apache.uima.resource.RelativePathResolver#setDataPath(java.lang.String)
   */
  public void setDataPath(String aPath) throws MalformedURLException {
    List<URL> urls = new ArrayList<URL>();

    // tokenize based on path.separator system property
    String pathSepChar = System.getProperty("path.separator");
    StringTokenizer tokenizer = new StringTokenizer(aPath, pathSepChar);
    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken();
      URL url = new File(tok).toURL();
      urls.add(url);
      // Note, this URL can contain space characters if there were spaces in the
      // datapath. This may not be ideal but we're keeping that behavior for
      // backwards compatibility. Some components relied on this (e.g. by calling
      // URL.getFile() and expecting it to be a valid file name).
    }
    mBaseUrls = new URL[urls.size()];
    urls.toArray(mBaseUrls);
    mDataPath = aPath;
  }

  /**
   * @see org.apache.uima.resource.RelativePathResolver#resolveRelativePath(java.net.URL)
   */
  public URL resolveRelativePath(URL aRelativeUrl) {
    // try each base URL
    URL[] baseUrls = getBaseUrls();
    for (int i = 0; i < baseUrls.length; i++) {
      try {
        URL absUrl = new URL(baseUrls[i], aRelativeUrl.toString());
        // if file exists here, return this URL
        if (fileExistsAtUrl(absUrl))
          return absUrl;
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
    URL absURL;
    if (mClassLoader != null) {
      absURL = mClassLoader.getResource(f);
    } else // if no ClassLoader specified (could be the bootstrap classloader), try the system
    // classloader
    {
      absURL = ClassLoader.getSystemClassLoader().getResource(f);
    }
    if (absURL != null) {
      return absURL;
    }

    // no file could be found
    return null;
  }

  /**
   * @see org.apache.uima.resource.RelativePathResolver#setPathResolverClassLoader(java.lang.ClassLoader)
   */
  public void setPathResolverClassLoader(ClassLoader aClassLoader) {
    // set ClassLoader
    mClassLoader = aClassLoader;
  }

  /*
   * Utility method that checks to see if a file exists at the specified URL.
   */
  protected boolean fileExistsAtUrl(URL aUrl) {
    InputStream testStream = null;
    try {
      testStream = aUrl.openStream();
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      if (testStream != null) {
        try {
          testStream.close();
        } catch (IOException e) {
        }
      }
    }
  }

  /**
   * @return the base URLs that were parsed from the data path.
   */
  protected URL[] getBaseUrls() {
    return mBaseUrls;
  }
}
