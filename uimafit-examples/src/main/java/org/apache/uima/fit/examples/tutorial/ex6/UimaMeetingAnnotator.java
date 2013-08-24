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
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.examples.tutorial.type.Meeting;
import org.apache.uima.fit.examples.tutorial.type.UimaMeeting;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;

/**
 * Example annotator that iterates over Meeting annotations and annotates a meeting as a UimaMeeting
 * if a UIMA acronym occurs in close proximity to that meeting. When combined in an aggregate TAE
 * with the UimaAcronymAnnotator, demonstrates the use of the ResourceManager to share data between
 * annotators.
 */
@TypeCapability(inputs = "org.apache.uima.tutorial.Meeting", outputs = "org.apache.uima.tutorial.UimaMeeting")
public class UimaMeetingAnnotator extends JCasAnnotator_ImplBase {
  static final String RES_UIMA_TERM_TABLE = "uimaTermTable";

  @ExternalResource(key = RES_UIMA_TERM_TABLE)
  private StringMapResource uimaTermTable;

  /**
   * @see JCasAnnotator_ImplBase#process(JCas)
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // get document text
    String text = aJCas.getDocumentText();

    // We iterate over all Meeting annotations, and if we determine that
    // the topic of a meeting is UIMA-related, we create a UimaMeeting
    // annotation. We add each UimaMeeting annotation to a list, and then
    // later go back and add these to the CAS indexes. We need to do this
    // because it's not allowed to add to an index that you're currently
    // iterating over.
    List<UimaMeeting> uimaMeetings = new ArrayList<UimaMeeting>();

    for (Meeting meeting : select(aJCas, Meeting.class)) {
      // get span of text within 50 chars on either side of meeting
      // (window size should probably be a configuration parameter)
      int begin = meeting.getBegin() - 50;
      int end = meeting.getEnd() + 50;
      if (begin < 0) {
        begin = 0;
      }
      if (end > text.length()) {
        end = text.length();
      }
      String window = text.substring(begin, end);

      // look for UIMA acronyms within this window
      StringTokenizer tokenizer = new StringTokenizer(window, " \t\n\r.<.>/?\";:[{]}\\|=+()!");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        // look up token in map to see if it is an acronym
        if (uimaTermTable.get(token) != null) {
          // create annotation
          UimaMeeting annot = new UimaMeeting(aJCas, meeting.getBegin(), meeting.getEnd());
          annot.setRoom(meeting.getRoom());
          annot.setDate(meeting.getDate());
          annot.setStartTime(meeting.getStartTime());
          annot.setEndTime(meeting.getEndTime());
          // Add annotation to a list, to be later added to the indexes.
          // We need to do this because it's not allowed to add to an
          // index that you're currently iterating over.
          uimaMeetings.add(annot);
          break;
        }
      }
    }

    for (UimaMeeting meeting : uimaMeetings) {
      meeting.addToIndexes();
    }
  }

  public static void main(String[] args) throws Exception {
    File outputDirectory = new File("target/examples/tutorial/ex6/");
    outputDirectory.mkdirs();

    ExternalResourceDescription resource = createExternalResourceDescription(
            StringMapResource_impl.class,
            "file:org/apache/uima/fit/examples/tutorial/ex6/uimaAcronyms.txt");

    AggregateBuilder builder = new AggregateBuilder();
    builder.add(createEngineDescription(UimaAcronymAnnotator.class,
            UimaAcronymAnnotator.RES_ACRONYM_TABLE, resource));
    builder.add(createEngineDescription(UimaMeetingAnnotator.class,
            UimaMeetingAnnotator.RES_UIMA_TERM_TABLE, resource));
    AnalysisEngineDescription aggregate = builder.createAggregateDescription();

    aggregate.toXML(new FileOutputStream(new File(outputDirectory, "UimaMeetingDetectorTAE.xml")));
  }
}
