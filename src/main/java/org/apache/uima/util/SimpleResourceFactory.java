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

package org.apache.uima.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.uima.ResourceFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/**
 * A simple implementation of a {@link org.apache.uima.ResourceFactory}. This implementation
 * maintains a Map between the {@link ResourceSpecifier} sub-interface name (e.g.
 * <code>AnalysisEngineDescription</code>) and the class name of the resource to be constructed
 * from specifiers of that type.
 * <p>
 * UIMA developers who introduce new types of {@link Resource}s or {@link ResourceSpecifier}s may
 * create an instance of this class and use the {@link #addMapping(Class,Class)} method to register
 * a mapping between the ResourceSpecifier interface and the Class of the Resource that is to be
 * constructed from it. The <code>SimpleResourceFactory</code> should then be registered with the
 * framework by calling
 * <code>{@link UIMAFramework#getResourceFactory()}.{@link org.apache.uima.CompositeResourceFactory#registerFactory(Class,ResourceFactory) registerFactory(Class,ResourceFactory)};</code>
 * 
 * 
 */
public class SimpleResourceFactory implements ResourceFactory {
  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class<SimpleResourceFactory> CLASS_NAME = SimpleResourceFactory.class;

  /**
   * Map from ResourceSpecifier Class to List of Resource Classes. Resource initialization is
   * attempted in reverse order through this List, so more recently registered classes are tried
   * first.
   */
  protected Map<Class<? extends ResourceSpecifier>, List<Class<? extends Resource>>> mClassMap =
	      Collections.synchronizedMap(new HashMap<Class<? extends ResourceSpecifier>, List<Class<? extends Resource>>>());

  /**
   * Produces an appropriate <code>Resource</code> instance from a <code>ResourceSpecifier</code>.
   * 
   * @param aResourceClass
   *          the interface of the resource to be produced. This is intended to be a standard UIMA
   *          interface such as <code>TextAnalysisEngine</code> or <code>ASB</code>.
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters.
   * 
   * @return a <code>Resource</code> instance. Returns <code>null</code> if this factory does
   *         not know how to create a Resource from the <code>ResourceSpecifier</code> provided.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource
   * 
   * @see org.apache.uima.ResourceFactory#produceResource(Class, ResourceSpecifier,Map)
   */
  public Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    ResourceInitializationException lastException = null;

    // get all interfaces implemented by aSpecifier
    Class<?>[] interfaces = aSpecifier.getClass().getInterfaces();

    // look up class mapping
    List<Class<? extends Resource>> resourceClasses = null;
    for (int i = 0; i < interfaces.length; i++) {
      resourceClasses = mClassMap.get(interfaces[i]);
      if (resourceClasses != null)
        break;
    }

    if (resourceClasses != null) {
      // iterate backwards through the elements of the list, so that
      // we attempt to initialize the most recently registered Resource
      // classes first
      ListIterator<Class<? extends Resource>> i = resourceClasses.listIterator(resourceClasses.size());
      while (i.hasPrevious()) {
        Class<? extends Resource> currentClass = i.previous();
        ResourceInitializationException currentException = null;
        try {
          // check to see if this is a subclass of aResourceClass
          if (aResourceClass.isAssignableFrom(currentClass)) {
            // instantiate this Resource Class
            Resource resource = currentClass.newInstance();
            // attempt to initialize it
            UIMAFramework.getLogger(CLASS_NAME).logrb(Level.CONFIG, CLASS_NAME.getName(),
                    "produceResource", LOG_RESOURCE_BUNDLE, "UIMA_trying_resource_class__CONFIG",
                    currentClass.getName());

            if (resource.initialize(aSpecifier, aAdditionalParams)) {
              // success!
              return resource;
            }
          }
        }
        // if an exception occurs, log it but do not throw it... yet
        catch (IllegalAccessException e) {
          currentException = new ResourceInitializationException(
                  ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] {
                      currentClass.getName(), aSpecifier.getSourceUrlString() }, e);
        } catch (InstantiationException e) {
          currentException = new ResourceInitializationException(
                  ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] {
                      currentClass.getName(), aSpecifier.getSourceUrlString() }, e);
        } catch (Throwable t) {
          currentException = new ResourceInitializationException(
                  ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR, new Object[] {
                      currentClass.getName(), aSpecifier.getSourceUrlString() }, t);
        } finally {
          if (currentException != null) {
            currentException.fillInStackTrace();
            // UIMAFramework.getLogger().logException(currentException);
            // store this exception
            lastException = currentException;
          }
        }
        // try again
      }
    }

    // No resource could be created. If an exception occurred,
    // throw it. Otherwise, return null.
    if (lastException != null) {
      throw lastException;
    } else {
      return null;
    }
  }

  /**
   * Configures this <code>SimpleResourceFactory</code> by adding a new mapping between a
   * <code>ResourceSpecifier</code> class and a <code>Resource</code> class.
   * 
   * @param aSpecifierInterface
   *          the subinterface of <code>ResourceSpecifier</code>.
   * @param aResourceClass
   *          a subclass of <code>Resource</code> that is to be instantiated from resource
   *          specifiers of the given class.
   */
  public void addMapping(Class<? extends ResourceSpecifier> aSpecifierInterface, Class<? extends Resource> aResourceClass) {
    List<Class<? extends Resource>> mappingList = mClassMap.get(aSpecifierInterface);
    if (mappingList == null) {
      // No mapping exists. Create a new list and put it in the map.
      mappingList = new ArrayList<Class<? extends Resource>>();
      mClassMap.put(aSpecifierInterface, mappingList);
    }

    // add the new Resource Class to the end of the mapping list
    mappingList.add(aResourceClass);
  }

  /**
   * Configures this <code>SimpleResourceFactory</code> by adding a new mapping between a
   * <code>ResourceSpecifier</code> class and a <code>Resource</code> class.
   * 
   * @param aSpecifierInterfaceName
   *          name of the subinterface of <code>ResourceSpecifier</code>.
   * @param aResourceClassName
   *          the name of a subclass of <code>Resource</code> that is to be instantiated from
   *          resource specifiers of the given class.
   * @throws ClassNotFoundException -
   */
  @SuppressWarnings("unchecked")
  public void addMapping(String aSpecifierInterfaceName, String aResourceClassName)
          throws ClassNotFoundException {
    addMapping((Class<? extends ResourceSpecifier>) Class.forName(aSpecifierInterfaceName),
    		(Class<? extends Resource>) Class.forName(aResourceClassName));
  }
}
