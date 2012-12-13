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
package org.uimafit.util;

import org.apache.uima.jcas.JCas;
import org.uimafit.type.AnalyzedText;

/**
 * @author Philip Ogren
 */

public class TypeSystemUtil {

	public static String getAnalyzedText(JCas jCas) {
		return _getAnalyzedText(jCas).getText();
	}

	public static void setAnalyzedText(JCas jCas, String text) {
		_getAnalyzedText(jCas).setText(text);
	}

	private static AnalyzedText _getAnalyzedText(JCas jCas) {
		AnalyzedText analyzedText = JCasUtil.selectByIndex(jCas, AnalyzedText.class, 0);
		if (analyzedText == null) {
			analyzedText = new AnalyzedText(jCas);
			analyzedText.setText(jCas.getDocumentText());
			analyzedText.addToIndexes();
		}
		return analyzedText;
	}
}
