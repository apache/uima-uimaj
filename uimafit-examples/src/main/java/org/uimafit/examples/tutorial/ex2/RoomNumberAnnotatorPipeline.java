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
package org.uimafit.examples.tutorial.ex2;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitive;
import static org.apache.uima.fit.util.JCasUtil.select;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.uimafit.examples.tutorial.type.RoomNumber;

/**
 * 
 * @author Philip Ogren
 * 
 */
public class RoomNumberAnnotatorPipeline {

	public static void main(String[] args) throws UIMAException {
		AnalysisEngine analysisEngine = createPrimitive(RoomNumberAnnotator.class, "Patterns",
				new String[] { "\\b[0-4]\\d-[0-2]\\d\\d\\b", "\\b[G1-4][NS]-[A-Z]\\d\\d\\b" },
				"Locations", new String[] { "Downtown", "Uptown" });

		JCas jCas = analysisEngine.newJCas();
		String text = "The meeting was moved from Yorktown 01-144 to Hawthorne 1S-W33.";
		jCas.setDocumentText(text);

		analysisEngine.process(jCas);

		for (RoomNumber roomNumber : select(jCas, RoomNumber.class)) {
			System.out.println(roomNumber.getCoveredText() + "\tbuilding = "
					+ roomNumber.getBuilding());
		}
	}
}
