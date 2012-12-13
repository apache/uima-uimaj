/* 
 Copyright 2010 Regents of the University of Colorado.  
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

package org.apache.uima.fit.descriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.Capability;
import org.junit.Test;
import org.uimafit.factory.testAes.Annotator4;

/**
 * @author Philip Ogren
 */

public class TypeCapabilityTest extends ComponentTestBase {

	@Test
	public void testTC() throws ResourceInitializationException {
		AnalysisEngineDescription aed = AnalysisEngineFactory.createPrimitiveDescription(
				Annotator4.class, typeSystemDescription);
		Capability[] capabilities = aed.getAnalysisEngineMetaData().getCapabilities();
		assertEquals(1, capabilities.length);
		Capability capability = capabilities[0];
		TypeOrFeature[] inputs = capability.getInputs();
		assertEquals(1, inputs.length);
		assertEquals("org.uimafit.type.Token", inputs[0].getName());
		assertTrue(inputs[0].isType());

		TypeOrFeature[] outputs = capability.getOutputs();
		assertEquals(1, outputs.length);
		assertEquals("org.uimafit.type.Token:pos", outputs[0].getName());
		assertFalse(outputs[0].isType());

	}
}
