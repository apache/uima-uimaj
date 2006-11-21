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

package org.apache.uima.tttypesystem;

import org.apache.uima.cas.text.TCAS;
import org.apache.uima.cas.TypeSystem;

/**
 * Symbolic TT type system constants used by the tokenizer.
 * 
 */
public final class TTTypeSystemConsts {

  private TTTypeSystemConsts() {
    super();
  }

  public static final String TYPE_NAME_LEXICAL_ANNOTATION = "uima.tt.LexicalAnnotation";

  public static final String TYPE_NAME_TOKEN_ANNOTATION = "uima.tt.TokenAnnotation";

  public static final String TYPE_NAME_TERM_ANNOTATION = "uima.tt.TermAnnotation";

  public static final String FEATURE_FULL_NAME_TOKEN_PROPERTIES = TYPE_NAME_TOKEN_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "tokenProperties";

  public static final String FEATURE_FULL_NAME_TOKEN_NUMBER = TYPE_NAME_TOKEN_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "tokenNumber";

  public static final String FEATURE_FULL_NAME_TOKEN_ISSTOPWORD = TYPE_NAME_TOKEN_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "stopwordToken";

  public static final String FEATURE_FULL_NAME_TOKEN_NORMALIZEDCOVEREDTEXT = TYPE_NAME_TOKEN_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "normalizedCoveredText";

  public static final String TYPE_NAME_SENTENCE_ANNOTATION = "uima.tt.SentenceAnnotation";

  public static final String FEATURE_FULL_NAME_SENTENCE_NUMBER = TYPE_NAME_SENTENCE_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "sentenceNumber";

  public static final String TYPE_NAME_PARAGRAPH_ANNOTATION = "uima.tt.ParagraphAnnotation";

  public static final String FEATURE_FULL_NAME_PARAGRAPH_NUMBER = TYPE_NAME_PARAGRAPH_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "paragraphNumber";

  public static final String FEATURE_FULL_NAME_TOKEN_LEMMA = TYPE_NAME_TOKEN_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "lemma";

  public static final String FEATURE_FULL_NAME_TOKEN_LEMMA_LIST = TYPE_NAME_TOKEN_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "lemmaEntries";

  public static final String TYPE_NAME_COMPPART_ANNOTATION = "uima.tt.CompPartAnnotation";

  public static final String FEATURE_FULL_NAME_LANGUAGE_CANDIDATES = TCAS.TYPE_NAME_DOCUMENT_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "languageCandidates";

  public static final String TYPE_NAME_LANGUAGE_CONFIDENCE_PAIR = "uima.tt.LanguageConfidencePair";

  public static final String FEATURE_FULL_NAME_LANGUAGE_CONFIDENCE_PAIR_CONFIDENCE = TYPE_NAME_LANGUAGE_CONFIDENCE_PAIR
                  + TypeSystem.FEATURE_SEPARATOR + "languageConfidence";

  public static final String FEATURE_FULL_NAME_LANGUAGE_CONFIDENCE_PAIR_LANGUAGEID = TYPE_NAME_LANGUAGE_CONFIDENCE_PAIR
                  + TypeSystem.FEATURE_SEPARATOR + "languageID";

  public static final String FEATURE_FULL_NAME_LANGUAGE_CONFIDENCE_PAIR_LANGUAGE = TYPE_NAME_LANGUAGE_CONFIDENCE_PAIR
                  + TypeSystem.FEATURE_SEPARATOR + "language";

  public static final String TYPE_NAME_CATEGORY_CONFIDENCE_PAIR = "uima.tt.CategoryConfidencePair";

  public static final String FEATURE_FULL_NAME_CATEGORY_STRING = TYPE_NAME_CATEGORY_CONFIDENCE_PAIR
                  + TypeSystem.FEATURE_SEPARATOR + "categoryString";

  public static final String FEATURE_FULL_NAME_CATEGORY_CONFIDENCE = TYPE_NAME_CATEGORY_CONFIDENCE_PAIR
                  + TypeSystem.FEATURE_SEPARATOR + "categoryConfidence";

  public static final String FEATURE_FULL_NAME_CATEGORIES = TCAS.TYPE_NAME_DOCUMENT_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "categories";

  public static final String TYPE_NAME_KEY_STRING_ENTRY = "uima.tt.KeyStringEntry";

  public static final String FEATURE_FULL_NAME_KEYSTRINGENTRY_KEY = TYPE_NAME_KEY_STRING_ENTRY
                  + TypeSystem.FEATURE_SEPARATOR + "key";

  public static final String TYPE_NAME_SYNONYM = "uima.tt.Synonym";

  public static final String FEATURE_FULL_NAME_SYNONYM_ENTRIES = TYPE_NAME_TOKEN_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "synonymEntries";

  public static final String TYPE_NAME_LEMMA = "uima.tt.Lemma";

  public static final String FEATURE_FULL_NAME_LEMMA_POS = TYPE_NAME_LEMMA
                  + TypeSystem.FEATURE_SEPARATOR + "partOfSpeech";

  public static final String FEATURE_FULL_NAME_LEMMA_MORPH_ID = TYPE_NAME_LEMMA
                  + TypeSystem.FEATURE_SEPARATOR + "morphID";

  public static final String INDEX_NAME_LEMMA = "Lemma Index";

  private static final int UNKNOWN_POS = 0;

  private static final String[] POS_CONSTS = { "UnspecifiedPOS", "Pronoun", "Verb", "Noun",
      "Adjective", "Adverb", "Preposition", "Interjection", "Conjunction", "MasculinNoun",
      "FemininNoun", "VerbPronominal", "TransitiveVerb", "IntransitiveVerb", "PluralNoun",
      "NeuterNoun" };

  public static String partOfSpeechToString(int pos) {
    if (pos < 0 || pos >= POS_CONSTS.length) {
      return POS_CONSTS[UNKNOWN_POS];
    }
    return POS_CONSTS[pos];
  }

  public static final String TYPE_NAME_STOPWORD_ANNOTATION = "uima.tt.StopwordAnnotation";

  public static final String TYPE_NAME_SYNONYM_ANNOTATION = "uima.tt.SynonymAnnotation";

  public static final String FEATURE_FULL_NAME_SYNONYMS = TYPE_NAME_SYNONYM_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "synonyms";

  public static final String TYPE_NAME_SPELL_CORRECTION_ANNOTATION = "uima.tt.SpellCorrectionAnnotation";

  public static final String FEATURE_FULL_NAME_CORRECTION_TERMS = TYPE_NAME_SPELL_CORRECTION_ANNOTATION
                  + TypeSystem.FEATURE_SEPARATOR + "correctionTerms";

  public static final String TYPE_NAME_MULTI_WORD_ANNOTATION = "uima.tt.MultiWordAnnotation";

  public static final String TYPE_NAME_MULTI_TOKEN_ANNOTATION = "uima.tt.MultiTokenAnnotation";

  public static final String TYPE_NAME_COMPOUND_ANNOTATION = "uima.tt.CompoundAnnotation";

}
