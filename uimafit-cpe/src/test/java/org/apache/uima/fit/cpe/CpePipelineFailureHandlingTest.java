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

import static org.apache.uima.fit.cpe.CpePipeline.runPipeline;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Progress;
import org.junit.Test;

public class CpePipelineFailureHandlingTest {
  private static AtomicInteger processed = new AtomicInteger(0);
  
  @Test
  public void test() throws Exception {
    int failAfter = 50;
    try {
      runPipeline( //
              createReaderDescription(Reader.class, //
                      "failAfter", failAfter),
              createEngineDescription(Annotator.class));
    } catch (Exception e) {
      // Ignore
    }

    assertThat(processed.get())
      .as("CPE stopped processing after reader threw an exception")
      .isEqualTo(failAfter);
  }

  public static class Reader extends JCasCollectionReader_ImplBase {

    @ConfigurationParameter
    private int failAfter;

    @Override
    public Progress[] getProgress() {
      return null;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
      return processed.get() < failAfter * 2;
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
      if (processed.incrementAndGet() >= failAfter) {
        throw new CollectionException();
      }
    }
  }

  public static class Annotator extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      // Nothing to do
    }
  }
}
