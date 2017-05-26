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

package org.apache.uima.internal.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * UIMAClassLoader is used as extension ClassLoader for UIMA to load additional components like
 * annotators and resources. The classpath of the classloader is specified as string.
 * 
 * The strategy for this ClassLoader tries to load the class itself before the classloading is
 * delegated to the application class loader.
 * 
 */
public class UIMAClassLoader extends URLClassLoader {
  
  static {if (!ClassLoader.registerAsParallelCapable()) {
           System.err.println("WARNING - Failed to register the UIMA Class loader as parallel-capable - should never happen");     
          }
         }

  /**
   * locks for loading more than 1 class at a time (on different threads)
   * no more than the total number of cores, rounded up to pwr of 2
   */
  final private static int nbrLocks = Utilities.nextHigherPowerOf2(Runtime.getRuntime().availableProcessors());
  // not static
  final private Object[] syncLocks = new Object[nbrLocks];
   
  /**
   * Transforms the string classpath to a URL array based classpath.
   * 
   * The classpath string must be separated with the filesystem path separator.
   * 
   * @param classpath
   *          a classpath string
   * @return URL[] array of wellformed URL's
   * @throws MalformedURLException
   *           if a malformed URL has occurred in the classpath string.
   */
  public static URL[] transformClasspath(String classpath) throws MalformedURLException {
    // initialize StringTokenizer to separate the classpath
    StringTokenizer tok = new StringTokenizer(classpath, File.pathSeparator);
    // pathList of the classpath entries
    List<String> pathList = new ArrayList<String>();

    // extract all classpath entries and add them to the pathList
    while (tok.hasMoreTokens()) {
      pathList.add(tok.nextToken());
    }
    final int max = pathList.size();
    URL[] urlArray = new URL[max];

    // transform all classpath entries to an URL and add them to an URL array
    for (int i = 0; i < max; i++) {
      urlArray[i] = (new File(pathList.get(i))).toURI().toURL();
      // calling toURI() first handles spaces in classpath
    }

    return urlArray;
  }

  /**
   * Creates a new UIMAClassLoader based on a classpath string
   * 
   * @param classpath
   *          a classpath string
   * 
   * @throws MalformedURLException
   *           if a malformed URL has occurred in the classpath string.
   */
  public UIMAClassLoader(String classpath) throws MalformedURLException {
    super(transformClasspath(classpath));
    commonInit();
  }

  /**
   * Creates a new UIMAClassLoader based on a classpath URL's
   * 
   * @param classpath
   *          an array of wellformed classpath URL's
   */
  public UIMAClassLoader(URL[] classpath) {
    super(classpath);
    commonInit();
  }
  
  /**
   * Creates a new UIMAClassLoader based on a classpath URL's. Also a parent ClassLoader can be
   * specified.
   * 
   * @param classpath
   *          an array of wellformed classpath URL's
   * @param parent
   *          specify the parent of the classloader
   */
  public UIMAClassLoader(URL[] classpath, ClassLoader parent) {
    super(classpath, parent);
    commonInit();
  }

  /**
   * Creates a new UIMAClassLoader based on a classpath string. Also a parent ClassLoader can be
   * specified.
   * 
   * @param classpath
   *          a classpath string
   * @param parent
   *          specify the parent of the classloader
   * 
   * @throws MalformedURLException
   *           if a malformed URL has occurred in the classpath string.
   */
  public UIMAClassLoader(String classpath, ClassLoader parent) throws MalformedURLException {
    super(transformClasspath(classpath), parent);
    commonInit();
  }
  
  private void commonInit() {
      for (int i = 0; i < nbrLocks; i++) {
      syncLocks[i] = new Object();
    }
  }


  /**
   * Do not use this factory method - throws unsupportedOperationException
   * @param urls -
   * @return -
   * @throws UnsupportedOperationException -
   */
  public static URLClassLoader newInstance(final URL[] urls) {
    throw new UnsupportedOperationException();
  }

  /**
   * Do not use this factory method - throws unsupportedOperationException
   * @param urls -
   * @param parent -
   * @return -
   * @throws UnsupportedOperationException -
   */
  public static URLClassLoader newInstance(final URL[] urls, final ClassLoader parent) {
    throw new UnsupportedOperationException();
  }

  /*
   * Try to load the class itself before delegate the class loading to its parent
   */
  protected Class<?> loadClass(String name, boolean resolve)
          throws ClassNotFoundException {
 
    // requirement: ensure that the protected defineClass() method is called only once for each class loader and class name pair.
    // pick a random syncLock to synchronize
    // Although the sync locks are not one/per/class, there should be enough of them to make the likelyhood
    //   of needing to wait very low (unless it's the same class-name being loaded, of course).
    synchronized (syncLocks[name.hashCode() & (nbrLocks - 1)]) {
      // First, check if the class has already been loaded
      Class<?> c = findLoadedClass(name);
      if (c == null) {
        try {
          // try to load class
          c = findClass(name);
        } catch (ClassNotFoundException e) {
          // delegate class loading for this class-name
          c = super.loadClass(name, false);
        }
      }
      if (resolve) {
        resolveClass(c);
      }
      return c;    

    }
  }
  
  /* 
   * loads resource from this class loader first, if possible
   * (non-Javadoc)
   * @see java.lang.ClassLoader#getResource(java.lang.String)
   */
  @Override
  public URL getResource(String name) {
    URL url = findResource(name);
    
    if (null == url) {
      url = super.getResource(name);
    }
    return url;
  }
  
}
