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

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Testing that collectionProcessComplete is correctly propagated to annotators.
 */
public class AnnotatorForCollectionProcessCompleteTest extends CasAnnotator_ImplBase {

  public static volatile String lastValue;

  private String testValue;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    testValue = (String) aContext.getConfigParameterValue("TestValue");
  }

  @Override
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    // does nothing
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    lastValue = testValue;
  }
}
