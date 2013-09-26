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

package org.apache.uima.resource;

import java.util.Map;

import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Logger;

/**
 * <code>Resource</code> is the general term for all UIMA components that can be acquired and used
 * by an application (or by other resources).
 * <p>
 * <code>Resource</code>s may be co-located with their client or distributed as services. This is
 * made transparent to the client.
 * <p>
 * A {@link ResourceSpecifier} contains information that can be used acquire a reference to a
 * <code>Resource</code>, whether that is done by instantiating the resource locally or locating
 * an existing resource available as a service.
 * <p>
 * The {@link org.apache.uima.ResourceFactory} takes a <code>ResourceSpecifier</code> and returns
 * an instance of the specified <code>Resource</code>. Again, this can be done by creating the
 * instance or by locating an existing instance.
 * <p>
 * Most applications will not need to deal with this abstract <code>Resource</code> interface.
 * UIMA Developers who need to introduce new types of Resources, however, will need to implement
 * this interface.
 * 
 * 
 */
public interface Resource {

  /**
   * Initializes this <code>Resource</code> from a <code>ResourceSpecifier</code>. Applications
   * do not need to call this method. It is called automatically by the <code>ResourceFactory</code>
   * and cannot be called a second time.
   * 
   * @param aSpecifier
   *          specifies how to create a resource or locate an existing resource service.
   * @param aAdditionalParams
   *          a Map containing additional parameters. May be <code>null</code> if there are no
   *          parameters. Each class that implements this interface can decide what additional
   *          parameters it supports.
   * 
   * @return true if and only if initialization completed successfully. Reutrns false if the given
   *         <code>ResourceSpecifier</code> is not of an appropriate type for this Resource. If
   *         the <code>ResourceSpecifier</code> is of an appropriate type but is invalid or if
   *         some other failure occurs, an exception should be thrown.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization.
   * @throws UIMA_IllegalStateException
   *           if this method is called more than once on a single Resource instance.
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException;

  /**
   * Gets the metadata that describes this <code>Resource</code>.
   * 
   * @return an object containing all metadata for this resource.
   */
  public ResourceMetaData getMetaData();

  /**
   * Gets the {@link ResourceManager} that this Resource uses to locate other Resources.
   * 
   * @return the ResourceManager
   */
  public ResourceManager getResourceManager();

  /**
   * Gets the Logger that this Resource is currently using.
   * 
   * @return this Resource's logger
   */
  public Logger getLogger();

  /**
   * Sets the Logger that this Resource will use. If this method is not called, the default logger ({@link org.apache.uima.UIMAFramework#getLogger()})
   * will be used.
   * 
   * @param aLogger
   *          the logger for this Resource to use
   */
  public void setLogger(Logger aLogger);

  /**
   * Releases all resources held by this <code>Resource</code>.
   */
  public void destroy();

  /**
   * Gets the UIMA Context for this Resource. This can be used to access external resources or
   * facilities such as the Logger.
   * 
   * @return the UimaContext for use by this Resource
   */
  public UimaContext getUimaContext();

  /**
   * Gets the Administrative interface to the UIMA Context. This can be used by deployment wrappers
   * to modify the UimaContext (for example, by setting the Session object).
   * 
   * @return the administrative interface to this Resource's UimaContext
   */
  public UimaContextAdmin getUimaContextAdmin();

  /**
   * Key for the initialization parameter whose value is a reference to the {@link UimaContext} that
   * is to be used by this Resource to access its external resource and configuration parameters.
   * This value is used as a key in the <code>aAdditionalParams</code> Map that is passed to the
   * {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_UIMA_CONTEXT = "UIMA_CONTEXT";

  /**
   * Key for the initialization parameter whose value is a reference to the {@link ResourceManager}
   * that this Resource should use to locate and access other Resources. This value is used as a key
   * in the <code>aAdditionalParams</code> Map that is passed to the
   * {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_RESOURCE_MANAGER = "RESOURCE_MANAGER";

  /**
   * Key for the initialization parameter whose value is a
   * {@link org.apache.uima.resource.metadata.ConfigurationParameterSettings} object that holds
   * configuration settings that will be used to configure this Resource, overriding any
   * conflicting settings specified in this Resource's Descriptor. This value is used as a key in
   * the <code>aAdditionalParams</code> Map that is passed to the
   * {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_CONFIG_PARAM_SETTINGS = "CONFIG_PARAM_SETTINGS";
  
  /**
   * Key for the initialization parameter whose value is a {@link org.apache.uima.util.Settings}
   * object that holds the external override settings.  This will replace any prior settings.  
   * If omitted the value in the parent UIMA Context is inherited.  If there is no parent (i.e. at 
   * the root or top-level context) then the files in the system property UimaExternalOverrides are used.
   * This value is used as a key in the <code>aAdditionalParams</code> Map that is passed to the
   * {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_EXTERNAL_OVERRIDE_SETTINGS = "EXTERNAL_OVERRIDE_SETTINGS";

  /**
   * Key for the initialization parameter whose value is a {@link java.util.Properties} object that
   * holds settings that tune the performance of the framework. This value is used as a key in the
   * <code>aAdditionalParams</code> Map that is passed to the
   * {@link #initialize(ResourceSpecifier,Map)} method.
   * 
   * @see org.apache.uima.UIMAFramework#getDefaultPerformanceTuningProperties()
   */
  public static final String PARAM_PERFORMANCE_TUNING_SETTINGS = "PERFORMANCE_TUNING_SETTINGS";

  // /**
  // * Key for the initialization parameter whose value is an array of
  // * SofaMapping objects defined in an aggregate analysis engine.
  // *
  // * This value is used as a key in the <code>additionalParams</code> Map that
  // * is passed to the {@link #initialize(ResourceSpecifier,Map)} method.
  // */
  /** Used to pass the sofa mappings to the ASB */
  public static final String PARAM_AGGREGATE_SOFA_MAPPINGS = "AGGREGATE_SOFA_MAPPINGS";  // internal use only
  
  /**
   * Key to specify a pre-existing 
   * {@link org.apache.uima.resource.ConfigurationManager} object.
   * If specified, this object is used when initializing the UimaContext
   * associated with this Resource, instead
   * of creating a new instance.
   * This value is used as a key in
   * the <code>aAdditionalParams</code> Map that is passed to the
   * {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_CONFIG_MANAGER = "CONFIG_MANAGER";

}
