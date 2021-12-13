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
package org.apache.uima.cas.serdes.transitions;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.uima.cas.CAS;
import org.assertj.core.internal.Failures;

public class CasSerDesCycleConfiguration {
  private final String title;
  private final FailableBiConsumer<CAS, CAS, ?> cycle;

  public CasSerDesCycleConfiguration(String aTitle, FailableBiConsumer<CAS, CAS, ?> aCycle) {
    title = aTitle;
    cycle = aCycle;
  }

  public String getTitle() {
    return title;
  }

  public void performCycle(CAS aSourceCas, CAS aTargetCas) {
    try {
      cycle.accept(aSourceCas, aTargetCas);
    } catch (Throwable e) {
      AssertionError error = Failures.instance().failure("Unable to (de)serialize CAS");
      error.initCause(e);
      throw error;
    }
  }
}