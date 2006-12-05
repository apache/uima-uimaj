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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
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
 * <p>
 * This is an alternative implementation of
 * {@link org.apache.uima.examples.flow.WhiteboardFlowController}. It is slightly more complex but
 * should acheive better performance because CAS Type handles are resolved once, during
 * intitialization, instead of repeatedly resolved at each step of the flow.
 */
public class WhiteboardFlowController2 extends CasFlowController_ImplBase {
  /**
   * A Collection of {@link ComponentInfo} objects, one for each component of this aggregate. The
   * ComponentInfo objects store the component key and the component's required input types.
   */
  private Collection mComponentInfo = new ArrayList();

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
   * @see org.apache.uima.flow.CasFlowController_ImplBase#typeSystemInit(org.apache.uima.cas.TypeSystem)
   */
  public void typeSystemInit(TypeSystem aTypeSystem) throws AnalysisEngineProcessException {
    super.typeSystemInit(aTypeSystem);
    // Iterate over available AEs and get the required input types of each AE.
    // Resolve those to Type handles in the TypeSystem and store this information in
    // the mComponentInfo field for use in routing.
    Iterator aeIter = getContext().getAnalysisEngineMetaDataMap().entrySet().iterator();
    while (aeIter.hasNext()) {
      Map.Entry entry = (Map.Entry) aeIter.next();
      String aeKey = (String) entry.getKey();
      AnalysisEngineMetaData md = (AnalysisEngineMetaData) entry.getValue();
      Capability[] capabilities = md.getCapabilities();

      ComponentInfo compInfo = new ComponentInfo();
      compInfo.key = aeKey;
      compInfo.inputTypesByCapability = new Type[capabilities.length][];

      for (int i = 0; i < capabilities.length; i++) {
        List inputTypes = new ArrayList();
        TypeOrFeature[] inputs = capabilities[i].getInputs();
        for (int j = 0; j < inputs.length; j++) {
          if (inputs[j].isType()) {
            Type typeHandle = aTypeSystem.getType(inputs[j].getName());
            if (typeHandle != null) {
              inputTypes.add(typeHandle);
            }
          }
        }
        compInfo.inputTypesByCapability[i] = new Type[inputTypes.size()];
        inputTypes.toArray(compInfo.inputTypesByCapability[i]);
      }
      mComponentInfo.add(compInfo);
    }
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
      CAS cas = getCas();

      // iterate over available AEs
      Iterator componentIter = mComponentInfo.iterator();
      while (componentIter.hasNext()) {
        ComponentInfo componentInfo = (ComponentInfo) componentIter.next();
        // skip AEs that were already called on this CAS
        if (!mAlreadyCalled.contains(componentInfo.key)) {
          boolean satisfied = false;
          for (int i = 0; i < componentInfo.inputTypesByCapability.length; i++) {
            satisfied = casContainsTypes(cas, componentInfo.inputTypesByCapability[i]);
            if (satisfied)
              break;
          }
          if (satisfied) {
            mAlreadyCalled.add(componentInfo.key);
            if (mLogger.isLoggable(Level.FINEST)) {
              getContext().getLogger().log(Level.FINEST, "Next AE is: " + componentInfo.key);
            }
            return new SimpleStep(componentInfo.key);
          }
        }
      }
      // no appropriate AEs to call - end of flow
      getContext().getLogger().log(Level.FINEST, "Flow Complete.");
      return new FinalStep();
    }

    /**
     * Checks if the CAS contains at least one instance of each of the specified types.
     * 
     * @param aCAS
     *          the CAS to check
     * @param aTypes
     *          array of types to look for
     * 
     * @return true iff <code>aCAS</code> contains at least one instance of each type in
     *         <code>aTypes</code>
     */
    private boolean casContainsTypes(CAS aCAS, Type[] aTypes) {
      for (int i = 0; i < aTypes.length; i++) {
        Iterator iter = aCAS.getIndexRepository().getAllIndexedFS(aTypes[i]);
        if (!iter.hasNext())
          return false;
      }
      return true;
    }
  }

  /**
   * Data structure that holds the key of a component (AnalysisEngine) and its required input types.
   */
  static private class ComponentInfo {
    String key;

    /**
     * Required input types, organized by capability. For example, inputTypesByCapability[0] is the
     * array of input types for the first capability. This is organized like this because an
     * AnalysisEngine is ready to run if <i>any</i> of its capabilities have all of their inputs
     * satisfied.
     */
    Type[][] inputTypesByCapability;
  }
}
