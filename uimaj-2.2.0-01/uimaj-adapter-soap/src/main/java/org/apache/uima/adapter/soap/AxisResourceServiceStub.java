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

package org.apache.uima.adapter.soap;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceServiceStub;
import org.apache.uima.resource.metadata.ResourceMetaData;

/**
 * Proxy to a {@link ResourceService} that uses Axis SOAP to communicate with the service.
 * 
 * 
 */
public abstract class AxisResourceServiceStub implements ResourceServiceStub {

  /**
   * The service endpoint URL.
   */
  private URL mEndpoint;

  /**
   * Timeout in milliseconds; null to use Axis's default value.
   */
  private Integer mTimeout;

  /**
   * Sets the endpoint of the service with which this proxy communicates.
   * 
   * @param aEndpoint
   *          the service endpoint URI
   * @param aTimeout
   *          timeout period in milliseconds, or null to use Axis's default value
   * 
   * @throws MalformedURLException
   *           if <code>aEndpoint</code> is not a valid URL
   */
  public AxisResourceServiceStub(String aEndpoint, Integer aTimeout) throws MalformedURLException {
    mEndpoint = new URL(aEndpoint);
    mTimeout = aTimeout;
  }

  /**
   * @see org.apache.uima.resource.service.ResourceService#getMetaData()
   */
  public ResourceMetaData callGetMetaData() throws ResourceServiceException {
    final QName operationQName = new QName("http://uima.apache.org/resource", "getMetaData");
    final QName resourceMetaDataTypeQName = new QName("http://uima.apache.org/resourceSpecifier",
            "resourceMetaData");

    try {
      Service service = new Service();
      Call call = (Call) service.createCall();
      call.setTimeout(getTimeout());
      call.setTargetEndpointAddress(mEndpoint);
      call.setOperationName(operationQName);

      call.registerTypeMapping(ResourceMetaData.class, resourceMetaDataTypeQName,
              new XmlSerializerFactory(), new XmlDeserializerFactory());

      return (ResourceMetaData) call.invoke(new Object[0]);
    } catch (ServiceException e) {
      throw new ResourceServiceException(e);
    } catch (java.rmi.RemoteException e) {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * Gets the service endpoint URL.
   * 
   * @return the service endpoint URL
   */
  public URL getServiceEndpoint() {
    return mEndpoint;
  }

  /**
   * Gets the timeout period.
   * 
   * @return the timeout period in milliseconds. Null indicates that Axis's default value will be
   *         used.
   */
  public Integer getTimeout() {
    return mTimeout;
  }
}
