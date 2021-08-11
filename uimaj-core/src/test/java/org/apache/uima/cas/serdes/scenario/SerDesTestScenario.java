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
package org.apache.uima.cas.serdes.scenario;

import static org.apache.uima.cas.serdes.CasToComparableText.toComparableString;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CasCompare;
import org.apache.uima.cas.serdes.transitions.CasSerDesCycleConfiguration;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.assertj.core.internal.Failures;

public class SerDesTestScenario implements Runnable {
  private final String title;
  private final String debugInfo;
  private final FailableSupplier<CAS, ?> sourceCasSupplier;
  private final FailableSupplier<CAS, ?> targetCasSupplier;
  private final FailableBiConsumer<CAS, CAS, ?> cycle;

  public SerDesTestScenario(CasSourceTargetConfiguration aSourceTargetConfig,
          CasSerDesCycleConfiguration aCycle) {
    title = aCycle.getTitle() + " - " + aSourceTargetConfig.getTitle();
    debugInfo = aSourceTargetConfig.getDebugInfo();
    sourceCasSupplier = aSourceTargetConfig::createSourceCas;
    targetCasSupplier = aSourceTargetConfig::createTargetCas;
    cycle = aCycle::performCycle;
  }

  public SerDesTestScenario(String aTitle, FailableSupplier<CAS, ?> aSourceCasSupplier,
          FailableSupplier<CAS, ?> aTargetCasSupplier, FailableBiConsumer<CAS, CAS, ?> aCycle) {
    title = aTitle;
    debugInfo = "no additional details";
    sourceCasSupplier = aSourceCasSupplier;
    targetCasSupplier = aTargetCasSupplier;
    cycle = aCycle;
  }

  public CAS createSourceCas() {
    try {
      return sourceCasSupplier.get();
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to create source CAS");
      error.initCause(e);
      throw error;
    }
  }

  public CAS createTargetCas() {
    try {
      return targetCasSupplier.get();
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to create target CAS");
      error.initCause(e);
      throw error;
    }
  }

  @Override
  public void run() {
    // Fetch the source and target CASes
    CAS sourceCas = createSourceCas();
    CAS targetCas = createTargetCas();

    // Perform a serialization/de-serialization cycle
    serializationDeserializationCycle(sourceCas, targetCas);

    // Compare the de-serialized CAS against the original CAS
    assertThat(toComparableString(targetCas)) //
            .as("Comparable string representation must match (%s)", debugInfo)
            .isEqualTo(toComparableString(sourceCas));

    assertThat(targetCas) //
            .as("CasCompare comparison must match (%s)", debugInfo)
            .usingComparator((a, b) -> CasCompare.compareCASes((CASImpl) a, (CASImpl) b) ? 0 : 1) //
            .isEqualTo(sourceCas);
  }

  public void serializationDeserializationCycle(CAS aSourceCas, CAS aTargetCas) {
    try {
      cycle.accept(aSourceCas, aTargetCas);
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to (de)serialize CAS");
      error.initCause(e);
      throw error;
    }
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getTitle();
  }
}