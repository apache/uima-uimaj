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

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

/**
 */

public class DefaultPackageAETest extends ComponentTestBase {

  @Test
  public void testPackageLessAE() throws Exception {
    AnalysisEngineDescription aed = AnalysisEngineFactory
            .createEngineDescription(DefaultPackageAE.class, (Object[]) null);
    jCas.setDocumentText("some text");
    SimplePipeline.runPipeline(jCas, aed);

    aed = AnalysisEngineFactory.createEngineDescription(DefaultPackageAE2.class, (Object[]) null);
    jCas.reset();
    jCas.setDocumentText("some text");
    SimplePipeline.runPipeline(jCas, aed);

  }

  public static class DefaultPackageAE2 extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      // does nothing
    }
  }

}
