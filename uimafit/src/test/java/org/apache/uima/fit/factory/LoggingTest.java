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
package org.apache.uima.fit.factory;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.component.CasConsumer_ImplBase;
import org.apache.uima.fit.component.CasFlowController_ImplBase;
import org.apache.uima.fit.component.CasMultiplier_ImplBase;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.component.JCasFlowController_ImplBase;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.internal.ExtendedLogger;
import org.apache.uima.flow.Flow;
import org.apache.uima.impl.RootUimaContext_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Progress;
import org.apache.uima.util.impl.JSR47Logger_impl;
import org.apache.uima.util.impl.Log4jLogger_impl;
import org.junit.Test;

public class LoggingTest {
  @Test
  public void testJSR47Logger() throws Exception {
    final List<LogRecord> records = new ArrayList<LogRecord>();

    // Tell the logger to log everything
    ConsoleHandler handler = (ConsoleHandler) LogManager.getLogManager().getLogger("")
            .getHandlers()[0];
    java.util.logging.Level oldLevel = handler.getLevel();
    handler.setLevel(Level.ALL);
    // Capture the logging output without actually logging it
    handler.setFilter(new Filter() {
      public boolean isLoggable(LogRecord record) {
        records.add(record);
        System.out.printf("[%s] %s%n", record.getSourceClassName(), record.getMessage());
        return false;
      }
    });

    UIMAFramework.getLogger().setLevel(org.apache.uima.util.Level.INFO);

    try {
      UimaContextAdmin ctx = new RootUimaContext_impl();
      ctx.setLogger(JSR47Logger_impl.getInstance());
      ExtendedLogger logger = new ExtendedLogger(ctx);
           
      logger.setLevel(org.apache.uima.util.Level.ALL);
      trigger(logger);
      logger.setLevel(org.apache.uima.util.Level.OFF);
      trigger(logger);

      assertEquals(10, records.size());
      assertEquals(Level.FINER, records.get(0).getLevel());
      assertEquals(Level.FINER, records.get(1).getLevel());
      assertEquals(Level.FINE, records.get(2).getLevel());
      assertEquals(Level.FINE, records.get(3).getLevel());
      assertEquals(Level.INFO, records.get(4).getLevel());
      assertEquals(Level.INFO, records.get(5).getLevel());
      assertEquals(Level.WARNING, records.get(6).getLevel());
      assertEquals(Level.WARNING, records.get(7).getLevel());
      assertEquals(Level.SEVERE, records.get(8).getLevel());
      assertEquals(Level.SEVERE, records.get(9).getLevel());
    } finally {
      if (oldLevel != null) {
        handler.setLevel(oldLevel);
        handler.setFilter(null);
      }
    }
  }

  @Test
  public void testLog4JLogger() throws Exception {
    final List<LoggingEvent> records = new ArrayList<LoggingEvent>();

    BasicConfigurator.configure();

    // Tell the logger to log everything
    Logger rootLogger = org.apache.log4j.LogManager.getRootLogger();
    org.apache.log4j.Level oldLevel = rootLogger.getLevel();
    rootLogger.setLevel(org.apache.log4j.Level.ALL);
    Appender appender = (Appender) rootLogger.getAllAppenders().nextElement();
    // Capture the logging output without actually logging it
    appender.addFilter(new org.apache.log4j.spi.Filter() {
      @Override
      public int decide(LoggingEvent event) {
        records.add(event);
        LocationInfo l = event.getLocationInformation();
        System.out.printf("[%s:%s] %s%n", l.getFileName(), l.getLineNumber(), event.getMessage());
        return org.apache.log4j.spi.Filter.DENY;
      }
    });

    try {
      UimaContextAdmin ctx = new RootUimaContext_impl();
      ctx.setLogger(Log4jLogger_impl.getInstance());
      ExtendedLogger logger = new ExtendedLogger(ctx);

      logger.setLevel(org.apache.uima.util.Level.ALL);
      trigger(logger);
      logger.setLevel(org.apache.uima.util.Level.OFF);
      trigger(logger);

      assertEquals(10, records.size());
      assertEquals(org.apache.log4j.Level.ALL, records.get(0).getLevel());
      assertEquals(org.apache.log4j.Level.ALL, records.get(1).getLevel());
      assertEquals(org.apache.log4j.Level.DEBUG, records.get(2).getLevel());
      assertEquals(org.apache.log4j.Level.DEBUG, records.get(3).getLevel());
      assertEquals(org.apache.log4j.Level.INFO, records.get(4).getLevel());
      assertEquals(org.apache.log4j.Level.INFO, records.get(5).getLevel());
      assertEquals(org.apache.log4j.Level.WARN, records.get(6).getLevel());
      assertEquals(org.apache.log4j.Level.WARN, records.get(7).getLevel());
      assertEquals(org.apache.log4j.Level.ERROR, records.get(8).getLevel());
      assertEquals(org.apache.log4j.Level.ERROR, records.get(9).getLevel());
    } finally {
      if (oldLevel != null) {
        rootLogger.setLevel(oldLevel);
        appender.clearFilters();
      }
    }
  }

  private void trigger(ExtendedLogger aLogger) {
    if (aLogger.isTraceEnabled()) {
      aLogger.trace("Logging: " + getClass().getName());
      aLogger.trace("Logging: " + getClass().getName(), new IllegalArgumentException());
    }
    if (aLogger.isDebugEnabled()) {
      aLogger.debug("Logging: " + getClass().getName());
      aLogger.debug("Logging: " + getClass().getName(), new IllegalArgumentException());
    }
    if (aLogger.isInfoEnabled()) {
      aLogger.info("Logging: " + getClass().getName());
      aLogger.info("Logging: " + getClass().getName(), new IllegalArgumentException());
    }
    if (aLogger.isWarnEnabled()) {
      aLogger.warn("Logging: " + getClass().getName());
      aLogger.warn("Logging: " + getClass().getName(), new IllegalArgumentException());
    }
    if (aLogger.isErrorEnabled()) {
      aLogger.error("Logging: " + getClass().getName());
      aLogger.error("Logging: " + getClass().getName(), new IllegalArgumentException());
    }
  }

  @Test
  public void testAllKindsOfComponents() throws Exception {
    System.out.println("=== testAllKindsOfComponents ===");
    final List<LogRecord> records = new ArrayList<LogRecord>();

    // Tell the logger to log everything
    ConsoleHandler handler = (ConsoleHandler) LogManager.getLogManager().getLogger("")
            .getHandlers()[0];
    java.util.logging.Level oldLevel = handler.getLevel();
    handler.setLevel(Level.ALL);
    // Capture the logging output without actually logging it
    handler.setFilter(new Filter() {
      public boolean isLoggable(LogRecord record) {
        records.add(record);
        System.out.printf("[%s] %s%n", record.getSourceClassName(), record.getMessage());
        return false;
      }
    });
    
    UIMAFramework.getLogger().setLevel(org.apache.uima.util.Level.INFO);

    try {
      JCas jcas = JCasFactory.createJCas();

      createReader(LoggingCasCollectionReader.class).hasNext();
      assertLogDone(records);

      createReader(LoggingJCasCollectionReader.class).hasNext();
      assertLogDone(records);

      // createFlowControllerDescription(LoggingJCasFlowController.class).
      // assertLogDone(records);

      createEngine(LoggingCasAnnotator.class).process(jcas.getCas());
      assertLogDone(records);

      createEngine(LoggingJCasAnnotator.class).process(jcas);
      assertLogDone(records);

      createEngine(LoggingCasConsumer.class).process(jcas.getCas());
      assertLogDone(records);

      createEngine(LoggingJCasConsumer.class).process(jcas);
      assertLogDone(records);

      createEngine(LoggingCasMultiplier.class).process(jcas.getCas());
      assertLogDone(records);

      createEngine(LoggingJCasMultiplier.class).process(jcas);
      assertLogDone(records);
    } finally {
      if (oldLevel != null) {
        handler.setLevel(oldLevel);
        handler.setFilter(null);
      }
    }
  }

  private void assertLogDone(List<LogRecord> records) {
    assertEquals(1, records.size());
    assertEquals(Level.INFO, records.get(0).getLevel());
    records.clear();
  }

  public static class LoggingCasMultiplier extends CasMultiplier_ImplBase {

    public boolean hasNext() throws AnalysisEngineProcessException {
      getLogger().info("Logging: " + getClass().getName());
      return false;
    }

    public AbstractCas next() throws AnalysisEngineProcessException {
      // Never called
      return null;
    }

    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      // Never called
    }
  }

  public static class LoggingJCasMultiplier extends JCasMultiplier_ImplBase {
    public boolean hasNext() throws AnalysisEngineProcessException {
      getLogger().info("Logging: " + getClass().getName());
      return false;
    }

    public AbstractCas next() throws AnalysisEngineProcessException {
      // Never called
      return null;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      // Never called
    }
  }

  public static class LoggingJCasCollectionReader extends JCasCollectionReader_ImplBase {
    public boolean hasNext() throws IOException, CollectionException {
      getLogger().info("Logging: " + getClass().getName());
      return false;
    }

    public Progress[] getProgress() {
      return new Progress[0];
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
      // Never called
    }
  }

  public static class LoggingResource extends Resource_ImplBase {
    @Override
    public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
            throws ResourceInitializationException {
      boolean ret = super.initialize(aSpecifier, aAdditionalParams);
      getLogger().info("Logging: " + getClass().getName());
      return ret;
    }
  }

  public static class LoggingCasCollectionReader extends CasCollectionReader_ImplBase {
    public void getNext(CAS aCAS) throws IOException, CollectionException {
      // Never called
    }

    public boolean hasNext() throws IOException, CollectionException {
      getLogger().info("Logging: " + getClass().getName());
      return false;
    }

    public Progress[] getProgress() {
      return new Progress[0];
    }
  }

  public static class LoggingCasAnnotator extends CasAnnotator_ImplBase {
    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      getLogger().info("Logging: " + getClass().getName());
    }
  }

  public static class LoggingCasConsumer extends CasConsumer_ImplBase {
    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      getLogger().info("Logging: " + getClass().getName());
    }
  }

  public static class LoggingJCasAnnotator extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      getLogger().info("Logging: " + getClass().getName());
    }
  }

  public static class LoggingJCasConsumer extends JCasConsumer_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      getLogger().info("Logging: " + getClass().getName());
    }
  }

  public static class LoggingCasFlowController extends CasFlowController_ImplBase {
    @Override
    public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
      getLogger().info("Logging: " + getClass().getName());
      return null;
    }
  }

  public static class LoggingJCasFlowController extends JCasFlowController_ImplBase {
    @Override
    public Flow computeFlow(JCas aJCas) throws AnalysisEngineProcessException {
      getLogger().info("Logging: " + getClass().getName());
      return null;
    }
  }
}
