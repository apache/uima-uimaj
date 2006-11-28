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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.attachments.AttachmentUtils;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.uima.analysis_engine.AnalysisEngineServiceStub;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.service.impl.ServiceDataCargo;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.TCASException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.metadata.ResourceMetaData;

/**
 * Proxy to an {@link AnalysisEngineService} that makes use of Axis SOAP to communicate with the
 * service.
 * 
 * 
 */
public class AxisAnalysisEngineServiceStub extends AxisResourceServiceStub implements
        AnalysisEngineServiceStub {

  /**
   * Whether attachments should be used to send binary-serialized data
   */
  private boolean mUseAttachments;

  /**
   * Sets the endpoint of the service with which this proxy communicates.
   * 
   * @param aEndpoint
   *          the service endpoint URI
   * @param aTimeout
   *          the timeout period in millseconds, or null to use Axis's default value
   * 
   * @throws MalformedURLException
   *           if <code>aEndpoint</code> is not a valid URL
   */
  public AxisAnalysisEngineServiceStub(String aEndpoint, Integer aTimeout)
          throws MalformedURLException {
    this(aEndpoint, aTimeout, false);
  }

  /**
   * Sets the endpoint of the service with which this proxy communicates.
   * 
   * @param aEndpoint
   *          the service endpoint URI
   * @param aTimeout
   *          the timeout period in millseconds, or null to use Axis's default value
   * @param aUseAttachments
   *          whether attachments should be used to send binary-serialized data
   * 
   * @throws MalformedURLException
   *           if <code>aEndpoint</code> is not a valid URL
   */
  public AxisAnalysisEngineServiceStub(String aEndpoint, Integer aTimeout, boolean aUseAttachments)
          throws MalformedURLException {
    super(aEndpoint, aTimeout);
    mUseAttachments = aUseAttachments;
  }

  /**
   * @see org.apache.uima.reference_impl.analysis_engine.service.AnalysisEngineServiceStub#callGetMetaData()
   */
  public AnalysisEngineMetaData callGetAnalysisEngineMetaData() throws ResourceServiceException {
    final QName mOperationQName = new QName("http://uima.apache.org/analysis_engine",
            "getAnalysisEngineMetaData");
    final QName mResourceMetaDataTypeQName = new QName("http://uima.apache.org/resourceSpecifier",
            "resourceMetaData");

    try {
      Service service = new Service();
      Call call = (Call) service.createCall();
      call.setTimeout(getTimeout());
      call.setTargetEndpointAddress(getServiceEndpoint());
      call.setOperationName(mOperationQName);

      call.registerTypeMapping(ResourceMetaData.class, mResourceMetaDataTypeQName,
              new XmlSerializerFactory(), new XmlDeserializerFactory());

      return (AnalysisEngineMetaData) call.invoke(new Object[0]);
    } catch (ServiceException e) {
      throw new ResourceServiceException(e);
    } catch (java.rmi.RemoteException e) {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see org.apache.uima.reference_impl.analysis_engine.service.AnalysisEngineServiceStub#callProcess(CAS)
   */
  public void callProcess(CAS aCAS) throws ResourceServiceException {
    final QName operationQName = new QName("http://uima.apache.org/analysis_engine", "process");
    final QName resultSpecTypeQName = new QName("http://uima.apache.org/analysis_engine",
            "resultSpecification");
    final QName serviceDataCargoTypeQName = new QName("http://uima.apache.org/analysis_engine",
            "serviceDataCargo");

    try {
      Service service = new Service();
      Call call = (Call) service.createCall();

      call.setTargetEndpointAddress(getServiceEndpoint());
      call.setTimeout(getTimeout());
      call.setOperationName(operationQName);

      call.registerTypeMapping(ResultSpecification.class, resultSpecTypeQName,
              new XmlSerializerFactory(), new XmlDeserializerFactory());
      call.registerTypeMapping(ServiceDataCargo.class, serviceDataCargoTypeQName,
              new BinarySerializerFactory(mUseAttachments), new BinaryDeserializerFactory());

      // extract data from CAS to prepare for binary serialization
      // (do not send process trace)
      ServiceDataCargo dataCargo = new ServiceDataCargo(aCAS, null);
      // call service
      Object result = call.invoke(new Object[] { dataCargo, null });
      // System.out.println("Got return value of class: " + result.getClass().getName()); //DEBUG
      ServiceDataCargo resultCargo = null;
      // if result was attachment, extract data and deserialize
      if (result instanceof AttachmentPart) {
        ObjectInputStream objStream = null;
        try {
          DataHandler dataHandler = AttachmentUtils
                  .getActivationDataHandler((AttachmentPart) result);
          Object content = dataHandler.getContent();
          // System.out.println(content.getClass().getName());
          objStream = new ObjectInputStream((InputStream) content);
          resultCargo = (ServiceDataCargo) objStream.readObject();
        } catch (IOException e) {
          throw new ResourceServiceException(e);
        } catch (ClassNotFoundException e) {
          throw new ResourceServiceException(e);
        } finally {
          if (objStream != null) {
            try {
              objStream.close();
            } catch (IOException e) {
              throw new ResourceServiceException(e);
            }
          }
        }
      } else if (result instanceof ServiceDataCargo) // direct return
      {
        resultCargo = (ServiceDataCargo) result;
      } else {
        throw new ResourceServiceException(
                ResourceServiceException.UNEXPECTED_SERVICE_RETURN_VALUE_TYPE, new Object[] {
                    ServiceDataCargo.class.getName(),
                    resultCargo == null ? "null" : resultCargo.getClass().getName() });
      }

      // unmarshal analysis results into the original AnalysisProcessData object
      // (do not replace CAS type system, as it should not have been changed
      // by the service)
      resultCargo.unmarshalCas(aCAS, false);
    } catch (ServiceException e) {
      throw new ResourceServiceException(e);
    } catch (java.rmi.RemoteException e) {
      throw new ResourceServiceException(e);
    } catch (TCASException e) {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see org.apache.uima.reference_impl.resource.service.ResourceServiceStub#destroy()
   */
  public void destroy() {
    // no resources to clean up
  }

  public void callBatchProcessComplete() throws ResourceServiceException {
    // currently not implemented for SOAP services
  }

  public void callCollectionProcessComplete() throws ResourceServiceException {
    // currently not implemented for SOAP services
  }

}
