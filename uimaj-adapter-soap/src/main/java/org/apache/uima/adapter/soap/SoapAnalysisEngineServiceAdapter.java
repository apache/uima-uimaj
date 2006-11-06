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
import java.util.Map;

import org.apache.uima.Constants;
import org.apache.uima.analysis_engine.AnalysisEngineServiceStub;
import org.apache.uima.analysis_engine.service.impl.AnalysisEngineServiceAdapter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;

/**
 * Reference implementation of {@link AnalysisEngineServiceAdapter} for SOAP.
 * 
 * 
 */
public class SoapAnalysisEngineServiceAdapter extends AnalysisEngineServiceAdapter
{

  /**
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier, Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
      throws ResourceInitializationException
  {
    //aSpecifier must be a URISpecifier using the SOAP protocol
    if (!(aSpecifier instanceof URISpecifier))
    {
      return false;
    }
    URISpecifier uriSpec = (URISpecifier) aSpecifier;
    if (!uriSpec.getProtocol().equals(Constants.PROTOCOL_SOAP)
        && !uriSpec.getProtocol().equals(Constants.PROTOCOL_SOAP_WITH_ATTACHMENTS))
    {
      return false;
    }
    //resource type must be null or AnalysisEngine
    if (uriSpec.getResourceType() != null
        && !uriSpec.getResourceType().equals(URISpecifier.RESOURCE_TYPE_ANALYSIS_ENGINE))
    {
      return false;
    }

    try
    {
      //create proxy to service
      AnalysisEngineServiceStub stub = new AxisAnalysisEngineServiceStub(uriSpec.getUri(), uriSpec
          .getTimeout(), uriSpec.getProtocol().equals(Constants.PROTOCOL_SOAP_WITH_ATTACHMENTS));
      setStub(stub);

      //finish initialization.  This requires access to metadata, so must be called
      //after we create the stub
      super.initialize(aSpecifier, aAdditionalParams);

      return true;
    }
    catch (MalformedURLException e)
    {
      throw new ResourceInitializationException(ResourceInitializationException.MALFORMED_URL,
          new Object[] { uriSpec.getUri(), uriSpec.getSourceUrlString() }, e);
    }
  }
}
