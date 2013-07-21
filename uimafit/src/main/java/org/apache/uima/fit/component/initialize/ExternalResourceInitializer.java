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
package org.apache.uima.fit.component.initialize;

import static org.apache.uima.fit.factory.ExternalResourceFactory.PREFIX_SEPARATOR;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.fit.component.ExternalResourceAware;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.ExternalResourceLocator;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.fit.internal.ResourceList;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.springframework.beans.SimpleTypeConverter;

/**
 * Configurator class for {@link ExternalResource} annotations.
 * 
 */
public final class ExternalResourceInitializer {

  private static final Object INITIALIZED = new Object();

  private static Map<Object, Object> initializedResources = new WeakHashMap<Object, Object>();

  private ExternalResourceInitializer() {
    // No instances
  }

  /**
   * Configure a component from the given context.
   * @param object
   *          the component.
   * @param context
   *          the UIMA context.
   * 
   * @param <T>
   *          the component type.
   * @throws ResourceInitializationException
   *           if the external resource cannot be configured.
   */
  public static <T> void initialize(T object, UimaContext context)
          throws ResourceInitializationException {
    configure(context, object.getClass(), object.getClass(), object);
  }

  /**
   * Helper method for recursively configuring super-classes.
   * 
   * @param <T>
   *          the component type.
   * @param context
   *          the context containing the resource bindings.
   * @param baseCls
   *          the class on which configuration started.
   * @param cls
   *          the class currently being configured.
   * @param object
   *          the object being configured.
   * @throws ResourceInitializationException
   *           if required resources could not be bound.
   */
  private static <T> void configure(UimaContext context, Class<?> baseCls, Class<?> cls, T object)
          throws ResourceInitializationException {
    if (cls.getSuperclass() != null) {
      configure(context, baseCls, cls.getSuperclass(), object);
    } else {
      // Try to initialize the external resources only once, not for each step of the
      // class hierarchy of a component.
      initializeNestedResources(context);
    }

    for (Field field : cls.getDeclaredFields()) {
      if (!ReflectionUtil.isAnnotationPresent(field, ExternalResource.class)) {
        continue;
      }
      
      ExternalResource era = ReflectionUtil.getAnnotation(field, ExternalResource.class);

      // Get the resource key. If it is a nested resource, also get the prefix.
      String key = era.key();
      if (key.length() == 0) {
        key = field.getName();
      }
      if (object instanceof ExternalResourceAware) {
        String prefix = ((ExternalResourceAware) object).getResourceName();
        if (prefix != null) {
          key = prefix + PREFIX_SEPARATOR + key;
        }
      }

      // Obtain the resource
      Object value = getResourceObject(context, key);
      if (value instanceof ExternalResourceLocator) {
        value = ((ExternalResourceLocator) value).getResource();
      }

      // Sanity checks
      if (value == null && era.mandatory()) {
        throw new ResourceInitializationException(new IllegalStateException("Mandatory resource ["
                + key + "] is not set on [" + baseCls + "]"));
      }

      // Now record the setting and optionally apply it to the given
      // instance.
      if (value != null) {
        field.setAccessible(true);
        
        try {
          if (value instanceof ResourceList) {
            // Value is a multi-valued resource
            ResourceList resList = (ResourceList) value;
            
            // We cannot do this in ResourceList because the resource doesn't have access to
            // the UIMA context we use here. Resources are initialize with their own contexts
            // by the UIMA framework!
            List<Object> elements = new ArrayList<Object>();
            for (int i = 0; i < resList.getSize(); i++) {
              Object elementValue = getResourceObject(context, resList.getResourceName()
                      + PREFIX_SEPARATOR + ResourceList.ELEMENT_KEY + "[" + i + "]");
              elements.add(elementValue);
            }

            SimpleTypeConverter converter = new SimpleTypeConverter();
            value = converter.convertIfNecessary(elements, field.getType());
          }
          
          try {
            field.set(object, value);
          } catch (IllegalAccessException e) {
            throw new ResourceInitializationException(e);
          }
        }
        finally {          
          field.setAccessible(false);
        }
      }
    }
  }
  
  private static Object getResourceObject(UimaContext aContext, String aKey)
          throws ResourceInitializationException {
    Object value;
    try {
      value = aContext.getResourceObject(aKey);
    } catch (ResourceAccessException e) {
      throw new ResourceInitializationException(e);
    }
    return value;
  }

  /**
   * Scan the context and initialize external resources injected into other external resources.
   * 
   * @param aContext
   *          the UIMA context.
   */
  private static void initializeNestedResources(UimaContext aContext)
          throws ResourceInitializationException {
    List<ExternalResourceAware> awareResources = new ArrayList<ExternalResourceAware>();

    // Initialize the resources - each resource must only be initialized once. We remember
    // if a resource has already been initialized in a weak hash map, so we automatically
    // forget about resources that are garbage collected.
    for (Object r : getResources(aContext)) {
      synchronized (initializedResources) {
        if (r instanceof ExternalResourceAware && !initializedResources.containsKey(r)) {
          // Already mark the resource as initialized so we do not run into an
          // endless recursive loop when initialize() is called again.
          initializedResources.put(r, INITIALIZED);
          initialize(r, aContext);
          awareResources.add((ExternalResourceAware) r);
        }
      }
    }

    // Notify the resources after everything has been configured
    for (ExternalResourceAware res : awareResources) {
      res.afterResourcesInitialized();
    }
  }

  /**
   * Get all resources declared in the context.
   */
  private static Collection<?> getResources(UimaContext aContext)
          throws ResourceInitializationException {
    if (!(aContext instanceof UimaContextAdmin)) {
      return Collections.emptyList();
    }

    ResourceManager resMgr = ((UimaContextAdmin) aContext).getResourceManager();
    if (!(resMgr instanceof ResourceManager_impl)) {
      // Unfortunately there is not official way to access the list of resources. Thus we
      // have to rely on the UIMA implementation details and access the internal resource
      // map via reflection. If the resource manager is not derived from the default
      // UIMA resource manager, then we cannot really do anything here.
      throw new IllegalStateException("Unsupported resource manager implementation ["
              + resMgr.getClass() + "]");
    }

    Field resourceMapField = null;
    try {
      // Fetch the list of resources
      resourceMapField = ReflectionUtil.getField(resMgr, "mResourceMap");
      resourceMapField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, Object> resources = (Map<String, Object>) resourceMapField.get(resMgr);

      return resources.values();
    } catch (SecurityException e) {
      throw new ResourceInitializationException(e);
    } catch (NoSuchFieldException e) {
      throw new ResourceInitializationException(e);
    } catch (IllegalArgumentException e) {
      throw new ResourceInitializationException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceInitializationException(e);
    } finally {
      if (resourceMapField != null) {
        resourceMapField.setAccessible(false);
      }
    }
  }
}
