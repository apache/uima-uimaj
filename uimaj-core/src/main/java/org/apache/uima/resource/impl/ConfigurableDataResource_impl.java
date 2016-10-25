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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.uima.resource.ConfigurableDataResourceSpecifier;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.Resource_ImplBase;
import org.apache.uima.util.UriUtils;

/**
 * A simple {@link DataResource} implementation that can read data from a file via a URL. There is
 * an attribute for specifying the location of a local cache for a remote file, but this is not
 * currently being used.
 * 
 * 
 */
public class ConfigurableDataResource_impl extends Resource_ImplBase implements DataResource {

  /** URI of data. */
  private URI mUri;

  /** URL of file. This is formed from the URI, if possible. */
  private URL mFileUrl;

  /**
   * Initializes this DataResource.
   * 
   * @param aSpecifier
   *          describes how to create this DataResource. Must (at least for now) be a
   *          {@link FileResourceSpecifier}.
   * @param aAdditionalParams
   *          not currently used
   * 
   * @return true if and only if initialization completed successfully. Returns false if this
   *         implementation cannot handle the given <code>ResourceSpecifier</code>.
   * 
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier, Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    // aSpecifier must be a ConfigurableDataResourceSpecifier
    if (!(aSpecifier instanceof ConfigurableDataResourceSpecifier)) {
      return false;
    }

    ConfigurableDataResourceSpecifier spec = (ConfigurableDataResourceSpecifier) aSpecifier;
    try {
      // create URI object from URL specified in descriptor
      mUri = UriUtils.quote(spec.getUrl());
    } catch (URISyntaxException e) {
      throw new ResourceInitializationException(e);
    }

    // set metadata
    this.setMetaData(spec.getMetaData());

    // now attempt to create a URL, which can actually be used to access the data
    // Get Relative Path Resolver
    RelativePathResolver relPathResolver = getRelativePathResolver(aAdditionalParams);

    // Get the file URL, resolving relative path as necessary
    try {
      mFileUrl = relPathResolver.resolveRelativePath(new URL(mUri.toString()));
    } catch (IOException e) {
      // this is OK. The URI may not be a valid URL (e.g. it may use a non-standard protocol).
      // in this case getUrl returns null but getUri can still be used to access the URI
    }
    
    // call super initialize to set uima context from additional params if available
    // this context is to allow getting access to the Resource Manager.
    // https://issues.apache.org/jira/browse/UIMA-5153
    super.initialize(aSpecifier, aAdditionalParams);
    return true;
  }

  /**
   * @see org.apache.uima.resource.DataResource#getInputStream()
   */
  public InputStream getInputStream() throws IOException {
    return mFileUrl.openStream();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.DataResource#getUri()
   */
  public URI getUri() {
    return mUri;
  }

  /**
   * @see org.apache.uima.resource.DataResource#getUrl()
   */
  public URL getUrl() {
    return mFileUrl;
  }

  /**
   * Gets the file name of the local cache for a remote resource file, if any. This is not currently
   * supported; it always returns null.
   * 
   * @return the local cache File
   */
  protected File getLocalCache() {
    return null;
  }

  /**
   * @see DataResource#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    // obj must be a DataResource_impl
    if (!(obj instanceof ConfigurableDataResource_impl))
      return false;

    // URIs must be the same
    URI uri = ((ConfigurableDataResource_impl) obj).getUri();
    if (uri == null || !uri.equals(this.getUri()))
      return false;

    // Local Cache Files must be the same
    File localCache = ((ConfigurableDataResource_impl) obj).getLocalCache();
    if (localCache == null && this.getLocalCache() != null)
      return false;

    if (localCache != null && !localCache.equals(this.getLocalCache()))
      return false;

    return true;
  }

  /**
   * @see DataResource#hashCode()
   */
  public int hashCode() {
    // add hash codes of member variables
    int hashCode = 0;
    if (mUri != null)
      hashCode += mUri.hashCode();

    return hashCode;
  }
}
