/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.fit.testing.factory;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * This class provides convenience methods for creating tokens and sentences and add them to a
 * {@link JCas}.
 * 
 * @param <TOKEN_TYPE>
 *          the type system token type (e.g. {@code org.apache.uima.fit.examples.type.Token})
 * @param <SENTENCE_TYPE>
 *          the type system sentence type (e.g. {@code org.apache.uima.fit.examples.type.Sentence})
 */

public class TokenBuilder<TOKEN_TYPE extends Annotation, SENTENCE_TYPE extends Annotation> {
  private Class<TOKEN_TYPE> tokenClass;

  private Class<SENTENCE_TYPE> sentenceClass;

  private String posFeatureName;

  private String stemFeatureName;

  /**
   * Calls {@link TokenBuilder#TokenBuilder(Class, Class, String, String)} with the last two
   * arguments as null.
   * 
   * @param aTokenClass
   *          the class of your token type from your type system (e.g.
   *          org.apache.uima.fit.type.Token.class)
   * @param aSentenceClass
   *          the class of your sentence type from your type system (e.g.
   *          org.apache.uima.fit.type.Sentence.class)
   */
  public TokenBuilder(Class<TOKEN_TYPE> aTokenClass, Class<SENTENCE_TYPE> aSentenceClass) {
    this(aTokenClass, aSentenceClass, null, null);
  }

  /**
   * Instantiates a TokenBuilder with the type system information that the builder needs to build
   * tokens.
   * 
   * @param aTokenClass
   *          the class of your token type from your type system (e.g.
   *          org.apache.uima.fit.type.Token.class)
   * @param aSentenceClass
   *          the class of your sentence type from your type system (e.g.
   *          org.apache.uima.fit.type.Sentence.class)
   * @param aPosFeatureName
   *          the feature name for the part-of-speech tag for your token type. This assumes that
   *          there is a single string feature for which to put your pos tag. null is an ok value.
   * @param aStemFeatureName
   *          the feature name for the stem for your token type. This assumes that there is a single
   *          string feature for which to put your stem. null is an ok value.
   */
  public TokenBuilder(final Class<TOKEN_TYPE> aTokenClass,
          final Class<SENTENCE_TYPE> aSentenceClass, String aPosFeatureName,
          String aStemFeatureName) {
    tokenClass = aTokenClass;
    sentenceClass = aSentenceClass;
    setPosFeatureName(aPosFeatureName);
    setStemFeatureName(aStemFeatureName);
  }

  /**
   * Instantiates a TokenBuilder with the type system information that the builder needs to build
   * tokens.
   * 
   * @param <T>
   *          the type system token type (e.g. org.apache.uima.fit.examples.type.Token)
   * @param <S>
   *          the type system sentence type (e.g.
   *          {@code org.apache.uima.fit.examples.type.Sentence})
   * @param aTokenClass
   *          the class of your token type from your type system (e.g.
   *          {@code org.apache.uima.fit.type.Token})
   * @param aSentenceClass
   *          the class of your sentence type from your type system (e.g.
   *          {@code org.apache.uima.fit.type.Sentence})
   * @return the builder.
   */
  public static <T extends Annotation, S extends Annotation> TokenBuilder<T, S> create(
          Class<T> aTokenClass, Class<S> aSentenceClass) {
    return new TokenBuilder<T, S>(aTokenClass, aSentenceClass);
  }

  /**
   * Set the feature name for the part-of-speech tag for your token type. This assumes that there is
   * a single string feature for which to put your pos tag. null is an ok value.
   * 
   * @param aPosFeatureName
   *          the part-of-speech feature name.
   */
  public void setPosFeatureName(String aPosFeatureName) {
    posFeatureName = aPosFeatureName;
  }

  /**
   * Set the feature name for the stem for your token type. This assumes that there is a single
   * string feature for which to put your stem. null is an ok value.
   * 
   * @param aStemFeatureName
   *          the stem feature name.
   */
  public void setStemFeatureName(String aStemFeatureName) {
    stemFeatureName = aStemFeatureName;
  }

  /**
   * Builds white-space delimited tokens from the input text.
   * 
   * @param aJCas
   *          the JCas to add the Token annotations to
   * @param aText
   *          the text to initialize the {@link JCas} with
   */
  public void buildTokens(JCas aJCas, String aText) {
    if (aText == null) {
      throw new IllegalArgumentException("text may not be null.");
    }
    buildTokens(aJCas, aText, aText, null, null);
  }

  /**
   * @param aJCas
   *          the JCas to add the Token annotations to
   * @param aText
   *          the text to initialize the {@link JCas} with
   * @param aTokensString
   *          the tokensString must have the same non-white space characters as the text. The
   *          tokensString is used to identify token boundaries using white space - i.e. the only
   *          difference between the 'text' parameter and the 'tokensString' parameter is that the
   *          latter may have more whitespace characters. For example, if the text is "She ran."
   *          then the tokensString might be "She ran ."
   * @see #buildTokens(JCas, String, String, String, String)
   */
  public void buildTokens(JCas aJCas, String aText, String aTokensString) {
    if (aTokensString == null) {
      throw new IllegalArgumentException("tokensString may not be null.");
    }
    buildTokens(aJCas, aText, aTokensString, null, null);
  }

  /**
   * @param aJCas
   *          the JCas to add the Token annotations to
   * @param aText
   *          the text to initialize the {@link JCas} with
   * @param aTokensString
   *          the tokensString must have the same non-white space characters as the text. The
   *          tokensString is used to identify token boundaries using white space - i.e. the only
   *          difference between the 'text' parameter and the 'tokensString' parameter is that the
   *          latter may have more whitespace characters. For example, if the text is "She ran."
   *          then the tokensString might be "She ran ."
   * @param aPosTagsString
   *          the posTagsString should be a space delimited string of part-of-speech tags - one for
   *          each token
   * @see #buildTokens(JCas, String, String, String, String)
   */
  public void buildTokens(JCas aJCas, String aText, String aTokensString, String aPosTagsString) {
    buildTokens(aJCas, aText, aTokensString, aPosTagsString, null);
  }

  /**
   * Build tokens for the given text, tokens, part-of-speech tags, and word stems.
   * 
   * @param aJCas
   *          the JCas to add the Token annotations to
   * @param aText
   *          the text to initialize the {@link JCas} with
   * @param aTokensString
   *          the tokensString must have the same non-white space characters as the text. The
   *          tokensString is used to identify token boundaries using white space - i.e. the only
   *          difference between the 'text' parameter and the 'tokensString' parameter is that the
   *          latter may have more whitespace characters. For example, if the text is "She ran."
   *          then the tokensString might be "She ran ."
   * @param aPosTagsString
   *          the posTagsString should be a space delimited string of part-of-speech tags - one for
   *          each token
   * @param aStemsString
   *          the stemsString should be a space delimited string of stems - one for each token
   */
  public void buildTokens(JCas aJCas, String aText, String aTokensString, String aPosTagsString,
          String aStemsString) {
    aJCas.setDocumentText(aText);

    if (aPosTagsString != null && posFeatureName == null) {
      throw new IllegalArgumentException("posTagsString must be null if TokenBuilder is "
              + "not initialized with a feature name corresponding to the part-of-speech "
              + "feature of the token type (assuming your token type has such a feature).");
    }

    if (aStemsString != null && stemFeatureName == null) {
      throw new IllegalArgumentException("stemsString must be null if TokenBuilder is not "
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
            throw new IllegalArgumentException(
                    String.format("unable to find string %s", tokenString));
          }
        }

        // add the Token
        int start = offset;
        offset = offset + tokenString.length();
        Annotation token = AnnotationFactory.createAnnotation(aJCas, start, offset, tokenClass);
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
      if (!tokenAnnotations.isEmpty()) {
        int begin = tokenAnnotations.get(0).getBegin();
        int end = tokenAnnotations.get(tokenAnnotations.size() - 1).getEnd();
        AnnotationFactory.createAnnotation(aJCas, begin, end, sentenceClass);
      }
    }
  }
}
