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

package org.apache.uima.fit.factory.testAes;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource_ImplBase;

/**
 * Parametrized AE for testing {@link ExternalResource} annotations.
 * 
 */
public class ParameterizedAE2 extends JCasAnnotator_ImplBase {
  @ExternalResource
  DummyResource res;

  public static final String RES_OTHER = "other";

  @ExternalResource(key = RES_OTHER)
  DummyResource res2;

  public static final String RES_OPTIONAL = "optional";

  @ExternalResource(key = RES_OPTIONAL, mandatory = false)
  DummyResource res3;

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // Nothing to do
  }

  public static final class DummyResource extends Resource_ImplBase {
    public String getName() {
      return DummyResource.class.getName();
    }
  }
}
