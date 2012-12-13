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
package org.uimafit.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.junit.Test;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;

/**
 * @author Steven Bethard, Philip Ogren
 */

public class AnnotationFactoryTest extends ComponentTestBase {

	@Test
	public void testCreateAnnotation() throws UIMAException {
		Token token = AnnotationFactory.createAnnotation(jCas, 0, 10, Token.class);
		assertEquals(0, token.getBegin());
		assertEquals(10, token.getEnd());

		Sentence sentence = AnnotationFactory.createAnnotation(jCas, 0, 10, Sentence.class);
		assertEquals(0, sentence.getBegin());
		assertEquals(10, sentence.getEnd());

		UIMAException ue = null;
		try {
			AnnotationFactory.createAnnotation(null, 0, 10, Sentence.class);
		}
		catch (UIMAException e) {
			ue = e;
		}
		assertNotNull(ue);

	}
}
