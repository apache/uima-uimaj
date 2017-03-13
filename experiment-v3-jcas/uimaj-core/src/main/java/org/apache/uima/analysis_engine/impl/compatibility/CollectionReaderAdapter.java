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

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Adapter that allows CollectionReaders to implement the AnalysisComponent interface.
 */
public class CollectionReaderAdapter implements AnalysisComponent {
  private CollectionReader mCollectionReader;

  private TypeSystem mLastTypeSystem;

  private UimaContext mUimaContext;

  private boolean mSofaAware;
  
  private boolean mProcessCalled;

  /**
   * Create a new annotator adapter.
   * 
   * @param aCollectionReader
   *          the CollectionReader instance
   * @param aMetaData
   *          metadata for the annotator. Needed to compute ResultSpecification.
   */
  public CollectionReaderAdapter(CollectionReader aCollectionReader,
          AnalysisEngineMetaData aMetaData) {
    mCollectionReader = aCollectionReader;
    mSofaAware = aMetaData.isSofaAware();
    mProcessCalled = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#initialize(org.apache.uima.UimaContext)
   */
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    // Initialize the CollectionReader, passing the appropriate UimaContext
    // We pass an empty descriptor to satisfy the Collection Reader's initialize
    // method; we don't want to do any additional set-up of resources or
    // config params, that's all handled in the initialization of the enclosing
    // Primitive AnalysisEngine.
    AnalysisEngineDescription_impl desc = new AnalysisEngineDescription_impl();

    Map<String, Object> paramsMap = new HashMap<String, Object>();
    paramsMap.put(Resource.PARAM_UIMA_CONTEXT, aContext);
    mCollectionReader.initialize(desc, paramsMap);
    mUimaContext = aContext;
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
        mCollectionReader.typeSystemInit(typeSystem);
        mLastTypeSystem = typeSystem;
      }
    } catch (ResourceInitializationException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.annotator.Annotator#process(org.apache.uima.core.AbstractCas)
   */
  public void process(AbstractCas aCAS) throws AnalysisEngineProcessException {
    // Does nothing on the first call to process - CollectionReaders ignore their input CAS.
    // On a subsequent call to process, we want to reset the CollectionReader, which we
    // try to do by calling its reconfigure method.
    if (mProcessCalled) {
      try {
        reconfigure();
      } catch (ResourceInitializationException e) {
        throw new AnalysisEngineProcessException(e);
      } catch (ResourceConfigurationException e) {
        throw new AnalysisEngineProcessException(e);
      }
    }
    else {
      mProcessCalled = true;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#batchProcessComplete()
   */
  public void batchProcessComplete() throws AnalysisEngineProcessException {
    // CollectionReaders don't implement batchProcessComplete
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#collectionProcessComplete()
   */
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    // CollectionReaders don't implement collectionProcessComplete
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#destroy()
   */
  public void destroy() {
    mCollectionReader.destroy();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.core.AnalysisComponent#reconfigure()
   */
  public void reconfigure() throws ResourceInitializationException, ResourceConfigurationException {
    mCollectionReader.reconfigure();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    try {
      return mCollectionReader.hasNext();
    } catch (CollectionException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {
    // get a new CAS
    CAS cas = mUimaContext.getEmptyCas(CAS.class);

    // check if type system changed; if so, notify CollectionReader
    checkTypeSystemChange(cas);

    // Get the right view of the CAS. Sofa-aware components get the base CAS.
    // Sofa-unaware components get whatever is mapped to the default text sofa.
    CAS view = ((CASImpl) cas).getBaseCAS();
    if (!mSofaAware) {
      view = cas.getView(CAS.NAME_DEFAULT_SOFA);
    }

    try {
      mCollectionReader.getNext(view);
    } catch (CollectionException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
    return cas;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next(org.apache.uima.core.AbstractCas)
   */
  public void next(AbstractCas aEmptyCas) throws AnalysisEngineProcessException {
    if (!CAS.class.isAssignableFrom(aEmptyCas.getClass())) {
      throw new AnalysisEngineProcessException(
              AnalysisEngineProcessException.INCORRECT_CAS_INTERFACE, new Object[] { CAS.class,
                  aEmptyCas.getClass() });
    }

  }

  /**
   * Get the CAS interface required by this annotator.
   * 
   * @return the CAS interface required by this annotator
   */
  public Class<? extends AbstractCas> getRequiredCasInterface() {
    // CollectionReaders don't use the input CAS, so they don't
    // care what CAS interface they receive
    return AbstractCas.class;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#getCasInstancesRequired()
   */
  public int getCasInstancesRequired() {
    return 1;
  }

  public void setResultSpecification(ResultSpecification aResultSpec) {
    // Collection Readers
  }
}
