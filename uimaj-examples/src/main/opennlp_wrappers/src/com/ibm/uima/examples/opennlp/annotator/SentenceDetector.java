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

import java.io.IOException;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.JTextAnnotator_ImplBase;
import org.apache.uima.examples.opennlp.Sentence;
import org.apache.uima.jcas.impl.JCas;

/**
 * Simple Annotator to detect sentences and create Sentence annotations in the
 * CAS. Uses the OpenNLP MaxEnt Sentence Detector.
 * 
 */
public class SentenceDetector extends JTextAnnotator_ImplBase {

    public static final String MODEL_FILE_PARAM = "ModelFile";
    
    public static final String COMPONENT_ID = "OpenNLP Sentence Detector";

    private opennlp.tools.lang.english.SentenceDetector sentenceDetector;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(org.apache.uima.analysis_engine.annotator.AnnotatorContext)
     */
    public void initialize(AnnotatorContext aContext)
            throws AnnotatorInitializationException,
            AnnotatorConfigurationException {
        super.initialize(aContext);
        String modelFile;
        try {
            modelFile = (String) aContext
                    .getConfigParameterValue(MODEL_FILE_PARAM);
        } catch (AnnotatorContextException e) {
            throw new AnnotatorConfigurationException(e);
        }
        try {
            sentenceDetector = new opennlp.tools.lang.english.SentenceDetector(modelFile);
        } catch (IOException e) {
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
        String docText = aJCas.getDocumentText();
        int sentenceOffsets[] = sentenceDetector.sentPosDetect(docText);
        int begin = 0;
        for (int i = 0; i < sentenceOffsets.length; i++) {
            Sentence sentence = new Sentence(aJCas, begin, sentenceOffsets[i]);
            sentence.setComponentId(COMPONENT_ID);
            sentence.addToIndexes();
            begin = sentenceOffsets[i];
        }
    }

}
