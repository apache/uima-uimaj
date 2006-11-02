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

package org.apache.uima.adapter.vinci;

import java.util.Map;

import org.apache.uima.Constants;
import org.apache.uima.analysis_engine.service.impl.AnalysisEngineServiceAdapter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;

/**
 * Reference implementation of {@link AnalysisEngineServiceAdapter} for Vinci.
 * 
 * 
 */
public class VinciAnalysisEngineServiceAdapter extends AnalysisEngineServiceAdapter
{

  /**
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier, Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
      throws ResourceInitializationException
  {
    //aSpecifier must be a URISpecifier 
    if (!(aSpecifier instanceof URISpecifier))
    {
      return false;
    }
    
    URISpecifier uriSpec = (URISpecifier) aSpecifier;
    //protocol must be Vinci or VinciBinaryCAS
    if (!uriSpec.getProtocol().equals(Constants.PROTOCOL_VINCI)
        && !uriSpec.getProtocol().equals(Constants.PROTOCOL_VINCI_BINARY_CAS))
    {
      return false;
    }
    
    //As of 2.0, we allow an AnalysisEngine adapter to connect
    //to a CAS Consumer service.  So we no longer reject that case.

    //create proxy to service
    if (uriSpec.getProtocol().equals(Constants.PROTOCOL_VINCI))
    {
      setStub(new VinciAnalysisEngineServiceStub(uriSpec.getUri(), uriSpec.getTimeout(),
          this, uriSpec.getParameters()));
    }
    else
    {
      setStub(new VinciBinaryAnalysisEngineServiceStub(uriSpec.getUri(), uriSpec.getTimeout(),
          this, uriSpec.getParameters()));
    }

    //do superclass initialization, which among other things initializes UimaContext.
    //note we need to establish connection to service before calling this, since
    //superclass initialization depends on having access to the component metadata.
    super.initialize(aSpecifier, aAdditionalParams);    
          
    return true;
  }

}
