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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;

/**
 * UIMA Logging Test
 */
public class TestLog4jLogger_impl extends TestCase {

   @Override
  public void setUp() throws Exception {
      BasicConfigurator.configure();
   }

   @Override
  public void tearDown() throws Exception {
      BasicConfigurator.resetConfiguration();
   }

   private static HashMap<String, Level> logLevels = new HashMap<String, Level>(
         9);
   static {
      logLevels.put("OFF", Level.OFF);
      logLevels.put("ERROR", Level.SEVERE);
      logLevels.put("WARN", Level.WARNING);
      logLevels.put("INFO", Level.INFO);
      logLevels.put("INFO", Level.CONFIG);
      logLevels.put("DEBUG", Level.FINE);
      logLevels.put("ALL", Level.FINER);
      logLevels.put("ALL", Level.FINEST);
      logLevels.put("ALL", Level.ALL);
   }

   public void testLogWrapperCreation() throws Exception {
      org.apache.uima.util.Logger uimaLogger = Log4jLogger_impl.getInstance();
      org.apache.uima.util.Logger classLogger = Log4jLogger_impl
            .getInstance(this.getClass());

      // check base configuration
      assertNotNull(uimaLogger);
      assertNotNull(classLogger);
      assertTrue(uimaLogger.isLoggable(Level.INFO));
      assertTrue(classLogger.isLoggable(Level.INFO));
      classLogger.log("ola");
      classLogger.log(Level.INFO, "OLA in info");

      uimaLogger.log(Level.INFO, "UIMA OLA in info");
   }

   public void testIsLoggable() throws Exception {
      // create logger
      org.apache.uima.util.Logger uimaLogger = Log4jLogger_impl.getInstance();
      org.apache.uima.util.Logger classLogger = Log4jLogger_impl
            .getInstance(this.getClass());

      assertNotNull(uimaLogger);
      assertNotNull(classLogger);
      Logger log4jLogger = org.apache.log4j.Logger.getLogger("org.apache.uima");
      while (log4jLogger.getLevel() == null) {
         log4jLogger = Logger.getRootLogger();
      }

      String key = log4jLogger.getLevel().toString();

      Level defaultLogLevel = logLevels.get(key);

      assertNotNull(defaultLogLevel);
      // check message logging for root logger based on default log level
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.ALL), uimaLogger
            .isLoggable(Level.ALL));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.FINEST), uimaLogger
            .isLoggable(Level.FINEST));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.FINER), uimaLogger
            .isLoggable(Level.FINER));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.FINE), uimaLogger
            .isLoggable(Level.FINE));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.CONFIG), uimaLogger
            .isLoggable(Level.CONFIG));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.INFO), uimaLogger
            .isLoggable(Level.INFO));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.WARNING), uimaLogger
            .isLoggable(Level.WARNING));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.SEVERE), uimaLogger
            .isLoggable(Level.SEVERE));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.OFF), uimaLogger
            .isLoggable(Level.OFF));

      // check message logging for class logger based on default log level
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.ALL), classLogger
            .isLoggable(Level.ALL));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.FINEST), classLogger
            .isLoggable(Level.FINEST));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.FINER), classLogger
            .isLoggable(Level.FINER));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.FINE), classLogger
            .isLoggable(Level.FINE));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.CONFIG), classLogger
            .isLoggable(Level.CONFIG));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.INFO), classLogger
            .isLoggable(Level.INFO));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.WARNING), classLogger
            .isLoggable(Level.WARNING));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.SEVERE), classLogger
            .isLoggable(Level.SEVERE));
      assertEquals(defaultLogLevel.isGreaterOrEqual(Level.OFF), classLogger
            .isLoggable(Level.OFF));

      // reset class logger level to OFF
      // Logger.getLogger(this.getClass().getName()).setLevel(java.util.logging.Level.OFF);
      classLogger.setLevel(Level.OFF);
      assertFalse(classLogger.isLoggable(Level.ALL));
      assertFalse(classLogger.isLoggable(Level.FINEST));
      assertFalse(classLogger.isLoggable(Level.FINER));
      assertFalse(classLogger.isLoggable(Level.FINE));
      assertFalse(classLogger.isLoggable(Level.CONFIG));
      assertFalse(classLogger.isLoggable(Level.INFO));
      assertFalse(classLogger.isLoggable(Level.WARNING));
      assertFalse(classLogger.isLoggable(Level.SEVERE));
      assertTrue(classLogger.isLoggable(Level.OFF));

      // reset class logger level to ALL
      // Logger.getLogger(this.getClass().getName()).setLevel(java.util.logging.Level.ALL);
      classLogger.setLevel(Level.ALL);
      assertTrue(classLogger.isLoggable(Level.ALL));
      assertTrue(classLogger.isLoggable(Level.FINEST));
      assertTrue(classLogger.isLoggable(Level.FINER));
      assertTrue(classLogger.isLoggable(Level.FINE));
      assertTrue(classLogger.isLoggable(Level.CONFIG));
      assertTrue(classLogger.isLoggable(Level.INFO));
      assertTrue(classLogger.isLoggable(Level.WARNING));
      assertTrue(classLogger.isLoggable(Level.SEVERE));
      assertTrue(classLogger.isLoggable(Level.OFF));

      // reset class logger level to WARNING
      // Logger.getLogger(this.getClass().getName()).setLevel(java.util.logging.Level.WARNING);
      classLogger.setLevel(Level.WARNING);
      assertFalse(classLogger.isLoggable(Level.ALL));
      assertFalse(classLogger.isLoggable(Level.FINEST));
      assertFalse(classLogger.isLoggable(Level.FINER));
      assertFalse(classLogger.isLoggable(Level.FINE));
      assertFalse(classLogger.isLoggable(Level.CONFIG));
      assertFalse(classLogger.isLoggable(Level.INFO));
      assertTrue(classLogger.isLoggable(Level.WARNING));
      assertTrue(classLogger.isLoggable(Level.SEVERE));
      assertTrue(classLogger.isLoggable(Level.OFF));

      // reset log level to default log level
      classLogger.setLevel(defaultLogLevel);
   }

   public void testMessageLogMethods() throws Exception {
     final List<LoggingEvent> records = new ArrayList<LoggingEvent>();
     
     // Tell the logger to log everything
     Logger rootLogger = org.apache.log4j.LogManager.getRootLogger();
     rootLogger.setLevel(org.apache.log4j.Level.ALL);
     Appender appender = (Appender) rootLogger.getAllAppenders().nextElement();
     // Capture the logging output without actually logging it
     appender.addFilter(new org.apache.log4j.spi.Filter() {
       @Override
       public int decide(LoggingEvent event) {
         records.add(event);
         LocationInfo l = event.getLocationInformation();
         System.out.printf("[%s:%s] %s%n", l.getFileName(), l.getLineNumber(), event.getMessage());
         assertEquals(TestLog4jLogger_impl.this.getClass().getSimpleName()+".java", 
                 l.getFileName());
         return org.apache.log4j.spi.Filter.DENY;
       }
     }); 
     
      // create Logger
      final org.apache.uima.util.Logger logger = Log4jLogger_impl.getInstance(getClass());
      // reset log level to INFO
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
      Throwable thrown = new Throwable();
      logger.log(Level.INFO, "My fourth test message", thrown);
      logger.log(Level.INFO, "", thrown);
      logger.log(Level.INFO, null, thrown);
      thrown = null;
      logger.log(Level.INFO, "My fourth test message", thrown);

      new Runnable() {
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
      
      assertEquals(16, records.size());  // all calls except those with null or "" msgs (including non-null throwable/exception)
   }

   public void testMessageKeyLogMethods() throws Exception {
     final List<LoggingEvent> records = new ArrayList<LoggingEvent>();
     
     // Tell the logger to log everything
     Logger rootLogger = org.apache.log4j.LogManager.getRootLogger();
     rootLogger.setLevel(org.apache.log4j.Level.ALL);
     Appender appender = (Appender) rootLogger.getAllAppenders().nextElement();
     // Capture the logging output without actually logging it
     appender.addFilter(new org.apache.log4j.spi.Filter() {
       @Override
       public int decide(LoggingEvent event) {
         records.add(event);
         LocationInfo l = event.getLocationInformation();
         System.out.printf("[%s:%s] %s%n", l.getFileName(), l.getLineNumber(), event.getMessage());
         assertEquals(TestLog4jLogger_impl.this.getClass().getSimpleName()+".java", 
                 l.getFileName());
         return org.apache.log4j.spi.Filter.DENY;
       }
     }); 

      // create Logger
      org.apache.uima.util.Logger logger = Log4jLogger_impl.getInstance();
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
      logger.logrb(Level.INFO, null, null, bundle, null,
            new Object[] { "message key test" });
      logger.logrb(Level.INFO, null, null, null, msgKey, new Object[] { "" });
      logger.logrb(Level.INFO, null, null, null, null, new Object[] { "" });
      logger.logrb(Level.INFO, null, null, null, null, new Object[] { null });
      logger.logrb(Level.INFO, "testClass", "testMethod", bundle, msgKey,
            objects);
      logger.logrb(Level.INFO, "testClass", "testMethod", null, null, objects);

      // test method logrb(Level, String, String, String, String, thrown)
      Throwable thrown = new Throwable();
      logger.logrb(Level.INFO, null, null, bundle, msgKey, thrown);
      logger.logrb(Level.INFO, null, null, bundle, null, thrown);
      logger.logrb(Level.INFO, null, null, null, msgKey, thrown);
      logger.logrb(Level.INFO, null, null, null, null, thrown);
      thrown = null;
      logger.logrb(Level.INFO, "testClass", "testMethod", bundle, msgKey,
            thrown);
      logger.logrb(Level.INFO, "testClass", "testMethod", null, null, thrown);

      assertEquals(18, records.size());
   }

   public void testLoggerFromUIMAFramework() {
      org.apache.uima.util.Logger logger = UIMAFramework.getLogger(this
            .getClass());

      logger.setLevel(Level.INFO);

      // File file = File.createTempFile("LoggingTest","log");
      // file.deleteOnExit();

      // change output temporary file
      // logger.setOutputStream(new PrintStream(new FileOutputStream(file)));

      // log test with method log(Level,String)
      logger.log(Level.INFO,
            "------------------------------------------------------------");
      logger.log(Level.INFO, "My first test message");
      logger.log(Level.INFO, "");
      logger.log(Level.INFO, null);

   }
}
