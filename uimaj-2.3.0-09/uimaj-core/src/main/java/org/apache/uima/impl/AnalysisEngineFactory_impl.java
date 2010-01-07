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

package org.apache.uima.impl;

import java.util.Map;

import org.apache.uima.Constants;
import org.apache.uima.ResourceFactory;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.analysis_engine.impl.AggregateAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.MultiprocessingAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.UimacppAnalysisEngineImpl;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/**
 * Resource Factory that handles {@link AnalysisEngineDescription} and {@link TaeDescription}
 * elements.
 * 
 * 
 */
public class AnalysisEngineFactory_impl implements ResourceFactory {
  /**
   * @see org.apache.uima.ResourceFactory#produceResource(java.lang.Class,
   *      org.apache.uima.resource.ResourceSpecifier, java.util.Map)
   */
  public Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    // It is important to know whether we need a Multiprocessing-capable
    // Analysis Engine implementation - this is determined by whether there
    // is a value for the PARAM_NUM_SIMULTANEOUS_REQUESTS parameter.
    boolean multiprocessing = (aAdditionalParams != null)
            && aAdditionalParams.containsKey(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS);

    Resource resource = null;
    if (aSpecifier instanceof ResourceCreationSpecifier
            && aResourceClass.isAssignableFrom(TextAnalysisEngine.class))
    // NOTE: for backwards-compatibility, we have to check TextAnalysisEngine,
    // not AnalysisEngine. Otherwise produceTAE would fail becasue
    // TextAnalysisEngien.class.isAssignableFrom(AnalysisEngine.class) is false.
    {
      ResourceCreationSpecifier spec = (ResourceCreationSpecifier) aSpecifier;
      if (multiprocessing) {
        resource = new MultiprocessingAnalysisEngine_impl();
      } else {

        String frameworkImpl = spec.getFrameworkImplementation();
        if (frameworkImpl == null || frameworkImpl.length() == 0) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.MISSING_FRAMEWORK_IMPLEMENTATION,
                  new Object[] { aSpecifier.getSourceUrlString() });
        }

        if (frameworkImpl.startsWith(Constants.CPP_FRAMEWORK_NAME)) {
          resource = new UimacppAnalysisEngineImpl();
        } else if (frameworkImpl.startsWith(Constants.JAVA_FRAMEWORK_NAME)) {
          if (spec instanceof AnalysisEngineDescription
                  && !((AnalysisEngineDescription) spec).isPrimitive()) {
            resource = new AggregateAnalysisEngine_impl();
          } else {
            resource = new PrimitiveAnalysisEngine_impl();
          }
        } else {
          throw new ResourceInitializationException(
                  ResourceInitializationException.UNSUPPORTED_FRAMEWORK_IMPLEMENTATION,
                  new Object[] { spec.getFrameworkImplementation(), aSpecifier.getSourceUrlString() });
        }
      }
    }

    if (resource != null && resource.initialize(aSpecifier, aAdditionalParams)) {
      return resource;
    } else {
      return null;
    }
  }
}
