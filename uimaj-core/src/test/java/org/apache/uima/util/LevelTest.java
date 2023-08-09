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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * class Level provides the message level constants for logging and tracing
 * 
 */
public class LevelTest {
  @Test
  public void testLevelOff() throws Exception {
    Level level = Level.OFF;

    // check if level is on
    assertThat(level.isOn()).isFalse();
    // check if level is equal to "OFF"
    assertThat(level.equals(Level.OFF)).isTrue();
    // check if level is not equal to "FINE"
    assertThat(level.equals(Level.FINE)).isFalse();
    // check if level text is "OFF"
    assertThat("OFF").isEqualTo(level.toString());
    // check if level value is Integer.MAX_VALUE
    assertThat(Integer.MAX_VALUE).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isFalse();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isFalse();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isFalse();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isFalse();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isFalse();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isFalse();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isFalse();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();

  }

  @Test
  public void testLevelALL() throws Exception {
    Level level = Level.ALL;

    // check if level is on
    assertThat(level.isOn()).isTrue();
    // check if level is equal to "ALL"
    assertThat(level.equals(Level.ALL)).isTrue();
    // check if level is not equal to "CONFIG"
    assertThat(level.equals(Level.CONFIG)).isFalse();
    // check if level text is "ALL"
    assertThat("ALL").isEqualTo(level.toString());
    // check if level value is Integer.MIN_VALUE
    assertThat(Integer.MIN_VALUE).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isTrue();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isTrue();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isTrue();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isTrue();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isTrue();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isTrue();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isTrue();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();
  }

  @Test
  public void testLevelFINEST() throws Exception {
    Level level = Level.FINEST;

    // check if level is on
    assertThat(level.isOn()).isTrue();
    // check if level is equal to "FINEST"
    assertThat(level.equals(Level.FINEST)).isTrue();
    // check if level is not equal to "WARNING"
    assertThat(level.equals(Level.WARNING)).isFalse();
    // check if level text is "FINEST"
    assertThat("FINEST").isEqualTo(level.toString());
    // check if level value is 10000
    assertThat(10000).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isTrue();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isTrue();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isTrue();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isTrue();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isTrue();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isTrue();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();

  }

  @Test
  public void testLevelFINER() throws Exception {
    Level level = Level.FINER;

    // check if level is on
    assertThat(level.isOn()).isTrue();
    // check if level is equal to "FINER"
    assertThat(level.equals(Level.FINER)).isTrue();
    // check if level is not equal to "WARNING"
    assertThat(level.equals(Level.WARNING)).isFalse();
    // check if level text is "FINER"
    assertThat("FINER").isEqualTo(level.toString());
    // check if level value is 20000
    assertThat(20000).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isFalse();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isTrue();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isTrue();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isTrue();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isTrue();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isTrue();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();

  }

  @Test
  public void testLevelFINE() throws Exception {
    Level level = Level.FINE;

    // check if level is on
    assertThat(level.isOn()).isTrue();
    // check if level is equal to "FINE"
    assertThat(level.equals(Level.FINE)).isTrue();
    // check if level is not equal to "WARNING"
    assertThat(level.equals(Level.WARNING)).isFalse();
    // check if level text is "FINE"
    assertThat("FINE").isEqualTo(level.toString());
    // check if level value is 30000
    assertThat(30000).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isFalse();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isFalse();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isTrue();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isTrue();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isTrue();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isTrue();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();

  }

  @Test
  public void testLevelCONFIG() throws Exception {
    Level level = Level.CONFIG;

    // check if level is on
    assertThat(level.isOn()).isTrue();
    // check if level is equal to "CONFIG"
    assertThat(level.equals(Level.CONFIG)).isTrue();
    // check if level is not equal to "WARNING"
    assertThat(level.equals(Level.WARNING)).isFalse();
    // check if level text is "CONFIG"
    assertThat("CONFIG").isEqualTo(level.toString());
    // check if level value is 40000
    assertThat(40000).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isFalse();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isFalse();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isFalse();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isTrue();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isTrue();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isTrue();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();

  }

  @Test
  public void testLevelINFO() throws Exception {
    Level level = Level.INFO;

    // check if level is on
    assertThat(level.isOn()).isTrue();
    // check if level is equal to "INFO"
    assertThat(level.equals(Level.INFO)).isTrue();
    // check if level is not equal to "WARNING"
    assertThat(level.equals(Level.WARNING)).isFalse();
    // check if level text is "INFO"
    assertThat("INFO").isEqualTo(level.toString());
    // check if level value is 50000
    assertThat(50000).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isFalse();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isFalse();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isFalse();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isFalse();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isTrue();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isTrue();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();
  }

  @Test
  public void testLevelWARNING() throws Exception {
    Level level = Level.WARNING;

    // check if level is on
    assertThat(level.isOn()).isTrue();
    // check if level is equal to "WARNING"
    assertThat(level.equals(Level.WARNING)).isTrue();
    // check if level is not equal to "OFF"
    assertThat(level.equals(Level.OFF)).isFalse();
    // check if level text is "WARNING"
    assertThat("WARNING").isEqualTo(level.toString());
    // check if level value is 60000
    assertThat(60000).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isFalse();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isFalse();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isFalse();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isFalse();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isFalse();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isTrue();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();

  }

  @Test
  public void testLevelSEVERE() throws Exception {
    Level level = Level.SEVERE;

    // check if level is on
    assertThat(level.isOn()).isTrue();
    // check if level is equal to "SEVERE"
    assertThat(level.equals(Level.SEVERE)).isTrue();
    // check if level is not equal to "OFF"
    assertThat(level.equals(Level.OFF)).isFalse();
    // check if level text is "SEVERE"
    assertThat("SEVERE").isEqualTo(level.toString());
    // check if level value is 70000
    assertThat(70000).isEqualTo(level.toInteger());

    // check if level is greater or equal to ALL
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();
    // check if level is greater or equal to FINEST
    assertThat(level.isGreaterOrEqual(Level.FINEST)).isFalse();
    // check if level is greater or equal to FINER
    assertThat(level.isGreaterOrEqual(Level.FINER)).isFalse();
    // check if level is greater or equal to FINE
    assertThat(level.isGreaterOrEqual(Level.FINE)).isFalse();
    // check if level is greater or equal to CONFIG
    assertThat(level.isGreaterOrEqual(Level.CONFIG)).isFalse();
    // check if level is greater or equal to INFO
    assertThat(level.isGreaterOrEqual(Level.INFO)).isFalse();
    // check if level is greater or equal to WARNING
    assertThat(level.isGreaterOrEqual(Level.WARNING)).isFalse();
    // check if level is greater or equal to SEVERE
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();
    // check if level is greater or equal to OFF
    assertThat(level.isGreaterOrEqual(Level.OFF)).isTrue();

  }

  @Test
  public void testEquals() throws Exception {
    Level level = Level.SEVERE;
    Integer myInt = 70000;

    // check if level is equal to "SEVERE"
    assertThat(level.equals(Level.SEVERE)).isTrue();
    // check with another class than Level
    assertThat(level.equals(myInt)).isFalse();
    // check with null value
    assertThat(level.equals(null)).isFalse();
  }

  @Test
  public void testisGreaterOrEqual() throws Exception {
    Level level = Level.INFO;

    // check if level "ALL" is greater or equal to "INFO"
    assertThat(level.isGreaterOrEqual(Level.ALL)).isFalse();

    // check if level "SEVERE" is greater or equal to "INFO"
    assertThat(level.isGreaterOrEqual(Level.SEVERE)).isTrue();

    // check with null value
    assertThat(level.isGreaterOrEqual(null)).isFalse();
  }

}
