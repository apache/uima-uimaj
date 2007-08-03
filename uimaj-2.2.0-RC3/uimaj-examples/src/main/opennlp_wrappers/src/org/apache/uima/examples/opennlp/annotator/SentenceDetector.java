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

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.examples.opennlp.Sentence;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Simple Annotator to detect sentences and create Sentence annotations in the CAS. Uses the OpenNLP
 * MaxEnt Sentence Detector.
 * 
 */
public class SentenceDetector extends JCasAnnotator_ImplBase {

  public static final String MODEL_FILE_PARAM = "ModelFile";

  public static final String COMPONENT_ID = "OpenNLP Sentence Detector";

  private opennlp.tools.lang.english.SentenceDetector sentenceDetector;

  /**
   * Initialize the Annotator.
   * 
   * @see JCasAnnotator_ImplBase#initialize(UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    String modelFile;
    modelFile = (String) aContext.getConfigParameterValue(MODEL_FILE_PARAM);
    try {
      sentenceDetector = new opennlp.tools.lang.english.SentenceDetector(modelFile);
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * Process a CAS.
   * 
   * @see JCasAnnotator_ImplBase#process(JCas)
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
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
