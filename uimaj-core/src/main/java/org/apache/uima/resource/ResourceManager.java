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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.util.XMLizable;

/**
 * A <code>ResourceManager</code> holds a collection of {@link org.apache.uima.resource.Resource}
 * objects, each registered under a specified key.
 * 
 * 
 */
public interface ResourceManager {

  /**
   * Gets the data path used to resolve relative paths. More than one directory may be specified by
   * separating them with the System <code>path.separator</code> character (; on windows, : on
   * UNIX).
   * 
   * @return the data path
   */
  public String getDataPath();

  /**
   * Sets the data path used to resolve relative paths. More than one directory may be specified by
   * separating them with the System <code>path.separator</code> character (; on windows, : on
   * UNIX). The elements of this path may be URLs or File paths.
   * 
   * @param aPath
   *          the data path
   * 
   * @throws MalformedURLException
   *           if an element of the path is neither a valid URL or a valid file path
   */
  public void setDataPath(String aPath) throws MalformedURLException;

  /**
   * Attempts to resolve a relative path to an absolute path using the same mechanism that the
   * ResourceManager uses to find resources -- a lookup in the datapath followed by a lookup in the
   * classpath.
   * 
   * @param aRelativePath
   *          a relative URL or file path
   * 
   * @return the absolute URL of an actual file in the datapath or classpath, null if no file
   *         matching <code>aRelativePath</code> is found.
   * @throws MalformedURLException if the path cannot be converted to a URL
   */
  public URL resolveRelativePath(String aRelativePath) throws MalformedURLException;

  /**
   * Gets the instance of the implementation object for a resource that has been registered under the specified name.
   * 
   * @param aName
   *          the name of the resource to retrieve
   * 
   * @return the instance of the implementation object for the resource, registered under <code>aName</code>, 
   *         <code>null</code> if none  exists.
   * 
   * @throws ResourceAccessException
   *           if the requested resource could not be initialized. A common cause is that it
   *           requires parameters and the {@link #getResource(String,String[])} method should have
   *           been called instead of this method.
   * @throws ResourceAccessException tbd
   */
  public Object getResource(String aName) throws ResourceAccessException;

  /**
   * Returns one of two kinds of objects (or null):
   *   - an instance of the implementation object for a resource, that has 
   *     been loaded with a DataResource resource produced by the resource given the aParms
   *     
   *   - (if there is no implementation defined for this resource) 
   *     returns an instance of the DataResource, itself, produced by the resource given the aParms
   *    
   *   An example of a parameterized Resource is a
   *     dictionary whose data depend on a specified language identifier.
   *   
   *   If the implementation object class exists, but no instance has been 
   *   created (yet) for the particular data resource corresponding to the parameters,
   *   then this method will create and register a new instance and call its
   *   load() api using the data resource corresponding to the parameters, and
   *   return that.
   * 
   * @param aName
   *          the name of the parameterized resource to retrieve
   * @param aParams
   *          the parameters determining which particular instance is returned
   *          and specifying a particular DataResource instance to use in initializing
   *          the implementation of the resource (if there is an implementation).
   *          
   *          If there is no implementation, the DataResource instance 
   *          produced from the named Resource given these
   *          parameters is returned instead.
   * 
   * @return one of two kinds of objects (or null):
   *         an instance of the requested implementation of the named resource where that instance 
   *         has been initialized by calling its load method with the DataResource instance produced
   *         from the Resource given aParams,
   *         
   *         or, (if the named resource has no implementation) the DataResource instance 
   *         corresponding to the named Resource, given aParams, 
   *         
   *         or if no resource with this name exists, <code>null</code>.  
   * 
   * @throws ResourceAccessException
   *           if there is a resource registered under <code>aName</code> but it could not be
   *           instantiated for the specified parameters.
   */
  public Object getResource(String aName, String[] aParams) throws ResourceAccessException;

  /**
   * Gets the Class of the Resource that will be returned by a call to {@link #getResource(String)}
   * or {@link #getResource(String,String[])}.
   * 
   * For those resource specifications which include an implementation class, this call returns that class.
   * 
   * @param aName
   *          the name of a resource
   * @param <N> The generic type for the returned class
   * 
   * @return the Class for the resource named <code>aName</code>, <code>null</code> if there is
   *         no resource registered under that name.
   */
  public <N> Class<N> getResourceClass(String aName);

  /**
   * Retrieves the URL to the named resource. This can be used, for example, to locate configuration
   * or authority files.
   * 
   * @param aKey
   *          the key by which the resource is identified. If this key was declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor, then the resource
   *          manager is used to locate the resource. If not, the key is assumed to be the resource
   *          name and is looked up in the {@link #getDataPath() data path} or in the class path
   *          using {@link java.lang.ClassLoader#getResource(String)}.
   * 
   * @return the <code>URL</code> at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public URL getResourceURL(String aKey) throws ResourceAccessException;

  /**
   * Retrieves an InputStream for reading from the named resource. This can be used, for example, to
   * read configuration or authority files.
   * 
   * @param aKey
   *          the key by which the resource is identified. If this key was declared in the
   *          &lt;externalResourceDependencies&gt; section of the annotator's descriptor, then the
   *          resource manager is used to locate the resource. If not, the key is assumed to be the
   *          resource name and is looked up in the {@link #getDataPath() data path} or in the class
   *          path using {@link java.lang.ClassLoader#getResource(String)}.
   * 
   * @return an <code>InputStream</code> for reading from the named resource, <code>null</code>
   *         if the named resource could not be found. It is the caller's responsibility to close
   *         this stream once it is no longer needed.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public InputStream getResourceAsStream(String aKey) throws ResourceAccessException;

  /**
   * Retrieves the URL to the named resource. This can be used, for example, to locate configuration
   * or authority files. This version of this method takes an array of parameters used to further
   * identify the resource. This can be used, for example, with resources that vary depending on the
   * language of the document being analyzed.
   * 
   * @param aKey
   *          the key by which the resource is identified. If this key was declared in the
   *          &lt;externalResourceDependencies&gt; section of the annotator's descriptor, then the
   *          resource manager is used to locate the resource. If not, the key is assumed to be the
   *          resource name and is looked up in the {@link #getDataPath() data path} or in the class
   *          path using {@link java.lang.ClassLoader#getResource(String)}.
   * @param aParams
   *          parameters used to further identify the resource
   * 
   * @return the <code>URL</code> at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public URL getResourceURL(String aKey, String[] aParams) throws ResourceAccessException;

  /**
   * Retrieves an InputStream for reading from the named resource. This can be used, for example, to
   * read configuration or authority files. This version of this method takes an array of parameters
   * used to further identify the resource. This can be used, for example, with resources that vary
   * depending on the language of the document being analyzed.
   * 
   * @param aKey
   *          the key by which the resource is identified. If this key was declared in the
   *          &lt;externalResourceDependencies&gt; section of the annotator's descriptor, then the
   *          resource manager is used to locate the resource. If not, the key is assumed to be the
   *          resource name and is looked up in the {@link #getDataPath() data path} or in the class
   *          path using {@link java.lang.ClassLoader#getResource(String)}.
   * @param aParams
   *          parameters used to further identify the resource
   * 
   * @return an <code>InputStream</code> for reading from the named resource, <code>null</code>
   *         if the named resource could not be found. It is the caller's responsibility to close
   *         this stream once it is no longer needed.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public InputStream getResourceAsStream(String aKey, String[] aParams)
          throws ResourceAccessException;

  /**
   * Initializes all external resources declared in a ResourceCreationSpecifier.
   * Multi-threading: may be called on multiple threads.  
   * 
   *   Initialization should be done once, on the first call
   * 
   * External resources have a Container class representing the resource, 
   * which are instances of Resource.
   * 
   * This may act as the implementation class, or they may also have a
   * separately specified implementation class, which may or may not implement Resource.
   * 
   * As part of the initialization of the Container class, 
   * by default, External Resource Bindings are processed to hook them up
   * with defined External Resources, using the default implementation
   * of resolveAndValidateResourceDependencies.
   *     
   * @param aConfiguration
   *          the ResourceManagerConfiguration containing resource declarations and bindings
   * @param aQualifiedContextName
   *          qualified name of UimaContext for the component (e.g. analysis engine) that is
   *          declaring these external resources
   * @param aAdditionalParams
   *          additional parameters to be passed to resource initialize methods
   * 
   * @throws ResourceInitializationException
   *           if an initialization failure occurs
   */
  public void initializeExternalResources(ResourceManagerConfiguration aConfiguration,
          String aQualifiedContextName, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException;

  /**
   * Resolves a component's external resource dependencies (bindings) using this resource manager.
   * 
   * The default implementation has special defaulting logic:
   * 
   *   If a binding specifies a non-existing resource, 
   *   an attempt is made to interpret the key as a file name, looked up 
   *   using the current context for relative path resolution.  
   *     - If successfully found, a FileResourceSpecifier is created using the file
   *       and used as the implementing class. 
   * 
   *   If no resource can be found at all, then unless the dependency is marked "optional", 
   *   an ResourceInitializationException is thrown.
   * 
   * Multi-threading: may be called on multiple threads, repeatedly for the same set of resources.
   * Implementations should recognize this and skip repeated resolutions.
   * 
   * @param aDependencies
   *          declarations of a component's dependencies on external resources
   * @param aQualifiedContextName
   *          qualified name of UimaContext for the component (e.g. analysis engine) that is
   *          declaring these dependencies
   * 
   * @throws ResourceInitializationException
   *           if a required dependency is not satisfied
   */
  public void resolveAndValidateResourceDependencies(ExternalResourceDependency[] aDependencies,
          String aQualifiedContextName) throws ResourceInitializationException;

  /**
   * Sets the classpath for the UIMA extension ClassLoader and specifies if the extension
   * ClassLoader should also be used to resolve resources.
   * 
   * @param classpath
   *          extension ClassLoader classpath
   * @param resolveResource
   *          if true ClassLoad resolves resources
   * 
   * @throws MalformedURLException
   *           if a malformed URL has occurred in the classpath string.
   */
  public void setExtensionClassPath(String classpath, boolean resolveResource)
          throws MalformedURLException;

  /**
   * Sets the classpath for the UIMA extension ClassLoader and specifies if the extension
   * ClassLoader should also be used to resolve resources. Also allows a parent ClassLoader to be
   * specified.
   * 
   * @param parent
   *          parent ClassLoader for the extension ClassLoader
   * @param classpath
   *          extension ClassLoader classpath
   * @param resolveResource
   *          if true ClassLoad resolves resources
   * 
   * @throws MalformedURLException
   *           if a malformed URL has occurred in the classpath string.
   */
  public void setExtensionClassPath(ClassLoader parent, String classpath, boolean resolveResource)
          throws MalformedURLException;

  /**
   * Returns the UIMA extension class loader.
   * 
   * @return ClassLoader - returns the UIMA extension class loader of null if it is not available.
   */
  public ClassLoader getExtensionClassLoader();

  /**
   * Gets the CasManager, which manages the creation and pooling of CASes.
   * @return the CasManager
   */
  public CasManager getCasManager();

  /**
   * Sets the CasManager, which manages the creation and pooling of CASes.
   * This method does not normally need to be called by an application.  It allows
   * a custom CAS Manager implementation to be substituted for the default one,
   * which may be useful when embedding UIMA in other middleware where a different
   * CAS Manager implementation may be desired.
   * <p>
   * This method can only be called once, and must be called before creating any 
   * AnalysisEngines that use this ResourceManager.  An Exception will be thrown if this 
   * method is called twice or is called after {@link #getCasManager()} has already been called 
   * (which happens during AE initialization).
   * 
   * @param aCasManager CAS Manager instance to plug in
   */
  public void setCasManager(CasManager aCasManager);
  
  /**
   * Gets a cache of imported descriptors, so that the parsed objects can be reused if the
   * same URL is imported more than once.
   * @return A map from absolute URL to the XMLizable object that was parsed from that URL
   * @deprecated  Intended just for internal use.
   */
  @Deprecated
  public Map<String,XMLizable> getImportCache();
  
  /**
   * Loads a user class using either the UIMA extension class loader (if specified) or 
   * the loader the UIMA framework is running in.
   * @param name the class to load
   * @param <N> the generic type for the returned class
   * @return the class
   * @throws ClassNotFoundException -
   */
  public <N> Class<N> loadUserClass(String name) throws ClassNotFoundException;
  
  /**
   * Frees all resources held by this ResourceManager, and marks the ResourceManager as having been destroyed.
   * A destroyed ResourceManager will throw an exception if an attempt is made to continue using it.
   * 
   * Resources managed by a ResourceManager include all of the external shared Resources and a CAS Pool.
   * The Resources managed by this manager will have their destroy() methods called, as part of the
   * execution of this API.
   * 
   * The framework does not call this method; it is up to the containing application to decide if and when
   * a ResourceManager instance should be destroyed.  This is because the containing application is the only
   * knowledgeable source; for example a single ResourceManager might be used for multiple UIMA Pipelines.
   */
  public void destroy();
  
  /**
   * 
   * @return a List of External Shared Resource instances instantiated by this Resource Manager.
   *         For parameterized resources, those which have been asked for (having unique parameter sets) 
   *         are included.
   */
  public List<Object> getExternalResources();
}
