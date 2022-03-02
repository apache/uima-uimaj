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

import static java.util.Arrays.asList;
import static org.apache.uima.fit.cpe.CpePipeline.runPipeline;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.component.CasConsumer_ImplBase;
import org.apache.uima.fit.component.CasMultiplier_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.junit.jupiter.api.Test;

public class CasMultiplierTest {
  /**
   * Simulates a CPE with CAS multipliers that always read one CAS and always produce one CAS. It
   * actually appears to work despite CPE not supporting CAS multipliers.
   */
  @SuppressWarnings("javadoc")
  @Test
  public void testRunPipeline() throws Exception {
    CollectionReaderDescription reader = createReaderDescription(Reader.class);

    AnalysisEngineDescription incrementor = createEngineDescription(Incrementor.class);

    AnalysisEngineDescription consumer = createEngineDescription(Consumer.class);

    AnalysisEngineDescription aggregate = createEngineDescription(incrementor, incrementor,
            incrementor, consumer);

    runPipeline(reader, aggregate);

    // The order in which the consumer sees the CASes is arbitrary, in particular because we never
    // tell the CPE that the aggregate which contains the consumer cannot be scaled out.
    assertFalse(aggregate.getAnalysisEngineMetaData().getOperationalProperties()
            .isMultipleDeploymentAllowed());
    Collections.sort(Consumer.result);

    assertEquals(asList(4, 5, 6, 7, 8, 9, 10, 11, 12, 13), Consumer.result);
  }

  public static class Reader extends CasCollectionReader_ImplBase {
    private int generated = 0;

    @Override
    public void getNext(CAS aCAS) throws IOException, CollectionException {
      generated++;
      aCAS.setDocumentText(Integer.toString(generated));
      // System.out.printf("%n[%s] Generated: %s%n", Thread.currentThread().getName(),
      // aCAS.getDocumentText());
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
      return generated < 10;
    }

    @Override
    public Progress[] getProgress() {
      return null;
    }
  }

  /**
   * Takes in a CAS, interprets its text as an integer, adds one to it, and creates a new CAS with
   * the new value as text.
   */
  public static class Incrementor extends CasMultiplier_ImplBase {
    private boolean inputReceived = false;
    private boolean outputCreated = false;

    private int value = -1;

    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
      return inputReceived && !outputCreated;
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
      outputCreated = true;
      inputReceived = false;

      CAS output = getEmptyCAS();
      output.setDocumentText(Integer.toString(value + 1));
      value = -1;

      // System.out.printf("[%s] Out : %s%n", Thread.currentThread().getName(),
      // output.getDocumentText());

      return output;
    }

    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      outputCreated = false;
      inputReceived = true;

      value = Integer.valueOf(aCAS.getDocumentText());

      // System.out.printf("[%s] In : %s%n", Thread.currentThread().getName(),
      // aCAS.getDocumentText());
    }
  }

  public static class Consumer extends CasConsumer_ImplBase {
    public static List<Integer> result;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
      super.initialize(aContext);
      result = new ArrayList<>();
    }

    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      // System.out.printf("Result : %s%n", aCAS.getDocumentText());
      result.add(Integer.valueOf(aCAS.getDocumentText()));
    }
  }
}
