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

package org.apache.uima.analysis_engine.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.cas.SofaID;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;

/**
 * Reference implementation of {@link AnnotatorContext}.
 * 
 * @deprecated Use {link UimaContext} instead
 * @forRemoval 4.0.0
 */
@Deprecated(since = "3.6.0")
public class AnnotatorContext_impl implements AnnotatorContext {

  /**
   * The UimaContextAdmin that this AnnotatorContext wraps.
   */
  private UimaContextAdmin mUimaContext;

  /**
   * Creates a new AnnotatorContext_impl.
   * 
   * @param aUimaContext
   *          the UIMA Context that this AnnotatorContext wraps.
   * 
   */
  public AnnotatorContext_impl(UimaContextAdmin aUimaContext) {
    mUimaContext = aUimaContext;
  }

  @Override
  public Object getConfigParameterValue(String aName) {
    return mUimaContext.getConfigParameterValue(aName);
  }

  @Override
  public Object getConfigParameterValue(String aGroupName, String aParamName) {
    return mUimaContext.getConfigParameterValue(aGroupName, aParamName);
  }

  @Override
  public Logger getLogger() {
    return mUimaContext.getLogger();
  }

  /**
   * @return the InstrumentationFacility to be used within this AnalysisEngine.
   */
  @Override
  public InstrumentationFacility getInstrumentationFacility() {
    return mUimaContext.getInstrumentationFacility();
  }

  @Override
  public String[] getConfigurationGroupNames() {
    return mUimaContext.getConfigurationGroupNames();
  }

  @Override
  public String[] getConfigParameterNames() {
    return mUimaContext.getConfigParameterNames();
  }

  @Override
  public String[] getConfigParameterNames(String aGroup) {
    return mUimaContext.getConfigParameterNames(aGroup);
  }

  /**
   * Locates Resource URL's using the ResourceManager, or, if that fails, the ClassLoader.
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceURL(String)
   */
  @Override
  public URL getResourceURL(String aKey) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceURL(aKey);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  @Override
  public URI getResourceURI(String aKey) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceURI(aKey);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  @Override
  public String getResourceFilePath(String aKey) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceFilePath(aKey);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /**
   * Acquires Resource InputStreams using the ResourceManager, or, if that fails, the ClassLoader.
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceAsStream(String)
   */
  @Override
  public InputStream getResourceAsStream(String aKey) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceAsStream(aKey);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /**
   * Acquires a Resource object using the ResourceManager.
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceObject(String)
   */
  @Override
  public Object getResourceObject(String aKey) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceObject(aKey);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  @Override
  public InputStream getResourceAsStream(String aKey, String[] aParams)
          throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceAsStream(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  @Override
  public Object getResourceObject(String aKey, String[] aParams) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceObject(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  @Override
  public URL getResourceURL(String aKey, String[] aParams) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceURL(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  @Override
  public URI getResourceURI(String aKey, String[] aParams) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceURI(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  @Override
  public String getResourceFilePath(String aKey, String[] aParams)
          throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceFilePath(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  @Override
  public String getDataPath() {
    return mUimaContext.getDataPath();
  }

  /**
   * Sets the current ProcessTrace object, which will receive trace events generated by the
   * InstrumentationFacility.
   * <p>
   * This method is to be called from the Analysis Engine, not the Annotator, so it is not part of
   * the AnnotatorContext interface.
   * 
   * @param aProcessTrace
   *          -
   */
  public void setProcessTrace(ProcessTrace aProcessTrace) {
    mUimaContext.setProcessTrace(aProcessTrace);
  }

  /**
   * @deprecated {@link AnnotatorContext#mapToSofaID(java.lang.String)}
   * @forRemoval 4.0.0
   */
  @Override
  @Deprecated(since = "2.3.1")
  public SofaID mapToSofaID(String aSofaName) {
    return mUimaContext.mapToSofaID(aSofaName);
  }

  /**
   * @deprecated {@link AnnotatorContext#getSofaMappings()}
   * @forRemoval 4.0.0
   */
  @Override
  @Deprecated(since = "2.3.1")
  public SofaID[] getSofaMappings() {
    return mUimaContext.getSofaMappings();
  }
}
