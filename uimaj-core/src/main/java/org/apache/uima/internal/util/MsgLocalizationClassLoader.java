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

import java.net.URL;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Class Loader for loading localized messages
 * See https://issues.apache.org/jira/browse/UIMA-1714
 * Delegates to other ClassLoaders, in the following order
 *   the class loader that loaded the 2nd previous caller
 *   the class loader that loaded the 3rd previous caller
 *   etc.
 *   and finally, the thread local context loader, if it exists UIMA-3692
 * Note: the caller of this method is presumed to be framework code
 *   that was, in turn, called to perform some logging or whatever,
 *   so we skip the 1st previous caller.
 *   
 * Note that each of these class loaders will, in turn, delegate
 *   if they are set up to do so
 *   
 * Note: if a caller's class loader is the same as the previously tried one,
 *   we skip it (simple speed optimization, and may avoid some kind of strange loop)  
 */

public class MsgLocalizationClassLoader {

  /** 
   * this inner class only for purposes of getting access to the protected method
   * to get the call stack
   */
  static class CallStack extends SecurityManager {
    Class<?>[] getCallStack() {
      return getClassContext();
    }
  }
  
  static final CallStack csi = new CallStack();

  static class CallClimbingClassLoader extends ClassLoader {
  
    static final ThreadLocal<ClassLoader> originalTccl = new ThreadLocal<>();
    /*
     * Try to load the class itself before delegate the class loading to its parent
     */
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
      // First, check if the class has already been loaded
      Class<?> c = findLoadedClass(name);
      if (c == null) {
        // try to load class
        c = findClass(name);  // may throw; if so, don't delegate, delegation has already been done
      }
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }
  
    /**
     * Called after findLoadedClass has returned null
     * Delegates loading in specific order
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      Map<ClassLoader,ClassLoader> alreadySearched = new IdentityHashMap<ClassLoader, ClassLoader>(7);
      // get the call stack
      Class<?>[] cs = new CallStack().getCallStack();
      // start at the caller of the caller's class loader
      // cs[0] is getClassContext
      // cs[1] is getCallStack
      // cs[2] is this method, find class     
      for (int i = 3; i < cs.length; i++) {
        Class<?> callingClass = cs[i];
        ClassLoader cl = callingClass.getClassLoader();
        if (null == cl) { // means system class loader
          cl = ClassLoader.getSystemClassLoader();
        }
        if (null != alreadySearched.get(cl)) {
          continue;
        }
        alreadySearched.put(cl, cl);
        Class<?> c = null;
        try {
          c = cl.loadClass(name);  // include delegation
          return c;
        } catch (ClassNotFoundException e) {
          // leave c == null
        }      
      }
      // UIMA-3692, UIMA-4793 try the thread context class loader
      // if not found, will return class not found exception
      try {
        ClassLoader cl = originalTccl.get();
        if (cl != null) {
          return cl.loadClass(name);
        }
      } catch (ClassNotFoundException e) {}
      // last try: the current thread context class loader
      return Thread.currentThread().getContextClassLoader().loadClass(name);
    }
    
    @Override
    public URL getResource(String name) {
      Map<ClassLoader,ClassLoader> alreadySearched = new IdentityHashMap<ClassLoader, ClassLoader>(7);
      // get the call stack
      Class<?>[] cs = new CallStack().getCallStack();
      // start at the caller of the caller's class loader
      // cs[0] is getClassContext
      // cs[1] is getCallStack
      // cs[2] is this method, find class
      for (int i = 3; i < cs.length; i++) {
        Class<?> callingClass = cs[i];
        ClassLoader cl = callingClass.getClassLoader();
        if (null == cl) { // means system class loader
          cl = ClassLoader.getSystemClassLoader();
        }
        if (null != alreadySearched.get(cl)) {
          continue;
        }
        alreadySearched.put(cl, cl);

        URL c = cl.getResource(name);  // include delegation
        if (null != c) {
          return c;
        }    
      }
      // UIMA-3692, UIMA-4793  try the thread context class loader
      // if not found, will return class not found exception
      ClassLoader cl = originalTccl.get();
      if (cl != null) {
        URL c = cl.getResource(name);
        if (null != c) {
          return c;
        }
      }
      return Thread.currentThread().getContextClassLoader().getResource(name);
    }
  }
  
  private static final CallClimbingClassLoader MSG_LOCALIZATION_CLASS_LOADER = 
    new CallClimbingClassLoader();
  
  public static URL getResource(String name) {
    return MSG_LOCALIZATION_CLASS_LOADER.getResource(name);
  }
  
  public static ClassLoader getMsgLocalizationClassLoader() {
    return MSG_LOCALIZATION_CLASS_LOADER;
  }
  
  public static Class<?> loadClass(String name) throws ClassNotFoundException {
    return MSG_LOCALIZATION_CLASS_LOADER.loadClass(name);
  }
}
