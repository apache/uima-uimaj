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
package org.apache.uima.fit.cpe;

import static java.lang.Runtime.getRuntime;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

public final class CpePipeline {
  private CpePipeline() {
    // No instances
  }

  /**
   * Run the CollectionReader and AnalysisEngines as a multi-threaded pipeline. This call uses a
   * number of threads equal to the number of available processors (as reported by Java, so usually
   * boiling down to cores) minus 1 - minimum of 1.
   * 
   * @param readerDesc
   *          The CollectionReader that loads the documents into the CAS.
   * @param descs
   *          Primitive AnalysisEngineDescriptions that process the CAS, in order. If you have a mix
   *          of primitive and aggregate engines, then please create the AnalysisEngines yourself
   *          and call the other runPipeline method.
   * @throws SAXException
   *           if there was a XML-related problem materializing the component descriptors that are
   *           referenced from the CPE descriptor
   * @throws InvalidXMLException
   *           if there was a XML-related problem materializing the component descriptors that are
   *           referenced from the CPE descriptor
   * @throws IOException
   *           if there was a I/O-related problem materializing the component descriptors that are
   *           referenced from the CPE descriptor
   * @throws CpeDescriptorException
   *           if there was a problem configuring the CPE descriptor
   * @throws ResourceInitializationException
   *           if there was a problem initializing or running the CPE.
   * @throws AnalysisEngineProcessException
   *           if there was a problem initializing or running the CPE.
   */
  public static void runPipeline(final CollectionReaderDescription readerDesc,
          final AnalysisEngineDescription... descs)
          throws SAXException, CpeDescriptorException, IOException, ResourceInitializationException,
          InvalidXMLException, AnalysisEngineProcessException {

    runPipeline(Math.max(1, getRuntime().availableProcessors() - 1), readerDesc, descs);
  }

  /**
   * Run the CollectionReader and AnalysisEngines as a multi-threaded pipeline.
   * 
   * @param parallelism
   *          Number of threads to use when running the analysis engines in the CPE.
   * @param readerDesc
   *          The CollectionReader that loads the documents into the CAS.
   * @param descs
   *          Primitive AnalysisEngineDescriptions that process the CAS, in order. If you have a mix
   *          of primitive and aggregate engines, then please create the AnalysisEngines yourself
   *          and call the other runPipeline method.
   * @throws SAXException
   *           if there was a XML-related problem materializing the component descriptors that are
   *           referenced from the CPE descriptor
   * @throws IOException
   *           if there was a I/O-related problem materializing the component descriptors that are
   *           referenced from the CPE descriptor
   * @throws CpeDescriptorException
   *           if there was a problem configuring the CPE descriptor
   * @throws ResourceInitializationException
   *           if there was a problem initializing or running the CPE.
   * @throws InvalidXMLException
   *           if there was a problem initializing or running the CPE.
   * @throws AnalysisEngineProcessException
   *           if there was a problem running the CPE.
   */
  public static void runPipeline(final int parallelism,
          final CollectionReaderDescription readerDesc, final AnalysisEngineDescription... descs)
          throws SAXException, CpeDescriptorException, IOException, ResourceInitializationException,
          InvalidXMLException, AnalysisEngineProcessException {
    AnalysisEngineDescription topLevelAnalysisEngine;

    if (descs.length == 1 && !mayContainCasMultiplier(descs[0])) {
      topLevelAnalysisEngine = descs[0];
    } else {
      topLevelAnalysisEngine = createEngineDescription(descs);
      topLevelAnalysisEngine.getMetaData().setName("Top-level CPE Aggregate");
    }

    CpeBuilder builder = new CpeBuilder();
    builder.setReader(readerDesc);
    builder.setAnalysisEngine(topLevelAnalysisEngine);
    builder.setMaxProcessingUnitThreadCount(parallelism);

    StatusCallbackListenerImpl status = new StatusCallbackListenerImpl();
    CollectionProcessingEngine engine = builder.createCpe(status);

    engine.process();
    while (status.isProcessing) {
      synchronized (status) {
        try {
          status.wait();
        } catch (InterruptedException e) {
          // Do nothing
        }

        if (status.exceptions.size() > 0) {
          if (engine.isProcessing()) {
            engine.kill();
          }

          throw new AnalysisEngineProcessException(status.exceptions.get(0));
        }
      }
    }
  }

  private static boolean mayContainCasMultiplier(final AnalysisEngineDescription desc) {
    if (desc.isPrimitive()) {
      return desc.getAnalysisEngineMetaData().getOperationalProperties()
              .isMultipleDeploymentAllowed();
    }

    for (MetaDataObject mdo : desc.getDelegateAnalysisEngineSpecifiersWithImports().values()) {
      if (mdo instanceof Import) {
        // The imported delegate might be a CAS multiplier, but we cannot really tell without
        // risking an exception. So let's just assume it does.
        return true;
      }

      if (mdo instanceof AnalysisEngineDescription) {
        AnalysisEngineDescription aed = (AnalysisEngineDescription) mdo;
        if (aed.getAnalysisEngineMetaData().getOperationalProperties().getOutputsNewCASes()) {
          return true;
        }
      }
    }

    return false;
  }

  private static class StatusCallbackListenerImpl implements StatusCallbackListener {

    private final List<Exception> exceptions = new ArrayList<Exception>();

    private boolean isProcessing = true;

    @Override
    public void entityProcessComplete(CAS arg0, EntityProcessStatus arg1) {
      if (arg1.isException()) {
        for (Exception e : arg1.getExceptions()) {
          exceptions.add(e);
          synchronized (this) {
            notify();
          }
        }
      }
    }

    @Override
    public void aborted() {
      if (isProcessing) {
        isProcessing = false;
        synchronized (this) {
          notify();
        }
      }
    }

    @Override
    public void batchProcessComplete() {
      // Do nothing
    }

    @Override
    public void collectionProcessComplete() {
      if (isProcessing) {
        isProcessing = false;
        synchronized (this) {
          notify();
        }
      }
    }

    @Override
    public void initializationComplete() {
      // Do nothing
    }

    @Override
    public void paused() {
      // Do nothing
    }

    @Override
    public void resumed() {
      // Do nothing
    }
  }
}
