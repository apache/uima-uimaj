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
package org.uimafit.data;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.XMLSerializer;
import org.uimafit.factory.JCasFactory;
import org.uimafit.testing.factory.TokenBuilder;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;
import org.xml.sax.SAXException;

/**
 * @author Steven Bethard, Philip Ogren
 */

public class CreateSampleXMIFile {

	public static void main(String[] args) throws UIMAException, SAXException, IOException {
		TokenBuilder<Token, Sentence> tokenBuilder = new TokenBuilder<Token, Sentence>(Token.class,
				Sentence.class, "pos", "stem");
		JCas jCas = JCasFactory.createJCas();
		String text = "Me and all my friends are non-conformists.";
		tokenBuilder.buildTokens(jCas, text, "Me and all my friends are non - conformists .",
				"M A A M F A N - C .", "me and all my friend are non - conformist .");

		FileOutputStream out = new FileOutputStream("src/test/resources/data/docs/test.xmi");
		XmiCasSerializer ser = new XmiCasSerializer(jCas.getTypeSystem());
		XMLSerializer xmlSer = new XMLSerializer(out, false);
		ser.serialize(jCas.getCas(), xmlSer.getContentHandler());
		out.close();

	}
}
