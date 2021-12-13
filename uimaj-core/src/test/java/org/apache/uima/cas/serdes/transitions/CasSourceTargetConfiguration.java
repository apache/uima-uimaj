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

import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.uima.cas.CAS;
import org.assertj.core.internal.Failures;

public class CasSourceTargetConfiguration {
  private final String title;
  private final String debugInfo;
  private final FailableSupplier<CAS, ?> sourceCasSupplier;
  private final FailableSupplier<CAS, ?> targetCasSupplier;

  private CasSourceTargetConfiguration(Builder builder) {
    this.title = builder.title;
    this.debugInfo = builder.debugInfo;
    this.sourceCasSupplier = builder.sourceCasSupplier;
    this.targetCasSupplier = builder.targetCasSupplier;
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

  public String getTitle() {
    return title;
  }

  public String getDebugInfo() {
    return debugInfo;
  }

  /**
   * Creates builder to build {@link CasSourceTargetConfiguration}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link CasSourceTargetConfiguration}.
   */
  public static final class Builder {
    private String title;
    private String debugInfo;
    private FailableSupplier<CAS, ?> sourceCasSupplier;
    private FailableSupplier<CAS, ?> targetCasSupplier;

    private Builder() {
    }

    public Builder withTitle(String aTitle) {
      title = aTitle;
      return this;
    }

    public Builder withDebugInfo(String aDebugInfo) {
      debugInfo = aDebugInfo;
      return this;
    }

    public Builder withSourceCasSupplier(FailableSupplier<CAS, ?> aSourceCasSupplier) {
      sourceCasSupplier = aSourceCasSupplier;
      return this;
    }

    public Builder withTargetCasSupplier(FailableSupplier<CAS, ?> aTargetCasSupplier) {
      targetCasSupplier = aTargetCasSupplier;
      return this;
    }

    public CasSourceTargetConfiguration build() {
      return new CasSourceTargetConfiguration(this);
    }
  }
}