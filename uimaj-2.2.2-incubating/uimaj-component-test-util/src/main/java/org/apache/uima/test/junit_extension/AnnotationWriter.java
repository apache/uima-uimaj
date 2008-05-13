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
package org.apache.uima.test.junit_extension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelTypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

/**
 * The AnnotationWriter class writes specified annotations to an output file.
 * The encoding of the output file is UTF-8
 */

public class AnnotationWriter extends CasConsumer_ImplBase implements CasConsumer
{
	//output file
	private File outFile;
	//output file writer
	private OutputStreamWriter fileWriter;
	//respected annotations
	private String[] tofs;
	//check if reconfigure must be called
	private boolean reconfig = false;
	
	private final static String featureOnlyKey = "feature";

	/**
	 * Initializes this CAS Consumer with the parameters specified in the 
	 * descriptor.
	 * 
	 * @throws ResourceInitializationException if there is error in 
	 * initializing the resources
	 */
	public void initialize() throws ResourceInitializationException
	{

		// extract configuration parameter settings
		String oPath = (String) getUimaContext().getConfigParameterValue("outputFile");

		//Output file should be specified in the descriptor
		if (oPath == null)
		{
			//set reconfiguration - reconfig() must be called
			this.reconfig = true;
		}
		else
		{
			// If specified output directory does not exist, try to create it
			this.outFile = new File(oPath);
			if (this.outFile.getParentFile() != null && !this.outFile.getParentFile().exists())
			{
				if (!this.outFile.getParentFile().mkdirs())
					throw new ResourceInitializationException(
						ResourceInitializationException.RESOURCE_DATA_NOT_VALID,
						new Object[] { oPath, "outputFile" });
			}
			try
			{
				this.fileWriter = new OutputStreamWriter(new FileOutputStream(this.outFile, false), "UTF-8");
			}
			catch (IOException e)
			{
				throw new ResourceInitializationException(e);
			}
		}

		//extract annotation types
		this.tofs = (String[]) getUimaContext().getConfigParameterValue("AnnotationTypes");
		//sort array
		if (this.tofs != null)
			Arrays.sort(this.tofs);

	}

	/**
	 * processTofs() writes als specified types an features to a HashMap.
	 * 
	 * @param aCAS a CAS with a TypeSystem
	 * @param tofs respected tofs
	 * @return HashMap - Map with all types an features.
	 */
	private HashMap processTofs(CAS aCAS, String[] someTofs)
	{
		HashMap types = new HashMap(10);

		for (int i = 0; i < someTofs.length; i++)
		{
			Type type = aCAS.getTypeSystem().getType(someTofs[i]);

			if (type == null) //maybe a feature
			{
				int index = someTofs[i].indexOf(":");

				if (index != -1)
				{
					String typename = someTofs[i].substring(0, index);
					Type typeKey = aCAS.getTypeSystem().getType(typename);

					//get feature object (Vector) for the current type
					Object obj = types.get(typeKey);

					//if type is not included in the typelist create type and add feature
					if (obj == null)
					{
						ArrayList list = new ArrayList(10);
						Feature fs = aCAS.getTypeSystem().getFeatureByFullName(someTofs[i]);
						list.add(0, featureOnlyKey);
						list.add(fs);
						types.put(typeKey, list);
					}
					else //add feature to type
						{
						//cast feature vector for the current type
						ArrayList vec = (ArrayList) obj;
						Feature fs = aCAS.getTypeSystem().getFeatureByFullName(someTofs[i]);
						vec.add(fs);
					}

				}
			}
			else
			{
				//add type as key and a Vector as Feature container 
				if(types.containsKey(type)){
					ArrayList featureList = (ArrayList) types.get(type);
					if(featureList.size() >0 && featureList.get(0).equals(featureOnlyKey)){
						featureList.remove(0);
					}
					// type is already in the list do not overwrite it!
				}else{
					types.put(type, new ArrayList(10));
				}
			}
		}

		return types;
	}

	public synchronized void processCas(CAS aCAS) throws ResourceProcessException
	{
		if (this.reconfig == true)
		{
			throw new ResourceProcessException(
				ResourceInitializationException.CONFIG_SETTING_ABSENT,
				new Object[] { "outputFile" });
		}

		//get low level CAS
		LowLevelCAS ll_cas = aCAS.getLowLevelCAS();

		//get low level TypeSystem
		LowLevelTypeSystem ll_typeSystem = ll_cas.ll_getTypeSystem();

		//get types and feature interessted in
		HashMap types = processTofs(aCAS, this.tofs);

		try
		{
			//iterate and print annotations
			FSIterator typeIterator = aCAS.getAnnotationIndex().iterator();

			for (typeIterator.moveToFirst(); typeIterator.isValid(); typeIterator.moveToNext())
			{
				Iterator it = types.keySet().iterator();

				while (it.hasNext())
				{
					//get current type and features
					Type currentType = (Type) it.next();
					boolean isFeatureOnly = false;
					
					ArrayList featureList = (ArrayList) types.get(currentType);
					if(featureList.size() >0 && featureList.get(0).equals(featureOnlyKey)){
						featureList.remove(0);
						isFeatureOnly = true;
					}
					Feature[] features = (Feature[]) featureList.toArray(new Feature[] {
					});
					
					AnnotationFS annot = (AnnotationFS) typeIterator.get();

					if (annot.getType().getName() == currentType.getName())
					{
						//only for formatting necessary
						boolean firstFeature = true;

						String span = annot.getCoveredText();
						if(!isFeatureOnly){
							this.fileWriter.write(
								annot.getType().getShortName()
									+ "(" + annot.getBegin() + "," + annot.getEnd()	+ "): "	+ span);
						}else{
							this.fileWriter.write(
								annot.getType().getShortName()
									+ ": ");
						}

						for (int f = 0; f < features.length; f++)
						{
							if (firstFeature)
							{
								this.fileWriter.write("  { ");
								firstFeature = false;
							}
							else
							{
								this.fileWriter.write(", ");
							}

							Feature fs = features[f];
							int typeClass = ll_cas.ll_getTypeClass(ll_typeSystem.ll_getCodeForType(fs.getRange()));
							this.fileWriter.write(fs.getShortName() + "=");
							
							switch (typeClass)
							{
								case LowLevelCAS.TYPE_CLASS_FLOAT :
									this.fileWriter.write(Float.toString(annot.getFloatValue(fs)));
									break;

								case LowLevelCAS.TYPE_CLASS_INT :
									this.fileWriter.write(Integer.toString(annot.getIntValue(fs)));
									break;

								case LowLevelCAS.TYPE_CLASS_STRING :
									String value = annot.getStringValue(fs);
									if(value != null) {
										this.fileWriter.write(value);	
									} else {
										this.fileWriter.write("null");
									}
									break;
								
								case LowLevelCAS.TYPE_CLASS_FS:
								
									FeatureStructure fStruct = annot.getFeatureValue(fs);
									if(fStruct != null) {
										this.fileWriter.write(fStruct.toString());	
									} else {
										this.fileWriter.write("null");
									}
									break;
							}
						}

						if (firstFeature == false)
						{
							this.fileWriter.write(" }");
						}

						this.fileWriter.write(System.getProperty("line.separator"));
					}

				}
			}

			this.fileWriter.flush();
		}
		catch (Exception ex)
		{
			throw new ResourceProcessException(ex);
		}

	}

	public void batchProcessComplete(ProcessTrace aTrace) throws ResourceProcessException, IOException
	{
		 // nothing to do here 	
	}

	public void collectionProcessComplete(ProcessTrace aTrace) throws ResourceProcessException, IOException
	{
		if (this.fileWriter != null)
		{
			this.fileWriter.close();
		}
	}

	public void reconfigure() throws ResourceConfigurationException
	{
		//reset reconfiguration - is done
		this.reconfig = false;

		super.reconfigure();
		// extract configuration parameter settings
		String oPath = (String) getUimaContext().getConfigParameterValue("outputFile");
		File oFile = new File(oPath);
		//if output file has changed, close exiting file and open new
		if (!oFile.equals(this.outFile))
		{
			this.outFile = oFile;
			try
			{
				if (this.fileWriter != null)
					this.fileWriter.close();

				// If specified output directory does not exist, try to create it
				if (oFile.getParentFile() != null && !oFile.getParentFile().exists())
				{
					if (!oFile.getParentFile().mkdirs())
						throw new ResourceConfigurationException(
							ResourceInitializationException.RESOURCE_DATA_NOT_VALID,
							new Object[] { oPath, "outputFile" });
				}
				//write result specification to the output file
				this.fileWriter = new OutputStreamWriter(new FileOutputStream(oFile, false), "UTF-8");
			}
			catch (IOException e)
			{
				throw new ResourceConfigurationException();
			}
		}

		//extract annotation types
		this.tofs = (String[]) getUimaContext().getConfigParameterValue("AnnotationTypes");
		//sort array
		if (this.tofs != null)
			Arrays.sort(this.tofs);

	}

	public void destroy()
	{
		if (this.fileWriter != null)
		{
			try
			{
				this.fileWriter.close();
			}
			catch (IOException e)
			{
				// ignore IOException on destroy
			}
		}
	}

}
