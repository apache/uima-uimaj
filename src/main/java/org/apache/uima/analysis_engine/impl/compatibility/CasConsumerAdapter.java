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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Adapter that allows CasConsumers to implement the AnalysisComponent interface.
 */
public class CasConsumerAdapter implements AnalysisComponent {
  private CasConsumer mCasConsumer;

  private TypeSystem mLastTypeSystem;

  private AnalysisEngineMetaData mMetaData;

  /**
   * Create a new annotator adapter.
   * 
   * @param aCasConsumer
   *          the CasConsumer instance
   * @param aMetaData
   *          metadata for the annotator. Needed to compute ResultSpecification.
   */
  public CasConsumerAdapter(CasConsumer aCasConsumer, AnalysisEngineMetaData aMetaData) {
    mCasConsumer = aCasConsumer;
    mMetaData = aMetaData;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#initialize(org.apache.uima.UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    // Initialize the CasConsumer, passing the appropriate UimaContext
    // and a dummy descriptor containing the metadata passed to our constructor
    AnalysisEngineDescription_impl desc = new AnalysisEngineDescription_impl();
    desc.setMetaData(mMetaData);

    Map<String, Object> paramsMap = new HashMap<String, Object>();
    paramsMap.put(Resource.PARAM_UIMA_CONTEXT, aContext);
    mCasConsumer.initialize(desc, paramsMap);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.annotator.Annotator#process(org.apache.uima.core.AbstractCas)
   */
  public void process(AbstractCas aCAS) throws AnalysisEngineProcessException {
    if (!CAS.class.isAssignableFrom(aCAS.getClass())) {
      throw new AnalysisEngineProcessException(
              AnalysisEngineProcessException.INCORRECT_CAS_INTERFACE, new Object[] { CAS.class,
                  aCAS.getClass() });
    }

    // check if type system changed; if so, notify Annotator
    checkTypeSystemChange(aCAS);

    try {
      mCasConsumer.processCas((CAS) aCAS);
    } catch (ResourceProcessException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#typeSystemChanged(org.apache.uima.core.AbstractCas)
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
        mCasConsumer.typeSystemInit(typeSystem);
        mLastTypeSystem = typeSystem;
      }
    } catch (ResourceInitializationException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#batchProcessComplete()
   */
  public void batchProcessComplete() throws AnalysisEngineProcessException {
    try {
      mCasConsumer.batchProcessComplete(new ProcessTrace_impl());
    } catch (ResourceProcessException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#collectionProcessComplete()
   */
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    try {
      mCasConsumer.collectionProcessComplete(new ProcessTrace_impl());
    } catch (ResourceProcessException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#destroy()
   */
  public void destroy() {
    mCasConsumer.destroy();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException, ResourceInitializationException {
    mCasConsumer.reconfigure();
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
   * Get the CAS interface required by this CasConsumer. Currently always returns CAS.class.
   * 
   * @return the CAS interface required by this CasConsumer
   */
  public Class<CAS> getRequiredCasInterface() {
    return CAS.class;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#getCasInstancesRequired()
   */
  public int getCasInstancesRequired() {
    return 0;
  }

  public void setResultSpecification(ResultSpecification aResultSpec) {
    // CAS Consumers don't use Result Specifications
  }
}
