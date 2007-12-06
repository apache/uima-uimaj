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

package org.apache.uima.examples;

import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * A simple multiple subject of analysis (multi-Sofa) example annotator Expects an English text Sofa
 * as input Creates a German text Sofa as output
 * 
 * This annotator has no configuration parameters, and requires no initialization method
 */

public class SofaExampleAnnotator extends CasAnnotator_ImplBase {
  public void process(CAS aCas) throws AnalysisEngineProcessException {
    CAS englishView, germanView;

    // get the CAS view for the English document
    englishView = aCas.getView("EnglishDocument");

    // Create the German text Sofa and open its view
    germanView = aCas.createView("GermanDocument");

    // Get some necessary Type System constants
    Type annot = englishView.getAnnotationType();
    Type cross = englishView.getTypeSystem().getType("sofa.test.CrossAnnotation");
    Feature other = cross.getFeatureByBaseName("otherAnnotation");

    // Get the English text
    String engText = englishView.getDocumentText();

    // Setup for translated text
    int engEnd = 0;
    int germBegin = 0;
    int germEnd = 0;
    StringBuffer translation = new StringBuffer();

    // Parse the English text
    StringTokenizer st = new StringTokenizer(engText);
    while (st.hasMoreTokens()) {
      String thisTok = st.nextToken();
      int engBegin = engText.indexOf(thisTok, engEnd);
      engEnd = engBegin + thisTok.length();

      // Create token annotations on English text
      AnnotationFS engAnnot = englishView.createAnnotation(annot, engBegin, engEnd);
      englishView.addFsToIndexes(engAnnot);

      // Simple word-by-word translation
      String germWord = translate(thisTok);

      // Accumulate the translated text
      if (germBegin > 0) {
        translation.append(' ');
        germBegin += 1;
      }
      translation.append(germWord);

      // Create token annotations on German text
      germEnd = germBegin + germWord.length();
      AnnotationFS germAnnot = germanView.createAnnotation(cross, germBegin, germEnd);
      germanView.addFsToIndexes(germAnnot);

      // add link to English text
      germAnnot.setFeatureValue(other, engAnnot);
      germBegin = germEnd;
    }

    // Finally, set the output tranlation Sofa data
    germanView.setDocumentText(translation.toString());

  }

  static char wThis[] = { 't', 'h', 'i', 's' };

  static char wBeer[] = { 'b', 'e', 'e', 'r' };

  static char wIs[] = { 'i', 's' };

  private String translate(String word) {
    String lword = word.toLowerCase();
    if (Arrays.equals(wThis, lword.toCharArray()))
      return new String("das");
    if (Arrays.equals(wBeer, lword.toCharArray()))
      return new String("bier");
    if (Arrays.equals(wIs, lword.toCharArray()))
      return new String("ist");
    return new String("gut");
  }

}
