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

import static java.util.Collections.emptyEnumeration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.impl.FSClassRegistry;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.spi.FsIndexCollectionProvider;
import org.apache.uima.spi.JCasClassProvider;
import org.apache.uima.spi.TypePrioritiesProvider;
import org.apache.uima.spi.TypeSystemDescriptionProvider;
import org.apache.uima.spi.TypeSystemProvider;

/**
 * UIMAClassLoader is used as extension ClassLoader for UIMA to load additional components like
 * annotators and resources. The classpath of the classloader is specified as string.
 * 
 * The strategy for this ClassLoader tries to load the class itself before the classloading is
 * delegated to the application class loader.
 * 
 * This loader supports loading a special class "MethodHandlesLookup" from
 * org.apache.uima.cas.impl.MethodHandlesLookup This is loaded from a byte string in order to have
 * the defaulting mechanism for MethodHandlesLookup default to this class loaders context.
 * 
 */
public class UIMAClassLoader extends URLClassLoader {
  private static final URL[] NO_URLS = new URL[0];

  static {
    if (!ClassLoader.registerAsParallelCapable()) {
      System.err.println(
              "WARNING - Failed to register the UIMA Class loader as parallel-capable - should never happen");
    }
  }

  /**
   * This is a trick to allow loading the same class multiple times in different UIMAClassLoaders
   * https://issues.apache.org/jira/browse/UIMA-5030
   */
  public static final String MHLC = "org.apache.uima.cas.impl.MethodHandlesLookup";
  /**
   * This is the byte array that defines the class org.apache.uima.cas.impl.MethodHandlesLookup,
   * obtained by converting the .class file to a hex byte string.
   */
  static byte[] methodHandlesLookupClass = Misc.hex_string_to_bytearray(
          "CAFEBABE00000034001B07000201002C6F72672F6170616368652F75696D612F6361732F696D706C2F4D6574686F6448616E646C65734C6F"
                  + "6F6B75700700040100106A6176612F6C616E672F4F626A6563740100063C696E69743E010003282956010004436F64650A000300090C0005"
                  + "000601000F4C696E654E756D6265725461626C650100124C6F63616C5661726961626C655461626C650100047468697301002E4C6F72672F"
                  + "6170616368652F75696D612F6361732F696D706C2F4D6574686F6448616E646C65734C6F6F6B75703B0100166765744D6574686F6448616E"
                  + "646C65734C6F6F6B757001002928294C6A6176612F6C616E672F696E766F6B652F4D6574686F6448616E646C6573244C6F6F6B75703B0A00"
                  + "11001307001201001E6A6176612F6C616E672F696E766F6B652F4D6574686F6448616E646C65730C0014000F0100066C6F6F6B757001000A"
                  + "536F7572636546696C650100184D6574686F6448616E646C65734C6F6F6B75702E6A61766101000C496E6E6572436C617373657307001901"
                  + "00256A6176612F6C616E672F696E766F6B652F4D6574686F6448616E646C6573244C6F6F6B75700100064C6F6F6B75700021000100030000"
                  + "00000002000200050006000100070000002F00010001000000052AB70008B100000002000A0000000600010000001A000B0000000C000100"
                  + "000005000C000D00000009000E000F00010007000000240001000000000004B80010B000000002000A0000000600010000001D000B000000"
                  + "0200000002001500000002001600170000000A000100180011001A0019");

  /**
   * locks for loading more than 1 class at a time (on different threads) no more than the total
   * number of cores, rounded up to pwr of 2
   */
  private static final int nbrLocks = Misc
          .nextHigherPowerOf2(Runtime.getRuntime().availableProcessors());

  // not static
  private final Object[] syncLocks = new Object[nbrLocks];

  private boolean isClosed = false;

  /**
   * Transforms the string classpath to a URL array based classpath.
   * 
   * The classpath string must be separated with the file system path separator.
   * 
   * @param classpath
   *          a classpath string
   * @return URL[] array of well-formed URL's
   * @throws MalformedURLException
   *           if a malformed URL has occurred in the classpath string.
   */
  public static URL[] transformClasspath(String classpath) throws MalformedURLException {
    // initialize StringTokenizer to separate the classpath
    StringTokenizer tok = new StringTokenizer(classpath, File.pathSeparator);

    // pathList of the classpath entries
    List<String> pathList = new ArrayList<>();

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
    super(Misc.classpath2urls(classpath));
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
   * Creates a new UIMAClassLoader with the given parent ClassLoader.
   * 
   * @param parent
   *          specify the parent of the classloader
   */
  public UIMAClassLoader(ClassLoader parent) {
    super(NO_URLS, parent);
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
    super(Misc.classpath2urls(classpath), parent);
    commonInit();
  }

  private void commonInit() {
    for (int i = 0; i < nbrLocks; i++) {
      syncLocks[i] = new Object();
    }
  }

  /**
   * Do not use this factory method - throws unsupportedOperationException
   * 
   * @param urls
   *          -
   * @return -
   * @throws UnsupportedOperationException
   *           -
   */
  public static URLClassLoader newInstance(final URL[] urls) {
    throw new UnsupportedOperationException();
  }

  /**
   * Do not use this factory method - throws unsupportedOperationException
   * 
   * @param urls
   *          -
   * @param parent
   *          -
   * @return -
   * @throws UnsupportedOperationException
   *           -
   */
  public static URLClassLoader newInstance(final URL[] urls, final ClassLoader parent) {
    throw new UnsupportedOperationException();
  }

  /*
   * Try to load the class itself before delegate the class loading to its parent String is like
   * x.y.Foo
   */
  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

    // requirement: ensure that the protected defineClass() method is called only once for each
    // class loader and class name pair.
    // pick a random syncLock to synchronize
    // Although the sync locks are not one/per/class, there should be enough of them to make the
    // likelihood of needing to wait very low (unless it's the same class-name being loaded, of
    // course).
    synchronized (syncLocks[name.hashCode() & (nbrLocks - 1)]) {
      Class<?> c = null;

      if (c == null) {
        // Check if the class has already been loaded
        c = findLoadedClass(name);
      }

      if (c == null) {
        try {
          // try to load class
          if (MHLC.equals(name)) {
            c = defineClass(MHLC, methodHandlesLookupClass, 0, methodHandlesLookupClass.length);
          } else {
            c = findClass(name);
          }
        } catch (ClassNotFoundException e) {
          if (isUimaInternalPackage(name)) {
            // There may be cases where the target class uses a classloader that has no access
            // to the UIMA internal classes - in particular to the FSGenerator3 - so we force using
            // the UIMA classloader in this case.
            c = UIMAFramework.class.getClassLoader().loadClass(name);
          } else {
            // delegate class loading for this class-name
            c = super.loadClass(name, false);
          }
        }
      }

      if (resolve) {
        resolveClass(c);
      }

      // Accessing the interfaces will implicitly trigger resolution - but we can't really help
      // ourselves at the moment
      if (isUimaSpiImplementation(c)) {
        // We never want to return local SPI implementations loaded by the UIMAClassLoader
        // https://github.com/apache/uima-uimaj/issues/431
        var parent = getParent();
        if (parent != null) {
          c = parent.loadClass(name);
        } else {
          c = ClassLoader.getSystemClassLoader().loadClass(name);
        }
      }

      return c;
    }
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    synchronized (syncLocks[name.hashCode() & (nbrLocks - 1)]) { // https://issues.apache.org/jira/browse/UIMA-5741
      Enumeration<URL> delegateResources;
      var parent = getParent();
      if (parent != null) {
        delegateResources = parent.getResources(name);
      } else {
        delegateResources = ClassLoader.getSystemClassLoader().getResources(name);
      }

      Enumeration<URL> localResources = emptyEnumeration();
      if (!isUimaSpiConfigurationFile(name)) {
        localResources = findResources(name);
      }

      return new CombinedEnumeration<>(localResources, delegateResources);
    }
  }

  @Override
  public URL getResource(String name) {

    synchronized (syncLocks[name.hashCode() & (nbrLocks - 1)]) { // https://issues.apache.org/jira/browse/UIMA-5741
      if (isUimaSpiConfigurationFile(name)) {
        // We never want to return local SPI implementations
        // https://github.com/apache/uima-uimaj/issues/431
        var parent = getParent();
        if (parent != null) {
          return parent.getResource(name);
        }

        return ClassLoader.getSystemClassLoader().getResource(name);
      }

      var url = findResource(name);

      if (null == url) {
        url = super.getResource(name);
      }

      return url;
    }
  }

  private boolean isUimaSpiImplementation(Class<?> c) {
    return TypeSystemProvider.class.isAssignableFrom(c)
            || TypeSystemDescriptionProvider.class.isAssignableFrom(c)
            || JCasClassProvider.class.isAssignableFrom(c)
            || FsIndexCollectionProvider.class.isAssignableFrom(c)
            || TypePrioritiesProvider.class.isAssignableFrom(c);
  }

  private boolean isUimaInternalPackage(String name) {
    return name.startsWith("org.apache.uima.cas.impl.")
            || name.startsWith("org.apache.uima.jcas.cas.");
  }

  private boolean isUimaSpiConfigurationFile(String name) {
    return name.contains("META-INF/services/org.apache.uima.spi.");
  }

  /**
   * The UIMA Class Loader extends URLClassLoader. This kind of classloader supports the close()
   * method.
   * 
   * When this class loader is closed, it remembers this.
   *
   * @return true if this class loader has been closed.
   */
  public boolean isClosed() {
    return isClosed;
  }

  @Override
  public void close() throws IOException {
    isClosed = true;
    // There is a circular dependency between the static initializer blocks of FSClassRegistry and
    // TypeSystemImpl which requires that the TypeSystemImpl class must be initialized before the
    // FSClassRegistry to avoid exceptions. The if-statement here is a red-herring because the
    // actual comparison does not really matter - under normal circumstances, `staticTsi` cannot be
    // null.
    // However, what it really does is trigger the static initialization block of TypeSystemImpl
    // so that the subsequent call to FSClassRegistry does not trigger an exception.
    if (TypeSystemImpl.staticTsi != null) {
      FSClassRegistry.unregister_jcci_classloader(this);
    }
    super.close();
  }

  // Package-scope visibility for testing
  Object getClassLoadingLockForTesting(String aClassName) {
    return super.getClassLoadingLock(aClassName);
  }

  private static class CombinedEnumeration<T> implements Enumeration<T> {
    private final Enumeration<T> first;
    private final Enumeration<T> second;

    public CombinedEnumeration(Enumeration<T> first, Enumeration<T> second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean hasMoreElements() {
      return first.hasMoreElements() || second.hasMoreElements();
    }

    @Override
    public T nextElement() {
      if (first.hasMoreElements()) {
        return first.nextElement();
      } else if (second.hasMoreElements()) {
        return second.nextElement();
      } else {
        throw new NoSuchElementException("No more elements");
      }
    }
  }
}
