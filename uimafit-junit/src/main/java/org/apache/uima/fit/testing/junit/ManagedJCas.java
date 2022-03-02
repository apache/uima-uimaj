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
import static org.apache.uima.fit.factory.JCasFactory.createJCas;

import java.util.Set;
import java.util.WeakHashMap;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Provides a {@link JCas} object which is automatically reset before the test. The idea of this
 * class is to re-use JCas objects across different test method to avoid the overhead of having to
 * set up a new JCas every time. Each thread requesting a JCas gets a different instance (the JCases
 * are internally managed as {@link ThreadLocal}. When a test completes, all of the JCasses that
 * handed out to any thread are reset (except any JCases which may meanwhile have been garbage
 * collected).
 */
public final class ManagedJCas extends TestWatcher {
  private final ThreadLocal<JCas> casHolder;

  private final static Set<JCas> managedCases = synchronizedSet(newSetFromMap(new WeakHashMap<>()));

  /**
   * Provides a JCas with an auto-detected type system.
   */
  public ManagedJCas() {
    casHolder = ThreadLocal.withInitial(() -> {
      try {
        JCas cas = createJCas();
        managedCases.add(cas);
        return cas;
      } catch (UIMAException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Provides a JCas with the specified type system.
   * 
   * @param aTypeSystemDescription
   *          the type system used to initialize the CAS.
   */
  public ManagedJCas(TypeSystemDescription aTypeSystemDescription) {
    casHolder = ThreadLocal.withInitial(() -> {
      try {
        JCas cas = createJCas(aTypeSystemDescription);
        managedCases.add(cas);
        return cas;
      } catch (UIMAException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * @return the JCas object managed by this rule.
   */
  public JCas get() {
    return casHolder.get();
  }

  @Override
  protected void starting(Description description) {
    managedCases.forEach(cas -> cas.reset());
  }
}