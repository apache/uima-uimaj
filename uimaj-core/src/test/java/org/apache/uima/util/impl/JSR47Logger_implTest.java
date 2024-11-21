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

import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.uima.util.Level;
import org.junit.jupiter.api.Test;

class JSR47Logger_implTest {

  private static final HashMap<String, Level> logLevels = new HashMap<>(9);

  static {
    logLevels.put("OFF", Level.OFF);
    logLevels.put("SEVERE", Level.SEVERE);
    logLevels.put("WARNING", Level.WARNING);
    logLevels.put("INFO", Level.INFO);
    logLevels.put("CONFIG", Level.CONFIG);
    logLevels.put("FINE", Level.FINE);
    logLevels.put("FINER", Level.FINER);
    logLevels.put("FINEST", Level.FINEST);
    logLevels.put("ALL", Level.ALL);
  }

  @Test
  void testLogWrapperCreation() throws Exception {

    // Set the root logger's level to INFO ... may not be the default
    java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.INFO);

    try {
      var uimaLogger = JSR47Logger_impl.getInstance();
      var classLogger = JSR47Logger_impl.getInstance(this.getClass());
      uimaLogger.setLevel(null); // causes it to inherit from above
      classLogger.setLevel(null); // causes it to inherit from above

      // check base configuration
      assertThat(uimaLogger).isNotNull();
      assertThat(classLogger).isNotNull();
      assertThat(uimaLogger.isLoggable(Level.INFO)).isTrue();
      assertThat(classLogger.isLoggable(Level.INFO)).isTrue();
    } finally {
      java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.INFO);

    }
  }

  @Test
  void testIsLoggable() throws Exception {
    // create logger
    var uimaLogger = JSR47Logger_impl.getInstance();
    var classLogger = JSR47Logger_impl.getInstance(this.getClass());
    // get uimaLogger log level, get parent logger of "org.apache.uima" until we have the
    // JSR47 root logger that defines the default log level
    Logger jsrLogger = java.util.logging.Logger.getLogger("org.apache.uima");
    while (jsrLogger.getLevel() == null) {
      jsrLogger = jsrLogger.getParent();
    }
    Level defaultLogLevel = logLevels.get(jsrLogger.getLevel().toString());

    try {
      uimaLogger.setLevel(null); // causes it to inherit from above
      classLogger.setLevel(null); // causes it to inherit from above

      // check message logging for root logger based on default log level
      assertThat(uimaLogger.isLoggable(Level.ALL))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.ALL));
      assertThat(uimaLogger.isLoggable(Level.FINEST))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.FINEST));
      assertThat(uimaLogger.isLoggable(Level.FINER))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.FINER));
      assertThat(uimaLogger.isLoggable(Level.FINE))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.FINE));
      assertThat(uimaLogger.isLoggable(Level.CONFIG))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.CONFIG));
      assertThat(uimaLogger.isLoggable(Level.INFO))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.INFO));
      assertThat(uimaLogger.isLoggable(Level.WARNING))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.WARNING));
      assertThat(uimaLogger.isLoggable(Level.SEVERE))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.SEVERE));
      assertThat(uimaLogger.isLoggable(Level.OFF))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.OFF));

      // check message logging for class logger based on default log level
      assertThat(classLogger.isLoggable(Level.ALL))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.ALL));
      assertThat(classLogger.isLoggable(Level.FINEST))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.FINEST));
      assertThat(classLogger.isLoggable(Level.FINER))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.FINER));
      assertThat(classLogger.isLoggable(Level.FINE))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.FINE));
      assertThat(classLogger.isLoggable(Level.CONFIG))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.CONFIG));
      assertThat(classLogger.isLoggable(Level.INFO))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.INFO));
      assertThat(classLogger.isLoggable(Level.WARNING))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.WARNING));
      assertThat(classLogger.isLoggable(Level.SEVERE))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.SEVERE));
      assertThat(classLogger.isLoggable(Level.OFF))
              .isEqualTo(defaultLogLevel.isGreaterOrEqual(Level.OFF));

      // reset class logger level to OFF
      // Logger.getLogger(this.getClass().getName()).setLevel(java.util.logging.Level.OFF);
      classLogger.setLevel(Level.OFF);
      assertThat(classLogger.isLoggable(Level.ALL)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINEST)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINER)).isFalse();
      assertThat(classLogger.isLoggable(Level.FINE)).isFalse();
      assertThat(classLogger.isLoggable(Level.CONFIG)).isFalse();
      assertThat(classLogger.isLoggable(Level.INFO)).isFalse();
      assertThat(classLogger.isLoggable(Level.WARNING)).isFalse();
      assertThat(classLogger.isLoggable(Level.SEVERE)).isFalse();
      assertThat(classLogger.isLoggable(Level.OFF)).isFalse();

      // reset class logger level to ALL
      // Logger.getLogger(this.getClass().getName()).setLevel(java.util.logging.Level.ALL);
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
      // Logger.getLogger(this.getClass().getName()).setLevel(java.util.logging.Level.WARNING);
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
      // reset log level to default log level
      classLogger.setLevel(Level.INFO);
      uimaLogger.setLevel(Level.INFO);
    }
  }

  @Test
  void testMessageLogMethods() throws Exception {
    // create Logger
    final org.apache.uima.util.Logger logger = JSR47Logger_impl.getInstance();
    // reset log level to INFO
    try {
      logger.setLevel(Level.INFO);

      // File file = File.createTempFile("LoggingTest","log");
      // file.deleteOnExit();

      // change output temporary file
      // logger.setOutputStream(new PrintStream(new FileOutputStream(file)));

      // log test with method log(Level,String)
      logger.log(Level.INFO, "My first test message");
      logger.log(Level.INFO, "");
      logger.log(Level.INFO, null);

      // log test with method log(Level,String,Object)
      Object obj = null;
      logger.log(Level.INFO, "My {0} test message", "second");
      logger.log(Level.INFO, "My {0} test message", new Object());
      logger.log(Level.INFO, "My {0} test message", "");
      logger.log(Level.INFO, "My {0} test message", obj);
      logger.log(Level.INFO, "", "");
      logger.log(Level.INFO, null, "");

      // log test with method log(Level,String,Object[])
      logger.log(Level.INFO, "My {0} test message", new Object[] { "third" });
      logger.log(Level.INFO, "My {0} test message", new Object[] {});
      logger.log(Level.INFO, "", new Object[] { "" });
      logger.log(Level.INFO, "", new Object[] { null });
      logger.log(Level.INFO, "My {0} test message", new Object[] { "" });
      logger.log(Level.INFO, "My {0} test message", new Object[] { null });
      logger.log(Level.INFO, null, "");

      // log test with method log(Level,String,Throwable)
      logger.setLevel(Level.WARNING); // Don't log the expected exceptions
      Throwable thrown = new Throwable();
      logger.log(Level.INFO, "My fourth test message", thrown);
      logger.log(Level.INFO, "", thrown);
      logger.log(Level.INFO, null, thrown);
      thrown = null;
      logger.log(Level.INFO, "My fourth test message", thrown);

      new Runnable() {
        @Override
        public void run() {
          logger.log(getClass().getName(), Level.INFO, "Message from wrapper", null);
        }
      }.run();

      // test deprecated log method
      logger.log("My fifth test message");
      logger.log("");
      logger.log(null);

      // test deprecated logException method
      Exception ex = new Exception("My sixth test message");
      logger.logException(ex);
      logger.logException(null);
    } finally {
      logger.setLevel(Level.INFO);
    }
  }

  @Test
  void testMessageKeyLogMethods() throws Exception {
    // create Logger
    org.apache.uima.util.Logger logger = JSR47Logger_impl.getInstance();

    try {
      // reset log level to INFO
      logger.setLevel(Level.INFO);

      // File file = File.createTempFile("LoggingTest","log");
      // file.deleteOnExit();

      // change output temporary file
      // logger.setOutputStream(new PrintStream(new FileOutputStream(file)));

      // test deprecated log(String, String, Object[])
      String msgKey = "UIMA_logger_test";
      String bundle = "org.apache.uima.util.impl.logger_test_messages";
      logger.log(bundle, msgKey, new Object[] { "message key test" });
      logger.log(bundle, null, new Object[] { "message key test" });
      logger.log(bundle, msgKey, new Object[] { "" });
      logger.log(bundle, msgKey, new Object[] { null });

      // test method logrb(Level, String, String, String, String)
      logger.logrb(Level.INFO, null, null, bundle, msgKey);
      logger.logrb(Level.INFO, null, null, bundle, null);
      logger.logrb(Level.INFO, null, null, null, msgKey);
      logger.logrb(Level.INFO, null, null, null, null);
      logger.logrb(Level.INFO, "testClass", "testMethod", bundle, msgKey);
      logger.logrb(Level.INFO, "testClass", "testMethod", null, null);

      // test method logrb(Level, String, String, String, String, Object)
      Object obj = null;
      logger.logrb(Level.INFO, null, null, bundle, msgKey, new Object());
      logger.logrb(Level.INFO, null, null, bundle, msgKey, "message key test");
      logger.logrb(Level.INFO, null, null, bundle, null, "message key test");
      logger.logrb(Level.INFO, null, null, null, msgKey, "");
      logger.logrb(Level.INFO, null, null, null, null, "");
      logger.logrb(Level.INFO, "testClass", "testMethod", bundle, msgKey, obj);
      logger.logrb(Level.INFO, "testClass", "testMethod", null, null, obj);

      // test method logrb(Level, String, String, String, String, Object[])
      Object[] objects = null;
      logger.logrb(Level.INFO, null, null, bundle, msgKey, new Object[] {});
      logger.logrb(Level.INFO, null, null, bundle, null, new Object[] { "message key test" });
      logger.logrb(Level.INFO, null, null, null, msgKey, new Object[] { "" });
      logger.logrb(Level.INFO, null, null, null, null, new Object[] { "" });
      logger.logrb(Level.INFO, null, null, null, null, new Object[] { null });
      logger.logrb(Level.INFO, "testClass", "testMethod", bundle, msgKey, objects);
      logger.logrb(Level.INFO, "testClass", "testMethod", null, null, objects);

      // test method logrb(Level, String, String, String, String, thrown)
      Throwable thrown = new Throwable();
      logger.setLevel(Level.WARNING); // Don't log the expected exceptions
      logger.logrb(Level.INFO, null, null, bundle, msgKey, thrown);
      logger.logrb(Level.INFO, null, null, bundle, null, thrown);
      logger.logrb(Level.INFO, null, null, null, msgKey, thrown);
      logger.logrb(Level.INFO, null, null, null, null, thrown);
      thrown = null;
      logger.logrb(Level.INFO, "testClass", "testMethod", bundle, msgKey, thrown);
      logger.logrb(Level.INFO, "testClass", "testMethod", null, null, thrown);

      // https://issues.apache.org/jira/browse/UIMA-5719
      logger.logrb(Level.WARNING, "testClass", "testMethod", "org.apache.uima.impl.log_messages",
              "UIMA_external_override_ignored__CONFIG", new Object[] { "n1", "${abc}" });
    } finally {
      logger.setLevel(Level.INFO);
    }
  }
}
