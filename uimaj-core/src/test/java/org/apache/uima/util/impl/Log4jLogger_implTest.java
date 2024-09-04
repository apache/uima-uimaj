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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * UIMA Logging Test
 */
class Log4jLogger_implTest {

  @BeforeEach
  void setUp() throws Exception {
    // BasicConfigurator.configure();
  }

  @AfterEach
  void tearDown() throws Exception {
    // BasicConfigurator.resetConfiguration();
  }

  // private static HashMap<String, Level> logLevels = new HashMap<String, Level>(
  // 9);
  // static {
  // logLevels.put("OFF", Level.OFF);
  // logLevels.put("ERROR", Level.SEVERE);
  // logLevels.put("WARN", Level.WARNING);
  // logLevels.put("INFO", Level.INFO);
  // logLevels.put("INFO", Level.CONFIG);
  // logLevels.put("DEBUG", Level.FINE);
  // logLevels.put("ALL", Level.FINER);
  // logLevels.put("ALL", Level.FINEST);
  // logLevels.put("ALL", Level.ALL);
  // }

  @Test
  void testLogWrapperCreation() throws Exception {
    var uimaLogger = Log4jLogger_impl.getInstance();
    var classLogger = Log4jLogger_impl.getInstance(this.getClass());

    // check base configuration
    assertThat(uimaLogger).isNotNull();
    assertThat(classLogger).isNotNull();
    assertThat(uimaLogger.isLoggable(Level.INFO)).isTrue();
    assertThat(classLogger.isLoggable(Level.INFO)).isTrue();
    classLogger.log("ola");
    classLogger.log(Level.INFO, "OLA in info");

    uimaLogger.log(Level.INFO, "UIMA OLA in info");
    https: // issues.apache.org/jira/browse/UIMA-5719
    uimaLogger.logrb(Level.WARNING, "testClass", "testMethod", "org.apache.uima.impl.log_messages",
            "UIMA_external_override_ignored__CONFIG", new Object[] { "n1", "${abc}" });
  }

  @Test
  void testIsLoggable() throws Exception {
    // create logger
    org.apache.uima.util.Logger uimaLogger = null;
    try {
      uimaLogger = Log4jLogger_impl.getInstance();
    } catch (Exception e) {
      System.err.println(e);
      e.printStackTrace(System.err);
      throw e;
    }
    var classLogger = Log4jLogger_impl.getInstance(this.getClass());

    assertThat(uimaLogger).isNotNull();
    assertThat(classLogger).isNotNull();
    var log4jLogger = org.apache.logging.log4j.LogManager.getLogger("org.apache.uima");
    while (log4jLogger.getLevel() == null) {
      log4jLogger = LogManager.getRootLogger();
    }

    var key = "INFO"; // log4jLogger.getLevel().toString();

    var defaultLogLevel = Level.INFO; // logLevels.get(key); // doesn't work

    assertThat(defaultLogLevel).isNotNull();
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
    assertThat(classLogger.isLoggable(Level.OFF)).isTrue();

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

    // reset log level to default log level
    classLogger.setLevel(defaultLogLevel);
  }

  @Test
  void testMessageLogMethods() throws Exception {
    try (var capture = new Log4JMessageCapture()) {
      org.apache.uima.util.Logger tempLogger = null;
      try {
        tempLogger = Log4jLogger_impl.getInstance(getClass());
      } catch (Throwable e) {
        System.err.println("debug Caught throwable");
        e.printStackTrace(System.err);
        System.err.println("debug finished stacktrace");
        throw e;
      }

      final var logger = tempLogger;
      logger.setLevel(Level.INFO);

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
      var thrown = new Throwable();
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
      var ex = new Exception("My sixth test message");
      logger.logException(ex);
      logger.logException(null);

      // all calls except those with null or "" msgs (including non-null throwable/exception)
      assertThat(capture.getAllEvents().size()).isEqualTo(16);

      // https://issues.apache.org/jira/browse/UIMA-5719
      logger.logrb(Level.WARNING, "testClass", "testMethod", "org.apache.uima.impl.log_messages",
              "UIMA_external_override_ignored__CONFIG", new Object[] { "n1", "${abc}" });
    }
  }

  @Test
  void testMessageKeyLogMethods() throws Exception {
    final var nbrcalls = new int[1];
    nbrcalls[0] = 0;

    // Tell the logger to log everything
    var rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
            .getRootLogger();
    // create Logger
    var logger = Log4jLogger_impl.getInstance();

    try {
      rootLogger.get().setLevel(org.apache.logging.log4j.Level.ALL);
      rootLogger.getContext().updateLoggers();
      // Appender appender = (Appender) rootLogger.getAllAppenders().nextElement();
      // Capture the logging output without actually logging it
      // appender.addFilter(new org.apache.logging.log4j.spi.Filter() {

      Filter filter = new AbstractFilter() {
        @Override
        public Result filter(LogEvent event) {
          nbrcalls[0]++;
          var ste = event.getSource();
          System.out.printf("[%s:%s] %s%n", ste.getFileName(), ste.getLineNumber(),
                  event.getMessage().getFormattedMessage());
          assertThat(ste.getFileName())
                  .isEqualTo(Log4jLogger_implTest.this.getClass().getSimpleName() + ".java");
          return Result.DENY;
        }
      };

      var app = (ConsoleAppender) rootLogger.get().getAppenders().values().stream().findFirst()
              .get();
      app.addFilter(filter);
      try {
        // @Override
        // public int decide(LoggingEvent event) {
        // nbrcalss[0] ++;
        // LocationInfo l = event.getLocationInformation();
        // System.out.printf("[%s:%s] %s%n", l.getFileName(), l.getLineNumber(),
        // event.getMessage());
        // assertEquals(TestLog4jLogger_impl.this.getClass().getSimpleName()+".java",
        // l.getFileName());
        // return org.apache.logging.log4j.spi.Filter.DENY;
        // }
        // });

        // reset log level to INFO
        logger.setLevel(Level.INFO);

        // File file = File.createTempFile("LoggingTest","log");
        // file.deleteOnExit();

        // change output temporary file
        // logger.setOutputStream(new PrintStream(new FileOutputStream(file)));

        // test deprecated log(String, String, Object[])
        var msgKey = "UIMA_logger_test";
        var bundle = "org.apache.uima.util.impl.logger_test_messages";
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
        var thrown = new Throwable();
        logger.logrb(Level.INFO, null, null, bundle, msgKey, thrown);
        logger.logrb(Level.INFO, null, null, bundle, null, thrown);
        logger.logrb(Level.INFO, null, null, null, msgKey, thrown);
        logger.logrb(Level.INFO, null, null, null, null, thrown);
        thrown = null;
        logger.logrb(Level.INFO, "testClass", "testMethod", bundle, msgKey, thrown);
        logger.logrb(Level.INFO, "testClass", "testMethod", null, null, thrown);

        assertThat(nbrcalls[0]).isEqualTo(18);
      } finally {
        app.removeFilter(filter); // otherwise, subsequent test's filter gets appended, not replace
      }

    } finally {
      logger.setLevel(Level.INFO);
      rootLogger.setLevel(org.apache.logging.log4j.Level.INFO);
    }
  }

  @Test
  void testLoggerFromUIMAFramework() {
    var logger = UIMAFramework.getLogger(this.getClass());

    logger.setLevel(Level.INFO);

    // File file = File.createTempFile("LoggingTest","log");
    // file.deleteOnExit();

    // change output temporary file
    // logger.setOutputStream(new PrintStream(new FileOutputStream(file)));

    // log test with method log(Level,String)
    logger.log(Level.INFO, "------------------------------------------------------------");
    logger.log(Level.INFO, "My first test message");
    logger.log(Level.INFO, "message with \"{0}\"", "substitute");
    logger.info("message with \"{}\"", "substitute"); // new logger style
    logger.info("message with \"{}\"", new Object[] { "substitute" }); // new logger style

    // https://issues.apache.org/jira/browse/UIMA-5719
    logger.logrb(Level.WARNING, "testClass", "testMethod", "org.apache.uima.impl.log_messages",
            "UIMA_external_override_ignored__CONFIG", new Object[] { "n1", "${abc}" });
  }
}
