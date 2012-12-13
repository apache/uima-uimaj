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

package org.uimafit.factory.testAes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.JCasFlow_ImplBase;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.apache.uima.jcas.JCas;

/**
 * @author Philip Ogren, Fabio Mancinelli
 * 
 *         NOTE: this class extends org.uimafit.component.JCasFlowController_ImplBase
 */

public class ReversableTestFlowController extends org.uimafit.component.JCasFlowController_ImplBase {

	public static final String PARAM_REVERSE_ORDER = ConfigurationParameterFactory
			.createConfigurationParameterName(ReversableTestFlowController.class, "reverseOrder");
	@ConfigurationParameter
	private boolean reverseOrder = false;

	@Override
	public Flow computeFlow(JCas jCas) throws AnalysisEngineProcessException {
		return new ReversableFlow(getContext(), reverseOrder);
	}

	private static class ReversableFlow extends JCasFlow_ImplBase {
		private List<String> keys = new ArrayList<String>();

		private int i = 0;

		public ReversableFlow(FlowControllerContext context, boolean reverseOrder) {
			Iterator<Map.Entry<String, AnalysisEngineMetaData>> iterator = context
					.getAnalysisEngineMetaDataMap().entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, AnalysisEngineMetaData> entry = iterator.next();
				String key = entry.getKey();
				keys.add(key);
			}
			Collections.sort(keys);
			if (reverseOrder) {
				Collections.reverse(keys);
			}
		}

		public Step next() throws AnalysisEngineProcessException {
			if (i < keys.size()) {
				return new SimpleStep(keys.get(i++));
			}

			return new FinalStep();
		}
	}

}
