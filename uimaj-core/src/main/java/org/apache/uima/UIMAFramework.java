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

package org.apache.uima;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineManagement.State;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.analysis_engine.impl.AnalysisEngineManagementImpl;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasInitializer;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionProcessingManager;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Logger;
import org.apache.uima.util.UimaTimer;
import org.apache.uima.util.XMLParser;

/**
 * This is an application's main interface point to the UIMA Framework. Static methods on this class
 * allow the application to create instances of UIMA components.
 * <p>
 * This class also provides the ability to change the underlying UIMA implementation. All UIMA
 * implementations must provide a subclass of <code>UIMAFramework</code> as part of their
 * distribution. If you wish to use a UIMA implementation other than the default reference
 * implementation, set the System property <code>uima.framework_impl</code> to the fully qualified
 * class name of the <code>UIMAFramework</code> subclass that you wish to use. Note that this must
 * be done prior to loading this class. If the <code>uima.framework_impl</code> property has not
 * been set when this class is loaded, the default reference implementation will be used.
 * 
 * 
 */
public abstract class UIMAFramework {
  /**
   * Key to be used in the Properties object returned by
   * {@link #getDefaultPerformanceTuningProperties()}. The value of this key represents the size of
   * the initial CAS heap in terms of the number of cells (32 bits per cell).
   */
  public static final String CAS_INITIAL_HEAP_SIZE = "cas_initial_heap_size";

  /**
   * Key to be used in the Properties object returned by
   * {@link #getDefaultPerformanceTuningProperties()}. The value of this key indicates whether the
   * ProcessTrace mechanism (which tracks the time spent in individual components of an aggregate AE
   * or CPE) is enabled. A value of "true" (case insensitive) enables ProcessTrace; any other value
   * disables process trace.
   */
  public static final String PROCESS_TRACE_ENABLED = "process_trace_enabled";

  /**
   * Key to be used in the Properties object returned by
   * {@link #getDefaultPerformanceTuningProperties()}. The value of this key indicates whether
   * socket KeepAlive should be turned on (currently implemented only for Vinci clients).  The
   * default is true.  A value of "false" (case insensitive) for this property disables the keepAlive; 
   * any other value leaves the default setting of true.
   * @see java.net.Socket#setKeepAlive(boolean)
   */
  public static final Object SOCKET_KEEPALIVE_ENABLED = "socket_keepalive_enabled";

  /**
   * Key to be used in the Properties object returned by
   * {@link #getDefaultPerformanceTuningProperties()}. The value of this key indicates whether the
   * JCas object cache should be used (significant memory overhead, but may have performance
   * benefits). The default is true. A value of "false" (case insensitive) for this property
   * disables the cache; any other value leaves the default setting of true.
   */
  public static final String JCAS_CACHE_ENABLED = "jcas_cache_enabled";

  /**
   * To be implemented by subclasses; this should return a Properties object representing the
   * default performance tuning settings for the framework. It must return a new Properties object
   * each time it is called.
   * 
   * @return default performance tuning properties
   */
  protected abstract Properties _getDefaultPerformanceTuningProperties();

  /**
   * The singleton instance of <code>UIMAFramework</code> used by this application. The value of
   * this field is determined by the <code>uima.framework_impl</code> System property. During this
   * class's static initializer, a new instance of whatever class is named by this property will be
   * created. If no value for this property is set, the default reference implementation ({@link org.apache.uima.impl.UIMAFramework_impl})
   * will be used.
   */
  private static final UIMAFramework mInstance;

  /**
   * The name of the <code>UIMAFramework</code> subclass for the UIMA reference implementation. An
   * instance of this class will be created if the application does not supply a custom framework
   * implementation.
   */
  private static final String REF_IMPL_CLASS_NAME = "org.apache.uima.impl.UIMAFramework_impl";

  /**
   * Gets the framework implementation's version number as a string. This will be the major version
   * number, minor version number, and build revision in that order, separated by dots.
   * 
   * @return the version number string
   */
  public static String getVersionString() {
    return "" + getMajorVersion() + "." + getMinorVersion() + "." + getBuildRevision();
  }

  /**
   * Gets the major version number of the framework implementation.
   * 
   * @return the major version number
   */
  public static short getMajorVersion() {
    return getInstance()._getMajorVersion();
  }

  /**
   * Gets the minor version number of the framework implementation.
   * 
   * @return the minor version number
   */
  public static short getMinorVersion() {
    return getInstance()._getMinorVersion();
  }

  /**
   * Gets the build revision number of the framework implementation.
   * 
   * @return the build revision number
   */
  public static short getBuildRevision() {
    return getInstance()._getBuildRevision();
  }

  /**
   * Get a reference to the <code>ResourceFactory</code>. Most applications do not need to deal
   * with the <code>ResourceFactory</code> - instead one of the static <code>produce</code>
   * methods on this class may be used to create Resources.
   * <p>
   * The framework's Resource Factory always implements {@link CompositeResourceFactory}. A
   * composite resource factory produces resources by delegating to other {@link ResourceFactory}
   * objects. Developers to register their own specialized <code>ResourceFactory</code> objects by
   * calling the {@link CompositeResourceFactory#registerFactory(Class,ResourceFactory)} method.
   * 
   * @return the <code>ResourceFactory</code> to be used by the application.
   */
  public static CompositeResourceFactory getResourceFactory() {
    return getInstance()._getResourceFactory();
  }

  /**
   * Get a reference to the {@link ResourceSpecifierFactory}. This factory is used when
   * constructing {@link ResourceSpecifier}s from scratch.
   * <p>
   * 
   * @return the <code>ResourceSpecifierFactory</code> to be used by the application.
   */
  public static ResourceSpecifierFactory getResourceSpecifierFactory() {
    return getInstance()._getResourceSpecifierFactory();
  }

  /**
   * Get a reference to the UIMA {@link XMLParser}, which is used to parse
   * {@link ResourceSpecifier} objects from their XML representations.
   * 
   * @return the <code>XMLParser</code> to be used by the application.
   */
  public static XMLParser getXMLParser() {
    return getInstance()._getXMLParser();
  }

  /**
   * Creates a new {@link CollectionProcessingManager} instance. The
   * <code>CollectionProcessingManager</code> facilitates the development of applications that
   * process collections of entities using an {@link AnalysisEngine}.
   * 
   * @return a new <code>CollectionProcessingManager</code> instance to be used by the
   *         application.
   */
  public static CollectionProcessingManager newCollectionProcessingManager() {
    return getInstance()._newCollectionProcessingManager(null);
  }

  /**
   * Creates a new {@link CollectionProcessingManager} instance. The
   * <code>CollectionProcessingManager</code> facilitates the development of applications that
   * process collections of entities using an {@link AnalysisEngine}.
   * 
   * @param aResourceManager
   *          the <code>ResourceManager</code> to be used by this CPM. If not specified, the
   *          default one returned by {@link #newDefaultResourceManager()} will be used.
   * 
   * @return a new <code>CollectionProcessingManager</code> instance to be used by the
   *         application.
   */
  public static CollectionProcessingManager newCollectionProcessingManager(
          ResourceManager aResourceManager) {
    return getInstance()._newCollectionProcessingManager(aResourceManager);
  }

  /**
   * Produces an appropriate <code>Resource</code> instance from a <code>ResourceSpecifier</code>.
   * The <code>ResourceSpecifier</code> may either specify how to construct a new instance or how
   * to locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>Resource</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static Resource produceResource(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    return produceResource(Resource.class, aSpecifier, aAdditionalParams);
  }

  /**
   * Produces an appropriate <code>Resource</code> instance of a specified class from a
   * <code>ResourceSpecifier</code>. The <code>ResourceSpecifier</code> may either specify how
   * to construct a new instance or how to locate an existing instance.
   * 
   * @param aResourceClass
   *          a subclass of <code>Resource</code> to be produced.
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>Resource</code> instance. This will be a subclass of
   *         <code>aResourceClass</code>.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    Resource resource = getResourceFactory().produceResource(aResourceClass, aSpecifier,
            aAdditionalParams);
    if (resource == null) {
      throw new ResourceInitializationException(ResourceInitializationException.DO_NOT_KNOW_HOW,
              new Object[] { aResourceClass.getName(), aSpecifier.getSourceUrlString() });
    }
    return resource;
  }

  /**
   * Produces an appropriate <code>Resource</code> instance of a specified class from a
   * <code>ResourceSpecifier</code>. The <code>ResourceSpecifier</code> may either specify how
   * to construct a new instance or how to locate an existing instance.
   * 
   * @param aResourceClass
   *          a subclass of <code>Resource</code> to be produced.
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   * @param aResourceManager
   *          the <code>ResourceManager</code> to be used by this analysis engine. If not
   *          specified, the default one returned by {@link #newDefaultResourceManager()} will be
   *          used.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>Resource</code> instance. This will be a subclass of
   *         <code>aResourceClass</code>.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
          ResourceManager aResourceManager, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    // add ResourceManager to aAdditionalParams map
    if (aResourceManager != null) {
      if (aAdditionalParams == null) {
        aAdditionalParams = new HashMap<String, Object>();
      } else {  // copy to avoid modifying the original which might be immutable
        aAdditionalParams = new HashMap<String, Object>(aAdditionalParams);
      }
      aAdditionalParams.put(Resource.PARAM_RESOURCE_MANAGER, aResourceManager);
    }

    return produceResource(aResourceClass, aSpecifier, aAdditionalParams);
  }
  
  /**
   * Called if AE initialization succeeds. Sets the AE state to Ready and time
   * it took to initialize the AE. 
   * 
   * @param ae
   * @param initStartTime
   */
  private static void updateAeState(AnalysisEngine ae, long initStartTime) {
	  if ( ae.getManagementInterface() instanceof AnalysisEngineManagementImpl) {
	      ((AnalysisEngineManagementImpl)ae.getManagementInterface()).setState(State.Ready);
	      ((AnalysisEngineManagementImpl)ae.getManagementInterface()).
	        setInitializationTime( System.currentTimeMillis() - initStartTime);
	    }
  }
  /**
   * Produces an {@link AnalysisEngine} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * <p>
   * The AnalysisEngine returned from this method is not guaranteed to be able to process multiple
   * simultaneous requests. See {@link #produceAnalysisEngine(ResourceSpecifier,int,int)} for more
   * information.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>AnalysisEngine</code>.
   * 
   * @return an <code>AnalysisEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngine produceAnalysisEngine(ResourceSpecifier aSpecifier)
          throws ResourceInitializationException {
	    AnalysisEngine ae = null;
	    //	Fetch current time to compute initialization time
	    long initStartTime = System.currentTimeMillis();
	    ae = (AnalysisEngine) produceResource(AnalysisEngine.class, aSpecifier, null);
	    //	initialization succeeded, update AE state and initialization time
	    updateAeState(ae,initStartTime);
	    return ae;

  }

  /**
   * Produces an {@link AnalysisEngine} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>AnalysisEngine</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return an <code>AnalysisEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngine produceAnalysisEngine(ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
	    AnalysisEngine ae = null;
	    //	Fetch current time to compute initialization time
	    long initStartTime = System.currentTimeMillis();
	    ae = (AnalysisEngine) produceResource(AnalysisEngine.class, aSpecifier, aAdditionalParams);
	    //	initialization succeeded, update AE state and initialization time
	    updateAeState(ae,initStartTime);
	    return ae;

  }

  /**
   * Produces an {@link AnalysisEngine} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>AnalysisEngine</code>.
   * @param aResourceManager
   *          the <code>ResourceManager</code> to be used by this analysis engine. If not
   *          specified, the default one returned by {@link #newDefaultResourceManager()} will be
   *          used.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return an <code>AnalysisEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngine produceAnalysisEngine(ResourceSpecifier aSpecifier,
          ResourceManager aResourceManager, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    AnalysisEngine ae = null;
    //	Fetch current time to compute initialization time
    long initStartTime = System.currentTimeMillis();
    ae = (AnalysisEngine) produceResource(AnalysisEngine.class, aSpecifier, aResourceManager,
            aAdditionalParams);
    //	initialization succeeded, update AE state and initialization time
    updateAeState(ae,initStartTime);
    return ae;
  }

  /**
   * Produces an {@link AnalysisEngine} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * <p>
   * This version of <code>produceAnalysisEngine</code> allows the convenient creation of
   * AnalysisEngines that can handle multiple simultaneous requests. Using this method is equivalent
   * to using {@link #produceAnalysisEngine(ResourceSpecifier,Map)} and including values for
   * {@link AnalysisEngine#PARAM_NUM_SIMULTANEOUS_REQUESTS} and
   * {@link AnalysisEngine#PARAM_TIMEOUT_PERIOD} in the parameter map.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>AnalysisEngine</code>.
   * @param aMaxSimultaneousRequests
   *          the number of simultaneous requests that this AnalysisEngine should be able to
   *          process. The value for this parameter should be chosen careful - see the JavaDocs for
   *          {@link AnalysisEngine#PARAM_NUM_SIMULTANEOUS_REQUESTS}.
   * @param aTimeoutPeriod -
   *          when the number of simultaneous requests exceeds <code>aMaxSimultaneousReqeusts</code>,
   *          additional requests will wait for other requests to finish. This parameter determines
   *          the maximum number of milliseconds that a new request should wait before throwing an
   *          exception - a value of 0 will cause them to wait forever. See the JavaDocs for
   *          {@link AnalysisEngine#PARAM_TIMEOUT_PERIOD}.
   * 
   * @return an <code>AnalysisEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static AnalysisEngine produceAnalysisEngine(ResourceSpecifier aSpecifier,
          int aMaxSimultaneousRequests, int aTimeoutPeriod) throws ResourceInitializationException {
    // add parameters to the aAdditionalParams map
    Map<String, Object> aAdditionalParams = new HashMap<String, Object>();

    aAdditionalParams.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, Integer.valueOf(
            aMaxSimultaneousRequests));
    aAdditionalParams.put(AnalysisEngine.PARAM_TIMEOUT_PERIOD, Integer.valueOf(aTimeoutPeriod));

    AnalysisEngine ae = null;
    //	Fetch current time to compute initialization time
    long initStartTime = System.currentTimeMillis();

    ae = (AnalysisEngine) produceResource(AnalysisEngine.class, aSpecifier, aAdditionalParams);
    //	initialization succeeded, update AE state and initialization time
    updateAeState(ae,initStartTime);
    return ae;
  }

  /**
   * Produces a {@link TextAnalysisEngine} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * <p>
   * The TextAnalysisEngine returned from this method is not guaranteed to be able to process
   * multiple simultaneous requests. See {@link #produceTAE(ResourceSpecifier,int,int)} for more
   * information.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>TextAnalysisEngine</code>.
   * 
   * @return a <code>TextAnalysisEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * 
   * @deprecated As of v2.0, {@link #produceAnalysisEngine(ResourceSpecifier)} should be used
   *             instead.
   */
  @Deprecated
  public static TextAnalysisEngine produceTAE(ResourceSpecifier aSpecifier)
          throws ResourceInitializationException {
    return (TextAnalysisEngine) produceResource(TextAnalysisEngine.class, aSpecifier, null);
  }

  /**
   * Produces a {@link TextAnalysisEngine} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>TextAnalysisEngine</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>TextAnalysisEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * 
   * @deprecated As of v2.0, {@link #produceAnalysisEngine(ResourceSpecifier,Map)} should be used
   *             instead.
   */
  @Deprecated
  public static TextAnalysisEngine produceTAE(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    return (TextAnalysisEngine) produceResource(TextAnalysisEngine.class, aSpecifier,
            aAdditionalParams);
  }

  /**
   * Produces an {@link TextAnalysisEngine} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>TextAnalysisEngine</code>.
   * @param aResourceManager
   *          the <code>ResourceManager</code> to be used by this analysis engine. If not
   *          specified, the default one returned by {@link #newDefaultResourceManager()} will be
   *          used.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>TextAnalysisEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * 
   * @deprecated As of v2.0, {@link #produceAnalysisEngine(ResourceSpecifier,ResourceManager,Map)}
   *             should be used instead.
   */
  @Deprecated
  public static TextAnalysisEngine produceTAE(ResourceSpecifier aSpecifier,
          ResourceManager aResourceManager, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    return (TextAnalysisEngine) produceResource(TextAnalysisEngine.class, aSpecifier,
            aResourceManager, aAdditionalParams);
  }

  /**
   * Produces a {@link TextAnalysisEngine} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * <p>
   * This version of <code>produceTAE</code> allows the convenient creation of TAEs that can
   * handle multiple simultaneous requests. Using this method is equivalent to using
   * {@link #produceTAE(ResourceSpecifier,Map)} and including values for
   * {@link AnalysisEngine#PARAM_NUM_SIMULTANEOUS_REQUESTS} and
   * {@link AnalysisEngine#PARAM_TIMEOUT_PERIOD} in the parameter map.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>TextAnalysisEngine</code>.
   * @param aMaxSimultaneousRequests
   *          the number of simultaneous requests that this TAE should be able to process. The value
   *          for this parameter should be chosen careful - see the JavaDocs for
   *          {@link AnalysisEngine#PARAM_NUM_SIMULTANEOUS_REQUESTS}.
   * @param aTimeoutPeriod -
   *          when the number of simultaneous requests exceeds <code>aMaxSimultaneousReqeusts</code>,
   *          additional requests will wait for other requests to finish. This parameter determines
   *          the maximum number of milliseconds that a new request should wait before throwing an
   *          exception - a value of 0 will cause them to wait forever. See the JavaDocs for
   *          {@link AnalysisEngine#PARAM_TIMEOUT_PERIOD}.
   * 
   * @return a <code>TextAnalysisEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * 
   * @deprecated As of v2.0, {@link #produceAnalysisEngine(ResourceSpecifier,int,int)} should be
   *             used instead.
   */
  @Deprecated
  public static TextAnalysisEngine produceTAE(ResourceSpecifier aSpecifier,
          int aMaxSimultaneousRequests, int aTimeoutPeriod) throws ResourceInitializationException {
    // add parameters to the aAdditionalParams map
    Map<String, Object> aAdditionalParams = new HashMap<String, Object>();

    aAdditionalParams.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, Integer.valueOf(
            aMaxSimultaneousRequests));
    aAdditionalParams.put(AnalysisEngine.PARAM_TIMEOUT_PERIOD, Integer.valueOf(aTimeoutPeriod));

    return (TextAnalysisEngine) produceResource(TextAnalysisEngine.class, aSpecifier,
            aAdditionalParams);
  }

  /**
   * Produces a {@link CasConsumer} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CasConsumer</code>.
   * 
   * @return a <code>CasConsumer</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static CasConsumer produceCasConsumer(ResourceSpecifier aSpecifier)
          throws ResourceInitializationException {
    return (CasConsumer) produceResource(CasConsumer.class, aSpecifier, null);
  }

  /**
   * Produces a {@link CasConsumer} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CasConsumer</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>CasConsumer</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static CasConsumer produceCasConsumer(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    return (CasConsumer) produceResource(CasConsumer.class, aSpecifier, aAdditionalParams);
  }

  /**
   * Produces an {@link CasConsumer} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CasConsumer</code>.
   * @param aResourceManager
   *          the <code>ResourceManager</code> to be used by this CasConsumer. If not specified,
   *          the default one returned by {@link #newDefaultResourceManager()} will be used.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return an <code>CasConsumer</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static CasConsumer produceCasConsumer(ResourceSpecifier aSpecifier,
          ResourceManager aResourceManager, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    return (CasConsumer) produceResource(CasConsumer.class, aSpecifier, aResourceManager,
            aAdditionalParams);
  }

  /**
   * Produces a {@link CollectionReader} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CollectionReader</code>.
   * 
   * @return a <code>CollectionReader</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static CollectionReader produceCollectionReader(ResourceSpecifier aSpecifier)
          throws ResourceInitializationException {
    return (CollectionReader) produceResource(CollectionReader.class, aSpecifier, null);
  }

  /**
   * Produces a {@link CollectionReader} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CollectionReader</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>CollectionReader</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static CollectionReader produceCollectionReader(ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    return (CollectionReader) produceResource(CollectionReader.class, aSpecifier, aAdditionalParams);
  }

  /**
   * Produces an {@link CollectionReader} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CollectionReader</code>.
   * @param aResourceManager
   *          the <code>ResourceManager</code> to be used by this CollectionReader. If not
   *          specified, the default one returned by {@link #newDefaultResourceManager()} will be
   *          used.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return an <code>CollectionReader</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static CollectionReader produceCollectionReader(ResourceSpecifier aSpecifier,
          ResourceManager aResourceManager, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    return (CollectionReader) produceResource(CollectionReader.class, aSpecifier, aResourceManager,
            aAdditionalParams);
  }

  /**
   * Produces a {@link CasInitializer} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CasInitializer</code>.
   * 
   * @return a <code>CasInitializer</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   * 
   * @deprecated As of v2.0, CAS Initializers are deprecated. A component that performs an operation
   *             like HTML detagging should instead be implemented as a "multi-Sofa" annotator. See
   *             org.apache.uima.examples.XmlDetagger for an example.
   */
  @Deprecated
  public static CasInitializer produceCasInitializer(ResourceSpecifier aSpecifier)
          throws ResourceInitializationException {
    return (CasInitializer) produceResource(CasInitializer.class, aSpecifier, null);
  }

  /**
   * Produces a {@link CasInitializer} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CasInitializer</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>CasInitializer</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static CasInitializer produceCasInitializer(ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    return (CasInitializer) produceResource(CasInitializer.class, aSpecifier, aAdditionalParams);
  }

  /**
   * Produces an {@link CasInitializer} instance from a <code>ResourceSpecifier</code>. The
   * <code>ResourceSpecifier</code> may either specify how to construct a new instance or how to
   * locate an existing instance.
   * 
   * @param aSpecifier
   *          an object that specifies how to acquire an instance of a <code>Resource</code>.
   *          This must specify a subclass of <code>CasInitializer</code>.
   * @param aResourceManager
   *          the <code>ResourceManager</code> to be used by this CasInitializer. If not
   *          specified, the default one returned by {@link #newDefaultResourceManager()} will be
   *          used.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link Resource#initialize(ResourceSpecifier,Map)} method. May be <code>null</code>
   *          if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return an <code>CasInitializer</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the resource.
   */
  public static CasInitializer produceCasInitializer(ResourceSpecifier aSpecifier,
          ResourceManager aResourceManager, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    return (CasInitializer) produceResource(CasInitializer.class, aSpecifier, aResourceManager,
            aAdditionalParams);
  }

  /**
   * Produces a {@link CollectionProcessingEngine} instance from a <code>cpeDescription</code>.
   * 
   * @param aCpeDescription
   *          an object that specifies how to create an instance of a
   *          <code>CollectionProcessingEngine</code>.
   * 
   * @return a <code>CollectionProcessingEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the CPE.
   */
  public static CollectionProcessingEngine produceCollectionProcessingEngine(
          CpeDescription aCpeDescription) throws ResourceInitializationException {
    return getInstance()._produceCollectionProcessingEngine(aCpeDescription, null);
  }

  /**
   * Produces a {@link CollectionProcessingEngine} instance from a <code>cpeDescription</code>.
   * 
   * @param aCpeDescription
   *          an object that specifies how to create an instance of a
   *          <code>CollectionProcessingEngine</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link CollectionProcessingEngine#initialize(CpeDescription,Map)} method. May be
   *          <code>null</code> if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>CollectionProcessingEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the CPE.
   */
  public static CollectionProcessingEngine produceCollectionProcessingEngine(
          CpeDescription aCpeDescription, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    return getInstance()._produceCollectionProcessingEngine(aCpeDescription, aAdditionalParams);
  }

  /**
   * Produces a {@link CollectionProcessingEngine} instance from a <code>cpeDescription</code>.
   * 
   * @param aCpeDescription
   *          an object that specifies how to create an instance of a
   *          <code>CollectionProcessingEngine</code>.
   * @param aResourceManager
   *          the <code>ResourceManager</code> to be used by this CollectionProcessingEngine. If
   *          not specified, the default one returned by {@link #newDefaultResourceManager()} will
   *          be used.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link CollectionProcessingEngine#initialize(CpeDescription,Map)} method. May be
   *          <code>null</code> if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>CollectionProcessingEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the CPE.
   */
  public static CollectionProcessingEngine produceCollectionProcessingEngine(
          CpeDescription aCpeDescription, ResourceManager aResourceManager, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    if (aResourceManager != null) {
      if (aAdditionalParams == null) {
        aAdditionalParams = new HashMap<String, Object>();
      } else {
        // copy to avoid modifying original, which might be immutable, etc.
        aAdditionalParams = new HashMap<String, Object>(aAdditionalParams);
      }
      aAdditionalParams.put(Resource.PARAM_RESOURCE_MANAGER, aResourceManager);
    }
    return getInstance()._produceCollectionProcessingEngine(aCpeDescription, aAdditionalParams);
  }

  /**
   * Gets the {@link org.apache.uima.util.Logger} used by the UIMA framework. An application won't
   * generally write to this logger, although nothing is stopping it from doing so.
   * <p>
   * In the UIMA SDK, the logger is implemented using the Java 1.4 logger as a back end. If you want
   * to configure the logger, for example to specify the location of the log file and the logging
   * level, you should use the standard Java 1.4 logger properties or the java.util.logging APIs.
   * See the section "Specifying the Logging Configuration" in the Annotator and Analysis Engine
   * Developer's Guide chapter of the UIMA documentation for more information.
   * 
   * @return the default Logger used by UIMA components
   */
  public static Logger getLogger() {
    return getInstance()._getLogger();
  }

  /**
   * Gets the {@link org.apache.uima.util.Logger} used by a particular Class, for example an
   * Annotator. An application won't generally write to this logger, although nothing is stopping it
   * from doing so.
   * <p>
   * In the UIMA SDK, the logger is implemented using the Java 1.4 logger as a back end. If you want
   * to configure the logger, for example to specify the location of the log file and the logging
   * level, you should use the standard Java 1.4 logger properties or the java.util.logging APIs.
   * See the section "Specifying the Logging Configuration" in the Annotator and Analysis Engine
   * Developer's Guide chapter of the UIMA documentation for more information.
   * 
   * @param component
   *          the Class for a component, for example an Annotator or CAS Consumer
   * 
   * @return the Logger used by the specified component class
   */
  public static Logger getLogger(Class<?> component) {
    return getInstance()._getLogger(component);
  }

  /**
   * Creates a new {@link org.apache.uima.util.Logger}, which can be passed for example to the
   * {@link AnalysisEngine#setLogger(Logger)} method in order to have separate Analysis Engine
   * instances used separate loggers.
   * 
   * @return a new Logger instance
   * 
   * @see #getLogger()
   */
  public static Logger newLogger() {
    return getInstance()._newLogger();
  }

  /**
   * Creates a new {@link org.apache.uima.util.UimaTimer}, which is used to collect performance
   * statistics for UIMA components.
   * 
   * @return a new Timer instance
   */
  public static UimaTimer newTimer() {
    return getInstance()._newTimer();
  }

  /**
   * Gets a new instance of the default {@link ResourceManager} used by this implementation. An
   * application can configure this ResourceManager and then pass it to the
   * {@link #produceAnalysisEngine(ResourceSpecifier,ResourceManager,Map)} method.
   * 
   * @return a new <code>ResourceManager</code> to be used by the application.
   */
  public static ResourceManager newDefaultResourceManager() {
    return getInstance()._newDefaultResourceManager();
  }

  /**
   * Gets a new instance of the default {@link org.apache.uima.resource.ResourceManagerPearWrapper} used by this implementation. 
   * 
   * @return a new <code>ResourceManagerPearWrapper</code> to be used by the application.
   */
  public static ResourceManager newDefaultResourceManagerPearWrapper() {
    return getInstance()._newDefaultResourceManagerPearWrapper();
  }

  /**
   * Gets a new instance of the {@link ConfigurationManager} used by this implementation. This will
   * be used by Resources to manage access to their configuration parameters.
   * 
   * @return a new <code>ConfigurationManager</code> to be used by the application.
   */
  public static ConfigurationManager newConfigurationManager() {
    return getInstance()._newConfigurationManager();
  }
  
  // ugly way to pass vars to 0-arg constructors
  //    for root uima context
  public static final ThreadLocal<ResourceManager> newContextResourceManager = new ThreadLocal<ResourceManager>();
  public static final ThreadLocal<ConfigurationManager> newContextConfigManager = new ThreadLocal<ConfigurationManager>();
  
  /**
   * Gets a new instance of a {@link UimaContext}. Applications do not generally need to call this
   * method.
   * 
   * @param aLogger
   *          the logger that will be returned by this UimaContext's {@link #getLogger()} method.
   * @param aResourceManager
   *          the ResourceManager that will be used by this UimaContext to locate and access
   *          external resource.
   * @param aConfigManager
   *          the ConfigurationManager that will be used by this UimaContext to manage Configuration
   *          Parameter settings.
   * 
   * @return a new UIMA Context to be used by the application.
   */
  public static UimaContextAdmin newUimaContext(Logger aLogger, ResourceManager aResourceManager,
          ConfigurationManager aConfigManager) {
    // We use an ugly trick to make the 3 values available to the new UIMA context during its initialization - 
    //   we put them in threadlocals for this class (UIMAFramework).
    UimaContextAdmin context; 
    try {
      newContextResourceManager.set(aResourceManager);
      newContextConfigManager.set(aConfigManager);     
      context = getInstance()._newUimaContext();
    } finally {
      newContextResourceManager.set(null);
      newContextConfigManager.set(null);
    }
    context.initializeRoot(aLogger, aResourceManager, aConfigManager);
    return context;
  }

  /**
   * Gets the default performance tuning settings for the framework. Advanced users can tweak the
   * framework by modifying these properties and passing the modified Properties object into the
   * {@link #produceTAE(ResourceSpecifier,Map)} or
   * {@link #produceCollectionProcessingEngine(CpeDescription,Map)} methods by putting it into the
   * <code>aAdditionalParams</code> map under the key
   * {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}. For example, the following code set the
   * initial heap size allocated in the CAS to 100,000:
   * 
   * <pre>
   * Properties uimaPerfProps = UIMAFramework.getDefaultPerformanceTuningProperties();
   * uimaPerfProps.setProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE, &quot;100000&quot;);
   * HashMap params = new HashMap();
   * params.put(Resource.PARAM_PERFORMANCE_TUNING_SETTINGS, uimaPerfProps);
   * AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier, params);
   * </pre>
   * 
   * <p>
   * Valid keys for the {@link Properties} object returned by this method are specified as
   * constants on this interface.
   * 
   * @return the default set of performance tuning properties. A new object is returned each time
   *         this method is called, so changes made to the returned objects will not affect other
   *         callers.
   */
  public static Properties getDefaultPerformanceTuningProperties() {
    return getInstance()._getDefaultPerformanceTuningProperties();
  }

  /**
   * Gets the <code>UIMAFramework</code> instance currently in use.
   * 
   * @return the <code>UIMAFramework</code> instance currently in use
   */
  protected static UIMAFramework getInstance() {
    return mInstance;
  }

  /**
   * To be implemented by subclasses; this should return the major version number of this
   * implementation.
   * 
   * @return the major version number
   */
  protected abstract short _getMajorVersion();

  /**
   * To be implemented by subclasses; this should return the minor version number of this
   * implementation.
   * 
   * @return the minor version number
   */
  protected abstract short _getMinorVersion();

  /**
   * To be implemented by subclasses; called from this class's static initializer to complete
   * initialization of the singleton instance. This initialization is done outside the constructor
   * so that the {@link #getInstance()} method can be used during initialization.
   * 
   * @throws Exception
   *           if initialization fails
   */
  protected abstract void _initialize() throws Exception;

  /**
   * To be implemented by subclasses; this should return the build revision number of this
   * implementation.
   * 
   * @return the build revision number
   */
  protected abstract short _getBuildRevision();

  /**
   * To be implemented by subclasses; this should return a reference to the
   * <code>ResourceFactory</code> used by this implementation, which must implement
   * {@link CompositeResourceFactory}.
   * 
   * @return the <code>ResourceFactory</code> to be used by the application
   */
  protected abstract CompositeResourceFactory _getResourceFactory();

  /**
   * To be implemented by subclasses; this should return a reference to the
   * <code>ResourceSpecifierFactory</code> used by this implementation.
   * 
   * @return the <code>ResourceSpecifierFactory</code> to be used by the application.
   */
  protected abstract ResourceSpecifierFactory _getResourceSpecifierFactory();

  /**
   * To be implemented by subclasses; this should return a reference to the UIMA {@link XMLParser}
   * used by this implementation.
   * 
   * @return the <code>XMLParser</code> to be used by the application.
   */
  protected abstract XMLParser _getXMLParser();

  /**
   * To be implemented by subclasses; this should create a new instance of a class implementing
   * {@link CollectionProcessingManager}.
   * 
   * @param aResourceManager
   *          the ResourceManager to be used by the CPM
   * 
   * @return a new <code>CollectionProcessingManager</code> to be used by the application.
   */
  protected abstract CollectionProcessingManager _newCollectionProcessingManager(
          ResourceManager aResourceManager);

  /**
   * To be implemented by subclasses; this should return a reference to the default UIMA
   * {@link Logger} used by this implementation.
   * 
   * @return the default <code>Logger</code> used by this implementation
   */
  protected abstract Logger _getLogger();

  /**
   * To be implemented by subclasses; this should return a reference to the UIMA {@link Logger} of
   * the specified source class.
   * @param component the class to get the logger for 
   * @return the <code>Logger</code> of the specified source class
   */
  protected abstract Logger _getLogger(Class<?> component);

  /**
   * To be implemented by subclasses; this should return a new UIMA {@link Logger} instance.
   * 
   * @return a new <code>Logger</code> instance
   */
  protected abstract Logger _newLogger();

  /**
   * To be implemented by subclasses; this should return a new UIMA {@link UimaTimer} instance.
   * 
   * @return a new <code>Timer</code> instance
   */
  protected abstract UimaTimer _newTimer();

  /**
   * To be implemented by subclasses; this should return a new instance of the default
   * {@link ResourceManager} used by this implementation.
   * 
   * @return a new <code>ResourceManager</code> to be used by the application.
   */
  protected abstract ResourceManager _newDefaultResourceManager();

  /**
   * To be implemented by subclasses; this should return a new instance of the default
   * {@link org.apache.uima.resource.ResourceManagerPearWrapper} used by this implementation.
   * 
   * @return a new <code>ResourceManagerPearWrapper</code> to be used by the application.
   */
  protected abstract ResourceManager _newDefaultResourceManagerPearWrapper();
  
  
  /**
   * To be implemented by subclasses; this should return a new instance of the default
   * {@link ConfigurationManager} used by this implementation.
   * 
   * @return a new <code>ConfigurationManager</code> to be used by the application.
   */
  protected abstract ConfigurationManager _newConfigurationManager();

  /**
   * To be implemented by subclasses; this should return a new instance of the default
   * {@link UimaContextAdmin} used by this implementation.
   * 
   * @return a new <code>UimaContextAdmin</code> to be used by the application.
   */
  protected abstract UimaContextAdmin _newUimaContext();

  /**
   * To be implemented by subclasses; this should produce a {@link CollectionProcessingEngine}
   * instance from a <code>cpeDescription</code>.
   * 
   * @param aCpeDescription
   *          an object that specifies how to create an instance of a
   *          <code>CollectionProcessingEngine</code>.
   * @param aAdditionalParams
   *          a Map containing additional parameters to pass to the
   *          {@link CollectionProcessingEngine#initialize(CpeDescription,Map)} method. May be
   *          <code>null</code> if there are no parameters. Parameter names are defined as constants on the
   *          {@link AnalysisEngine}, and {@link Resource}.
   *          Furthermore, the entry under the key {@link Resource#PARAM_PERFORMANCE_TUNING_SETTINGS}
   *          is a map which may contain settings with keys defined as constants here {@link UIMAFramework} interfaces. 
   *          For example this can be used to set
   *          performance-tuning settings as described in
   *          {@link #getDefaultPerformanceTuningProperties()}.
   * 
   * @return a <code>CollectionProcessingEngine</code> instance.
   * 
   * @throws ResourceInitializationException
   *           if a failure occurred during production of the CPE.
   */
  protected abstract CollectionProcessingEngine _produceCollectionProcessingEngine(
          CpeDescription aCpeDescription, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException;

  static {
    // intall framework implementation
    String frameworkClassName = null;
    try {
      frameworkClassName = System.getProperty("uima.framework_impl");
    } catch (SecurityException e) {
      // can't access system properties
    }

    if (frameworkClassName == null) {
      frameworkClassName = REF_IMPL_CLASS_NAME; // use default
    }
    try {
      Class<?> implClass = Class.forName(frameworkClassName);
      mInstance = (UIMAFramework) implClass.newInstance();
      mInstance._initialize();
    } catch (Exception e) {
      // could not load reference implementation
      throw new UIMA_IllegalStateException(UIMA_IllegalStateException.COULD_NOT_CREATE_FRAMEWORK,
              new Object[] { frameworkClassName }, e);
    }
  }
}
