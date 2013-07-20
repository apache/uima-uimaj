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

import static java.util.Arrays.asList;
import static org.apache.uima.fit.descriptor.OperationalProperties.MODIFIES_CAS_DEFAULT;
import static org.apache.uima.fit.descriptor.OperationalProperties.MULTIPLE_DEPLOYMENT_ALLOWED_DEFAULT;
import static org.apache.uima.fit.descriptor.OperationalProperties.OUTPUTS_NEW_CASES_DEFAULT;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.createConfigurationData;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.ensureParametersComeInPairs;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.setParameters;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindExternalResource;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDependencies;
import static org.apache.uima.fit.factory.FsIndexFactory.createFsIndexCollection;
import static org.apache.uima.fit.factory.TypePrioritiesFactory.createTypePriorities;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.impl.AggregateAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.analysis_engine.metadata.impl.FlowControllerDeclaration_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.ConfigurationParameterFactory.ConfigurationData;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;

/**
 * A collection of static methods for creating UIMA {@link AnalysisEngineDescription
 * AnalysisEngineDescriptions} and {@link AnalysisEngine AnalysisEngines}.
 * 
 * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
 *      component instances?</a>
 */
public final class AnalysisEngineFactory {
  private AnalysisEngineFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Get an AnalysisEngine from the name (Java-style, dotted) of an XML descriptor file, and a set
   * of configuration parameters.
   * 
   * @param descriptorName
   *          The fully qualified, Java-style, dotted name of the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return the {@link AnalysisEngine} created from the XML descriptor and the configuration
   *         parameters.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(String descriptorName,
          Object... configurationData) throws InvalidXMLException, IOException,
          ResourceInitializationException {
    AnalysisEngineDescription aed = createEngineDescription(descriptorName, configurationData);
    return UIMAFramework.produceAnalysisEngine(aed);
  }

  /**
   * Get an AnalysisEngine from the name (Java-style, dotted) of an XML descriptor file, and a set
   * of configuration parameters.
   * 
   * @param descriptorName
   *          The fully qualified, Java-style, dotted name of the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return the {@link AnalysisEngine} created from the XML descriptor and the configuration
   *         parameters.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(String, Object...)}
   */
  @Deprecated
  public static AnalysisEngine createAnalysisEngine(String descriptorName,
          Object... configurationData) throws InvalidXMLException, IOException,
          ResourceInitializationException {
    return createEngine(descriptorName, configurationData);
  }

  /**
   * This method provides a convenient way to instantiate an AnalysisEngine where the default view
   * is mapped to the view name passed into the method.
   * 
   * @param analysisEngineDescription
   *          the analysis engine description from which the engine is instantiated
   * @param viewName
   *          the view name to map the default view to
   * @return an aggregate analysis engine consisting of a single component whose default view is
   *         mapped to the the view named by viewName.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @see AggregateBuilder
   */
  public static AnalysisEngine createEngine(
          AnalysisEngineDescription analysisEngineDescription, String viewName)
          throws ResourceInitializationException {
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(analysisEngineDescription, CAS.NAME_DEFAULT_SOFA, viewName);
    return builder.createAggregate();
  }

  /**
   * This method provides a convenient way to instantiate an AnalysisEngine where the default view
   * is mapped to the view name passed into the method.
   * 
   * @param analysisEngineDescription
   *          the analysis engine description from which the engine is instantiated
   * @param viewName
   *          the view name to map the default view to
   * @return an aggregate analysis engine consisting of a single component whose default view is
   *         mapped to the the view named by viewName.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @see AggregateBuilder
   * @deprecated use {@link #createEngine(AnalysisEngineDescription, String)}
   */
  @Deprecated
  public static AnalysisEngine createAnalysisEngine(
          AnalysisEngineDescription analysisEngineDescription, String viewName)
          throws ResourceInitializationException {
    return createEngine(analysisEngineDescription, viewName);
  }

  
  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param desc
   *          the descriptor to create the analysis engine from.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(AnalysisEngineDescription desc,
          Object... configurationData) throws ResourceInitializationException {
    if (configurationData == null || configurationData.length == 0) {
      return UIMAFramework.produceAnalysisEngine(desc, null, null);
    }
    else {
      AnalysisEngineDescription descClone = (AnalysisEngineDescription) desc.clone();
      ResourceCreationSpecifierFactory.setConfigurationParameters(descClone, configurationData);
      return UIMAFramework.produceAnalysisEngine(descClone);
    }
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param desc
   *          the descriptor to create the analysis engine from.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(AnalysisEngineDescription, Object...)}
   */
  @Deprecated
  public static AnalysisEngine createPrimitive(AnalysisEngineDescription desc,
          Object... configurationData) throws ResourceInitializationException {
    return createEngine(desc, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}. The type system is detected
   * automatically using {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type
   * priorities are detected automatically using
   * {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are detected automatically using
   * {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(Class<? extends AnalysisComponent> componentClass,
          Object... configurationData) throws ResourceInitializationException {
    AnalysisEngineDescription desc = createEngineDescription(componentClass, configurationData);

    // create the AnalysisEngine, initialize it and return it
    return createEngine(desc);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}. The type system is detected
   * automatically using {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type
   * priorities are detected automatically using
   * {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are detected automatically using
   * {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(Class, Object...)}
   */
  @Deprecated
  public static AnalysisEngine createPrimitive(Class<? extends AnalysisComponent> componentClass,
          Object... configurationData) throws ResourceInitializationException {
    return createEngine(componentClass, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(Class<? extends AnalysisComponent> componentClass,
          TypeSystemDescription typeSystem, Object... configurationData)
          throws ResourceInitializationException {
    return createEngine(componentClass, typeSystem, (TypePriorities) null, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(Class, TypeSystemDescription, Object...)}
   */
  @Deprecated
  public static AnalysisEngine createPrimitive(Class<? extends AnalysisComponent> componentClass,
          TypeSystemDescription typeSystem, Object... configurationData)
          throws ResourceInitializationException {
    return createEngine(componentClass, typeSystem, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities as an array of type names (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(Class<? extends AnalysisComponent> componentClass,
          TypeSystemDescription typeSystem, String[] typePriorities, Object... configurationData)
          throws ResourceInitializationException {
    TypePriorities tp = null;
    if (typePriorities != null) {
      tp = TypePrioritiesFactory.createTypePriorities(typePriorities);
    }
    return createEngine(componentClass, typeSystem, tp, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities as an array of type names (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(Class, TypeSystemDescription, String[], Object...)}
   */
  @Deprecated
  public static AnalysisEngine createPrimitive(Class<? extends AnalysisComponent> componentClass,
          TypeSystemDescription typeSystem, String[] typePriorities, Object... configurationData)
          throws ResourceInitializationException {
    return createEngine(componentClass, typeSystem, typePriorities, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(Class<? extends AnalysisComponent> componentClass,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          Object... configurationData) throws ResourceInitializationException {

    AnalysisEngineDescription desc = createEngineDescription(componentClass, typeSystem,
            typePriorities, configurationData);

    // create the AnalysisEngine, initialize it and return it
    return createEngine(desc);
  }
  
  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(Class, TypeSystemDescription, TypePriorities, Object...)}
   */
  @Deprecated
  public static AnalysisEngine createPrimitive(Class<? extends AnalysisComponent> componentClass,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          Object... configurationData) throws ResourceInitializationException {
    return createEngine(componentClass, typeSystem, typePriorities, configurationData);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component classes.
   * 
   * @param componentClasses
   *          a list of class that extend {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(
          List<Class<? extends AnalysisComponent>> componentClasses,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          SofaMapping[] sofaMappings, Object... configurationData)
          throws ResourceInitializationException {
    AnalysisEngineDescription desc = createEngineDescription(componentClasses, typeSystem,
            typePriorities, sofaMappings, configurationData);
    // create the AnalysisEngine, initialize it and return it
    AnalysisEngine engine = new AggregateAnalysisEngine_impl();
    engine.initialize(desc, null);
    return engine;
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component classes.
   * 
   * @param componentClasses
   *          a list of class that extend {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(List, TypeSystemDescription, TypePriorities, SofaMapping[], Object...)}
   */
  @Deprecated
  public static AnalysisEngine createAggregate(
          List<Class<? extends AnalysisComponent>> componentClasses,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          SofaMapping[] sofaMappings, Object... configurationData)
          throws ResourceInitializationException {
    return createEngine(componentClasses, typeSystem, typePriorities, sofaMappings,
            configurationData);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component classes.
   * 
   * @param componentClasses
   *          a list of class that extend {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(
          List<Class<? extends AnalysisComponent>> componentClasses,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          SofaMapping[] sofaMappings, FlowControllerDescription flowControllerDescription,
          Object... configurationData) throws ResourceInitializationException {
    AnalysisEngineDescription desc = createEngineDescription(componentClasses, typeSystem,
            typePriorities, sofaMappings, configurationData, flowControllerDescription);
    // create the AnalysisEngine, initialize it and return it
    AnalysisEngine engine = new AggregateAnalysisEngine_impl();
    engine.initialize(desc, null);
    return engine;
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component classes.
   * 
   * @param componentClasses
   *          a list of class that extend {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(List, TypeSystemDescription, TypePriorities, SofaMapping[], FlowControllerDescription, Object...)}
   */
  @Deprecated
  public static AnalysisEngine createAggregate(
          List<Class<? extends AnalysisComponent>> componentClasses,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          SofaMapping[] sofaMappings, FlowControllerDescription flowControllerDescription,
          Object... configurationData) throws ResourceInitializationException {
    return createEngine(componentClasses, typeSystem, typePriorities, sofaMappings,
            flowControllerDescription, configurationData);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component descriptions.
   * 
   * @param analysisEngineDescriptions
   *          a list of analysis engine descriptions from which the aggregate engine is instantiated
   * @param componentNames
   *          a list of names for the analysis engines in the aggregate. There must be exactly one
   *          name for each analysis engine, given in the same order as the descriptions.
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(
          List<AnalysisEngineDescription> analysisEngineDescriptions, List<String> componentNames,
          TypePriorities typePriorities, SofaMapping[] sofaMappings)
          throws ResourceInitializationException {

    AnalysisEngineDescription desc = createEngineDescription(analysisEngineDescriptions,
            componentNames, typePriorities, sofaMappings, null);
    // create the AnalysisEngine, initialize it and return it
    AnalysisEngine engine = new AggregateAnalysisEngine_impl();
    engine.initialize(desc, null);
    return engine;
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component descriptions.
   * 
   * @param analysisEngineDescriptions
   *          a list of analysis engine descriptions from which the aggregate engine is instantiated
   * @param componentNames
   *          a list of names for the analysis engines in the aggregate. There must be exactly one
   *          name for each analysis engine, given in the same order as the descriptions.
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(List, List, TypePriorities, SofaMapping[])}
   */
  @Deprecated
  public static AnalysisEngine createAggregate(
          List<AnalysisEngineDescription> analysisEngineDescriptions, List<String> componentNames,
          TypePriorities typePriorities, SofaMapping[] sofaMappings)
          throws ResourceInitializationException {
    return createEngine(analysisEngineDescriptions, componentNames, typePriorities,
            sofaMappings);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component descriptions.
   * 
   * @param analysisEngineDescriptions
   *          a list of analysis engine descriptions from which the aggregate engine is instantiated
   * @param componentNames
   *          a list of names for the analysis engines in the aggregate. There must be exactly one
   *          name for each analysis engine, given in the same order as the descriptions.
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngine(
          List<AnalysisEngineDescription> analysisEngineDescriptions, List<String> componentNames,
          TypePriorities typePriorities, SofaMapping[] sofaMappings,
          FlowControllerDescription flowControllerDescription)
          throws ResourceInitializationException {

    AnalysisEngineDescription desc = createEngineDescription(analysisEngineDescriptions,
            componentNames, typePriorities, sofaMappings, flowControllerDescription);
    // create the AnalysisEngine, initialize it and return it
    AnalysisEngine engine = new AggregateAnalysisEngine_impl();
    engine.initialize(desc, null);
    return engine;
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component descriptions.
   * 
   * @param analysisEngineDescriptions
   *          a list of analysis engine descriptions from which the aggregate engine is instantiated
   * @param componentNames
   *          a list of names for the analysis engines in the aggregate. There must be exactly one
   *          name for each analysis engine, given in the same order as the descriptions.
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @return an {@link AnalysisEngine} created from the specified component class and initialized
   *         with the configuration parameters.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngine(List, List, TypePriorities, SofaMapping[], FlowControllerDescription)}
   */
  @Deprecated
  public static AnalysisEngine createAggregate(
          List<AnalysisEngineDescription> analysisEngineDescriptions, List<String> componentNames,
          TypePriorities typePriorities, SofaMapping[] sofaMappings,
          FlowControllerDescription flowControllerDescription)
          throws ResourceInitializationException {
    return createEngine(analysisEngineDescriptions, componentNames, typePriorities,
            sofaMappings, flowControllerDescription);
  }

  /**
   * Get an {@link AnalysisEngine} from an XML descriptor file and a set of configuration
   * parameters.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The {@link AnalysisEngine} created from the XML descriptor and the configuration
   *         parameters.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   */
  public static AnalysisEngine createEngineFromPath(String descriptorPath,
          Object... configurationData) throws InvalidXMLException, IOException,
          ResourceInitializationException {
    AnalysisEngineDescription desc = createEngineDescriptionFromPath(descriptorPath,
            configurationData);
    return UIMAFramework.produceAnalysisEngine(desc);
  }

  /**
   * Get an {@link AnalysisEngine} from an XML descriptor file and a set of configuration
   * parameters.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The {@link AnalysisEngine} created from the XML descriptor and the configuration
   *         parameters.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @see <a href="package-summary.html#InstancesVsDescriptors">Why are descriptors better than
   *      component instances?</a>
   * @deprecated use {@link #createEngineFromPath(String, Object...)}
   */
  @Deprecated
  public static AnalysisEngine createAnalysisEngineFromPath(String descriptorPath,
          Object... configurationData) throws InvalidXMLException, IOException,
          ResourceInitializationException {
    return createEngineFromPath(descriptorPath, configurationData);
  }

  /**
   * Get an {@link AnalysisEngineDescription} from an XML descriptor file and a set of configuration
   * parameters.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The {@link AnalysisEngineDescription} created from the XML descriptor and the
   *         configuration parameters.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   */
  public static AnalysisEngineDescription createEngineDescriptionFromPath(
          String descriptorPath, Object... configurationData) throws InvalidXMLException,
          IOException {
    ResourceSpecifier specifier;
    specifier = ResourceCreationSpecifierFactory.createResourceCreationSpecifier(descriptorPath,
            configurationData);
    return (AnalysisEngineDescription) specifier;
  }

  /**
   * Get an {@link AnalysisEngineDescription} from an XML descriptor file and a set of configuration
   * parameters.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The {@link AnalysisEngineDescription} created from the XML descriptor and the
   *         configuration parameters.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   * @deprecated use {@link #createEngineDescriptionFromPath(String, Object...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createAnalysisEngineDescriptionFromPath(
          String descriptorPath, Object... configurationData) throws InvalidXMLException,
          IOException {
    return createEngineDescriptionFromPath(descriptorPath, configurationData);
  }

  /**
   * Provides a way to create an AnalysisEngineDescription using a descriptor file referenced by
   * name
   * 
   * @param descriptorName
   *          The fully qualified, Java-style, dotted name of the XML descriptor file.
   * @param configurationData
   *          should consist of name value pairs. Will override configuration parameter settings in
   *          the descriptor file
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this analysis engine.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   */
  public static AnalysisEngineDescription createEngineDescription(String descriptorName,
          Object... configurationData) throws InvalidXMLException, IOException {
    Import_impl imprt = new Import_impl();
    imprt.setName(descriptorName);
    URL url = imprt.findAbsoluteUrl(UIMAFramework.newDefaultResourceManager());
    ResourceSpecifier specifier = ResourceCreationSpecifierFactory.createResourceCreationSpecifier(
            url, configurationData);
    return (AnalysisEngineDescription) specifier;
  }

  /**
   * Provides a way to create an AnalysisEngineDescription using a descriptor file referenced by
   * name
   * 
   * @param descriptorName
   *          The fully qualified, Java-style, dotted name of the XML descriptor file.
   * @param configurationData
   *          should consist of name value pairs. Will override configuration parameter settings in
   *          the descriptor file
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this analysis engine.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   * @deprecated use {@link #createEngineDescription(String, Object...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createAnalysisEngineDescription(String descriptorName,
          Object... configurationData) throws InvalidXMLException, IOException {
    return createEngineDescription(descriptorName, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          Object... configurationData) throws ResourceInitializationException {
    return createEngineDescription(componentClass, typeSystem, (TypePriorities) null,
            configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(Class, TypeSystemDescription, Object...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createPrimitiveDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          Object... configurationData) throws ResourceInitializationException {
    return createEngineDescription(componentClass, typeSystem, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}. The type system is detected
   * automatically using {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type
   * priorities are detected automatically using
   * {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are detected automatically using
   * {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          Class<? extends AnalysisComponent> componentClass, Object... configurationData)
          throws ResourceInitializationException {
    TypeSystemDescription typeSystem = createTypeSystemDescription();
    TypePriorities typePriorities = createTypePriorities();
    FsIndexCollection fsIndexCollection = createFsIndexCollection();

    return createEngineDescription(componentClass, typeSystem,
            typePriorities, fsIndexCollection, (Capability[]) null, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}. The type system is detected
   * automatically using {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type
   * priorities are detected automatically using
   * {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are detected automatically using
   * {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(Class, Object...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createPrimitiveDescription(
          Class<? extends AnalysisComponent> componentClass, Object... configurationData)
          throws ResourceInitializationException {
    return createEngineDescription(componentClass, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, Object... configurationData)
          throws ResourceInitializationException {
    return createEngineDescription(componentClass, typeSystem, typePriorities,
            (FsIndexCollection) null, (Capability[]) null, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(Class, TypeSystemDescription, TypePriorities, Object...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createPrimitiveDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, Object... configurationData)
          throws ResourceInitializationException {
    return createEngineDescription(componentClass, typeSystem, typePriorities,
            configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param indexes
   *          the Feature Structure Index collection used by this analysis engine to iterate over
   *          annotations in the {@link org.apache.uima.cas.CAS}. If this is not null explicitly,
   *          any indexes declared via {@link org.apache.uima.fit.descriptor.FsIndexCollection} in
   *          the class are ignored.
   * @param capabilities
   *          the operations the component can perform in terms of consumed and produced types, sofa
   *          names, and languages. If this is set explicitly here, any capabilities declared via
   *          {@link SofaCapability} or {@link TypeCapability} in the class are ignored.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters. In addition to
   *          parameter names, external resource keys can also be specified. The value has to be an
   *          {@link ExternalResourceDescription} in that case.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          Object... configurationData) throws ResourceInitializationException {

    ensureParametersComeInPairs(configurationData);

    // Extract ExternalResourceDescriptions from configurationData
    // <ParamterName, ExternalResourceDescription> will be stored in this map
    Map<String, ExternalResourceDescription> externalResources = ExternalResourceFactory
            .extractExternalResourceParameters(configurationData);

    // Create primitive description normally
    ConfigurationData cdata = createConfigurationData(configurationData);
    return createEngineDescription(componentClass, typeSystem, typePriorities, indexes,
            capabilities, cdata.configurationParameters, cdata.configurationValues,
            externalResources);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param indexes
   *          the Feature Structure Index collection used by this analysis engine to iterate over
   *          annotations in the {@link org.apache.uima.cas.CAS}. If this is not null explicitly,
   *          any indexes declared via {@link org.apache.uima.fit.descriptor.FsIndexCollection} in
   *          the class are ignored.
   * @param capabilities
   *          the operations the component can perform in terms of consumed and produced types, sofa
   *          names, and languages. If this is set explicitly here, any capabilities declared via
   *          {@link SofaCapability} or {@link TypeCapability} in the class are ignored.
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters. In addition to
   *          parameter names, external resource keys can also be specified. The value has to be an
   *          {@link ExternalResourceDescription} in that case.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(Class, TypeSystemDescription, TypePriorities, FsIndexCollection, Capability[], Object...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createPrimitiveDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          Object... configurationData) throws ResourceInitializationException {
    return createEngineDescription(componentClass, typeSystem, typePriorities, indexes,
            capabilities, configurationData);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param indexes
   *          the Feature Structure Index collection used by this analysis engine to iterate over
   *          annotations in the {@link org.apache.uima.cas.CAS}. If this is not null explicitly,
   *          any indexes declared via {@link org.apache.uima.fit.descriptor.FsIndexCollection} in
   *          the class are ignored.
   * @param capabilities
   *          the operations the component can perform in terms of consumed and produced types, sofa
   *          names, and languages. If this is set explicitly here, any capabilities declared via
   *          {@link SofaCapability} or {@link TypeCapability} in the class are ignored.
   * @param configurationParameters
   *          the configuration parameter declarations.
   * @param configurationValues
   *          the configuration parameter values.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues)
          throws ResourceInitializationException {
    return createEngineDescription(componentClass, typeSystem, typePriorities, indexes,
            capabilities, configurationParameters, configurationValues, null);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param indexes
   *          the Feature Structure Index collection used by this analysis engine to iterate over
   *          annotations in the {@link org.apache.uima.cas.CAS}. If this is not null explicitly,
   *          any indexes declared via {@link org.apache.uima.fit.descriptor.FsIndexCollection} in
   *          the class are ignored.
   * @param capabilities
   *          the operations the component can perform in terms of consumed and produced types, sofa
   *          names, and languages. If this is set explicitly here, any capabilities declared via
   *          {@link SofaCapability} or {@link TypeCapability} in the class are ignored.
   * @param configurationParameters
   *          the configuration parameter declarations.
   * @param configurationValues
   *          the configuration parameter values.
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(Class, TypeSystemDescription, TypePriorities, FsIndexCollection, Capability[], ConfigurationParameter[], Object[])}
   */
  @Deprecated
  public static AnalysisEngineDescription createPrimitiveDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues)
          throws ResourceInitializationException {
    return createEngineDescription(componentClass, typeSystem, typePriorities, indexes,
            capabilities, configurationParameters, configurationValues);
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param indexes
   *          the Feature Structure Index collection used by this analysis engine to iterate over
   *          annotations in the {@link org.apache.uima.cas.CAS}. If this is set explicitly here,
   *          any indexes declared via {@link org.apache.uima.fit.descriptor.FsIndexCollection} in
   *          the class are ignored.
   * @param capabilities
   *          the operations the component can perform in terms of consumed and produced types, sofa
   *          names, and languages. If this is set explicitly here, any capabilities declared via
   *          {@link SofaCapability} or {@link TypeCapability} in the class are ignored.
   * @param configurationParameters
   *          the configuration parameter declarations.
   * @param configurationValues
   *          the configuration parameter values.
   * @param externalResources
   *          external resources to bind to the analysis engine. (may be null)
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues,
          Map<String, ExternalResourceDescription> externalResources)
          throws ResourceInitializationException {

    // create the descriptor and set configuration parameters
    AnalysisEngineDescription desc = UIMAFramework.getResourceSpecifierFactory()
            .createAnalysisEngineDescription();
    desc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    desc.setPrimitive(true);
    desc.setAnnotatorImplementationName(componentClass.getName());
    org.apache.uima.fit.descriptor.OperationalProperties componentAnno = ReflectionUtil
            .getInheritableAnnotation(org.apache.uima.fit.descriptor.OperationalProperties.class,
                    componentClass);
    if (componentAnno != null) {
      OperationalProperties op = desc.getAnalysisEngineMetaData().getOperationalProperties();
      op.setMultipleDeploymentAllowed(componentAnno.multipleDeploymentAllowed());
      op.setModifiesCas(componentAnno.modifiesCas());
      op.setOutputsNewCASes(componentAnno.outputsNewCases());
    } else {
      OperationalProperties op = desc.getAnalysisEngineMetaData().getOperationalProperties();
      op.setMultipleDeploymentAllowed(MULTIPLE_DEPLOYMENT_ALLOWED_DEFAULT);
      op.setModifiesCas(MODIFIES_CAS_DEFAULT);
      op.setOutputsNewCASes(OUTPUTS_NEW_CASES_DEFAULT);
    }

    // Configure resource meta data
    AnalysisEngineMetaData meta = desc.getAnalysisEngineMetaData();
    ResourceMetaDataFactory.configureResourceMetaData(meta, componentClass);

    // set parameters
    setParameters(desc, componentClass, configurationParameters, configurationValues);

    // set the type system
    if (typeSystem != null) {
      desc.getAnalysisEngineMetaData().setTypeSystem(typeSystem);
    }

    if (typePriorities != null) {
      desc.getAnalysisEngineMetaData().setTypePriorities(typePriorities);
    }

    // set indexes from the argument to this call and from the annotation present in the
    // component
    List<FsIndexCollection> fsIndexes = new ArrayList<FsIndexCollection>();
    if (indexes != null) {
      fsIndexes.add(indexes);
    } 
    fsIndexes.add(FsIndexFactory.createFsIndexCollection(componentClass));
    FsIndexCollection aggIndexColl = CasCreationUtils.mergeFsIndexes(fsIndexes,
            UIMAFramework.newDefaultResourceManager());
    desc.getAnalysisEngineMetaData().setFsIndexCollection(aggIndexColl);    

    // set capabilities from the argument to this call or from the annotation present in the
    // component if the argument is null
    if (capabilities != null) {
      desc.getAnalysisEngineMetaData().setCapabilities(capabilities);
    } else {
      Capability capability = CapabilityFactory.createCapability(componentClass);
      if (capability != null) {
        desc.getAnalysisEngineMetaData().setCapabilities(new Capability[] { capability });
      }
    }

    // Extract external resource dependencies
    desc.setExternalResourceDependencies(createExternalResourceDependencies(componentClass));

    // Bind External Resources
    if (externalResources != null) {
      for (Entry<String, ExternalResourceDescription> e : externalResources.entrySet()) {
        bindExternalResource(desc, e.getKey(), e.getValue());
      }
    }

    return desc;
  }

  /**
   * Create and configure a primitive {@link AnalysisEngine}.
   * 
   * @param componentClass
   *          a class that extends {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param indexes
   *          the Feature Structure Index collection used by this analysis engine to iterate over
   *          annotations in the {@link org.apache.uima.cas.CAS}. If this is set explicitly here,
   *          any indexes declared via {@link org.apache.uima.fit.descriptor.FsIndexCollection} in
   *          the class are ignored.
   * @param capabilities
   *          the operations the component can perform in terms of consumed and produced types, sofa
   *          names, and languages. If this is set explicitly here, any capabilities declared via
   *          {@link SofaCapability} or {@link TypeCapability} in the class are ignored.
   * @param configurationParameters
   *          the configuration parameter declarations.
   * @param configurationValues
   *          the configuration parameter values.
   * @param externalResources
   *          external resources to bind to the analysis engine. (may be null)
   * @return a description for this analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(Class, TypeSystemDescription, TypePriorities, FsIndexCollection, Capability[], ConfigurationParameter[], Object[], Map)}
   */
  @Deprecated
  public static AnalysisEngineDescription createPrimitiveDescription(
          Class<? extends AnalysisComponent> componentClass, TypeSystemDescription typeSystem,
          TypePriorities typePriorities, FsIndexCollection indexes, Capability[] capabilities,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues,
          Map<String, ExternalResourceDescription> externalResources)
          throws ResourceInitializationException {
    return createEngineDescription(componentClass, typeSystem, typePriorities, indexes,
            capabilities, configurationParameters, configurationValues, externalResources);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component classes.
   * 
   * @param componentClasses
   *          a list of class that extend {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          List<Class<? extends AnalysisComponent>> componentClasses,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          SofaMapping[] sofaMappings, Object... configurationData)
          throws ResourceInitializationException {

    List<AnalysisEngineDescription> primitiveEngineDescriptions = new ArrayList<AnalysisEngineDescription>();
    List<String> componentNames = new ArrayList<String>();

    for (Class<? extends AnalysisComponent> componentClass : componentClasses) {
      AnalysisEngineDescription primitiveDescription = createEngineDescription(componentClass,
              typeSystem, typePriorities, configurationData);
      primitiveEngineDescriptions.add(primitiveDescription);
      componentNames.add(componentClass.getName());
    }
    return createEngineDescription(primitiveEngineDescriptions, componentNames, typePriorities,
            sofaMappings, null);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component classes.
   * 
   * @param componentClasses
   *          a list of class that extend {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(List, TypeSystemDescription, TypePriorities, SofaMapping[], Object...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createAggregateDescription(
          List<Class<? extends AnalysisComponent>> componentClasses,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          SofaMapping[] sofaMappings, Object... configurationData)
          throws ResourceInitializationException {
    return createEngineDescription(componentClasses, typeSystem, typePriorities,
            sofaMappings, configurationData);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component descriptions.
   * 
   * @param analysisEngineDescriptions
   *          a list of analysis engine descriptions from which the aggregate engine is instantiated
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          AnalysisEngineDescription... analysisEngineDescriptions)
          throws ResourceInitializationException {
    String[] names = new String[analysisEngineDescriptions.length];
    int i = 0;
    for (AnalysisEngineDescription aed : analysisEngineDescriptions) {
      names[i] = aed.getImplementationName() + "-" + i;
      i++;
    }

    return createEngineDescription(asList(analysisEngineDescriptions), asList(names), null,
            null, null);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component descriptions.
   * 
   * @param analysisEngineDescriptions
   *          a list of analysis engine descriptions from which the aggregate engine is instantiated
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(AnalysisEngineDescription...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createAggregateDescription(
          AnalysisEngineDescription... analysisEngineDescriptions)
          throws ResourceInitializationException {
    return createEngineDescription(analysisEngineDescriptions);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component classes.
   * 
   * @param componentClasses
   *          a list of class that extend {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          List<Class<? extends AnalysisComponent>> componentClasses,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          SofaMapping[] sofaMappings, FlowControllerDescription flowControllerDescription,
          Object... configurationData) throws ResourceInitializationException {

    List<AnalysisEngineDescription> primitiveEngineDescriptions = new ArrayList<AnalysisEngineDescription>();
    List<String> componentNames = new ArrayList<String>();

    for (Class<? extends AnalysisComponent> componentClass : componentClasses) {
      AnalysisEngineDescription primitiveDescription = createEngineDescription(componentClass,
              typeSystem, typePriorities, configurationData);
      primitiveEngineDescriptions.add(primitiveDescription);
      componentNames.add(componentClass.getName());
    }
    return createEngineDescription(primitiveEngineDescriptions, componentNames, typePriorities,
            sofaMappings, flowControllerDescription);
  }

  /**
   * Create and configure an aggregate {@link AnalysisEngine} from several component classes.
   * 
   * @param componentClasses
   *          a list of class that extend {@link AnalysisComponent} e.g. via
   *          {@link org.apache.uima.fit.component.JCasAnnotator_ImplBase}
   * @param typeSystem
   *          A description of the types (may be null).
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @param configurationData
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(List, TypeSystemDescription, TypePriorities, SofaMapping[], FlowControllerDescription, Object...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createAggregateDescription(
          List<Class<? extends AnalysisComponent>> componentClasses,
          TypeSystemDescription typeSystem, TypePriorities typePriorities,
          SofaMapping[] sofaMappings, FlowControllerDescription flowControllerDescription,
          Object... configurationData) throws ResourceInitializationException {
    return createEngineDescription(componentClasses, typeSystem, typePriorities,
            sofaMappings, flowControllerDescription, configurationData);
  }

  /**
   * A simplified factory method for creating an aggregate description for a given flow controller
   * and a sequence of analysis engine descriptions
   * 
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @param analysisEngineDescriptions
   *          a list of analysis engine descriptions from which the aggregate engine is instantiated
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          FlowControllerDescription flowControllerDescription,
          AnalysisEngineDescription... analysisEngineDescriptions)
          throws ResourceInitializationException {
    String[] names = new String[analysisEngineDescriptions.length];
    int i = 0;
    for (AnalysisEngineDescription aed : analysisEngineDescriptions) {
      names[i] = aed.getImplementationName() + "-" + i;
      i++;
    }

    return createEngineDescription(asList(analysisEngineDescriptions), asList(names), null,
            null, flowControllerDescription);
  }

  /**
   * A simplified factory method for creating an aggregate description for a given flow controller
   * and a sequence of analysis engine descriptions
   * 
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @param analysisEngineDescriptions
   *          a list of analysis engine descriptions from which the aggregate engine is instantiated
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngineDescription(FlowControllerDescription, AnalysisEngineDescription...)}
   */
  @Deprecated
  public static AnalysisEngineDescription createAggregateDescription(
          FlowControllerDescription flowControllerDescription,
          AnalysisEngineDescription... analysisEngineDescriptions)
          throws ResourceInitializationException {
    return createEngineDescription(flowControllerDescription, analysisEngineDescriptions);
  }

  /**
   * A factory method for creating an aggregate description.
   * 
   * @param analysisEngineDescriptions
   *          list of analysis engine descriptions.
   * @param componentNames
   *          list of component names - must be one name per description!
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngineDescription createEngineDescription(
          List<AnalysisEngineDescription> analysisEngineDescriptions, List<String> componentNames,
          TypePriorities typePriorities, SofaMapping[] sofaMappings,
          FlowControllerDescription flowControllerDescription)
          throws ResourceInitializationException {

    if (componentNames == null) {
      throw new IllegalArgumentException("Parameter [componentNames] cannot be null");
    }

    if (analysisEngineDescriptions == null) {
      throw new IllegalArgumentException("Parameter [analysisEngineDescriptions] cannot be null");
    }

    if (analysisEngineDescriptions.size() != componentNames.size()) {
      throw new IllegalArgumentException("Number of descriptions ["
              + analysisEngineDescriptions.size() + "]does not match number of component names ["
              + componentNames.size() + "].");
    }

    // create the descriptor and set configuration parameters
    AnalysisEngineDescription desc = new AnalysisEngineDescription_impl();
    desc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    desc.setPrimitive(false);

    // if any of the aggregated analysis engines does not allow multiple
    // deployment, then the
    // aggregate engine may also not be multiply deployed
    boolean allowMultipleDeploy = true;
    for (AnalysisEngineDescription d : analysisEngineDescriptions) {
      allowMultipleDeploy &= d.getAnalysisEngineMetaData().getOperationalProperties()
              .isMultipleDeploymentAllowed();
    }
    desc.getAnalysisEngineMetaData().getOperationalProperties()
            .setMultipleDeploymentAllowed(allowMultipleDeploy);

    List<String> flowNames = new ArrayList<String>();

    for (int i = 0; i < analysisEngineDescriptions.size(); i++) {
      AnalysisEngineDescription aed = analysisEngineDescriptions.get(i);
      String componentName = componentNames.get(i);
      desc.getDelegateAnalysisEngineSpecifiersWithImports().put(componentName, aed);
      flowNames.add(componentName);
    }

    if (flowControllerDescription != null) {
      FlowControllerDeclaration flowControllerDeclaration = new FlowControllerDeclaration_impl();
      flowControllerDeclaration.setSpecifier(flowControllerDescription);
      desc.setFlowControllerDeclaration(flowControllerDeclaration);
    }

    FixedFlow fixedFlow = new FixedFlow_impl();
    fixedFlow.setFixedFlow(flowNames.toArray(new String[flowNames.size()]));
    desc.getAnalysisEngineMetaData().setFlowConstraints(fixedFlow);

    if (typePriorities != null) {
      desc.getAnalysisEngineMetaData().setTypePriorities(typePriorities);
    }

    if (sofaMappings != null) {
      desc.setSofaMappings(sofaMappings);
    }

    return desc;
  }

  /**
   * A factory method for creating an aggregate description.
   * 
   * @param analysisEngineDescriptions
   *          list of analysis engine descriptions.
   * @param componentNames
   *          list of component names - must be one name per description!
   * @param typePriorities
   *          The type priorities (may be null).
   * @param sofaMappings
   *          The SofA mappings (may be null).
   * @param flowControllerDescription
   *          the flow controller description to be used by this aggregate (may be null).
   * @return a description for this aggregate analysis engine.
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * @deprecated use {@link #createEngine(List, List, TypePriorities, SofaMapping[], FlowControllerDescription)}
   */
  @Deprecated
  public static AnalysisEngineDescription createAggregateDescription(
          List<AnalysisEngineDescription> analysisEngineDescriptions, List<String> componentNames,
          TypePriorities typePriorities, SofaMapping[] sofaMappings,
          FlowControllerDescription flowControllerDescription)
          throws ResourceInitializationException {
    return createEngineDescription(analysisEngineDescriptions, componentNames,
            typePriorities, sofaMappings, flowControllerDescription);
  }
}
