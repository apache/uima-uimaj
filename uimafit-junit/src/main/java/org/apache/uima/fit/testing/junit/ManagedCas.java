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
package org.apache.uima.fit.testing.junit;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;
import static org.apache.uima.fit.factory.CasFactory.createCas;

import java.util.Set;
import java.util.WeakHashMap;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Provides a {@link CAS} object which is automatically reset before the test. The idea of this
 * class is to re-use CAS objects across different test method to avoid the overhead of having to
 * set up a new CAS every time. Each thread requesting a CAS gets a different instance (the CASes
 * are internally managed as {@link ThreadLocal}. When a test completes, all of the CASses that
 * handed out to any thread are reset (except any CASes which may meanwhile have been garbage
 * collected).
 */
public final class ManagedCas
        implements TestWatcher, BeforeTestExecutionCallback, AfterAllCallback {
  private final ThreadLocal<CAS> casHolder;

  private final static Set<CAS> managedCases = synchronizedSet(newSetFromMap(new WeakHashMap<>()));

  /**
   * Provides a CAS with an auto-detected type system.
   */
  public ManagedCas() {
    casHolder = ThreadLocal.withInitial(() -> {
      try {
        CAS cas = createCas();
        managedCases.add(cas);
        return cas;
      } catch (UIMAException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Provides a CAS with the specified type system.
   * 
   * @param aTypeSystemDescription
   *          the type system used to initialize the CAS.
   */
  public ManagedCas(TypeSystemDescription aTypeSystemDescription) {
    casHolder = ThreadLocal.withInitial(() -> {
      try {
        CAS cas = createCas(aTypeSystemDescription);
        managedCases.add(cas);
        return cas;
      } catch (UIMAException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * @return the CAS object managed by this rule.
   */
  public CAS get() {
    return casHolder.get();
  }

  @Override
  public void beforeTestExecution(ExtensionContext aContext) throws Exception {
    managedCases.forEach(cas -> cas.reset());
  }

  @Override
  public void afterAll(ExtensionContext aContext) throws Exception {
    casHolder.set(null);
  }
}