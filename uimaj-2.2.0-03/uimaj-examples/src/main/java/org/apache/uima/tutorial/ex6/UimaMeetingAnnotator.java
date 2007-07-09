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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.tutorial.Meeting;
import org.apache.uima.tutorial.UimaMeeting;

/**
 * Example annotator that iterates over Meeting annotations and annotates a meeting as a UimaMeeting
 * if a UIMA acronym occurs in close proximity to that meeting. When combined in an aggregate TAE
 * with the UimaAcronymAnnotator, demonstrates the use of the ResourceManager to share data between
 * annotators.
 * 
 * 
 */
public class UimaMeetingAnnotator extends JCasAnnotator_ImplBase {
  /** Map whose keys are UIMA terms. */
  private StringMapResource mMap;

  /**
   * @see AnalysisComponent#initialize(UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    try {
      // get a reference to the String Map Resource
      mMap = (StringMapResource) getContext().getResourceObject("UimaTermTable");
    } catch (ResourceAccessException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * @see JCasAnnotator_ImplBase#process(JCas)
   */
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // get document text
    String text = aJCas.getDocumentText();

    // We iterate over all Meeting annotations, and if we determine that
    // the topic of a meeting is UIMA-related, we create a UimaMeeting
    // annotation. We add each UimaMeeting annotation to a list, and then
    // later go back and add these to the CAS indexes. We need to do this
    // because it's not allowed to add to an index that you're currently
    // iterating over.
    List uimaMeetings = new ArrayList();

    FSIndex meetingIndex = aJCas.getAnnotationIndex(Meeting.type);
    FSIterator iter = meetingIndex.iterator();
    while (iter.isValid()) {
      Meeting meeting = (Meeting) iter.get();
      // get span of text within 50 chars on either side of meeting
      // (window size should probably be a config. param)
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
        if (mMap.get(token) != null) {
          // create annotation
          UimaMeeting annot = new UimaMeeting(aJCas, meeting.getBegin(), meeting.getEnd(), meeting
                  .getRoom(), meeting.getDate(), meeting.getStartTime(), meeting.getEndTime());
          // Add annotation to a list, to be later added to the indexes.
          // We need to do this because it's not allowed to add to an
          // index that you're currently iterating over.
          uimaMeetings.add(annot);
          break;
        }
      }

      iter.moveToNext();
    }

    Iterator uimaMeetingIter = uimaMeetings.iterator();
    while (uimaMeetingIter.hasNext()) {
      UimaMeeting annot = (UimaMeeting) uimaMeetingIter.next();
      annot.addToIndexes();
    }
  }

}
