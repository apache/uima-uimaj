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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.net.URL;
import java.util.function.Function;

import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.util.Misc;

/**
 * UIMATypeSystemClassLoaderInjector supports injected generated class definitions into an existing class loader.
 * This may be used when multiple type systems are not in use.  Otherwise, @see {@link UIMATypeSystemClassLoader}
 * 
 * Redefinition of existing types is supported but may be ineffective if other classes have become "linked" to 
 * classes and methods being defined.
 * 
 * This is intended to be use in a batch mode, as opposed to using the UIMATypeSystemClassLoader, which permits
 * lazy generating and loading of JCas Classes.
 * 
 * To use, first create an instance of this class, specifying the type system and the class loader to inject these
 * classes into.
 * 
 * Then call generateAndLoadJCasClass for all types needing to be generated. 
 */
public class UIMATypeSystemClassLoaderInjector {
  
  private static final Class<String> sc = String.class;
  private static final Class<ClassLoader> clc = ClassLoader.class;
  
  /**
   * JCas classes (including xxxx_Type) are provided (for built-ins) or generated (from merged type system information)
   * The generated classes are loaded under an instance of this class loader, 
   * which is also then the loader (or the parent class loader) used for loading the UIMA pipeline's classes, 
   * including the application, external resources, annotators, and customized JCasGen'd classes (if any). 
   */
  final private TypeSystemImpl tsi;  
  
  final private ClassLoader cl;
  
  /** use these xxx.invokeExact(args) **/
  MethodHandle mhFindLoadedClass;
  MethodHandle mhGetPackage;
  MethodHandle mhDefinePackage;
  MethodHandle mhDefineClass;
  MethodHandle mhResolveClass;
  
  /**
   * Creates a new UIMAClassLoader based on a classpath URL's
   * 
   * @param classpath
   *          an array of wellformed classpath URL's
   */
  public UIMATypeSystemClassLoaderInjector(ClassLoader cl, TypeSystemImpl tsi) {
    this.cl = cl;
    this.tsi = tsi;
    
    Lookup methodHandleAccessContext = MethodHandles.lookup();
    
    mhFindLoadedClass = Misc.getProtectedMethodHandle(clc, methodHandleAccessContext, "findLoadedClass", sc);
    mhGetPackage      = Misc.getProtectedMethodHandle(clc, methodHandleAccessContext, "getPackage", sc);
    mhDefinePackage   = Misc.getProtectedMethodHandle(clc, methodHandleAccessContext, "definePackage", sc, sc, sc, sc, sc, sc, sc, URL.class);
    mhDefineClass     = Misc.getProtectedMethodHandle(clc, methodHandleAccessContext, "defineClass", sc, byte[].class, int.class, int.class);
    mhResolveClass    = Misc.getProtectedMethodHandle(clc, methodHandleAccessContext, "resolveClass", Class.class);      
  }
  
  /**
   * Generate a JCas cover class and its _Type variant, and load it using the class loader specified in the constructor
   * @param rootname
   * @return the loaded and resolved class
   */
  public Class<?> generateAndLoadClass(String rootname) {
    try {
      // redefining supported  TODO check if this works
//      Class<?> c = (Class<?>) mhFindLoadedClass.invokeExact(rootname);
//     
//      if (c != null) {
//        return c;  // assume resolved
//      }
      
      final int i = rootname.lastIndexOf('.');
      final String packageName = (i == -1) ? null : rootname.substring(0, i);
      
      // package must exist before defineClass is called
      if (mhGetPackage.invokeExact(packageName) == null) {
        mhDefinePackage.invokeExact(packageName, null, null, null, null, null, null, null);
      }
      byte[] b = tsi.jcasGenerate(rootname);  // has no static refs to _Type
      Class<?> c = (Class<?>) mhDefineClass.invokeExact(rootname, b, 0, b.length);
      
      b = tsi.jcas_TypeGenerate(rootname);
      mhResolveClass.invokeExact(mhDefineClass.invokeExact(rootname + "_Type", b, 0, b.length)); // always resolve _Type classes, static ref by rootclass
      mhResolveClass.invokeExact(c);
      
      return c;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}