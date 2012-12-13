/*
 Copyright 2009-2010
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

package org.uimafit.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.uimafit.ComponentTestBase;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;
import org.uimafit.util.ContainmentIndex.Type;

/**
 * Unit test for {@link ContainmentIndex}.
 * 
 * @author Richard Eckart de Castilho
 */
public class ContainmentIndexTest extends ComponentTestBase {
	@Test
	public void test() throws Exception {
		String text = "Will you come home today ? \n No , tomorrow !";
		tokenBuilder.buildTokens(jCas, text);

		List<Sentence> sentences = new ArrayList<Sentence>(select(jCas, Sentence.class));
		List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));

		ContainmentIndex<Sentence, Token> idx = ContainmentIndex.create(jCas, Sentence.class,
				Token.class, Type.BOTH);

		assertEquals(selectCovered(Token.class, sentences.get(0)),
				idx.containedIn(sentences.get(0)));
		assertEquals(selectCovered(Token.class, sentences.get(1)),
				idx.containedIn(sentences.get(1)));

		assertEquals(asList(sentences.get(0)), idx.containing(tokens.get(0)));
		assertEquals(asList(sentences.get(1)), idx.containing(tokens.get(tokens.size() - 1)));

		assertTrue(idx.isContainedIn(sentences.get(0), tokens.get(0)));
		assertFalse(idx.isContainedIn(sentences.get(0), tokens.get(tokens.size() - 1)));

		// After removing the annotation the index has to be rebuilt.
		assertTrue(idx.isContainedInAny(tokens.get(0)));
		sentences.get(0).removeFromIndexes();
		idx = ContainmentIndex.create(jCas, Sentence.class, Token.class, Type.BOTH);
		assertFalse(idx.isContainedInAny(tokens.get(0)));
	}
}
