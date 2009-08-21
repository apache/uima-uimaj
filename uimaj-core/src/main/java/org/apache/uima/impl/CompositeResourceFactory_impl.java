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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.uima.CompositeResourceFactory;
import org.apache.uima.ResourceFactory;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/**
 * A registry containing (ResourceSpecifier Class, {@link ResourceFactory}) pairs. Also implements
 * the <code>ResourceFactory</code> interface, and produces resources by delegating to the most
 * recently registered <code>ResourceFactory</code> instance that can handle the class of the
 * given specifier object.
 * 
 * 
 */
public class CompositeResourceFactory_impl implements CompositeResourceFactory {

  /**
   * List of Registration objects.
   */
  private List<Registration> mRegisteredFactories = Collections.synchronizedList(new ArrayList<Registration>());

  /**
   * @see org.apache.uima.ResourceFactory#produceResource(java.lang.Class,
   *      org.apache.uima.resource.ResourceSpecifier, java.util.Map)
   */
  public Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    // check for factories registered for this resource specifier type
    // (most recently registered first)
    ListIterator<Registration> it = mRegisteredFactories.listIterator(mRegisteredFactories.size());
    Resource result = null;
    while (it.hasPrevious()) {
      Registration reg = it.previous();
      if (reg.resourceSpecifierInterface.isAssignableFrom(aSpecifier.getClass())) {
        result = reg.factory.produceResource(aResourceClass, aSpecifier, aAdditionalParams);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  /**
   * @see org.apache.uima.CompositeResourceFactory#registerFactory(Class,ResourceFactory)
   */
  public void registerFactory(Class aResourceSpecifierInterface, ResourceFactory aFactory) {
    mRegisteredFactories.add(new Registration(aResourceSpecifierInterface, aFactory));
  }

  /**
   * Inner class that holds information on a registered factory.
   */
  static class Registration {
    Class resourceSpecifierInterface;

    ResourceFactory factory;

    /**
     * @param aResourceSpecifierInterface
     * @param aFactory
     */
    public Registration(Class aResourceSpecifierInterface, ResourceFactory aFactory) {
      resourceSpecifierInterface = aResourceSpecifierInterface;
      factory = aFactory;
    }
  }

}
