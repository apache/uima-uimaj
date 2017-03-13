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

package org.apache.uima.tutorial.ex5;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.tutorial.RoomNumber;
import org.apache.uima.util.Level;

/**
 * Example annotator that detects room numbers using Java 1.4 regular expressions.
 */
public class RoomNumberAnnotator extends JCasAnnotator_ImplBase {
  private Pattern[] mPatterns;

  private String[] mLocations;

  public static final String MESSAGE_DIGEST = "org.apache.uima.tutorial.ex5.RoomNumberAnnotator_Messages";

  /**
   * @see AnalysisComponent#initialize(UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    // Get config. parameter values
    String[] patternStrings = (String[]) aContext.getConfigParameterValue("Patterns");
    mLocations = (String[]) aContext.getConfigParameterValue("Locations");

    // compile regular expressions
    mPatterns = new Pattern[patternStrings.length];
    for (int i = 0; i < patternStrings.length; i++) {
      try {
        mPatterns[i] = Pattern.compile(patternStrings[i]);
      } catch (PatternSyntaxException e) {
        throw new ResourceInitializationException(MESSAGE_DIGEST, "regex_syntax_error",
                new Object[] { patternStrings[i] }, e);
      }
    }
  }

  /**
   * @see JCasAnnotator_ImplBase#process(JCas)
   */
  public void process(JCas aJCas) {
    // get document text
    String docText = aJCas.getDocumentText();

    // loop over patterns
    for (int i = 0; i < mPatterns.length; i++) {
      Matcher matcher = mPatterns[i].matcher(docText);
      while (matcher.find()) {
        // found one - create annotation
        RoomNumber annotation = new RoomNumber(aJCas);
        annotation.setBegin(matcher.start());
        annotation.setEnd(matcher.end());
        annotation.addToIndexes();
        annotation.setBuilding(mLocations[i]);
        getContext().getLogger().log(Level.FINEST, "Found: " + annotation);
      }
    }
  }

}
