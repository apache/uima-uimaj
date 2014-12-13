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
 * 
 */
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

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getConfigParameterValue(String)
   */
  public Object getConfigParameterValue(String aName) {
    return mUimaContext.getConfigParameterValue(aName);
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getConfigParameterValue(java.lang.String,
   *      java.lang.String)
   */
  public Object getConfigParameterValue(String aGroupName, String aParamName) {
    return mUimaContext.getConfigParameterValue(aGroupName, aParamName);
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getLogger()
   */
  public Logger getLogger() {
    return mUimaContext.getLogger();
  }

  /**
   * Gets the InstrumentationFacility to be used within this AnalysisEngine.
   * 
   * @return the InstrumentationFacility to be used within this AnalysisEngine
   */
  public InstrumentationFacility getInstrumentationFacility() {
    return mUimaContext.getInstrumentationFacility();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getConfigurationGroupNames()
   */
  public String[] getConfigurationGroupNames() {
    return mUimaContext.getConfigurationGroupNames();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getConfigurationParameterNames()
   */
  public String[] getConfigParameterNames() {
    return mUimaContext.getConfigParameterNames();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getConfigurationParameterNames(java.lang.String)
   */
  public String[] getConfigParameterNames(String aGroup) {
    return mUimaContext.getConfigParameterNames(aGroup);
  }

  /**
   * Locates Resource URL's using the ResourceManager, or, if that fails, the ClassLoader.
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceURL(String)
   */
  public URL getResourceURL(String aKey) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceURL(aKey);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceURI(java.lang.String)
   */
  public URI getResourceURI(String aKey) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceURI(aKey);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceFilePath(java.lang.String)
   */
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
  public Object getResourceObject(String aKey) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceObject(aKey);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceAsStream(java.lang.String,
   *      java.lang.String[])
   */
  public InputStream getResourceAsStream(String aKey, String[] aParams)
          throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceAsStream(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceObject(java.lang.String,
   *      java.lang.String[])
   */
  public Object getResourceObject(String aKey, String[] aParams) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceObject(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceURL(java.lang.String,
   *      java.lang.String[])
   */
  public URL getResourceURL(String aKey, String[] aParams) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceURL(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceURI(java.lang.String,
   *      java.lang.String[])
   */
  public URI getResourceURI(String aKey, String[] aParams) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceURI(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceFilePath(java.lang.String,
   *      java.lang.String[])
   */
  public String getResourceFilePath(String aKey, String[] aParams) throws AnnotatorContextException {
    try {
      return mUimaContext.getResourceFilePath(aKey, aParams);
    } catch (ResourceAccessException e) {
      throw new AnnotatorContextException(e);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getDataPath()
   */
  public String getDataPath() {
    return mUimaContext.getDataPath();
  }

  /**
   * Sets the current ProcessTrace object, which will receive trace events generated by the
   * InstrumentationFacility.
   * <p>
   * This method is to be called from the Analysis Engine, not the Annotator, so it is not part of
   * the AnnotatorContext interface.
   * @param aProcessTrace -
   */
  public void setProcessTrace(ProcessTrace aProcessTrace) {
    mUimaContext.setProcessTrace(aProcessTrace);
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#mapToSofaID(java.lang.String)
   * @deprecated
   */
  @Deprecated
  public SofaID mapToSofaID(String aSofaName) {
    return mUimaContext.mapToSofaID(aSofaName);
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getSofaMappings()
   * @deprecated
   */
  @Deprecated
  public SofaID[] getSofaMappings() {
    return mUimaContext.getSofaMappings();
  }
}
