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

import java.net.InetAddress;

import org.apache.uima.UIMAFramework;
import org.apache.uima.adapter.vinci.util.Constants;
import org.apache.uima.adapter.vinci.util.VinciSaxParser;
import org.apache.uima.analysis_engine.AnalysisEngineServiceStub;
import org.apache.uima.analysis_engine.impl.AnalysisEngineManagementImpl;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.service.impl.AnalysisEngineServiceAdapter;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.internal.util.SerializationUtils;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.SaxDeserializer;
import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.context.VinciContext;
import org.apache.vinci.transport.document.AFrame;

public class VinciBinaryAnalysisEngineServiceStub implements AnalysisEngineServiceStub
{
  private VinciClient mVinciClient;

  private static final boolean debug = false;

  private AnalysisEngineServiceAdapter mOwner;

  public VinciBinaryAnalysisEngineServiceStub(String endpointURI, AnalysisEngineServiceAdapter owner)
      throws ResourceInitializationException
  {
    this(endpointURI, null, owner, null);
  }

  public VinciBinaryAnalysisEngineServiceStub(String endpointURI, Integer timeout,
      AnalysisEngineServiceAdapter owner, Parameter[] parameters) throws ResourceInitializationException
  {
    mOwner = owner;
    
    //open Vinci connection
    try
    {
      String vnsHost;
      if (parameters != null && (vnsHost = getParameterValueFor("VNS_HOST", parameters)) != null)
      {
        //		Override vinci default VNS settings	
        VinciContext vctx = new VinciContext(InetAddress.getLocalHost().getCanonicalHostName(), 0);
        vctx.setVNSHost(vnsHost);
        String vnsPort = getParameterValueFor("VNS_PORT", parameters);
        if (vnsPort != null)
        {
          vctx.setVNSPort(Integer.parseInt(vnsPort));
        }
        else
        {
          vctx.setVNSPort(9000); // use default. 
        }

        if (debug)
        {
          System.out.println("Establishing connnection to " + endpointURI + " using VNS_HOST:"
              + vnsHost + " and VNS_PORT=" + ((vnsPort == null) ? "9000" : vnsPort));
        }
        //	establish connection to service
        mVinciClient = new VinciClient(endpointURI, AFrame.getAFrameFactory(), vctx);
      }
      else
      {
        //If VNS_HOST system property is not set, use default value
        if (System.getProperty("VNS_HOST") == null)
        {
          System.out.println("No VNS_HOST specified; using default " + Constants.DEFAULT_VNS_HOST);
          System.setProperty("VNS_HOST", Constants.DEFAULT_VNS_HOST);
        }

        if (debug)
        {
          System.out.println("Establishing connnection to " + endpointURI);
        }
        //	Use default VNS
        mVinciClient = new VinciClient(endpointURI, AFrame.getAFrameFactory());
      }
      if (timeout != null)
      {
        mVinciClient.setSocketTimeout(timeout.intValue());
      }
      if (debug)
      {
        System.out.println("Success");
      }
    }
    catch (Exception e)
    {
      throw new ResourceInitializationException(e);
    }
  }

  public static String getParameterValueFor(String aKey, Parameter[] parameters)
  {
    if (aKey != null)
    {
      for (int i = 0; parameters != null && i < parameters.length; i++)
      {
        if (aKey.equals(parameters[i].getName()))
        {
          return parameters[i].getValue();
        }
      }
    }
    return null; // aKey not found in parameters
  }

  /**
   * @see org.apache.uima.resource.service.ResourceServiceStb#callGetMetaData()
   */
  public ResourceMetaData callGetMetaData() throws ResourceServiceException
  {
    try
    {
      //  create Vinci Frame
      VinciFrame queryFrame = new VinciFrame();
      //  Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", "GetMeta");
      //  Send the request to the service and wait for response

      if (debug)
      {
        System.out.println("Calling GetMeta");
      }

      mVinciClient.setTransportableFactory(AFrame.getAFrameFactory());
      VinciFrame resultFrame = mVinciClient.rpc(queryFrame);

      if (debug)
      {
        System.out.println("Success");
      }

      //  Extract the data from Vinci Response frame
      //System.out.println(resultFrame.toXML()); //DEBUG

      // Remove things from the result frame that are not the MetaData objects we expect.
      // In the future other things may go in here.
      int i = 0;
      while (i < resultFrame.getKeyValuePairCount())
      {
        String key = resultFrame.getKeyValuePair(i).getKey();
        if (key.length() < 8 || !key.substring(key.length() - 8).equalsIgnoreCase("metadata"))
        {
          resultFrame.fdrop(key);
        }
        else
        {
          i++;
        }
      }

      //  Parse the XML into the AnalysisEngineMetaData object
      SaxDeserializer saxDeser = UIMAFramework.getXMLParser().newSaxDeserializer(null, null, false);

      VinciSaxParser vinciSaxParser = new VinciSaxParser();
      vinciSaxParser.setContentHandler(saxDeser);
      vinciSaxParser.parse(resultFrame);
      AnalysisEngineMetaData metadata = (AnalysisEngineMetaData) saxDeser.getObject();

      return metadata;
    }
    catch (Exception e)
    {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.service.AnalysisEngineServiceStub#callGetAnalysisEngineMetaData()
   */
  public AnalysisEngineMetaData callGetAnalysisEngineMetaData() throws ResourceServiceException
  {
    return (AnalysisEngineMetaData) callGetMetaData();
  }

  /**
   * @see org.apache.uima.analysis_engine.service.AnalysisEngineServiceStub#callProcess(CAS)
   */
  public void callProcess(CAS aCAS)
      throws ResourceServiceException
  {
    try
    {
      AFrame requestFrame = new AFrame();
      requestFrame.fset(Constants.VINCI_COMMAND, Constants.ANNOTATE);
      //  serialize CAS (including type system)
      CASMgr cas = (CASMgr) aCAS;
      CASCompleteSerializer serializer = Serialization.serializeCASComplete(cas);

      requestFrame.fsetTrueBinary("BinaryCAS", SerializationUtils.serialize(serializer));

      AFrame responseFrame = (AFrame) mVinciClient.sendAndReceive(requestFrame);

      //deserialize CAS from response frame
      byte[] responseCasBytes = responseFrame.fgetTrueBinary("BinaryCAS");
      CASSerializer responseSerializer = (CASSerializer) SerializationUtils
          .deserialize(responseCasBytes);
      ((CASImpl) cas).reinit(responseSerializer);

      //also read annotation time and enter into AnalysisEngineManagementMBean
      int annotationTime = responseFrame.fgetInt(Constants.ANNOTATION_TIME);
      if (annotationTime > 0)
      {
        AnalysisEngineManagementImpl mbean = 
          (AnalysisEngineManagementImpl)mOwner.getManagementInterface();
        mbean.reportAnalysisTime(annotationTime);
      }
    }
    catch (Exception e)
    {
      throw new ResourceServiceException(e);
    }
  }
  
  /**
   * @see org.apache.uima.analysis_engine.service.AnalysisEngineServiceStub#callBatchProcessComplete()
   */
  public void callBatchProcessComplete() throws ResourceServiceException
  {
    try
    {
      //  create Vinci Frame ( Data Cargo)
      AFrame queryFrame = new AFrame();
      //  Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", Constants.BATCH_PROCESS_COMPLETE);

      mVinciClient.send(queryFrame); //oneway call
    }
    catch (Exception e)
    {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.service.AnalysisEngineServiceStub#callCollectionProcessComplete()
   */
  public void callCollectionProcessComplete() throws ResourceServiceException
  {
    try
    {
      //  create Vinci Frame ( Data Cargo)
      AFrame queryFrame = new AFrame();
      //  Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", Constants.COLLECTION_PROCESS_COMPLETE);

      //make RPC call (return val ignored)
      mVinciClient.rpc(queryFrame);
    }
    catch (Exception e)
    {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see org.apache.uima.resource.service.impl.ResourceServiceStub#destroy()
   */
  public void destroy()
  {
    mVinciClient.close();
  }

}
