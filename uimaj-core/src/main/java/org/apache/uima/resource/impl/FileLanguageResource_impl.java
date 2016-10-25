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

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.FileLanguageResourceSpecifier;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.ParameterizedDataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.Resource_ImplBase;

/**
 * An implementation of {@link ParameterizedDataResource} for language-based resources. Initialized
 * from a {@link FileLanguageResourceSpecifier}.
 * 
 * 
 */
public class FileLanguageResource_impl extends Resource_ImplBase implements
        ParameterizedDataResource {

  /** URL prefix */
  private String mFileUrlPrefix;

  /** URL suffix */
  private String mFileUrlSuffix;

  /** Initialization parameters to be passed to DataResources. */
  private Map<String, Object> mResourceInitParams;

  /**
   * @see org.apache.uima.resource.Resource#initialize(org.apache.uima.resource.ResourceSpecifier,
   *      java.util.Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    // aSpecifier must be a FileLanguageResourceSpecifier
    if (!(aSpecifier instanceof FileLanguageResourceSpecifier))
      return false;

    // If we get here, aSpecifier is supported by this implementation.
    FileLanguageResourceSpecifier spec = (FileLanguageResourceSpecifier) aSpecifier;

    // Store the attributes for later use.
    mFileUrlPrefix = spec.getFileUrlPrefix();
    mFileUrlSuffix = spec.getFileUrlSuffix();

    // store initialization parameters to be passed on to DataReources
    mResourceInitParams = (aAdditionalParams == null) ? new HashMap<String, Object>() : new HashMap<String, Object>(
            aAdditionalParams);

    // call super initialize to set uima context from additional params if available
    // this context is to allow getting access to the Resource Manager.
    // https://issues.apache.org/jira/browse/UIMA-5153
    super.initialize(aSpecifier, aAdditionalParams);
    return true;
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
  }

  /**
   * @see org.apache.uima.resource.ParameterizedDataResource#getDataResource(java.lang.String[])
   */
  public DataResource getDataResource(String[] aParams) throws ResourceInitializationException {
    // one parameter - the language - is required
    if (aParams.length != 1) {
      throw new ResourceInitializationException(
              ResourceInitializationException.INCORRECT_NUMBER_OF_PARAMETERS, new Object[] { "1" });
    }

    String lang = aParams[0];
    DataResource resource = null;
    Exception firstException = null;

    while (resource == null && lang != null) {
      // build URL
      String urlString = mFileUrlPrefix + lang + mFileUrlSuffix;
      // build FileResource specifier and attempt to create DataResource
      FileResourceSpecifier fileSpec = UIMAFramework.getResourceSpecifierFactory()
              .createFileResourceSpecifier();
      fileSpec.setFileUrl(urlString);

      try {
        resource = (DataResource) UIMAFramework.produceResource(DataResource.class, fileSpec,
                mResourceInitParams);
      } catch (ResourceInitializationException e) {
        // this is expected if the data file cannot be found - record first
        // such exception only
        if (firstException == null) {
          firstException = e;
        }
      }

      if (resource == null) {
        // language fallback
        // truncate group name at the last _ or - character
        int lastUnderscore = lang.lastIndexOf('_');
        int lastHyphen = lang.lastIndexOf('-');
        int truncateAt = (lastUnderscore > lastHyphen) ? lastUnderscore : lastHyphen;
        if (truncateAt == -1) {
          lang = null;
        } else {
          lang = lang.substring(0, truncateAt);
        }
      }
    } // end while

    if (resource != null) {
      return resource;
    } else {
      throw new ResourceInitializationException(
              ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS, new Object[] { "["
                      + aParams[0] + "]" }, firstException);
    }
  }
}
