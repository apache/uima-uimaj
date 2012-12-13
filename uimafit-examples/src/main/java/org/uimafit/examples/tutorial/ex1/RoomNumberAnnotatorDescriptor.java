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
package org.uimafit.examples.tutorial.ex1;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * This class provides a main method which shows how to generate an xml descriptor file using the
 * RoomNumberAnnotator class definition. The resulting XML descriptor file is the same as the one
 * provided in the uimaj-examples except that instead of building the file in parallel with the
 * class definition, it is now built completely by using the class definition.
 * 
 * @author Philip Ogren
 * 
 */
public class RoomNumberAnnotatorDescriptor {

	public static AnalysisEngineDescription createDescriptor()
			throws ResourceInitializationException {
		TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
				.createTypeSystemDescription();
		return AnalysisEngineFactory.createPrimitiveDescription(RoomNumberAnnotator.class,
				typeSystemDescription);
	}

	public static void main(String[] args) throws Exception {
		File outputDirectory = new File("target/example-output/ex1/");
		outputDirectory.mkdirs();
		AnalysisEngineDescription aed = createDescriptor();
		aed.toXML(new FileOutputStream(new File(outputDirectory, "RoomNumberAnnotator.xml")));
	}

}
