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
package org.apache.uima.fit.factory;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.resource.ResourceInitializationException;

/**
 */
public final class UimaContextFactory {
  private UimaContextFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Create a new UimaContext with the given configuration parameters set.
   * 
   * @param configurationParameters
   *          The parameters to be set.
   * @return The new UimaContext.
   * @throws ResourceInitializationException
   *           If the context could not be created.
   */
  public static UimaContext createUimaContext(Object... configurationParameters)
          throws ResourceInitializationException {
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(NoOpAnnotator.class, null,
            configurationParameters);
    return engine.getUimaContext();
  }

}
