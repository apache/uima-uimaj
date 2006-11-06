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

import java.util.HashMap;
import java.util.Map;

import org.apache.axis.AxisFault;
import org.apache.axis.Handler;
import org.apache.axis.MessageContext;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.service.impl.AnalysisEngineService_impl;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.service.impl.ResourceService_impl;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLInputSource;

/**
 * Utility class for deploying Resources as Axis (SOAP)
 * services.  This class mantains a map between Axis service names and
 * the {@link ResourceService_impl} classes that provide the implementation
 * for those service names.  This map is maintained as static data on this
 * class so that it persists between invocations of the Axis services.
 * <P>
 * SOAP service implementation classes call the static
 * {@link #getServiceImpl(Class)} method from their constructors.  The service's
 * name and configuration parameters will be read from the Axis MessageContext.
 * If a {@link ResourceService_impl} instance has already been 
 * registered under this service's name, that instance will be returned.
 * Otherwise, a new <code>ResourceService_impl</code> will be created.
 */
public class AxisResourceServiceManager
{
  
      

  /**
   * The name of the deployment parameter whose value is the path to an XML 
   * resource specifier.  This resource specifier is used to constuct
   * Resource instances that process the requests received by this service.
   * A value for this option must be speciifed in the deployment descriptor 
   * for this service.
   */
  public static final String PARAM_RESOURCE_SPECIFIER_PATH = "resourceSpecifierPath";

  /**
   * The name of the deployment parameter whose value is the number of instances
   * of the Resource (specified by {@link #PARAM_RESOURCE_SPECIFIER_PATH}) to
   * be created.  The Resources are kept in a pool and used to service requests.
   * A value for this option must be speciifed in the deployment descriptor 
   * for this service.
   */
  public static final String PARAM_NUM_INSTANCES = "numInstances";
  
  /** 
   * The name of the deployment parameter whose value is the maximum time
   * (in milliseconds) to wait when attempting to acquire a resource from the 
   * pool.  If this period elapses an exception will be thrown back to the 
   * client.  A value of 0 (the default if no value is specified) will  cause 
   * the service to wait forever. 
   */
  public static final String PARAM_TIMEOUT_PERIOD = "timeoutPeriod";
   
  /** 
   * The name of the deployment parameter whose value is a boolean indicating
   * whether to write log messages during each service invocation.  This 
   * currently applies only to Analysis Engine services.
   */
  public static final String PARAM_ENABLE_LOGGING = "enableLogging";  
          
  /**
   * Gets a {@link ResourceService_impl} class to be used to process an
   * request.This method retrieves the service name and configuration 
   * parameters from the Axis MessageContext.  If a {@link ResourceService_impl}
   * object already exists for that service name, that object will be
   * returned.  Otherwise, a new <code>ResourceService_impl</code> object
   * will be created from the information in the MessageContext.
   * 
   * @param aResourceImplClass the class that will be instantiated when
   *   a new <code>ResourceService_impl</code> is to be created.  This must
   *   be a subclass of ResourceService_impl. 
   * @throws AxisFault if the configuration information could not be read
   */
  public static ResourceService_impl getServiceImpl(Class aServiceImplClass) 
    throws AxisFault
  {
    try
    {
      MessageContext ctx = MessageContext.getCurrentContext();
      if ( ctx == null )
  	  {
        throw new Exception( "MessageContext = NULL");
      }  
  
      Handler self = ctx.getService();
      if ( self == null )
      {	
        throw new Exception( "Handler = NULL");
      }
      
      //Get service name
      String serviceName = self.getName();
             
      //see if we have a ResourceService_impl registered for that name
      ResourceService_impl serviceImpl = (ResourceService_impl)
        mResourceServiceImplMap.get(serviceName);
      if (serviceImpl != null)
      {
        return serviceImpl;       
      }
      
      //No service impl registered for this service name, attempt to
      //create a new one
      
      //Get the Resource Specifier Path
      String resourceSpecifierPath = 
        (String)self.getOption(PARAM_RESOURCE_SPECIFIER_PATH);
      if ( resourceSpecifierPath == null || 
           resourceSpecifierPath.trim().length() == 0 )
      {
        throw new Exception( "Invalid Configuration - " + 
          PARAM_RESOURCE_SPECIFIER_PATH + " not Defined.  Check your deployment descriptor file (WSDD)");
      }
      //parse ResourceSpecifier
      ResourceSpecifier resourceSpecifier =
          UIMAFramework.getXMLParser().parseResourceSpecifier(
              new XMLInputSource(resourceSpecifierPath));
              
      //Get the number of instances to create
      String numInstancesStr = 
        (String)self.getOption(PARAM_NUM_INSTANCES);
      int numInstances;
      try
      {
        numInstances = Integer.parseInt(numInstancesStr);
      }
      catch(NumberFormatException e)
      {
        throw new Exception( "Invalid Configuration - " + 
          PARAM_NUM_INSTANCES + " not valid.  Check your deployment descriptor file (WSDD)");
      }
      
      //Get the timeout period
      String timeoutStr = 
        (String)self.getOption(PARAM_TIMEOUT_PERIOD);
      int timeout;
      if (timeoutStr == null)
      {
        timeout = 0;  //default value    
      }
      else
      {
        try
        {
          timeout = Integer.parseInt(timeoutStr);
        }
        catch(NumberFormatException e)
        {
          throw new Exception( "Invalid Configuration - " + 
            PARAM_TIMEOUT_PERIOD + " not valid.  Check your deployment descriptor file (WSDD)");
        }
      }

      //Get whether to enable logging
      String enableLogStr = 
        (String)self.getOption(PARAM_ENABLE_LOGGING);
      boolean enableLog = "true".equalsIgnoreCase(enableLogStr);

      //create and initialize the service implementation
      serviceImpl = (ResourceService_impl)aServiceImplClass.newInstance();
      HashMap initParams = new HashMap();
      initParams.put(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS, 
                     new Integer(numInstances));
      initParams.put(AnalysisEngine.PARAM_TIMEOUT_PERIOD, 
                     new Integer(timeout));                      
      serviceImpl.initialize(resourceSpecifier, initParams);

      //disable logging for Analysis Engines if deployer so indicated
      if (!enableLog && serviceImpl instanceof AnalysisEngineService_impl)
      {
        Logger nullLogger = UIMAFramework.newLogger();
        nullLogger.setOutputStream(null);
        
        ((AnalysisEngineService_impl)serviceImpl).getAnalysisEngine().setLogger(nullLogger);
      }


      mResourceServiceImplMap.put(serviceName, serviceImpl);
      return serviceImpl;
    }
    catch(Exception e)
    {
      UIMAFramework.getLogger().log(Level.SEVERE,
          e.getMessage(), e);
      throw AxisFault.makeFault(e);  
    }  
  }
  
  /**
   * Map from service names to ResourceService_impl objects.
   */
  private static Map mResourceServiceImplMap = new HashMap();
}

