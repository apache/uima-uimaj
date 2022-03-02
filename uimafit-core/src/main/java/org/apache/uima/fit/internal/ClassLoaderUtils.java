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
package org.apache.uima.fit.internal;

import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.resource.ResourceManager;
import org.springframework.util.ClassUtils;

/**
 * INTERNAL API - Helper functions to obtain a suitable classloader.
 */
public final class ClassLoaderUtils {
  private ClassLoaderUtils() {
    // No instances
  }

  /**
   * Looks up a suitable classloader in the following order:
   * <ol>
   * <li>The {@link UimaContext} in the {@link UimaContextHolder} of the current thread(if any)</li>
   * <li>The current thread-context classloader (if any)</li>
   * <li>The classloader through which uimaFIT (i.e. this class) was loaded.</li>
   * <li>For backwards compatibility then delegates to
   * {@link ClassUtils#getDefaultClassLoader()}</li>
   * </ol>
   *
   * @return a classloader or {@code null} if no suitable classloader could be found.
   */
  public static ClassLoader findClassloader() {
    ClassLoader uimaThreadContextClassLoader = getExtensionClassloader(
            UimaContextHolder.getContext());
    if (uimaThreadContextClassLoader != null) {
      return uimaThreadContextClassLoader;
    }

    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    if (contextClassLoader != null) {
      return contextClassLoader;
    }

    ClassLoader uimaFITClassLoader = ClassLoaderUtils.class.getClassLoader();
    if (uimaFITClassLoader != null) {
      return uimaFITClassLoader;
    }

    return ClassUtils.getDefaultClassLoader();
  }

  /**
   * Looks up a suitable classloader in the following order:
   * <ol>
   * <li>The extension classloader of the given {@link ResourceManager}</li>
   * <li>See {@link #findClassloader()}</li>
   * </ol>
   *
   * @return a classloader or {@code null} if no suitable classloader could be found.
   */
  public static ClassLoader findClassloader(ResourceManager aResMgr) {
    ClassLoader resourceManagerExtensionClassloader = getExtensionClassloader(aResMgr);
    if (resourceManagerExtensionClassloader != null) {
      return resourceManagerExtensionClassloader;
    }

    return findClassloader();
  }

  /**
   * Looks up a suitable classloader in the following order:
   * <ol>
   * <li>The extension classloader of the {@link ResourceManager} associated with the given
   * {@link UimaContext} (if any)</li>
   * <li>See {@link #findClassloader(ResourceManager)}</li>
   * </ol>
   *
   * @return a classloader or {@code null} if no suitable classloader could be found.
   */
  public static ClassLoader findClassloader(UimaContext aContext) {
    ClassLoader uimaContextExtensionClassloader = getExtensionClassloader(aContext);
    if (uimaContextExtensionClassloader != null) {
      return uimaContextExtensionClassloader;
    }

    return findClassloader((ResourceManager) null);
  }

  private static ClassLoader getExtensionClassloader(UimaContext aContext) {
    if (aContext instanceof UimaContextAdmin) {
      return getExtensionClassloader(((UimaContextAdmin) aContext).getResourceManager());
    }

    return null;
  }

  private static ClassLoader getExtensionClassloader(ResourceManager aResMgr) {
    if (aResMgr == null) {
      return null;
    }

    ClassLoader cl = aResMgr.getExtensionClassLoader();
    if (cl != null) {
      return aResMgr.getExtensionClassLoader();
    }

    return null;
  }
}
