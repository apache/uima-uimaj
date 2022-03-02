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
package org.apache.uima.fit.component;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CasMultiplierTest {
  @Test
  public void testRunPipeline() throws Exception {
    CollectionReaderDescription reader = createReaderDescription(Reader.class);

    AnalysisEngineDescription incrementor = createEngineDescription(TextIncrementor.class);

    AnalysisEngineDescription consumer = createEngineDescription(Consumer.class);

    runPipeline(reader, incrementor, incrementor, incrementor, consumer);

    assertEquals(13, Consumer.textResult);
  }

  @Disabled("UIMA-3470 not fixed yet")
  @Test
  public void testIteratePipelineOnText() throws Exception {
    CollectionReaderDescription reader = createReaderDescription(Reader.class);

    AnalysisEngineDescription incrementor = createEngineDescription(TextIncrementor.class);

    AnalysisEngineDescription consumer = createEngineDescription(Consumer.class);

    int expectedResult = 4;
    for (JCas jcas : iteratePipeline(reader, incrementor, incrementor, incrementor, consumer)) {
      assertEquals(expectedResult, Consumer.textResult);
      assertEquals(expectedResult, Integer.parseInt(jcas.getDocumentText()));
      expectedResult++;
    }
  }

  @Test
  public void testIteratePipelineOnFeature() throws Exception {
    CollectionReaderDescription reader = createReaderDescription(Reader.class);

    AnalysisEngineDescription incrementor = createEngineDescription(FeatureIncrementor.class);

    AnalysisEngineDescription consumer = createEngineDescription(Consumer.class);

    int expectedResult = 4;
    for (JCas jcas : iteratePipeline(reader, incrementor, incrementor, incrementor, consumer)) {
      assertEquals(expectedResult, Consumer.featureResult);
      assertEquals(expectedResult, Integer.parseInt(jcas.getDocumentLanguage()));
      expectedResult++;
    }
  }

  public static class Reader extends CasCollectionReader_ImplBase {
    private int generated = 0;

    @Override
    public void getNext(CAS aCAS) throws IOException, CollectionException {
      generated++;
      aCAS.setDocumentText(Integer.toString(generated));
      aCAS.setDocumentLanguage(Integer.toString(generated));
      // System.out.printf("%nGenerated: %s%n", aCAS.getDocumentText());
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
  public static class TextIncrementor extends CasMultiplier_ImplBase {
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

      // System.out.printf(" Out : %s%n", output.getDocumentText());

      return output;
    }

    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      outputCreated = false;
      inputReceived = true;

      value = Integer.valueOf(aCAS.getDocumentText());

      // System.out.printf(" In : %s%n", aCAS.getDocumentText());
    }
  }

  /**
   * Takes in a CAS, interprets its text as an integer, adds one to it, and creates a new CAS with
   * the new value as text.
   */
  public static class FeatureIncrementor extends CasMultiplier_ImplBase {
    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
      // By always returning "false" here, we tell the flow-controller to just pass on the input
      // CAS to the next step - actually we are operating as a simple CasAnnotator.
      return false;
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
      // This is actually never called because hasNext() is hard-coded to false.
      return null;
    }

    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      int n = Integer.parseInt(aCAS.getDocumentLanguage());
      // System.out.printf(" In : %s%n", aCAS.getDocumentLanguage());

      n++;
      aCAS.setDocumentLanguage(Integer.toString(n));
      // System.out.printf(" Out : %s%n", aCAS.getDocumentLanguage());
    }
  }

  public static class Consumer extends CasAnnotator_ImplBase {
    public static int textResult = -1;
    public static int featureResult = -1;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
      super.initialize(aContext);
      textResult = -1;
      featureResult = -1;
    }

    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      // System.out.printf("Text result : %s%n", aCAS.getDocumentText());
      textResult = Integer.valueOf(aCAS.getDocumentText());

      // System.out.printf("Feature result : %s%n", aCAS.getDocumentLanguage());
      try {
        featureResult = Integer.valueOf(aCAS.getDocumentLanguage());
      } catch (NumberFormatException e) {
        featureResult = -2;
      }
    }
  }
}
