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

package org.apache.uima.impl.analysis_engine.service.vinci;

import java.net.InetAddress;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineServiceStub;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.impl.analysis_engine.AnalysisEngineManagementImpl;
import org.apache.uima.impl.analysis_engine.service.vinci.util.Constants;
import org.apache.uima.impl.analysis_engine.service.vinci.util.VinciSaxParser;
import org.apache.uima.impl.collection.service.vinci.CASTransportable;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.SaxDeserializer;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.TransportableFactory;
import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.context.VinciContext;
import org.apache.vinci.transport.document.AFrame;

public class VinciAnalysisEngineServiceStub implements AnalysisEngineServiceStub
{
	private VinciClient mVinciClient;

	private Resource mOwner;

	private static final boolean debug = System.getProperty("DEBUG") != null;

	public VinciAnalysisEngineServiceStub(String endpointURI, Resource owner) throws ResourceInitializationException
	{
		this(endpointURI, null, owner, null);
	}

	public VinciAnalysisEngineServiceStub(String endpointURI, Integer timeout, Resource owner, Parameter[] parameters) throws ResourceInitializationException
	{
		mOwner = owner;

		//open Vinci connection
		try
		{
			String vnsHost;
			if (parameters != null && (vnsHost = VinciBinaryAnalysisEngineServiceStub.getParameterValueFor("VNS_HOST", parameters)) != null)
			{
				//		Override vinci default VNS settings	
				VinciContext vctx = new VinciContext(InetAddress.getLocalHost().getCanonicalHostName(), 0);
				vctx.setVNSHost(vnsHost);
				String vnsPort = VinciBinaryAnalysisEngineServiceStub.getParameterValueFor("VNS_PORT", parameters);
				if (vnsPort != null)
				{
					vctx.setVNSPort(Integer.parseInt(vnsPort));
				}
				else
				{
					vctx.setVNSPort(9000);   // use default. 
				}
				if (debug)
				{
					System.out.println("Establishing connnection to " + endpointURI + " using VNS_HOST:" + vnsHost + " and VNS_PORT=" + ((vnsPort == null) ? "9000" : vnsPort));
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
					System.out.println("Establishing connnedction to " + endpointURI);
				}
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
  
	/**
	 * @see org.apache.uima.resource.service.ResourceServiceStub#callGetMetaData()
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

			//  Parse the XML into the ProcessingResourceMetaData object
			SaxDeserializer saxDeser = UIMAFramework.getXMLParser().newSaxDeserializer(null, null, false);

			VinciSaxParser vinciSaxParser = new VinciSaxParser();
			vinciSaxParser.setContentHandler(saxDeser);
			vinciSaxParser.parse(resultFrame);
			ProcessingResourceMetaData metadata = (ProcessingResourceMetaData) saxDeser.getObject();

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
	public void callProcess(CAS aCAS) throws ResourceServiceException
	{
		doProcess(aCAS);
	}

	/**
	 * @see CasObjectProcessorServiceStub#callProcessCas(CAS)
	 */
	public void callProcessCas(CAS aCAS) throws ResourceServiceException
	{
		doProcess(aCAS);
	}

	/**
	 * The actual process call.
	 */
	private void doProcess(CAS aCAS) throws ResourceServiceException
	{
		try
		{
			aCAS = ((CASImpl) aCAS).getBaseCAS();

			// create CASTransportable ... always send the base CAS
			final CASTransportable query = new CASTransportable(aCAS, null, mOwner.getUimaContext(), true);
			query.setCommand("Annotate");

      mVinciClient.sendAndReceive(query, new TransportableFactory()
      {
        public Transportable makeTransportable()
        {
          // query.ignoreResponse = true; // TESTING
          return query;
        }
      });

			//if service reply included the time taken to do the analysis,
			//add that to the AnalysisEngineManagement MBean
  		int annotationTime = query.getExtraDataFrame().fgetInt("TAE:AnnotationTime");
			if (annotationTime > 0)
		  {
        AnalysisEngineManagementImpl mbean = 
          (AnalysisEngineManagementImpl)mOwner.getUimaContextAdmin().getManagementInterface();
        mbean.reportAnalysisTime(annotationTime);			
      }
		}
		catch (Exception e)
		{
			throw new ResourceServiceException(e);
		}
	}
  
	/**
	 * @see org.apache.uima.impl.resource.service.ResourceServiceStub#destroy()
	 */
	public void destroy()
	{
		mVinciClient.close();
	}

	/**
	 * @see org.apache.uima.impl.collection.service.CasObjectProcessorServiceStub#callBatchProcessComplete()
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
	 * @see org.apache.uima.impl.collection.service.CasObjectProcessorServiceStub#callCollectionProcessComplete()
	 */
	public void callCollectionProcessComplete() throws ResourceServiceException
	{
		try
		{
			//  create Vinci Frame ( Data Cargo)
			AFrame queryFrame = new AFrame();
			//  Add Vinci Command, so that the receiving service knows what to do
			queryFrame.fadd("vinci:COMMAND", Constants.COLLECTION_PROCESS_COMPLETE);

			//make RPC call (no return val)
			mVinciClient.rpc(queryFrame);
		}
		catch (Exception e)
		{
			throw new ResourceServiceException(e);
		}
	}

	/**
	 * @see org.apache.uima.impl.collection.service.CasObjectProcessorServiceStub#callIsReadOnly()
	 */
	public boolean callIsReadOnly() throws ResourceServiceException
	{
		try
		{
			//  create Vinci Frame ( Data Cargo)
			AFrame queryFrame = new AFrame();
			//  Add Vinci Command, so that the receiving service knows what to do
			queryFrame.fadd("vinci:COMMAND", Constants.IS_READONLY);

			//make RPC call
			VinciFrame resultFrame = mVinciClient.rpc(queryFrame);
			boolean result = resultFrame.fgetBoolean("Result");
			return result;
		}
		catch (Exception e)
		{
			throw new ResourceServiceException(e);
		}
	}

	/**
	 * @see org.apache.uima.impl.collection.service.CasObjectProcessorServiceStub#callIsStateless()
	 */
	public boolean callIsStateless() throws ResourceServiceException
	{
		try
		{
			//  create Vinci Frame ( Data Cargo)
			AFrame queryFrame = new AFrame();
			//  Add Vinci Command, so that the receiving service knows what to do
			queryFrame.fadd("vinci:COMMAND", Constants.IS_STATELESS);

			//make RPC call
			VinciFrame resultFrame = mVinciClient.rpc(queryFrame);
			boolean result = resultFrame.fgetBoolean("Result");
			return result;
		}
		catch (Exception e)
		{
			throw new ResourceServiceException(e);
		}
	}

}
