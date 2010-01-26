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

package org.apache.uima.examples.casMultiplier;

import java.io.File;
import java.io.PrintStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.cas.CAS;
import org.apache.uima.examples.PrintAnnotations;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.XMLInputSource;

/**
 * An example application that shows how to interact with a CasMultiplier. A CasMultiplier is a type
 * of Analysis Engine that outputs new CASes. One use of a CasMultiplier is to divide a large CAS
 * into smaller pieces - a CasMultiplier that does this is called a "Segmenter".
 * <p>
 * This program takes two arguments -
 * <ul>
 * <li>The path to the Analysis Engine Descriptor for the CasMultiplier to run (such as
 * descriptors/cas_multiplier/SimpleTextSegmenter.xml or
 * descriptors/cas_multiplier/SegmenterAndTokenizerAE.xml)</li>
 * <li>The file name of a text document to analyze (to see the effect of segmentation, choose a
 * document larger than 100k characters, which is the default segment size produced by the
 * SimpleTextSegmenter.</li>
 * </ul>
 */
public class CasMultiplierExampleApplication {
  static PrintStream outputStream;

  /**
   * Main program.
   * 
   * @param args
   *          Command-line arguments - see class description
   */
  public static void main(String[] args) {
    try {
      // get Resource Specifier from XML file
      XMLInputSource in = new XMLInputSource(args[0]);
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

      // create AnalysisEngine
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);

      // read input text file
      File textFile = new File(args[1]);
      String document = FileUtils.file2String(textFile, "UTF-8");

      // create a new CAS and set the document text
      CAS initialCas = ae.newCAS();
      initialCas.setDocumentText(document);

      // pass the CAS to the AnalysisEngine and get back
      // a CasIterator for stepping over the output CASes that are produced.
      CasIterator casIterator = ae.processAndOutputNewCASes(initialCas);
      while (casIterator.hasNext()) {
        CAS outCas = casIterator.next();

        // dump the document text and annotations for this segment
        System.out.println("********* NEW SEGMENT *********");
        System.out.println(outCas.getDocumentText());
        PrintAnnotations.printAnnotations(outCas, System.out);

        // release the CAS (important)
        outCas.release();
      }

      // If there's a CAS Consumer inside this aggregate and we want
      // it's collectionProcessComplete method to be called, we need to
      // call it ourselves. If run inside a CPE this would get called
      // automatically.
      ae.collectionProcessComplete();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
