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

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.resource.ResourceManager;

/**
 * INTERNAL API - Helper functions to obtain a suitable class loader.
 */
public final class ClassLoaderUtils {
  private ClassLoaderUtils() {
    // No instances
  }

  /**
   * Looks up a suitable class loader in the following order:
   * 
   * <ol>
   * <li>The {@link UimaContext} in the {@link UimaContextHolder} of the current thread(if any)</li>
   * <li>The current thread-context class loader (if any)</li>
   * <li>The class loader through which uimaFIT (i.e. this class) was loaded.</li>
   * <li>For backwards compatibility then delegates to {@link #getDefaultClassLoader()}</li>
   * </ol>
   *
   * @return a class loader or {@code null} if no suitable class loader could be found.
   */
  public static ClassLoader findClassLoader() {
    var uimaThreadContextClassLoader = getExtensionClassLoader(UimaContextHolder.getContext());
    if (uimaThreadContextClassLoader != null) {
      return uimaThreadContextClassLoader;
    }

    var contextClassLoader = Thread.currentThread().getContextClassLoader();
    if (contextClassLoader != null) {
      return contextClassLoader;
    }

    var uimaFITClassLoader = ClassLoaderUtils.class.getClassLoader();
    if (uimaFITClassLoader != null) {
      return uimaFITClassLoader;
    }

    return getDefaultClassLoader();
  }

  /**
   * Looks up a suitable class loader in the following order:
   * 
   * <ol>
   * <li>The extension class loader of the given {@link ResourceManager}</li>
   * <li>See {@link #findClassLoader()}</li>
   * </ol>
   *
   * @return a class loader or {@code null} if no suitable class loader could be found.
   */
  public static ClassLoader findClassLoader(ResourceManager aResMgr) {
    var resourceManagerExtensionClassloader = getExtensionClassLoader(aResMgr);
    if (resourceManagerExtensionClassloader != null) {
      return resourceManagerExtensionClassloader;
    }

    return findClassLoader();
  }

  /**
   * Looks up a suitable class loader in the following order:
   * 
   * <ol>
   * <li>The extension class loader of the {@link ResourceManager} associated with the given
   * {@link UimaContext} (if any)</li>
   * <li>See {@link #findClassLoader(ResourceManager)}</li>
   * </ol>
   *
   * @return a class loader or {@code null} if no suitable class loader could be found.
   */
  public static ClassLoader findClassLoader(UimaContext aContext) {
    var uimaContextExtensionClassloader = getExtensionClassLoader(aContext);
    if (uimaContextExtensionClassloader != null) {
      return uimaContextExtensionClassloader;
    }

    return findClassLoader((ResourceManager) null);
  }

  private static ClassLoader getExtensionClassLoader(UimaContext aContext) {
    if (aContext instanceof UimaContextAdmin) {
      return getExtensionClassLoader(((UimaContextAdmin) aContext).getResourceManager());
    }

    return null;
  }

  private static ClassLoader getExtensionClassLoader(ResourceManager aResMgr) {
    if (aResMgr == null) {
      return null;
    }

    var cl = aResMgr.getExtensionClassLoader();
    if (cl != null) {
      return cl;
    }

    return null;
  }

  private static ClassLoader getDefaultClassLoader() {
    ClassLoader cl;
    try {
      cl = Thread.currentThread().getContextClassLoader();
      if (cl != null) {
        return cl;
      }
    } catch (Throwable ex) {
      // Fall-through
    }

    try {
      cl = UIMAFramework.class.getClassLoader();
      if (cl != null) {
        return cl;
      }
    } catch (Throwable ex) {
      // Fall-through
    }

    try {
      cl = ClassLoader.getSystemClassLoader();
      if (cl != null) {
        return cl;
      }
    } catch (Throwable ex) {
      // Fall-through
    }

    return null;
  }
}
