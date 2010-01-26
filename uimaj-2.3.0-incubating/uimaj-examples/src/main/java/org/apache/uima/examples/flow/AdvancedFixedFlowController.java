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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.CasFlowController_ImplBase;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.ParallelStep;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Simple FlowController that invokes components in a fixed sequence.
 */
public class AdvancedFixedFlowController extends CasFlowController_ImplBase {
  public static final String PARAM_ACTION_AFTER_CAS_MULTIPLIER = "ActionAfterCasMultiplier";

  public static final String PARAM_ALLOW_CONTINUE_ON_FAILURE = "AllowContinueOnFailure";
  
  public static final String PARAM_FLOW = "Flow";

  private static final int ACTION_CONTINUE = 0;

  private static final int ACTION_STOP = 1;

  private static final int ACTION_DROP = 2;

  private static final int ACTION_DROP_IF_NEW_CAS_PRODUCED = 3;

  private ArrayList mSequence;

  private int mActionAfterCasMultiplier;
  
  private Set mAEsAllowingContinueOnFailure = new HashSet();

  public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);

    String[] flow = (String[])aContext.getConfigParameterValue(PARAM_FLOW);
    mSequence = new ArrayList();
    for (int i = 0; i < flow.length; i++) {
      String[] aes = flow[i].split(",");
      if (aes.length == 1) {
        mSequence.add(new SimpleStep(aes[0]));
      } else {
        Collection keys = new ArrayList();
        keys.addAll(Arrays.asList(aes));
        mSequence.add(new ParallelStep(keys));
      }            
    }

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
      throw new ResourceInitializationException(); // TODO
    }
    
    String[] aeKeysAllowingContinue = (String[])aContext
            .getConfigParameterValue(PARAM_ALLOW_CONTINUE_ON_FAILURE);
    if (aeKeysAllowingContinue != null) {
      mAEsAllowingContinueOnFailure.addAll(Arrays.asList(aeKeysAllowingContinue));
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
  public synchronized void addAnalysisEngines(Collection aKeys) {
    // Append new keys as a ParallelStep at end of Sequence
    // This is just an example of what could be done.
    // Note that in general, a "Collection" is unordered
    mSequence.add(new ParallelStep(new ArrayList(aKeys)));
  }

  /* (non-Javadoc)
   * @see org.apache.uima.flow.FlowController_ImplBase#removeAnalysisEngines(java.util.Collection)
   */
  public synchronized void removeAnalysisEngines(Collection aKeys) throws AnalysisEngineProcessException {
    // Remove keys from Sequence ... replace with null so step indices are still valid
    for (int i = 0; i < mSequence.size(); ++i) {
      Step step = (Step)mSequence.get(i);
      if (step instanceof SimpleStep && aKeys.contains(((SimpleStep)step).getAnalysisEngineKey())) {
        mSequence.set(i, null);
      }
      else if (step instanceof ParallelStep) {
        Collection keys = new ArrayList(((ParallelStep)step).getAnalysisEngineKeys());
        keys.removeAll(aKeys);
        if (keys.isEmpty()) {
          mSequence.set(i, null);
        }
        else {
          mSequence.set(i, new ParallelStep(keys));
        }
      }
    }
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

      // Get next in sequence, skipping any disabled ones
      Step nextStep;
      synchronized (AdvancedFixedFlowController.this) {
        do {
          if (currentStep >= mSequence.size()) {
            return new FinalStep(); // this CAS has finished the sequence
          }
          nextStep = (Step) mSequence.get(currentStep++);
        } while (nextStep == null);
      }

      // if next step is a CasMultiplier, set wasPassedToCasMultiplier to true for next time
      if (stepContainsCasMultiplier(nextStep))
        wasPassedToCasMultiplier = true;

      // now send the CAS to the next AE(s) in sequence.
      return nextStep;
    }

    /**
     * @param nextStep
     * @return
     */
    private boolean stepContainsCasMultiplier(Step nextStep) {
      if (nextStep instanceof SimpleStep) {
        AnalysisEngineMetaData md = (AnalysisEngineMetaData) getContext()
          .getAnalysisEngineMetaDataMap().get(((SimpleStep)nextStep).getAnalysisEngineKey());
        return md != null && md.getOperationalProperties() != null &&
                md.getOperationalProperties().getOutputsNewCASes();
      }
      else if (nextStep instanceof ParallelStep) {
        Iterator iter = ((ParallelStep)nextStep).getAnalysisEngineKeys().iterator();
        while (iter.hasNext()) {
          String key = (String)iter.next();
          AnalysisEngineMetaData md = (AnalysisEngineMetaData) getContext()
            .getAnalysisEngineMetaDataMap().get(key);
          if (md != null && md.getOperationalProperties() != null &&
                  md.getOperationalProperties().getOutputsNewCASes())
            return true;
        }
        return false;
      }
      else
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.flow.CasFlow_ImplBase#newCasProduced(CAS, String)
     */
    public synchronized Flow newCasProduced(CAS newCas, String producedBy) throws AnalysisEngineProcessException {
      // record that the input CAS has been segmented (affects its subsequent flow)
      casMultiplierProducedNewCas = true;
      // start the new output CAS from the next node after the CasMultiplier that produced it
      int i = 0;
      while (!stepContains((Step)mSequence.get(i), producedBy))
        i++;
      return new FixedFlowObject(i + 1, true);
    }

    /**
     * @param object
     * @param producedBy
     * @return
     */
    private boolean stepContains(Step step, String producedBy) {
      if (step instanceof SimpleStep) {
        return ((SimpleStep)step).getAnalysisEngineKey().equals(producedBy);
      }
      else if (step instanceof ParallelStep) {
        Iterator iter = ((ParallelStep)step).getAnalysisEngineKeys().iterator();
        while (iter.hasNext()) {
          String key = (String)iter.next();
          if (key.equals(producedBy))
            return true;
        }
        return false;
      }
      else
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.uima.flow.CasFlow_ImplBase#continueOnFailure(java.lang.String, java.lang.Exception)
     */
    public boolean continueOnFailure(String failedAeKey, Exception failure) {
      return mAEsAllowingContinueOnFailure.contains(failedAeKey);
    }
  }
}
