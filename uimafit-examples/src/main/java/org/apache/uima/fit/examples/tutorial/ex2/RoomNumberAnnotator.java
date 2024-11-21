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
package org.apache.uima.fit.examples.tutorial.ex2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.examples.tutorial.type.RoomNumber;
import org.apache.uima.jcas.JCas;

/**
 * This class demonstrates annotating member variables with the @ConfigurationParameter annotation.
 * Defining configuration parameters in this way in combination with using the
 * {@link JCasAnnotator_ImplBase uimaFIT's JCasAnnotator_ImplBase} class obviates the need for an
 * initialize method at all because the super class initialize method calls
 * {@link ConfigurationParameterInitializer#initialize}. This method initializes member variables
 * annotated as configuration parameters using the configuration parameter information provided in
 * the UimaContext.
 * 
 * This class was copied from the uimaj-examples project and modified in following ways:
 * <ul>
 * <li>The package name was changed to org.apache.uima.fit.tutorial.ex2</li>
 * <li>The super class was changed to org.apache.uima.fit.component.JCasAnnotator_ImplBase</li>
 * <li>The class is annotated with org.apache.uima.fit.descriptor.TypeCapability</li>
 * <li>mPatterns and mLocations is annotated with @ConfigurationParameters</li>
 * <li>the initialize method was removed</li>
 * </ul>
 */
@TypeCapability(outputs = { "org.apache.uima.tutorial.RoomNumber",
    "org.apache.uima.tutorial.RoomNumber:building" })
public class RoomNumberAnnotator extends JCasAnnotator_ImplBase {

  @ConfigurationParameter(name = "Patterns")
  private Pattern[] mPatterns;

  @ConfigurationParameter(name = "Locations")
  private String[] mLocations;

  /**
   * @see JCasAnnotator_ImplBase#process(JCas)
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // get document text
    String docText = aJCas.getDocumentText();

    // loop over patterns
    for (int i = 0; i < mPatterns.length; i++) {
      Matcher matcher = mPatterns[i].matcher(docText);
      while (matcher.find()) {
        // found one - create annotation
        RoomNumber annotation = new RoomNumber(aJCas, matcher.start(), matcher.end());
        annotation.setBuilding(mLocations[i]);
        annotation.addToIndexes();
        getLogger().debug("Found: {}", annotation);
      }
    }
  }
}
