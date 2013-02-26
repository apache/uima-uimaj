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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * <code>DataResource</code> is a simple resource that provides access to data. All
 * <code>DataResource</code>s will implement the {@link #getInputStream()} method to provide
 * access to their data.
 * <p>
 * <code>DataResource</code>s may optionally implement {@link #getUrl()}, which would return the
 * URL where the data is located. This may be necessary for some applications, but it is strongly
 * recommended the {@link #getInputStream()} be used whenever possible, because accessing the data
 * directly via the URL does not allow the ResourceManager to assist in caching or sharing of data.
 * 
 * 
 */
public interface DataResource extends Resource {

  /**
   * Gets an {@link InputStream} to the data. It is the caller's responsibility to close this input
   * stream when finished with it.
   * 
   * @return an InputStream to the data
   * 
   * @throws IOException
   *           if an I/O error occurred when trying to open the stream
   */
  public InputStream getInputStream() throws IOException;

  /**
   * Gets the URI of the data. In general, this method will return a URI that is equivalent to the
   * URL returned by {@link #getUrl()}. However, in the case where {@link #getUrl()} returns null
   * (indicating no URL available), this method may still return a URI. This can be the case if the
   * URI does not use a standard protocol such as http or file.
   * 
   * @return The URI of the data
   */
  public URI getUri();

  /**
   * Gets the URL where the data is stored. This method may return null if there is no appropriate
   * URL (for example if the data is stored in a relational database). It is recommended that the
   * {@link #getInputStream()} method be used whenever possible - see the class comment for more
   * information.
   * 
   * @return the URL where the data is stored, or null if this is not available.
   */
  public URL getUrl();

  /**
   * Determines if this <code>DataResource</code> is equal to another <code>DataResource</code>.
   * It is important that <code>DataResource</code> implementations override this method
   * appropriately, because the {@link ResourceManager} can make use of this method to determine
   * when cached data can be reused. Two <code>DataResource</code>s that are <code>equal</code>
   * according to this method will be considered to provide access to the same data; therefore, a
   * common cache can be used.
   * 
   * @param aObj
   *          the object to compare to
   * 
   * @return true if and only if <code>aObj</code> is a <code>DataResource</code> and provides
   *         access to the same data as this object.
   */
  public boolean equals(Object aObj);

  /**
   * Gest the hash code for this <code>DataResource</code>. As always, if the
   * {@link #equals(Object)} method is overridden, this method should also be overridden. Two
   * objects that are <code>equal</code> must have the same hash code.
   * 
   * @return the hash code for this object
   */
  public int hashCode();

  /**
   * Key for the initialization parameter whose value is a reference to the
   * {@link RelativePathResolver} that this DataResource should use to resolve relative resource
   * URLs. This value is used as a key in the <code>aAdditionalParams</code> Map that is passed to
   * the {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_RELATIVE_PATH_RESOLVER = "RELATIVE_PATH_RESOLVER";
}
