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

package org.apache.uima.analysis_engine.impl.compatibility;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.BaseAnnotator;
import org.apache.uima.analysis_engine.annotator.GenericAnnotator;
import org.apache.uima.analysis_engine.annotator.JTextAnnotator;
import org.apache.uima.analysis_engine.annotator.TextAnnotator;
import org.apache.uima.analysis_engine.impl.AnnotatorContext_impl;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Adapter that allows Annotators to implement the AnalysisComponent interface.
 */
public class AnnotatorAdapter implements AnalysisComponent {
  private BaseAnnotator mAnnotator;

  private Class<? extends AbstractCas> mCasInterface;

  private TypeSystem mLastTypeSystem;

  private ResultSpecification mDefaultResultSpecification;

  private Map<String, ResultSpecification> mLanguageToResultSpecMap = new HashMap<String, ResultSpecification>();

  /**
   * Create a new annotator adapter.
   * 
   * @param aAnnotator
   *          the annotator instance
   * @param aMetaData
   *          metadata for the annotator. Needed to compute ResultSpecification.
   * @param aAdditionalParams
   *          parameters passed to AE's initialize method. Used to allow containing Aggregate to
   *          influence ResultSpecification, for backwards compatibility with CapabilityLanguageFlow.
   * @throws ResourceInitializationException if the component is sofa-aware
   */
  public AnnotatorAdapter(BaseAnnotator aAnnotator, AnalysisEngineMetaData aMetaData,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    mAnnotator = aAnnotator;

    // check for the invalid case where a TextAnnotator or JTextAnnotator
    // declares sofa input/output capabilities. Text annotators should not be
    // "sofa-aware".
    if (aMetaData.isSofaAware()
            && (mAnnotator instanceof TextAnnotator || mAnnotator instanceof JTextAnnotator)) {
      throw new ResourceInitializationException(
              ResourceInitializationException.TEXT_ANNOTATOR_CANNOT_BE_SOFA_AWARE, new Object[] {
                  aMetaData.getName(),
                  (mAnnotator instanceof TextAnnotator) ? "TextAnnotator" : "JTextAnnotator",
                  aMetaData.getSourceUrlString() });
    }

    // determine which CAS interface this Annotator needs
    if (mAnnotator instanceof JTextAnnotator) {
      mCasInterface = JCas.class;
    } else {
      mCasInterface = CAS.class;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#initialize(org.apache.uima.UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    try {
      // wrap UimaContext in AnnotatorContext
      AnnotatorContext actxt = new AnnotatorContext_impl((UimaContextAdmin) aContext);
      mAnnotator.initialize(actxt);
    } catch (AnnotatorInitializationException e) {
      throw new ResourceInitializationException(e);
    } catch (AnnotatorConfigurationException e) {
      throw new ResourceInitializationException(e);
    }
  }

  public void setResultSpecification(ResultSpecification aResultSpec) {
    mDefaultResultSpecification = aResultSpec;
    mLanguageToResultSpecMap = new HashMap<String, ResultSpecification>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.annotator.Annotator#process(org.apache.uima.core.AbstractCas)
   */
  public void process(AbstractCas aCAS) throws AnalysisEngineProcessException {
    if (!mCasInterface.isAssignableFrom(aCAS.getClass())) {
      throw new AnalysisEngineProcessException(
              AnalysisEngineProcessException.INCORRECT_CAS_INTERFACE, new Object[] { mCasInterface,
                  aCAS.getClass() });
    }

    // check if type system changed; if so, notify Annotator
    checkTypeSystemChange(aCAS);

    // do proper typecasts and call process method
    try {
      if (mAnnotator instanceof TextAnnotator) {
        CAS cas = (CAS) aCAS;
        ResultSpecification rs = getResultSpecForLanguage(cas.getDocumentLanguage());
        rs.setTypeSystem(cas.getTypeSystem());
        ((TextAnnotator) mAnnotator).process(cas, rs);
      } else if (mAnnotator instanceof JTextAnnotator) {
        JCas jcas = (JCas) aCAS;
        ResultSpecification rs = getResultSpecForLanguage(jcas.getDocumentLanguage());
        rs.setTypeSystem(jcas.getTypeSystem());
        ((JTextAnnotator) mAnnotator).process(jcas, rs);
      } else if (mAnnotator instanceof GenericAnnotator) {
        mDefaultResultSpecification.setTypeSystem(((CAS) aCAS).getTypeSystem());
        ((GenericAnnotator) mAnnotator).process((CAS) aCAS, mDefaultResultSpecification);
      }
    } catch (AnnotatorProcessException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /**
   * @param language
   * @return the ResultSpecification for the language
   */
  private ResultSpecification getResultSpecForLanguage(String language) {
    // we cache this since it is called for each document
    ResultSpecification rs = mLanguageToResultSpecMap.get(language);
    if (rs == null) {
      TypeOrFeature[] tofs = mDefaultResultSpecification.getResultTypesAndFeatures(language);
      if (tofs.length > 0) {
        rs = UIMAFramework.getResourceSpecifierFactory().createResultSpecification();
        rs.setResultTypesAndFeatures(tofs);
      } else {
        // special case: if annotator lists no outputs for this language, call it
        // with the actual result spec, set up by language.
     
        // An earlier version of this comment erroneously asserted
        // "call for all possible outputs.
        // This is mainly for backwards compatibility,
        // but here's a rationalization: the FlowController wants us to invoke the
        // annotator, so calling it with no outputs doesn't really make sense."
        rs = mDefaultResultSpecification;
      }
      mLanguageToResultSpecMap.put(language, rs);
    }
    return rs;
  }

  /**
   * Checks it the type system of the given CAS is different from the last type system this
   * component was operating on. If it is different, calls the typeSystemInit method on the
   * component.
   * @param aCAS -
   * @throws AnalysisEngineProcessException - 
   */
  public void checkTypeSystemChange(AbstractCas aCAS) throws AnalysisEngineProcessException {
    try {
      TypeSystem typeSystem;
      if (aCAS instanceof JCas) {
        typeSystem = ((JCas) aCAS).getTypeSystem();
      } else // CAS 
      {
        typeSystem = ((CAS) aCAS).getTypeSystem();
      }
      if (typeSystem != mLastTypeSystem) {
        mAnnotator.typeSystemInit(typeSystem);
        mLastTypeSystem = typeSystem;
      }
    } catch (AnnotatorConfigurationException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (AnnotatorInitializationException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#batchProcessComplete()
   */
  public void batchProcessComplete() throws AnalysisEngineProcessException {
    // v1.x annotators cannot implement batchProcessComplete
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#collectionProcessComplete()
   */
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    // v1.x annotators cannot implement collectionProcessComplete
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#destroy()
   */
  public void destroy() {
    mAnnotator.destroy();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException, ResourceInitializationException {
    try {
      mAnnotator.reconfigure();
    } catch (AnnotatorConfigurationException e) {
      throw new ResourceConfigurationException(e);
    } catch (AnnotatorInitializationException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {
    throw new UIMA_UnsupportedOperationException(
            UIMA_UnsupportedOperationException.UNSUPPORTED_METHOD, new Object[] {
                AnnotatorAdapter.class, "next" });
  }

  /**
   * Get the CAS interface required by this annotator.
   * 
   * @return the CAS interface required by this annotator
   */
  public Class<? extends AbstractCas> getRequiredCasInterface() {
    return mCasInterface;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#getCasInstancesRequired()
   * @return the number of instances required
   */
  public int getCasInstancesRequired() {
    return 0;
  }

  protected BaseAnnotator getAnnotator() {
    return mAnnotator;
  }
}
