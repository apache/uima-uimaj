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
  @Override
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
    setMetaData(spec.getMetaData());

    // now attempt to create a URL, which can actually be used to access the data
    // Get Relative Path Resolver
    RelativePathResolver relPathResolver = null;
    if (aAdditionalParams != null) {
      relPathResolver = (RelativePathResolver) aAdditionalParams.get(PARAM_RELATIVE_PATH_RESOLVER);
    }
    if (relPathResolver == null) {
      relPathResolver = new RelativePathResolver_impl();
    }

    mFileUrl = relPathResolver.resolveRelativePath(mUri.toString());

    return true;
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * @see org.apache.uima.resource.DataResource#getInputStream()
   */
  @Override
  public InputStream getInputStream() throws IOException {
    return mFileUrl.openStream();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.DataResource#getUri()
   */
  @Override
  public URI getUri() {
    return mUri;
  }

  /**
   * @see org.apache.uima.resource.DataResource#getUrl()
   */
  @Override
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
  @Override
  public boolean equals(Object obj) {
    // obj must be a DataResource_impl
    if (!(obj instanceof ConfigurableDataResource_impl)) {
      return false;
    }

    // URIs must be the same
    URI uri = ((ConfigurableDataResource_impl) obj).getUri();
    if (uri == null || !uri.equals(getUri())) {
      return false;
    }

    // Local Cache Files must be the same
    File localCache = ((ConfigurableDataResource_impl) obj).getLocalCache();
    if ((localCache == null && getLocalCache() != null)
            || (localCache != null && !localCache.equals(getLocalCache()))) {
      return false;
    }

    return true;
  }

  /**
   * @see DataResource#hashCode()
   */
  @Override
  public int hashCode() {
    // add hash codes of member variables
    int hashCode = 0;
    if (mUri != null) {
      hashCode += mUri.hashCode();
    }

    return hashCode;
  }
}
