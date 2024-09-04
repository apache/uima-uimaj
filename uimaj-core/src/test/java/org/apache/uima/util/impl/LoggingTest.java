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

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.junit.jupiter.api.Test;

/**
 * Logger implementation test
 * 
 */
class LoggingTest {
  @Test
  void testDefaultLoggerCreation() throws Exception {
    // get default logger
    Logger logger = UIMAFramework.getLogger();
    assertThat(logger).isNotNull();

    // create another logger
    Logger logger1 = UIMAFramework.getLogger();

    // both loggers must reference the same instance
    assertThat(logger1).isEqualTo(logger);

    // test base logging functions
    logger.log(Level.SEVERE, "Log test messege with Level SEVERE");

    // https://issues.apache.org/jira/browse/UIMA-5719
    logger.logrb(Level.WARNING, "testClass", "testMethod", "org.apache.uima.impl.log_messages",
            "UIMA_external_override_ignored__CONFIG", new Object[] { "n1", "${abc}" });
  }

  @Test
  void testClassLoggerCreation() throws Exception {
    // get class logger
    Logger logger = UIMAFramework.getLogger(this.getClass());
    assertThat(logger).isNotNull();

    // create another class logger
    Logger logger1 = UIMAFramework.getLogger(this.getClass());

    // create default logger
    Logger defaultLogger = UIMAFramework.getLogger();

    // both loggers must reference the same instance
    assertThat(logger1).isEqualTo(logger);

    // should not be the same
    assertThat(logger1).isNotSameAs(defaultLogger);

    // test base logging functions
    logger.log(Level.SEVERE, "Log test messege with Level SEVERE");

    // https://issues.apache.org/jira/browse/UIMA-5719
    logger.logrb(Level.WARNING, "testClass", "testMethod", "org.apache.uima.impl.log_messages",
            "UIMA_external_override_ignored__CONFIG", new Object[] { "n1", "${abc}" });
  }

  @Test
  void testSetLevel() throws Exception {
    // should affect everything in org.apache.uima.*
    Logger uimaLogger = UIMAFramework.getLogger();

    try {
      // get class logger
      Logger logger = UIMAFramework.getLogger(this.getClass());

      // set level to WARNING
      uimaLogger.setLevel(Level.WARNING);
      assertThat(uimaLogger.isLoggable(Level.WARNING)).isTrue();
      assertThat(uimaLogger.isLoggable(Level.SEVERE)).isTrue();
      assertThat(uimaLogger.isLoggable(Level.INFO)).isFalse();
      assertThat(logger.isLoggable(Level.WARNING)).isTrue();
      assertThat(logger.isLoggable(Level.SEVERE)).isTrue();
      assertThat(logger.isLoggable(Level.INFO)).isFalse();

      // set level to FINE
      uimaLogger.setLevel(Level.FINE);
      assertThat(uimaLogger.isLoggable(Level.WARNING)).isTrue();
      assertThat(uimaLogger.isLoggable(Level.SEVERE)).isTrue();
      assertThat(uimaLogger.isLoggable(Level.INFO)).isTrue();
      assertThat(uimaLogger.isLoggable(Level.FINER)).isFalse();
      assertThat(uimaLogger.isLoggable(Level.ALL)).isFalse();
      assertThat(logger.isLoggable(Level.WARNING)).isTrue();
      assertThat(logger.isLoggable(Level.SEVERE)).isTrue();
      assertThat(logger.isLoggable(Level.INFO)).isTrue();
      assertThat(logger.isLoggable(Level.FINER)).isFalse();
      assertThat(logger.isLoggable(Level.ALL)).isFalse();
    } finally {
      uimaLogger.setLevel(Level.INFO); // otherwise, is stuck at INFO, too much logging
    }
  }
}
