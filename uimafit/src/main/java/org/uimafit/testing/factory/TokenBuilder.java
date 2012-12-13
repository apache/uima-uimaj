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

package org.uimafit.testing.factory;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.uimafit.factory.AnnotationFactory;

/**
 * 
 * This class provides convenience methods for creating tokens and sentences and add them to a
 * {@link JCas}.
 * 
 * @author Steven Bethard, Philip Ogren
 * @author Richard Eckart de Castilho
 * 
 * @param <TOKEN_TYPE>
 *            the type system token type (e.g. org.uimafit.examples.type.Token)
 * @param <SENTENCE_TYPE>
 *            the type system sentence type (e.g. org.uimafit.examples.type.Sentence)
 */

public class TokenBuilder<TOKEN_TYPE extends Annotation, SENTENCE_TYPE extends Annotation> {
	private Class<TOKEN_TYPE> tokenClass;
	private Class<SENTENCE_TYPE> sentenceClass;
	private String posFeatureName;
	private String stemFeatureName;

	/**
	 * Calls {@link TokenBuilder#TokenBuilder(Class, Class, String, String)} with the last two
	 * arguments as null.
	 */
	public TokenBuilder(Class<TOKEN_TYPE> tokenClass, Class<SENTENCE_TYPE> sentenceClass) {
		this(tokenClass, sentenceClass, null, null);
	}

	/**
	 * Instantiates a TokenBuilder with the type system information that the builder needs to build
	 * tokens.
	 * 
	 * @param tokenClass
	 *            the class of your token type from your type system (e.g.
	 *            org.uimafit.type.Token.class)
	 * @param sentenceClass
	 *            the class of your sentence type from your type system (e.g.
	 *            org.uimafit.type.Sentence.class)
	 * @param posFeatureName
	 *            the feature name for the part-of-speech tag for your token type. This assumes that
	 *            there is a single string feature for which to put your pos tag. null is an ok
	 *            value.
	 * @param stemFeatureName
	 *            the feature name for the stem for your token type. This assumes that there is a
	 *            single string feature for which to put your stem. null is an ok value.
	 */
	public TokenBuilder(final Class<TOKEN_TYPE> tokenClass, final Class<SENTENCE_TYPE> sentenceClass,
			String posFeatureName, String stemFeatureName) {
		this.tokenClass = tokenClass;
		this.sentenceClass = sentenceClass;
		setPosFeatureName(posFeatureName);
		setStemFeatureName(stemFeatureName);
	}

	/**
	 * Instantiates a TokenBuilder with the type system information that the builder needs to build
	 * tokens.
	 * 
	 * @param <T>
	 *            the type system token type (e.g. org.uimafit.examples.type.Token)
	 * @param <S>
	 *            the type system sentence type (e.g. org.uimafit.examples.type.Sentence)
	 * @param tokenClass
	 *            the class of your token type from your type system (e.g.
	 *            org.uimafit.type.Token.class)
	 * @param sentenceClass
	 *            the class of your sentence type from your type system (e.g.
	 *            org.uimafit.type.Sentence.class)
	 * @return the builder.
	 */
	public static <T extends Annotation, S extends Annotation> TokenBuilder<T, S> create(
			Class<T> tokenClass, Class<S> sentenceClass) {
		return new TokenBuilder<T, S>(tokenClass, sentenceClass);
	}

	/**
	 * Set the feature name for the part-of-speech tag for your token type. This assumes that there
	 * is a single string feature for which to put your pos tag. null is an ok value.
	 * 
	 * @param posFeatureName
	 *            the part-of-speech feature name.
	 */
	public void setPosFeatureName(String posFeatureName) {
		this.posFeatureName = posFeatureName;
	}

	/**
	 * Set the feature name for the stem for your token type. This assumes that there is a single
	 * string feature for which to put your stem. null is an ok value.
	 * 
	 * @param stemFeatureName
	 *            the stem feature name.
	 */
	public void setStemFeatureName(String stemFeatureName) {
		this.stemFeatureName = stemFeatureName;
	}

	/**
	 * Builds white-space delimited tokens from the input text.
	 * 
	 * @param jCas
	 *            the JCas to add the tokens to
	 * @param text
	 *            the JCas will have its document text set to this.
	 */
	public void buildTokens(JCas jCas, String text) throws UIMAException {
		if (text == null) {
			throw new IllegalArgumentException("text may not be null.");
		}
		buildTokens(jCas, text, text, null, null);
	}

	/**
	 * see {@link #buildTokens(JCas, String, String, String, String)}
	 */
	public void buildTokens(JCas jCas, String text, String tokensString) throws UIMAException {
		if (tokensString == null) {
			throw new IllegalArgumentException("tokensText may not be null.");
		}
		buildTokens(jCas, text, tokensString, null, null);
	}

	/**
	 * see {@link #buildTokens(JCas, String, String, String, String)}
	 */
	public void buildTokens(JCas jCas, String text, String tokensString, String posTagsString)
			throws UIMAException {
		buildTokens(jCas, text, tokensString, posTagsString, null);
	}

	/**
	 * Build tokens for the given text, tokens, part-of-speech tags, and word stems.
	 * 
	 * @param aJCas
	 *            the JCas to add the Token annotations to
	 * @param aText
	 *            this method sets the text of the JCas to this method. Therefore, it is generally a
	 *            good idea to call JCas.reset() before calling this method when passing in the
	 *            default view.
	 * @param aTokensString
	 *            the tokensString must have the same non-white space characters as the text. The
	 *            tokensString is used to identify token boundaries using white space - i.e. the
	 *            only difference between the 'text' parameter and the 'tokensString' parameter is
	 *            that the latter may have more whitespace characters. For example, if the text is
	 *            "She ran." then the tokensString might be "She ran ."
	 * @param aPosTagsString
	 *            the posTagsString should be a space delimited string of part-of-speech tags - one
	 *            for each token
	 * @param aStemsString
	 *            the stemsString should be a space delimitied string of stems - one for each token
	 */
	public void buildTokens(JCas aJCas, String aText, String aTokensString, String aPosTagsString,
			String aStemsString) throws UIMAException {
		aJCas.setDocumentText(aText);

		if (aPosTagsString != null && posFeatureName == null) {
			throw new IllegalArgumentException("posTagsString must be null if TokenBuilder is "
					+ "not initialized with a feature name corresponding to the part-of-speech "
					+ "feature of the token type (assuming your token type has such a feature).");
		}

		if (aStemsString != null && stemFeatureName == null) {
			throw new IllegalArgumentException(
					"stemsString must be null if TokenBuilder is not "
							+ "initialized with a feature name corresponding to the part-of-speech feature "
							+ "of the token type (assuming your token type has such a feature).");
		}

		Feature posFeature = null;
		if (posFeatureName != null) {
			// String fullPosFeatureName = tokenClass.getClass().getName()+":"+posFeatureName;
			// posFeature = jCas.getTypeSystem().getFeatureByFullName(fullPosFeatureName);
			posFeature = aJCas.getTypeSystem().getType(tokenClass.getName())
					.getFeatureByBaseName(posFeatureName);
		}
		Feature stemFeature = null;
		if (stemFeatureName != null) {
			stemFeature = aJCas.getTypeSystem().getType(tokenClass.getName())
					.getFeatureByBaseName(stemFeatureName);
		}

		String tokensString = aTokensString.replaceAll("\\s*\n\\s*", "\n");
		String[] sentenceStrings = tokensString.split("\n");
		String[] posTags = aPosTagsString != null ? aPosTagsString.split("\\s+") : null;
		String[] stems = aStemsString != null ? aStemsString.split("\\s+") : null;

		int offset = 0;
		int tokenIndex = 0;

		for (String sentenceString : sentenceStrings) {
			String[] tokenStrings = sentenceString.trim().split("\\s+");
			List<Annotation> tokenAnnotations = new ArrayList<Annotation>();
			for (String tokenString : tokenStrings) {
				// move the offset up to the beginning of the token
				while (!aText.startsWith(tokenString, offset)) {
					offset++;
					if (offset > aText.length()) {
						throw new IllegalArgumentException(String.format(
								"unable to find string %s", tokenString));
					}
				}

				// add the Token
				int start = offset;
				offset = offset + tokenString.length();
				Annotation token = AnnotationFactory.createAnnotation(aJCas, start, offset,
						tokenClass);
				tokenAnnotations.add(token);

				// set the stem and part of speech if present
				if (posTags != null) {
					token.setStringValue(posFeature, posTags[tokenIndex]);
				}
				if (stems != null) {
					token.setStringValue(stemFeature, stems[tokenIndex]);
				}
				tokenIndex++;
			}
			if (tokenAnnotations.size() > 0) {
				int begin = tokenAnnotations.get(0).getBegin();
				int end = tokenAnnotations.get(tokenAnnotations.size() - 1).getEnd();
				AnnotationFactory.createAnnotation(aJCas, begin, end, sentenceClass);
			}
		}

	}

}
