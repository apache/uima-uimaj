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

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.util.XMLInputSource;

/**
 * A simple Multiple Subject of Analysis (multi-Sofa) test application. Creates a text Sofa with
 * English text, calls an annotator that creates a text Sofa with German text, then dumps all
 * annotations found in both Sofas.
 * 
 * The application takes no arguments.
 */
public class SofaExampleApplication {
  /**
   * Main program
   * 
   */
  public static void main(String[] args) throws UIMAException, IOException {
    // parse AnalysisEngine descriptor
    XMLInputSource input = new XMLInputSource(
            "descriptors/analysis_engine/SofaExampleAnnotator.xml");
    AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
            input);

    // create AnalysisEngine
    AnalysisEngine seAnnotator = UIMAFramework.produceAnalysisEngine(desc);

    // create CAS
    CAS cas = seAnnotator.newCAS();

    // Create the English document Sofa
    CAS englishView = cas.createView("EnglishDocument");
    englishView.setDocumentText("this beer is good");

    // call a CAS Analysis Engine that "translates" the English document
    // and puts the translation into a German Sofa
    seAnnotator.process(cas);

    // get annotation iterator for the English CAS view
    FSIndex anIndex = englishView.getAnnotationIndex();
    FSIterator anIter = anIndex.iterator();

    // and print out all annotations found
    System.out.println("---Printing all annotations for English Sofa---");
    while (anIter.isValid()) {
      AnnotationFS annot = (AnnotationFS) anIter.get();
      System.out.println(" " + annot.getType().getName() + ": " + annot.getCoveredText());
      anIter.moveToNext();
    }

    // now try to get the CAS view for the German Sofa
    System.out.println();
    CAS germanView = cas.getView("GermanDocument");

    // and annotator iterator for the German CAS View
    anIndex = germanView.getAnnotationIndex();
    anIter = anIndex.iterator();
    Type cross = germanView.getTypeSystem().getType("sofa.test.CrossAnnotation");
    Feature other = cross.getFeatureByBaseName("otherAnnotation");

    // print out all annotations for the German Sofa
    System.out.println("---Printing all annotations for German Sofa---");
    while (anIter.isValid()) {
      AnnotationFS annot = (AnnotationFS) anIter.get();
      System.out.println(" " + annot.getType().getName() + ": " + annot.getCoveredText());
      if (annot.getType() == cross) {
        AnnotationFS crossAnnot = (AnnotationFS) annot.getFeatureValue(other);
        System.out.println("   other annotation feature: " + crossAnnot.getCoveredText());
      }
      anIter.moveToNext();
    }

    // Clean up
    seAnnotator.destroy();
  }
}
