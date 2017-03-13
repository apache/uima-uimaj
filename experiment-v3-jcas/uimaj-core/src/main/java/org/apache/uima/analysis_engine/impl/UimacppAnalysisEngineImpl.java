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

package org.apache.uima.analysis_engine.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.ResultNotSupportedException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.impl.UimaContext_ImplBase;
import org.apache.uima.impl.Util;
import org.apache.uima.internal.util.UUIDGenerator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.uimacpp.UimacppAnalysisComponent;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;

/**
 * Reference implementation of {@link AnalysisEngine}.
 * 
 * 
 */
public class UimacppAnalysisEngineImpl extends AnalysisEngineImplBase implements AnalysisEngine {
  /**
   * current class
   */
  private static final Class<UimacppAnalysisEngineImpl> CLASS_NAME = UimacppAnalysisEngineImpl.class;

  /**
   * The AnalysisEngineDescription for this AnlaysisEngine instance.
   */
  private ResourceCreationSpecifier mDescription;

  /**
   * For a primitive AnalysisEngine only, the Annotator instance that contains the analysis logic.
   */
  private UimacppAnalysisComponent mAnnotator;

  /**
   * For a primitive AnalysisEngine only, the AnnotatorContext instance that the Annotator uses to
   * access its environment.
   */
  private AnnotatorContext_impl mAnnotatorContext;

  private boolean mSofaAware = false;

  /**
   * If this is set it indicates that the AnalysisEngine is being constructed only to verify the
   * validity of the descriptor. The Annotator classes should not be instantiated in this case.
   */
  private boolean mVerificationMode = false;

  /**
   * @throws ResourceInitializationException or 
   * wraps invalid xml exception when processing imports
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier, Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
      // AnalysisEngine can be build from any ResourceCreationSpecifier-
      // CasConsumer descriptors as well as AnalysisEngine descriptors.
      if (!(aSpecifier instanceof ResourceCreationSpecifier)) {
        return false;
      }
      
      // aSpecifier must be a AnalysisEngineDescription or a CasConsumerDescription
      if (!(aSpecifier instanceof AnalysisEngineDescription)  
        && !(aSpecifier instanceof CasConsumerDescription) ) {
          return false;
      }

      mDescription = (ResourceCreationSpecifier) aSpecifier;

     // also framework implementation must start with org.apache.uima.cpp
     final String fwImpl = mDescription.getFrameworkImplementation();
     if (!fwImpl.startsWith(Constants.CPP_FRAMEWORK_NAME)) {
          return false;
     }

    // Aggregate TAF AEs mostly act like primitives (because the flow
    // control is all handled
    // in TAF). But, we do need to make sure that this AE's type system,
    // type priorities,
    // and fs indexes are formed by merging everything declared in the
    // individual
    // components' descriptors
    if (mDescription instanceof AnalysisEngineDescription  && 
    		 (! ((AnalysisEngineDescription)mDescription).isPrimitive())) { 

      // resolve deep type system imports
      try {
        mDescription.validate(getResourceManager());
      } catch (ResourceConfigurationException e) {
        throw new ResourceInitializationException(e);
      }

      mergeDelegateAnalysisEngineMetaData();      
    }

    ProcessingResourceMetaData md = (ProcessingResourceMetaData) mDescription.getMetaData();
    // resolve imports for primitives
    if (mDescription instanceof AnalysisEngineDescription  && 
            ((AnalysisEngineDescription)mDescription).isPrimitive()) { 
        try {
          md.resolveImports();
        } catch (InvalidXMLException e1) {
        throw new ResourceInitializationException(e1);
      }
       }

    super.initialize(aSpecifier, aAdditionalParams);

    getLogger().logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_init_begin__CONFIG", md.getName());

  
    // Normalize language codes. Need to do this since a wide variety of
    // spellings are acceptable according to ISO.
    normalizeIsoLangCodes(md);

    // clone this metadata and assign a UUID if not already present
    AnalysisEngineMetaData mdCopy = (AnalysisEngineMetaData) md.clone();

    if (mdCopy.getUUID() == null) {
      mdCopy.setUUID(UUIDGenerator.generate());
    }
    setMetaData(mdCopy);

    // Read parameters from the aAdditionalParams map.
    if (aAdditionalParams == null) {
      aAdditionalParams = Collections.emptyMap();
    }

    // determine if verification mode is on
    mVerificationMode = aAdditionalParams.containsKey(PARAM_VERIFICATION_MODE);

    // determine if this component is Sofa-aware (based on whether it
    // declares any input or output sofas in its capabilities)
    mSofaAware = getAnalysisEngineMetaData().isSofaAware();

    initializeAnalysisComponent();

    resetResultSpecificationToDefault();

    getLogger().logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_init_successful__CONFIG", md.getName());

    return true;
  }

  public void setResultSpecification(ResultSpecification aResultSpec) {
    if (aResultSpec == null) {
      resetResultSpecificationToDefault();
    } else if (mAnnotator != null) {
      //note have to check for null to handle "verification mode" where annotator is not instantiated
      mAnnotator.setResultSpecification(aResultSpec);
    }
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    if (mAnnotator != null)
      mAnnotator.destroy();
    getLogger().logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_destroyed__CONFIG", getMetaData().getName());
    super.destroy();
  }

  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException {
    enterProcess();
    try {
      // make initial call to the AnalysisComponent
      callAnalysisComponentProcess(aCAS);

      // return a CasIterator that allows caller to step through the
      // outputs
      // of this AnalysisComponent (if any)
      return new TafAnalysisComponentCasIterator(mAnnotator);
    } finally {
      exitProcess();
    }

  }

  public void batchProcessComplete() throws AnalysisEngineProcessException {
    enterBatchProcessComplete();
    try {
      mAnnotator.batchProcessComplete();
    } finally {
      exitBatchProcessComplete();
    }
  }

  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    enterCollectionProcessComplete();
    try {
      mAnnotator.collectionProcessComplete();
    } finally {
      exitCollectionProcessComplete();
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    // do base resource reconfiguration
    super.reconfigure();

    mAnnotator.reconfigure();
  }

  /**
   * Calls the Analysis Component's process method.
   * 
   * @param aCAS
   *          CAS to be processed by annotator
   * @throws AnalysisEngineProcessException -         
   */
  protected void callAnalysisComponentProcess(CAS aCAS) throws AnalysisEngineProcessException {
    // logging and instrumentation
    String resourceName = getMetaData().getName();
    getLogger().logrb(Level.FINE, CLASS_NAME.getName(), "process", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_process_begin__FINE", resourceName);
    try {
      // call Annotator's process method
      try {
        // lock out CAS functions to which annotator should not have
        // access
        enableProhibitedAnnotatorCasFunctions(aCAS, false);

        // Get the right view of the CAS. Sofa-aware components get the base CAS.
        // Sofa-unaware components get whatever is mapped to the _InitialView.   
        CAS view = Util.getStartingView(aCAS, mSofaAware, getUimaContextAdmin().getComponentInfo());
        // Get the right type of CAS and call the AnalysisComponent's
        // process method
        Class<CAS> requiredInterface = mAnnotator.getRequiredCasInterface();
        AbstractCas casToPass = getCasManager().getCasInterface(view, requiredInterface);
        mAnnotator.process(casToPass);
        getMBean().incrementCASesProcessed();
      } catch (Exception e) {
        if (e instanceof AnalysisEngineProcessException) {
          throw e;
        } else {
          throw new AnalysisEngineProcessException(
                  AnalysisEngineProcessException.ANNOTATOR_EXCEPTION, null, e);
        }
      } finally {
        // unlock CAS functions
        enableProhibitedAnnotatorCasFunctions(aCAS, true);
      }

      // log end of event
      getLogger().logrb(Level.FINE, CLASS_NAME.getName(), "process", LOG_RESOURCE_BUNDLE,
              "UIMA_analysis_engine_process_end__FINE", resourceName);
    } catch (Exception e) {
      // log and rethrow exception
      getLogger().log(Level.SEVERE, "", e);
      if (e instanceof AnalysisEngineProcessException)
        throw (AnalysisEngineProcessException) e;
      else
        throw new AnalysisEngineProcessException(e);
    }
  }

  /**
   * A utility method that performs initialization logic for a primitive AnalysisEngine.
   * 
   * @throws ResourceInitializationException
   *           if an initialization failure occurs
   */
  protected void initializeAnalysisComponent()
          throws ResourceInitializationException {
    // create Annotator Context and set Logger
    UimaContextAdmin uimaContext = getUimaContextAdmin();
    uimaContext.setLogger(UIMAFramework.getLogger(UimacppAnalysisComponent.class));
    mAnnotatorContext = new AnnotatorContext_impl(uimaContext);

    if (!mVerificationMode) {
      mAnnotator = new UimacppAnalysisComponent(mDescription, this);

      getUimaContextAdmin().defineCasPool(mAnnotator.getCasInstancesRequired(),
              getPerformanceTuningSettings(), mSofaAware);

      mAnnotator.initialize(uimaContext);
    }
  }

  /**
   * For an aggregate TAF AE, sets this aggregate AE's Type System, Type Priorities, and FS Index
   * Descriptions equal to the result of merging the information from its delegate AEs.
   * 
   * @throws ResourceInitializationException
   *           if an error occurs
   */
  protected void mergeDelegateAnalysisEngineMetaData() throws ResourceInitializationException {
    // do the merge
    TypeSystemDescription aggTypeSystem = CasCreationUtils.mergeDelegateAnalysisEngineTypeSystems(
            (AnalysisEngineDescription) mDescription, getResourceManager());
    TypePriorities aggTypePriorities = CasCreationUtils.mergeDelegateAnalysisEngineTypePriorities(
            (AnalysisEngineDescription) mDescription, getResourceManager());
    FsIndexCollection aggIndexColl = CasCreationUtils
            .mergeDelegateAnalysisEngineFsIndexCollections((AnalysisEngineDescription)mDescription, getResourceManager());

    // assign results of merge to this aggregate AE's metadata
    ProcessingResourceMetaData aggregateMD = (ProcessingResourceMetaData) mDescription.getMetaData();
    aggregateMD.setTypeSystem(aggTypeSystem);
    aggregateMD.setTypePriorities(aggTypePriorities);
    aggregateMD.setFsIndexCollection(aggIndexColl);
  }

  /**
   * Lock/unlock CAS functions to which Annotators should not have access
   * 
   * @param aCAS
   *          the CAS to be affected
   * @param aEnable
   *          false to lock out functions, true to re-enable them
   */
  protected void enableProhibitedAnnotatorCasFunctions(CAS aCAS, boolean aEnable) {
    // these methods are on the CASMgr interface - currently this requires a
    // typecast
    if (aCAS instanceof CASMgr) {
      ((CASMgr) aCAS).enableReset(aEnable);
    }
  }

  /**
   * Calls the Analysis Component's next() method.
   * 
   * @return CAS returned by the analysis component
   * @throws AnalysisEngineProcessException -
   * @throws ResultNotSupportedException -
   */
  protected CAS callAnalysisComponentNext() throws AnalysisEngineProcessException,
          ResultNotSupportedException {
    try {
      AbstractCas absCas = mAnnotator.next();
      getMBean().incrementCASesProcessed();
      // notify UimaContext that a CAS was returned -- it uses
      // this information to track how many CASes the AnalysisComponent
      // is using at any one time.
      ((UimaContext_ImplBase) getUimaContext()).returnedCAS(absCas);

      // convert back to CASImpl and then get the initial View
      CAS casToReturn;
      if (absCas instanceof JCas) {
        casToReturn = ((JCas) absCas).getCas();
      } else {
        casToReturn = (CAS) absCas;
      }
      casToReturn = casToReturn.getView(CAS.NAME_DEFAULT_SOFA);
      return casToReturn;
    } catch (Exception e) {
      // log and rethrow exception
      getLogger().log(Level.SEVERE, "", e);
      if (e instanceof AnalysisEngineProcessException)
        throw (AnalysisEngineProcessException) e;
      else
        throw new AnalysisEngineProcessException(e);
    }
  }

  /**
   * @deprecated
   * @return -
   */
  @Deprecated
  protected AnalysisProcessData_impl createAnalysisProcessData() {
    try {
      return new AnalysisProcessData_impl(newCAS(), getPerformanceTuningSettings());
    } catch (ResourceInitializationException e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * For a primitive AnalysisEngine only, gets the Annotator.
   * 
   * @return the Annotator
   */
  protected AnalysisComponent _getAnnotator() {
    return mAnnotator; // TODO: check if the cas is ok
  }

  /**
   * For a primitive AnalysisEngine only, gets the AnnotatorContext.
   * 
   * @return the AnnotatorContext
   */
  protected AnnotatorContext_impl _getAnnotatorContext() {
    return mAnnotatorContext;
  }

  /**
   * For an aggregate AnalysisEngine only, gets a Map from each component's key to the specifier for
   * that component.
   * 
   * @return a Map with String keys and ResourceSpecifier values
   */
  protected Map<String, ResourceSpecifier> _getComponentCasProcessorSpecifierMap() {
    try {
      return ((AnalysisEngineDescription)mDescription).getDelegateAnalysisEngineSpecifiers();
    } catch (InvalidXMLException e) {
      // this should not happen, because we resolve delegates during
      // initialization
      throw new UIMARuntimeException(e);
    }
  }


  /**
   * Implements the iterator that steps through all outputs from an AnalysisComponent.
   */
  class TafAnalysisComponentCasIterator implements CasIterator {
    private AnalysisComponent mAnalysisComponent;

    TafAnalysisComponentCasIterator(AnalysisComponent aAnalysisComponent) {
      mAnalysisComponent = aAnalysisComponent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.core.CasIterator#hasNext()
     */
    public boolean hasNext() throws AnalysisEngineProcessException {
      enterProcess();
      try {
        return mAnalysisComponent.hasNext();
      } finally {
        exitProcess();
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.core.CasIterator#next(java.lang.Class)
     */
    public CAS next() throws AnalysisEngineProcessException {
      enterProcess();
      try {
        // Make sure that the AnalysisComponent has a next output CAS to
        // return
        boolean analysisComponentHasNext = mAnalysisComponent.hasNext();
        if (!analysisComponentHasNext) {
          throw new UIMA_IllegalStateException(UIMA_IllegalStateException.NO_NEXT_CAS,
                  new Object[0]);
        }
        // call AnalyaisComponent.next method to populate CAS
        try {
          CAS cas = callAnalysisComponentNext();
          // cas.setParentID(mOriginalCas.getID());
          return cas;
        } catch (Exception e) {
          if (e instanceof AnalysisEngineProcessException) {
            throw (AnalysisEngineProcessException) e;
          }
          throw new AnalysisEngineProcessException(e);
        }
      } finally {
        exitProcess();
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.analysis_engine.CasIterator#release()
     */
    public void release() {
      // nothing to do
    }
  }

}
