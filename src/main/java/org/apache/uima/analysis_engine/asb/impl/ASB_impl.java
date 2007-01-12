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

package org.apache.uima.analysis_engine.asb.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.uima.ResourceFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.asb.ASB;
import org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase;
import org.apache.uima.analysis_engine.impl.AnalysisEngineManagementImpl;
import org.apache.uima.analysis_engine.impl.EmptyCasIterator;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.analysis_engine.metadata.impl.AnalysisEngineMetaData_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.apache.uima.flow.impl.FlowControllerContext_impl;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.Resource_ImplBase;
import org.apache.uima.util.Level;
import org.apache.uima.util.UimaTimer;

/**
 * A simple {@link ASB} implementation. This implementation is not specific to any transport
 * technology. It simply uses the {@link ResourceFactory} to acquire instances of its delegate
 * AnalysisEngines and then communicates with these delegate AnalysisEngines through the
 * {@link AnalysisEngine} interface. Any communication with remote AnalysisEngine services is done
 * through a {@link org.apache.uima.analysis_engine.service.AnalysisEngineServiceAdapter} and is not
 * the concern of this ASB implementation.
 * 
 * 
 */
public class ASB_impl extends Resource_ImplBase implements ASB {
  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class CLASS_NAME = ASB_impl.class;

  /**
   * Map from String key to delegate AnalysisEngine for all component AnalysisEngines within this
   * ASB.
   */
  private Map mComponentAnalysisEngineMap = new HashMap();

  /**
   * Map from String key to delegate AnalysisEngineMetaData for all component AnalysisEngines within
   * this ASB.
   */
  private Map mComponentAnalysisEngineMetaDataMap = new HashMap();

  /**
   * Map from String key to component (AnalysisEngine or FlowController) metadata.
   */
  private Map mAllComponentMetaDataMap = new HashMap();

  /**
   * Initialization parameters passed to this ASB's initialize method. They will be passed along to
   * the delegate AnalysisEngines.
   */
  private Map mInitParams;

  private SofaMapping[] mSofaMappings;

  private FlowControllerContainer mFlowControllerContainer;

  /**
   * Whether this aggregate is declared to output new CASes.
   */
  private boolean mOutputNewCASes;

  /**
   * UimaContext of the Aggregate AE containing this ASB.
   */
  private UimaContextAdmin mAggregateUimaContext;

  /**
   * Initializes this ASB.
   * 
   * @param aSpecifier
   *          describes how to create this ASB.
   * @param aAdditionalParams
   *          parameters which are passed along to the delegate Analysis Engines when they are
   *          constructed
   * 
   * @return true if and only if initialization completed successfully. Returns false if this
   *         implementation cannot handle the given <code>ResourceSpecifier</code>.
   * 
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
          throws ResourceInitializationException {
    UIMAFramework.getLogger(CLASS_NAME).logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize",
            LOG_RESOURCE_BUNDLE, "UIMA_asb_init_begin__CONFIG");
    if (!(aSpecifier instanceof ResourceCreationSpecifier)) {
      return false;
    }

    super.initialize(aSpecifier, aAdditionalParams);

    // save parameters for later
    mInitParams = aAdditionalParams;

    // save the sofa mappings of the aggregate AE that this AE is part of
    mSofaMappings = (SofaMapping[]) mInitParams.remove(Resource.PARAM_AGGREGATE_SOFA_MAPPINGS);
    // also remove them from the aAdditionalParams map, as they don't need to be passed
    // on to delegates
    // if (mSofaMappings != null)
    // mInitParams.remove(mInitParams.get(Resource.PARAM_AGGREGATE_SOFA_MAPPINGS));

    UIMAFramework.getLogger(CLASS_NAME).logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize",
            LOG_RESOURCE_BUNDLE, "UIMA_asb_init_successful__CONFIG");
    return true;
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    // destroy component AnalysisEngines
    Iterator i = mComponentAnalysisEngineMap.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry entry = (Map.Entry) i.next();
      Resource delegate = (Resource) entry.getValue();
      delegate.destroy();
    }
    if (mFlowControllerContainer != null) {
      mFlowControllerContainer.destroy();
    }
  }

  /**
   * Called by the Aggregate Analysis Engine to provide this ASB with information it needs to
   * operate.
   * 
   * @param aSpecifiers
   *          the specifiers for all component AEs within this Aggregate. The ASB will instantiate
   *          those AEs.
   * @param aParentContext
   *          UIMA context for the aggregate AE
   * @param aFlowControllerDeclaration
   *          declaration (key and specifier) of FlowController to be used for this aggregate.
   * @parma aAggregateMetadata metadata for the aggregate AE
   * @throws ResourceInitializationException
   */
  public void setup(Map aSpecifiers, UimaContextAdmin aParentContext,
          FlowControllerDeclaration aFlowControllerDeclaration,
          AnalysisEngineMetaData aAggregateMetadata) throws ResourceInitializationException {
    mAggregateUimaContext = aParentContext;

    // clear the delegate AnalysisEngine and AnalysisEngineMetaData maps
    mComponentAnalysisEngineMap.clear();
    mComponentAnalysisEngineMetaDataMap.clear();
    mAllComponentMetaDataMap.clear();

    // loop through all entires in the (key, specifier) map
    Iterator i = aSpecifiers.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry entry = (Map.Entry) i.next();
      String key = (String) entry.getKey();
      ResourceSpecifier spec = (ResourceSpecifier) entry.getValue();

      Map sofamap = new TreeMap();

      // retrieve the sofa mappings for input/output sofas of this analysis engine
      if (mSofaMappings != null && mSofaMappings.length > 0) {
        for (int s = 0; s < mSofaMappings.length; s++) {
          // the mapping is for this analysis engine
          if (mSofaMappings[s].getComponentKey().equals(key)) {
            // if component sofa name is null, replace it with the default for CAS sofa name
            // This is to support old style CAS
            if (mSofaMappings[s].getComponentSofaName() == null)
              mSofaMappings[s].setComponentSofaName(CAS.NAME_DEFAULT_TEXT_SOFA);
            sofamap.put(mSofaMappings[s].getComponentSofaName(), mSofaMappings[s]
                    .getAggregateSofaName());
          }
        }
      }

      // create child UimaContext and insert into mInitParams map
      if (mInitParams == null)
        mInitParams = new HashMap();
      UimaContextAdmin childContext = aParentContext.createChild(key, sofamap);
      mInitParams.put(Resource.PARAM_UIMA_CONTEXT, childContext);

      AnalysisEngine ae;

      // if running in "validation mode", don't try to connnect to any services
      if (mInitParams.containsKey(AnalysisEngineImplBase.PARAM_VERIFICATION_MODE)
              && !(spec instanceof ResourceCreationSpecifier)) {
        // but we need placeholder entries in maps to satisfy later checking
        ae = new DummyAnalysisEngine();
      } else {
        // construct an AnalysisEngine - initializing it with the parameters
        // passed to this ASB's initialize method
        ae = UIMAFramework.produceAnalysisEngine(spec, mInitParams);
      }

      // add the Analysis Engine and its metadata to the appropriate lists

      // add AnlaysisEngine to maps based on key
      mComponentAnalysisEngineMap.put(key, ae);
      mComponentAnalysisEngineMetaDataMap.put(key, ae.getAnalysisEngineMetaData());
    }

    // make Maps unmodifiable
    mComponentAnalysisEngineMap = Collections.unmodifiableMap(mComponentAnalysisEngineMap);
    mComponentAnalysisEngineMetaDataMap = Collections
            .unmodifiableMap(mComponentAnalysisEngineMetaDataMap);

    mOutputNewCASes = aAggregateMetadata.getOperationalProperties().getOutputsNewCASes();

    // initialize the FlowController
    initFlowController(aFlowControllerDeclaration, aParentContext, aAggregateMetadata);

    // initialize the AllComponentMetaData map to include AEs plus the FlowController
    mAllComponentMetaDataMap = new HashMap(mComponentAnalysisEngineMetaDataMap);
    mAllComponentMetaDataMap.put(aFlowControllerDeclaration.getKey(), mFlowControllerContainer
            .getMetaData());
    mAllComponentMetaDataMap = Collections.unmodifiableMap(mAllComponentMetaDataMap);
  }

  /**
   * Initializes the FlowController for this aggregate.
   */
  protected void initFlowController(FlowControllerDeclaration aFlowControllerDeclaration,
          UimaContextAdmin aParentContext, AnalysisEngineMetaData aAggregateMetadata)
          throws ResourceInitializationException {
    String key = aFlowControllerDeclaration.getKey();
    if (key == null || key.length() == 0) {
      key = "_FlowController"; // default key
    }

    HashMap flowControllerParams = new HashMap();

    // retrieve the sofa mappings for the FlowControler
    Map sofamap = new TreeMap();
    if (mSofaMappings != null && mSofaMappings.length > 0) {
      for (int s = 0; s < mSofaMappings.length; s++) {
        // the mapping is for this analysis engine
        if (mSofaMappings[s].getComponentKey().equals(key)) {
          // if component sofa name is null, replace it with the default for CAS sofa name
          // This is to support old style CAS
          if (mSofaMappings[s].getComponentSofaName() == null)
            mSofaMappings[s].setComponentSofaName(CAS.NAME_DEFAULT_TEXT_SOFA);
          sofamap.put(mSofaMappings[s].getComponentSofaName(), mSofaMappings[s]
                  .getAggregateSofaName());
        }
      }
    }
    FlowControllerContext ctxt = new FlowControllerContext_impl(aParentContext, key, sofamap,
            getComponentAnalysisEngineMetaData(), aAggregateMetadata);
    flowControllerParams.put(PARAM_UIMA_CONTEXT, ctxt);
    flowControllerParams.put(PARAM_RESOURCE_MANAGER, getResourceManager());
    mFlowControllerContainer = new FlowControllerContainer();
    mFlowControllerContainer.initialize(aFlowControllerDeclaration.getSpecifier(),
            flowControllerParams);
  }

  /**
   * @see org.apache.uima.analysis_engine.asb.ASB#getDelegateAnalysisEngineMetaData()
   */
  public Map getComponentAnalysisEngineMetaData() {
    return mComponentAnalysisEngineMetaDataMap;
  }

  /**
   * @see org.apache.uima.analysis_engine.asb.ASB#getDelegateAnalysisEngines()
   */
  public Map getComponentAnalysisEngines() {
    return mComponentAnalysisEngineMap;
  }

  public Map getAllComponentMetaData() {
    return mAllComponentMetaDataMap;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.asb.ASB#process(org.apache.uima.cas.CAS)
   */
  public CasIterator process(CAS aCAS) throws AnalysisEngineProcessException {
    return new AggregateCasIterator(aCAS);
  }

  /** Not public API. Is declared public so it can be used by test case. */
  public FlowControllerContainer getFlowControllerContainer() {
    return mFlowControllerContainer;
  }

  /**
   * Gets the MBean that provides the management interface to this AE. Returns the same object as
   * UimaContext.getManagementInterface() but casted to the AnalysisEngineManagement type.
   */
  protected AnalysisEngineManagementImpl getMBean() {
    return (AnalysisEngineManagementImpl) mAggregateUimaContext.getManagementInterface();
  }

  /**
   * Inner class implementing the CasIterator returned from the processAndOutputNewCASes(CAS)
   * method. This class contains most of the execution control logic for the aggregate AE.
   * 
   */
  class AggregateCasIterator implements CasIterator {
    /** The CAS that was input to the Aggregate AE's process method. */
    CAS mInputCas;

    /**
     * Stack, which holds StackFrame objects. A stack is necessary to handle CasMultipliers, because
     * when a CasMultiplier is invoked we need to save the state of processing of the current CAS
     * and start processing the output CASes instead. Since CasMultipliers can be nested, we need a
     * stack.
     */
    Stack casIteratorStack = new Stack();

    /**
     * Set of CASes that are in circulation (that is, they have been passed to FlowController and
     * the FlowController hasn't yet returned a FinalStep for them). Needed so we can clean up on
     * error.
     */
    Set activeCASes = new HashSet();

    /** Holds the next CAS to be returned, if it is known. */
    CAS nextCas = null;

    /** timer for timing processing done during calls to next() */
    UimaTimer timer = UIMAFramework.newTimer();

    /**
     * Creates a new AggregateCasIterator for the given input CAS. The CasIterator will return all
     * of the output CASes that this Aggregate AE generates when run on that input CAS, if any.
     * 
     * @param inputCas
     *          the CAS to be input to the Aggregate AE (this is the CAS that was passed to the
     *          Aggregate AE's processAndOutputNewCASes(CAS) method)
     * @throws AnalysisEngineProcessException
     *           if processing fails
     */
    public AggregateCasIterator(CAS inputCas) throws AnalysisEngineProcessException {
      timer.startIt();
      try {
        mInputCas = inputCas;
        // compute the flow for this CAS
        FlowContainer flow = mFlowControllerContainer.computeFlow(inputCas);
        // store CAS and Flow in an initial stack frame which will later be read by the
        // processUtilNextOutputCas method.
        casIteratorStack.push(new StackFrame(new EmptyCasIterator(), inputCas, flow, null));
        // do the initial procesing here (this will do all of the processing in the case
        // where this AE is not a CasMultiplier)
        nextCas = processUntilNextOutputCas();
        getMBean().incrementCASesProcessed();
      } finally {
        timer.stopIt();
        getMBean().reportAnalysisTime(timer.getDuration());
      }
    }

    /**
     * Returns whether there are any more CASes to be returned.
     */
    public boolean hasNext() throws AnalysisEngineProcessException {
      timer.startIt();
      try {
        if (nextCas == null)
          nextCas = processUntilNextOutputCas();
        return (nextCas != null);
      } finally {
        timer.stopIt();
        getMBean().reportAnalysisTime(timer.getDuration());
      }
    }

    /** Gets the next output CAS. */
    public CAS next() throws AnalysisEngineProcessException {
      timer.startIt();
      try {
        CAS toReturn = nextCas;
        if (toReturn == null)
          toReturn = processUntilNextOutputCas();
        if (toReturn == null) {
          throw new UIMA_IllegalStateException(UIMA_IllegalStateException.NO_NEXT_CAS,
                  new Object[0]);
        }
        nextCas = null;
        getMBean().incrementCASesProcessed();
        return toReturn;
      } finally {
        timer.stopIt();
        getMBean().reportAnalysisTime(timer.getDuration());
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.analysis_engine.CasIterator#release()
     */
    public void release() {
      // release all active, internal CASes
      Iterator iter = activeCASes.iterator();
      while (iter.hasNext()) {
        CAS cas = (CAS) iter.next();
        // mFlowControllerContainer.dropCas(cas);
        if (cas != mInputCas) // don't release the input CAS, it's caller's responsibility
        {
          cas.release();
        }
      }

      // recursively release any CasIterators on stack
      while (!casIteratorStack.isEmpty()) {
        ((StackFrame) casIteratorStack.pop()).casIterator.release();
      }
    }

    /**
     * This is the main execution control method for the aggregate AE. It is called by the
     * AggregateCasProcessorCasIterator.next() method. This runs the Aggregate, starting from its
     * current state, until such time as the FlowController indicates a CAS should be returned to
     * the caller. The AggregateCasIterator remembers the state, so calling this method a second
     * time will continue processing from where it left off.
     * 
     * @return the next CAS to be output. Returns null if the processing of the input CAS has
     *         completed.
     * 
     * @throws ProcessingException
     *           if a failure occurs during processing
     */
    private CAS processUntilNextOutputCas() throws AnalysisEngineProcessException {
      try {
        while (true) {
          CAS cas = null;
          FlowContainer flow = null;
          // get an initial CAS from the CasIteratorStack
          while (cas == null) {
            if (casIteratorStack.isEmpty()) {
              return null; // there are no more CAS Iterators to obtain CASes from
            }
            StackFrame frame = (StackFrame) casIteratorStack.peek();
            if (frame.casIterator.hasNext()) {
              cas = frame.casIterator.next();
              // this is a new output CAS so we need to compute a flow for it
              flow = frame.originalCasFlow.newCasProduced(cas, frame.casMultiplierAeKey);
            } else {
              // we've finished routing all the Output CASes from a StackFrame. Now
              // get the originalCas (the one that was input to the CasMultiplier) from
              // that stack frame and continue with its flow
              cas = frame.originalCas;
              flow = frame.originalCasFlow;
              cas.setCurrentComponentInfo(null); // this CAS is done being processed by the
              // previous AnalysisComponent
              casIteratorStack.pop(); // remove this state from the stack now
            }
          }

          // record active CASes in case we encounter an exception and need to release them
          activeCASes.add(cas);

          // get next CAS Processor to route to
          Step nextStep = flow.next();

          // repeat until we reach a FinalStep
          while (!(nextStep instanceof FinalStep)) {
            if (nextStep instanceof SimpleStep) {
              String nextAeKey = ((SimpleStep) nextStep).getAnalysisEngineKey();
              AnalysisEngine nextAe = (AnalysisEngine) mComponentAnalysisEngineMap.get(nextAeKey);
              if (nextAe != null) {
                // invoke next AE in flow
                CasIterator casIter;
                casIter = nextAe.processAndOutputNewCASes(cas);
                if (casIter.hasNext()) // new CASes are output
                {
                  // get the first output CAS
                  CAS outputCas = casIter.next();
                  // push the CasIterator, original CAS, and Flow onto a stack so we
                  // can get the other output CASes and the original CAS later
                  casIteratorStack.push(new StackFrame(casIter, cas, flow, nextAeKey));
                  // compute Flow for the output CAS
                  flow = flow.newCasProduced(outputCas, nextAeKey);
                  // now route the output CAS through the flow
                  cas = outputCas;
                  activeCASes.add(cas);
                } else {
                  // no new CASes are output; this cas is done being processed
                  // by that AnalysisEngine so clear the componentInfo
                  cas.setCurrentComponentInfo(null);
                }
              } else {
                throw new AnalysisEngineProcessException(
                        AnalysisEngineProcessException.UNKNOWN_ID_IN_SEQUENCE,
                        new Object[] { nextAeKey });
              }
            } else {
              throw new AnalysisEngineProcessException(
                      AnalysisEngineProcessException.UNSUPPORTED_STEP_TYPE, new Object[] { nextStep
                              .getClass() });
            }
            nextStep = flow.next();
          }
          // FinalStep was returned from FlowController.
          // We're done with the CAS.
          assert (nextStep instanceof FinalStep);
          FinalStep finalStep = (FinalStep) nextStep;
          activeCASes.remove(cas);
          // If this is the input CAS, just return null to indicate we're done
          // processing it. It is an error if the FlowController tried to drop this CAS.
          if (cas == mInputCas) {
            if (finalStep.getForceCasToBeDropped()) {
              throw new AnalysisEngineProcessException(
                      AnalysisEngineProcessException.ILLEGAL_DROP_CAS, new Object[0]);
            }
            return null;
          }
          // Otherwise, this is a new CAS produced within this Aggregate. We may or
          // may not return it, depending on the setting of the outputsNewCASes operational
          // property in this AE's metadata, and on the value of FinalStep.forceCasToBeDropped
          if (mOutputNewCASes && !finalStep.getForceCasToBeDropped()) {
            return cas;
          } else {
            cas.release();
          }
        }
      } catch (Exception e) {
        release(); // release held CASes before throwing exception
        if (e instanceof AnalysisEngineProcessException) {
          throw (AnalysisEngineProcessException) e;
        } else {
          throw new AnalysisEngineProcessException(e);
        }
      }
    }
  }

  /**
   * A frame on the processing stack for this Aggregate AE. Each time processing encounters a
   * CasMultiplier, a new StackFrame is created to store the state associated with the processing of
   * output CASes produced by that CasMultiplier.
   */
  static class StackFrame {
    StackFrame(CasIterator casIterator, CAS originalCas, FlowContainer originalCasFlow,
            String lastAeKey) {
      this.casIterator = casIterator;
      this.originalCas = originalCas;
      this.originalCasFlow = originalCasFlow;
      this.casMultiplierAeKey = lastAeKey;
    }

    /** CasIterator that returns output CASes produced by the CasMultiplier. */
    CasIterator casIterator;

    /** The CAS that was passed as input to the CasMultiplier. */
    CAS originalCas;

    /**
     * The Flow object for the original CAS, so we can pick up processing from there once we've
     * processed all the Output CASes.
     */
    FlowContainer originalCasFlow;

    /** The key that identifies the CasMultiplier whose output we are processing */
    String casMultiplierAeKey;
  }

  /**
   * Dummy analysis engine to use in place of remote AE when in "verification mode".
   */
  private static class DummyAnalysisEngine extends PrimitiveAnalysisEngine_impl {
    public DummyAnalysisEngine() {
      setMetaData(new AnalysisEngineMetaData_impl());
    }
  }
}
