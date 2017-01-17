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

import org.apache.uima.util.Level;

/**
 * UIMA Logging interface implementation test without using an logging toolkit
 * 
 */
public class Logger_implTest extends TestCase {
  public Logger_implTest(String arg0) {
    super(arg0);
  }

  public void testLogWrapperCreation() throws Exception {
    org.apache.uima.util.Logger rootLogger = Logger_impl.getInstance();
    org.apache.uima.util.Logger rootLogger1 = Logger_impl.getInstance();
    org.apache.uima.util.Logger classLogger = Logger_impl.getInstance(this.getClass());
    org.apache.uima.util.Logger classLogger1 = Logger_impl.getInstance(this.getClass());

    rootLogger.setLevel(Level.INFO);

    // check default configuration
    Assert.assertNotNull(rootLogger);
    Assert.assertNotNull(classLogger);
    Assert.assertTrue(rootLogger.isLoggable(Level.INFO));
    Assert.assertTrue(classLogger.isLoggable(Level.INFO));

    // check getInstance() calls
    Assert.assertNotSame(classLogger, rootLogger);
    Assert.assertEquals(rootLogger, rootLogger1);
    Assert.assertNotSame(classLogger, classLogger1);
  }

  public void testMessageLeveling() throws Exception {
    // create logger
    org.apache.uima.util.Logger rootLogger = Logger_impl.getInstance();
    org.apache.uima.util.Logger classLogger = Logger_impl.getInstance(this.getClass());

    rootLogger.setLevel(Level.INFO);

    // check message leveling root logger
    Assert.assertFalse(rootLogger.isLoggable(Level.ALL));
    Assert.assertFalse(rootLogger.isLoggable(Level.FINEST));
    Assert.assertFalse(rootLogger.isLoggable(Level.FINER));
    Assert.assertFalse(rootLogger.isLoggable(Level.FINE));
    Assert.assertFalse(rootLogger.isLoggable(Level.CONFIG));
    Assert.assertTrue(rootLogger.isLoggable(Level.INFO));
    Assert.assertTrue(rootLogger.isLoggable(Level.WARNING));
    Assert.assertTrue(rootLogger.isLoggable(Level.SEVERE));
    Assert.assertTrue(rootLogger.isLoggable(Level.OFF));

    // check message leveling class logger
    Assert.assertFalse(rootLogger.isLoggable(Level.ALL));
    Assert.assertFalse(rootLogger.isLoggable(Level.FINEST));
    Assert.assertFalse(rootLogger.isLoggable(Level.FINER));
    Assert.assertFalse(rootLogger.isLoggable(Level.FINE));
    Assert.assertFalse(rootLogger.isLoggable(Level.CONFIG));
    Assert.assertTrue(rootLogger.isLoggable(Level.INFO));
    Assert.assertTrue(rootLogger.isLoggable(Level.WARNING));
    Assert.assertTrue(rootLogger.isLoggable(Level.SEVERE));
    Assert.assertTrue(rootLogger.isLoggable(Level.OFF));

    // reset class logger level to OFF
    classLogger.setLevel(Level.OFF);
    Assert.assertFalse(classLogger.isLoggable(Level.ALL));
    Assert.assertFalse(classLogger.isLoggable(Level.FINEST));
    Assert.assertFalse(classLogger.isLoggable(Level.FINER));
    Assert.assertFalse(classLogger.isLoggable(Level.FINE));
    Assert.assertFalse(classLogger.isLoggable(Level.CONFIG));
    Assert.assertFalse(classLogger.isLoggable(Level.INFO));
    Assert.assertFalse(classLogger.isLoggable(Level.WARNING));
    Assert.assertFalse(classLogger.isLoggable(Level.SEVERE));
    Assert.assertTrue(classLogger.isLoggable(Level.OFF));

    // reset class logger level to ALL
    classLogger.setLevel(Level.ALL);
    Assert.assertTrue(classLogger.isLoggable(Level.ALL));
    Assert.assertTrue(classLogger.isLoggable(Level.FINEST));
    Assert.assertTrue(classLogger.isLoggable(Level.FINER));
    Assert.assertTrue(classLogger.isLoggable(Level.FINE));
    Assert.assertTrue(classLogger.isLoggable(Level.CONFIG));
    Assert.assertTrue(classLogger.isLoggable(Level.INFO));
    Assert.assertTrue(classLogger.isLoggable(Level.WARNING));
    Assert.assertTrue(classLogger.isLoggable(Level.SEVERE));
    Assert.assertTrue(classLogger.isLoggable(Level.OFF));

    // reset class logger level to WARNING
    classLogger.setLevel(Level.WARNING);
    Assert.assertFalse(classLogger.isLoggable(Level.ALL));
    Assert.assertFalse(classLogger.isLoggable(Level.FINEST));
    Assert.assertFalse(classLogger.isLoggable(Level.FINER));
    Assert.assertFalse(classLogger.isLoggable(Level.FINE));
    Assert.assertFalse(classLogger.isLoggable(Level.CONFIG));
    Assert.assertFalse(classLogger.isLoggable(Level.INFO));
    Assert.assertTrue(classLogger.isLoggable(Level.WARNING));
    Assert.assertTrue(classLogger.isLoggable(Level.SEVERE));
    Assert.assertTrue(classLogger.isLoggable(Level.OFF));

  }
}
