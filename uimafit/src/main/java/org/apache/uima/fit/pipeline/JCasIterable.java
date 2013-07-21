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

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.LifeCycleUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

/**
 * A class implementing iteration over a the documents of a collection. Each element in the Iterable
 * is a JCas containing a single document. The documents have been loaded by the CollectionReader
 * and processed by the AnalysisEngine (if any).
 * 
 */
public class JCasIterable implements Iterator<JCas>, Iterable<JCas> {

  private CollectionReader collectionReader;

  private AnalysisEngine[] analysisEngines;

  private JCas jCas;
  
  private boolean selfComplete = false;
  
  private boolean selfDestroy = false;

  /**
   * Iterate over the documents loaded by the CollectionReader. (Uses an JCasAnnotatorAdapter to
   * create the document JCas.) By default, components get no lifecycle events, such as 
   * collectionProcessComplete or destroy when this constructor is used.
   * 
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aTypeSystemDescription
   *          a type system description
   */
  public JCasIterable(final CollectionReader aReader,
          final TypeSystemDescription aTypeSystemDescription) throws UIMAException, IOException {
    this(aReader, AnalysisEngineFactory.createEngine(NoOpAnnotator.class, aTypeSystemDescription));
  }

  /**
   * Iterate over the documents loaded by the CollectionReader, running the AnalysisEngine on each
   * one before yielding them. By default, components get no lifecycle events, such as 
   * collectionProcessComplete or destroy when this constructor is used.
   * 
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aEngines
   *          The AnalysisEngines for processing documents.
   */
  public JCasIterable(final CollectionReader aReader, final AnalysisEngine... aEngines)
          throws UIMAException, IOException {
    collectionReader = aReader;
    analysisEngines = aEngines;
    jCas = CasCreationUtils.createCas(collectMetaData()).getJCas();
  }

  /**
   * Iterate over the documents loaded by the CollectionReader, running the AnalysisEngine on each
   * one before yielding them. When created with this constructor, analysis engines by default
   * receive a collectionProcessComplete call when all documents have been read from the reader
   * and all components get destroyed.
   * 
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aEngines
   *          The AnalysisEngines for processing documents.
   */
  public JCasIterable(final CollectionReaderDescription aReader,
          final AnalysisEngineDescription... aEngines) throws UIMAException, IOException {
    setSelfComplete(true);
    setSelfDestroy(true);
    collectionReader = createReader(aReader);
    analysisEngines = new AnalysisEngine[] { createEngine(createEngineDescription(aEngines)) };
    jCas = CasCreationUtils.createCas(collectMetaData()).getJCas();
  }
  
  private List<ResourceMetaData> collectMetaData() {
    final List<ResourceMetaData> metaData = new ArrayList<ResourceMetaData>();
    metaData.add(collectionReader.getMetaData());
    for (AnalysisEngine engine : analysisEngines) {
      metaData.add(engine.getMetaData());
    }
    return metaData;
  }

  public Iterator<JCas> iterator() {
    return this;
  }

  public boolean hasNext() {
    try {
      return collectionReader.hasNext();
    } catch (CollectionException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    finally {
      if (selfDestroy) {
        destroy();
      }
    }
  }

  public JCas next() {
    jCas.reset();
    boolean error = true;
    boolean destroyed = false;
    try {
      collectionReader.getNext(jCas.getCas());
      for (AnalysisEngine engine : analysisEngines) {
        engine.process(jCas);
      }

      if (!hasNext() && selfComplete) {
        collectionProcessComplete();
      }

      if (!hasNext() && selfDestroy) {
        destroy();
        destroyed = true;
      }

      error = false;
    } catch (CollectionException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (AnalysisEngineProcessException e) {
      throw new IllegalStateException(e);
    }
    finally {
      if (error && selfDestroy && !destroyed) {
        destroy();
      }
    }
    
    return jCas;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
  
  public void collectionProcessComplete() throws AnalysisEngineProcessException
  {
    LifeCycleUtil.collectionProcessComplete(analysisEngines);
  }
  
  public void destroy() {
    LifeCycleUtil.close(collectionReader);
    LifeCycleUtil.destroy(collectionReader);
    LifeCycleUtil.destroy(analysisEngines);
  }

  public boolean isSelfComplete() {
    return selfComplete;
  }

  /**
   * Send a collectionProcessComplete call to analysis engines when the reader has no further
   * CASes to produce.
   */
  public void setSelfComplete(boolean aSelfComplete) {
    selfComplete = aSelfComplete;
  }

  public boolean isSelfDestroy() {
    return selfDestroy;
  }

  /**
   * Send a destroy call to analysis engines when the reader has no further CASes to produce or
   * if an error occurs.
   */
  public void setSelfDestroy(boolean aSelfDestroy) {
    selfDestroy = aSelfDestroy;
  }
}
