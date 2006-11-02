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

package org.apache.uima.examples.opennlp.annotator;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.lang.english.PosTagger;
import opennlp.tools.ngram.Dictionary;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.JTextAnnotator_ImplBase;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.examples.opennlp.Sentence;
import org.apache.uima.examples.opennlp.Token;
import org.apache.uima.jcas.impl.JCas;

/**
 * UIMA Analysis Engine that invokes the OpenNLP POS tagger. The OpenNLP POS
 * tagger generates a PennTreeBank style POS tags. This annotator assumes that
 * sentences and tokens have already been annotated in the CAS with Sentence and
 * Token annotations, respectively. We iterate over sentences, then iterate over
 * tokens in the current sentece to accumlate a list of words, then invoke the
 * OpenNLP POS tagger on the list of words. For each Token we then update the
 * posTag field with the POS tag. The model file for the OpenNLP POS tagger is
 * specified as a parameter (MODEL_FILE_PARAM).
 * 
 */
public class POSTagger extends JTextAnnotator_ImplBase {

    public static final String MODEL_FILE_PARAM = "ModelFile";

    private PosTagger tagger;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(org.apache.uima.analysis_engine.annotator.AnnotatorContext)
     */
    public void initialize(AnnotatorContext aContext)
            throws AnnotatorInitializationException,
            AnnotatorConfigurationException {
        super.initialize(aContext);

        try {
            // Get configuration parameter values
            String modelFile = (String) aContext
                    .getConfigParameterValue(MODEL_FILE_PARAM);
            tagger = new PosTagger(modelFile, (Dictionary)null);
        } catch (Exception e) {
            throw new AnnotatorInitializationException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.analysis_engine.annotator.JTextAnnotator#process(org.apache.uima.jcas.impl.JCas,
     *      org.apache.uima.analysis_engine.ResultSpecification)
     */
    public void process(JCas aJCas, ResultSpecification aResultSpec)
            throws AnnotatorProcessException {

        ArrayList tokenList = new ArrayList();
        ArrayList wordList = new ArrayList();

        AnnotationIndex sentenceIndex = (AnnotationIndex) aJCas
                .getJFSIndexRepository().getAnnotationIndex(Sentence.type);
        AnnotationIndex tokenIndex = (AnnotationIndex) aJCas
                .getJFSIndexRepository().getAnnotationIndex(Token.type);

        // iterate over Sentences
        FSIterator sentenceIterator = sentenceIndex.iterator();
        while (sentenceIterator.hasNext()) {
            Sentence sentence = (Sentence) sentenceIterator.next();

            tokenList.clear();
            wordList.clear();

            // iterate over Tokens
            FSIterator tokenIterator = tokenIndex.subiterator(sentence);
            while (tokenIterator.hasNext()) {
                Token token = (Token) tokenIterator.next();

                tokenList.add(token);
                wordList.add(token.getCoveredText());
            }

            List wordTagList = tagger.tag(wordList);

            try {
                for (int i = 0; i < tokenList.size(); i++) {
                    Token token = (Token) tokenList.get(i);
                    String posTag = (String) wordTagList.get(i);
                    token.setPosTag(posTag);
                }
            } catch (IndexOutOfBoundsException e) {
                System.err
                        .println("POS tagger error - list of tags shorter than list of words");
            }
        }
    }

}
