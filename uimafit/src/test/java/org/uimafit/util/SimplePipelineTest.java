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

package org.uimafit.util;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.junit.Test;
import org.uimafit.ComponentTestBase;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.JCasFactory;
import org.uimafit.factory.testAes.Annotator1;
import org.uimafit.factory.testAes.Annotator2;
import org.uimafit.factory.testAes.Annotator3;
import org.uimafit.pipeline.SimplePipeline;

/**
 * @author Philip Ogren
 */

public class SimplePipelineTest extends ComponentTestBase {

	@Test
	public void test1() throws UIMAException, IOException {
		JCasFactory.loadJCas(jCas, "src/test/resources/data/docs/test.xmi");
		AnalysisEngineDescription aed1 = AnalysisEngineFactory.createPrimitiveDescription(
				Annotator1.class, typeSystemDescription);
		AnalysisEngineDescription aed2 = AnalysisEngineFactory.createPrimitiveDescription(
				Annotator2.class, typeSystemDescription);
		AnalysisEngineDescription aed3 = AnalysisEngineFactory.createPrimitiveDescription(
				Annotator3.class, typeSystemDescription);
		SimplePipeline.runPipeline(jCas, aed1, aed2, aed3);

	}
}
