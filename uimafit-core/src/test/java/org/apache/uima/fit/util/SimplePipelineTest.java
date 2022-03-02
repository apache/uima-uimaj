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
package org.apache.uima.fit.util;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.factory.testAes.Annotator1;
import org.apache.uima.fit.factory.testAes.Annotator2;
import org.apache.uima.fit.factory.testAes.Annotator3;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

/**
 */
public class SimplePipelineTest extends ComponentTestBase {

  @Test
  public void test1() throws UIMAException, IOException {
    // Creating a CAS locally here to work around UIMA-5097 - otherwise this test may fail if
    // run in Eclipse or in other unit test setups where the same JVM is re-used for multiple tests.
    TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
    TypePriorities tp = TypePrioritiesFactory
            .createTypePriorities(new String[] { "org.apache.uima.fit.type.Sentence",
                "org.apache.uima.fit.type.AnalyzedText", "org.apache.uima.fit.type.Token" });
    JCas jcas = CasCreationUtils.createCas(tsd, tp, null).getJCas();

    CasIOUtil.readJCas(jcas, new File("src/test/resources/data/docs/test.xmi"));
    AnalysisEngineDescription aed1 = AnalysisEngineFactory.createEngineDescription(Annotator1.class,
            typeSystemDescription);
    AnalysisEngineDescription aed2 = AnalysisEngineFactory.createEngineDescription(Annotator2.class,
            typeSystemDescription);
    AnalysisEngineDescription aed3 = AnalysisEngineFactory.createEngineDescription(Annotator3.class,
            typeSystemDescription);
    SimplePipeline.runPipeline(jcas, aed1, aed2, aed3);
  }
}
