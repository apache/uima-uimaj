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

package org.apache.uima.impl;

import java.util.Map;

import org.apache.uima.ResourceFactory;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;

/**
 * Resource Factory that handles {@link CustomResourceSpecifier} elements.
 */
public class CustomResourceFactory_impl implements ResourceFactory {
  /**
   * @see org.apache.uima.ResourceFactory#produceResource(java.lang.Class,
   *      org.apache.uima.resource.ResourceSpecifier, java.util.Map)
   */
  public Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    
    if (aSpecifier instanceof CustomResourceSpecifier) {
      String className = ((CustomResourceSpecifier)aSpecifier).getResourceClassName();
      //check additional params map for ResourceManager, and use the UIMA extension ClassLoader
      //if one exists
      ClassLoader loader = null;
      ResourceManager resMgr = null;
      if (aAdditionalParams != null) {
        resMgr = (ResourceManager)aAdditionalParams.get(Resource.PARAM_RESOURCE_MANAGER);
      }
      if (resMgr != null) {
        loader = resMgr.getExtensionClassLoader();
      }
      if (loader == null) {
        loader = this.getClass().getClassLoader();
      }
      
      //load the Resourceclass
      Class<?> resourceClass;
      try {
        resourceClass = Class.forName(className, true, loader);
      } catch (ClassNotFoundException e) {
        throw new ResourceInitializationException(
                ResourceInitializationException.CLASS_NOT_FOUND, new Object[] { className,
                    aSpecifier.getSourceUrlString() }, e);
      }
      
      //check that the class implements the required interface
      if (!aResourceClass.isAssignableFrom(resourceClass)) {
        throw new ResourceInitializationException(
                ResourceInitializationException.RESOURCE_DOES_NOT_IMPLEMENT_INTERFACE,
                new Object[] { className, aResourceClass.getName(),
                    aSpecifier.getSourceUrlString() });
      }
      
      // instantiate this Resource Class
      Resource resource;
      try {
        resource = (Resource) resourceClass.newInstance();
      } catch (InstantiationException e) {
        throw new ResourceInitializationException(
                ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { className,
                    aSpecifier.getSourceUrlString() }, e);
      } catch (IllegalAccessException e) {
        throw new ResourceInitializationException(
                ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { className,
                    aSpecifier.getSourceUrlString() }, e);
      }
      // attempt to initialize it
      boolean initializeOK = false;
      try {
        initializeOK = resource.initialize(aSpecifier, aAdditionalParams);
      } catch (Exception e) {
        throw new ResourceInitializationException(
            ResourceInitializationException.EXCEPTION_WHEN_INITIALIZING_CUSTOM_RESOURCE, 
            new Object[] { className, aSpecifier.getSourceUrlString() },
            e);
      } catch (Throwable e) {
        throw new ResourceInitializationException(
            ResourceInitializationException.THROWABLE_WHEN_INITIALIZING_CUSTOM_RESOURCE, 
            new Object[] { className, aSpecifier.getSourceUrlString() },
            e);
      }
      if (initializeOK) {
        // success!
        return resource;
      } else
      // failure, for some unknown reason :( 
      {
        throw new ResourceInitializationException(
                ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR, new Object[] {
                    className, aSpecifier.getSourceUrlString() });
      }     
    }  
    //unsupported ResourceSpecifier type
    return null;
  }
}
