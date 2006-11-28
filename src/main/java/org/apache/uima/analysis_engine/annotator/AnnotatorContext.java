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

package org.apache.uima.analysis_engine.annotator;

import java.io.InputStream;
import java.net.URL;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.Logger;

/**
 * This interface is maintained only for backwards compatibility with annotators developed in UIMA
 * SDK v1.x. New code should use {@link UimaContext} instead.
 */
public interface AnnotatorContext {

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
   * @return the value of the parameter with the given name. The annotator is expected to know the
   *         data type of its parameters. Returns <code>null</code> if the parameter does not
   *         exist or has not been assigned a value.
   * 
   * @throws AnnotatorContextException
   *           if an internal failure has occurred in the <code>AnnotatorContext</code>.
   */
  public Object getConfigParameterValue(String aParamName) throws AnnotatorContextException;

  /**
   * Retrieves the value for a configuration parameter in a particular group. If that group contains
   * no value for the specified parameter, the fallback strategy specified by the Analysis Engine's
   * {@link org.apache.uima.resource.metadata.ConfigurationParameterDeclarations#getSearchStrategy()}
   * property will be used. The search strategy can be specified in the Analysis Engine Descriptor.
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
   * @return the value of the parameter with the given name. The annotator is expected to know the
   *         data type of its parameters. Returns <code>null</code> if the parameter does not
   *         exist or has not been assigned a value.
   * 
   * @throws AnnotatorContextException
   *           if an internal failure has occurred in the <code>AnnotatorContext</code>.
   */
  public Object getConfigParameterValue(String aGroupName, String aParamName)
          throws AnnotatorContextException;

  /**
   * Gets the names of all configuration parameter groups.
   * 
   * @return an array containing the names of all configuration groups that exist for this
   *         annotator. Returns an empty array if no groups are declared.
   */
  public String[] getConfigurationGroupNames();

  /**
   * Gets the names of all configuration parameters in the specified group.
   * 
   * @param aGroup
   *          the group name
   * 
   * @return an array containing the names of all configuration parameters declared in {<code>aGroup</code>.
   *         Note that this does include parameters with null values. Returns an empty array if
   *         there are none (including if the group does not exist).
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
   * Gets the <code>Logger</code> to which this annotator should send its log output. Annotators
   * should use this facility rather than writing to their own log files (or to stdout).
   * 
   * @return an instance of a logger for use by this annotator.
   * 
   * @throws AnnotatorContextException
   *           if an internal failure has occurred in the <code>AnnotatorContext</code>.
   */
  public Logger getLogger() throws AnnotatorContextException;

  /**
   * Gets the <code>InstrumentationFacility</code> that this annotator can use to record
   * information about its performance.
   * 
   * @return an instance of the instrumentation facility for use by this annotator.
   * 
   * @throws AnnotatorContextException
   *           if an internal failure has occurred in the <code>AnnotatorContext</code>.
   */
  public InstrumentationFacility getInstrumentationFacility() throws AnnotatorContextException;

  /**
   * Retrieves the URL to the named resource. This can be used, for example, to locate configuration
   * or authority files. The resource should be declared in the &lt;externalResourceDependencies&gt;
   * section of the descriptor.
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
   * @see org.apache.uima.UimaContext#getResourceURL(String)
   */
  public URL getResourceURL(String aKey) throws AnnotatorContextException;

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
   * @see org.apache.uima.UimaContext#getResourceAsStream(String)
   */
  public InputStream getResourceAsStream(String aKey) throws AnnotatorContextException;

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
   * @see org.apache.uima.UimaContext#getResourceObject(String)
   */
  public Object getResourceObject(String aKey) throws AnnotatorContextException;

  /**
   * Retrieves the URL to the named resource. This can be used, for example, to locate configuration
   * or authority files. The resource should be declared in the &lt;externalResourceDependencies&gt;
   * section of the descriptor.
   * <p>
   * For backwards compatibility, if the key is not declared as a resource dependency, it is looked
   * up directly in the {@link #getDataPath() data path} and the class path. However, this usage is
   * deprecated and support may be dropped in future versions. ALL external resource dependencies
   * should be declared in the descriptor.
   * <p>
   * This version of this method takes an array of parameters used to further identify the resource.
   * This can be used, for example, with resources that vary depending on the language of the
   * document being analyzed, such as when the &lt;fileLanguageResourceSpecifier> element is used in
   * the component descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * @param aParams
   *          parameters used to further identify the resource. When used to identify the language
   *          for a &lt;fileLanguageResourceSpecifier>, this array should contain a single element,
   *          the ISO language code for the language of the document (e.g. "en", "de").
   * 
   * @return the <code>URL</code> at which the named resource is located, <code>null</code> if
   *         the named resource could not be found.
   * 
   * @see org.apache.uima.UimaContext#getResourceURL(String,String[])
   */
  public URL getResourceURL(String aKey, String[] aParams) throws AnnotatorContextException;

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
   * document being analyzed, such as when the &lt;fileLanguageResourceSpecifier> element is used in
   * the component descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should bd declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * @param aParams
   *          parameters used to further identify the resource. When used to identify the language
   *          for a &lt;fileLanguageResourceSpecifier>, this array should contain a single element,
   *          the ISO language code for the language of the document (e.g. "en", "de").
   * 
   * @return an <code>InputStream</code> for reading from the named resource, <code>null</code>
   *         if the named resource could not be found. It is the caller's responsibility to close
   *         this stream once it is no longer needed.
   * 
   * @see org.apache.uima.UimaContext#getResourceAsStream(String,String[])
   */
  public InputStream getResourceAsStream(String aKey, String[] aParams)
          throws AnnotatorContextException;

  /**
   * Retrieves the named resource object. This can be used to acquire references to external
   * resources. The resource should be declared in the &lt;externalResourceDependencies&gt; section
   * of the descriptor.
   * <p>
   * This version of this method takes an array of parameters used to further identify the resource.
   * This can be used, for example, with resources that vary depending on the language of the
   * document being analyzed, such as when the &lt;fileLanguageResourceSpecifier> element is used in
   * the component descriptor.
   * 
   * @param aKey
   *          the key by which the resource is identified. This key should be declared in the
   *          &lt;externalResourceDependencies&gt; section of the descriptor.
   * @param aParams
   *          parameters used to further identify the resource. When used to identify the language
   *          for a &lt;fileLanguageResourceSpecifier>, this array should contain a single element,
   *          the ISO language code for the language of the document (e.g. "en", "de").
   * 
   * @return the object bound to <code>aName</code>, <code>null</code> if none.
   * 
   * @see org.apache.uima.UimaContext#getResourceObject(String,String[])
   */
  public Object getResourceObject(String aKey, String[] aParams) throws AnnotatorContextException;

  /**
   * Gets the data path used to locate resources. This path may contain more than one directory,
   * separated by the System <code>path.separator</code> character (; on windows, : on UNIX).
   * <p>
   * This method is intended to be used only for integration of legacy or third-party components
   * that have their own resource management facility. If possible, it is recommended that you use
   * the <code>getResoureXXX</code> methods instead.
   * 
   * @return the data path
   * 
   * @throws AnnotatorContextException
   *           if an internal failure has occurred in the <code>AnnotatorContext</code>.
   */
  public String getDataPath() throws AnnotatorContextException;

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
  public SofaID mapToSofaID(String aSofaName);

  /**
   * 
   * @return array of SofaID objects containing mapping of component sofa name to absolute sofa id
   * 
   * @deprecated As of v2.0, annotators no longer need to explicitly call this method. CAS views can
   *             now be obtained directly by the method {@link CAS#getView(String)}, and the
   *             framework will automatically do the necessary Sofa mappings.
   */
  public SofaID[] getSofaMappings();

}
