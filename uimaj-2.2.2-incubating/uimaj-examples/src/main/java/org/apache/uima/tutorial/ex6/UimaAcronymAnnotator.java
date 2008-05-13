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

package org.apache.uima.tutorial.ex6;

import java.util.StringTokenizer;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.tutorial.UimaAcronym;

/**
 * Annotates UIMA acronyms and provides their expanded forms. When combined in an aggregate TAE with
 * the UimaMeetingAnnotator, demonstrates the use of the ResourceManager to share data between
 * annotators.
 * 
 * 
 */
public class UimaAcronymAnnotator extends JCasAnnotator_ImplBase {
  /** Map from acronyms to their expanded forms */
  private StringMapResource mMap;

  /**
   * @see AnalysisComponent#initialize(UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    // get a reference to the String Map Resource
    try {
      mMap = (StringMapResource) getContext().getResourceObject("AcronymTable");
    } catch (ResourceAccessException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * @see JCasAnnotator_ImplBase#process(JCas)
   */
  public void process(JCas aJCas) {
    // go through document word-by-word
    String text = aJCas.getDocumentText();
    int pos = 0;
    StringTokenizer tokenizer = new StringTokenizer(text, " \t\n\r.<.>/?\";:[{]}\\|=+()!", true);
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      // look up token in map to see if it is an acronym
      String expandedForm = mMap.get(token);
      if (expandedForm != null) {
        // create annotation
        UimaAcronym annot = new UimaAcronym(aJCas, pos, pos + token.length(), expandedForm);
        annot.addToIndexes();
      }
      // incrememnt pos and go to next token
      pos += token.length();
    }
  }

}
