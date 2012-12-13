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
package org.uimafit.examples.experiment.pos;

import static org.uimafit.examples.experiment.pos.ViewNames.GOLD_VIEW;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.component.ViewCreatorAnnotator;
import org.uimafit.examples.type.Sentence;
import org.uimafit.examples.type.Token;

/**
 * This AE assumes that their is part-of-speech tagged text in the default view
 * with the format "word/tag word/tag...". It converts this data into Token
 * objects and plain text which are posted to the GOLD_VIEW.
 * 
 * @author Philip
 * 
 */
@SofaCapability(inputSofas = CAS.NAME_DEFAULT_SOFA, outputSofas = GOLD_VIEW)
public class GoldTagger extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		try {
			JCas defaultView = jCas.getView(CAS.NAME_DEFAULT_SOFA); // see
																	// javadoc
																	// comment
																	// for
																	// SofaCapability
																	// for why
																	// we have
																	// to
																	// retrieve
																	// the
																	// default
																	// view from
																	// the JCas
			String tagData = defaultView.getDocumentText();

			JCas goldView = ViewCreatorAnnotator.createViewSafely(jCas, GOLD_VIEW);

			String[] wordTagPairs = tagData.split("\\s+");
			StringBuffer text = new StringBuffer();
			int offset = 0;
			List<Token> tokens = new ArrayList<Token>();
			for (String wordTagPair : wordTagPairs) {
				String word = wordTagPair.split("/")[0];
				String tag = wordTagPair.split("/")[1];
				text.append(word);
				Token token = new Token(goldView, offset, text.length());
				token.setPos(tag);
				tokens.add(token);
				text.append(" ");
				offset += word.length() + 1;
			}

			goldView.setDocumentText(text.toString().trim());
			new Sentence(goldView, 0, goldView.getDocumentText().length()).addToIndexes();

			for (Token token : tokens) {
				token.addToIndexes();
			}
		}
		catch (CASException ce) {
			throw new AnalysisEngineProcessException(ce);
		}
	}

}
