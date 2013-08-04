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

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;

/**
 * A class implementing iteration over a the documents of a collection. Each element in the Iterable
 * is a JCas containing a single document. The documents have been loaded by the CollectionReader
 * and processed by the AnalysisEngine (if any).
 * 
 */
public class JCasIterable implements Iterable<JCas> {

  private final CollectionReaderDescription reader;

  private final AnalysisEngineDescription[] engines;

  /**
   * Iterate over the documents loaded by the CollectionReader, running the AnalysisEngine on each
   * one before yielding them. When created with this constructor, analysis engines by default
   * receive a collectionProcessComplete call when all documents have been read from the reader and
   * all components get destroyed.
   * 
   * @param aReader
   *          The CollectionReader for loading documents.
   * @param aEngines
   *          The AnalysisEngines for processing documents.
   */
  public JCasIterable(final CollectionReaderDescription aReader,
          final AnalysisEngineDescription... aEngines) {
    reader = aReader;
    engines = aEngines;
  }

  public JCasIterator iterator() {
    try {
      JCasIterator i = new JCasIterator(createReader(reader),
              createEngine(createEngineDescription(engines)));
      i.setSelfComplete(true);
      i.setSelfDestroy(true);
      return i;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (UIMAException e) {
      throw new IllegalStateException(e);
    }
  }
}
