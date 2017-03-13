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

package org.apache.uima.util;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 * class Level provides the message level constants for logging and tracing
 * 
 */
public class LevelTest extends TestCase {

  public LevelTest(String arg0) {
    super(arg0);
  }

  public void testLevelOff() throws Exception {
    Level level = Level.OFF;

    // check if level is on
    Assert.assertFalse(level.isOn());
    // check if level is equal to "OFF"
    Assert.assertTrue(level.equals(Level.OFF));
    // check if level is not equal to "FINE"
    Assert.assertFalse(level.equals(Level.FINE));
    // check if level text is "OFF"
    Assert.assertEquals(level.toString(), "OFF");
    // check if level value is Integer.MAX_VALUE
    Assert.assertEquals(level.toInteger(), Integer.MAX_VALUE);

    // check if level is greater or equal to ALL
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertFalse(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertFalse(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertFalse(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertFalse(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));

  }

  public void testLevelALL() throws Exception {
    Level level = Level.ALL;

    // check if level is on
    Assert.assertTrue(level.isOn());
    // check if level is equal to "ALL"
    Assert.assertTrue(level.equals(Level.ALL));
    // check if level is not equal to "CONFIG"
    Assert.assertFalse(level.equals(Level.CONFIG));
    // check if level text is "ALL"
    Assert.assertEquals(level.toString(), "ALL");
    // check if level value is Integer.MIN_VALUE
    Assert.assertEquals(level.toInteger(), Integer.MIN_VALUE);

    // check if level is greater or equal to ALL
    Assert.assertTrue(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertTrue(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertTrue(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertTrue(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));
  }

  public void testLevelFINEST() throws Exception {
    Level level = Level.FINEST;

    // check if level is on
    Assert.assertTrue(level.isOn());
    // check if level is equal to "FINEST"
    Assert.assertTrue(level.equals(Level.FINEST));
    // check if level is not equal to "WARNING"
    Assert.assertFalse(level.equals(Level.WARNING));
    // check if level text is "FINEST"
    Assert.assertEquals(level.toString(), "FINEST");
    // check if level value is 10000
    Assert.assertEquals(level.toInteger(), 10000);

    // check if level is greater or equal to ALL
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertTrue(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertTrue(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertTrue(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));

  }

  public void testLevelFINER() throws Exception {
    Level level = Level.FINER;

    // check if level is on
    Assert.assertTrue(level.isOn());
    // check if level is equal to "FINER"
    Assert.assertTrue(level.equals(Level.FINER));
    // check if level is not equal to "WARNING"
    Assert.assertFalse(level.equals(Level.WARNING));
    // check if level text is "FINER"
    Assert.assertEquals(level.toString(), "FINER");
    // check if level value is 20000
    Assert.assertEquals(level.toInteger(), 20000);

    // check if level is greater or equal to ALL
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertTrue(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertTrue(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertTrue(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));

  }

  public void testLevelFINE() throws Exception {
    Level level = Level.FINE;

    // check if level is on
    Assert.assertTrue(level.isOn());
    // check if level is equal to "FINE"
    Assert.assertTrue(level.equals(Level.FINE));
    // check if level is not equal to "WARNING"
    Assert.assertFalse(level.equals(Level.WARNING));
    // check if level text is "FINE"
    Assert.assertEquals(level.toString(), "FINE");
    // check if level value is 30000
    Assert.assertEquals(level.toInteger(), 30000);

    // check if level is greater or equal to ALL
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertTrue(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertTrue(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertTrue(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertTrue(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));

  }

  public void testLevelCONFIG() throws Exception {
    Level level = Level.CONFIG;

    // check if level is on
    Assert.assertTrue(level.isOn());
    // check if level is equal to "CONFIG"
    Assert.assertTrue(level.equals(Level.CONFIG));
    // check if level is not equal to "WARNING"
    Assert.assertFalse(level.equals(Level.WARNING));
    // check if level text is "CONFIG"
    Assert.assertEquals(level.toString(), "CONFIG");
    // check if level value is 40000
    Assert.assertEquals(level.toInteger(), 40000);

    // check if level is greater or equal to ALL
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertTrue(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertTrue(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertTrue(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));

  }

  public void testLevelINFO() throws Exception {
    Level level = Level.INFO;

    // check if level is on
    Assert.assertTrue(level.isOn());
    // check if level is equal to "INFO"
    Assert.assertTrue(level.equals(Level.INFO));
    // check if level is not equal to "WARNING"
    Assert.assertFalse(level.equals(Level.WARNING));
    // check if level text is "INFO"
    Assert.assertEquals(level.toString(), "INFO");
    // check if level value is 50000
    Assert.assertEquals(level.toInteger(), 50000);

    // check if level is greater or equal to ALL
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertFalse(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertTrue(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertTrue(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));
  }

  public void testLevelWARNING() throws Exception {
    Level level = Level.WARNING;

    // check if level is on
    Assert.assertTrue(level.isOn());
    // check if level is equal to "WARNING"
    Assert.assertTrue(level.equals(Level.WARNING));
    // check if level is not equal to "OFF"
    Assert.assertFalse(level.equals(Level.OFF));
    // check if level text is "WARNING"
    Assert.assertEquals(level.toString(), "WARNING");
    // check if level value is 60000
    Assert.assertEquals(level.toInteger(), 60000);

    // check if level is greater or equal to ALL
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertFalse(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertFalse(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertTrue(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));

  }

  public void testLevelSEVERE() throws Exception {
    Level level = Level.SEVERE;

    // check if level is on
    Assert.assertTrue(level.isOn());
    // check if level is equal to "SEVERE"
    Assert.assertTrue(level.equals(Level.SEVERE));
    // check if level is not equal to "OFF"
    Assert.assertFalse(level.equals(Level.OFF));
    // check if level text is "SEVERE"
    Assert.assertEquals(level.toString(), "SEVERE");
    // check if level value is 70000
    Assert.assertEquals(level.toInteger(), 70000);

    // check if level is greater or equal to ALL
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));
    // check if level is greater or equal to FINEST
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINEST));
    // check if level is greater or equal to FINER
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINER));
    // check if level is greater or equal to FINE
    Assert.assertFalse(level.isGreaterOrEqual(Level.FINE));
    // check if level is greater or equal to CONFIG
    Assert.assertFalse(level.isGreaterOrEqual(Level.CONFIG));
    // check if level is greater or equal to INFO
    Assert.assertFalse(level.isGreaterOrEqual(Level.INFO));
    // check if level is greater or equal to WARNING
    Assert.assertFalse(level.isGreaterOrEqual(Level.WARNING));
    // check if level is greater or equal to SEVERE
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));
    // check if level is greater or equal to OFF
    Assert.assertTrue(level.isGreaterOrEqual(Level.OFF));

  }

  public void testEquals() throws Exception {
    Level level = Level.SEVERE;
    Integer myInt = Integer.valueOf(70000);

    // check if level is equal to "SEVERE"
    Assert.assertTrue(level.equals(Level.SEVERE));
    // check with another class than Level
    Assert.assertFalse(level.equals(myInt));
    // check with null value
    Assert.assertFalse(level.equals(null));
  }

  public void testisGreaterOrEqual() throws Exception {
    Level level = Level.INFO;

    // check if level "ALL" is greater or equal to "INFO"
    Assert.assertFalse(level.isGreaterOrEqual(Level.ALL));

    // check if level "SEVERE" is greater or equal to "INFO"
    Assert.assertTrue(level.isGreaterOrEqual(Level.SEVERE));

    // check with null value
    Assert.assertFalse(level.isGreaterOrEqual(null));
  }

}
