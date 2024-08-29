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

import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

/**
 * Simple FlowController that invokes components in a fixed sequence.
 */
public class FixedFlowController extends CasFlowController_ImplBase {

 // @formatter:off
  /**
   * Key for the configuration parameter that determines what should happen to a
   * CAS after it has been input to a CAS Multiplier.  Possible values are:
   * continue:  the CAS continues on to the next element in the flow
   * stop: the CAS will no longer continue in the flow, and will be returned from the
   *        aggregate if possible.
   * drop: the CAS will no longer continue in the flow, and will be dropped (not 
   *        returned from the aggregate) if possible.
   * dropIfNewCasProduced (the default): if the CAS multiplier produced a new CAS as a
   *        result of processing this CAS, then this CAS will be dropped. If not, then this CAS
   *        will continue.
   */
 // @formatter:on
  public static final String PARAM_ACTION_AFTER_CAS_MULTIPLIER = "ActionAfterCasMultiplier";

  private static final int ACTION_CONTINUE = 0;

  private static final int ACTION_STOP = 1;

  private static final int ACTION_DROP = 2;

  private static final int ACTION_DROP_IF_NEW_CAS_PRODUCED = 3;

//@formatter:off
  // make final to work better in multi-thread case  UIMA-2373
  // working assumption: 
  //   A single instance of this class may be used on multiple replications of a UIMA pipeline.
  //     In this case, the initialization would be done once (because the same context object is passed for the replicas).
  //   The mSequence is read-only after being set up
  //     -- except for the calls to addAnalysisEngines or removeAnalysisEngines.
  //        But these are intended for maybe in the future supporting dynamically adding/removing annotators from
  //        aggregates - but are not supported as of 2014.
  //   
  //   Users might run several CASes asynchronously using this FixedFlowController object,
  //   on different threads. However, users will not re-initialize this with a different 
  //   flowControllerContext while this object is controlling CASes from the previous Object.
  // When this was a synchronized list, some contention observed between the "reads", which can be eliminated by
  //   switching this to a copy-on-write kind of final list.
  //      -- this has the added "benefit" (maybe eventually) of having better semantics for letting existing
  //         Flow objects continue to use the "old" settings, and only the new ones picking up the new ones.
//@formatter:on
private final List<String> mSequence = new CopyOnWriteArrayList<>(); // UIMA-4013

  private int mActionAfterCasMultiplier;

  @Override
  public synchronized void initialize(FlowControllerContext aContext)
          throws ResourceInitializationException {
    if (getContext() == aContext) {
      return; // only do initialize once per instance of this and same context
    }
    mSequence.clear(); // not cleared for multiple init calls (perhaps on multiple threads) with the
                       // same context
    super.initialize(aContext);
    FlowConstraints flowConstraints = aContext.getAggregateMetadata().getFlowConstraints();
    if (flowConstraints instanceof FixedFlow) {
      String[] sequence = ((FixedFlow) flowConstraints).getFixedFlow();
      ArrayList<String> keysToAdd = new ArrayList<>(sequence.length);
      for (String key : sequence) {
        if (!aContext.getAnalysisEngineMetaDataMap().containsKey(key)) {
          throw new ResourceInitializationException(
                  ResourceInitializationException.FLOW_CONTROLLER_MISSING_DELEGATE,
                  new Object[] { this.getClass().getName(), key,
                      aContext.getAggregateMetadata().getSourceUrlString() });
        }
        keysToAdd.add(key);
      }
      mSequence.addAll(keysToAdd);
    } else {
      throw new ResourceInitializationException(
              ResourceInitializationException.FLOW_CONTROLLER_REQUIRES_FLOW_CONSTRAINTS,
              new Object[] { this.getClass().getName(), "fixedFlow",
                  aContext.getAggregateMetadata().getSourceUrlString() });
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
      throw new ResourceInitializationException(
              ResourceInitializationException.INVALID_ACTION_AFTER_CAS_MULTIPLIER,
              new Object[] { actionAfterCasMultiplier });
    }
  }

  @Override
  public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
    return new FixedFlowObject(0);
  }

  @Override
  public void addAnalysisEngines(Collection<String> aKeys) {
    // Append new keys to end of Sequence
    mSequence.addAll(aKeys);
  }

  @Override
  public void removeAnalysisEngines(Collection<String> aKeys)
          throws AnalysisEngineProcessException {
    // Remove keys from Sequence
    mSequence.removeAll(aKeys);
  }

  public static FlowControllerDescription getDescription() {
    var desc = getResourceSpecifierFactory().createFlowControllerDescription();

    desc.setImplementationName(FixedFlowController.class.getName());

    var metaData = desc.getFlowControllerMetaData();
    metaData.setName("Fixed Flow Controller");
    metaData.setDescription("Simple FlowController that uses the FixedFlow element of the\n"
            + "\t\taggregate descriptor to determine a linear flow.");
    metaData.setVendor("The Apache Software Foundation");
    metaData.setVersion("1.0");

    var capability = getResourceSpecifierFactory().createCapability();
    metaData.setCapabilities(capability);

    var param = getResourceSpecifierFactory().createConfigurationParameter();
    param.setName("ActionAfterCasMultiplier");
    param.setType("String");
    param.setDescription(
            "The action to be taken after a CAS has been input to a CAS Multiplier and the CAS Multiplier has finished processing it.\n"
                    + "\t\t Valid values are:\n"
                    + "\t\t\tcontinue - the CAS continues on to the next element in the flow\n"
                    + "\t\t\tstop - the CAS will no longer continue in the flow, and will be returned from the aggregate if possible.\n"
                    + "\t\t\tdrop - the CAS will no longer continue in the flow, and will be dropped (not returned from the aggregate) if possible.\t \n"
                    + "\t\t\tdropIfNewCasProduced (the default) - if the CAS multiplier produced a new CAS as a result of processing this CAS, then this\n"
                    + "\t\t\t\tCAS will be dropped.  If not, then this CAS will continue.");

    var parameterDeclarations = getResourceSpecifierFactory()
            .createConfigurationParameterDeclarations();
    parameterDeclarations.setConfigurationParameters(param);
    metaData.setConfigurationParameterDeclarations(parameterDeclarations);

    var paramSetting = getResourceSpecifierFactory().createNameValuePair();
    paramSetting.setName("ActionAfterCasMultiplier");
    paramSetting.setValue("dropIfNewCasProduced");

    var parameterSettings = getResourceSpecifierFactory().createConfigurationParameterSettings();
    parameterSettings.setParameterSettings(paramSetting);
    metaData.setConfigurationParameterSettings(parameterSettings);

    return desc;
  }

  public static FlowControllerDescription makeDefaultDescription() {
    var desc = getResourceSpecifierFactory().createFlowControllerDescription();

    desc.setImplementationName(FixedFlowController.class.getName());

    var metaData = desc.getFlowControllerMetaData();
    metaData.setName("Fixed Flow Controller");
    metaData.setDescription("Simple FlowController that uses the FixedFlow element of the\n"
            + "\t\taggregate descriptor to determine a linear flow.");
    metaData.setVendor("The Apache Software Foundation");
    metaData.setVersion("1.0");

    var capability = getResourceSpecifierFactory().createCapability();
    metaData.setCapabilities(capability);

    var param = getResourceSpecifierFactory().createConfigurationParameter();
    param.setName("ActionAfterCasMultiplier");
    param.setType("String");
    param.setDescription(
            "The action to be taken after a CAS has been input to a CAS Multiplier and the CAS Multiplier has finished processing it.\n"
                    + "\t\t Valid values are:\n"
                    + "\t\t\tcontinue - the CAS continues on to the next element in the flow\n"
                    + "\t\t\tstop - the CAS will no longer continue in the flow, and will be returned from the aggregate if possible.\n"
                    + "\t\t\tdrop - the CAS will no longer continue in the flow, and will be dropped (not returned from the aggregate) if possible.\t \n"
                    + "\t\t\tdropIfNewCasProduced (the default) - if the CAS multiplier produced a new CAS as a result of processing this CAS, then this\n"
                    + "\t\t\t\tCAS will be dropped.  If not, then this CAS will continue.");

    var parameterDecls = getResourceSpecifierFactory().createConfigurationParameterDeclarations();
    parameterDecls.setConfigurationParameters(param);
    metaData.setConfigurationParameterDeclarations(parameterDecls);

    var paramSetting = getResourceSpecifierFactory().createNameValuePair();
    paramSetting.setName("ActionAfterCasMultiplier");
    paramSetting.setValue("dropIfNewCasProduced");

    var parameterSettings = getResourceSpecifierFactory().createConfigurationParameterSettings();
    parameterSettings.setParameterSettings(paramSetting);
    metaData.setConfigurationParameterSettings(parameterSettings);

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
     * @param aStartStep
     *          index of mSequence to start at
     * @param aInternallyCreatedCas
     *          true to indicate that this Flow object is for a CAS that was produced by a
     *          CasMultiplier within this aggregate. Such CASes area allowed to be dropped and not
     *          output from the aggregate.
     */
    public FixedFlowObject(int aStartStep, boolean aInternallyCreatedCas) {
      currentStep = aStartStep;
      internallyCreatedCas = aInternallyCreatedCas;
    }

    @Override
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
      AnalysisEngineMetaData md = getContext().getAnalysisEngineMetaDataMap()
              .get(mSequence.get(currentStep));
      if (md.getOperationalProperties().getOutputsNewCASes()) {
        wasPassedToCasMultiplier = true;
      }

      // now send the CAS to the next AE in sequence.
      return new SimpleStep(mSequence.get(currentStep++));
    }

    @Override
    public Flow newCasProduced(CAS newCas, String producedBy)
            throws AnalysisEngineProcessException {
      // record that the input CAS has been segmented (affects its subsequent flow)
      casMultiplierProducedNewCas = true;
      // start the new output CAS from the next node after the CasMultiplier that produced it
      int i = 0;
      while (!mSequence.get(i).equals(producedBy)) {
        i++;
      }
      return new FixedFlowObject(i + 1, true);
    }
  }
}
