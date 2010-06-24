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

import org.apache.uima.Constants;
import org.apache.uima.ResourceFactory;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * Specialized Resource Factory for producing CollectionReaders.
 * 
 */
public class CollectionReaderFactory_impl implements ResourceFactory {

  /**
   * @see org.apache.uima.ResourceFactory#produceResource(java.lang.Class,
   *      org.apache.uima.resource.ResourceSpecifier, java.util.Map)
   */
  public Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    if (aSpecifier instanceof CollectionReaderDescription) {
      CollectionReaderDescription desc = (CollectionReaderDescription) aSpecifier;
      
      //check framework implementation (CollectionReaders only supported in Java)
      String frameworkImpl = desc.getFrameworkImplementation();
      if (frameworkImpl == null || frameworkImpl.length() == 0) {
        throw new ResourceInitializationException(
                ResourceInitializationException.MISSING_FRAMEWORK_IMPLEMENTATION,
                new Object[] { aSpecifier.getSourceUrlString() });
      }
      if (!frameworkImpl.startsWith(Constants.JAVA_FRAMEWORK_NAME)) {
        throw new ResourceInitializationException(
                ResourceInitializationException.UNSUPPORTED_FRAMEWORK_IMPLEMENTATION,
                new Object[] { desc.getFrameworkImplementation(), aSpecifier.getSourceUrlString() });        
      }
      
      String className = desc.getImplementationName();
      
      if (className == null || className.length() == 0) {
        throw new ResourceInitializationException(
                ResourceInitializationException.MISSING_IMPLEMENTATION_CLASS_NAME,
                new Object[] {aSpecifier.getSourceUrlString()});
      }

      // load class using UIMA Extension ClassLoader if there is one
      ClassLoader cl = null;
      ResourceManager resourceManager = null;
      if (aAdditionalParams != null) {
        resourceManager = (ResourceManager) aAdditionalParams.get(Resource.PARAM_RESOURCE_MANAGER);
      }
      if (resourceManager != null) {
        cl = resourceManager.getExtensionClassLoader();
      }
      if (cl == null) {
        cl = this.getClass().getClassLoader();
      }

      try {
        Class<?> implClass = Class.forName(className, true, cl);

        // check to see if this is a subclass of BaseCollectionReader and of aResourceClass
        if (!BaseCollectionReader.class.isAssignableFrom(implClass)) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.NOT_A_COLLECTION_READER, new Object[] {
                      className, aSpecifier.getSourceUrlString() });
        }
        if (!aResourceClass.isAssignableFrom(implClass)) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.RESOURCE_DOES_NOT_IMPLEMENT_INTERFACE,
                  new Object[] { className, aResourceClass.getName(),
                      aSpecifier.getSourceUrlString() });
        }

        // instantiate this Resource Class
        Resource resource = (Resource) implClass.newInstance();
        // attempt to initialize it
        if (resource.initialize(aSpecifier, aAdditionalParams)) {
          // success!
          // Next: see Jira UIMA-554
          (((CollectionReader_ImplBase)resource).getCasManager()).addMetaData(
              (ProcessingResourceMetaData) resource.getMetaData().clone());
          return resource;
        } else // failure, for some unknown reason :( This isn't likely to happen
        {
          throw new ResourceInitializationException(
                  ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR, new Object[] {
                      className, aSpecifier.getSourceUrlString() });
        }
      }
      // if an exception occurs, log it but do not throw it... yet
      catch (ClassNotFoundException e) {
        throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
                new Object[] { className, aSpecifier.getSourceUrlString() }, e);
      } catch (IllegalAccessException e) {
        throw new ResourceInitializationException(
                ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { className,
                    aSpecifier.getSourceUrlString() }, e);
      } catch (InstantiationException e) {
        throw new ResourceInitializationException(
                ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] { className,
                    aSpecifier.getSourceUrlString() }, e);
      }
    } else // not a CR descriptor
    {
      return null;
    }
  }

}
