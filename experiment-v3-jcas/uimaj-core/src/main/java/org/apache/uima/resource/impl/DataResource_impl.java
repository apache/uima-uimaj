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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.uima.UIMARuntimeException;
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
public class DataResource_impl extends Resource_ImplBase implements DataResource {

  /** URL of file. */
  private URL mFileUrl;

  /** Filename of local cache, if any. */
  private File mLocalCache;

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
    // aSpecifier must be a FileResourceSpecifier
    if (!(aSpecifier instanceof FileResourceSpecifier))
      return false;

    // If we get here, aSpecifier is supported by this implementation.
    FileResourceSpecifier spec = (FileResourceSpecifier) aSpecifier;

    // Get Relative Path Resolver
    RelativePathResolver relPathResolver = getRelativePathResolver(aAdditionalParams);
      
    // Get the file URL, resolving relative path as necessary
    IOException ioEx = null;
    try {
      // Get the file URL from the specifier.  If the user has passed a file path
      // (e.g. c:\Program Files\...) instead of a URL, be lenient and convert it to
      // a URL
      URL relativeUrl;
      try {
        relativeUrl = new URL(spec.getFileUrl());
      }
      catch (MalformedURLException e) {
        //try to treat the URL as a file name.  
        File file = new File(spec.getFileUrl());
        if (file.isAbsolute()) {
          //for absolute paths, use File.toURL(), which handles
          //windows absolute paths correctly
          relativeUrl = file.toURL();
        } else {
          //for relative paths, we can' use File.toURL() because it always
          //produces an absolute URL.  Instead we do the following, which
          //won't work for windows absolute paths (but that's OK, since we
          //know we're working with a relative path)
          relativeUrl = new URL("file", "", spec.getFileUrl());
        }
      }
      
      //resolve relative paths
      mFileUrl = relPathResolver.resolveRelativePath(relativeUrl);

      // Store local cache info, even though it is not used
      if (spec.getLocalCache() == null) {
        mLocalCache = null;
      } else {
        mLocalCache = new File(spec.getLocalCache());
      }

    } catch (IOException e) {
      ioEx = e;
    }
    if (mFileUrl == null) {
      throw new ResourceInitializationException(
              ResourceInitializationException.COULD_NOT_ACCESS_DATA, new Object[] { spec
                      .getFileUrl() }, ioEx);
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

  /**
   * @see org.apache.uima.resource.DataResource#getUrl()
   */
  public URL getUrl() {
    return mFileUrl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.DataResource#getUri()
   */
  public URI getUri() {
    try {
      return UriUtils.quote(mFileUrl);
    } catch (URISyntaxException e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * Gets the file name of the local cache for a remote resource file, if any.
   * 
   * @return the local cache File
   */
  protected File getLocalCache() {
    return mLocalCache;
  }

  /**
   * @see DataResource#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    // obj must be a DataResource_impl
    if (!(obj instanceof DataResource_impl))
      return false;

    // URLs must be the same (but don't use URL.equals(), which does DNS resolution!)
    URL url = ((DataResource_impl) obj).getUrl();
    if (url == null || !url.toString().equals(this.getUrl().toString()))
      return false;

    // Local Cache Files must be the same
    File localCache = ((DataResource_impl) obj).getLocalCache();
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
    if (mFileUrl != null)
      hashCode += mFileUrl.toString().hashCode(); //don't use URL.hashCode(), which does DNS resolution
    if (mLocalCache != null)
      hashCode += mLocalCache.hashCode();

    return hashCode;
  }
}
