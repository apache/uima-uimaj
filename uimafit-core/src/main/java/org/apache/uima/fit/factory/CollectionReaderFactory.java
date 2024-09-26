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
package org.apache.uima.fit.factory;

import static org.apache.uima.fit.factory.ConfigurationParameterFactory.createConfigurationData;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.ensureParametersComeInPairs;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.setParameters;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResourceOnce;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createResourceDependencies;
import static org.apache.uima.fit.factory.FsIndexFactory.createFsIndexCollection;
import static org.apache.uima.fit.factory.ResourceCreationSpecifierFactory.createResourceCreationSpecifier;
import static org.apache.uima.fit.factory.TypePrioritiesFactory.createTypePriorities;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.ConfigurationParameterFactory.ConfigurationData;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;

/**
 */
public final class CollectionReaderFactory {

  private CollectionReaderFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Create a CollectionReader from an XML descriptor file and a set of configuration parameters.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created from the XML descriptor and the configuration parameters.
   * @throws ResourceInitializationException
   *           if the descriptor could not be created or if the component could not be instantiated
   * @throws InvalidXMLException
   *           if the descriptor could not be created
   * @throws IOException
   *           if the descriptor could not be read
   */
  public static CollectionReader createReaderFromPath(String descriptorPath,
          Object... configurationData)
          throws ResourceInitializationException, InvalidXMLException, IOException {
    CollectionReaderDescription desc = createReaderDescriptionFromPath(descriptorPath,
            configurationData);
    return UIMAFramework.produceCollectionReader(desc, ResourceManagerFactory.newResourceManager(),
            null);
  }

  /**
   * Create a CollectionReader from an XML descriptor file and a set of configuration parameters.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created from the XML descriptor and the configuration parameters.
   * @deprecated use {@link #createReaderFromPath(String, Object...)}
   * @throws ResourceInitializationException
   *           if the descriptor could not be created or if the component could not be instantiated
   * @throws InvalidXMLException
   *           if the descriptor could not be created
   * @throws IOException
   *           if the descriptor could not be read
   */
  @Deprecated
  public static CollectionReader createCollectionReaderFromPath(String descriptorPath,
          Object... configurationData)
          throws ResourceInitializationException, InvalidXMLException, IOException {
    return createReaderFromPath(descriptorPath, configurationData);
  }

  /**
   * Create a CollectionReader from an XML descriptor file and a set of configuration parameters.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return the description created from the XML descriptor and the configuration parameters.
   * @throws InvalidXMLException
   *           if the descriptor could not be created or if the component could not be instantiated
   * @throws IOException
   *           if the descriptor could not be read
   */
  public static CollectionReaderDescription createReaderDescriptionFromPath(String descriptorPath,
          Object... configurationData) throws InvalidXMLException, IOException {
    ResourceCreationSpecifier specifier = createResourceCreationSpecifier(descriptorPath,
            configurationData);
    return (CollectionReaderDescription) specifier;
  }

  /**
   * Create a CollectionReader from an XML descriptor file and a set of configuration parameters.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created from the XML descriptor and the configuration parameters.
   * @deprecated use {@link #createReaderDescriptionFromPath(String, Object...)}
   * @throws InvalidXMLException
   *           if the descriptor could not be created or if the component could not be instantiated
   * @throws IOException
   *           if the descriptor could not be read
   */
  @Deprecated
  public static CollectionReaderDescription createCollectionReaderDescriptionFromPath(
          String descriptorPath, Object... configurationData)
          throws InvalidXMLException, IOException {
    return createReaderDescriptionFromPath(descriptorPath, configurationData);
  }

  /**
   * Get a CollectionReader from the name (Java-style, dotted) of an XML descriptor file, and a set
   * of configuration parameters.
   * 
   * @param descriptorName
   *          The fully qualified, Java-style, dotted name of the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The AnalysisEngine created from the XML descriptor and the configuration parameters.
   * @throws ResourceInitializationException
   *           if the descriptor could not be created or if the component could not be instantiated
   * @throws InvalidXMLException
   *           if the descriptor could not be created
   * @throws IOException
   *           if the descriptor could not be read
   */
  public static CollectionReader createReader(String descriptorName, Object... configurationData)
          throws IOException, ResourceInitializationException, InvalidXMLException {
    ResourceManager resMgr = ResourceManagerFactory.newResourceManager();
    Import imp = UIMAFramework.getResourceSpecifierFactory().createImport();
    imp.setName(descriptorName);
    URL url = imp.findAbsoluteUrl(resMgr);
    ResourceSpecifier specifier = createResourceCreationSpecifier(url, configurationData);
    return UIMAFramework.produceCollectionReader(specifier, resMgr, null);
  }

  /**
   * Get a CollectionReader from the name (Java-style, dotted) of an XML descriptor file, and a set
   * of configuration parameters.
   * 
   * @param descriptorName
   *          The fully qualified, Java-style, dotted name of the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The AnalysisEngine created from the XML descriptor and the configuration parameters.
   * @deprecated use {@link #createReader(String, Object...)}
   * @throws ResourceInitializationException
   *           if the descriptor could not be created or if the component could not be instantiated
   * @throws InvalidXMLException
   *           if the descriptor could not be created
   * @throws IOException
   *           if the descriptor could not be read
   */
  @Deprecated
  public static CollectionReader createCollectionReader(String descriptorName,
          Object... configurationData)
          throws IOException, ResourceInitializationException, InvalidXMLException {
    return createReader(descriptorName, configurationData);
  }

  /**
   * Get a CollectionReader from a CollectionReader class, a type system, and a set of configuration
   * parameters. The type system is detected automatically using
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}.
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   */
  public static CollectionReader createReader(Class<? extends CollectionReader> readerClass,
          Object... configurationData) throws ResourceInitializationException {
    TypeSystemDescription tsd = createTypeSystemDescription();
    return createReader(readerClass, tsd, (TypePriorities) null, configurationData);
  }

  /**
   * Get a CollectionReader from a CollectionReader class, a type system, and a set of configuration
   * parameters. The type system is detected automatically using
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}.
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @deprecated use {@link #createReader(Class, Object...)}
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   */
  @Deprecated
  public static CollectionReader createCollectionReader(
          Class<? extends CollectionReader> readerClass, Object... configurationData)
          throws ResourceInitializationException {
    return createReader(readerClass, configurationData);
  }

  /**
   * Get a CollectionReader from a CollectionReader class, a type system, and a set of configuration
   * parameters.
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   */
  public static CollectionReader createReader(Class<? extends CollectionReader> readerClass,
          TypeSystemDescription typeSystem, Object... configurationData)
          throws ResourceInitializationException {
    return createReader(readerClass, typeSystem, (TypePriorities) null, configurationData);
  }

  /**
   * Get a CollectionReader from a CollectionReader class, a type system, and a set of configuration
   * parameters.
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @deprecated use {@link #createReader(Class, TypeSystemDescription, Object...)}
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   */
  @Deprecated
  public static CollectionReader createCollectionReader(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          Object... configurationData) throws ResourceInitializationException {
    return createReader(readerClass, typeSystem, configurationData);
  }

  /**
   * Get a CollectionReader from a CollectionReader class, a type system, and a set of configuration
   * parameters.
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param prioritizedTypeNames
   *          the type names in order of their priority.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @deprecated use
   *             {@link #createReaderDescription(Class, TypeSystemDescription, String[], Object...)}
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   */
  @Deprecated
  public static CollectionReader createReader(Class<? extends CollectionReader> readerClass,
          TypeSystemDescription typeSystem, String[] prioritizedTypeNames,
          Object... configurationData) throws ResourceInitializationException {
    TypePriorities typePriorities = createTypePriorities(prioritizedTypeNames);
    return createReader(readerClass, typeSystem, typePriorities, configurationData);
  }

  /**
   * Get a CollectionReader from a CollectionReader class, a type system, and a set of configuration
   * parameters.
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param prioritizedTypeNames
   *          the type names in order of their priority.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @deprecated use
   *             {@link #createReaderDescription(Class, TypeSystemDescription, String[], Object...)}
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   * 
   * @deprecated use {@link #createReader(Class, TypeSystemDescription, String[], Object...)}
   */
  @Deprecated
  public static CollectionReader createCollectionReader(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          String[] prioritizedTypeNames, Object... configurationData)
          throws ResourceInitializationException {
    return createReader(readerClass, typeSystem, prioritizedTypeNames, configurationData);
  }

  /**
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   */
  public static CollectionReader createReader(Class<? extends CollectionReader> readerClass,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          Object... configurationData) throws ResourceInitializationException {
    CollectionReaderDescription desc = createReaderDescription(readerClass, typeSystem,
            typePriorities, configurationData);
    return createReader(desc);
  }

  /**
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   * 
   * @deprecated use {@link #createReader(Class, TypeSystemDescription, TypePriorities, Object...)}
   */
  @Deprecated
  public static CollectionReader createCollectionReader(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, Object... configurationData)
          throws ResourceInitializationException {
    return createReader(readerClass, typeSystem, typePriorities, configurationData);
  }

  /**
   * This method creates a CollectionReader from a CollectionReaderDescription adding additional
   * configuration parameter data as desired
   * 
   * @param desc
   *          a descriptor
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   */
  public static CollectionReader createReader(CollectionReaderDescription desc,
          Object... configurationData) throws ResourceInitializationException {
    CollectionReaderDescription descClone = (CollectionReaderDescription) desc.clone();
    ResourceCreationSpecifierFactory.setConfigurationParameters(descClone, configurationData);
    return UIMAFramework.produceCollectionReader(descClone,
            ResourceManagerFactory.newResourceManager(), null);
  }

  /**
   * This method creates a CollectionReader from a CollectionReaderDescription adding additional
   * configuration parameter data as desired
   * 
   * @param desc
   *          a descriptor
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return The CollectionReader created and initialized with the type system and configuration
   *         parameters.
   * @throws ResourceInitializationException
   *           if the component could not be initialized
   * 
   * @deprecated use {@link #createReader(CollectionReaderDescription, Object...)}
   */
  @Deprecated
  public static CollectionReader createCollectionReader(CollectionReaderDescription desc,
          Object... configurationData) throws ResourceInitializationException {
    return createReader(desc, configurationData);
  }

  /**
   * A simple factory method for creating a CollectionReaderDescription with a given class, type
   * system description, and configuration data. The type system is detected automatically using
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type priorities are
   * detected automatically using {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are
   * detected automatically using {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   */
  public static CollectionReaderDescription createReaderDescription(
          Class<? extends CollectionReader> readerClass, Object... configurationData)
          throws ResourceInitializationException {
    TypeSystemDescription typeSystem = createTypeSystemDescription();
    TypePriorities typePriorities = createTypePriorities();
    FsIndexCollection fsIndexCollection = createFsIndexCollection();
    return createReaderDescription(readerClass, typeSystem, typePriorities, fsIndexCollection,
            (Capability[]) null, configurationData);
  }

  /**
   * A simple factory method for creating a CollectionReaderDescription with a given class, type
   * system description, and configuration data The type system is detected automatically using
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}.
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   * 
   * @deprecated use {@link #createReaderDescription(Class, Object...)}
   */
  @Deprecated
  public static CollectionReaderDescription createDescription(
          Class<? extends CollectionReader> readerClass, Object... configurationData)
          throws ResourceInitializationException {
    TypeSystemDescription tsd = createTypeSystemDescription();
    return createReaderDescription(readerClass, tsd, (TypePriorities) null, configurationData);
  }

  /**
   * A simple factory method for creating a CollectionReaderDescription with a given class, type
   * system description, and configuration data
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   */
  public static CollectionReaderDescription createReaderDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          Object... configurationData) throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, (TypePriorities) null,
            configurationData);
  }

  /**
   * A simple factory method for creating a CollectionReaderDescription with a given class, type
   * system description, and configuration data
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   * 
   * @deprecated use {@link #createReaderDescription(Class, TypeSystemDescription, Object...)}
   */
  @Deprecated
  public static CollectionReaderDescription createDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          Object... configurationData) throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, configurationData);
  }

  /**
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param prioritizedTypeNames
   *          the type names in order of their priority.
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   */
  public static CollectionReaderDescription createReaderDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          String[] prioritizedTypeNames, Object... configurationData)
          throws ResourceInitializationException {
    TypePriorities typePriorities = createTypePriorities(prioritizedTypeNames);
    return createReaderDescription(readerClass, typeSystem, typePriorities, configurationData);
  }

  /**
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param prioritizedTypeNames
   *          the type names in order of their priority.
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   * 
   * @deprecated use
   *             {@link #createReaderDescription(Class, TypeSystemDescription, String[], Object...)}
   */
  @Deprecated
  public static CollectionReaderDescription createDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          String[] prioritizedTypeNames, Object... configurationData)
          throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, prioritizedTypeNames,
            configurationData);
  }

  /**
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   */
  public static CollectionReaderDescription createReaderDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, Object... configurationData)
          throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, typePriorities,
            (FsIndexCollection) null, (Capability[]) null, configurationData);
  }

  /**
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   * 
   * @deprecated use
   *             {@link #createReaderDescription(Class, TypeSystemDescription, TypePriorities, Object...)}
   */
  @Deprecated
  public static CollectionReaderDescription createDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, Object... configurationData)
          throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, typePriorities, configurationData);
  }

  /**
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param indexes
   *          the index definitions
   * @param capabilities
   *          the input and output capabilities
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   */
  public static CollectionReaderDescription createReaderDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          Object... configurationData) throws ResourceInitializationException {

    ensureParametersComeInPairs(configurationData);

    // Extract ExternalResourceDescriptions from configurationData
    // <ParamterName, ExternalResourceDescription> will be stored in this map
    Map<String, ExternalResourceDescription> externalResources = ExternalResourceFactory
            .extractResourceParameters(configurationData);

    // Create description normally
    ConfigurationData cdata = createConfigurationData(configurationData);
    return createReaderDescription(readerClass, typeSystem, typePriorities, indexes, capabilities,
            cdata.configurationParameters, cdata.configurationValues, externalResources);
  }

  /**
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param indexes
   *          the index definitions
   * @param capabilities
   *          the input and output capabilities
   * @param configurationData
   *          configuration parameter data as name value pairs. Will override values already set in
   *          the description.
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   * 
   * @deprecated use
   *             {@link #createReaderDescription(Class, TypeSystemDescription, TypePriorities, FsIndexCollection, Capability[], Object...)}
   */
  @Deprecated
  public static CollectionReaderDescription createDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          Object... configurationData) throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, typePriorities, indexes, capabilities,
            configurationData);
  }

  /**
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param indexes
   *          the index definitions
   * @param capabilities
   *          the input and output capabilities
   * @param configurationParameters
   *          the configuration parameters
   * @param configurationValues
   *          the configuration values associated with the parameters
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   */
  public static CollectionReaderDescription createReaderDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues)
          throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, typePriorities, indexes, capabilities,
            configurationParameters, configurationValues, null);
  }

  /**
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param indexes
   *          the index definitions
   * @param capabilities
   *          the input and output capabilities
   * @param configurationParameters
   *          the configuration parameters
   * @param configurationValues
   *          the configuration values associated with the parameters
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   * @deprecated use
   *             {@link #createReaderDescription(Class, TypeSystemDescription, TypePriorities, FsIndexCollection, Capability[], ConfigurationParameter[], Object[])}
   */
  @Deprecated
  public static CollectionReaderDescription createDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues)
          throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, typePriorities, indexes, capabilities,
            configurationParameters, configurationValues);
  }

  /**
   * The factory method for creating CollectionReaderDescription objects for a given class,
   * TypeSystemDescription, TypePriorities, capabilities, and configuration data
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param indexes
   *          the index definitions
   * @param capabilities
   *          the input and output capabilities
   * @param configurationParameters
   *          the configuration parameters
   * @param configurationValues
   *          the configuration values associated with the parameters
   * @param externalResources
   *          the external resources
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   */
  public static CollectionReaderDescription createReaderDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues,
          Map<String, ExternalResourceDescription> externalResources)
          throws ResourceInitializationException {
    // create the descriptor and set configuration parameters
    CollectionReaderDescription desc = UIMAFramework.getResourceSpecifierFactory()
            .createCollectionReaderDescription();
    desc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    desc.setImplementationName(readerClass.getName());

    // set parameters
    setParameters(desc, readerClass, configurationParameters, configurationValues);

    // Configure resource meta data
    ResourceMetaData meta = desc.getMetaData();
    ResourceMetaDataFactory.configureResourceMetaData(meta, readerClass);

    // set the type system
    if (typeSystem != null) {
      desc.getCollectionReaderMetaData().setTypeSystem(typeSystem);
    }

    if (typePriorities != null) {
      desc.getCollectionReaderMetaData().setTypePriorities(typePriorities);
    }

    // set indexes from the argument to this call or from the annotation present in the
    // component if the argument is null
    if (indexes != null) {
      desc.getCollectionReaderMetaData().setFsIndexCollection(indexes);
    } else {
      desc.getCollectionReaderMetaData().setFsIndexCollection(createFsIndexCollection(readerClass));
    }

    // set capabilities from the argument to this call or from the annotation present in the
    // component if the argument is null
    if (capabilities != null) {
      desc.getCollectionReaderMetaData().setCapabilities(capabilities);
    } else {
      Capability capability = CapabilityFactory.createCapability(readerClass);
      if (capability != null) {
        desc.getCollectionReaderMetaData().setCapabilities(new Capability[] { capability });
      }
    }

    // Extract external resource dependencies
    desc.setExternalResourceDependencies(createResourceDependencies(readerClass));

    // Bind External Resources
    if (externalResources != null) {
      for (Entry<String, ExternalResourceDescription> e : externalResources.entrySet()) {
        bindResourceOnce(desc, e.getKey(), e.getValue());
      }
    }

    return desc;
  }

  /**
   * The factory method for creating CollectionReaderDescription objects for a given class,
   * TypeSystemDescription, TypePriorities, capabilities, and configuration data
   * 
   * @param readerClass
   *          The class of the CollectionReader to be created.
   * @param typeSystem
   *          A description of the types used by the CollectionReader (may be null).
   * @param typePriorities
   *          the type priorities
   * @param indexes
   *          the index definitions
   * @param capabilities
   *          the input and output capabilities
   * @param configurationParameters
   *          the configuration parameters
   * @param configurationValues
   *          the configuration values associated with the parameters
   * @param externalResources
   *          the external resources
   * @return the description created from the default parameters specified in the class and the
   *         configuration parameters
   * @throws ResourceInitializationException
   *           if the descriptor could not be set up
   * 
   * @deprecated use
   *             {@link #createReaderDescription(Class, TypeSystemDescription, TypePriorities, FsIndexCollection, Capability[], ConfigurationParameter[], Object[], Map)}
   */
  @Deprecated
  public static CollectionReaderDescription createDescription(
          Class<? extends CollectionReader> readerClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues,
          Map<String, ExternalResourceDescription> externalResources)
          throws ResourceInitializationException {
    return createReaderDescription(readerClass, typeSystem, typePriorities, indexes, capabilities,
            configurationParameters, configurationValues, externalResources);
  }
}
