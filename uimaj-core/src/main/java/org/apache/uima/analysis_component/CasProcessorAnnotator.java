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
package org.apache.uima.analysis_component;

import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.impl.AnalysisEngineProcessorAdapter;
import org.apache.uima.analysis_engine.impl.AnalysisEngineProcessorStub;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ResourceMetaData;

public class CasProcessorAnnotator extends AnalysisEngineProcessorAdapter {
  private ResourceMetaData metaData;
  private CasProcessor<? extends Exception> delegate;

  public CasProcessorAnnotator(CasProcessor<? extends Exception> aCasAnnotator) {
    metaData = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineMetaData();
    delegate = aCasAnnotator;
  }

  @Override
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    setStub(makeDelegate());
    return super.initialize(aSpecifier, aAdditionalParams);
  }

  private AnalysisEngineProcessorStub makeDelegate() {
    return new AnalysisEngineProcessorStub() {

      @Override
      public ResourceMetaData getMetaData() {
        return metaData;
      }

      @Override
      public void process(CAS aCAS) throws AnalysisEngineProcessException {
        try {
          delegate.process(aCAS);
        } catch (Exception e) {
          if (e instanceof AnalysisEngineProcessException) {
            throw (AnalysisEngineProcessException) e;
          } else {
            throw new AnalysisEngineProcessException(e);
          }
        }
      }
    };
  }

  public static CasProcessorAnnotator of(CasProcessor<? extends Exception> aCasAnnotator)
          throws ResourceInitializationException {
    CasProcessorAnnotator engine = new CasProcessorAnnotator(aCasAnnotator);
    engine.initialize(null, null);
    return engine;
  }
}
