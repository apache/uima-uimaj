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

package org.apache.uima.resource.service.impl;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceServiceStub;
import org.apache.uima.resource.Resource_ImplBase;
import org.apache.uima.resource.metadata.ResourceMetaData;

/**
 * Insulates applications from the knowledge that they are interacting with a
 * {@link ResourceService_impl} rather than a local instance of a {@link Resource}. This is an abstract
 * base class that specific resource adapter implementations may extend.
 * <p>
 * This class implements the {@link Resource} interface and encapsulates all communications with a
 * remote <code>ResourceService</code>. Thus, applications can interact with this adapter in the
 * same way they would interact with any <code>Resource</code>, and can be completely unaware of
 * the fact that a remote <code>ResourceService</code> is being used.
 * 
 * 
 */
public abstract class ResourceServiceAdapter extends Resource_ImplBase {

  /**
   * The stub that communicates with the remote service.
   */
  private ResourceServiceStub mStub;

  /**
   * Cached meta data.
   */
  private ResourceMetaData mCachedMetaData;

  /**
   * Sets the stub to be used to communicate with the remote service. Subclasses must call this from
   * their <code>initialize</code> method.
   * 
   * @param aStub
   *          the stub for the remote service
   */
  protected void setStub(ResourceServiceStub aStub) {
    mStub = aStub;
  }

  /**
   * Gets the stub to be used to communicate with the remote service.
   * 
   * @return the stub for the remote service
   */
  protected ResourceServiceStub getStub() {
    return mStub;
  }

  /**
   * @see org.apache.uima.resource.Resource#getMetaData()
   */
  public ResourceMetaData getMetaData() {
    try {
      if (mCachedMetaData == null) {
        mCachedMetaData = getStub().callGetMetaData();
      }
      return mCachedMetaData;
    } catch (ResourceServiceException e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
  }
}
