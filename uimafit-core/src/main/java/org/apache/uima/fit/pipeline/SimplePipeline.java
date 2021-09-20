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

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.util.LifeCycleUtil.collectionProcessComplete;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.fit.util.LifeCycleUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.CasCreationUtils;

/**
 *
 */
public final class SimplePipeline {
  private SimplePipeline() {
    // This class is not meant to be instantiated
  }

  /**
   * <p>
   * Run the CollectionReader and AnalysisEngines as a pipeline. After processing all CASes provided
   * by the reader, the method calls the life-cycle methods
   * ({@link AnalysisEngine#collectionProcessComplete() collectionProcessComplete()} on the engines
   * and {@link Resource#destroy() destroy()}) on all engines. Note that the life-cycle methods are
   * <b>NOT</b> called on the reader. As the reader was instantiated by the caller, it must also be
   * managed (i.e. destroyed) the caller.
   * </p>
   * <p>
   * Note that with this method, external resources cannot be shared between the reader and the
   * analysis engines. They can be shared amongst the analysis engines.
   * </p>
   * <p>
   * The CAS is created using the resource manager used by the collection reader.
   * </p>
   * 
   * @param reader
   *          The CollectionReader that loads the documents into the CAS.
   * @param descs
   *          Primitive AnalysisEngineDescriptions that process the CAS, in order. If you have a mix
   *          of primitive and aggregate engines, then please create the AnalysisEngines yourself
   *          and call the other runPipeline method.
   * @throws IOException
   *           if there is an I/O problem in the reader
   * @throws ResourceInitializationException 
   *           if there is a problem initializing or running the pipeline.
   * @throws CollectionException 
   *           if there is a problem initializing or running the pipeline.
   * @throws AnalysisEngineProcessException 
   *           if there is a problem initializing or running the pipeline.
   */
  public static void runPipeline(final CollectionReader reader,
          final AnalysisEngineDescription... descs) throws IOException,
          ResourceInitializationException, AnalysisEngineProcessException, CollectionException {
    AnalysisEngine aae = null;
    try {
      // Create AAE
      final AnalysisEngineDescription aaeDesc = createEngineDescription(descs);
  
      // Instantiate AAE
      aae = createEngine(aaeDesc);
  
      // Create CAS from merged metadata
      final CAS cas = CasCreationUtils.createCas(asList(reader.getMetaData(), aae.getMetaData()), 
              null, reader.getResourceManager());
      reader.typeSystemInit(cas.getTypeSystem());

      // Process
      while (reader.hasNext()) {
        reader.getNext(cas);
        aae.process(cas);
        cas.reset();
      }

      // Signal end of processing
      aae.collectionProcessComplete();
    } finally {
      // Destroy
      LifeCycleUtil.destroy(aae);
    }
  }

  /**
   * <p>
   * Run the CollectionReader and AnalysisEngines as a pipeline. After processing all CASes provided
   * by the reader, the method calls {@link AnalysisEngine#collectionProcessComplete()
   * collectionProcessComplete()} on the engines, {@link CollectionReader#close() close()} on the
   * reader and {@link Resource#destroy() destroy()} on the reader and all engines.
   * </p>
   * <p>
   * External resources can be shared between the reader and the analysis engines.
   * </p>
   * <p>
   * This method is suitable for the batch-processing of sets of documents where the overheaded
   * of instantiating the pipeline components does not significantly impact the overall runtime
   * of the pipeline. If you need to avoid this overhead, e.g. because you wish to run a pipeline
   * on individual documents, then you should not use this method. Instead, create a CAS using
   * {@link JCasFactory}, create a reader instance using {@link CollectionReaderFactory#createReader},
   * create an engine instance using {@link AnalysisEngineFactory#createEngine} and then use
   * a loop to process the data, resetting the CAS after each step.
   * </p>
   * <pre><code>
   *   while (reader.hasNext()) {
   *     reader.getNext(cas);
   *     engine.process(cas);
   *     cas.reset();
   *   }
   * </code></pre>
   * 
   * @param readerDesc
   *          The CollectionReader that loads the documents into the CAS.
   * @param descs
   *          Primitive AnalysisEngineDescriptions that process the CAS, in order. If you have a mix
   *          of primitive and aggregate engines, then please create the AnalysisEngines yourself
   *          and call the other runPipeline method.
   * @throws IOException
   *           if there is an I/O problem in the reader
   * @throws ResourceInitializationException 
   *           if there is a problem initializing or running the pipeline.
   * @throws CollectionException 
   *           if there is a problem initializing or running the pipeline.
   * @throws AnalysisEngineProcessException 
   *           if there is a problem initializing or running the pipeline.
   */
  public static void runPipeline(final CollectionReaderDescription readerDesc,
          final AnalysisEngineDescription... descs) throws IOException,
          ResourceInitializationException, AnalysisEngineProcessException, CollectionException {
    CollectionReader reader = null;
    AnalysisEngine aae = null;
    ResourceManager resMgr = null;
    try {
      resMgr = ResourceManagerFactory.newResourceManager();
      
      // Create the components
      reader = UIMAFramework.produceCollectionReader(readerDesc, resMgr, null);
  
      // Create AAE
      final AnalysisEngineDescription aaeDesc = createEngineDescription(descs);
  
      // Instantiate AAE
      aae = UIMAFramework.produceAnalysisEngine(aaeDesc, resMgr, null);
  
      // Create CAS from merged metadata
      final CAS cas = CasCreationUtils.createCas(asList(reader.getMetaData(), aae.getMetaData()),
              null, resMgr);
      reader.typeSystemInit(cas.getTypeSystem());

      // Process
      while (reader.hasNext()) {
        reader.getNext(cas);
        aae.process(cas);
        cas.reset();
      }

      // Signal end of processing
      aae.collectionProcessComplete();
    } finally {
      // Destroy
      LifeCycleUtil.destroy(reader);
      LifeCycleUtil.destroy(aae);
      LifeCycleUtil.destroy(resMgr);
    }
  }

  /**
   * <p>
   * Provides a simple way to run a pipeline for a given collection reader and sequence of analysis
   * engines. After processing all CASes provided by the reader, the method calls
   * {@link AnalysisEngine#collectionProcessComplete() collectionProcessComplete()} on the engines.
   * Note that {@link AnalysisEngine#destroy()} and {@link CollectionReader#destroy()} are
   * <b>NOT</b> called. As the components were instantiated by the caller, they must also be managed
   * (i.e. destroyed) the caller.
   * </p>
   * <p>
   * External resources can only be shared between the reader and/or the analysis engines if the
   * reader/engines have been previously instantiated using a shared resource manager.
   * </p>
   * <p>
   * The CAS is created using the resource manager used by the collection reader.
   * </p>
   * 
   * @param reader
   *          a collection reader
   * @param engines
   *          a sequence of analysis engines
   * @throws IOException
   *           if there is an I/O problem in the reader
   * @throws CollectionException 
   *           if there is a problem initializing or running the pipeline.
   * @throws ResourceInitializationException 
   *           if there is a problem initializing or running the pipeline.
   * @throws AnalysisEngineProcessException 
   *           if there is a problem initializing or running the pipeline.
   */
  public static void runPipeline(final CollectionReader reader, final AnalysisEngine... engines)
          throws IOException, AnalysisEngineProcessException, ResourceInitializationException,
          CollectionException {
    runPipeline(reader.getResourceManager(), reader, engines);
  }
  
  /**
   * <p>
   * Provides a simple way to run a pipeline for a given collection reader and sequence of analysis
   * engines. After processing all CASes provided by the reader, the method calls
   * {@link AnalysisEngine#collectionProcessComplete() collectionProcessComplete()} on the engines.
   * Note that {@link AnalysisEngine#destroy()} and {@link CollectionReader#destroy()} are
   * <b>NOT</b> called. As the components were instantiated by the caller, they must also be managed
   * (i.e. destroyed) the caller.
   * </p>
   * <p>
   * External resources can only be shared between the reader and/or the analysis engines if the
   * reader/engines have been previously instantiated using a shared resource manager.
   * </p>
   * 
   * @param aResMgr
   *          a resource manager. Normally the same one used by the collection reader and analysis
   *          engines.
   * @param reader
   *          a collection reader
   * @param engines
   *          a sequence of analysis engines
   * @throws IOException
   *           if there is an I/O problem in the reader
   * @throws ResourceInitializationException 
   *           if there is a problem initializing or running the pipeline.
   * @throws CollectionException 
   *           if there is a problem initializing or running the pipeline.
   * @throws AnalysisEngineProcessException 
   *           if there is a problem initializing or running the pipeline.
   */
  public static void runPipeline(final ResourceManager aResMgr, final CollectionReader reader,
          final AnalysisEngine... engines) throws IOException, ResourceInitializationException,
          AnalysisEngineProcessException, CollectionException {
    final List<ResourceMetaData> metaData = new ArrayList<ResourceMetaData>();
    metaData.add(reader.getMetaData());
    for (AnalysisEngine engine : engines) {
      metaData.add(engine.getMetaData());
    }

    final CAS cas = CasCreationUtils.createCas(metaData, null, aResMgr);
    reader.typeSystemInit(cas.getTypeSystem());

    while (reader.hasNext()) {
      reader.getNext(cas);
      runPipeline(cas, engines);
      cas.reset();
    }

    collectionProcessComplete(engines);
  }

  /**
   * <p>
   * Run a sequence of {@link AnalysisEngine analysis engines} over a {@link JCas}. The result of
   * the analysis can be read from the JCas.
   * </p>
   * <p>
   * External resources can be shared between the analysis engines.
   * </p>
   * 
   * @param aCas
   *          the CAS to process
   * @param aDescs
   *          a sequence of analysis engines to run on the jCas
   * @throws ResourceInitializationException
   *           if there is a problem initializing the components
   * @throws AnalysisEngineProcessException
   *           if there is a problem during the execution of the components
   */
  public static void runPipeline(final CAS aCas, final AnalysisEngineDescription... aDescs)
          throws ResourceInitializationException, AnalysisEngineProcessException {
    AnalysisEngine aae = null;
    try {
      // Create aggregate AE
      final AnalysisEngineDescription aaeDesc = createEngineDescription(aDescs);
  
      // Instantiate
      aae = createEngine(aaeDesc);
      
      // Process
      aae.process(aCas);

      // Signal end of processing
      aae.collectionProcessComplete();
    } finally {
      // Destroy
      LifeCycleUtil.destroy(aae);
    }
  }

  /**
   * <p>
   * Run a sequence of {@link AnalysisEngine analysis engines} over a {@link JCas}. The result of
   * the analysis can be read from the JCas.
   * </p>
   * <p>
   * External resources can be shared between the analysis engines.
   * </p>
   * 
   * @param jCas
   *          the jCas to process
   * @param descs
   *          a sequence of analysis engines to run on the jCas
   * @throws ResourceInitializationException
   *           if there is a problem initializing the components
   * @throws AnalysisEngineProcessException
   *           if there is a problem during the execution of the components
   */
  public static void runPipeline(final JCas jCas, final AnalysisEngineDescription... descs)
          throws AnalysisEngineProcessException, ResourceInitializationException {
    runPipeline(jCas.getCas(), descs);
  }

  /**
   * <p>
   * Run a sequence of {@link AnalysisEngine analysis engines} over a {@link JCas}. This method does
   * not {@link AnalysisEngine#destroy() destroy} the engines or send them other events like
   * {@link AnalysisEngine#collectionProcessComplete()}. This is left to the caller.
   * </p>
   * <p>
   * External resources can only be shared between the analysis engines if the engines have been
   * previously instantiated using a shared resource manager.
   * </p>
   * 
   * 
   * @param jCas
   *          the jCas to process
   * @param engines
   *          a sequence of analysis engines to run on the jCas
   * @throws AnalysisEngineProcessException
   *           if there is a problem during the execution of the components
   */
  public static void runPipeline(final JCas jCas, final AnalysisEngine... engines)
          throws AnalysisEngineProcessException {
    for (AnalysisEngine engine : engines) {
      engine.process(jCas);
    }
  }

  /**
   * <p>
   * Run a sequence of {@link AnalysisEngine analysis engines} over a {@link CAS}. This method does
   * not {@link AnalysisEngine#destroy() destroy} the engines or send them other events like
   * {@link AnalysisEngine#collectionProcessComplete()}. This is left to the caller.
   * </p>
   * <p>
   * External resources can only be shared between the analysis engines if the engines have been
   * previously instantiated using a shared resource manager.
   * </p>
   * 
   * @param cas
   *          the CAS to process
   * @param engines
   *          a sequence of analysis engines to run on the jCas
   * @throws AnalysisEngineProcessException
   *           if there is a problem during the execution of the components
   */
  public static void runPipeline(final CAS cas, final AnalysisEngine... engines)
          throws AnalysisEngineProcessException {
    for (AnalysisEngine engine : engines) {
      engine.process(cas);
    }
  }

  /**
   * <p>
   * Iterate through the {@link JCas JCases} processed by the pipeline, allowing to access each one
   * after it has been processed.
   * </p>
   * <p>
   * External resources can be shared between the reader and the analysis engines.
   * </p>
   * 
   * @param aReader
   *          the collection reader.
   * @param aEngines
   *          the analysis engines.
   * @return an {@link Iterable}&lt;{@link JCas}&gt; which can be used in an extended for-loop.
   */
  public static JCasIterable iteratePipeline(final CollectionReaderDescription aReader,
          AnalysisEngineDescription... aEngines) {
    return new JCasIterable(aReader, aEngines);
  }
}
