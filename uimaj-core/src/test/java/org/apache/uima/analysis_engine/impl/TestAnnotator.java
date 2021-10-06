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
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.Session;
import org.apache.uima.resource.impl.ConfigurationManager_impl;

/**
 * Annotator class used for testing.
 */
public class TestAnnotator extends CasAnnotator_ImplBase {
  // Process method saves information to these static fields,
  // which are queried by the unit test.
  public static String lastDocument;

  public static String stringParamValue;

  public static ResultSpecification lastResultSpec;

  public static boolean typeSystemInitCalled;

  public static Session lastConfigurationManagerSession;

  public static synchronized String getLastDocument() {
    return lastDocument;
  }

  public static synchronized ResultSpecification getLastResultSpec() {
    return lastResultSpec;
  }

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    typeSystemInitCalled = false;
    lastResultSpec = null;
    lastDocument = null;
    stringParamValue = (String) getContext().getConfigParameterValue("StringParam");
  }

  @Override
  public void typeSystemInit(TypeSystem aTypeSystem) {
    typeSystemInitCalled = true;
  }

  @Override
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    lastDocument = aCAS.getDocumentText();
    lastResultSpec = getResultSpecification();
    ConfigurationManager cfgMgr = ((UimaContextAdmin) getContext()).getConfigurationManager();
    lastConfigurationManagerSession = ((ConfigurationManager_impl) cfgMgr).getSession();
  }
}
