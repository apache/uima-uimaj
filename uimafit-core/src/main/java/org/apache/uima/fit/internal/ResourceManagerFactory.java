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

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.impl.UimaVersion;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * INTERNAL API - Helper functions for dealing with resource managers and classloading
 *
 * This API is experimental and is very likely to be removed or changed in future versions.
 */
public class ResourceManagerFactory {
  private static final Logger LOG = LoggerFactory.getLogger(ResourceManagerFactory.class);
  
  private static ResourceManagerCreator resourceManagerCreator = new DefaultResourceManagerCreator();

  private ResourceManagerFactory() {
    // No instances
  }

  public static ResourceManager newResourceManager() throws ResourceInitializationException {
    return resourceManagerCreator.newResourceManager();
  }

  /**
   * Mind that returning a singleton resource manager from {@link ResourceManagerFactory} is
   * generally a bad idea because it gets destroyed on a regular basis. For this reason, it is
   * called {@link ResourceManagerFactory#newResourceManager()} and not
   * {@code getResourceManager()}.
   */
  public static synchronized void setResourceManagerCreator(
          ResourceManagerCreator resourceManagerCreator) {
    ResourceManagerFactory.resourceManagerCreator = resourceManagerCreator;
  }

  public static ResourceManagerCreator getResourceManagerCreator() {
    return resourceManagerCreator;
  }

  public interface ResourceManagerCreator {
    ResourceManager newResourceManager() throws ResourceInitializationException;
  }

  public static class DefaultResourceManagerCreator implements ResourceManagerCreator {
    @Override
    public ResourceManager newResourceManager() throws ResourceInitializationException {
      UimaContext activeContext = UimaContextHolder.getContext();
      if (activeContext != null) {
        // If we are already in a UIMA context, then we re-use it. Mind that the JCas cannot
        // handle switching across more than one classloader.
        // This can be done since UIMA 2.9.0 and starts being handled in uimaFIT 2.3.0
        // See https://issues.apache.org/jira/browse/UIMA-5056
        LOG.trace("Using resource manager from active UIMA context");
        return ((UimaContextAdmin) activeContext).getResourceManager();
      }

      // If there is no UIMA context, then we create a new resource manager
      // UIMA core still does not fall back to the context classloader in all cases.
      // This was the default behavior until uimaFIT 2.2.0.
      ResourceManager resMgr;
      if (Thread.currentThread().getContextClassLoader() != null) {
        // If the context classloader is set, then we want the resource manager to fallb
        // back to it. However, it may not reliably do that that unless we explictly pass
        // null here. See. UIMA-6239.
        LOG.trace("Detected thread context classloader: preparing resource manager to use it");
        resMgr = new ResourceManager_impl(null);
      } else {
        resMgr = UIMAFramework.newDefaultResourceManager();
      }

      // Since UIMA Core version 2.10.3 and 3.0.1 the thread context classloader is taken
      // into account by the core framework. Thus, we no longer have to explicitly set a
      // classloader these or more recent versions. (cf. UIMA-5802)
      short maj = UimaVersion.getMajorVersion();
      short min = UimaVersion.getMinorVersion();
      short rev = UimaVersion.getBuildRevision();
      boolean uimaCoreIgnoresContextClassloader = 
              (maj == 2 && (min < 10 || (min == 10 && rev < 3))) || // version < 2.10.3
              (maj == 3 && ((min == 0 && rev < 1)));                // version < 3.0.1
      if (uimaCoreIgnoresContextClassloader) {
        LOG.trace("Detected UIMA version " + maj + "." + min + "." + rev + 
                " which ignores the thread context classloader, setting it explicitly");
        resMgr.setExtensionClassLoader(ClassLoaderUtils.findClassloader(), true);
      }

      return resMgr;
    }
  }
}
