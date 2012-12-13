/*
 Copyright 2009
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

 getCoveredAnnotations() contains code adapted from the UIMA Subiterator class.
 */
package org.apache.uima.fit.util;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.exists;
import static org.apache.uima.fit.util.JCasUtil.getAnnotationType;
import static org.apache.uima.fit.util.JCasUtil.getType;
import static org.apache.uima.fit.util.JCasUtil.getView;
import static org.apache.uima.fit.util.JCasUtil.indexCovered;
import static org.apache.uima.fit.util.JCasUtil.indexCovering;
import static org.apache.uima.fit.util.JCasUtil.isCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectBetween;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectCovering;
import static org.apache.uima.fit.util.JCasUtil.selectFollowing;
import static org.apache.uima.fit.util.JCasUtil.selectPreceding;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;
import static org.apache.uima.fit.util.JCasUtil.selectSingleRelative;
import static org.apache.uima.fit.util.JCasUtil.toText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;
import org.uimafit.type.AnalyzedText;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;

/**
 * Test cases for {@link JCasUtil}.
 *
 * @author Richard Eckart de Castilho
 * @author Torsten Zesch
 */
public class JCasUtilTest extends ComponentTestBase {
	/**
	 * Test Tokens (Stems + Lemmas) overlapping with each other.
	 */
	@Test
	public void testSelectCoveredOverlapping() throws Exception {
		add(jCas, 3, 16);
		add(jCas, 37, 61);
		add(jCas, 49, 75);
		add(jCas, 54, 58);
		add(jCas, 66, 84);

		for (Token t : select(jCas, Token.class)) {
			// The naive approach is assumed to be correct
			List<Sentence> stem1 = selectCovered(jCas, Sentence.class, t.getBegin(), t.getEnd());
			List<Sentence> stem2 = selectCovered(jCas, Sentence.class, t);
			check(jCas, t, stem1, stem2);
		}
	}

	/**
	 * Test what happens if there is actually nothing overlapping with the Token.
	 */
	@Test
	public void testSelectCoveredNoOverlap() throws Exception {
		new Sentence(jCas, 3, 31).addToIndexes();
		new Sentence(jCas, 21, 21).addToIndexes();
		new Sentence(jCas, 24, 44).addToIndexes();
		new Sentence(jCas, 30, 45).addToIndexes();
		new Sentence(jCas, 32, 43).addToIndexes();
		new Sentence(jCas, 47, 61).addToIndexes();
		new Sentence(jCas, 48, 77).addToIndexes();
		new Sentence(jCas, 65, 82).addToIndexes();
		new Sentence(jCas, 68, 80).addToIndexes();
		new Sentence(jCas, 72, 65).addToIndexes();

		new Token(jCas, 73, 96).addToIndexes();

		for (Token t : select(jCas, Token.class)) {
			// The naive approach is assumed to be correct
			List<Sentence> stem1 = selectCovered(jCas, Sentence.class, t.getBegin(), t.getEnd());
			List<Sentence> stem2 = selectCovered(jCas, Sentence.class, t);
			check(jCas, t, stem1, stem2);
		}
	}

	@Test
	public void testSelectCoverRandom() throws Exception {
		final int ITERATIONS = 10;

		for (int i = 0; i < ITERATIONS; i++) {
			CAS cas = jCas.getCas();
			initRandomCas(cas, 10 * i);

			JCas jcas = cas.getJCas();
			long timeNaive = 0;
			long timeOptimized = 0;
			for (Token t : select(jcas, Token.class)) {
				long ti = System.currentTimeMillis();
				// The naive approach is assumed to be correct
				List<Sentence> stem1 = selectCovered(jcas, Sentence.class, t.getBegin(), t.getEnd());
				timeNaive += System.currentTimeMillis() - ti;

				ti = System.currentTimeMillis();
				List<Sentence> stem2 = selectCovered(jcas, Sentence.class, t);
				timeOptimized += System.currentTimeMillis() - ti;

				Collection<Sentence> stem3 = indexCovered(jcas, Token.class, Sentence.class).get(t);

				check(jcas, t, stem1, stem2);
				check(jcas, t, stem1, stem3);
			}
			System.out.format("Speed up factor %.2f [naive:%d optimized:%d diff:%d]\n", 
					 (double) timeNaive / (double) timeOptimized, timeNaive, timeOptimized, 
					timeNaive - timeOptimized);
		}
	}
	
	/**
	 * Test what happens if there is actually nothing overlapping with the Token.
	 */
	@Test
	public void testSelectBetweenInclusion() throws Exception {
		Token t1 = new Token(jCas, 45, 57);
		t1.addToIndexes();
		Token t2 = new Token(jCas, 52, 52);
		t2.addToIndexes();
		
		new Sentence(jCas, 52, 52).addToIndexes();

		List<Sentence> stem1 = selectBetween(jCas, Sentence.class, t1, t2);
		assertTrue(stem1.isEmpty());
	}

	@Test
	public void testSelectBetweenRandom() throws Exception {
		final int ITERATIONS = 10;

		Random rnd = new Random();

		for (int i = 1; i <= ITERATIONS; i++) {
			CAS cas = jCas.getCas();
			initRandomCas(cas, 10 * i);

			JCas jcas = cas.getJCas();
			List<Token> tokens = new ArrayList<Token>(select(jcas, Token.class));

			long timeNaive = 0;
			long timeOptimized = 0;
			for (int j = 0; j < ITERATIONS; j++) {
				Token t1 = tokens.get(rnd.nextInt(tokens.size()));
				Token t2 = tokens.get(rnd.nextInt(tokens.size()));
				
				int left = Math.min(t1.getEnd(), t2.getEnd());
				int right = Math.max(t1.getBegin(), t2.getBegin());

				long ti;
				List<Sentence> reference;
				if (
					(t1.getBegin() < t2.getBegin() && t2.getBegin() < t1.getEnd()) ||
					(t1.getBegin() < t2.getEnd() && t2.getEnd() < t1.getEnd()) ||
					(t2.getBegin() < t1.getBegin() && t1.getBegin() < t2.getEnd()) ||
					(t2.getBegin() < t1.getEnd() && t1.getEnd() < t2.getEnd())
				) {
					// If the boundary annotations overlap, the result must be empty
					ti = System.currentTimeMillis();
					reference = new ArrayList<Sentence>();
					timeNaive += System.currentTimeMillis() - ti;
				}
				else {
					ti = System.currentTimeMillis();
					reference = selectCovered(jcas, Sentence.class, left, right);
					timeNaive += System.currentTimeMillis() - ti;
				}
				
				ti = System.currentTimeMillis();
				List<Sentence> actual = selectBetween(Sentence.class, t1, t2);
				timeOptimized += System.currentTimeMillis() - ti;

				assertEquals("Naive: Searching between "+t1+" and "+t2, reference, actual);
			}
			
			System.out.format("Speed up factor %.2f [naive:%d optimized:%d diff:%d]\n", 
					 (double) timeNaive / (double) timeOptimized, timeNaive, timeOptimized, 
					timeNaive - timeOptimized);
		}
	}
	
	/**
	 * Test Tokens (Stems + Lemmas) overlapping with each other.
	 */
	@Test
	public void testSelectCoveringOverlapping() throws Exception {
		add(jCas, 3, 16);
		add(jCas, 37, 61);
		add(jCas, 49, 75);
		add(jCas, 54, 58);
		add(jCas, 66, 84);

		assertEquals(0, selectCovering(jCas, Token.class, 36, 52).size());
		assertEquals(1, selectCovering(jCas, Token.class, 37, 52).size());
		assertEquals(2, selectCovering(jCas, Token.class, 49, 52).size());
	}
	
	private void initRandomCas(CAS cas, int size)
	{
		Random rnd = new Random();
		List<Type> types = new ArrayList<Type>();
		types.add(cas.getTypeSystem().getType(Token.class.getName()));
		types.add(cas.getTypeSystem().getType(Sentence.class.getName()));

		// Shuffle the types
		for (int n = 0; n < 10; n++) {
			Type t = types.remove(rnd.nextInt(types.size()));
			types.add(t);
		}

		// Randomly generate annotations
		for (int n = 0; n < size; n++) {
			for (Type t : types) {
				int begin = rnd.nextInt(100);
				int end = begin + rnd.nextInt(30);
				cas.addFsToIndexes(cas.createAnnotation(t, begin, end));
			}
		}
	}

	@SuppressWarnings("unused")
	private void print(Collection<? extends Annotation> annos) {
		for (Annotation a : annos) {
			System.out
					.println(a.getClass().getSimpleName() + " " + a.getBegin() + " " + a.getEnd());
		}
	}

	private Token add(JCas jcas, int begin, int end) {
		Token t = new Token(jcas, begin, end);
		t.addToIndexes();
		new Sentence(jcas, begin, end).addToIndexes();
		return t;
	}

	private void check(JCas jcas, Token t, Collection<? extends Annotation> a1,
			Collection<? extends Annotation> a2) {
		// List<Annotation> annos = new ArrayList<Annotation>();
		// FSIterator fs = jcas.getAnnotationIndex().iterator();
		// while (fs.hasNext()) {
		// annos.add((Annotation) fs.next());
		// }
		//
		// System.out.println("--- Index");
		// print(annos);
		// System.out.println("--- Container");
		// print(Collections.singleton(t));
		// System.out.println("--- Naive");
		// print(a1);
		// System.out.println("--- Optimized");
		// print(a2);
		assertEquals("Container: [" + t.getBegin() + ".." + t.getEnd() + "]", a1, a2);
	}

	@Test
	public void testIterator() throws Exception {
		String text = "Rot wood cheeses dew?";
		tokenBuilder.buildTokens(jCas, text);

		assertEquals(asList("Rot", "wood", "cheeses", "dew?"),
				toText(JCasUtil.select(jCas, Token.class)));
	}

	@Test
	public void testSelectByIndex() throws UIMAException {
		String text = "Rot wood cheeses dew?";
		tokenBuilder.buildTokens(jCas, text);

		assertEquals("dew?", JCasUtil.selectByIndex(jCas, Token.class, -1).getCoveredText());
		assertEquals("dew?", JCasUtil.selectByIndex(jCas, Token.class, 3).getCoveredText());
		assertEquals("Rot", JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText());
		assertEquals("Rot", JCasUtil.selectByIndex(jCas, Token.class, -4).getCoveredText());
		assertNull(JCasUtil.selectByIndex(jCas, Token.class, -5));
		assertNull(JCasUtil.selectByIndex(jCas, Token.class, 4));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testSelectOnArrays() throws Exception {
		String text = "Rot wood cheeses dew?";
		tokenBuilder.buildTokens(jCas, text);

		Collection<TOP> allFS = select(jCas, TOP.class);
		FSArray allFSArray = new FSArray(jCas, allFS.size());
		int i = 0;
		for (FeatureStructure fs : allFS) {
			allFSArray.set(i, fs);
			i++;
		}

		// Print what is expected
		for (FeatureStructure fs : allFS) {
			System.out.println("Type: " + fs.getType().getName() + "]");
		}
		System.out.println("Tokens: [" + toText(select(jCas, Token.class)) + "]");

		// Document Annotation, one sentence and 4 tokens.
		assertEquals(6, allFS.size());

		assertEquals(toText(select(jCas, Token.class)), toText(select(allFSArray, Token.class)));

		assertEquals(toText((Iterable) select(jCas, Token.class)),
				toText((Iterable) select(allFSArray, Token.class)));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testSelectOnLists() throws Exception {
		String text = "Rot wood cheeses dew?";
		tokenBuilder.buildTokens(jCas, text);

		Collection<TOP> allFS = select(jCas, TOP.class);

		// Building a list... OMG!
		NonEmptyFSList allFSList = new NonEmptyFSList(jCas);
		NonEmptyFSList head = allFSList;
		Iterator<TOP> i = allFS.iterator();
		while (i.hasNext()) {
			head.setHead(i.next());
			if (i.hasNext()) {
				head.setTail(new NonEmptyFSList(jCas));
				head = (NonEmptyFSList) head.getTail();
			}
			else {
				head.setTail(new EmptyFSList(jCas));
			}
		}

		// Print what is expected
		for (FeatureStructure fs : allFS) {
			System.out.println("Type: " + fs.getType().getName() + "]");
		}
		System.out.println("Tokens: [" + toText(select(jCas, Token.class)) + "]");

		// Document Annotation, one sentence and 4 tokens.
		assertEquals(6, allFS.size());

		assertEquals(toText(select(jCas, Token.class)), toText(select(allFSList, Token.class)));

		assertEquals(toText((Iterable) select(jCas, Token.class)),
				toText((Iterable) select(allFSList, Token.class)));
	}

	@Test
	public void testToText() throws UIMAException {
		String text = "Rot wood cheeses dew?";
		tokenBuilder.buildTokens(jCas, text);
		assertEquals(asList(text.split(" ")), toText(select(jCas, Token.class)));
	}

	@Test
	public void testSelectSingleRelative() throws UIMAException {
		String text = "one two three";
		tokenBuilder.buildTokens(jCas, text);
		List<Token> token = new ArrayList<Token>(select(jCas, Token.class));

		Token preceding = selectSingleRelative(jCas, Token.class, token.get(1), -1);
		assertEquals(token.get(0).getCoveredText(), preceding.getCoveredText());
		
		Token following = selectSingleRelative(jCas, Token.class, token.get(1), 1);
		assertEquals(token.get(2).getCoveredText(), following.getCoveredText());
	}

	@Test
	public void testSelectFollowingPreceding() throws UIMAException {
		String text = "one two three";
		tokenBuilder.buildTokens(jCas, text);
		List<Token> token = new ArrayList<Token>(select(jCas, Token.class));

		assertEquals(token.get(0).getCoveredText(),
				selectPreceding(jCas, Token.class, token.get(1), 1).get(0).getCoveredText());
		assertEquals(token.get(2).getCoveredText(),
				selectFollowing(jCas, Token.class, token.get(1), 1).get(0).getCoveredText());
	}

	@Test
	public void testSelectFollowingPrecedingBuiltinTypes() {
		this.jCas.setDocumentText("A B C");
		// remove the DocumentAnnotation
		for (Annotation ann : JCasUtil.select(jCas, Annotation.class)) {
			ann.removeFromIndexes();
		}
		Annotation a = new Annotation(this.jCas, 0, 1);
		Annotation b = new Annotation(this.jCas, 2, 3);
		Annotation c = new Annotation(this.jCas, 4, 5);
		for (Annotation ann : Arrays.asList(a, b, c)) {
			ann.addToIndexes();
		}

		assertEquals(Arrays.asList(a), selectPreceding(this.jCas, Annotation.class, b, 2));
		assertEquals(Arrays.asList(a, b), selectPreceding(this.jCas, Annotation.class, c, 2));
		assertEquals(Arrays.asList(b, c), selectFollowing(this.jCas, Annotation.class, a, 2));
		assertEquals(Arrays.asList(c), selectFollowing(this.jCas, Annotation.class, b, 2));
	}

	@Test
	public void testSelectFollowingPrecedingDifferentTypes() {
		this.jCas.setDocumentText("A B C D E");
		Token a = new Token(this.jCas, 0, 1);
		Token b = new Token(this.jCas, 2, 3);
		Token c = new Token(this.jCas, 4, 5);
		Token d = new Token(this.jCas, 6, 7);
		Token e = new Token(this.jCas, 8, 9);
		for (Token token : Arrays.asList(a, b, c, d, e)) {
			token.addToIndexes();
		}
		Sentence sentence = new Sentence(this.jCas, 2, 5);
		sentence.addToIndexes();

		List<Token> preceding = selectPreceding(this.jCas, Token.class, sentence, 1);
		assertEquals(Arrays.asList("A"), JCasUtil.toText(preceding));
		assertEquals(Arrays.asList(a), preceding);
		preceding = selectPreceding(this.jCas, Token.class, sentence, 2);
		assertEquals(Arrays.asList("A"), JCasUtil.toText(preceding));
		assertEquals(Arrays.asList(a), preceding);

		List<Token> following = selectFollowing(this.jCas, Token.class, sentence, 1);
		assertEquals(Arrays.asList("D"), JCasUtil.toText(following));
		assertEquals(Arrays.asList(d), following);
		following = selectFollowing(this.jCas, Token.class, sentence, 2);
		assertEquals(Arrays.asList("D", "E"), JCasUtil.toText(following));
		assertEquals(Arrays.asList(d, e), following);
		following = selectFollowing(this.jCas, Token.class, sentence, 3);
		assertEquals(Arrays.asList("D", "E"), JCasUtil.toText(following));
		assertEquals(Arrays.asList(d, e), following);
	}

	@Test
	public void testSelectFollowingPrecedingDifferentTypesMatchingSpansReversePriorities() {
		this.jCas.setDocumentText("A B C D E");
		Sentence a = new Sentence(this.jCas, 0, 1);
		Sentence b = new Sentence(this.jCas, 2, 3);
		Sentence c = new Sentence(this.jCas, 4, 5);
		Sentence d = new Sentence(this.jCas, 6, 7);
		Sentence e = new Sentence(this.jCas, 8, 9);
		for (Sentence sentence : Arrays.asList(a, b, c, d, e)) {
			sentence.addToIndexes();
		}
		AnalyzedText text = new AnalyzedText(this.jCas, 2, 3);
		text.addToIndexes();

		List<Sentence> preceding = selectPreceding(this.jCas, Sentence.class, text, 1);
		assertEquals(Arrays.asList("A"), JCasUtil.toText(preceding));
		assertEquals(Arrays.asList(a), preceding);
		preceding = selectPreceding(this.jCas, Sentence.class, text, 2);
		assertEquals(Arrays.asList("A"), JCasUtil.toText(preceding));
		assertEquals(Arrays.asList(a), preceding);

		List<Sentence> following = selectFollowing(this.jCas, Sentence.class, text, 1);
		assertEquals(Arrays.asList("C"), JCasUtil.toText(following));
		assertEquals(Arrays.asList(c), following);
		following = selectFollowing(this.jCas, Sentence.class, text, 2);
		assertEquals(Arrays.asList("C", "D"), JCasUtil.toText(following));
		assertEquals(Arrays.asList(c, d), following);
	}

	@Test
	public void testExists() throws UIMAException {
		JCas jcas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();

		assertFalse(exists(jcas, Token.class));

		new Token(jcas, 0, 1).addToIndexes();

		assertTrue(exists(jcas, Token.class));
	}

	@Test
	public void testSelectSingle() throws UIMAException {
		JCas jcas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();

		try {
			selectSingle(jcas, Token.class);
			fail("Found annotation that has not yet been created");
		}
		catch (IllegalArgumentException e) {
			// OK
		}

		new Token(jcas, 0, 1).addToIndexes();

		selectSingle(jcas, Token.class);

		new Token(jcas, 1, 2).addToIndexes();

		try {
			selectSingle(jcas, Token.class);
			fail("selectSingle must fail if there is more than one annotation of the type");
		}
		catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void testSelectIsCovered() throws UIMAException {
		String text = "Will you come home today ? \n No , tomorrow !";
		tokenBuilder.buildTokens(jCas, text);

		List<Sentence> sentences = new ArrayList<Sentence>(select(jCas, Sentence.class));
		List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));

		assertEquals(6, selectCovered(Token.class, sentences.get(0)).size());
		assertEquals(4, selectCovered(Token.class, sentences.get(1)).size());

		assertTrue(isCovered(jCas, sentences.get(0), Token.class));
		tokens.get(0).removeFromIndexes();
		tokens.get(1).removeFromIndexes();
		tokens.get(2).removeFromIndexes();
		tokens.get(3).removeFromIndexes();
		tokens.get(4).removeFromIndexes();
		tokens.get(5).removeFromIndexes();
		assertFalse(isCovered(jCas, sentences.get(0), Token.class));
	}

	@Test
	public void testGetInternalUimaType() {
		Type t = getType(jCas, Annotation.class);
		assertNotNull(t);
	}

	@Test
	public void testGetView() throws Exception {
		JCas jcas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();

		assertNull(getView(jcas, "view1", null));
		assertNotNull(getView(jcas, "view1", true));
		assertNotNull(getView(jcas, "view1", null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNonExistingView() throws Exception {
		JCas jcas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();
		assertNull(getView(jcas, "view1", false));
	}

	@Test
	public void testGetType() throws UIMAException {
		String text = "Rot wood cheeses dew?";
		tokenBuilder.buildTokens(jCas, text);

		assertEquals(Token.class.getName(), getType(jCas, Token.class).getName());
		assertEquals(Token.class.getName(), getAnnotationType(jCas, Token.class).getName());
		assertEquals("uima.cas.TOP", getType(jCas, TOP.class).getName());
		assertEquals("uima.tcas.Annotation", getType(jCas, Annotation.class).getName());
		assertEquals("uima.tcas.Annotation", getAnnotationType(jCas, Annotation.class).getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNonAnnotationType() throws UIMAException {
		String text = "Rot wood cheeses dew?";
		tokenBuilder.buildTokens(jCas, text);

		getAnnotationType(jCas, TOP.class);
	}

	@Test
	public void testIndexCovering() throws Exception {
		String text = "Will you come home today ? \n No , tomorrow !";
		tokenBuilder.buildTokens(jCas, text);

		List<Sentence> sentences = new ArrayList<Sentence>(select(jCas, Sentence.class));
		List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));

		Map<Token, Collection<Sentence>> index = indexCovering(jCas, Token.class, Sentence.class);

		// Check covering annotations are found
		assertEquals(asList(sentences.get(0)), index.get(tokens.get(0)));
		assertEquals(asList(sentences.get(1)), index.get(tokens.get(tokens.size() - 1)));

		// Check sentence 0 contains first token
		assertTrue(index.get(tokens.get(0)).contains(sentences.get(0)));

		// Check sentence 0 does not contain last token.
		assertFalse(index.get(tokens.get(tokens.size() - 1)).contains(sentences.get(0)));

		// Check the first token is contained in any sentence
		assertTrue(!index.get(tokens.get(0)).isEmpty());
		// After removing the annotation the index has to be rebuilt.
		sentences.get(0).removeFromIndexes();
		index = indexCovering(jCas, Token.class, Sentence.class);
		// Check the first token is not contained in any sentence
		assertFalse(!index.get(tokens.get(0)).isEmpty());
	}
}
