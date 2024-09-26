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
package org.apache.uima.fit.pipeline;

import static org.apache.uima.UIMAFramework.produceAnalysisEngine;
import static org.apache.uima.UIMAFramework.produceCollectionReader;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.internal.ResourceManagerFactory.newResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.fit.util.LifeCycleUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

/**
 * A class implementing iteration over a the documents of a collection. Each element in the Iterable
 * is a JCas containing a single document. The documents have been loaded by the CollectionReader
 * and processed by the AnalysisEngine (if any).
 */
public class JCasIterator implements Iterator<JCas> {

  private final CollectionReader collectionReader;

  private final AnalysisEngine[] analysisEngines;

  private final JCas jCas;

  private final ResourceManager resMgr;

  private boolean selfComplete = false;

  private boolean selfDestroy = false;

  private boolean destroyed = false;

  private boolean resourceManagerCreatedInternally = false;

  /**
   * Iterate over the documents loaded by the given reader, running the analysis engines on each one
   * before yielding them. By default, components <b>DO get</b> life-cycle events, such as
   * collectionProcessComplete or destroy when this constructor is used.
   * 
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aEngines
   *          The AnalysisEngines for processing documents.
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization of the components
   * @throws CASException
   *           if the JCas could not be initialized
   */
  public JCasIterator(final CollectionReaderDescription aReader,
          final AnalysisEngineDescription... aEngines)
          throws CASException, ResourceInitializationException {
    this(null, aReader, aEngines);
  }

  /**
   * Iterate over the documents loaded by the given reader, running the analysis engines on each one
   * before yielding them. By default, components <b>DO get</b> life-cycle events, such as
   * collectionProcessComplete or destroy when this constructor is used.
   * 
   * @param aResMgr
   *          The {@link ResourceManager} used to create the components and the JCas. If this
   *          parameter is {@code null} then {@link ResourceManagerFactory#newResourceManager()}
   *          will be used to obtain a resource manager. If a new resource manager was internally
   *          created, it is destroyed at the end of the pipeline (if {@link #isSelfDestroy()}).
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aEngines
   *          The AnalysisEngines for processing documents.
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization of the components
   * @throws CASException
   *           if the JCas could not be initialized
   */
  public JCasIterator(final ResourceManager aResMgr, final CollectionReaderDescription aReader,
          final AnalysisEngineDescription... aEngines)
          throws CASException, ResourceInitializationException {
    selfComplete = true;
    selfDestroy = true;

    if (aResMgr == null) {
      resMgr = newResourceManager();
      resourceManagerCreatedInternally = true;
    } else {
      resMgr = aResMgr;
      resourceManagerCreatedInternally = false;
    }

    collectionReader = produceCollectionReader(aReader, resMgr, null);

    analysisEngines = new AnalysisEngine[] {
        produceAnalysisEngine(createEngineDescription(aEngines), resMgr, null) };

    jCas = createCas(resMgr, collectionReader, analysisEngines);
    collectionReader.typeSystemInit(jCas.getTypeSystem());
  }

  /**
   * Iterate over the documents loaded by the CollectionReader, running the AnalysisEngine on each
   * one before yielding them. By default, components <b>do NOT get</b> life-cycle events, such as
   * collectionProcessComplete or destroy when this constructor is used.
   * 
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aEngines
   *          The AnalysisEngines for processing documents.
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization of the components
   * @throws CASException
   *           if the JCas could not be initialized
   */
  public JCasIterator(final CollectionReader aReader, final AnalysisEngine... aEngines)
          throws CASException, ResourceInitializationException {
    this(ResourceManagerFactory.newResourceManager(), aReader, aEngines);
    resourceManagerCreatedInternally = true;
  }

  /**
   * Iterate over the documents loaded by the CollectionReader. (Uses an JCasAnnotatorAdapter to
   * create the document JCas.) By default, components <b>do NOT get</b> life-cycle events, such as
   * collectionProcessComplete or destroy when this constructor is used.
   * 
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aTypeSystemDescription
   *          a type system description
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization of the components
   * @throws CASException
   *           if the JCas could not be initialized
   */
  public JCasIterator(final CollectionReader aReader,
          final TypeSystemDescription aTypeSystemDescription)
          throws CASException, ResourceInitializationException {
    this(aReader, createEngine(NoOpAnnotator.class, aTypeSystemDescription));
  }

  /**
   * Iterate over the documents loaded by the CollectionReader, running the AnalysisEngine on each
   * one before yielding them. By default, components <b>do NOT get</b> life-cycle events, such as
   * collectionProcessComplete or destroy when this constructor is used.
   * 
   * @param aResMgr
   *          The {@link ResourceManager} used to create the JCas. Should be the one also used by
   *          the CollectionReader and the AnalysisEngines. If this parameter is {@code null} then
   *          {@link ResourceManagerFactory#newResourceManager()} will be used to obtain a resource
   *          manager. If a new resource manager was internally created, it is destroyed at the end
   *          of the pipeline (if {@link #isSelfDestroy()}).
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aEngines
   *          The AnalysisEngines for processing documents.
   * @throws ResourceInitializationException
   *           if a failure occurs during initialization of the components
   * @throws CASException
   *           if the JCas could not be initialized
   */
  public JCasIterator(final ResourceManager aResMgr, final CollectionReader aReader,
          final AnalysisEngine... aEngines) throws CASException, ResourceInitializationException {
    if (aResMgr == null) {
      resMgr = newResourceManager();
      resourceManagerCreatedInternally = true;
    } else {
      resMgr = aResMgr;
      resourceManagerCreatedInternally = false;
    }

    collectionReader = aReader;
    analysisEngines = aEngines;

    jCas = createCas(resMgr, collectionReader, analysisEngines);
    collectionReader.typeSystemInit(jCas.getTypeSystem());
  }

  private JCas createCas(final ResourceManager aResMgr, final CollectionReader aReader,
          final AnalysisEngine... aEngines) throws CASException, ResourceInitializationException {
    Collection<MetaDataObject> metaData = new ArrayList<MetaDataObject>();
    metaData.add(aReader.getProcessingResourceMetaData());
    for (AnalysisEngine ae : aEngines) {
      metaData.add(ae.getProcessingResourceMetaData());
    }

    return CasCreationUtils.createCas(metaData, null, aResMgr).getJCas();
  }

  @Override
  public boolean hasNext() {
    if (destroyed) {
      return false;
    }

    boolean error = true;
    try {
      boolean hasOneMore = collectionReader.hasNext();
      error = false;
      return hasOneMore;
    } catch (CollectionException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      if (error && selfDestroy) {
        destroy();
      }
    }
  }

  @Override
  public JCas next() {
    jCas.reset();
    boolean error = true;
    boolean destroyed = false;
    try {
      collectionReader.getNext(jCas.getCas());
      for (AnalysisEngine engine : analysisEngines) {
        engine.process(jCas);
      }

      // Only call hasNext() if auto complete or destroy is enabled.
      if ((selfComplete || selfDestroy) && !hasNext()) {
        if (selfComplete) {
          collectionProcessComplete();
        }

        if (selfDestroy) {
          destroy();
          destroyed = true;
        }
      }

      error = false;
    } catch (CollectionException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (AnalysisEngineProcessException e) {
      throw new IllegalStateException(e);
    } finally {
      if (error && selfDestroy && !destroyed) {
        destroy();
      }
    }

    return jCas;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Notify analysis engines that the collection process is complete.
   * 
   * @throws AnalysisEngineProcessException
   *           if there was a problem completing the process
   */
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    LifeCycleUtil.collectionProcessComplete(analysisEngines);
  }

  /**
   * Close and destroy all components.s
   */
  public void destroy() {
    if (!destroyed) {
      LifeCycleUtil.close(collectionReader);
      LifeCycleUtil.destroy(collectionReader);
      LifeCycleUtil.destroy(analysisEngines);
      if (resourceManagerCreatedInternally) {
        LifeCycleUtil.destroy(resMgr);
      }
      destroyed = true;
    }
  }

  /**
   * Get whether {@link #collectionProcessComplete()} is automatically called.
   * 
   * @return whether {@link #collectionProcessComplete()} is automatically called.
   */
  public boolean isSelfComplete() {
    return selfComplete;
  }

  /**
   * Send a {@link #collectionProcessComplete()} call to analysis engines when the reader has no
   * further CASes to produce.
   * 
   * @param aSelfComplete
   *          whether to enable the automatic call to {@link #collectionProcessComplete()}
   */
  public void setSelfComplete(boolean aSelfComplete) {
    selfComplete = aSelfComplete;
  }

  /**
   * Get whether {@link #destroy()} is automatically called.
   * 
   * @return whether {@link #destroy()} is automatically called.
   */
  public boolean isSelfDestroy() {
    return selfDestroy;
  }

  /**
   * Send a destroy call to analysis engines when the reader has no further CASes to produce or if
   * an error occurs.
   * 
   * @param aSelfDestroy
   *          whether to enable the automatic call to {@link Resource#destroy()}
   */
  public void setSelfDestroy(boolean aSelfDestroy) {
    selfDestroy = aSelfDestroy;
  }
}
