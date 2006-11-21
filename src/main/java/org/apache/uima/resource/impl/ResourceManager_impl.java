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
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.resource.CasManager;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.ParameterizedDataResource;
import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.util.Level;

/**
 * Reference implementation of {@link org.apache.uima.resource.ResourceManager}.
 * 
 * 
 */
public class ResourceManager_impl implements ResourceManager {
  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * Object used for resolving relative paths. This is built by parsing the data path.
   */
  private RelativePathResolver mRelativePathResolver;

  /**
   * Map from qualified key names (declared in resource dependency XML) to Resource objects.
   */
  private Map mResourceMap = Collections.synchronizedMap(new HashMap());

  /**
   * Internal map from resource names (declared in resource declaration XML) to ResourceRegistration
   * objects. Used during initialization only.
   */
  private Map mInternalResourceRegistrationMap = Collections.synchronizedMap(new HashMap());

  /**
   * Map from String keys to Class objects. For ParameterizedResources only, stores the
   * implementation class corresponding to each resource name.
   */
  private Map mParameterizedResourceImplClassMap = Collections.synchronizedMap(new HashMap());

  /**
   * Internal map from resource names (declared in resource declaration XML) to Class objects. Used
   * internally during resource initialization.
   */
  private Map mInternalParameterizedResourceImplClassMap = Collections
                  .synchronizedMap(new HashMap());

  /**
   * Map from ArrayList(0:String,1:DataResource) keys to Resource objects. For
   * ParameterizedResources only, stores the DataResources that have already been encountered, and
   * the Resources that have been instantiated therefrom.
   */
  private Map mParameterizedResourceInstanceMap = Collections.synchronizedMap(new HashMap());

  /**
   * UIMA extension ClassLoader. ClassLoader is created if an extension classpath is specified at
   * the ResourceManager
   */
  private UIMAClassLoader uimaCL = null;

  /** CasManager - manages creation and pooling of CASes. */
  private CasManager mCasManager = new CasManager_impl(this);

  /**
   * Creates a new <code>ResourceManager_impl</code>.
   */
  public ResourceManager_impl() {
    mRelativePathResolver = new RelativePathResolver_impl();
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#setExtensionClassPath(java.lang.String, boolean)
   */
  public void setExtensionClassPath(String classpath, boolean resolveResource)
                  throws MalformedURLException {
    // create UIMA extension ClassLoader with the given classpath
    uimaCL = new UIMAClassLoader(classpath, this.getClass().getClassLoader());

    if (resolveResource) {
      // set UIMA extension ClassLoader also to resolve resources
      mRelativePathResolver.setPathResolverClassLoader(uimaCL);
    }
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#setExtensionClassPath(ClassLoader,java.lang.String,
   *      boolean)
   */
  public void setExtensionClassPath(ClassLoader parent, String classpath, boolean resolveResource)
                  throws MalformedURLException {
    // create UIMA extension ClassLoader with the given classpath
    uimaCL = new UIMAClassLoader(classpath, parent);

    if (resolveResource) {
      // set UIMA extension ClassLoader also to resolve resources
      mRelativePathResolver.setPathResolverClassLoader(uimaCL);
    }
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#getExtensionClassLoader()
   */
  public ClassLoader getExtensionClassLoader() {
    return uimaCL;
  }

  /**
   * Creates a new <code>ResourceManager_impl</code> with a custom ClassLoader to use for locating
   * resources.
   */
  public ResourceManager_impl(ClassLoader aClassLoader) {
    mRelativePathResolver = new RelativePathResolver_impl(aClassLoader);
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#getDataPath()
   */
  public String getDataPath() {
    return mRelativePathResolver.getDataPath();
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#setDataPath(String)
   */
  public void setDataPath(String aPath) throws MalformedURLException {
    mRelativePathResolver.setDataPath(aPath);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#resolveRelativePath(java.lang.String)
   */
  public URL resolveRelativePath(String aRelativePath) throws MalformedURLException {
    URL relativeUrl;
    try {
      relativeUrl = new URL(aRelativePath);
    } catch (MalformedURLException e) {
      relativeUrl = new URL("file", "", aRelativePath);
    }
    return mRelativePathResolver.resolveRelativePath(relativeUrl);
  }

  /**
   * @see org.apache.uima.resource.ResourceManager#getResource(String)
   */
  public Object getResource(String aName) throws ResourceAccessException {
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
  public Object getResource(String aName, String[] aParams) throws ResourceAccessException {
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
    ArrayList nameAndResource = new ArrayList();
    nameAndResource.add(aName);
    nameAndResource.add(dr);
    Object resourceInstance = mParameterizedResourceInstanceMap.get(nameAndResource);
    if (resourceInstance != null) {
      return resourceInstance;
    }

    // We haven't encountered this before. See if we need to instantiate a
    // SharedResourceObject
    Class sharedResourceObjectClass = (Class) mParameterizedResourceImplClassMap.get(aName);
    if (sharedResourceObjectClass != null) {
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

  /**
   * @see org.apache.uima.resource.ResourceManager#getResourceClass(java.lang.String)
   */
  public Class getResourceClass(String aName) {
    Object r = mResourceMap.get(aName);
    if (r == null) // no such resource
    {
      return null;
    }

    // if this is a ParameterizedDataResource, look up its class
    if (r instanceof ParameterizedDataResource) {
      Class customResourceClass = (Class) mParameterizedResourceImplClassMap.get(aName);
      if (customResourceClass == null) {
        // return the default class
        return DataResource_impl.class;
      }
      return customResourceClass;
    } else {
      // return r's Class
      return r.getClass();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getResourceAsStream(java.lang.String,
   *      java.lang.String[])
   */
  public InputStream getResourceAsStream(String aKey, String[] aParams)
                  throws ResourceAccessException {
    try {
      // see if this resource is registered in the ResourceManager
      Object r = getResource(aKey, aParams);
      // if so, and if it is a DataResource, use its InputStream
      if (r != null && r instanceof DataResource) {
        return ((DataResource) r).getInputStream();
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new ResourceAccessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getResourceAsStream(java.lang.String)
   */
  public InputStream getResourceAsStream(String aKey) throws ResourceAccessException {
    try {
      // see if this resource is registered in the ResourceManager
      Object r = getResource(aKey);
      // if so, and if it is a DataResource, use its InputStream
      if (r != null && r instanceof DataResource) {
        return ((DataResource) r).getInputStream();
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new ResourceAccessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getResourceURL(java.lang.String,
   *      java.lang.String[])
   */
  public URL getResourceURL(String aKey, String[] aParams) throws ResourceAccessException {
    // try
    // {
    // see if this resource is registered in the ResourceManager
    Object r = getResource(aKey, aParams);
    // if so, and if it is a DataResource, use its URL
    if (r != null && r instanceof DataResource) {
      return ((DataResource) r).getUrl();
    } else {
      return null;
      // //fall back on Relative Path Resolver (searches data path then ClassLoader)
      // URL relativeUrl;
      // try
      // {
      // relativeUrl = new URL(aKey);
      // }
      // catch(MalformedURLException e)
      // {
      // relativeUrl = new URL("file","",aKey);
      // }
      // return mRelativePathResolver.resolveRelativePath(relativeUrl);
    }
    // }
    // catch(MalformedURLException e)
    // {
    // throw new ResourceAccessException(e);
    // }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getResourceURL(java.lang.String)
   */
  public URL getResourceURL(String aKey) throws ResourceAccessException {
    // try
    // {
    // see if this resource is registered in the ResourceManager
    Object r = getResource(aKey);
    // if so, and if it is a DataResource, use its URL
    if (r != null && r instanceof DataResource) {
      return ((DataResource) r).getUrl();
    } else {
      return null;
      // //fall back on Relative Path Resolver (searches data path then ClassLoader)
      // // String keyNoContext = stripContext(aKey); Stripping to last / doesn't work - also strips
      // part of path!!!
      // URL relativeUrl;
      // try
      // {
      // relativeUrl = new URL(aKey);//keyNoContext);
      // }
      // catch(MalformedURLException e)
      // {
      // relativeUrl = new URL("file","",aKey);//keyNoContext);
      // }
      // return mRelativePathResolver.resolveRelativePath(relativeUrl);
    }
    // }
    // catch(MalformedURLException e)
    // {
    // throw new ResourceAccessException(e);
    // }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#initializeExternalResources(org.apache.uima.resource.metadata.ResourceManagerConfiguration,
   *      java.lang.String, java.util.Map)
   */
  public void initializeExternalResources(ResourceManagerConfiguration aConfiguration,
                  String aQualifiedContextName, Map aAdditionalParams)
                  throws ResourceInitializationException {
    // register resources
    ExternalResourceDescription[] resources = aConfiguration.getExternalResources();
    for (int i = 0; i < resources.length; i++) {
      String name = resources[i].getName();

      // check for existing resource registration under this name
      ResourceRegistration registration = (ResourceRegistration) mInternalResourceRegistrationMap
                      .get(name);
      if (registration == null) {
        registerResource(name, resources[i], aQualifiedContextName, aAdditionalParams);
      } else {
        // log a message if the resource definitions are not identical
        if (!registration.description.equals(resources[i])) {
          // if the resource was overridden in an enclosing aggregate, use an INFO level message.
          // if not (e.g. sibling annotators declare the same resource name), it's a WARNING.
          if (aQualifiedContextName.startsWith(registration.definingContext)) {
            UIMAFramework.getLogger().logrb(
                            Level.CONFIG,
                            ResourceManager_impl.class.getName(),
                            "initializeExternalResources",
                            LOG_RESOURCE_BUNDLE,
                            "UIMA_overridden_resource__CONFIG",
                            new Object[] { name, registration.definingContext,
                                aQualifiedContextName });
          } else {
            UIMAFramework.getLogger().logrb(
                            Level.WARNING,
                            ResourceManager_impl.class.getName(),
                            "initializeExternalResources",
                            LOG_RESOURCE_BUNDLE,
                            "UIMA_duplicate_resource_name__WARNING",
                            new Object[] { name, aQualifiedContextName,
                                registration.definingContext });
          }
        }
      }
    }
    // apply bindings
    ExternalResourceBinding[] bindings = aConfiguration.getExternalResourceBindings();
    for (int i = 0; i < bindings.length; i++) {
      ResourceRegistration registration = (ResourceRegistration) mInternalResourceRegistrationMap
                      .get(bindings[i].getResourceName());
      if (registration == null) {
        throw new ResourceInitializationException(
                        ResourceInitializationException.UNKNOWN_RESOURCE_NAME, new Object[] {
                            bindings[i].getResourceName(), bindings[i].getSourceUrlString() });
      }
      mResourceMap.put(aQualifiedContextName + bindings[i].getKey(), registration.resource);
      // record the link from key to resource class (for parameterized resources only)
      mParameterizedResourceImplClassMap
                      .put(aQualifiedContextName + bindings[i].getKey(),
                                      mInternalParameterizedResourceImplClassMap.get(bindings[i]
                                                      .getResourceName()));

    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#resolveAndValidateResourceDependencies(org.apache.uima.resource.ExternalResourceDependency[],
   *      java.lang.String)
   */
  public void resolveAndValidateResourceDependencies(ExternalResourceDependency[] aDependencies,
                  String aQualifiedContextName) throws ResourceInitializationException {
    for (int i = 0; i < aDependencies.length; i++) {
      // get resource
      String qname = aQualifiedContextName + aDependencies[i].getKey();
      Object resource = mResourceMap.get(qname);
      if (resource == null) {
        // no resource found
        // try to look up in classpath/datapath
        URL relativeUrl;
        try {
          relativeUrl = new URL("file", "", aDependencies[i].getKey());
        } catch (MalformedURLException e) {
          throw new ResourceInitializationException(e);
        }
        URL absUrl = mRelativePathResolver.resolveRelativePath(relativeUrl);
        if (absUrl != null) {
          // found - create a DataResource object and store it in the mResourceMap
          FileResourceSpecifier spec = new FileResourceSpecifier_impl();
          spec.setFileUrl(absUrl.toString());
          resource = UIMAFramework.produceResource(spec, null);
          mResourceMap.put(qname, resource);
        }
      }
      if (resource == null) // still no resource found - throw exception if required
      {
        if (!aDependencies[i].isOptional()) {
          throw new ResourceInitializationException(
                          ResourceInitializationException.RESOURCE_DEPENDENCY_NOT_SATISFIED,
                          new Object[] { aDependencies[i].getKey(),
                              aDependencies[i].getSourceUrlString() });
        }
      } else {
        // make sure resource exists and implements the correct interface
        try {
          if (aDependencies[i].getInterfaceName() != null
                          && aDependencies[i].getInterfaceName().length() > 0) {
            // get UIMA extension ClassLoader if available
            ClassLoader cl = getExtensionClassLoader();
            Class theInterface = null;

            if (cl != null) {
              // use UIMA extension ClassLoader to load the class
              theInterface = cl.loadClass(aDependencies[i].getInterfaceName());
            } else {
              // use application ClassLoader to load the class
              theInterface = Class.forName(aDependencies[i].getInterfaceName());
            }

            Class resourceClass = getResourceClass(qname);
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
                              aDependencies[i].getInterfaceName(),
                              aDependencies[i].getSourceUrlString() });
        }
      }
    }
  }

  /**
   * Instantiates a resource and inserts it in the internal resource map.
   */
  private void registerResource(String aName, ExternalResourceDescription aResourceDescription,
                  String aDefiningContext, Map aResourceInitParams)
                  throws ResourceInitializationException {
    // add the relative path resolver to the resource init. params
    Map initParams = (aResourceInitParams == null) ? new HashMap() : new HashMap(
                    aResourceInitParams);
    initParams.put(DataResource.PARAM_RELATIVE_PATH_RESOLVER, mRelativePathResolver);

    // create the initial resource using the resource factory
    Object r = UIMAFramework.produceResource(aResourceDescription.getResourceSpecifier(),
                    initParams);

    // load implementation class (if any) and ensure that it implements
    // SharedResourceObject
    String implementationName = aResourceDescription.getImplementationName();
    Class implClass = null;
    if (implementationName != null && implementationName.length() > 0) {
      try {
        // get UIMA extension ClassLoader if available
        ClassLoader cl = getExtensionClassLoader();

        if (cl != null) {
          // use UIMA extension ClassLoader to load the class
          implClass = cl.loadClass(implementationName);
        } else {
          // use application ClassLoader to load the class
          implClass = Class.forName(implementationName);
        }
      } catch (ClassNotFoundException e) {
        throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
                        new Object[] { implementationName,
                            aResourceDescription.getSourceUrlString() }, e);
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
          sro.load((DataResource) r);
          r = sro;
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
      mInternalParameterizedResourceImplClassMap.put(aName, implClass);
    } else
    // it is some other type of Resource
    {
      // it is an error to specify an implementation class in this case
      if (implClass != null) {
        throw new ResourceInitializationException(
                        ResourceInitializationException.NOT_A_DATA_RESOURCE, new Object[] {
                            implClass.getName(), aName, r.getClass().getName(),
                            aResourceDescription.getSourceUrlString() });
      }
    }

    // put resource in internal map for later retrieval
    ResourceRegistration registration = new ResourceRegistration(r, aResourceDescription,
                    aDefiningContext);
    mInternalResourceRegistrationMap.put(aName, registration);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceManager#getCasManager()
   */
  public CasManager getCasManager() {
    return mCasManager;
  }

  static class ResourceRegistration {
    Object resource;

    ExternalResourceDescription description;

    String definingContext;

    ResourceRegistration(Object resource, ExternalResourceDescription description,
                    String definingContext) {
      this.resource = resource;
      this.description = description;
      this.definingContext = definingContext;
    }
  }
}
