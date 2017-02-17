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

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.resource.impl.RelativePathResolver_impl;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Settings;

/**
 * Implementation base class for {@link org.apache.uima.resource.Resource}s. Provides access to
 * resource metadata and the UIMA Context, which in turn provides access to framework facilities
 * such as logging and resource management.
 * 
 * 
 */
public abstract class Resource_ImplBase implements Resource {

  /**
   * Metadata for this Resource.
   */
  private ResourceMetaData mMetaData;

  /**
   * Admin interface to the UIMA Context
   */
  private UimaContextAdmin mUimaContextAdmin;

  /**
   * Whether this Resource's {@link #initialize(ResourceSpecifier,Map)} method has been called.
   */
  private boolean mInitialized = false;

  /**
   * @see org.apache.uima.resource.Resource#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   *      
   * multi-thread safe, given that each instance of this class is only called on one thread, once.
   * The critical parts that update shared information (in shared uima context) are inside a synchronize block
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {

    // get name of resource, to be used in error messages
    final ResourceMetaData metadata1 = getMetaData();
    String name = (metadata1 == null) ? getClass().getName() : metadata1.getName();

    // check for repeat initialization
    if (mInitialized) {
      throw new UIMA_IllegalStateException(UIMA_IllegalStateException.RESOURCE_ALREADY_INITIALIZED,
              new Object[] { name });
    }

    // is there a UIMAContext provided in the aAdditionalParams map?
    // if so, use it - it could be a shared context for scale-up of the same resource
    if (aAdditionalParams != null) {
      mUimaContextAdmin = (UimaContextAdmin) aAdditionalParams.get(PARAM_UIMA_CONTEXT);
    }
    if (mUimaContextAdmin == null) {// no, we have to create one 
    
      // skip this part if initializing an external resource
      // https://issues.apache.org/jira/browse/UIMA-5153
      if (!(aSpecifier instanceof ConfigurableDataResourceSpecifier) &&
          !(aSpecifier instanceof FileLanguageResourceSpecifier) &&
          !(aSpecifier instanceof FileResourceSpecifier)) {
        // get or create ResourceManager
        ResourceManager resMgr = null;
        if (aAdditionalParams != null) {
          resMgr = (ResourceManager) aAdditionalParams.get(PARAM_RESOURCE_MANAGER);
        }
        if (resMgr == null) {
          resMgr = UIMAFramework.newDefaultResourceManager();
        }
  
        // get a Logger for this class and set its ResourceManager so that
        // UIMA extension ClassLoader is used to locate message digests.
        Logger logger = UIMAFramework.getLogger(this.getClass());
        logger.setResourceManager(resMgr);
        
        ConfigurationManager configMgr = null;
        if (aAdditionalParams != null) {
          configMgr = (ConfigurationManager)aAdditionalParams.get(PARAM_CONFIG_MANAGER);
        }
        if (configMgr == null) {
          configMgr = UIMAFramework.newConfigurationManager();
        }
  
        // create and initialize UIMAContext
        mUimaContextAdmin = UIMAFramework.newUimaContext(logger, resMgr, configMgr);
      }
    } else {
      // configure logger of the UIMA context so that class-specific logging
      // levels and UIMA extension classLoader will work
      // get a Logger for this class and set its ResourceManager so that
      // UIMA extension ClassLoader is used to locate message digests.
      Logger logger = UIMAFramework.getLogger(this.getClass());
      logger.setResourceManager(mUimaContextAdmin.getResourceManager());
      mUimaContextAdmin.setLogger(logger);
    }

    // if this is a local resource (instantiated from a ResourceCreationSpecifier),
    // initialize the ResourceManager and UIMA Context.
    if (aSpecifier instanceof ResourceCreationSpecifier) {
      // resolve imports in the metadata
      ResourceMetaData metadata = ((ResourceCreationSpecifier) aSpecifier).getMetaData();
      name = metadata.getName();
      try {
        // the resolveImports method has synch block around updates to the metadata
        metadata.resolveImports(getResourceManager());
      } catch (InvalidXMLException e) {
        throw new ResourceInitializationException(e);
      }
      // store Resource metadata so it can be retrieved via getMetaData() method
      setMetaData(metadata);
      
      // Check if a Settings object for the external overrides has been provided in the additional
      // parameters map.  If not and not already set from the parent UimaContext then create one 
      // (for the root context) from the system defaults
      Settings externalOverrides = aAdditionalParams == null ? null : 
                    (Settings) aAdditionalParams.get(Resource.PARAM_EXTERNAL_OVERRIDE_SETTINGS);
      if (externalOverrides != null) {
        mUimaContextAdmin.setExternalOverrides(externalOverrides);
      } else {
        // synch around test/set of the (possibly shared) uima-context info about external param overrides
        synchronized(mUimaContextAdmin) {
          if (mUimaContextAdmin.getExternalOverrides() == null) {
            externalOverrides = UIMAFramework.getResourceSpecifierFactory().createSettings();  // i.e. new Settings_impl()
            try {
              externalOverrides.loadSystemDefaults();
            } catch (ResourceConfigurationException e) {
              throw new ResourceInitializationException(ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR,
                      new Object[] { name, metadata.getSourceUrlString() }, e);
            }
            mUimaContextAdmin.setExternalOverrides(externalOverrides);
          }
        }
      }
      
      // initialize configuration
      try {
        // createContext checks and skips repeated calls with same args (on different threads, for example)
        mUimaContextAdmin.getConfigurationManager().createContext(
                mUimaContextAdmin.getQualifiedContextName(), getMetaData(), mUimaContextAdmin.getExternalOverrides());
        mUimaContextAdmin.getConfigurationManager().setSession(mUimaContextAdmin.getSession());
      } catch (ResourceConfigurationException e) {
        throw new ResourceInitializationException(
                ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR, new Object[] {
                    name, metadata.getSourceUrlString() }, e);
      }

      // initialize any external resource declared in this descriptor
      // UIMA-5274  Set & restore the UimaContextHolder so that resources created on this thread can use the Settings
      ResourceManagerConfiguration resMgrCfg = ((ResourceCreationSpecifier) aSpecifier)
              .getResourceManagerConfiguration();
      if (resMgrCfg != null) {
        UimaContext prevContext = UimaContextHolder.setContext(mUimaContextAdmin);
        try {
          try {
            resMgrCfg.resolveImports(getResourceManager());
          } catch (InvalidXMLException e) {
            throw new ResourceInitializationException(e);
          }
          if (aAdditionalParams == null) {
            aAdditionalParams = new HashMap<String, Object>();
            aAdditionalParams.put(PARAM_RESOURCE_MANAGER, mUimaContextAdmin.getResourceManager());
          } else {
            if (!aAdditionalParams.containsKey(PARAM_RESOURCE_MANAGER)) {
              // copy in case original is shared on multi-threads, or
              // is unmodifiable
              // and to avoid updating passed - in map
              aAdditionalParams = new HashMap<String, Object>(aAdditionalParams);
              aAdditionalParams.put(PARAM_RESOURCE_MANAGER, mUimaContextAdmin.getResourceManager());
            }
          }
          // initializeExternalResources is synchronized

          // https://issues.apache.org/jira/browse/UIMA-5153
          final HashMap<String, Object> aAdditionalParmsForExtResources = new HashMap<String, Object>(aAdditionalParams); // copy in case
          if (aAdditionalParmsForExtResources.get(PARAM_UIMA_CONTEXT) == null) {
            aAdditionalParmsForExtResources.put(PARAM_UIMA_CONTEXT, mUimaContextAdmin);
          }

          mUimaContextAdmin.getResourceManager().initializeExternalResources(resMgrCfg,
                  mUimaContextAdmin.getQualifiedContextName(), aAdditionalParmsForExtResources);
        } finally {
          UimaContextHolder.setContext(prevContext);
        }
      }

      // resolve and validate this component's external resource dependencies
      ExternalResourceDependency[] resourceDependencies = ((ResourceCreationSpecifier) aSpecifier)
              .getExternalResourceDependencies();
      if (resourceDependencies != null) {
        // resolveAndValidateResourceDependencies is synchronized
        mUimaContextAdmin.getResourceManager().resolveAndValidateResourceDependencies(
                resourceDependencies, mUimaContextAdmin.getQualifiedContextName());
      }
    }
    mInitialized = true;
    return true;
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
  }

  /**
   * @see org.apache.uima.resource.Resource#getMetaData()
   */
  public ResourceMetaData getMetaData() {
    return mMetaData;
  }

  /**
   * Sets the <code>ResourceMetaData</code> object associated with this <code>Resource</code>.
   * Any previously existing metadata will be replaced.
   * <p>
   * Resource subclasses should call this method during initialization in order to set the metadata
   * before any calls to {@link #getMetaData()} are made.
   * 
   * @param aMetaData
   *          metadata to assign to this <code>Resource</code>
   */
  protected void setMetaData(ResourceMetaData aMetaData) {
    mMetaData = aMetaData;
  }

  /**
   * Get the logger for this UIMA framework class.
   * Note that this is NOT the user's logger in the UimaContext
   */
  public Logger getLogger() {
    return UIMAFramework.getLogger(this.getClass());
  }

  /**
   * Set the logger in the current UimaContext for use by user annotators. 
   */
  public void setLogger(Logger aLogger) {
    if (getUimaContext() != null) {
      getUimaContextAdmin().setLogger(aLogger);
    }
  }

  /**
   * @see org.apache.uima.resource.Resource#getResourceManager()
   */
  public ResourceManager getResourceManager() {
    if (getUimaContextAdmin() != null)
      return getUimaContextAdmin().getResourceManager();
    else
      return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.Resource#getUimaContext()
   */
  public UimaContext getUimaContext() {
    return mUimaContextAdmin;
  }

  /**
   * Gets the Admin interface to this Resource's UimaContext.
   */
  public UimaContextAdmin getUimaContextAdmin() {
    return mUimaContextAdmin;
  }

  /**
   * Get the CasManager for this Resource. The CasManager manages the creation and pooling of CASes.
   * 
   * @return the CasManager
   */
  public CasManager getCasManager() {
    return getResourceManager().getCasManager();
  }
  
  public RelativePathResolver getRelativePathResolver(Map<String, Object> aAdditionalParams) {
    RelativePathResolver relPathResolver = null;
    if (aAdditionalParams != null) {
      relPathResolver = (RelativePathResolver) aAdditionalParams.get(DataResource.PARAM_RELATIVE_PATH_RESOLVER);
    }
    if (relPathResolver == null) {
      relPathResolver = new RelativePathResolver_impl();
    }
    return relPathResolver;
  }

}
