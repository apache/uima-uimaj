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

import org.apache.axis.AxisFault;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.service.impl.AnalysisEngineService_impl;
import org.apache.uima.analysis_engine.service.impl.ServiceDataCargo;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Level;

/**
 * A class used to deploy a {@link AnalysisEngineService} as an Axis (SOAP)
 * service.
 */
public class AxisAnalysisEngineService_impl 
{
  
    
    

  /**
   * Constructor, responsible for initializing the service.
   */
  public AxisAnalysisEngineService_impl() throws AxisFault
  {
    mServiceImpl = (AnalysisEngineService_impl)
      AxisResourceServiceManager.getServiceImpl(
        AnalysisEngineService_impl.class);
  }  

  /**
   * Gets metadata for this Resource service.
   * 
   * @param metadata
   */
  public ResourceMetaData getMetaData()
    throws ResourceServiceException
  {
    try
    {
      return mServiceImpl.getMetaData();
    }
    catch(ResourceServiceException e)
    {
      UIMAFramework.getLogger().log(Level.SEVERE,
          e.getMessage(), e);
      throw e;
    }
    catch(RuntimeException e)
    {
      UIMAFramework.getLogger().log(Level.SEVERE,
          e.getMessage(), e);
      throw e;      
    }    
  } 
  
  /**
   * Processes an entity.
   * 
   * @param aData data to be processed
   * @param aResultSpec specifies which results the Analysis Engine should
   *   produce
   * 
   * @return the results of analysis
   */
  public ServiceDataCargo process(
    ServiceDataCargo aData, ResultSpecification aResultSpec)
    throws ResourceServiceException
  {
    try
    {
      return mServiceImpl.process(aData,aResultSpec);
    }
    catch(ResourceServiceException e)
    {
      UIMAFramework.getLogger().log(Level.SEVERE,
          e.getMessage(), e);
      throw e;
    }
    catch(RuntimeException e)
    {
      UIMAFramework.getLogger().log(Level.SEVERE,
          e.getMessage(), e);
      throw e;      
    }
  }       
  
  /**
   * Class that will actually implement functionality for this service.
   */
  private AnalysisEngineService_impl mServiceImpl;
}

