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

import static java.nio.file.Files.newInputStream;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CasDumpWriterTest {

  @Test
  public void test(@TempDir Path folder) throws Exception {
    File outputFile = folder.resolve("dump-output.txt").toFile();

    AnalysisEngineDescription writer = createEngineDescription(CasDumpWriter.class, //
            CasDumpWriter.PARAM_OUTPUT_FILE, outputFile.getPath());

    JCas jcas = JCasFactory.createJCas();
    try (InputStream is = newInputStream(Paths.get("src/test/resources/data/docs/test.xmi"))) {
        CasIOUtils.load(is, jcas.getCas());
    }
    runPipeline(jcas, writer);
    assertTrue(outputFile.exists());

    String reference = readFileToString(new File("src/test/resources/data/reference/test.xmi.dump"),
            "UTF-8").trim();
    String actual = readFileToString(outputFile, "UTF-8").trim();
    actual = actual.replaceAll("\r\n", "\n");

    assertEquals(reference, actual);
  }
}
