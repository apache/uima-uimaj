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

import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.apache.uima.fit.factory.FlowControllerFactory.createFlowControllerDescription;
import static org.junit.Assert.assertNotNull;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.Step;
import org.junit.Test;
import org.uimafit.component.CasFlowController_ImplBase;
import org.uimafit.component.NoOpAnnotator;
import org.uimafit.factory.testRes.TestExternalResource;

/**
 * @author Shuo Yang
 * @author Richard Eckart de Castilho
 */
public class FlowControllerFactoryExternalResourceTest {
	@Test
	public void testAutoExternalResourceBinding() throws Exception {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(NoOpAnnotator.class));
		builder.setFlowControllerDescription(createFlowControllerDescription(
				TestFlowController.class,
				TestFlowController.PARAM_RESOURCE,
				createExternalResourceDescription(TestExternalResource.class,
						TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE)));
		AnalysisEngine aggregateEngine = builder.createAggregate();
		aggregateEngine.process(aggregateEngine.newCAS());
	}

	public static class TestFlowController extends CasFlowController_ImplBase {
		public final static String PARAM_RESOURCE = "resource";
		@ExternalResource(key = PARAM_RESOURCE)
		private TestExternalResource resource;

		@Override
		public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
			assertNotNull(resource);
			resource.assertConfiguredOk();
			return new TestFlowObject();
		}
	}
	
	public static class TestFlowObject extends CasFlow_ImplBase {
		public Step next() throws AnalysisEngineProcessException {
			return new FinalStep();
		}
	}
}
