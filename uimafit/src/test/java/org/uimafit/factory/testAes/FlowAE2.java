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

import java.util.Arrays;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.TypeSystemUtil;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;

/**
 * @author Philip Ogren
 */

public class FlowAE2 extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		String analyzedText = TypeSystemUtil.getAnalyzedText(jCas);
		String sortedText = sort(analyzedText);
		TypeSystemUtil.setAnalyzedText(jCas, sortedText);
	}

	public static String sort(String text) {
		char[] chars = text.toCharArray();
		Arrays.sort(chars);
		return new String(chars).trim();
	}
}
