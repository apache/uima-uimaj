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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceManager;

//@formatter:off
/**
 * Utilities supporting a unified approach to loading classes,
 * incorporating the resource manager's classloader if available,
 * and making use of the Thread Context Class Loader (TCCL)
 *
 * For backwards compatibility, if a class is not found using the 
 * Thread Context Class Loader, 
 *   for classloading: try again using the 
 *                     class loader for this class since that's what the previous impl did,
 *                     and some applications will break otherwise, with class-not-found.
 *   for resourceloading: try again using the Classloader.getSystemClassLoader, 
 *                     since that's what the previous impl did
 */
//@formatter:on
public class Class_TCCL {

  static public <T> Class<T> forName(String className) throws ClassNotFoundException {
    return forName(className, null, true);
  }

  static public <T> Class<T> forName(String className, ResourceManager rm)
          throws ClassNotFoundException {
    return forName(className, rm, true);
  }

  static public <T> Class<T> forName(String className, ResourceManager rm, boolean resolve)
          throws ClassNotFoundException {
    List<ClassLoader> clsTried = new ArrayList<>();
    List<ClassNotFoundException> suppressedExceptions = new ArrayList<>();
    
    // Try extension classloader
    if (rm != null) {
      ClassLoader excl = rm.getExtensionClassLoader();
      
      if (excl != null) {
        try {
          return (Class<T>) Class.forName(className, resolve, excl);
        }
        catch (ClassNotFoundException e) {
          clsTried.add(excl);
          suppressedExceptions.add(e);
        }
      }
    }
    
    // Try TCCL
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    if (tccl != null) {
      try {
        return (Class<T>) Class.forName(className, resolve, tccl);
      }
      catch (ClassNotFoundException e) {
        clsTried.add(tccl);
        suppressedExceptions.add(e);
      }
    }
    
    try {
      return (Class<T>) Class.forName(className, resolve, Class_TCCL.class.getClassLoader());
    }
    catch (ClassNotFoundException e) {
      clsTried.add(tccl);
      suppressedExceptions.add(e);
    }
    
    ClassNotFoundException e = new ClassNotFoundException(
            "Class [" + className + "] not found in any of the accessible classloaders "
                    + clsTried.stream().map(Objects::toString).collect(Collectors.joining(", ")));
    suppressedExceptions.forEach(e::addSuppressed);
    throw e;
  }

  static public <T> Class<T> forName(String className, Map<String, Object> additionalParams)
          throws ClassNotFoundException {
    ResourceManager rm = (additionalParams != null)
            ? (ResourceManager) additionalParams.get(Resource.PARAM_RESOURCE_MANAGER)
            : null;
    return forName(className, rm);
  }

  /**
   * @deprecated Method should not be used and will be removed in a future version.
   */
  @Deprecated
  static public ClassLoader get_cl(ResourceManager rm) {

    ClassLoader cl = (rm == null) ? null : rm.getExtensionClassLoader();

    if (cl == null) {
      cl = get_parent_cl();
    }

    return cl;
  }

  /**
   * @deprecated Method should not be used and will be removed in a future version. 
   */
  @Deprecated
  static public ClassLoader get_parent_cl() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return (cl == null) ? Class_TCCL.class.getClassLoader() : cl;
  }

  // only for Java 8 + , needs lambda
  // static public <T> Class<T> loadclass_throw_if_not_found(
  // String className, ResourceManager rm, Supplier<String> sourceUrl)
  // throws ResourceInitializationException {
  // try {
  // Class<T> c = forName(className, rm);
  // return c;
  // } catch (ClassNotFoundException e) {
  // throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
  // new Object[] { className, sourceUrl.get() }, e);
  // }
  // }
}
