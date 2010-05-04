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

import org.apache.uima.Constants;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.ResourceMetaData;

/**
 * A <code>ResourceCreationSpecifier</code> is the supertype of
 * {@link org.apache.uima.analysis_engine.AnalysisEngineDescription},
 * {@link org.apache.uima.collection.CasConsumerDescription},
 * {@link org.apache.uima.collection.CollectionReaderDescription}, and
 * {@link org.apache.uima.collection.CasInitializerDescription}.
 * <p>
 * All Resource Creation Specifiers must the following:
 * <ul>
 * <li><code>frameworkImplementation</code>: The name of the UIMA framework in which the
 * component executes. The name for this implementation is given by {@link org.apache.uima.Constants#JAVA_FRAMEWORK_NAME}. 
 * A component that runs in the C++ enablement layer needs to have the framework name given by
 * {@link org.apache.uima.Constants#CPP_FRAMEWORK_NAME}.
 * <li>
 * <li><code>implementationName</code>: The fully-qualified Java class name of the user's
 * component (Annotator, CAS Consumer, Collection Reader, or CAS Initializer).
 * <li>
 * <li><code>metaData</code>: the {@link ResourceMetaData} describing the resource</li>
 * </ul>
 * 
 * The following are optional:
 * <ul>
 * <li>A set of {@link ExternalResourceDependency} objects that define this resource's dependencies
 * on other resources.</li>
 * <li>A set of {@link ExternalResourceDescription} objects that satisfy the dependencies.</li>
 * </ul>
 * 
 * 
 */
public interface ResourceCreationSpecifier extends ResourceSpecifier {
  /**
   * Gets the name of the AE framework implementation within which the Resource executes. The
   * framework name for this implementation is given by {@link org.apache.uima.Constants#JAVA_FRAMEWORK_NAME}..
   * 
   * @return the framework implementation name
   */
  public String getFrameworkImplementation();

  /**
   * Sets the name of the AE framework implementation within which the <code>ResourceCreationSpecifier</code> executes. The
   * framework name for this implementation is given by {@link Constants#JAVA_FRAMEWORK_NAME}..
   * 
   * @param aFrameworkImplementation
   *          the framework implementation name
   */
  public void setFrameworkImplementation(String aFrameworkImplementation);

  /**
   * Retrieves the name of this <code>ResourceCreationSpecifier</code>'s implementation. This must be a fully qualified Java class
   * name.
   * 
   * @return the implementation name of the CasConsumer
   */
  public String getImplementationName();

  /**
   * Sets the name of this <code>ResourceCreationSpecifier</code>'s implementation. This must be a fully qualified Java class
   * name.
   * 
   * @param aImplementationName
   *          the implementation name of the CasConsumer
   */
  public void setImplementationName(String aImplementationName);

  /**
   * Retrieves the <code>ResourceMetaData</code> to assign to the newly constructed <code>ResourceCreationSpecifier</code>.
   * 
   * @return the metadata for the new resource. This will always be modifiable.
   */
  public ResourceMetaData getMetaData();

  /**
   * Sets the MetaData for this <code>ResourceCreationSpecifier</code>.
   * 
   * @param aMetaData
   *          metadata to assign
   */
  public void setMetaData(ResourceMetaData aMetaData);

  /**
   * Retrieves descriptions of this <code>ResourceCreationSpecifier</code>'s dependencies on external resources. Each
   * required external resource is assigned a String identifier. This is the identifier that this
   * <code>ResourceCreationSpecifier</code> can use to locate the Resource (using the
   * {@link org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceObject(String)}
   * method).
   * 
   * @return an array of {@link ExternalResourceDependency} objects that describe this
   *         AnalysisEngine's resource dependencies.
   */
  public ExternalResourceDependency[] getExternalResourceDependencies();

  /**
   * Sets the descriptions of this <code>ResourceCreationSpecifier</code>'s dependencies on external resources.
   * 
   * @param aDependencies
   *          an array of {@link ExternalResourceDependency} objects that describe this
   *          <code>ResourceCreationSpecifier</code>'s resource dependencies.
   */
  public void setExternalResourceDependencies(ExternalResourceDependency[] aDependencies);

  /**
   * Gets the external resource dependency with the given key.
   * 
   * @param aKey
   *          the key of the external resource dependency to get
   * 
   * @return the resource dependency with the specified key, <code>null</code> if none.
   */
  public ExternalResourceDependency getExternalResourceDependency(String aKey);

  /**
   * Retrieves the Resource Manager configuration, which declares the resources that satisfy the
   * dependencies defined by {@link #getExternalResourceDependencies()}.
   * 
   * @return the Resource Manager configuration that describes how external resource dependencies
   *         are bound to actual resources.
   */
  public ResourceManagerConfiguration getResourceManagerConfiguration();

  /**
   * Sets the Resource Manager configuration, which declares the resources that satisfy the
   * dependencies defined by {@link #getExternalResourceDependencies()}.
   * 
   * @param aResourceManagerConfiguration
   *          the Resource Manager configuration that describes how external resource dependencies
   *          are bound to actual resources.
   */
  public void setResourceManagerConfiguration(
          ResourceManagerConfiguration aResourceManagerConfiguration);

  /**
   * Checks that this <code>ResourceCreationSpecifier</code> is valid. 
   * An exception is thrown if it is not valid. This only does
   * fairly lightweight checking. To do a more complete but more expensive check, use
   * {@link #doFullValidation()}.
   * 
   * @throws ResourceInitializationException
   *           if <code>aDesc</code> is invalid
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings in <code>aDesc</code> are invalid
   */
  public void validate() throws ResourceInitializationException, ResourceConfigurationException;

  /**
   * Checks that this <code>ResourceCreationSpecifier</code> is valid. 
   * An exception is thrown if it is not valid. This only does
   * fairly lightweight checking. To do a more complete but more expensive check, use
   * {@link #doFullValidation()}.
   * 
   * @param aResourceManager
   *          a ResourceManager instance to use to resolve imports by name.
   * 
   * @throws ResourceInitializationException
   *           if <code>aDesc</code> is invalid
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings in <code>aDesc</code> are invalid
   */
  public void validate(ResourceManager aResourceManager) throws ResourceInitializationException,
          ResourceConfigurationException;

  /**
   * Does full validation of this <code>ResourceCreationSpecifier</code>. 
   * This essentially performs all operations necessary to
   * instantiate a Resource except that it does not actually instantiate the implementation class.
   * If appropriate, this method will also attempt to create a CAS based on the descriptor, in order
   * to do full type system verification. If any operations fail, an exception will be thrown.
   * 
   * @throws ResourceInitializationException
   *           if validation failed
   */
  public void doFullValidation() throws ResourceInitializationException;

  /**
   * Does full validation of this <code>ResourceCreationSpecifier</code>. 
   * This essentially performs all operations necessary to
   * instantiate a Resource except that it does not actually instantiate the implementation class.
   * If appropriate, this method will also attempt to create a CAS based on the descriptor, in order
   * to do full type system verification. If any operations fail, an exception will be thrown.
   * 
   * @param aResourceManager
   *          a ResourceManager instance to use to load annotator classes, external resource
   *          classes, and resolve imports by name.
   * @throws ResourceInitializationException
   *           if validation failed
   */
  public void doFullValidation(ResourceManager aResourceManager)
          throws ResourceInitializationException;

}
