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

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.junit.jupiter.api.Test;

public class CpePipelineTest {
  @Test
  public void test() throws Exception {
    runPipeline( //
            createReaderDescription(Reader.class), createEngineDescription(Annotator.class),
            createEngineDescription(Writer.class));

    assertThat(Writer.MARKER_SEEN).isEqualTo(MARKER);
  }

  public static final String TEXT = "Some text";

  public static final String MARKER = "annotator has seen this document";

  public static class Reader extends JCasCollectionReader_ImplBase {

    private int size = 1;

    private int current = 0;

    private boolean initTypeSystemCalled = false;

    @Override
    public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException {
      initTypeSystemCalled = true;
    }

    @Override
    public Progress[] getProgress() {
      return null;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
      assertThat(initTypeSystemCalled) //
              .as("typeSystemInit() has not been called") //
              .isTrue();
      return this.current < this.size;
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
      jCas.setDocumentText(TEXT);
      this.current += 1;
    }

  }

  public static class Annotator extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      jCas.setDocumentLanguage(MARKER);
    }
  }

  public static class Writer extends JCasAnnotator_ImplBase {

    public static String MARKER_SEEN;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      MARKER_SEEN = jCas.getDocumentLanguage();
    }
  }
}
