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
package org.apache.uima.analysis_component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CasProcessorAnnotatorTest {

  private static final String NEEDLE = "needle";

  private CAS cas;

  @BeforeEach
  public void setup() throws Exception {
    cas = CasCreationUtils.createCas();
  }

  @Test
  void thatProcessingWithJCasInterfaceWorks() throws Exception {
    AnalysisEngine engine = CasProcessorAnnotator.of(_cas -> _cas.setDocumentText(NEEDLE));
    engine.process(cas.getJCas());

    assertThat(cas.getDocumentText()).isEqualTo(NEEDLE);
  }

  @Test
  void thatProcessingWithCasInterfaceWorks() throws Exception {
    AnalysisEngine engine = CasProcessorAnnotator.of(_cas -> _cas.setDocumentText(NEEDLE));
    engine.process(cas);

    assertThat(cas.getDocumentText()).isEqualTo(NEEDLE);
  }

  @Test
  void thatAnalysisEngineProcessExceptionMakesItThrough() throws Exception {
    AnalysisEngine engine = CasProcessorAnnotator.of(_cas -> {
      throw new AnalysisEngineProcessException(NEEDLE, null);
    });

    assertThatExceptionOfType(AnalysisEngineProcessException.class)
            .isThrownBy(() -> engine.process(cas)) //
            .matches(ex -> NEEDLE.equals(ex.getMessageKey()));
  }

  @Test
  void thatGenericExceptionIsWrapped() throws Exception {
    AnalysisEngine engine = CasProcessorAnnotator.of(_cas -> {
      throw new RuntimeException(NEEDLE);
    });

    Assertions.setMaxStackTraceElementsDisplayed(1000);
    assertThatExceptionOfType(AnalysisEngineProcessException.class)
            .isThrownBy(() -> engine.process(cas)) //
            .matches(ex -> ex.getCause() instanceof RuntimeException)
            .matches(ex -> NEEDLE.equals(ex.getCause().getMessage()));
  }
}
