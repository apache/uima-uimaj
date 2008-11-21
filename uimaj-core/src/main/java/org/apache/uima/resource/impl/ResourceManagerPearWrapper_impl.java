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

package org.apache.uima.resource.impl;

import java.net.MalformedURLException;

import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceManagerPearWrapper;

/**
 * Reference implementation of {@link org.apache.uima.resource.ResourceManager},
 * used for PearAnalysisEngineWrapper.
 * 
 * It is a subclass of ResourceManager_impl, with a different classpath/datapath
 * but everything else referring to the same objects as the parent.
 * 
 * 
 */
public class ResourceManagerPearWrapper_impl extends ResourceManager_impl implements ResourceManagerPearWrapper {

  /**
   * UIMA extension ClassLoader. ClassLoader is created if an extension classpath is specified at
   * the ResourceManager
   */
  private UIMAClassLoader uimaCL = null;

  /**
   * Object used for resolving relative paths. This is built by parsing the data path.
   */
  private RelativePathResolver mRelativePathResolver;

  /**
   * Initializes from the parent, a new <code>ResourceManagerForPearWrapper_impl</code>.
   */
  public void initializeFromParentResourceManager(ResourceManager resourceManager) {
    ResourceManager_impl r = (ResourceManager_impl) resourceManager;
    mRelativePathResolver = new RelativePathResolver_impl();
    mResourceMap = r.mResourceMap;
    mInternalResourceRegistrationMap = r.mInternalResourceRegistrationMap;
    mParameterizedResourceImplClassMap = r.mParameterizedResourceImplClassMap;
    mInternalParameterizedResourceImplClassMap = r.mInternalParameterizedResourceImplClassMap;
    mParameterizedResourceInstanceMap = r.mParameterizedResourceInstanceMap;
    mCasManager = r.mCasManager;
  }
 
  /**
   * @see org.apache.uima.resource.ResourceManager#setExtensionClassPath(java.lang.String, boolean)
   */
  public void setExtensionClassPath(String classpath, boolean resolveResource)
          throws MalformedURLException {
    // create UIMA extension ClassLoader with the given classpath
    uimaCL = new UIMAClassLoader(classpath, this.getClass().getClassLoader());

    if (resolveResource) {
      // set UIMA extension ClassLoader also to resolve resources
      getRelativePathResolver().setPathResolverClassLoader(uimaCL);
    }
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#setExtensionClassPath(ClassLoader,java.lang.String,
   *      boolean)
   */
  public void setExtensionClassPath(ClassLoader parent, String classpath, boolean resolveResource)
          throws MalformedURLException {
    // create UIMA extension ClassLoader with the given classpath
    uimaCL = new UIMAClassLoader(classpath, parent);

    if (resolveResource) {
      // set UIMA extension ClassLoader also to resolve resources
      getRelativePathResolver().setPathResolverClassLoader(uimaCL);
    }
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#getExtensionClassLoader()
   */
  public ClassLoader getExtensionClassLoader() {
    return uimaCL;
  }

  @Override
  protected RelativePathResolver getRelativePathResolver() {
    return mRelativePathResolver;
  }

}
