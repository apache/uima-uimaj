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

package org.apache.uima.examples.flow;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.flow.CasFlowController_ImplBase;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * FlowController implementing a simple version of the "whiteboard" flow model. Each time a CAS is
 * received, it looks at the pool of available AEs that have not yet run on that CAS, and picks one
 * whose input requirements are satisfied.
 * <p>
 * Limitations: only looks at types, not features. Ignores languagesSupported. Does not handle
 * multiple Sofas or CasMultipliers.
 */
public class WhiteboardFlowController extends CasFlowController_ImplBase {

  /**
   * UIMA logger instance we will use to log messages when flow decisions are made.
   */
  private Logger mLogger;
  
  
  
  /* (non-Javadoc)
   * @see org.apache.uima.flow.FlowController_ImplBase#initialize(org.apache.uima.flow.FlowControllerContext)
   */
  public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    mLogger = aContext.getLogger();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.CasFlowController_ImplBase#computeFlow(org.apache.uima.cas.CAS)
   */
  public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
    WhiteboardFlow flow = new WhiteboardFlow();
    flow.setCas(aCAS);
    return flow;
  }

  /**
   * A separate instance of WhiteboardFlow is created for each input CAS, and is responsible for
   * routing that CAS to all appropriate AnalysisEngines.
   */
  class WhiteboardFlow extends CasFlow_ImplBase {
    private Set mAlreadyCalled = new HashSet();

    /**
     * Get the next AnalyisEngine that should receive the CAS.
     */
    public Step next() throws AnalysisEngineProcessException {
      // Get the CAS that this Flow object is responsible for routing.
      // Each Flow instance is responsible for a single CAS.
      CAS cas = getCas();

      // iterate over available AEs
      Iterator aeIter = getContext().getAnalysisEngineMetaDataMap().entrySet().iterator();
      while (aeIter.hasNext()) {
        Map.Entry entry = (Map.Entry) aeIter.next();
        // skip AEs that were already called on this CAS
        String aeKey = (String) entry.getKey();
        if (!mAlreadyCalled.contains(aeKey)) {
          // check for satisfied input capabilities (i.e. the CAS contains at least one instance
          // of each required input
          AnalysisEngineMetaData md = (AnalysisEngineMetaData) entry.getValue();
          Capability[] caps = md.getCapabilities();
          boolean satisfied = true;
          for (int i = 0; i < caps.length; i++) {
            satisfied = inputsSatisfied(caps[i].getInputs(), cas);
            if (satisfied)
              break;
          }
          if (satisfied) {
            mAlreadyCalled.add(aeKey);
            if (mLogger.isLoggable(Level.FINEST)) {
              getContext().getLogger().log(Level.FINEST, "Next AE is: " + aeKey);
            }
            return new SimpleStep(aeKey);
          }
        }
      }
      // no appropriate AEs to call - end of flow
      getContext().getLogger().log(Level.FINEST, "Flow Complete.");
      return new FinalStep();
    }

    /**
     * Checks if a set of input requirements are satisfied in the given CAS. This currently looks
     * only at types, not features. It returns true iff the default CAS view's indexes contain at
     * least one instance of each required input type.
     * 
     * @param aInputs
     *          input requirements
     * @param aCAS
     *          the CAS to check against
     * @return true iff the input requirements are satisfied
     */
    private boolean inputsSatisfied(TypeOrFeature[] aInputs, CAS aCAS) {
      for (int i = 0; i < aInputs.length; i++) {
        TypeOrFeature input = aInputs[i];
        if (input.isType()) {
          Type type = aCAS.getTypeSystem().getType(input.getName());
          if (type == null)
            return false;
          Iterator iter = aCAS.getIndexRepository().getAllIndexedFS(type);
          if (!iter.hasNext())
            return false;
        }
      }
      return true;
    }
  }
}
