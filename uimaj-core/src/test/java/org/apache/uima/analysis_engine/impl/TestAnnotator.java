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

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.Annotator_ImplBase;
import org.apache.uima.analysis_engine.annotator.TextAnnotator;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;

/**
 * Annotator class used for testing.
 * 
 */
public class TestAnnotator extends Annotator_ImplBase implements TextAnnotator {
  // Process method saves information to these static fields,
  // which are queried by the unit test.
  public static String lastDocument;

  public static String stringParamValue;

  public static ResultSpecification lastResultSpec;

  public static boolean typeSystemInitCalled;

  public static synchronized String getLastDocument() {
    return lastDocument;  
  }
  
  public static synchronized ResultSpecification getLastResultSpec() {
    return lastResultSpec;
  }
  
  /**
   * @see org.apache.uima.analysis_engine.annotator.Annotator#initialize(AnnotatorContext)
   */
  public void initialize(AnnotatorContext aContext) throws AnnotatorConfigurationException,
          AnnotatorInitializationException {
    super.initialize(aContext);
    typeSystemInitCalled = false;
    lastResultSpec = null;
    lastDocument = null;
    try {
      stringParamValue = (String) getContext().getConfigParameterValue("StringParam");
    } catch (AnnotatorContextException e) {
      throw new AnnotatorInitializationException(e);
    }
  }

  public void typeSystemInit(TypeSystem aTypeSystem) {
    typeSystemInitCalled = true;
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.TextAnnotator#process(CAS,ResultSpecification)
   */
  public void process(CAS aCAS, ResultSpecification aResultSpec) throws AnnotatorProcessException {
    // set static fields to contain document text, result spec,
    // and value of StringParam configuration parameter.
    lastDocument = aCAS.getDocumentText();
    lastResultSpec = aResultSpec;
  }
}
