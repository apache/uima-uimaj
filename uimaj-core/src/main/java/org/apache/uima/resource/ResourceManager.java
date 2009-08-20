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
   */
  public URL resolveRelativePath(String aRelativePath) throws MalformedURLException;

  /**
   * Gets the Resource that has been registered under the specified name.
   * 
   * @param aName
   *          the name of the resource to retrieve
   * 
   * @return the Resource registered under <code>aName</code>, <code>null</code> if none
   *         exists.
   * 
   * @throws ResourceInitializationException
   *           if the requested resource could not be initialized. A common cause is that it
   *           requires parameters and the {@link #getResource(String,String[])} method should have
   *           been called instead of this method.
   */
  public Object getResource(String aName) throws ResourceAccessException;

  /**
   * Gets an instance of a parameterized Resource. An example of a parameterized Resource is a
   * dictionary whose data depends on a specified language identifier.
   * 
   * @param aName
   *          the name of the resource to retrieve
   * @param aParams
   *          the parameters determining which particular instance is returned
   * 
   * @return the requested Resource, <code>null</code> if there is no resource registered under
   *         the name <code>aName</code>.
   * 
   * @throws ResourceInitializationException
   *           if there is a resource registered under <code>aName</code> but it could not be
   *           instantiated for the specified parameters.
   */
  public Object getResource(String aName, String[] aParams) throws ResourceAccessException;

  /**
   * Gets the Class of the Resource that will be returned by a call to {@link #getResource(String)}
   * or {@link #getResource(String,String[])}.
   * 
   * @param aName
   *          the name of a resource
   * 
   * @return the Class for the resource named <code>aName</code>, <code>null</code> if there is
   *         no resource registered under that name.
   */
  public Class<? extends Resource> getResourceClass(String aName);

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
   * Resolves a component's external resource dependencies using this resource manager. Throws an
   * exception if any required dependencies are not satisfied.
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
   * @return A map from absolute URL to the XMLizable object that was parsed from thar URL
   */
  public Map<String,XMLizable> getImportCache();
}
