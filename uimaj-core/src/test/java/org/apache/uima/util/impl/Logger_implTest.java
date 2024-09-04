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

package org.apache.uima.util.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.util.Level;
import org.junit.jupiter.api.Test;

/**
 * UIMA Logging interface implementation test without using an logging toolkit
 */
class Logger_implTest {
  @Test
  void testLogWrapperCreation() throws Exception {
    org.apache.uima.util.Logger rootLogger = Logger_impl.getInstance();
    org.apache.uima.util.Logger rootLogger1 = Logger_impl.getInstance();
    org.apache.uima.util.Logger classLogger = Logger_impl.getInstance(this.getClass());
    org.apache.uima.util.Logger classLogger1 = Logger_impl.getInstance(this.getClass());

    rootLogger.setLevel(Level.INFO);

    // check default configuration
    assertThat(rootLogger).isNotNull();
    assertThat(classLogger).isNotNull();
    assertThat(rootLogger.isLoggable(Level.INFO)).isTrue();
    assertThat(classLogger.isLoggable(Level.INFO)).isTrue();

    // check getInstance() calls
    assertThat(classLogger).isNotSameAs(rootLogger);
    assertThat(rootLogger1).isEqualTo(rootLogger);
    assertThat(classLogger).isNotSameAs(classLogger1);
  }

  @Test
  void testMessageLeveling() throws Exception {
    // create logger

    org.apache.uima.util.Logger rootLogger = Logger_impl.getInstance();
    org.apache.uima.util.Logger classLogger = Logger_impl.getInstance(this.getClass());

    try {
      rootLogger.setLevel(Level.INFO);

      // check message leveling root logger
      assertThat(rootLogger.isLoggable(Level.ALL)).isFalse();
      assertThat(rootLogger.isLoggable(Level.FINEST)).isFalse();
      assertThat(rootLogger.isLoggable(Level.FINER)).isFalse();
      assertThat(rootLogger.isLoggable(Level.FINE)).isFalse();
      assertThat(rootLogger.isLoggable(Level.CONFIG)).isFalse();
      assertThat(rootLogger.isLoggable(Level.INFO)).isTrue();
      assertThat(rootLogger.isLoggable(Level.WARNING)).isTrue();
      assertThat(rootLogger.isLoggable(Level.SEVERE)).isTrue();
      assertThat(rootLogger.isLoggable(Level.OFF)).isTrue();

      // check message leveling class logger
      assertThat(rootLogger.isLoggable(Level.ALL)).isFalse();
      assertThat(rootLogger.isLoggable(Level.FINEST)).isFalse();
      assertThat(rootLogger.isLoggable(Level.FINER)).isFalse();
      assertThat(rootLogger.isLoggable(Level.FINE)).isFalse();
      assertThat(rootLogger.isLoggable(Level.CONFIG)).isFalse();
      assertThat(rootLogger.isLoggable(Level.INFO)).isTrue();
      assertThat(rootLogger.isLoggable(Level.WARNING)).isTrue();
      assertThat(rootLogger.isLoggable(Level.SEVERE)).isTrue();
      assertThat(rootLogger.isLoggable(Level.OFF)).isTrue();

      // reset class logger level to OFF
      classLogger.setLevel(Level.OFF);
      assertThat(classLogger.isLoggable(Level.ALL)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINEST)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINER)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINE)).isFalse();
      assertThat(classLogger.isLoggable(Level.CONFIG)).isFalse();
      assertThat(classLogger.isLoggable(Level.INFO)).isFalse();
      assertThat(classLogger.isLoggable(Level.WARNING)).isFalse();
      assertThat(classLogger.isLoggable(Level.SEVERE)).isFalse();
      assertThat(classLogger.isLoggable(Level.OFF)).isTrue();

      // reset class logger level to ALL
      classLogger.setLevel(Level.ALL);
      assertThat(classLogger.isLoggable(Level.ALL)).isTrue();
      assertThat(classLogger.isLoggable(Level.FINEST)).isTrue();
      assertThat(classLogger.isLoggable(Level.FINER)).isTrue();
      assertThat(classLogger.isLoggable(Level.FINE)).isTrue();
      assertThat(classLogger.isLoggable(Level.CONFIG)).isTrue();
      assertThat(classLogger.isLoggable(Level.INFO)).isTrue();
      assertThat(classLogger.isLoggable(Level.WARNING)).isTrue();
      assertThat(classLogger.isLoggable(Level.SEVERE)).isTrue();
      assertThat(classLogger.isLoggable(Level.OFF)).isTrue();

      // reset class logger level to WARNING
      classLogger.setLevel(Level.WARNING);
      assertThat(classLogger.isLoggable(Level.ALL)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINEST)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINER)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINE)).isFalse();
      assertThat(classLogger.isLoggable(Level.CONFIG)).isFalse();
      assertThat(classLogger.isLoggable(Level.INFO)).isFalse();
      assertThat(classLogger.isLoggable(Level.WARNING)).isTrue();
      assertThat(classLogger.isLoggable(Level.SEVERE)).isTrue();
      assertThat(classLogger.isLoggable(Level.OFF)).isTrue();
    } finally {
      rootLogger.setLevel(Level.INFO);
      classLogger.setLevel(Level.INFO);
    }
  }
}
