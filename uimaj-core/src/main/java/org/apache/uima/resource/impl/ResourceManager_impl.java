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

package org.apache.uima.resource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.resource.CasManager;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.ParameterizedDataResource;
import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLizable;

/**
 * Reference implementation of {@link org.apache.uima.resource.ResourceManager}.
 * 
 * 
 */
public class ResourceManager_impl implements ResourceManager {
  
  /**
   * Ties an External Resource instance to
   *   - its description 
   *     -- name
   *     -- textual description
   *     -- a ResourceSpecifier describing how to create it
   *     -- (optional) the String name of the Java class that implements the resource)
   *   - its defining UIMA Context
   *   
   *   These are used to validate multiple declarations, and to get
   *   a resource to tie it to a binding
   */
  static protected class ResourceRegistration { // make protected https://issues.apache.org/jira/browse/UIMA-2102
    /**
     * For ParameterizedDataResources or DataResources, is the implementation object, which is 
     *   an arbitrary Java class implementing SharedDataResource (which has the "load" method)
     *   
     * If the external resource specification omitted the implementation class, a default FileResource  
     */
    Object resource;  

    ExternalResourceDescription description;

    String definingContext;

    public ResourceRegistration(Object resourceOrImplementation, ExternalResourceDescription description,
            String definingContext) {
      this.resource = resourceOrImplementation;
      this.description = description;
      this.definingContext = definingContext;
    }
  }

  /**
   * resource bundle for log messages
   */
  protected static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";
  
  protected static final Class<Resource> EMPTY_RESOURCE_CLASS = Resource.class;

  private AtomicBoolean isDestroyed = new AtomicBoolean(false);
  /**
   * a monitor lock for synchronizing get/set of casManager ref
   */
  private final Object casManagerMonitor = new Object();

  /**
   * Object used for resolving relative paths. This is built by parsing the data path.
   */
  private final RelativePathResolver mRelativePathResolver;

  /**
   * Map from qualified key names (declared in resource dependency XML) to 
   *   either DataResource objects or SharedResourceObjects that impl Resource.
   *   
   * This map is many to one (multiple keys may refer to the same Resource object)
   * 
   * Can't be concurrentMap because it (currently) depends on storing nulls
   * 
   * Keeping the Map's 2nd argument as Object, although it could be Resource, 
   * for backwards compatibility 
   */
  final protected Map<String, Object> mResourceMap;
  
  /**
   * Internal map from resource names (declared in resource declaration XML) to ResourceRegistration
   * objects. 
   *   Uses: for detecting if multiple declarations are made for the same resource name
   *         .values() for list of all resources that have been registered
   * 
   * This is a one-to-one map.
   */
  final protected Map<String, ResourceRegistration> mInternalResourceRegistrationMap;

  /**
   * Map from String keys to Class objects. For ParameterizedResources only, stores the
   * implementation class (not a Resource) corresponding to each resource name.
   * 
   * These class objects may or may not be Resource instances.
   *   They may be arbitrary classes, except that they must implement SharedResourceObject.
   * 
   * This is a many to one map; many keys may refer to the same class
   * 
   * key = aQualifiedContextName + the key name in an external resource binding
   */
  final protected Map<String, Class<?>> mParameterizedResourceImplClassMap;

  /**
   * Internal map from resource names (declared in resource declaration XML) to Class objects
   * for parameterized Resource.
   * 
   *   These class objects may or may not be Resource instances.  
   *   They may be arbitrary classes, except that they must implement SharedResourceObject.
   *   
   * These are "customized" when referenced, by 
   * parameter strings (such as language, for a Dictionary resource). Used
   * internally during resource initialization.
   * 
   * key = external resource declared name.
   */
  final protected Map<String, Class<?>> mInternalParameterizedResourceImplClassMap;

  /**
   * Map from ParameterizedResourceKey to SharedResourceObject or DataResource instances.
   * 
   *   Note: SharedResourceObjects are not "Resource" instances, but rather arbitrary Java classes that
   *   are the "implementations" of Resources.  The only requirement on these Java classes is 
   *   that they implement SharedResourceObject interface.
   *   
   * This map is for ParameterizedResources only, and 
   * stores the SharedResourceObjects or DataResource objects that have already been instantiated and loaded.
   */
  final protected Map<List<Object>, Object> mParameterizedResourceInstanceMap;

  /**
   * UIMA extension ClassLoader. ClassLoader is created if an extension classpath is specified at
   * the ResourceManager
   * 
   * volatile might be better than synch sets/gets
   */
  private volatile UIMAClassLoader uimaCL = null;

  /** CasManager - manages creation and pooling of CASes. */
  // volatile to support double-checked locking idiom
  protected volatile CasManager mCasManager = null;

  /**
   * Cache of imported descriptors, so that parsed objects can be reused if the
   * same URL is imported more than once.
   * 
   * All callers of this synchronize on the importCache object before doing a 
   *    get
   *    ...
   *    put
   * sequence
   * 
   * Use Case where synchronization is needed:
   *   running multiple instances on multiple threads, sharing a common resource manager,
   *   the initialization that merges typePriorities happens lazily, when using Cas Multipliers,
   *   and occurs when the first getCas call happens on a thread.  Although these calls
   *   are synchronized among themselves, any other use of this map that might occur
   *   simultaneously is not.
   */
  // leaving this as a synchronizedMap - for backwards compatibility
  // internal users do sync around get/set pairs anyways, but can't rely on
  // what external users do
  //   Because internal users do a sync, only one thread at a time is using this
  //   (for internal calls) anyways, so there's no advantage to the extra overhead
  //   of making this a ConcurrentHashMap  (March 2014)
  private Map<String, XMLizable> importCache = Collections.synchronizedMap(new HashMap<String,XMLizable>());
  
  /**
   * Cache of imported descriptor URLs from which the parsed objects in importCache
   * were created, so that these URLs are not re-parsed if the same URL is imported again.
   */
  private Map<String, Set<String>> importUrlsCache = Collections.synchronizedMap(new HashMap<String,Set<String>>());
  
  /**
   * Creates a new <code>ResourceManager_impl</code>.
   */
  public ResourceManager_impl() {
    mResourceMap = Collections.synchronizedMap(new HashMap<String, Object>());
    mInternalResourceRegistrationMap = new ConcurrentHashMap<String, ResourceRegistration>();
    mParameterizedResourceImplClassMap =  new ConcurrentHashMap<String, Class<?>>();
    mInternalParameterizedResourceImplClassMap = new ConcurrentHashMap<String, Class<?>>();
    mParameterizedResourceInstanceMap =  new ConcurrentHashMap<List<Object>, Object>();
    mRelativePathResolver = new RelativePathResolver_impl(); 
  }

  /**
   * Creates a new <code>ResourceManager_impl</code> with a custom ClassLoader to use for locating
   * resources.
   * @param aClassLoader -
   */
  public ResourceManager_impl(ClassLoader aClassLoader) {
    mResourceMap = Collections.synchronizedMap(new HashMap<String, Object>());
    mInternalResourceRegistrationMap = new ConcurrentHashMap<String, ResourceRegistration>();
    mParameterizedResourceImplClassMap =  new ConcurrentHashMap<String, Class<?>>();
    mInternalParameterizedResourceImplClassMap = new ConcurrentHashMap<String, Class<?>>();
    mParameterizedResourceInstanceMap =  new ConcurrentHashMap<List<Object>, Object>();
    mRelativePathResolver = new RelativePathResolver_impl(aClassLoader);
  }

  /*
   * Version for Pear wrapper 
   */
  public ResourceManager_impl(
      Map<String, Object> resourceMap,
      Map<String, ResourceRegistration> internalResourceRegistrationMap,
      Map<String, Class<?>> parameterizedResourceImplClassMap,
      Map<String, Class<?>> internalParameterizedResourceImplClassMap,
      Map<List<Object>, Object> parameterizedResourceInstanceMap) {
    mResourceMap = resourceMap;
    mInternalResourceRegistrationMap = internalResourceRegistrationMap;
    mParameterizedResourceImplClassMap =  parameterizedResourceImplClassMap;
    mInternalParameterizedResourceImplClassMap = internalParameterizedResourceImplClassMap;
    mParameterizedResourceInstanceMap =  parameterizedResourceInstanceMap;
    mRelativePathResolver = new RelativePathResolver_impl(); 
  }
  
  public ResourceManager_impl copy() {
    ResourceManager_impl rm = new ResourceManager_impl(
        this.mResourceMap,
        this.mInternalResourceRegistrationMap,
        this.mParameterizedResourceImplClassMap,
        this.mInternalParameterizedResourceImplClassMap,
        this.mParameterizedResourceInstanceMap
        );
    // non-final fields init
    rm.uimaCL = this.uimaCL;
    rm.importCache = this.importCache;
    rm.importUrlsCache = this.importUrlsCache;
    return rm; 
  }
  
  
 /**
  * Support reusing UIMA Class Loader instances to speed up
  * things including the Component Description Editor when
  * obtaining info from CustomResourceSpecifiers
  * https://issues.apache.org/jira/browse/UIMA-1722
  * @param uimaCL -
  * @param resolveResource -
  */
 public synchronized void setExtensionClassPath(UIMAClassLoader uimaCL, boolean resolveResource) {
   this.uimaCL = uimaCL;
   
   if (resolveResource) {
     // set UIMA extension ClassLoader also to resolve resources
     getRelativePathResolver().setPathResolverClassLoader(uimaCL);
   }
 }

 /**

  /**
   * @see org.apache.uima.resource.ResourceManager#setExtensionClassPath(java.lang.String, boolean)
   */
  @Override
  public synchronized void setExtensionClassPath(String classpath, boolean resolveResource)
          throws MalformedURLException {
    // create UIMA extension ClassLoader with the given classpath
    uimaCL = new UIMAClassLoader(classpath, this.getClass().getClassLoader());

    if (resolveResource) {
      // set UIMA extension ClassLoader also to resolve resources
      getRelativePathResolver().setPathResolverClassLoader(uimaCL);
    }
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#setExtensionClassPath(ClassLoader,java.lang.String,
   *      boolean)
   */
  @Override
  public synchronized void setExtensionClassPath(ClassLoader parent, String classpath, boolean resolveResource)
          throws MalformedURLException {
    // create UIMA extension ClassLoader with the given classpath
    uimaCL = new UIMAClassLoader(classpath, parent);

    if (resolveResource) {
      // set UIMA extension ClassLoader also to resolve resources
      getRelativePathResolver().setPathResolverClassLoader(uimaCL);
    }
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#getExtensionClassLoader()
   */
  @Override
  public ClassLoader getExtensionClassLoader() {
    return uimaCL;
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#getDataPath()
   */
  @Override
  public String getDataPath() {
    return getRelativePathResolver().getDataPath();
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#setDataPath(String)
   */
  @Override
  public void setDataPath(String aPath) throws MalformedURLException {
    getRelativePathResolver().setDataPath(aPath);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#resolveRelativePath(java.lang.String)
   */
  @Override
  public URL resolveRelativePath(String aRelativePath) throws MalformedURLException {
    URL relativeUrl;
    try {
      relativeUrl = new URL(aRelativePath);
    } catch (MalformedURLException e) {
      relativeUrl = new URL("file", "", aRelativePath);
    }
    return getRelativePathResolver().resolveRelativePath(relativeUrl);
  }

  private void checkDestroyed() {
    if (isDestroyed.get()) {
      throw new IllegalStateException("ResourceManager is destroyed");
    }    
  }
  
  /**
   * @see org.apache.uima.resource.ResourceManager#getResource(String)
   */
  @Override
  public Object getResource(String aName) throws ResourceAccessException {
    checkDestroyed();
    Object r = mResourceMap.get(aName);
    // if this is a ParameterizedDataResource, it is an error
    if (r instanceof ParameterizedDataResource) {
      throw new ResourceAccessException(ResourceAccessException.PARAMETERS_REQUIRED,
              new Object[] { aName });
    }
    return r;
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#getResource(java.lang.String, java.lang.String[])
   */
  @Override
  public Object getResource(String aName, String[] aParams) throws ResourceAccessException {
    /* Multi-core design
     *   This may be called by user code sharing the same Resource Manager, and / or the same 
     *     uima context object.
     *   Do double-checked idiom to avoid locking where resource is already available, loaded   
     */
    checkDestroyed();
    Object r = mResourceMap.get(aName);

    // if no resource found, return null
    if (r == null) {
      return null;
    }
    // if not a ParameterizedDataResource, it is an error
    if (!(r instanceof ParameterizedDataResource)) {
      throw new ResourceAccessException(ResourceAccessException.PARAMETERS_NOT_ALLOWED,
              new Object[] { aName });
    }
    ParameterizedDataResource pdr = (ParameterizedDataResource) r;

    // get a particular DataResource instance for the specified parameters
    DataResource dr;
    try {
      dr = pdr.getDataResource(aParams);
    } catch (ResourceInitializationException e) {
      throw new ResourceAccessException(e);
    }

    // see if we've already encountered this DataResource under this resource name
    List<Object> nameAndResource = new ArrayList<Object>(2);
    nameAndResource.add(aName);
    nameAndResource.add(dr);
    Object resourceInstance = mParameterizedResourceInstanceMap.get(nameAndResource);
    if (resourceInstance != null) {
      return resourceInstance;
    }
    synchronized(mParameterizedResourceInstanceMap) {
      // double-check idiom
      resourceInstance = mParameterizedResourceInstanceMap.get(nameAndResource);
      if (resourceInstance != null) {
        return resourceInstance;
      }
      // We haven't encountered this before. See if we need to instantiate a
      // SharedResourceObject
      Class<?> sharedResourceObjectClass = mParameterizedResourceImplClassMap.get(aName);
      if (sharedResourceObjectClass != EMPTY_RESOURCE_CLASS) {
        try {
          SharedResourceObject sro = (SharedResourceObject) sharedResourceObjectClass.newInstance();
          sro.load(dr);
          mParameterizedResourceInstanceMap.put(nameAndResource, sro);
          return sro;
        } catch (InstantiationException e) {
          throw new ResourceAccessException(e);
        } catch (IllegalAccessException e) {
          throw new ResourceAccessException(e);
        } catch (ResourceInitializationException e) {
          throw new ResourceAccessException(e);
        }
      } else
      // no impl. class - just return the DataResource
      {
        mParameterizedResourceInstanceMap.put(nameAndResource, dr);
        return dr;
      }
    }
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#getResourceClass(java.lang.String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public Class<?> getResourceClass(String aName) {
    Object r = mResourceMap.get(aName);
    if (r == null) // no such resource
    {
      return null;
    }

    // if this is a ParameterizedDataResource, look up its class
    if (r instanceof ParameterizedDataResource) {
      Class<?> customResourceClass = mParameterizedResourceImplClassMap.get(aName);
      if (customResourceClass == EMPTY_RESOURCE_CLASS) {
        // return the default class
        return DataResource_impl.class;
      }
      return customResourceClass;
    } else {
      // return r's Class
      // could be, for 
      return (Class<? extends Resource>) r.getClass();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getResourceAsStream(java.lang.String,
   *      java.lang.String[])
   */
  @Override
  public InputStream getResourceAsStream(String aKey, String[] aParams)
          throws ResourceAccessException {
    return getResourceAsStreamCommon(getResource(aKey, aParams));
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getResourceAsStream(java.lang.String)
   */
  @Override
  public InputStream getResourceAsStream(String aKey) throws ResourceAccessException {
    return getResourceAsStreamCommon(getResource(aKey));
  }

  private InputStream getResourceAsStreamCommon(Object resource) throws ResourceAccessException {
    checkDestroyed();
    try {
      if (resource != null && resource instanceof DataResource) {
        return ((DataResource) resource).getInputStream();
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new ResourceAccessException(e);
    }    
  }

  private URL getResourceAsStreamCommonUrl(Object resource) {
    if (resource != null && resource instanceof DataResource) {
      return ((DataResource) resource).getUrl();
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getResourceURL(java.lang.String,
   *      java.lang.String[])
   */
  @Override
  public URL getResourceURL(String aKey, String[] aParams) throws ResourceAccessException {
    return getResourceAsStreamCommonUrl(getResource(aKey, aParams));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getResourceURL(java.lang.String)
   */
  @Override
  public URL getResourceURL(String aKey) throws ResourceAccessException {
    return getResourceAsStreamCommonUrl(getResource(aKey));
  }

  /*
   * (non-Javadoc)
   * 
   * This method is called during Resource Initialization, 
   *   - only for resources which are "local", that is, instances of ResourceCreationSpecifier
   *   - and therefore might have external resource declarations 
   * 
   * Compare with resolveAndValidateResourceDependencies, called for resource binding resolution.
   * 
   * @see ResourceManager#initializeExternalResources(ResourceManagerConfiguration, String, Map<String, Object>)
   */
  @Override
  public synchronized void initializeExternalResources(ResourceManagerConfiguration aConfiguration,
          String aQualifiedContextName, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    // register resources
    checkDestroyed();
    
    // set up aAdditionalParams to have this resource manager if not already set
    // so that External Resource instances created from this use this creating/owning
    // resource manager as the value returned from their getResourceManager call
    // see https://issues.apache.org/jira/browse/UIMA-5148
    if (null == aAdditionalParams) {
      aAdditionalParams = new HashMap<String, Object>();
    }
    if (!aAdditionalParams.containsKey(Resource.PARAM_RESOURCE_MANAGER)) {
      aAdditionalParams.put(Resource.PARAM_RESOURCE_MANAGER, this);
    }
    
    // set up aAdditionalParams to have this resource manager if not already set
    // so that External Resource instances created from this use this creating/owning
    // UIMA Context so that getResourceManager works
    // see https://issues.apache.org/jira/browse/UIMA-5153
    
    ExternalResourceDescription[] resources = aConfiguration.getExternalResources();
    for (int i = 0; i < resources.length; i++) {
      String name = resources[i].getName();

      // check for existing resource registration under this name
      ResourceRegistration registration = mInternalResourceRegistrationMap.get(name);
      if (registration == null) {
        registerResource(name, resources[i], aQualifiedContextName, aAdditionalParams);
      } else {
        // log a message if the resource definitions are not identical
        if (!registration.description.equals(resources[i])) {
          // if the resource was overridden in an enclosing aggregate, use an INFO level message.
          // if not (e.g. sibling annotators declare the same resource name), it's a WARNING.
          if (aQualifiedContextName.startsWith(registration.definingContext)) {
            UIMAFramework.getLogger().logrb(Level.CONFIG, ResourceManager_impl.class.getName(),
                    "initializeExternalResources", LOG_RESOURCE_BUNDLE,
                    "UIMA_overridden_resource__CONFIG",
                    new Object[] { name, aQualifiedContextName, registration.definingContext});
          } else {
            UIMAFramework.getLogger().logrb(Level.WARNING, ResourceManager_impl.class.getName(),
                    "initializeExternalResources", LOG_RESOURCE_BUNDLE,
                    "UIMA_duplicate_resource_name__WARNING",
                    new Object[] { name, registration.definingContext, aQualifiedContextName});
          }
        }
      }
    }
    // apply bindings
    ExternalResourceBinding[] bindings = aConfiguration.getExternalResourceBindings();
    for (int i = 0; i < bindings.length; i++) {
      ResourceRegistration registration = mInternalResourceRegistrationMap
              .get(bindings[i].getResourceName());
      if (registration == null) {
        throw new ResourceInitializationException(
                ResourceInitializationException.UNKNOWN_RESOURCE_NAME, new Object[] {
                    bindings[i].getResourceName(), bindings[i].getSourceUrlString() });
      }
      mResourceMap.put(aQualifiedContextName + bindings[i].getKey(), registration.resource);
      // record the link from key to resource class (for parameterized resources only)
      Class<?> impl = mInternalParameterizedResourceImplClassMap.get(bindings[i].getResourceName()); 
      mParameterizedResourceImplClassMap.put(aQualifiedContextName + bindings[i].getKey(),
                                             (impl == null) ? EMPTY_RESOURCE_CLASS : impl);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * Called during resource initialization, when the resource has external resource bindings,
   * to resolve those bindings                                              
   *                                               
   * @see ResourceManager#resolveAndValidateResourceDependencies(ExternalResourceDependency[], String)
   *      
   * Multi-threaded.  Partial avoidance of re-resolving, but if a resource fails to resolve, it will be 
   *   reattempted on every call
   */
  @Override
  public synchronized void resolveAndValidateResourceDependencies(ExternalResourceDependency[] aDependencies,
          String aQualifiedContextName) throws ResourceInitializationException {
    checkDestroyed();
    for (int i = 0; i < aDependencies.length; i++) {
      // get resource
      String qname = aQualifiedContextName + aDependencies[i].getKey();
      Object resourceImpl = mResourceMap.get(qname);  // may or may not implement Resource, may implement SharedResourceObject
      
      if (resourceImpl == null) {
        // no resource found
        // try to look up in classpath/datapath
        URL relativeUrl;
        try {
          relativeUrl = new URL("file", "", aDependencies[i].getKey());
        } catch (MalformedURLException e) {
          throw new ResourceInitializationException(e);
        }
        URL absUrl = getRelativePathResolver().resolveRelativePath(relativeUrl);
        if (absUrl != null) {
          // found - create a DataResource object and store it in the mResourceMap
          FileResourceSpecifier spec = new FileResourceSpecifier_impl();
          spec.setFileUrl(absUrl.toString());
          // produces an instance of DataResourceImpl
          resourceImpl = UIMAFramework.produceResource(spec, null);
          mResourceMap.put(qname, resourceImpl);
        }
      }
      
      if (resourceImpl == null) { // still no resource found - throw exception if required

        if (!aDependencies[i].isOptional()) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.RESOURCE_DEPENDENCY_NOT_SATISFIED, new Object[] {
                      aDependencies[i].getKey(), aDependencies[i].getSourceUrlString() });
        }

      } else {  // resource not null

        // make sure resource exists and implements the correct interface
        try {
          String name = aDependencies[i].getInterfaceName();
          if (name != null && name.length() > 0) {
            Class<?> theInterface = loadUserClass(name);

            Class<?> resourceClass = getResourceClass(qname);
            if (!theInterface.isAssignableFrom(resourceClass)) {
              throw new ResourceInitializationException(
                      ResourceInitializationException.RESOURCE_DOES_NOT_IMPLEMENT_INTERFACE,
                      new Object[] { qname, aDependencies[i].getInterfaceName(),
                          aDependencies[i].getSourceUrlString() });
            }
          }
        } catch (ClassNotFoundException e) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.CLASS_NOT_FOUND, new Object[] {
                      aDependencies[i].getInterfaceName(), aDependencies[i].getSourceUrlString() });
        }
      }
      
    }
  }

  /**
   * Instantiates a resource and inserts it in the internal resource map.
   */
  private void registerResource(String aName, ExternalResourceDescription aResourceDescription,
          String aDefiningContext, Map<String, Object> aResourceInitParams) throws ResourceInitializationException {
    // add the relative path resolver to the resource init. params
    Map<String, Object> initParams = (aResourceInitParams == null) ? new HashMap<String, Object>() : new HashMap<String, Object>(
            aResourceInitParams);
    initParams.put(DataResource.PARAM_RELATIVE_PATH_RESOLVER, getRelativePathResolver());
    
    // determine if verification mode is on.  If so, we don't want to load the resource data
    boolean verificationMode = initParams.containsKey(AnalysisEngineImplBase.PARAM_VERIFICATION_MODE);
    
    // create the initial resource using the resource factory
    Resource r = UIMAFramework.produceResource(aResourceDescription.getResourceSpecifier(),
            initParams);

    // load implementation class (if any) and ensure that it implements
    // SharedResourceObject
    String implementationName = aResourceDescription.getImplementationName();
    Class<?> implClass = null;  // might or might not impl Resource
    Object implInstance = r;  // what will be registered, might be the Resource, or its implementation
    if (implementationName != null && implementationName.length() > 0) {
      try {
        implClass = loadUserClass(implementationName);
      } catch (ClassNotFoundException e) {
        throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
                new Object[] { implementationName, aResourceDescription.getSourceUrlString() }, e);
      }

      if (!SharedResourceObject.class.isAssignableFrom(implClass)) {
        throw new ResourceInitializationException(
                ResourceInitializationException.NOT_A_SHARED_RESOURCE_OBJECT, new Object[] {
                    implementationName, aResourceDescription.getSourceUrlString() });
      }
    }

    // is this a DataResource?
    if (r instanceof DataResource) {
      // instantiate and load the resource object if there is one
      if (implClass != null) {
        try {
          SharedResourceObject sro = (SharedResourceObject) implClass.newInstance();
          if (!verificationMode) {
            sro.load((DataResource) r);
          }
          implInstance = sro;   // so the implementation is registered, for DataResources
        } catch (InstantiationException e) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] {
                      implClass.getName(), aResourceDescription.getSourceUrlString() }, e);
        } catch (IllegalAccessException e) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.COULD_NOT_INSTANTIATE, new Object[] {
                      implClass.getName(), aResourceDescription.getSourceUrlString() }, e);
        }
      }
    }
    // is it a ParameterizedDataResource?
    else if (r instanceof ParameterizedDataResource) {
      // we can't load the SharedResourceObject now, but we need to remember
      // which class it is for later when we get a request with parameters
      mInternalParameterizedResourceImplClassMap.put(aName, (null == implClass) ? EMPTY_RESOURCE_CLASS : implClass);
    } else {
    // it is some other type of Resource
      // it is an error to specify an implementation class in this case
      if (implClass != null) {
        throw new ResourceInitializationException(
                ResourceInitializationException.NOT_A_DATA_RESOURCE, new Object[] {
                    implClass.getName(), aName, r.getClass().getName(),
                    aResourceDescription.getSourceUrlString() });
      }
    }

    // put resource or its implementation (for DataResources)  in internal map for later retrieval
    ResourceRegistration registration = new ResourceRegistration(implInstance, aResourceDescription,
            aDefiningContext);
    mInternalResourceRegistrationMap.put(aName, registration);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getCasManager()
   */
  @Override
  public CasManager getCasManager() {
    //Optimization for case where mCasManager already created
    // Some sync contention was observed - this makes it less.  UIMA-4012
    if(mCasManager != null) {
      return mCasManager;
    }
    synchronized(casManagerMonitor) {
      if (mCasManager == null) {
        mCasManager = new CasManager_impl(this);
      }
      return mCasManager;
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.resource.ResourceManager#setCasManager(org.apache.uima.resource.CasManager)
   */
  @Override
  public void setCasManager(CasManager aCasManager) {
    synchronized(casManagerMonitor) {
      if (mCasManager == null) {
        mCasManager = aCasManager;
      }
      else {
        throw new UIMA_IllegalStateException(
                UIMA_IllegalStateException.CANNOT_SET_CAS_MANAGER, new Object[0]);
      }
    }
  }
 
  // This method overridden by subclass for pear wrapper
  protected RelativePathResolver getRelativePathResolver() {
    return mRelativePathResolver;
  }

  @Override
  public Map<String, XMLizable> getImportCache() {
    return importCache;
  }

  public Map<String, Set<String>> getImportUrlsCache() {
    return importUrlsCache;
  }
  
  @Override
  public <N> Class<N> loadUserClass(String name) throws ClassNotFoundException {
    ClassLoader cl = getExtensionClassLoader();
    if (cl == null) {
      cl = this.getClass().getClassLoader();
    }
    return (Class<N>) Class.forName(name, true, cl);
  }
  
  public static Class<?> loadUserClass(String name, ResourceManager rm) throws ClassNotFoundException {
    return (rm == null) 
             ? Class.forName(name, true, ResourceManager_impl.class.getClassLoader())
             : rm.loadUserClass(name);
  }
  
  public static Class<?> loadUserClassOrThrow(String name, ResourceManager rm, ResourceSpecifier aSpecifier) 
      throws ResourceInitializationException {
    try {
      return (rm == null) 
               ? Class.forName(name, true, ResourceManager_impl.class.getClassLoader())
               : rm.loadUserClass(name);
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException(
          ResourceInitializationException.CLASS_NOT_FOUND, new Object[] { name,
              aSpecifier.getSourceUrlString() }, e);
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.ResourceManager#destroy()
   */
  @Override
  public void destroy() {
    boolean alreadyDestroyed = isDestroyed.getAndSet(true);
    if (alreadyDestroyed) {
      return;
    }
    
    for (ResourceRegistration rr : mInternalResourceRegistrationMap.values()) {
      if (rr.resource instanceof Resource) {
        ((Resource)rr.resource).destroy();
      }
    }
    
    for (Object r : mParameterizedResourceInstanceMap.values()) {
      if (r instanceof Resource) {
        ((Resource)r).destroy();
      }
    }
    
    // no destroy of caspool at this time
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.ResourceManager#getExternalResources()
   */
  @Override
  public List<Object> getExternalResources() {
    
    List<Object> rs = new ArrayList<>();
    for (ResourceRegistration r : mInternalResourceRegistrationMap.values()) {
      if (!(r instanceof ParameterizedDataResource)) {
        rs.add(r.resource);
      } 
    }
    
    for (Object r : mParameterizedResourceInstanceMap.values()) {
      rs.add(r);
    }
      
    return rs;
  }
  
  
  
  

}
