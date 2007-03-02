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

package org.apache.uima.flow.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.CasFlowController_ImplBase;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * Simple FlowController that invokes components in a fixed sequence.
 */
public class FixedFlowController extends CasFlowController_ImplBase {

  /**
   * Key for the configuation parameter that determines what should happen to a
   * CAS after it has been input to a CAS Multiplier.  Possible values are:
   * continue – the CAS continues on to the next element in the flow
   * stop – the CAS will no longer continue in the flow, and will be returned from the
   *        aggregate if possible.
   * drop – the CAS will no longer continue in the flow, and will be dropped (not 
   *        returned from the aggregate) if possible.
   * dropIfNewCasProduced (the default) – if the CAS multiplier produced a new CAS as a
   *        result of processing this CAS, then this CAS will be dropped. If not, then this CAS
   *        will continue.
   */
  public static final String PARAM_ACTION_AFTER_CAS_MULTIPLIER = "ActionAfterCasMultiplier";

  private static final int ACTION_CONTINUE = 0;

  private static final int ACTION_STOP = 1;

  private static final int ACTION_DROP = 2;

  private static final int ACTION_DROP_IF_NEW_CAS_PRODUCED = 3;

  private ArrayList mSequence;

  private int mActionAfterCasMultiplier;

  public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    FlowConstraints flowConstraints = aContext.getAggregateMetadata().getFlowConstraints();
    mSequence = new ArrayList();
    mSequence.addAll(Arrays.asList(((FixedFlow) flowConstraints).getFixedFlow()));

    String actionAfterCasMultiplier = (String) aContext
            .getConfigParameterValue(PARAM_ACTION_AFTER_CAS_MULTIPLIER);
    if ("continue".equalsIgnoreCase(actionAfterCasMultiplier)) {
      mActionAfterCasMultiplier = ACTION_CONTINUE;
    } else if ("stop".equalsIgnoreCase(actionAfterCasMultiplier)) {
      mActionAfterCasMultiplier = ACTION_STOP;
    } else if ("drop".equalsIgnoreCase(actionAfterCasMultiplier)) {
      mActionAfterCasMultiplier = ACTION_DROP;
    } else if ("dropIfNewCasProduced".equalsIgnoreCase(actionAfterCasMultiplier)) {
      mActionAfterCasMultiplier = ACTION_DROP_IF_NEW_CAS_PRODUCED;
    } else if (actionAfterCasMultiplier == null) {
      mActionAfterCasMultiplier = ACTION_DROP_IF_NEW_CAS_PRODUCED; // default
    } else {
      throw new ResourceInitializationException(ResourceInitializationException.INVALID_ACTION_AFTER_CAS_MULTIPLIER,
              new Object[]{actionAfterCasMultiplier});
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.CasFlowController_ImplBase#computeFlow(org.apache.uima.cas.CAS)
   */
  public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
    return new FixedFlowObject(0);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.flow.FlowController_ImplBase#addAnalysisEngines(java.util.Collection)
   */
  public void addAnalysisEngines(Collection aKeys) {
    // Append new keys to end of Sequence
    mSequence.addAll(aKeys);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.flow.FlowController_ImplBase#removeAnalysisEngines(java.util.Collection)
   */
  public void removeAnalysisEngines(Collection aKeys) throws AnalysisEngineProcessException {
    //Remove keys from Sequence
    mSequence.removeAll(aKeys);
  }

  public static FlowControllerDescription getDescription() {
    URL descUrl = FixedFlowController.class
            .getResource("/org/apache/uima/flow/FixedFlowController.xml");
    FlowControllerDescription desc;
    try {
      desc = (FlowControllerDescription) UIMAFramework.getXMLParser().parse(
              new XMLInputSource(descUrl));
    } catch (InvalidXMLException e) {
      throw new UIMARuntimeException(e);
    } catch (IOException e) {
      throw new UIMARuntimeException(e);
    }
    return desc;
  }

  class FixedFlowObject extends CasFlow_ImplBase {
    private int currentStep;

    private boolean wasPassedToCasMultiplier = false;

    private boolean casMultiplierProducedNewCas = false;

    private boolean internallyCreatedCas = false;

    /**
     * Create a new fixed flow starting at step <code>startStep</code> of the fixed sequence.
     * 
     * @param startStep
     *          index of mSequence to start at
     */
    public FixedFlowObject(int startStep) {
      this(startStep, false);
    }

    /**
     * Create a new fixed flow starting at step <code>startStep</code> of the fixed sequence.
     * 
     * @param startStep
     *          index of mSequence to start at
     * @param internallyCreatedCas
     *          true to indicate that this Flow object is for a CAS that was produced by a
     *          CasMultiplier within this aggregate. Such CASes area allowed to be dropped and not
     *          output from the aggregate.
     * 
     */
    public FixedFlowObject(int startStep, boolean internallyCreatedCas) {
      currentStep = startStep;
      this.internallyCreatedCas = internallyCreatedCas;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.flow.Flow#next()
     */
    public Step next() throws AnalysisEngineProcessException {
      // if CAS was passed to a CAS multiplier on the last step, special processing
      // is needed according to the value of the ActionAfterCasMultiplier config parameter
      if (wasPassedToCasMultiplier) {
        switch (mActionAfterCasMultiplier) {
          case ACTION_STOP:
            return new FinalStep();
          case ACTION_DROP:
            return new FinalStep(internallyCreatedCas);
          case ACTION_DROP_IF_NEW_CAS_PRODUCED:
            if (casMultiplierProducedNewCas) {
              return new FinalStep(internallyCreatedCas);
            }
            // else, continue with flow
            break;
          // if action is ACTION_CONTINUE, just continue with flow
        }
        wasPassedToCasMultiplier = false;
        casMultiplierProducedNewCas = false;
      }

      if (currentStep >= mSequence.size()) {
        return new FinalStep(); // this CAS has finished the sequence
      }

      // if next step is a CasMultiplier, set wasPassedToCasMultiplier to true for next time
      // TODO: optimize
      AnalysisEngineMetaData md = (AnalysisEngineMetaData) getContext()
              .getAnalysisEngineMetaDataMap().get(mSequence.get(currentStep));
      if (md.getOperationalProperties().getOutputsNewCASes())
        wasPassedToCasMultiplier = true;

      // now send the CAS to the next AE in sequence.
      return new SimpleStep((String)mSequence.get(currentStep++));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.flow.CasFlow_ImplBase#newCasProduced(CAS, String)
     */
    public Flow newCasProduced(CAS newCas, String producedBy) throws AnalysisEngineProcessException {
      // record that the input CAS has been segmented (affects its subsequent flow)
      casMultiplierProducedNewCas = true;
      // start the new output CAS from the next node after the CasMultiplier that produced it
      int i = 0;
      while (!mSequence.get(i).equals(producedBy))
        i++;
      return new FixedFlowObject(i + 1, true);
    }
  }
}
