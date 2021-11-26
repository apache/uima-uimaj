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

import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.UIMAFramework.produceResource;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.canParameterBeSet;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.createConfigurationData;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.ConfigurationParameterFactory.ConfigurationData;
import org.apache.uima.fit.internal.ExtendedExternalResourceDescription_impl;
import org.apache.uima.fit.internal.ReflectionUtil;
import org.apache.uima.fit.internal.ResourceList;
import org.apache.uima.resource.ConfigurableDataResourceSpecifier;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.ParameterizedDataResource;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.impl.ConfigurableDataResourceSpecifier_impl;
import org.apache.uima.resource.impl.ConfigurableDataResource_impl;
import org.apache.uima.resource.impl.ExternalResourceDescription_impl;
import org.apache.uima.resource.impl.FileResourceSpecifier_impl;
import org.apache.uima.resource.impl.Parameter_impl;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.impl.ResourceManagerConfiguration_impl;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.util.InvalidXMLException;

/**
 * Helper methods for external resources.
 * 
 */
public final class ExternalResourceFactory {
  public static final String PARAM_RESOURCE_NAME = "__UIMAFIT_RESOURCE_NAME__";

  /**
   * Used to separate resource name from key for nested resource.
   */
  public static final String PREFIX_SEPARATOR = "##";

  /**
   * Counter used to create unique resource names.
   */
  private final static AtomicLong DISAMBIGUATOR = new AtomicLong();

  private ExternalResourceFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * This method determines if the field is annotated with
   * {@link org.apache.uima.fit.descriptor.ExternalResource}.
   * 
   * @param field
   *          the field to analyze
   * @return whether the field is marked as an external resource
   */
  public static boolean isExternalResourceField(Field field) {
    return ReflectionUtil.isAnnotationPresent(field,
            org.apache.uima.fit.descriptor.ExternalResource.class);
  }

  /**
   * Create an external resource description for a custom resource.
   * 
   * @param aInterface
   *          the interface the resource should implement.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @return the description.
   * @see CustomResourceSpecifier
   */
  public static ExternalResourceDescription createResourceDescription(
          Class<? extends Resource> aInterface, Object... aParams) {
    return createNamedResourceDescription(uniqueResourceKey(aInterface.getName()), aInterface,
            aParams);
  }

  /**
   * Create an external resource description for a custom resource.
   * 
   * @param aName
   *          the name of the resource (the key).
   * @param aInterface
   *          the interface the resource should implement.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @return the description.
   * @see CustomResourceSpecifier
   */
  public static ExternalResourceDescription createNamedResourceDescription(final String aName,
          Class<? extends Resource> aInterface, Object... aParams) {
    ConfigurationParameterFactory.ensureParametersComeInPairs(aParams);

    // Extract ExternalResourceDescriptions from configurationData
    List<ExternalResourceBinding> bindings = new ArrayList<ExternalResourceBinding>();
    List<ExternalResourceDescription> descs = new ArrayList<ExternalResourceDescription>();
    for (Entry<String, ExternalResourceDescription> res : extractResourceParameters(aParams)
            .entrySet()) {
      bindings.add(createResourceBinding(res.getKey(), res.getValue()));
      descs.add(res.getValue());
    }

    ResourceSpecifier spec;
    if (ConfigurableDataResource_impl.class.isAssignableFrom(aInterface)) {
      ConfigurationData cfg = ConfigurationParameterFactory.createConfigurationData(aParams);
      ResourceMetaData_impl meta = new ResourceMetaData_impl();

      ConfigurationData reflectedConfigurationData = createConfigurationData(aInterface);
      ResourceCreationSpecifierFactory.setConfigurationParameters(meta,
              reflectedConfigurationData.configurationParameters,
              reflectedConfigurationData.configurationValues);
      ResourceCreationSpecifierFactory.setConfigurationParameters(meta,
              cfg.configurationParameters, cfg.configurationValues);

      ConfigurableDataResourceSpecifier_impl spec1 = new ConfigurableDataResourceSpecifier_impl();
      spec1.setUrl("");
      spec1.setMetaData(meta);
      spec = spec1;
    } else {
      List<Parameter> params = new ArrayList<Parameter>();
      if (aParams != null) {
        for (int i = 0; i < aParams.length / 2; i++) {
          if (ExternalResourceFactory.getResourceParameterType(aParams[i * 2 + 1]) != ResourceValueType.NO_RESOURCE) {
            continue;
          }

          Parameter param = new Parameter_impl();
          param.setName((String) aParams[i * 2]);
          param.setValue((String) aParams[i * 2 + 1]);
          params.add(param);
        }
      }

      CustomResourceSpecifier spec1 = UIMAFramework.getResourceSpecifierFactory()
              .createCustomResourceSpecifier();
      spec1.setResourceClassName(aInterface.getName());
      spec1.setParameters(params.toArray(new Parameter[params.size()]));
      spec = spec1;
    }

    ExtendedExternalResourceDescription_impl extRes = new ExtendedExternalResourceDescription_impl();
    extRes.setName(aName);
    extRes.setResourceSpecifier(spec);
    extRes.setExternalResourceBindings(bindings);
    extRes.setExternalResources(descs);

    return extRes;
  }

  /**
   * Create an external resource description for a {@link SharedResourceObject}.
   * @param aUrl
   *          the URL from which the resource is initialized.
   * @param aInterface
   *          the interface the resource should implement.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * 
   * @return the description.
   * @see ConfigurableDataResourceSpecifier
   * @see SharedResourceObject
   */
  public static ExternalResourceDescription createSharedResourceDescription(
          String aUrl, Class<? extends SharedResourceObject> aInterface, Object... aParams) {
    return createNamedResourceDescriptionUsingUrl(uniqueResourceKey(aInterface.getName()), aInterface,
            aUrl, aParams);
  }

  /**
   * Create an external resource description for a {@link SharedResourceObject}.
   * @param aUrl
   *          the URL from which the resource is initialized.
   * @param aInterface
   *          the interface the resource should implement.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * 
   * @return the description.
   * @see ConfigurableDataResourceSpecifier
   * @see SharedResourceObject
   */
  public static ExternalResourceDescription createSharedResourceDescription(
          URL aUrl, Class<? extends SharedResourceObject> aInterface, Object... aParams) {
    return createNamedResourceDescriptionUsingUrl(uniqueResourceKey(aInterface.getName()), aInterface,
            aUrl.toString(), aParams);
  }

  /**
   * Create an external resource description for a {@link SharedResourceObject}.
   * @param aFile
   *          the file from which the resource is initialized.
   * @param aInterface
   *          the interface the resource should implement.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * 
   * @return the description.
   * @see ConfigurableDataResourceSpecifier
   * @see SharedResourceObject
   */
  public static ExternalResourceDescription createSharedResourceDescription(
          File aFile, Class<? extends SharedResourceObject> aInterface, Object... aParams) {
    try {
      return createSharedResourceDescription(aFile.toURI().toURL(), aInterface, aParams);
    } catch (MalformedURLException e) {
      // This is something that usually cannot happen, so we degrade this to an
      // IllegalArgumentException which is a RuntimeException that does not need to be caught.
      throw new IllegalArgumentException("File converts to illegal URL [" + aFile + "]");
    }
  }

  /**
   * Create an external resource description for a {@link SharedResourceObject}.
   * 
   * @param aName
   *          the name of the resource (the key).
   * @param aInterface
   *          the interface the resource should implement.
   * @param aUrl
   *          the URL from which the resource is initialized.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @return the description.
   * @see ConfigurableDataResourceSpecifier
   * @see SharedResourceObject
   */
  public static ExternalResourceDescription createNamedResourceDescriptionUsingUrl(final String aName,
          Class<? extends SharedResourceObject> aInterface, String aUrl, Object... aParams) {
    // Extract ExternalResourceDescriptions from configurationData
    List<ExternalResourceBinding> bindings = new ArrayList<ExternalResourceBinding>();
    List<ExternalResourceDescription> descs = new ArrayList<ExternalResourceDescription>();
    for (Entry<String, ExternalResourceDescription> res : extractResourceParameters(aParams)
            .entrySet()) {
      bindings.add(createResourceBinding(res.getKey(), res.getValue()));
      descs.add(res.getValue());
    }

    ConfigurationData cfg = ConfigurationParameterFactory.createConfigurationData(aParams);
    ResourceMetaData_impl meta = new ResourceMetaData_impl();

    ConfigurationData reflectedConfigurationData = createConfigurationData(aInterface);
    ResourceCreationSpecifierFactory.setConfigurationParameters(meta,
            reflectedConfigurationData.configurationParameters,
            reflectedConfigurationData.configurationValues);
    ResourceCreationSpecifierFactory.setConfigurationParameters(meta, cfg.configurationParameters,
            cfg.configurationValues);

    ConfigurableDataResourceSpecifier_impl spec = new ConfigurableDataResourceSpecifier_impl();
    spec.setUrl(aUrl);
    spec.setMetaData(meta);

    ExtendedExternalResourceDescription_impl extRes = new ExtendedExternalResourceDescription_impl();
    extRes.setName(aName);
    extRes.setResourceSpecifier(spec);
    extRes.setImplementationName(aInterface.getName());
    extRes.setExternalResourceBindings(bindings);
    extRes.setExternalResources(descs);

    return extRes;
  }

  /**
   * Create an external resource description for a file addressable via an URL.
   * 
   * @param aName
   *          the name of the resource (the key).
   * @param aUrl
   *          a URL.
   * @return the description.
   * @see FileResourceSpecifier
   */
  public static ExternalResourceDescription createNamedFileResourceDescription(
          final String aName, String aUrl) {
    ExternalResourceDescription extRes = new ExternalResourceDescription_impl();
    extRes.setName(aName);
    FileResourceSpecifier frs = new FileResourceSpecifier_impl();
    frs.setFileUrl(aUrl);
    extRes.setResourceSpecifier(frs);
    return extRes;
  }

  /**
   * Create an external resource binding.
   * 
   * @param aKey
   *          the key to bind to.
   * @param aResource
   *          the resource to bind.
   * @return the description.
   */
  public static ExternalResourceBinding createResourceBinding(final String aKey,
          final ExternalResourceDescription aResource) {
    return createResourceBinding(aKey, aResource.getName());
  }

  /**
   * Create an external resource binding. This is a more convenient method of creating an
   * {@link ExternalResourceBinding} than calling
   * {@link ResourceSpecifierFactory#createExternalResourceBinding()} and setting the resource name
   * and key manually.
   * 
   * @param aKey
   *          the key to bind to.
   * @param aResourceKey
   *          the resource key to bind.
   * @return the description.
   */
  public static ExternalResourceBinding createResourceBinding(final String aKey,
          final String aResourceKey) {
    ExternalResourceBinding extResBind = getResourceSpecifierFactory()
            .createExternalResourceBinding();
    extResBind.setResourceName(aResourceKey);
    extResBind.setKey(aKey);
    return extResBind;
  }

  /**
   * Creates an {@link ExternalResourceDependency} for a field annotated with
   * {@link ExternalResource}.
   * 
   * @param field
   *          the field to analyze
   * @return a external resource dependency
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static ExternalResourceDependency createResourceDependency(Field field) {
    ExternalResource era = ReflectionUtil.getAnnotation(field, ExternalResource.class);

    // Get the binding key for the specified field. If no key is set, use the field name as key.
    String key = era.key();
    if (key.length() == 0) {
      key = field.getName();
    }

    // Get the type of class/interface a resource has to implement to bind to the annotated field.
    // If no API is set, get it from the annotated field type.
    Class<? extends Resource> api = era.api();
    
    // If no API is specified, look at the annotated field
    if (api == Resource.class) {
      if (
              Resource.class.isAssignableFrom(field.getType()) || 
              SharedResourceObject.class.isAssignableFrom(field.getType())
      ) {
        // If no API is set, check if the field type is already a resource type
        api = (Class<? extends Resource>) field.getType();
      } else {
        // If the field does not have a resource type, assume whatever. This allows to use
        // a resource locator without having to specify the api parameter. It also allows
        // to directly inject Java objects - yes, I know that Object does not extend
        // Resource - REC, 2011-03-25
        api = (Class) Object.class;
      }
    }

    return createResourceDependency(key, api, !era.mandatory(), era.description());
  }

  /**
   * Creates an ExternalResourceDependency for a given key and interface. This is a more convenient
   * method of creating an {@link ExternalResourceDependency} than calling
   * {@link ResourceSpecifierFactory#createExternalResourceDependency()} and setting the fields
   * manually.
   * 
   * @param aKey
   *          the resource key
   * @param aInterface
   *          the resource interface
   * @param aOptional
   *          determines whether the dependency is optional
   * @param aDescription
   *          a description of the resource
   * @return the external resource dependency
   */
  public static ExternalResourceDependency createResourceDependency(final String aKey,
          final Class<?> aInterface, final boolean aOptional, String aDescription) {
    ExternalResourceDependency dep = getResourceSpecifierFactory()
            .createExternalResourceDependency();
    dep.setInterfaceName(aInterface.getName());
    dep.setKey(aKey);
    dep.setOptional(aOptional);
    dep.setDescription(aDescription);
    return dep;
  }

  /**
   * @param cls
   *          the class to analyze
   * @return the external resource dependencies
   * @throws ResourceInitializationException
   *           if the external resource dependencies could not be created
   */
  public static ExternalResourceDependency[] createResourceDependencies(
          Class<?> cls) throws ResourceInitializationException {
    Map<String, ExternalResourceDependency> depMap = new HashMap<String, ExternalResourceDependency>();
    ExternalResourceFactory.createResourceDependencies(cls, cls, depMap);
    Collection<ExternalResourceDependency> deps = depMap.values();
    return deps.toArray(new ExternalResourceDependency[deps.size()]);
  }

  private static <T> void createResourceDependencies(Class<?> baseCls, Class<?> cls,
          Map<String, ExternalResourceDependency> dependencies)
          throws ResourceInitializationException {
    if (cls.getSuperclass() != null) {
      createResourceDependencies(baseCls, cls.getSuperclass(), dependencies);
    }

    for (Field field : cls.getDeclaredFields()) {
      if (!ReflectionUtil.isAnnotationPresent(field, ExternalResource.class)) {
        continue;
      }

      ExternalResourceDependency dep = createResourceDependency(field);

      if (dependencies.containsKey(dep.getKey())) {
        throw new ResourceInitializationException(new IllegalStateException("Key [" + dep.getKey()
                + "] may only be used on a single field."));
      }

      dependencies.put(dep.getKey(), dep);
    }
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * is encounter that has the specified key, the resource will be bound.
   * <p>
   * <b>Caveat</b>: If you use this method, you may expect that {@link DataResource#getUrl()} or
   * {@link DataResource#getUri()} will return the same URL that you have specified here. This may
   * <b>NOT</b> be the case. UIMA will internally try to resolve the URL via a
   * {@link ResourceManager}. If it cannot resolve a remove URL, this mechanism will think it may be
   * a local file and will return some local path - or it may redirect it to some location as though
   * fit by the {@link ResourceManager}.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aUrl
   *          a URL.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @see FileResourceSpecifier
   */
  public static void bindResource(ResourceSpecifier aDesc, String aKey, URL aUrl)
          throws InvalidXMLException {
    bindResource(aDesc, aKey, aUrl.toString());
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * is encounter that has the specified key, the resource will be bound.
   * <p>
   * <b>Caveat</b>: If you use this method, you may expect that {@link DataResource#getUrl()} or
   * {@link DataResource#getUri()} will return the URL of the file that you have specified here.
   * This may <b>NOT</b> be the case. UIMA will internally try to resolve the URL via a
   * {@link ResourceManager}. If it cannot resolve a remove URL, this mechanism will think it may be
   * a local file and will return some local path - or it may redirect it to some location as though
   * fit by the {@link ResourceManager}.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aFile
   *          a file.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @see FileResourceSpecifier
   */
  public static void bindResource(ResourceSpecifier aDesc, String aKey, File aFile)
          throws InvalidXMLException {
    try {
      bindResource(aDesc, aKey, aFile.toURI().toURL());
    } catch (MalformedURLException e) {
      // This is something that usually cannot happen, so we degrade this to an
      // IllegalArgumentException which is a RuntimeException that does not need to be caught.
      throw new IllegalArgumentException("File converts to illegal URL [" + aFile + "]");
    }
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * is encountered that has the specified key, the resource will be bound.
   * <p>
   * <b>Caveat</b>: If you use this method, you may expect that {@link DataResource#getUrl()} or
   * {@link DataResource#getUri()} will return the same URL that you have specified here. This is
   * may <b>NOT</b> be the case. UIMA will internally try to resolve the URL via a
   * {@link ResourceManager}. If it cannot resolve a remove URL, this mechanism will think it may be
   * a local file and will return some local path - or it may redirect it to some location as though
   * fit by the {@link ResourceManager}.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aUrl
   *          a URL.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @see FileResourceSpecifier
   * @deprecated Use {@link #bindResourceUsingUrl(ResourceSpecifier, String, String)}
   */
  @Deprecated
  public static void bindResource(ResourceSpecifier aDesc, String aKey, String aUrl)
          throws InvalidXMLException {
    bindResourceUsingUrl(aDesc, aKey, aUrl);
  }
  
  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * is encountered that has the specified key, the resource will be bound.
   * <p>
   * <b>Caveat</b>: If you use this method, you may expect that {@link DataResource#getUrl()} or
   * {@link DataResource#getUri()} will return the same URL that you have specified here. This is
   * may <b>NOT</b> be the case. UIMA will internally try to resolve the URL via a
   * {@link ResourceManager}. If it cannot resolve a remove URL, this mechanism will think it may be
   * a local file and will return some local path - or it may redirect it to some location as though
   * fit by the {@link ResourceManager}.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aUrl
   *          a URL.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @see FileResourceSpecifier
   */
  public static void bindResourceUsingUrl(ResourceSpecifier aDesc, String aKey, String aUrl)
          throws InvalidXMLException {
    ExternalResourceDescription extRes = createNamedFileResourceDescription(aKey, aUrl);
    bindResource(aDesc, aKey, extRes);
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * with a compatible type is found, the resource will be bound.
   * 
   * @param aDesc
   *          a description.
   * @param aRes
   *          the resource to bind.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @throws ClassNotFoundException
   *           if the resource implementation class or interface class could not be accessed
   * @see CustomResourceSpecifier
   */
  public static void bindResource(ResourceSpecifier aDesc, Class<? extends Resource> aRes,
          String... aParams) throws InvalidXMLException, ClassNotFoundException {
    bindResource(aDesc, aRes, aRes, aParams);
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * with a compatible type is found, the resource will be bound.
   * 
   * @param aDesc
   *          a description.
   * @param aApi
   *          the resource interface.
   * @param aRes
   *          the resource to bind.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @throws ClassNotFoundException
   *           if the resource implementation class or interface class could not be accessed
   * @see CustomResourceSpecifier
   */
  public static void bindResource(ResourceSpecifier aDesc, Class<?> aApi,
          Class<? extends Resource> aRes, String... aParams) throws InvalidXMLException,
          ClassNotFoundException {
    // Appending a disambiguation suffix it possible to have multiple instances of the same
    // resource with different settings to different keys.
    ExternalResourceDescription extRes = createNamedResourceDescription(
            uniqueResourceKey(aRes.getName()), aRes, (Object[]) aParams);
    bindResource(aDesc, extRes);
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * with a compatible type is found, the resource will be bound.
   * 
   * @param aDesc
   *          a description.
   * @param aRes
   *          the resource to bind.
   * @param aUrl
   *          the URL from which the resource is initialized.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @throws ClassNotFoundException
   *           if the resource implementation class or interface class could not be accessed
   * @see SharedResourceObject
   */
  public static void bindResourceUsingUrl(ResourceSpecifier aDesc,
          Class<? extends SharedResourceObject> aRes, String aUrl, Object... aParams)
          throws InvalidXMLException, ClassNotFoundException {
    ExternalResourceDescription extRes = createNamedResourceDescriptionUsingUrl(
            uniqueResourceKey(aRes.getName()), aRes, aUrl, aParams);
    scanRecursivelyForDependenciesByInterfaceAndBind((AnalysisEngineDescription) aDesc, extRes);
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * is encountered that has a key equal to the API class name, the resource will be bound.
   * 
   * @param aDesc
   *          a description.
   * @param aApi
   *          the resource interface.
   * @param aRes
   *          the resource to bind.
   * @param aUrl
   *          the URL from which the resource is initialized.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @see SharedResourceObject
   */
  public static void bindResourceUsingUrl(ResourceSpecifier aDesc, Class<?> aApi,
          Class<? extends SharedResourceObject> aRes, String aUrl, Object... aParams)
          throws InvalidXMLException {
    bindResourceUsingUrl(aDesc, aApi.getName(), aRes, aUrl, aParams);
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * with the given key is encountered the resource will be bound.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aRes
   *          the resource to bind.
   * @param aUrl
   *          the URL from which the resource is initialized.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @see SharedResourceObject
   */
  public static void bindResourceUsingUrl(ResourceSpecifier aDesc, String aKey,
          Class<? extends SharedResourceObject> aRes, String aUrl, Object... aParams)
          throws InvalidXMLException {
    ExternalResourceDescription extRes = createNamedResourceDescriptionUsingUrl(
            uniqueResourceKey(aRes.getName()), aRes, aUrl, aParams);
    scanRecursivelyForDependenciesByKeyAndBind((AnalysisEngineDescription) aDesc, aKey, extRes);
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * with the given key is encountered, the given resource is bound to it.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aRes
   *          the resource to bind.
   * @param aParams
   *          parameters passed to the resource when it is created.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @see CustomResourceSpecifier
   */
  public static void bindResource(ResourceSpecifier aDesc, String aKey,
          Class<? extends Resource> aRes, String... aParams) throws InvalidXMLException {
    if (ParameterizedDataResource.class.isAssignableFrom(aRes)) {
      createDependency(aDesc, aKey, DataResource.class);
    }

    // Appending a disambiguation suffix it possible to have multiple instances of the same
    // resource with different settings to different keys.
    ExternalResourceDescription extRes = createNamedResourceDescription(
            uniqueResourceKey(aRes.getName()), aRes, (Object[]) aParams);
    bindResource(aDesc, aKey, extRes);
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * with a compatible type is found, the given resource is bound to it.
   * 
   * @param aDesc
   *          a description.
   * @param aResDesc
   *          the resource description.
   * @throws InvalidXMLException
   *           if import resolution failed
   * @throws ClassNotFoundException
   *           if the resource implementation class or interface class could not be accessed
   */
  public static void bindResource(ResourceSpecifier aDesc, ExternalResourceDescription aResDesc)
          throws InvalidXMLException, ClassNotFoundException {
    // Dispatch
    if (aDesc instanceof AnalysisEngineDescription) {
      scanRecursivelyForDependenciesByInterfaceAndBind((AnalysisEngineDescription) aDesc, aResDesc);
    }
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * with the given key is encountered, the given resource is bound to it.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aResDesc
   *          the resource description.
   * @throws InvalidXMLException
   *           if import resolution failed
   */
  public static void bindResource(ResourceSpecifier aDesc, String aKey,
          ExternalResourceDescription aResDesc) throws InvalidXMLException {
    // Dispatch
    if (aDesc instanceof AnalysisEngineDescription) {
      scanRecursivelyForDependenciesByKeyAndBind((AnalysisEngineDescription) aDesc, aKey, aResDesc);
    }
  }

  /**
   * Create a new dependency for the specified resource and bind it. This method is helpful for UIMA
   * components that do not use the uimaFIT {@link ExternalResource} annotation, because no external
   * resource dependencies can be automatically generated by uimaFIT for such components.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aImpl
   *          the resource implementation.
   * @param aParams
   *          additional parameters supported by the resource.
   * @throws InvalidXMLException
   *           if import resolution failed
   */
  public static void createDependencyAndBind(ResourceSpecifier aDesc, String aKey,
          Class<? extends Resource> aImpl, String... aParams) throws InvalidXMLException {
    Class<?> api = (ParameterizedDataResource.class.isAssignableFrom(aImpl)) ? DataResource.class
            : aImpl;
    createDependencyAndBind(aDesc, aKey, aImpl, api, aParams);
  }

  /**
   * Create a new dependency for the specified resource and bind it. This method is helpful for UIMA
   * components that do not use the uimaFIT {@link ExternalResource} annotation, because no external
   * resource dependencies can be automatically generated by uimaFIT for such components.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aImpl
   *          the resource implementation.
   * @param aApi
   *          the resource interface
   * @param aParams
   *          additional parameters supported by the resource.
   * @throws InvalidXMLException
   *           if import resolution failed
   */
  public static void createDependencyAndBind(ResourceSpecifier aDesc, String aKey,
          Class<? extends Resource> aImpl, Class<?> aApi, String... aParams)
          throws InvalidXMLException {
    createDependency(aDesc, aKey, aApi);
    bindResource(aDesc, aKey, aImpl, aParams);
  }

  /**
   * Create a new dependency for the specified resource and bind it. This method is helpful for UIMA
   * components that do not use the uimaFIT {@link ExternalResource} annotation, because no external
   * resource dependencies can be automatically generated by uimaFIT for such components.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aApi
   *          the resource API.
   */
  public static void createDependency(ResourceSpecifier aDesc, String aKey, Class<?> aApi) {
    ExternalResourceDependency[] deps = getResourceDependencies(aDesc);
    if (deps == null) {
      deps = new ExternalResourceDependency[] {};
    }

    // Check if the resource dependency is already present
    boolean found = false;
    for (ExternalResourceDependency dep : deps) {
      if (dep.getKey().equals(aKey)) {
        found = true;
        break;
      }
    }

    // If not, create one
    if (!found) {
      setResourceDependencies(
              aDesc,
              ArrayUtils.add(deps,
                      createResourceDependency(aKey, aApi, false, null)));
    }
  }

  /**
   * Convenience method to set the external resource dependencies on a resource specifier.
   * Unfortunately different methods need to be used for different sub-classes.
   * 
   * @throws IllegalArgumentException
   *           if the sub-class passed is not supported.
   */
  private static void setResourceDependencies(ResourceSpecifier aDesc,
          ExternalResourceDependency[] aDependencies) {
    if (aDesc instanceof CollectionReaderDescription) {
      ((CollectionReaderDescription) aDesc).setExternalResourceDependencies(aDependencies);
    } else if (aDesc instanceof AnalysisEngineDescription) {
      ((AnalysisEngineDescription) aDesc).setExternalResourceDependencies(aDependencies);
    } else {
      throw new IllegalArgumentException(
              "Resource specified cannot have external resource dependencies");
    }
  }

  /**
   * Convenience method to get the external resource dependencies from a resource specifier.
   * Unfortunately different methods need to be used for different sub-classes.
   * 
   * @throws IllegalArgumentException
   *           if the sub-class passed is not supported.
   */
  private static ExternalResourceDependency[] getResourceDependencies(
          ResourceSpecifier aDesc) {
    if (aDesc instanceof CollectionReaderDescription) {
      return ((CollectionReaderDescription) aDesc).getExternalResourceDependencies();
    } else if (aDesc instanceof AnalysisEngineDescription) {
      return ((AnalysisEngineDescription) aDesc).getExternalResourceDependencies();
    } else {
      throw new IllegalArgumentException(
              "Resource specified cannot have external resource dependencies");
    }
  }

  /**
   * Create a new dependency for the specified resource and bind it. This method is helpful for UIMA
   * components that do not use the uimaFIT {@link ExternalResource} annotation, because no external
   * resource dependencies can be automatically generated by uimaFIT for such components.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aImpl
   *          the resource implementation.
   * @param aUrl
   *          the resource URL.
   * @param aParams
   *          additional parameters supported by the resource.
   * @throws InvalidXMLException
   *           if import resolution failed
   */
  public static void createDependencyAndBindUsingUrl(AnalysisEngineDescription aDesc, String aKey,
          Class<? extends SharedResourceObject> aImpl, String aUrl, Object... aParams)
          throws InvalidXMLException {
    if (aDesc.getExternalResourceDependency(aKey) == null) {
      ExternalResourceDependency[] deps = aDesc.getExternalResourceDependencies();
      if (deps == null) {
        deps = new ExternalResourceDependency[] {};
      }
      aDesc.setExternalResourceDependencies(ArrayUtils.add(deps,
              createResourceDependency(aKey, aImpl, false, null)));
    }
    bindResourceUsingUrl(aDesc, aKey, aImpl, aUrl, aParams);
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * a compatible type is found, the given resource is bound to it.
   * 
   * @param aDesc
   *          a description.
   * @param aResDesc
   *          the resource description.
   */
  private static void scanRecursivelyForDependenciesByInterfaceAndBind(
          AnalysisEngineDescription aDesc, ExternalResourceDescription aResDesc)
          throws InvalidXMLException, ClassNotFoundException {
    // Recursively address delegates
    if (!aDesc.isPrimitive()) {
      for (ResourceSpecifier delegate : aDesc.getDelegateAnalysisEngineSpecifiers().values()) {
        bindResource(delegate, aResDesc);
      }
    }

    // Bind if necessary
    Class<?> resClass = Class.forName(getImplementationName(aResDesc));
    for (ExternalResourceDependency dep : aDesc.getExternalResourceDependencies()) {
      Class<?> apiClass = Class.forName(dep.getInterfaceName());

      // Never bind fields of type Object. See also ExternalResourceInitializer#getApi()
      if (apiClass.equals(Object.class)) {
        continue;
      }

      if (apiClass.isAssignableFrom(resClass)) {
        bindResourceOnce(aDesc, dep.getKey(), aResDesc);
      }
    }
  }

  /**
   * Scan the given resource specifier for external resource dependencies and whenever a dependency
   * with the given key is encountered, the given resource is bound to it.
   * 
   * @param aDesc
   *          a description.
   * @param aKey
   *          the key to bind to.
   * @param aResDesc
   *          the resource description.
   */
  private static void scanRecursivelyForDependenciesByKeyAndBind(AnalysisEngineDescription aDesc,
          String aKey, ExternalResourceDescription aResDesc) throws InvalidXMLException {
    // Recursively address delegates
    if (!aDesc.isPrimitive()) {
      for (ResourceSpecifier delegate : aDesc.getDelegateAnalysisEngineSpecifiers().values()) {
        bindResource(delegate, aKey, aResDesc);
      }
    }

    // Bind if necessary
    for (ExternalResourceDependency dep : aDesc.getExternalResourceDependencies()) {
      if (aKey.equals(dep.getKey())) {
        bindResourceOnce(aDesc, aKey, aResDesc);
      }
    }
  }

  /**
   * Create a binding for the given external resource in the given resource manager. This method
   * also scans the given external resource for any nested external resources and creates
   * bindings for them as well.
   * 
   * @param aResMgrCfg
   *          the resource manager to create the binding in.
   * @param aBindTo
   *          what key to bind to.
   * @param aRes
   *          the resource that should be bound.
   * @deprecated Use {@link #bindResourceOnce(ResourceManagerConfiguration, String, ExternalResourceDescription)}
   */
  @Deprecated
  public static void bindResource(ResourceManagerConfiguration aResMgrCfg, String aBindTo,
          ExternalResourceDescription aRes) {
    bindResourceOnce(aResMgrCfg, aBindTo, aRes);
  }
  
  /**
   * Create a binding for the given external resource in the given resource manager. This method
   * also scans the given external resource for any nested external resources and creates
   * bindings for them as well.
   * <p>
   * <b>NOTE:</b>If you use this method on resource manager configurations of aggregate analysis
   * engine descriptions because it will <b>not have any effects on the delegate analysis
   * engines</b> of the aggregate. If you want to recursively bind an external resource to the
   * delegates in an aggregate engine, use e.g.
   * {@link #bindResource(ResourceSpecifier, String, ExternalResourceDescription)}.
   * 
   * @param aResMgrCfg
   *          the resource manager to create the binding in.
   * @param aBindTo
   *          what key to bind to.
   * @param aRes
   *          the resource that should be bound.
   */
  public static void bindResourceOnce(ResourceManagerConfiguration aResMgrCfg, String aBindTo,
          ExternalResourceDescription aRes) {
    // Create a map of all bindings
    Map<String, ExternalResourceBinding> bindings = new HashMap<>();
    for (ExternalResourceBinding b : aResMgrCfg.getExternalResourceBindings()) {
      bindings.put(b.getKey(), b);
    }

    // Create a map of all resources
    Map<String, ExternalResourceDescription> resources = new HashMap<>();
    for (ExternalResourceDescription r : aResMgrCfg.getExternalResources()) {
      resources.put(r.getName(), r);
    }

    // For the current resource, add resource and binding
    ExternalResourceBinding extResBind = createResourceBinding(aBindTo, aRes);
    bindings.put(extResBind.getKey(), extResBind);
    resources.put(aRes.getName(), aRes);

    // Handle nested resources
    bindNestedResources(aRes, bindings, resources);

    // Commit everything to the resource manager configuration
    aResMgrCfg.setExternalResourceBindings(bindings.values().toArray(
            new ExternalResourceBinding[bindings.size()]));
    aResMgrCfg.setExternalResources(resources.values().toArray(
            new ExternalResourceDescription[resources.size()]));
  }

  /**
   * Create a new external resource binding.
   * 
   * @param aRes
   *          the resource to bind to
   * @param aBindTo
   *          what key to bind to.
   * @param aNestedRes
   *          the resource that should be bound.
   * @deprecated Use {@link #bindResourceOnce(ExternalResourceDescription, String, ExternalResourceDescription)}
   */
  @Deprecated
  public static void bindResource(ExternalResourceDescription aRes, String aBindTo,
          ExternalResourceDescription aNestedRes) {
    bindResourceOnce(aRes, aBindTo, aNestedRes);
  }
  
  /**
   * Create a binding for the given external resource in the given resource. This method also scans
   * the given external resource for any nested external resources and creates bindings for them as
   * well.
   * <p>
   * <b>NOTE:</b> This method only works on {@link ExtendedExternalResourceDescription_impl}
   * instances. Any {@link ExternalResourceDescription} instances created with uimaFIT use this
   * implementation. For reasons of convenience, the method signature uses
   * {@link ExternalResourceDescription} but will thrown an {@link IllegalArgumentException} if the
   * wrong implementations are provided.
   * 
   * @param aRes
   *          the resource to bind to
   * @param aBindTo
   *          what key to bind to.
   * @param aNestedRes
   *          the resource that should be bound.
   * @throws IllegalArgumentException
   *           if the given resource description is not an instance of
   *           {@link ExtendedExternalResourceDescription_impl}.
   */
  public static void bindResourceOnce(ExternalResourceDescription aRes, String aBindTo,
          ExternalResourceDescription aNestedRes) {
    if (!(aRes instanceof ExtendedExternalResourceDescription_impl)) {
      throw new IllegalArgumentException("Nested resources are only supported on instances of ["
              + ExtendedExternalResourceDescription_impl.class.getName() + "] which"
              + "can be created with uimaFIT's createExternalResourceDescription() methods.");
    }

    ExtendedExternalResourceDescription_impl extRes = (ExtendedExternalResourceDescription_impl) aRes;

    // Create a map of all bindings
    Map<String, ExternalResourceBinding> bindings = new HashMap<String, ExternalResourceBinding>();
    for (ExternalResourceBinding b : extRes.getExternalResourceBindings()) {
      bindings.put(b.getKey(), b);
    }

    // Create a map of all resources
    Map<String, ExternalResourceDescription> resources = new HashMap<String, ExternalResourceDescription>();
    for (ExternalResourceDescription r : extRes.getExternalResources()) {
      resources.put(r.getName(), r);
    }

    // For the current resource, add resource and binding
    ExternalResourceBinding extResBind = createResourceBinding(aBindTo, aNestedRes);
    bindings.put(extResBind.getKey(), extResBind);
    resources.put(aNestedRes.getName(), aNestedRes);

    // Handle nested resources
    bindNestedResources(aRes, bindings, resources);

    // Commit everything to the resource manager configuration
    extRes.setExternalResourceBindings(bindings.values());
    extRes.setExternalResources(resources.values());
  }

  /**
   * Helper method to recursively bind resources bound to other resources (a.k.a. nested resources).
   * 
   * @param aRes
   *          resource.
   * @param aBindings
   *          bindings already made.
   * @param aResources
   *          resources already bound.
   */
  private static void bindNestedResources(ExternalResourceDescription aRes,
          Map<String, ExternalResourceBinding> aBindings,
          Map<String, ExternalResourceDescription> aResources) {
    // Handle nested resources
    if (aRes instanceof ExtendedExternalResourceDescription_impl) {
      ExtendedExternalResourceDescription_impl extRes = (ExtendedExternalResourceDescription_impl) aRes;

      // Tell the external resource its name. This is needed in order to find the resources
      // bound to this resource later on. Set only if the resource supports this parameter.
      // Mind that supporting this parameter is mandatory for resource implementing
      // ExternalResourceAware.
      if (canParameterBeSet(extRes.getResourceSpecifier(), PARAM_RESOURCE_NAME)) {
        ConfigurationParameterFactory.setParameter(extRes.getResourceSpecifier(),
                PARAM_RESOURCE_NAME, aRes.getName());
      }

      // Create a map of all resources
      Map<String, ExternalResourceDescription> res = new HashMap<String, ExternalResourceDescription>();
      for (ExternalResourceDescription r : extRes.getExternalResources()) {
        res.put(r.getName(), r);
      }

      // Bind nested resources
      for (ExternalResourceBinding b : extRes.getExternalResourceBindings()) {
        // Avoid re-prefixing the resource name
        String key = b.getKey();
        if (!key.startsWith(aRes.getName() + PREFIX_SEPARATOR)) {
          key = aRes.getName() + PREFIX_SEPARATOR + b.getKey();
        }
        // Avoid unnecessary binding and an infinite loop when a resource binds to itself
        if (!aBindings.containsKey(key)) {
          // Mark the current binding as processed so we do not recurse
          aBindings.put(key, b);
          ExternalResourceDescription nestedRes = res.get(b.getResourceName());
          aResources.put(nestedRes.getName(), nestedRes);
          bindNestedResources(nestedRes, aBindings, aResources);
          // Set the proper key on the binding.
          b.setKey(key);
        }
      }
    }
  }

  /**
   * Create a binding for the given external resource in the resource manager configuration of the
   * given resource. If no resource manager configuration exists yet, it will be created. This
   * method also scans the given external resource for any nested external resources and creates
   * bindings for them as well.
   * <p>
   * <b>NOTE:</b>IF you use this method with aggregate analysis engine descriptions because it will
   * <b>not have any effects on the delegate analysis engines</b> of the aggregate. If you want to
   * recursively bind an external resource to the delegates in an aggregate engine, use e.g.
   * {@link #bindResource(ResourceSpecifier, String, ExternalResourceDescription)}.
   * 
   * @param aDesc
   *          the specifier to create the binding in.
   * @param aBindTo
   *          what key to bind to.
   * @param aRes
   *          the resource that should be bound.
   */
  public static void bindResourceOnce(ResourceCreationSpecifier aDesc, String aBindTo,
          ExternalResourceDescription aRes) {
    ResourceManagerConfiguration resMgrCfg = aDesc.getResourceManagerConfiguration();
    if (resMgrCfg == null) {
      resMgrCfg = new ResourceManagerConfiguration_impl();
      aDesc.setResourceManagerConfiguration(resMgrCfg);
    }

    bindResourceOnce(resMgrCfg, aBindTo, aRes);
  }

  /**
   * Create a binding for the given external resource in the given resource manager configuration.
   * This method <b>does not</b> scan the given external resource for any nested external resources
   * and <b>does not</b> create bindings for them. Use
   * {@link #bindResourceOnce(ResourceCreationSpecifier, String, ExternalResourceDescription)} if
   * you wish to bind nested resources as well.
   * <p>
   * <b>NOTE:</b>If you use this method on resource manager configurations of aggregate analysis
   * engine descriptions because it will <b>not have any effects on the delegate analysis
   * engines</b> of the aggregate. If you want to recursively bind an external resource to the
   * delegates in an aggregate engine, use e.g.
   * {@link #bindResource(ResourceSpecifier, String, ExternalResourceDescription)}.
   * 
   * @param aResMgrCfg
   *          the resource manager to create the binding in.
   * @param aBindTo
   *          what key to bind to.
   * @param aRes
   *          the resource that should be bound.
   * @deprecated Use
   *             {@link #bindResourceOnceWithoutNested(ResourceManagerConfiguration, String, String)}.
   */
  @Deprecated
  public static void bindResource(ResourceManagerConfiguration aResMgrCfg, String aBindTo,
          String aRes) {
    bindResourceOnceWithoutNested(aResMgrCfg, aBindTo, aRes);
  }

  /**
   * Create a binding for the given external resource in the given resource manager configuration.
   * This method <b>does not</b> scan the given external resource for any nested external resources
   * and <b>does not</b> create bindings for them. Use
   * {@link #bindResourceOnce(ResourceCreationSpecifier, String, ExternalResourceDescription)} if
   * you wish to bind nested resources as well.
   * <p>
   * <b>NOTE:</b>If you use this method on resource manager configurations of aggregate analysis
   * engine descriptions because it will <b>not have any effects on the delegate analysis
   * engines</b> of the aggregate. If you want to recursively bind an external resource to the
   * delegates in an aggregate engine, use e.g.
   * {@link #bindResource(ResourceSpecifier, String, ExternalResourceDescription)}.
   * 
   * @param aResMgrCfg
   *          the resource manager to create the binding in.
   * @param aBindTo
   *          what key to bind to.
   * @param aRes
   *          the resource that should be bound.
   */
  public static void bindResourceOnceWithoutNested(ResourceManagerConfiguration aResMgrCfg,
          String aBindTo, String aRes) {
    ExternalResourceBinding extResBind = createResourceBinding(aBindTo, aRes);
    aResMgrCfg.addExternalResourceBinding(extResBind);
  }
  
  
  /**
   * Create a binding for the given external resource in resource manager configuration of the given
   * resource creation specified. If no resource manager configuration exists yet, it is created.
   * This method <b>does not</b> scan the given external resource for any nested external resources
   * and <b>does not</b> create bindings for them. Use
   * {@link #bindResourceOnce(ResourceCreationSpecifier, String, ExternalResourceDescription)} if
   * you wish to bind nested resources as well.
   * <p>
   * <b>NOTE:</b>If you use this method on an aggregate analysis engine description, it will <b>not
   * have any effects on the delegate analysis engines</b> of the aggregate. If you want to
   * recursively bind an external resource to the delegates in an aggregate engine, use e.g.
   * {@link #bindResource(ResourceSpecifier, String, ExternalResourceDescription)}.
   * 
   * @param aDesc
   *          the specifier to create the binding in.
   * @param aBindTo
   *          what key to bind to.
   * @param aRes
   *          the resource that should be bound.
   */
  public static void bindResourceOnceWithoutNested(ResourceCreationSpecifier aDesc, String aBindTo, String aRes) {
    ResourceManagerConfiguration resMgrCfg = aDesc.getResourceManagerConfiguration();
    if (resMgrCfg == null) {
      resMgrCfg = new ResourceManagerConfiguration_impl();
      aDesc.setResourceManagerConfiguration(resMgrCfg);
    }

    bindResourceOnceWithoutNested(resMgrCfg, aBindTo, aRes);
  }

  static String uniqueResourceKey(String aKey) {
    return aKey + '-' + identityHashCode(DISAMBIGUATOR) + '-' + DISAMBIGUATOR.getAndIncrement();
  }

  /**
   * Find the name of the class implementing this resource. The location where this name is stored
   * varies, depending if the resource extends {@link SharedResourceObject} or implements
   * {@link Resource}.
   * 
   * @param aDesc
   *          the external resource description.
   * @return the implementation name.
   */
  protected static String getImplementationName(ExternalResourceDescription aDesc) {
    if (aDesc.getResourceSpecifier() instanceof CustomResourceSpecifier) {
      return ((CustomResourceSpecifier) aDesc.getResourceSpecifier()).getResourceClassName();
    } else {
      return aDesc.getImplementationName();
    }
  }

  /**
   * Extracts the external resource from the configuration parameters and nulls out these
   * parameters. Mind that the array passed to this method is modified by the method.
   * 
   * @param configurationData
   *          the configuration parameters.
   * @return extRes the external resource parameters.
   */
  protected static Map<String, ExternalResourceDescription> extractResourceParameters(
          final Object[] configurationData) {
    if (configurationData == null) {
      return Collections.emptyMap();
    }

    Map<String, ExternalResourceDescription> extRes = new HashMap<String, ExternalResourceDescription>();
    for (int i = 0; i < configurationData.length - 1; i += 2) {
      String key = (String) configurationData[i];
      Object value = configurationData[i + 1];

      if (value == null) {
        continue;
      }
      
      // Store External Resource parameters separately
      ResourceValueType type = getResourceParameterType(value);
      if (type == ResourceValueType.PRIMITIVE) {
        ExternalResourceDescription description = (ExternalResourceDescription) value;
        extRes.put(key, description);
      }
      else if (type.isMultiValued()) {
        Collection<ExternalResourceDescription> resList;
        if (type == ResourceValueType.ARRAY) {
          resList = asList((ExternalResourceDescription[]) value);
        }
        else {
          resList = (Collection<ExternalResourceDescription>) value;
        }

        // Record the list elements
        List<Object> params = new ArrayList<Object>();
        params.add(ResourceList.PARAM_SIZE);
        params.add(String.valueOf(resList.size())); // "Resource" only supports String parameters!
        int n = 0;
        for (ExternalResourceDescription res : resList) {
          params.add(ResourceList.ELEMENT_KEY + "[" + n + "]");
          params.add(res);
          n++;
        }
        
        // Record the list and attach the list elements to the list
        extRes.put(key, createResourceDescription(ResourceList.class, params.toArray()));
      }
    }

    return extRes;
  }
  
  /**
   * Determine which kind of external resource the given value is. This is only meant for
   * uimaFIT internal use. This method is required by the ConfigurationParameterFactory, so it is
   * package private instead of private.
   */
  static ResourceValueType getResourceParameterType(Object aValue) {
    if (aValue == null) {
      return ResourceValueType.NO_RESOURCE;
    }
    
    boolean isResourcePrimitive = aValue instanceof ExternalResourceDescription;
    boolean isResourceArray = aValue.getClass().isArray()
            && ExternalResourceDescription.class.isAssignableFrom(aValue.getClass()
                    .getComponentType());
    boolean isResourceCollection = (Collection.class.isAssignableFrom(aValue
            .getClass()) && !((Collection) aValue).isEmpty() && ((Collection) aValue)
            .iterator().next() instanceof ExternalResourceDescription);
    if (isResourcePrimitive) {
      return ResourceValueType.PRIMITIVE;
    } else if (isResourceArray) {
      return ResourceValueType.ARRAY;
    } else if (isResourceCollection) {
      return ResourceValueType.COLLECTION;
    } else {
      return ResourceValueType.NO_RESOURCE;
    }
  }
  
  /**
   * Create an instance of a UIMA shared/external resource class.
   * 
   * @param <R>
   *          the resource type.
   * @param resourceClass
   *          the class implementing the resource.
   * @param params
   *          parameters passed to the resource when it is created. Each parameter consists of two
   *          arguments, the first being the name and the second being the parameter value
   * @return the resource instance.
   * @throws ResourceInitializationException
   *           if there was a problem instantiating the resource.
   */
  public static <R extends Resource> R createResource(Class<R> resourceClass,
          Object... params) throws ResourceInitializationException {
    return createResource(resourceClass, null, params);
  }

  /**
   * Create an instance of a UIMA shared/external resource class.
   * 
   * @param <R>
   *          the resource type.
   * @param resourceClass
   *          the class implementing the resource.
   * @param resMgr
   *          a resource manager (optional).
   * @param params
   *          parameters passed to the resource when it is created. Each parameter consists of two
   *          arguments, the first being the name and the second being the parameter value
   * @return the resource instance.
   * @throws ResourceInitializationException
   *           if there was a problem instantiating the resource.
   */
  @SuppressWarnings("unchecked")
  public static <R extends Resource> R createResource(Class<R> resourceClass,
          ResourceManager resMgr, Object... params) throws ResourceInitializationException {
    ExternalResourceDescription res = createResourceDescription(resourceClass, params);
    return (R) produceResource(resourceClass, res.getResourceSpecifier(), resMgr, emptyMap());
  }
  
  /**
   * Types of external resource values.
   */
  static enum ResourceValueType {
    NO_RESOURCE,
    PRIMITIVE,
    ARRAY,
    COLLECTION;
    
    public boolean isMultiValued()
    {
      return this == COLLECTION || this == ARRAY;
    }
  }
}
