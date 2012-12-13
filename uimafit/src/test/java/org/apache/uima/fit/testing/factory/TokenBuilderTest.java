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

package org.apache.uima.fit.testing.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.pear.util.FileUtil;
import org.junit.Test;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;

/**
 * @author Steven Bethard, Philip Ogren
 */

public class TokenBuilderTest extends ComponentTestBase {

	@Test
	public void test1() throws UIMAException {
		String text = "What if we built a rocket ship made of cheese?"
				+ "We could fly it to the moon for repairs.";
		tokenBuilder
				.buildTokens(
						jCas,
						text,
						"What if we built a rocket ship made of cheese ? \r\n We could fly it to the moon for repairs .",
						"A B C D E F G H I J K L M N O P Q R S T U");

		FSIndex<Annotation> sentenceIndex = jCas.getAnnotationIndex(Sentence.type);
		assertEquals(2, sentenceIndex.size());
		FSIterator<Annotation> sentences = sentenceIndex.iterator();
		Sentence sentence = (Sentence) sentences.next();
		assertEquals("What if we built a rocket ship made of cheese?", sentence.getCoveredText());
		sentence = (Sentence) sentences.next();
		assertEquals("We could fly it to the moon for repairs.", sentence.getCoveredText());

		FSIndex<Annotation> tokenIndex = jCas.getAnnotationIndex(Token.type);
		assertEquals(21, tokenIndex.size());
		Token token = JCasUtil.selectByIndex(jCas, Token.class, 0);
		testToken(token, "What", 0, 4, "A", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 1);
		testToken(token, "if", 5, 7, "B", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 9);
		testToken(token, "cheese", 39, 45, "J", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 10);
		testToken(token, "?", 45, 46, "K", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 11);
		testToken(token, "We", 46, 48, "L", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 12);
		testToken(token, "could", 49, 54, "M", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 19);
		testToken(token, "repairs", 78, 85, "T", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 20);
		testToken(token, ".", 85, 86, "U", null);
	}

	@Test
	public void test2() throws UIMAException {
		String text = "What if we built a rocket ship made of cheese? \n"
				+ "We could fly it to the moon for repairs.";
		tokenBuilder
				.buildTokens(
						jCas,
						text,
						"What if we built a rocket ship made of cheese ? \n We could fly it to the moon for repairs .",
						"A B C D E F G H I J K L M N O P Q R S T U");

		Token token = JCasUtil.selectByIndex(jCas, Token.class, 10);
		testToken(token, "?", 45, 46, "K", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 11);
		testToken(token, "We", 48, 50, "L", null);

		jCas.reset();
		text = "What if we built a rocket ship made of cheese? \n"
				+ "We could fly it to the moon for repairs.";
		tokenBuilder
				.buildTokens(
						jCas,
						text,
						"What if we built a rocket ship made of cheese ?\nWe could fly it to the moon for repairs .",
						"A B C D E F G H I J K L M N O P Q R S T U");

		token = JCasUtil.selectByIndex(jCas, Token.class, 10);
		testToken(token, "?", 45, 46, "K", null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 11);
		testToken(token, "We", 48, 50, "L", null);
	}

	@Test
	public void test3() throws UIMAException {
		String text = "If you like line writer, then you should really check out line rider.";
		tokenBuilder.buildTokens(jCas, text);

		FSIndex<Annotation> tokenIndex = jCas.getAnnotationIndex(Token.type);
		assertEquals(13, tokenIndex.size());
		Token token = JCasUtil.selectByIndex(jCas, Token.class, 0);
		testToken(token, "If", 0, 2, null, null);
		token = JCasUtil.selectByIndex(jCas, Token.class, 12);
		testToken(token, "rider.", 63, 69, null, null);
		FSIndex<Annotation> sentenceIndex = jCas.getAnnotationIndex(Sentence.type);
		assertEquals(1, sentenceIndex.size());
		Sentence sentence = JCasUtil.selectByIndex(jCas, Sentence.class, 0);
		assertEquals(text, sentence.getCoveredText());
	}

	private void testToken(Token token, String coveredText, int begin, int end,
			String partOfSpeech, String stem) {
		assertEquals(coveredText, token.getCoveredText());
		assertEquals(begin, token.getBegin());
		assertEquals(end, token.getEnd());
		assertEquals(partOfSpeech, token.getPos());
		assertEquals(stem, token.getStem());
	}

	@Test
	public void testSpaceSplit() {
		String[] splits = " asdf ".split(" ");
		assertEquals(2, splits.length);
	}

	@Test
	public void testBadInput() throws UIMAException {
		String text = "If you like line writer, then you should really check out line rider.";
		IllegalArgumentException iae = null;
		try {
			tokenBuilder.buildTokens(jCas, text,
					"If you like line rider, then you really don't need line writer");
		}
		catch (IllegalArgumentException e) {
			iae = e;
		}
		assertNotNull(iae);
	}

	@Test
	public void testStems() throws UIMAException {
		String text = "Me and all my friends are non-conformists.";
		tokenBuilder.buildTokens(jCas, text, "Me and all my friends are non - conformists .",
				"M A A M F A N - C .", "me and all my friend are non - conformist .");

		assertEquals("Me and all my friends are non-conformists.", jCas.getDocumentText());
		Token friendToken = JCasUtil.selectByIndex(jCas, Token.class, 4);
		assertEquals("friends", friendToken.getCoveredText());
		assertEquals("F", friendToken.getPos());
		assertEquals("friend", friendToken.getStem());
	}

	@Test
	public void test4() throws UIMAException {
		String text = "a b-c de--fg h,i,j,k";
		tokenBuilder.buildTokens(jCas, text, "a b - c d e - - f g h , i , j , k");

		FSIterator<Annotation> tokens = jCas.getAnnotationIndex(Token.type).iterator();
		int tokenCount = 0;
		while (tokens.hasNext()) {
			tokenCount++;
			tokens.next();
		}
		assertEquals(17, tokenCount);
	}

	@Test
	public void test5() throws Exception {
		JCas myView = jCas.createView("MyView");

		tokenBuilder.buildTokens(myView, "red and blue cars and tipsy motorcycles");

		Token token = JCasUtil.selectByIndex(myView, Token.class, 6);
		assertEquals("motorcycles", token.getCoveredText());

	}

	@Test
	public void testNewlinesFromFile() throws Exception {
		String text = FileUtil.loadTextFile(new File(
				"src/test/resources/data/docs/unix-newlines.txt"), "UTF-8");
		text = text.substring(1); // remove "\uFEFF" character from begining of text
		tokenBuilder.buildTokens(jCas, text);

		Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
		assertEquals(4, sentences.size());
		Iterator<Sentence> iterator = sentences.iterator();
		assertEquals("sentence 1.", iterator.next().getCoveredText());
		assertEquals("sentence 2.", iterator.next().getCoveredText());
		assertEquals("sentence 3.", iterator.next().getCoveredText());
		assertEquals("sentence 4.", iterator.next().getCoveredText());

		jCas.reset();
		text = FileUtil.loadTextFile(new File("src/test/resources/data/docs/windows-newlines.txt"),
				"UTF-8");
		text = text.substring(1); // remove "\uFEFF" character from begining of text
		tokenBuilder.buildTokens(jCas, text);

		sentences = JCasUtil.select(jCas, Sentence.class);
		assertEquals(4, sentences.size());
		iterator = sentences.iterator();
		assertEquals("sentence 1.", iterator.next().getCoveredText());
		assertEquals("sentence 2.", iterator.next().getCoveredText());
		assertEquals("sentence 3.", iterator.next().getCoveredText());
		assertEquals("sentence 4.", iterator.next().getCoveredText());

	}

}
