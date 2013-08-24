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

package org.apache.uima.fit.examples.resource;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;

/**
 * Example for the use of external resources with uimaFIT.
 */
public class ExternalResourceExample3 {
  /**
   * Simple example resource that can use another resource.
   */
  public static class ChainableResource extends Resource_ImplBase {
    public final static String RES_CHAINED_RESOURCE = "chainedResource";

    @ExternalResource(key = RES_CHAINED_RESOURCE, mandatory = false)
    private ChainableResource chainedResource;

    @Override
    public void afterResourcesInitialized() {
      // init logic that requires external resources
      System.out.println(getClass().getSimpleName() + ": " + chainedResource);
    }
  }

  /**
   * Example annotator that uses the resource. In the process() we only test if the model was
   * properly initialized by uimaFIT
   */
  public static class Annotator2 extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    final static String RES_MODEL = "model";

    @ExternalResource(key = RES_MODEL)
    private ChainableResource model;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      System.out.println(getClass().getSimpleName() + ": " + model);
    }
  }

  /**
   * Illustrate how to configure the annotator with a chainable resource
   */
  public static void main(String[] args) throws Exception {
    AnalysisEngineDescription aed = createEngineDescription(
            Annotator2.class,
            Annotator2.RES_MODEL,
            createExternalResourceDescription(ChainableResource.class,
                    ChainableResource.RES_CHAINED_RESOURCE,
                    createExternalResourceDescription(ChainableResource.class)));

    // Check the external resource was injected
    AnalysisEngine ae = createEngine(aed);
    ae.process(ae.newJCas());
  }
}