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

package org.apache.uima.util;

import java.util.Set;
import java.util.TreeSet;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FSTypeConstraint;
import org.apache.uima.resource.metadata.Capability;

/**
 * Static utility methods relating to analysis engines.
 * 
 */
public class AnalysisEngineUtils {

  /**
   * Creates a FSMatchConstraint used when formatting the CAS for output. This constraint filters
   * the feature structures in the CAS so that the only feature structures that are outputted are
   * those of types specified in the Analysis Engine's capability specification.
   * 
   * @param aMetaData
   *          metadata for the text analysis engine that is producing the results to be filtered
   * 
   * @return the filter to be passed to {@link TCasFormatter#format(CAS,FSMatchConstraint)}.
   */
  public static FSMatchConstraint createOutputFilter(AnalysisEngineMetaData aMetaData) {
    // get a list of the AE's output type names
    Set<String> outputTypes = new TreeSet<String>();
    // outputTypes.add("Document"); //always output the document
    Capability[] capabilities = aMetaData.getCapabilities();
    for (int i = 0; i < capabilities.length; i++) {
      TypeOrFeature[] outputs = capabilities[i].getOutputs();
      for (int j = 0; j < outputs.length; j++) {
        if (outputs[j].isType()) {
          outputTypes.add(outputs[j].getName());
        }
      }
    }

    FSTypeConstraint constraint = ConstraintFactory.instance().createTypeConstraint();

    for (String typeName  : outputTypes) {
      // add type to constraint
      constraint.add(typeName);
    }
    return constraint;
  }
}
