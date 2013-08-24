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
package org.apache.uima.fit.examples.tutorial.ex6;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import java.io.File;
import java.io.FileOutputStream;
import java.util.StringTokenizer;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.examples.tutorial.type.UimaAcronym;
import org.apache.uima.jcas.JCas;

/**
 * Annotates UIMA acronyms and provides their expanded forms. When combined in an aggregate TAE with
 * the UimaMeetingAnnotator, demonstrates the use of the ResourceManager to share data between
 * annotators.
 * 
 */
@TypeCapability(outputs = { "org.apache.uima.examples.tutorial.UimaAcronym",
    "org.apache.uima.examples.tutorial.UimaAcronym:expandedForm" })
public class UimaAcronymAnnotator extends JCasAnnotator_ImplBase {

  static final String RES_ACRONYM_TABLE = "acronymTable";

  @ExternalResource(key = RES_ACRONYM_TABLE)
  private StringMapResource acronymTable;

  @Override
  public void process(JCas aJCas) {
    // go through document word-by-word
    String text = aJCas.getDocumentText();
    int pos = 0;
    StringTokenizer tokenizer = new StringTokenizer(text, " \t\n\r.<.>/?\";:[{]}\\|=+()!", true);
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      // look up token in map to see if it is an acronym
      String expandedForm = acronymTable.get(token);
      if (expandedForm != null) {
        // create annotation
        UimaAcronym annot = new UimaAcronym(aJCas, pos, pos + token.length());
        annot.setExpandedForm(expandedForm);
        annot.addToIndexes();
      }
      // incrememnt pos and go to next token
      pos += token.length();
    }
  }

  public static void main(String[] args) throws Exception {
    File outputDirectory = new File("target/examples/tutorial/ex6/");
    outputDirectory.mkdirs();
    
    AnalysisEngineDescription aed = createEngineDescription(
            UimaAcronymAnnotator.class,
            UimaAcronymAnnotator.RES_ACRONYM_TABLE,
            createExternalResourceDescription(StringMapResource_impl.class,
                    "file:org/apache/uima/fit/examples/tutorial/ex6/uimaAcronyms.txt"));
    
    aed.toXML(new FileOutputStream(new File(outputDirectory, "UimaAcronymAnnotator.xml")));
  }
}
