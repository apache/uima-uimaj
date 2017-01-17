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

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * Logger implementation test
 * 
 */
public class LoggingTest extends TestCase {

  /**
   * Constructor for SequencerTest
   * 
   * @param arg0
   */
  public LoggingTest(String arg0) {
    super(arg0);
  }

  public void testDefaultLoggerCreation() throws Exception {
    try {
      // get default logger
      Logger logger = UIMAFramework.getLogger();
      Assert.assertNotNull(logger);

      // create another logger
      Logger logger1 = UIMAFramework.getLogger();

      // both loggers must reference the same instance
      Assert.assertEquals(logger, logger1);

      // test base logging functions
      logger.log(Level.SEVERE, "Log test messege with Level SEVERE");
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    }
  }

  public void testClassLoggerCreation() throws Exception {
    try {
      // get class logger
      Logger logger = UIMAFramework.getLogger(this.getClass());
      Assert.assertNotNull(logger);

      // create another class logger
      Logger logger1 = UIMAFramework.getLogger(this.getClass());

      // create default logger
      Logger defaultLogger = UIMAFramework.getLogger();

      // both loggers must reference the same instance
      Assert.assertEquals(logger, logger1);

      // should not be the same
      Assert.assertNotSame(defaultLogger, logger1);

      // test base logging functions
      logger.log(Level.SEVERE, "Log test messege with Level SEVERE");
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    }
  }

  public void testSetLevel() throws Exception {
    try {
      // get class logger
      Logger logger = UIMAFramework.getLogger(this.getClass());
      Logger uimaLogger = UIMAFramework.getLogger(); // should affect everything in
      // org.apache.uima.*

      // set level to WARNING
      uimaLogger.setLevel(Level.WARNING);
      Assert.assertTrue(uimaLogger.isLoggable(Level.WARNING));
      Assert.assertTrue(uimaLogger.isLoggable(Level.SEVERE));
      Assert.assertFalse(uimaLogger.isLoggable(Level.INFO));
      Assert.assertTrue(logger.isLoggable(Level.WARNING));
      Assert.assertTrue(logger.isLoggable(Level.SEVERE));
      Assert.assertFalse(logger.isLoggable(Level.INFO));

      // set level to FINE
      uimaLogger.setLevel(Level.FINE);
      Assert.assertTrue(uimaLogger.isLoggable(Level.WARNING));
      Assert.assertTrue(uimaLogger.isLoggable(Level.SEVERE));
      Assert.assertTrue(uimaLogger.isLoggable(Level.INFO));
      Assert.assertFalse(uimaLogger.isLoggable(Level.FINER));
      Assert.assertFalse(uimaLogger.isLoggable(Level.ALL));
      Assert.assertTrue(logger.isLoggable(Level.WARNING));
      Assert.assertTrue(logger.isLoggable(Level.SEVERE));
      Assert.assertTrue(logger.isLoggable(Level.INFO));
      Assert.assertFalse(logger.isLoggable(Level.FINER));
      Assert.assertFalse(logger.isLoggable(Level.ALL));
      uimaLogger.setLevel(Level.INFO);
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    }

  }

}
