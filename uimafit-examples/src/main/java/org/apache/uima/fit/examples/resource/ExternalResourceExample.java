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
import static org.apache.uima.fit.factory.ExternalResourceFactory.createSharedResourceDescription;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

/**
 * Example for the use of external resources with uimaFIT.
 * 
 */
public class ExternalResourceExample {
  /**
   * Simple model that only stores the URI it was loaded from. Normally data would be loaded from
   * the URI instead and made accessible through methods in this class. This simple example only
   * allows to access the URI.
   */
  public static final class SharedModel implements SharedResourceObject {
    private String uri;

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
      uri = aData.getUri().toString();
    }

    public String getUri() {
      return uri;
    }
  }

  /**
   * Example annotator that uses the share model object. In the process() we only test if the model
   * was properly initialized by uimaFIT
   */
  public static class Annotator extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    final static String RES_MODEL = "model";

    @ExternalResource(key = RES_MODEL)
    private SharedModel model;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      // Prints the instance ID to the console - this proves the same instance
      // of the SharedModel is used in both Annotator instances.
      System.out.println(getClass().getSimpleName() + ": " + model);
    }
  }

  /**
   * Illustrate how to configure the annotator with the shared model object.
   */
  public static void main(String[] args) throws Exception {
    ExternalResourceDescription extDesc = createSharedResourceDescription(new File("somemodel.bin"),
            SharedModel.class);

    // Binding external resource to each Annotator individually
    AnalysisEngineDescription aed1 = createEngineDescription(Annotator.class, Annotator.RES_MODEL,
            extDesc);
    AnalysisEngineDescription aed2 = createEngineDescription(Annotator.class, Annotator.RES_MODEL,
            extDesc);

    // Check the external resource was injected
    AnalysisEngineDescription aaed = createEngineDescription(aed1, aed2);
    AnalysisEngine ae = createEngine(aaed);
    ae.process(ae.newJCas());
  }
}