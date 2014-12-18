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

package org.apache.uima.analysis_engine.impl;

import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.Annotator_ImplBase;
import org.apache.uima.analysis_engine.annotator.GenericAnnotator;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Simple English to German translator
 * ... expects input "English" view to be delivered to process()
 * 
 */
public class MultiViewAnnotator extends Annotator_ImplBase implements GenericAnnotator {

  public void process(CAS aCas, ResultSpecification aResultSpec) throws AnnotatorProcessException {
    CAS engTcas, germTcas;

    engTcas = aCas;

    // Create the output German text Sofa and open CAS view
    germTcas = aCas.createView("GermanDocument");

    // Get some necessary Type System constants
    Type annot = engTcas.getAnnotationType();
    Type cross = engTcas.getTypeSystem().getType("sofa.test.CrossAnnotation");
    Feature other = cross.getFeatureByBaseName("otherAnnotation");

    // Get the English text
    String engText = engTcas.getDocumentText();

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
      AnnotationFS engAnnot = engTcas.createAnnotation(annot, engBegin, engEnd);
      engTcas.getIndexRepository().addFS(engAnnot);

      // Simple word-by-word translation
      String germWord = Translate(thisTok);

      // Accumulate the translated text
      if (germBegin > 0) {
        translation.append(' ');
        germBegin += 1;
      }
      translation.append(germWord.toCharArray(), 0, germWord.length());

      // Create token annotations on German text
      germEnd = germBegin + germWord.length();
      AnnotationFS germAnnot = germTcas.createAnnotation(cross, germBegin, germEnd);
      germTcas.getIndexRepository().addFS(germAnnot);

      // add link to English text
      germAnnot.setFeatureValue(other, engAnnot);
      germBegin = germEnd;
    }

    // Finally, set the output tranlation Sofa data
    germTcas.setDocumentText(translation.toString());

  }

  static char wThis[] = { 't', 'h', 'i', 's' };

  static char wBeer[] = { 'b', 'e', 'e', 'r' };

  static char wIs[] = { 'i', 's' };

  private String Translate(String word) {
    String lword = word.toLowerCase();
    if (Arrays.equals(wThis, lword.toCharArray()))
      return "das";
    if (Arrays.equals(wBeer, lword.toCharArray()))
      return "bier";
    if (Arrays.equals(wIs, lword.toCharArray()))
      return "ist";
    return "gut";
  }

}
