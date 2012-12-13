/* 
 Copyright 2009-2010	Regents of the University of Colorado.  
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
package org.uimafit.examples.getstarted;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

/**
 * 
 * @author Philip Ogren
 * 
 */
public class GetStartedQuickDescriptor {

	public static void main(String[] args) throws ResourceInitializationException,
			FileNotFoundException, SAXException, IOException {
		// uimaFIT automatically uses all type systems listed in META-INF/org.uimafit/types.txt
		
		// Instantiate the analysis engine using the value "uimaFIT" for the parameter
		// PARAM_STRING ("stringParam").
		AnalysisEngineDescription analysisEngineDescription = AnalysisEngineFactory
				.createPrimitiveDescription(GetStartedQuickAE.class,
						GetStartedQuickAE.PARAM_STRING, "uimaFIT");
		
		// Write the descriptor to an XML file
		analysisEngineDescription.toXML(new FileOutputStream("GetStartedQuickAE.xml"));
	}
}
