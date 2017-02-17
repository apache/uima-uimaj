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
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.UimaContextHolder;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.ResultNotSupportedException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.impl.compatibility.AnalysisComponentAdapterFactory;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.impl.UimaContext_ImplBase;
import org.apache.uima.impl.Util;
import org.apache.uima.internal.util.UUIDGenerator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * Reference implementation of {@link AnalysisEngine}.
 * 
 * 
 */
public class PrimitiveAnalysisEngine_impl extends AnalysisEngineImplBase implements AnalysisEngine {
  /**
   * UIMA-5043 Set & restore the UimaContextHolder around calls to user code so it can be used to access the External Settings
   */
  
  
  private static final Class<PrimitiveAnalysisEngine_impl> CLASS_NAME = PrimitiveAnalysisEngine_impl.class;
 
  /**
   * flag for embedders to test to see if they have a 
   * version of the framework which supports
   * multi-threaded produceResource 
   */
  public static final boolean INIT_THREADSAFE = true;
      
  private ResultSpecification mCurrentResultSpecification;
  /**
   * result specification derived from the output capabilities of this primitive, used in intersection of languages
   * Recomputed when type system changes
   */
  private ResultSpecification rsFromOutputCapabilities;

  private boolean mResultSpecChanged;

  private TypeSystem mLastTypeSystem;

  /**
   * The ResourceCreationSpecifier parsed from this component's descriptor.
   */
  private ResourceCreationSpecifier mDescription;

  /**
   * The AnalysisComponent that holds the user-developed analysis logic.
   */
  private AnalysisComponent mAnalysisComponent;

  /**
   * If this is set it indicates that the AnalysisEngine is being constructed only to verify the
   * validity of the descriptor. The Annotator classes should not be instantiated in this case.
   */
  private boolean mVerificationMode = false;

  private boolean mSofaAware;

  /**
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier, Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    try {
      // Primitive AnalysisEngine can be build from any ResourceCreationSpecifier-
      // this includes CollectionReader, and CasConsumer descriptors
      // as well as AnalysisEngine descriptors.

      if (!(aSpecifier instanceof ResourceCreationSpecifier)) {
        return false;
      }

      // BUT, for AnalysisEngineDescriptions, must not be an aggregate
      if (aSpecifier instanceof AnalysisEngineDescription
              && !((AnalysisEngineDescription) aSpecifier).isPrimitive()) {
        return false;
      }

      mDescription = (ResourceCreationSpecifier) aSpecifier;

      // also framework implementation must start with org.apache.uima.java
      final String fwImpl = mDescription.getFrameworkImplementation();
      if (!fwImpl.startsWith(Constants.JAVA_FRAMEWORK_NAME)) {
        return false;
      }

      super.initialize(aSpecifier, aAdditionalParams);
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) mDescription.getMetaData();
      if (null == md) {
        md = UIMAFramework.getResourceSpecifierFactory().createProcessingResourceMetaData();
        md.setName("(null)");
      }

      // Get logger for this class
      Logger logger = getLogger();
      logger.logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize", LOG_RESOURCE_BUNDLE,
              "UIMA_analysis_engine_init_begin__CONFIG", md.getName());

      // Normalize language codes. Need to do this since a wide variety of
      // spellings are acceptable according to ISO.
      normalizeIsoLangCodes(md);

      // clone this metadata and assign a UUID if not already present
      ResourceMetaData mdCopy = (ResourceMetaData) md.clone();

      if (mdCopy.getUUID() == null) {
        mdCopy.setUUID(UUIDGenerator.generate());
      }
      setMetaData(mdCopy);

      // validate the AnalysisEngineDescription and throw a
      // ResourceInitializationException if there is a problem
      mDescription.validate(getResourceManager());

      // Read parameters from the aAdditionalParams map.
      if (aAdditionalParams == null) {
        aAdditionalParams = Collections.emptyMap();
      }
      // determine if verification mode is on
      mVerificationMode = aAdditionalParams.containsKey(PARAM_VERIFICATION_MODE);

      // determine if this component is Sofa-aware (based on whether it
      // declares any input or output sofas in its capabilities)
      mSofaAware = getAnalysisEngineMetaData().isSofaAware();

      initializeAnalysisComponent(aAdditionalParams);

      // Initialize ResultSpec based on output capabilities
      // TODO: should only do this for outermost AE
      resetResultSpecificationToDefault();

      logger.logrb(Level.CONFIG, CLASS_NAME.getName(), "initialize", LOG_RESOURCE_BUNDLE,
              "UIMA_analysis_engine_init_successful__CONFIG", md.getName());
      return true;
    } catch (ResourceConfigurationException e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR, new Object[] {
                  getMetaData().getName(), aSpecifier.getSourceUrlString() });
    }
  }

  /**
   * Loads, instantiates, and initializes the AnalysisComponent contained in this AE.
   * 
   * @param aAdditionalParams
   *          parameters passed to this AE's initialize method
   * 
   * @throws ResourceInitializationException
   *           if an initialization failure occurs
   */
  protected void initializeAnalysisComponent(Map<String, Object> aAdditionalParams)
          throws ResourceInitializationException {
    // instantiate Annotator class
    final String annotatorClassName = mDescription.getImplementationName();

    if (annotatorClassName == null || annotatorClassName.length() == 0) {
      throw new ResourceInitializationException(
              ResourceInitializationException.MISSING_ANNOTATOR_CLASS_NAME,
              new Object[] { mDescription.getSourceUrlString() });
    }

    // load annotator class
    Class<?> annotatorClass = null;
    try {
      // get UIMA extension ClassLoader if available
      final ClassLoader cl = getUimaContextAdmin().getResourceManager().getExtensionClassLoader();

      if (cl != null) {
        // use UIMA extension ClassLoader to load the class
        annotatorClass = cl.loadClass(annotatorClassName);
      } else {
        // use application ClassLoader to load the class
        annotatorClass = Class.forName(annotatorClassName);
      }
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.ANNOTATOR_CLASS_NOT_FOUND, new Object[] {
                  annotatorClassName, mDescription.getSourceUrlString() }, e);
    }

    // Make sure the specified class can be adapter to an AnalysisComponent.
    if (!(AnalysisComponent.class.isAssignableFrom(annotatorClass))
            && !AnalysisComponentAdapterFactory.isAdaptable(annotatorClass)) {
      throw new ResourceInitializationException(
              ResourceInitializationException.NOT_AN_ANALYSIS_COMPONENT, new Object[] {
                  annotatorClass.getName(), mDescription.getSourceUrlString() });
    }

    // if we're in verification mode, stop here and do not try to instantiate the
    // analysis component
    if (mVerificationMode) {
      return;
    }

    try {
      Object userObject = annotatorClass.newInstance();
      if (userObject instanceof AnalysisComponent) {
        mAnalysisComponent = (AnalysisComponent) userObject;
      } else {
        mAnalysisComponent = AnalysisComponentAdapterFactory.createAdapter(userObject,
                getAnalysisEngineMetaData(), aAdditionalParams);
      }
    } catch (ResourceInitializationException e) {
      throw e;
    } catch (Exception e) {
      
      throw new ResourceInitializationException(
              ResourceInitializationException.COULD_NOT_INSTANTIATE_ANNOTATOR, new Object[] {
                  annotatorClassName, mDescription.getSourceUrlString() }, e);
    }

    // Set Logger, to enable annotator-specific logging
    UimaContextAdmin uimaContext = getUimaContextAdmin();
    Logger logger = UIMAFramework.getLogger(annotatorClass);
    logger.setResourceManager(this.getResourceManager());
    uimaContext.setLogger(logger);

    // initialize AnalysisComponent
    UimaContext prevContext = UimaContextHolder.setContext(getUimaContext());  // for use by POJOs
    try {
      mAnalysisComponent.initialize(getUimaContext());
      // set up the CAS pool for this AE (this won't do anything if mAnalysisComponent.getCasInstancesRequired() == 0)
      getUimaContextAdmin().defineCasPool(mAnalysisComponent.getCasInstancesRequired(),
              getPerformanceTuningSettings(), mSofaAware);
    } catch (Exception e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.ANNOTATOR_INITIALIZATION_FAILED, new Object[] {
                  annotatorClassName, mDescription.getSourceUrlString() }, e);
    } finally {
      UimaContextHolder.setContext(prevContext);
    }
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
    if (mAnalysisComponent != null) {
      UimaContext prevContext = UimaContextHolder.setContext(getUimaContext());  // for use by POJOs
      try {
        mAnalysisComponent.destroy();
      } finally {
        UimaContextHolder.setContext(prevContext);
      }
      getLogger().logrb(Level.CONFIG, CLASS_NAME.getName(), "destroy", LOG_RESOURCE_BUNDLE,
              "UIMA_analysis_engine_destroyed__CONFIG", getMetaData().getName());
    }
    super.destroy();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.AnalysisEngine#setResultSpecification(org.apache.uima.analysis_engine.ResultSpecification)
   */
  public void setResultSpecification(ResultSpecification aResultSpec) {
    if (aResultSpec == null) {
      resetResultSpecificationToDefault();
    } else {
      mCurrentResultSpecification = aResultSpec;
      mResultSpecChanged = true;
    }
  }

  /**
   * @see AnalysisEngine#processAndOutputNewCASes(CAS)
   */
  public CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException {
    enterProcess();
    try {
      // make initial call to the AnalysisComponent
      callAnalysisComponentProcess(aCAS);

      // return a CasIterator that allows caller to step through the outputs
      // of this AnalysisComponent (if any)
      return new AnalysisComponentCasIterator(mAnalysisComponent, aCAS);
    } finally {
      exitProcess();
    }
  }

  public void batchProcessComplete() throws AnalysisEngineProcessException {
    enterBatchProcessComplete();
    UimaContext prevContext = UimaContextHolder.setContext(getUimaContext());  // for use by POJOs
    try {
      getAnalysisComponent().batchProcessComplete();
    } finally {
      UimaContextHolder.setContext(prevContext);
      exitBatchProcessComplete();
    }
  }

  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    enterCollectionProcessComplete();
    UimaContext prevContext = UimaContextHolder.setContext(getUimaContext());  // for use by POJOs
    try {
      getAnalysisComponent().collectionProcessComplete();
    } finally {
      UimaContextHolder.setContext(prevContext);
      exitCollectionProcessComplete();
    }
  }

  /**
   * Calls the Analysis Component's process method.
   * 
   * @param aCAS
   *          CAS to be processed by annotator
   * @throws AnalysisEngineProcessException -         
   */
  protected void callAnalysisComponentProcess(final CAS aCAS) throws AnalysisEngineProcessException {
    // logging and instrumentation
    String resourceName = getMetaData().getName();
    Logger logger = getLogger();
    logger.logrb(Level.FINE, CLASS_NAME.getName(), "process", LOG_RESOURCE_BUNDLE,
            "UIMA_analysis_engine_process_begin__FINE", resourceName);
    try {
      CAS view = null;
      UimaContext prevContext = UimaContextHolder.setContext(getUimaContext());  // for use by POJOs
      // call Annotator's process method
      try {

        // Get the right view of the CAS. Sofa-aware components get the base CAS.
        // Sofa-unaware components get whatever is mapped to the _InitialView.
        //    note: if initial view is mapped, and the mapped view doesn't exist, throws an exception.
        view = Util.getStartingView(aCAS, mSofaAware, getUimaContextAdmin().getComponentInfo());
        // now get the right interface(e.g. CAS or JCAS)
        // must precede the switchClassLoader call below UIMA-2211
        Class<? extends AbstractCas> requiredInterface = mAnalysisComponent.getRequiredCasInterface();
        AbstractCas casToPass = getCasManager().getCasInterface(view, requiredInterface);

        // check if there was a change in the ResultSpecification or in
        // the TypeSystem. If so, set the changed type system into the ResultSpecification and
        // inform the component
        if (mResultSpecChanged || mLastTypeSystem != view.getTypeSystem()) {
          if (mLastTypeSystem != view.getTypeSystem()) {
            mLastTypeSystem = view.getTypeSystem();
            mCurrentResultSpecification.setTypeSystem(mLastTypeSystem);
            rsFromOutputCapabilities = new ResultSpecification_impl(mLastTypeSystem);
            rsFromOutputCapabilities.addCapabilities(this.getAnalysisEngineMetaData().getCapabilities());
          }
          // the actual ResultSpec we send to the component is formed by
          // looking at this primitive AE's declared output types and eliminating
          // any that are not in mCurrentResultSpecification.
          ResultSpecification analysisComponentResultSpec = 
            ((ResultSpecification_impl)mCurrentResultSpecification).intersect((ResultSpecification_impl)rsFromOutputCapabilities);
          mAnalysisComponent.setResultSpecification(analysisComponentResultSpec);
          mResultSpecChanged = false;
        }
       
        // insure view is passed to switch / restore class loader https://issues.apache.org/jira/browse/UIMA-2211
        ((CASImpl)view).switchClassLoaderLockCasCL(this.getResourceManager().getExtensionClassLoader());

        // call the process method
        mAnalysisComponent.process(casToPass);
        getMBean().incrementCASesProcessed();
        
        //note we do not clear the CAS's currentComponentInfo at this time
        // nor do we unlock the cas and switch it back (class loader-wise).  The AnalysisComponents still
        //can access the CAS until such time as its hasNext method returns false.  Thus is is the
        //AnalysisComponentCasIterator that knows when it is time to clear the currentComponentInfo.
      } catch (Exception e) {
        // catching Throwable to catch out-of-memory errors too, which are not Exceptions
        if (null != view) {
          ((CASImpl)view).restoreClassLoaderUnlockCas();
        }
        if (e instanceof AnalysisEngineProcessException) {
          throw (AnalysisEngineProcessException) e;
        } else {
          throw new AnalysisEngineProcessException(
                  AnalysisEngineProcessException.ANNOTATOR_EXCEPTION, null, e);
        }
      } catch (Error e) {  // out of memory error, for instance
        if (null != view) {
          ((CASImpl)view).restoreClassLoaderUnlockCas();
        }
        throw e;
      } finally {
        aCAS.setCurrentComponentInfo(null); // https://issues.apache.org/jira/browse/UIMA-5097
        UimaContextHolder.setContext(prevContext);
      }

      // log end of event
      logger.logrb(Level.FINE, CLASS_NAME.getName(), "process", LOG_RESOURCE_BUNDLE,
              "UIMA_analysis_engine_process_end__FINE", resourceName);
    } catch (Exception e) {
      // log and rethrow exception
      logger.log(Level.SEVERE, "", e);
      if (e instanceof AnalysisEngineProcessException)
        throw (AnalysisEngineProcessException) e;
      else
        throw new AnalysisEngineProcessException(e);
    }
  }

//  /**
//   * Creates the ResultSpecification to be passed to the AnalysisComponent. This is derived from the
//   * ResultSpec that is input to this AE (via its setResultSpecification method) by intersecting
//   * with the declared outputs of this AE, so that we never ask an AnalysisComponent to produce a
//   * result type that it does not declare in its outputs.
//   * 
//   * For each type or feature, the intersection includes intersecting the languages:
//   *   if either has x-unspecified, then the intersection is the languages of the other side.
//   *   else do a bit-intersection of the languages (this will produce too few results)
//   *     and then iterate over the smaller of the two sources:
//   *       for each non-base lang, if not in the other source already, see if the base lang
//   *       is in the other source, and if so, and the non-base lang.
//   * 
//   * @param currentResultSpecification
//   *          the result spec passed to this AE's setResultSpecification method
//   * @param capabilities
//   *          the capabilities of this AE
//   * 
//   * @return a ResultSpecifciation to pass to the AnalysisComponent
//   */
//  protected ResultSpecification computeAnalysisComponentResultSpec(
//          ResultSpecification inputResultSpec, Capability[] capabilities) {
//    ResultSpecification newResultSpec = new ResultSpecification_impl(inputResultSpec.getTypeSystem());
//    List<String> languagesToAdd = new ArrayList<String>();
// 
//    for (Capability capability : capabilities) {
//      TypeOrFeature[] outputs = capability.getOutputs();
//      String[] languages = capability.getLanguagesSupported();
//      if (null == languages || languages.length == 0) {
//        languages = X_UNSPECIFIED;
//      }
//      
//      for (TypeOrFeature tof : outputs) {
//        String tofName = tof.getName();
//        languagesToAdd.clear();
//        for (String language : languages) {
//          if ((tof.isType() && inputResultSpec.containsType(tofName, language)) ||
//              (!tof.isType() && inputResultSpec.containsFeature(tofName, language))) {
//            languagesToAdd.add(language);
//          }
//        }
//        if (0 < languagesToAdd.size()) {
//          if (tof.isType()) {
//            newResultSpec.addResultType(tofName, tof.isAllAnnotatorFeatures(), 
//                languagesToAdd.toArray(EMPTY_STRING_ARRAY));
//          } else {
//            newResultSpec.addResultFeature(tofName, languagesToAdd.toArray(EMPTY_STRING_ARRAY));
//          }  
//        }
//      }
//    }
//    return newResultSpec;    
//  }
    
//    for (int i = 0; i < capabilities.length; i++) {
//      Capability cap = capabilities[i];
//      TypeOrFeature[] outputs = cap.getOutputs();
//      String[] languages = cap.getLanguagesSupported();
//      if (languages.length == 0) {
//        languages = X_UNSPECIFIED;
//      }
//      for (int j = 0; j < outputs.length; j++) {
//        for (int k = 0; k < languages.length; k++) {
//          if (outputs[j].isType()
//                  && inputResultSpec.containsType(outputs[j].getName(), languages[k])) {
//            newResultSpec.addResultType(outputs[j].getName(), outputs[j].isAllAnnotatorFeatures(),
//                    new String[] { languages[k] });
//          } else if (!outputs[j].isType()
//                  && inputResultSpec.containsFeature(outputs[j].getName(), languages[k])) {
//            newResultSpec.addResultFeature(outputs[j].getName(), new String[] { languages[k] });
//          }
//        }
//      }
//    }
//    return newResultSpec;
//  }

  /**
   * Calls the Analysis Component's next() method.
   * 
   * @return CAS returned by the analysis component
   * @throws AnalysisEngineProcessException -
   * @throws ResultNotSupportedException -
   */
  protected CAS callAnalysisComponentNext() throws AnalysisEngineProcessException,
          ResultNotSupportedException {
    UimaContext prevContext = UimaContextHolder.setContext(getUimaContext());  // for use by POJOs
    try {
      AbstractCas absCas = mAnalysisComponent.next();
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

      // clear the CAS's component info, since it is no longer
      // being processed by this AnalysisComponent
      casToReturn.setCurrentComponentInfo(null);
      ((CASImpl)casToReturn).restoreClassLoaderUnlockCas();
      return casToReturn;
    } catch (Exception e) {
      // log and rethrow exception
      getLogger().log(Level.SEVERE, "", e);
      if (e instanceof AnalysisEngineProcessException)
        throw (AnalysisEngineProcessException) e;
      else
        throw new AnalysisEngineProcessException(e);
    } finally {
      UimaContextHolder.setContext(prevContext);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.AnalysisEngine#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    // do base resource reconfiguration
    super.reconfigure();

    // inform the annotator
    UimaContext prevContext = UimaContextHolder.setContext(getUimaContext());  // for use by POJOs
    try {
      mAnalysisComponent.reconfigure();
    } catch (ResourceInitializationException e) {
      throw new ResourceConfigurationException(e);
    } finally {
      UimaContextHolder.setContext(prevContext);
    }
  }

  protected AnalysisComponent getAnalysisComponent() {
    return mAnalysisComponent;
  }

  /**
   * Implements the iterator that steps through all outputs from an AnalysisComponent.
   */
  class AnalysisComponentCasIterator implements CasIterator {
    private AnalysisComponent mMyAnalysisComponent;
    private CAS mInputCas;
    private boolean casAvailable;

    AnalysisComponentCasIterator(AnalysisComponent aAnalysisComponent, CAS aInputCas) {
      mMyAnalysisComponent = aAnalysisComponent;
      mInputCas = aInputCas;
      casAvailable = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.core.CasIterator#hasNext()
     */
    public boolean hasNext() throws AnalysisEngineProcessException {
      enterProcess();
      if (casAvailable) {
        return true;
      }
      UimaContext prevContext = UimaContextHolder.setContext(getUimaContext());  // for use by POJOs
      try {
        casAvailable = mMyAnalysisComponent.hasNext();
        if (!casAvailable) {
          //when hasNext returns false, by contract the AnalysisComponent is done processing its
          //input CAS.  Now is the time to clear the currentComponentInfo to indicate that the
          //CAS is no longer being processed.
          mInputCas.setCurrentComponentInfo(null);
          ((CASImpl)mInputCas).restoreClassLoaderUnlockCas();
        }
        return casAvailable;
      } catch (Exception e) {
        ((CASImpl)mInputCas).restoreClassLoaderUnlockCas();
        if (e instanceof AnalysisEngineProcessException) {
          throw (AnalysisEngineProcessException) e;
        }
        throw new AnalysisEngineProcessException(e);
      }
 
      finally {
        UimaContextHolder.setContext(prevContext);
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
        // Make sure that the AnalysisComponent has a next CAS to return
        // Use the saved value so hasNext not called twice before next
        if (!casAvailable) {
          throw new UIMA_IllegalStateException(UIMA_IllegalStateException.NO_NEXT_CAS,
                  new Object[0]);
        }
        casAvailable = false;
        // call AnalysisComponent.next method to populate CAS
        try {
          CAS cas = callAnalysisComponentNext();
          // cas.setParentID(mOriginalCas.getID());
          return cas;
        } catch (Exception e) {
          ((CASImpl)mInputCas).restoreClassLoaderUnlockCas();
          
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
