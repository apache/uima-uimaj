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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.Session;
import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Settings;

/**
 * Provides access to external resources (other than the CAS). The <code>UimaContext</code>
 * provides UIMA resources (e.g. Annotators, Collection Readers, CAS Consumers, CAS Initializers)
 * with all access to external resources (other than the {@link org.apache.uima.cas.CAS}). Examples
 * include:
 * <ul>
 * <li>Configuration Parameters</li>
 * <li>Logging &amp; Instrumentation Facilities</li>
 * <li>Access to External Analysis Resources, such as dictionary files</li>
 * </ul>
 * 
 * 
 */
public interface UimaContext {

  /**
   * Retrieves the value for a configuration parameter that is not defined in any group or is
   * defined in the default group.
   * <p>
   * This method returns <code>null</code> if the parameter is optional and has not been assigned
   * a value. (For mandatory parameters, an exception is thrown during initialization if no value
   * has been assigned.) This method also returns <code>null</code> if there is no declared
   * configuration parameter with the specified name.
   * 
   * @param aParamName
   *          the name of the parameter to look up
   * 
   * @return the value of the parameter with the given name. The caller is expected to know the data
   *         type of the parameter. Returns <code>null</code> if the parameter does not exist or
   *         has not been assigned a value.
   */
  public Object getConfigParameterValue(String aParamName);

  /**
   * Retrieves the value for a configuration parameter in a particular group. If that group contains
   * no value for the specified parameter, the fallback strategy specified by the Analysis Engine's
   * {@link org.apache.uima.resource.metadata.ConfigurationParameterDeclarations#getSearchStrategy()}
   * property will be used. The search strategy can be specified in the descriptor.
   * <p>
   * This method returns <code>null</code> if the parameter is optional and has not been assigned
   * a value. (For mandatory parameters, an exception is thrown during initialization if no value
   * has been assigned.) This method also returns <code>null</code> if there is no declared
   * configuration parameter with the specified name.
   * 
   * @param aGroupName
   *          the name of the group containing the parameter
   * @param aParamName
   *          the name of the parameter to look up
   * 
   * @return the value of the parameter with the given name. The caller is expected to know the data
   *         type of the parameter. Returns <code>null</code> if the parameter does not exist or
   *         has not been assigned a value.
   */
  public Object getConfigParameterValue(String aGroupName, String aParamName);

  /**
   * Gets the names of all configuration parameter groups.
   * 
   * @return an array containing the names of all configuration groups that exist for this
   *         component. Returns an empty array if no groups are declared.
   */
  public String[] getConfigurationGroupNames();

  /**
   * Gets the names of all configuration parameters in the specified group.
   * 
   * @param aGroup
   *          the group name
   * 
   * @return an array containing the names of all configuration parameters declared in
   *         <code>aGroup</code>. Note that this does include parameters with null values.
   *         Returns an empty array if there are none (including if the group does not exist).
   */
  public String[] getConfigParameterNames(String aGroup);

  /**
   * Gets the names of all configuration parameters that are not declared in a group.
   * 
   * @return an array containing the names of all configuration parameters not declared in any
   *         group. Returns an empty array if there are none.
   */
  public String[] getConfigParameterNames();

  /**
   * Get the value of a shared configuration parameter from the external override settings.
   * 
   * @param name - the name of the parameter
   * @return     - the value found in the shared settings file(s), or null if missing.
   * @throws ResourceConfigurationException 
   *                 if the value references an undefined parameter, or the value is an array
   */
  public String getSharedSettingValue(String name) throws ResourceConfigurationException;
  
  /**
  * Get the array of values for a shared configuration parameter from the external override settings.
  * 
  * @param name  - the name of the parameter
  * @return      - an array of values found in the shared settings file(s), or null if missing.
  * @throws ResourceConfigurationException 
  *                  if the value references an undefined parameter, or the value is not an array
  */
  public String[] getSharedSettingArray(String name) throws ResourceConfigurationException;
  
  /**
   * Get the names of all the external override settings available.
   * 
   * @return - an array containing the names of all the external override settings.
   */
  public String[] getSharedSettingNames();
  
  /**
   * Gets the <code>Settings</code> used for external parameter overrides
   *  
   * @return the Settings object
   */
  public Settings getExternalOverrides();
  
  /**
   * Gets the <code>Logger</code> to which log output will be sent. UIMA components should use
   * this facility rather than writing to their own log files (or to stdout).
   * 
   * @return an instance of a logger for use by this annotator.
   */
  public Logger getLogger();

  /**
   * Gets the <code>InstrumentationFacility</code> that a component can use to record information
   * about its performance.
   * 
   * @return an instance of the instrumentation facility
   */
  public InstrumentationFacility getInstrumentationFacility();

  /**
   * Retrieves the URL to the named resource. This can be used, for example, to locate configuration
   * or authority files. The resource should be declared in the &lt;externalResourceDependencies&gt;
   * section of the descriptor.
   * <p>
   * Note that if the URL contains spaces may be encoded as %20. The {@link URL#getPath()} method
   * does NOT decode these sequences, therefore it is not safe to call
   * <code>getResourceURL().getPath()</code> and attempt to use the result as a file path.
   * Instead, you may use {@link #getResourceURI(String)} or {@link #getResourceFilePath(String)}.
   * <p>
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * 
   * @return the <code>URL</code> at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public URL getResourceURL(String aKey) throws ResourceAccessException;

  /**
   * Retrieves the URI to the named resource. This can be used, for example, to locate configuration
   * or authority files. The resource should be declared in the &lt;externalResourceDependencies&gt;
   * section of the descriptor.
   * <p>
   * This method is safer than {@link #getResourceURL(String)} in its treatment of file paths
   * containing spaces. This is because the {@link URI#getPath()} does perform URL decoding of that
   * path (decoding %20 sequences to spaces) whereas {@link URL#getPath()} does not.
   * <p>
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * 
   * @return the <code>URI</code> at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public URI getResourceURI(String aKey) throws ResourceAccessException;

  /**
   * Retrieves the absolute file path to the named resource. This can be used, for example, to
   * locate configuration or authority files. The resource should be declared in the
   * &lt;externalResourceDependencies&gt; section of the descriptor.
   * <p>
   * This only works if the resource is a local file. If the resource is not a local file (for
   * example, it could be an <code>http</code> URL, then an exception will be thrown.
   * <p>
   * URL decoding will be done on the file path, so it is safe to use this method for file paths
   * that contain spaces.
   * 
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * 
   * @return the absolute file path at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @throws ResourceAccessException
   *           if the resource is not a local file, or if a failure occurs in accessing the resource
   */
  public String getResourceFilePath(String aKey) throws ResourceAccessException;

  /**
   * Retrieves an InputStream for reading from the named resource. This can be used, for example, to
   * locate configuration or authority files. The resource should be declared in the
   * &lt;externalResourceDependencies&gt; section of the descriptor.
   * <p>
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
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
   * Retrieves the named resource object. This can be used to acquire references to external
   * resources. The resource should be declared in the &lt;externalResourceDependencies&gt; section
   * of the descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * 
   * @return the object bound to <code>aName</code>, <code>null</code> if none.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public Object getResourceObject(String aKey) throws ResourceAccessException;

  /**
   * Retrieves the URL to the named resource. This can be used, for example, to locate configuration
   * or authority files. The resource should be declared in the &lt;externalResourceDependencies&gt;
   * section of the descriptor.
   * <p>
   * Note that if the URL contains spaces they may be encoded as %20. The {@link URL#getPath()}
   * method does NOT decode these sequences, therefore it is not safe to call
   * <code>getResourceURL().getPath()</code> and attempt to use the result as a file path.
   * Instead, you may use {@link #getResourceURI(String)} or {@link #getResourceFilePath(String)}.
   * <p>
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * <p>
   * This version of this method takes an array of parameters used to further identify the resource.
   * This can be used, for example, with resources that vary depending on the language of the
   * document being analyzed, such as when the &lt;fileLanguageResourceSpecifier&gt; element is used in
   * the component descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * @param aParams
   *          parameters used to further identify the resource. When used to identify the language
   *          for a &lt;fileLanguageResourceSpecifier&gt;, this array should contain a single element,
   *          the ISO language code for the language of the document (e.g. "en", "de").
   * 
   * @return the <code>URL</code> at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public URL getResourceURL(String aKey, String[] aParams) throws ResourceAccessException;

  /**
   * Retrieves the URI to the named resource. This can be used, for example, to locate configuration
   * or authority files. The resource should be declared in the &lt;externalResourceDependencies&gt;
   * section of the descriptor.
   * <p>
   * This method is safer than {@link #getResourceURL(String)} in its treatment of file paths
   * containing spaces. This is because the {@link URI#getPath()} does perform URL decoding of that
   * path (decoding %20 sequences to spaces) whereas {@link URL#getPath()} does not.
   * <p>
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * <p>
   * This version of this method takes an array of parameters used to further identify the resource.
   * This can be used, for example, with resources that vary depending on the language of the
   * document being analyzed, such as when the &lt;fileLanguageResourceSpecifier&gt; element is used in
   * the component descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * @param aParams
   *          parameters used to further identify the resource. When used to identify the language
   *          for a &lt;fileLanguageResourceSpecifier&gt;, this array should contain a single element,
   *          the ISO language code for the language of the document (e.g. "en", "de").
   * 
   * @return the <code>URI</code> at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public URI getResourceURI(String aKey, String[] aParams) throws ResourceAccessException;

  /**
   * Retrieves the absolute file path to the named resource. This can be used, for example, to
   * locate configuration or authority files. The resource should be declared in the
   * &lt;externalResourceDependencies&gt; section of the descriptor.
   * <p>
   * This only works if the resource is a local file. If the resource is not a local file (for
   * example, it could be an <code>http</code> URL, then an exception will be thrown.
   * <p>
   * URL decoding will be done on the file path, so it is safe to use this method for file paths
   * that contain spaces.
   * <p>
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * <p>
   * This version of this method takes an array of parameters used to further identify the resource.
   * This can be used, for example, with resources that vary depending on the language of the
   * document being analyzed, such as when the &lt;fileLanguageResourceSpecifier&gt; element is used in
   * the component descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * @param aParams
   *          parameters used to further identify the resource. When used to identify the language
   *          for a &lt;fileLanguageResourceSpecifier&gt;, this array should contain a single element,
   *          the ISO language code for the language of the document (e.g. "en", "de").
   * 
   * @return the absolute file path at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @throws ResourceAccessException
   *           if the resource is not a local file, or if a failure occurs in accessing the resource
   */
  public String getResourceFilePath(String aKey, String[] aParams) throws ResourceAccessException;

  /**
   * Retrieves an InputStream for reading from the named resource. This can be used, for example, to
   * locate configuration or authority files. The resource should be declared in the
   * &lt;externalResourceDependencies&gt; section of the descriptor.
   * <p>
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * <p>
   * This version of this method takes an array of parameters used to further identify the resource.
   * This can be used, for example, with resources that vary depending on the language of the
   * document being analyzed, such as when the &lt;fileLanguageResourceSpecifier&gt; element is used in
   * the component descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should bd declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * @param aParams
   *          parameters used to further identify the resource. When used to identify the language
   *          for a &lt;fileLanguageResourceSpecifier&gt;, this array should contain a single element,
   *          the ISO language code for the language of the document (e.g. "en", "de").
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
   * Retrieves the named resource object. This can be used to acquire references to external
   * resources. The resource should be declared in the &lt;externalResourceDependencies&gt; section
   * of the descriptor.
   * <p>
   * This version of this method takes an array of parameters used to further identify the resource.
   * This can be used, for example, with resources that vary depending on the language of the
   * document being analyzed, such as when the &lt;fileLanguageResourceSpecifier&gt; element is used in
   * the component descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * @param aParams
   *          parameters used to further identify the resource. When used to identify the language
   *          for a &lt;fileLanguageResourceSpecifier&gt;, this array should contain a single element,
   *          the ISO language code for the language of the document (e.g. "en", "de").
   * 
   * @return the object bound to <code>aName</code>, <code>null</code> if none.
   * 
   * @throws ResourceAccessException
   *           if a failure occurs in accessing the resource
   */
  public Object getResourceObject(String aKey, String[] aParams) throws ResourceAccessException;

  /**
   * Gets the data path used to locate resources. This path may contain more than one directory,
   * separated by the System <code>path.separator</code> character (; on windows, : on UNIX).
   * <p>
   * This method is intended to be used only for integration of legacy or third-party components
   * that have their own resource management facility. If possible, it is recommended that you use
   * the <code>getResoureXXX</code> methods instead.
   * 
   * @return the data path
   */
  public String getDataPath();

  /**
   * Returns the Session object, which can be used to store data that pertains to a particular
   * client session. All data that must persist across requests must be stored in the Session object
   * and NOT in component instance variables. In some service deployments, a single component instance may
   * serve multiple clients.  In that case, the service wrapper may provide a different Session object
   * for each client, and this method would return the appropriate Session object for the component to use
   * for the current call.
   * <p>
   * Note that Session support is NOT implemented in any of the service wrappers (Vinci, SOAP) currently
   * provided in the UIMA SDK.
   * 
   * @return the current Session object
   */
  public Session getSession();

  /**
   * Retrieve actual sofa ID given a symbolic name
   * 
   * @param aSofaName
   *          this component's name for a SofA
   * @return absolute SofA ID
   * 
   * @deprecated As of v2.0, annotators no longer need to explicitly call this method. CAS views can
   *             now be obtained directly by the method {@link CAS#getView(String)}, and the
   *             framework will automatically do the necessary Sofa mappings.
   */
  @Deprecated
  public SofaID mapToSofaID(String aSofaName);

  /**
   * Retrieve the sofa name as known to the component given an absolute Sofa ID.
   * 
   * @param aSofaID
   *          absolute SofA ID
   * @return this component's name for that SofA
   */
  public String mapSofaIDToComponentSofaName(String aSofaID);

  /**
   * @return array of SofaID objects containing mapping of component sofa name to absolute sofa id
   * 
   * @deprecated As of v2.0, annotators no longer need to explicitly call this method. CAS views can
   *             now be obtained directly by the method {@link CAS#getView(String)}, and the
   *             framework will automatically do the necessary Sofa mappings.
   */
  @Deprecated
  public SofaID[] getSofaMappings();

  /**
   * Get an empty CAS. This method can only be called from CAS Multipliers, and typically a CAS
   * Multiplier would call this indirectly through its
   * {@link org.apache.uima.analysis_component.CasMultiplier_ImplBase#getEmptyCAS()} or
   * {@link org.apache.uima.analysis_component.JCasMultiplier_ImplBase#getEmptyJCas()} method.
   * <p>
   * This method may maintain a pool of CASes and may block if none are currently available.
   * 
   * @param aCasInterface
   *          the specific CAS interface that the component wants to use (e.g. CAS or JCas). Must
   *          specify a subtype of {@link AbstractCas}.
   * @param <T> the type of the CAS interface (CAS or JCas)
   * 
   * @return an empty CAS. This will be an implementation of <code>aCasInterface</code>.
   */
  public <T extends AbstractCas> T getEmptyCas(Class<T> aCasInterface);
  
}
