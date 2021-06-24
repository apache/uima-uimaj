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
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.util.Misc;

/**
 * UIMATypeSystemClassLoader is used only to load generated JCas classes, lazily.
 * Multiple instances of these support having multiple type systems in use in one JVM; it is up to the 
 * application setup code to create these class loaders.
 * 
 *   For simple cases, without multiple type systems, if you don't care about lazy instantiation, you can 
 *   have type system commit do a batch generation and load into a specific arbitrary class loader; 
 *   @see {@link UIMATypeSystemClassLoaderInjector}.
 * 
 * For Application, External Resource, and Annotator code to have references to these JCas classes, 
 * that code must be loaded under this ClassLoader (or a child of it). 
 * 
 * This loader has no class path; it is only used for lazy generation and loading of JCas classes
 * 
 * Because the _Type class has static references to the main JCas class, the main JCas class is loaded first.
 * 
 * This loader has a reference to the committed type system, that starts out as null.  Type system commit 
 * sets it.  If it is already set, a second set is an error unless the type systems are the same.
 * 
 * This loader does nothing unless the type system ref is set.  Then it looks up the type being loaded, and if it matches
 * a JCas type, it generates and loads the main and _Type classes. Otherwise, it delegates to its parent.
 * 
 */
public class UIMATypeSystemClassLoader extends ClassLoader {
  
  static {if (!ClassLoader.registerAsParallelCapable()) {
           System.err.println("WARNING - Failed to register the UIMA Class loader as parallel-capable - should never happen");     
          }
         }

  /**
   * locks for loading more than 1 class at a time (on different threads)
   * no more than the total number of cores, rounded up to pwr of 2
   */
  final private static int nbrLocks = Misc.nextHigherPowerOf2(Runtime.getRuntime().availableProcessors());
  // not static
  final private Object[] syncLocks = new Object[nbrLocks];
  {  for (int i = 0; i < nbrLocks; i++) {
       syncLocks[i] = new Object();
     }
  }

  /**
   * JCas classes (including xxxx_Type) are provided (for built-ins) or generated (from merged type system information)
   * The generated classes are loaded under an instance of this class loader, 
   * which is also then the loader (or the parent class loader) used for loading the UIMA pipeline's classes, 
   * including the application, external resources, annotators, and customized JCasGen'd classes (if any). 
   */
  private TypeSystemImpl tsi = null;  // The corresponding TypeSystemImpl or null
  
  /**
   * Creates a new UIMATypeSystemClassLoader
   */
  public UIMATypeSystemClassLoader() {
    super();
  }
  
  /**
   * Creates a new UIMAClassLoader, for a particular parent ClassLoader
   * 
   * @param parent
   *          specify the parent of the classloader
   */
  public UIMATypeSystemClassLoader(ClassLoader parent) {
    super(parent);
  }

  /*
   * Try to generate and load the JCas class before delegate the class loading to its parent
   * String is like x.y.Foo
   */
  protected Class<?> loadClass(String name, boolean resolve)
          throws ClassNotFoundException {
 
    if (tsi == null) {
      return super.loadClass(name, resolve);
    }
    
    String rootname = (name.endsWith("_Type")) ? name.substring(0, name.length() - 5) : name;
    if (!tsi.isJCasGenerateOnLoad(rootname)) {
      return super.loadClass(name, resolve);
    }
    
    // requirement: ensure that the protected defineClass() method is called only once for each class loader and class name pair.
    // pick a random syncLock to synchronize
    // Although the sync locks are not one/per/class, there should be enough of them to make the likelyhood
    //   of needing to wait very low (unless it's the same class-name being loaded, of course).
    synchronized (syncLocks[name.hashCode() & (nbrLocks - 1)]) {
      // First, check if the class has already been loaded
      Class<?> c = findLoadedClass(name);
      if (c == null) {
        // package must exist before defineClass is called
        final int i = name.lastIndexOf('.');
        final String packageName = (i == -1) ? null : name.substring(0, i);
        if (packageName != null && getPackage(packageName) == null) {
          definePackage(packageName, null, null, null, null, null, null, null);
        }
        
        byte[] b = tsi.jcasGenerate(rootname);  // has no static refs to _Type
        c = defineClass(rootname, b, 0, b.length);
        
        b = tsi.jcas_TypeGenerate(rootname);
        resolveClass(defineClass(rootname + "_Type", b, 0, b.length)); // always resolve _Type classes, static ref by rootclass
      }
      if (resolve) {
        resolveClass(c);
      }
      return c;    
    }
  }

  public void setTsi(TypeSystemImpl tsi) {
    this.tsi = tsi;
  }
  
  
}
