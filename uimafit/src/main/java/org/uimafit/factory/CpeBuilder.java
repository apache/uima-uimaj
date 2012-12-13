/*
 Copyright 2011
 Ubiquitous Knowledge Processing (UKP) Lab
 Technische Universitaet Darmstadt
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.uimafit.factory;

import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.UIMAFramework.produceCollectionProcessingEngine;
import static org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory.produceCasProcessor;
import static org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory.produceCollectionReader;
import static org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory.produceDescriptor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.impl.metadata.CpeDefaultValues;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeInclude;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

/**
 * Build a Collection Processing Engine from a {@link CollectionReaderDescription} and a
 * {@link AnalysisEngineDescription}. If an aggregate analysis engine description is used, the
 * builder will add each child of the aggregate engine as one processor to the engine. This works
 * only for aggregate analysis engines using a {@link FixedFlow}.
 *
 * @author Richard Eckart de Castilho
 */
public class CpeBuilder
{
//	private final Log log = LogFactory.getLog(getClass());

	private static final String ACTION_ON_MAX_ERROR = "terminate";

	/**
	 * used for calculating the CAS pool size which needs to be adjusted to the number of parallel
	 * pipelines
	 */
	private int maxProcessingUnitThreadCount = 1;

	private final CpeDescription cpeDesc = produceDescriptor();

	public void setMaxProcessingUnitThreadCount(int aMaxProcessingUnitThreadCount)
	{
		maxProcessingUnitThreadCount = aMaxProcessingUnitThreadCount;
	}

	public void setReader(CollectionReaderDescription aDesc)
		throws IOException, SAXException, CpeDescriptorException
	{
		// Remove all collection readers
		cpeDesc.setAllCollectionCollectionReaders(new CpeCollectionReader[0]);

		URL descUrl = materializeDescriptor(aDesc).toURI().toURL();
		CpeCollectionReader reader = produceCollectionReader(descUrl.toString());
		cpeDesc.addCollectionReader(reader);
	}

	public void setAnalysisEngine(AnalysisEngineDescription aDesc)
		throws IOException, SAXException, CpeDescriptorException, InvalidXMLException
	{
		// Remove all CAS processors
		cpeDesc.setCpeCasProcessors(null);

		if (aDesc.isPrimitive()) {
			// For a primitive AE we just add it.
			CpeIntegratedCasProcessor proc = createProcessor("", aDesc);
			cpeDesc.addCasProcessor(proc);
		}
		else {
			// For an aggregate AE we dive into the first aggregation level and add each of the
			// contained AEs separately, thus allowing us to control their properties separately

			Map<String, ResourceSpecifier> delegates = aDesc.getDelegateAnalysisEngineSpecifiers();
			FixedFlow flow = (FixedFlow) aDesc.getAnalysisEngineMetaData().getFlowConstraints();
			for (String key : flow.getFixedFlow()) {
				AnalysisEngineDescription aeDesc = (AnalysisEngineDescription) delegates.get(key);
//				boolean multi = aeDesc.getAnalysisEngineMetaData().getOperationalProperties()
//						.isMultipleDeploymentAllowed();
//				log.info("["+key+"] runs "+ (multi ? "multi-threaded" : "single-threaded"));
				CpeIntegratedCasProcessor proc = createProcessor(key, aeDesc);
				cpeDesc.addCasProcessor(proc);
			}
		}
	}

	public CpeDescription getCpeDescription()
	{
		return cpeDesc;
	}
	
	public CollectionProcessingEngine createCpe(StatusCallbackListener aListener)
		throws ResourceInitializationException, CpeDescriptorException
	{
		ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
		if (maxProcessingUnitThreadCount == 0) {
			cpeDesc.getCpeCasProcessors().setPoolSize(3);
		}
		else {
			cpeDesc.getCpeCasProcessors().setPoolSize(maxProcessingUnitThreadCount + 2);
			cpeDesc.setProcessingUnitThreadCount(maxProcessingUnitThreadCount);
		}
		CollectionProcessingEngine cpe = produceCollectionProcessingEngine(cpeDesc, resMgr, null);
		cpe.addStatusCallbackListener(aListener);
		return cpe;
	}

	/**
	 * Writes a temporary file containing a XML descriptor of the given resource. Returns the file.
	 *
	 * @param resource
	 *            A resource specifier that should we materialized.
	 * @return The file containing the XML representation of the given resource.
	 */
	private static File materializeDescriptor(ResourceSpecifier resource)
		throws IOException, SAXException
	{
		File tempDesc = File.createTempFile("desc", ".xml");
		tempDesc.deleteOnExit();
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(tempDesc));
			resource.toXML(out);
		}
		finally {
			IOUtils.closeQuietly(out);
		}

		return tempDesc;
	}

	private static CpeIntegratedCasProcessor createProcessor(String key, AnalysisEngineDescription aDesc)
		throws IOException, SAXException, CpeDescriptorException
	{
		URL descUrl = materializeDescriptor(aDesc).toURI().toURL();

		CpeInclude cpeInclude = getResourceSpecifierFactory().createInclude();
		cpeInclude.set(descUrl.toString());

		CpeComponentDescriptor ccd = getResourceSpecifierFactory().createDescriptor();
		ccd.setInclude(cpeInclude);

		CpeIntegratedCasProcessor proc = produceCasProcessor(key);
		proc.setCpeComponentDescriptor(ccd);
		proc.setAttributeValue(CpeDefaultValues.PROCESSING_UNIT_THREAD_COUNT, 1);
		proc.setActionOnMaxError(ACTION_ON_MAX_ERROR);
		proc.setMaxErrorCount(0);

		return proc;
	}
}
