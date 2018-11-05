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

import java.util.Map;
import java.util.function.Supplier;

import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;

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
public class Class_TCCL {

  static public <T> Class<T> forName(String className) 
      throws ClassNotFoundException {
    return forName(className, null, true);
  }
  
  static public <T> Class<T> forName(String className, ResourceManager rm) 
      throws ClassNotFoundException {
    return forName(className, rm, true);
  }  
  
  static public <T> Class<T> forName(String className, ResourceManager rm, boolean resolve) 
      throws ClassNotFoundException {
    try {
      return (Class<T>) Class.forName(className, resolve, get_cl(rm));
    } catch (ClassNotFoundException x) {  // 
      return (Class<T>) Class.forName(className, resolve, Class_TCCL.class.getClassLoader());
    }
  }
  
  static public <T> Class<T> forName(String className, Map<String, Object> additionalParams) 
      throws ClassNotFoundException {
    ResourceManager rm = (additionalParams != null)
                           ? (ResourceManager) additionalParams.get(Resource.PARAM_RESOURCE_MANAGER)
                           : null;
    return forName(className, rm);
  }
  
  static public ClassLoader get_cl(ResourceManager rm) {
    
    ClassLoader cl = (rm == null) ? null : rm.getExtensionClassLoader();
    
    if (cl == null) {
      cl = get_parent_cl();Thread.currentThread().getContextClassLoader();
    }
    
    if (cl == null) { 
      cl = Class_TCCL.class.getClassLoader();  // this class's classloader
    }
    
    return cl;
  }
  
  static public ClassLoader get_parent_cl() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return (cl == null) 
             ? Class_TCCL.class.getClassLoader()
             : cl;
  }

  // only for Java 8 + , needs lambda
//  static public <T> Class<T> loadclass_throw_if_not_found(
//      String className, ResourceManager rm, Supplier<String> sourceUrl) 
//          throws ResourceInitializationException {
//    try {
//      Class<T> c = forName(className, rm);
//      return c;
//    } catch (ClassNotFoundException e) {
//      throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
//              new Object[] { className, sourceUrl.get() }, e);
//    }
//  }
}
