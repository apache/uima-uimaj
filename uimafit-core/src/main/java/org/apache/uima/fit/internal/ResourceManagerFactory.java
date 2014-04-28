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

import java.net.MalformedURLException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.springframework.util.ClassUtils;

/**
 * INTERNAL API - Helper functions for dealing with resource managers and classloading
 * 
 * This API is experimental and is very likely to be removed or changed in future versions. 
 */
public class ResourceManagerFactory {
  private static ResourceManagerCreator resourceManagerCreator = new DefaultResourceManagerCreator();
  
  private ResourceManagerFactory() {
    // No instances
  }

  public static ResourceManager newResourceManager() throws ResourceInitializationException
  {
    return resourceManagerCreator.newResourceManager();
  }
  
  public static synchronized void setResourceManagerCreator(
          ResourceManagerCreator resourceManagerCreator) {
    ResourceManagerFactory.resourceManagerCreator = resourceManagerCreator;
  }
  
  public static ResourceManagerCreator getResourceManagerCreator() {
    return resourceManagerCreator;
  }
  
  public static interface ResourceManagerCreator {
    ResourceManager newResourceManager() throws ResourceInitializationException;
  }
  
  public static class DefaultResourceManagerCreator implements ResourceManagerCreator {
    public ResourceManager newResourceManager() throws ResourceInitializationException {
      try {
        ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
        resMgr.setExtensionClassPath(ClassUtils.getDefaultClassLoader(), "", true);
        return resMgr;
      }
      catch (MalformedURLException e) {
        throw new ResourceInitializationException(e);
      }
    }
  }
}
