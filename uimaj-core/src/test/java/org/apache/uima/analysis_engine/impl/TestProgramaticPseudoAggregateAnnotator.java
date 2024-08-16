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

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;

/**
 * Aggregate annotator which programmatically assembles its internals instead of relying on an
 * {@link AnalysisEngineDescription}. To the outside, it looks like a primitive but it properly
 * creates child contexts internally.
 */
public class TestProgramaticPseudoAggregateAnnotator extends CasAnnotator_ImplBase {

  private AnalysisEngine delegate;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);

    String paramValue = (String) getContext().getConfigParameterValue("StringParam");

    AnalysisEngineDescription delegateDesc = UIMAFramework.getResourceSpecifierFactory()
            .createAnalysisEngineDescription();
    delegateDesc.setPrimitive(true);
    delegateDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    delegateDesc.setAnnotatorImplementationName(TestAnnotator.class.getName());
    ConfigurationParameter param = new ConfigurationParameter_impl();
    param.setName("StringParam");
    param.setType(ConfigurationParameter.TYPE_STRING);
    delegateDesc.getAnalysisEngineMetaData().getConfigurationParameterDeclarations()
            .setConfigurationParameters(param);
    delegateDesc.getMetaData().getConfigurationParameterSettings()
            .setParameterSettings(new NameValuePair_impl("StringParam", "initial"));

    // Forward the parameter value to the child context
    UimaContextAdmin childContext = ((UimaContextAdmin) aContext).createChild("delegate", null);
    childContext.getConfigurationManager().setConfigParameterValue(
            childContext.getQualifiedContextName() + "StringParam", paramValue);

    Map<String, Object> additionalParams = new HashMap<>();
    additionalParams.put(Resource.PARAM_UIMA_CONTEXT, childContext);

    delegate = UIMAFramework.produceAnalysisEngine(delegateDesc,
            ((UimaContextAdmin) getContext()).getResourceManager(), additionalParams);
  }

  @Override
  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    delegate.process(aCAS);
  }

  @Override
  public void batchProcessComplete() throws AnalysisEngineProcessException {
    delegate.batchProcessComplete();

    super.batchProcessComplete();
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    delegate.collectionProcessComplete();

    super.collectionProcessComplete();
  }

  @Override
  public void destroy() {
    delegate.destroy();

    super.destroy();
  }
}
