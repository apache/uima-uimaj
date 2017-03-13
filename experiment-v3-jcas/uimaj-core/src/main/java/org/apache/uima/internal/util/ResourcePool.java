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

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.Resource_ImplBase;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Level;

/**
 * This class represents a simple pool of {@link Resource} instances.
 * <p>
 * Clients check-out Resources from the pool using the {@link #getResource()} method and check-in
 * Resources using the {@link #releaseResource(Resource)} method.
 * <p>
 * This resource pool implementation does not deal with differently configured resources in the same
 * pool. All resources are assumed to be equivalent and to share the same metadata. Therefore, the
 * resource metadata can be retrieved via the {@link #getMetaData()} method without checking out a
 * Resource instance from the pool.
 * 
 * 
 * 
 */
public class ResourcePool {

  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class<ResourcePool> CLASS_NAME = ResourcePool.class;

  /**
   * Creates new ResourcePool_impl
   * 
   * @param aNumInstances
   *          the number of Resource instances in the pool
   * @param aResourceSpecifier
   *          specifier that describes how to create the Resource instances for the pool
   * @param aResourceClass
   *          class of resource to instantiate
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  public ResourcePool(int aNumInstances, ResourceSpecifier aResourceSpecifier, Class<? extends Resource> aResourceClass)
          throws ResourceInitializationException {
    this(aNumInstances, aResourceSpecifier, aResourceClass, null);
  }

  /**
   * Creates new ResourcePool_impl
   * 
   * @param aNumInstances
   *          the number of Resource instances in the pool
   * @param aResourceSpecifier
   *          specifier that describes how to create the Resource instances for the pool
   * @param aResourceClass
   *          class of resource to instantiate
   * @param aResourceInitParams
   *          additional parameters to be passed to
   *          {@link Resource#initialize(ResourceSpecifier,Map)} methods. May be null if there are
   *          no parameters.
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  public ResourcePool(int aNumInstances, ResourceSpecifier aResourceSpecifier,
          Class<? extends Resource> aResourceClass, Map<String, Object> aResourceInitParams) throws ResourceInitializationException {
    mNumInstances = aNumInstances;

    fillPool(aResourceSpecifier, aResourceClass, aResourceInitParams);

    // store metadata so it can be accessed without a check-out
    mMetaData = mAllInstances.get(0).getMetaData();
  }

  /**
   * Checks out a Resource from the pool.
   * 
   * @return a Resource for use by the client. Returns <code>null</code> if none are available (in
   *         which case the client may wait on this object in order to be notified when an instance
   *         becomes available).
   */
  public synchronized Resource getResource() {
    if (!mFreeInstances.isEmpty()) {
      Resource r = mFreeInstances.remove(0);
      /*
       * UIMAFramework.getLogger().log( "Acquired resource " + r.getMetaData().getUUID() + " from
       * pool.");
       */
      return r;
    } else {
      // no instances available
      // UIMAFramework.getLogger().log("No Resource instances currently available");
      return null;
    }
  }

  /**
   * Checks in a Resource to the pool. Also notifies other Threads that may be waiting for a
   * connection.
   * 
   * @param aResource
   *          the resource to release
   */
  public synchronized void releaseResource(Resource aResource) {
    // make sure this Resource was actually belongs to this pool and is checked out
    if (!mAllInstances.contains(aResource) || mFreeInstances.contains(aResource)) {
      UIMAFramework.getLogger(CLASS_NAME).logrb(Level.WARNING, CLASS_NAME.getName(),
              "releaseResource", LOG_RESOURCE_BUNDLE, "UIMA_return_resource_to_pool__WARNING");
    } else {
      /*
       * UIMAFramework.getLogger().log( "Returned resource " + aResource.getMetaData().getUUID() + "
       * to the pool.");
       */
      // Add the Resource to the end of the free instances List
      mFreeInstances.add(aResource);
    }

    // Notify any threads waiting on this object
    notifyAll();
  }

  /**
   * Checks out a Resource from the pool. If none is currently available, wait for the specified
   * amount of time for one to be checked in.
   * 
   * @param aTimeout
   *          the time to wait in milliseconds. A value of &lt;=0 will wait forever.
   * 
   * @return a Resource for use by the client. Returns <code>null</code> if none are available (in
   *         which case the client may wait on this object in order to be notified when an instance
   *         becomes available).
   */
  public synchronized Resource getResource(long aTimeout) {
    long startTime = new Date().getTime();
    Resource resource;
    while ((resource = getResource()) == null) {
      try {
        wait(aTimeout);
      } catch (InterruptedException e) {
      }
      if (aTimeout > 0 && (new Date().getTime() - startTime) >= aTimeout) {
        // Timeout has expired
        return null;
      }
    }
    return resource;
  }

  /*
   * Checks out a specific resource from the pool, waiting as long as needed until it is free
   * @param r
   */

  public synchronized void checkoutSpecificResource(Resource r) {
    while (!mFreeInstances.contains(r)) {
      try {
        wait();
      } catch (InterruptedException e) {
      }
    }
    mFreeInstances.remove(r);
  }
  
  /**
   * Destroys all Resources in this pool.
   */
  public synchronized void destroy() {
    Iterator<Resource> i = mAllInstances.iterator();
    while (i.hasNext()) {
      Resource current = i.next();
      current.destroy();
    }
    mAllInstances.clear();
    mFreeInstances.clear();
  }

  /**
   * Gets the size of this pool (the total number of instances that it can hold).
   * 
   * @return the size of this pool
   */
  public int getSize() {
    return mNumInstances;
  }

  /**
   * Gets the metadata for the Resources in this pool. This pool implementation currently assumes
   * that all Resource instances in the pool are equivalent and share the same metadata.
   * 
   * @return the size of this pool
   */
  public ResourceMetaData getMetaData() {
    return mMetaData;
  }

  /**
   * Utility method used in the constructor to fill the pool with Resource instances.
   * 
   * @param aResourceSpecifier
   *          specifier that describes how to create the Resource instances for the pool
   * @param aResourceClass
   *          class of resource to instantiate
   * @param aResourceInitParams
   *          initialization parameters to be passed to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method.
   * 
   * 
   * @throws ResourceInitializationException
   *           if the Resource instances could not be created
   */
  protected void fillPool(ResourceSpecifier aResourceSpecifier, Class<? extends Resource> aResourceClass,
          Map<String, Object> aResourceInitParams) throws ResourceInitializationException {
    // fill the pool
    for (int i = 0; i < mNumInstances; i++) {
      Resource_ImplBase resource = (Resource_ImplBase) UIMAFramework.produceResource(
              aResourceClass, aResourceSpecifier, aResourceInitParams);

      mAllInstances.add(resource);
      mFreeInstances.add(resource);
    }
  }

  protected Vector<Resource> getAllInstances() {
    return mAllInstances;
  }

  protected Vector<Resource> getFreeInstances() {
    return mFreeInstances;
  }

  private final Vector<Resource> mAllInstances = new Vector<Resource>();

  private final Vector<Resource> mFreeInstances = new Vector<Resource>();

  private final int mNumInstances;

  private final ResourceMetaData mMetaData;
}
