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

import static java.lang.System.arraycopy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.uima.util.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Slf4jLogger_implCapturingTest {

  private final Exception ex = new IllegalArgumentException("boom");

  private Logger sut;

  @BeforeEach
  public void setup() {
    sut = new Slf4jLogger_impl(null);
  }

  @Test
  public void thatSimpleMessageIsLogged() throws Exception {
    try (Log4JMessageCapture capture = new Log4JMessageCapture()) {
      sut.trace("test", ex);
      sut.debug("test", ex);
      sut.info("test", ex);
      sut.warn("test", ex);
      sut.error("test", ex);
      assertThat(capture.getAndClearLatestEvents()) //
              .extracting( //
                      e -> e.getLevel(), //
                      e -> e.getMessage().getFormattedMessage(), //
                      e -> e.getThrown()) //
              .containsExactly( //
                      tuple(Level.TRACE, "test", ex), //
                      tuple(Level.DEBUG, "test", ex), //
                      tuple(Level.INFO, "test", ex), //
                      tuple(Level.WARN, "test", ex), //
                      tuple(Level.ERROR, "test", ex));
    }
  }

  @Test
  public void thatThrowableIsLogged() throws Exception {
    try (Log4JMessageCapture capture = new Log4JMessageCapture()) {
      sut.debug("test", ex);
      assertThat(capture.getAndClearLatestEvents()) //
              .extracting( //
                      e -> e.getMessage().getFormattedMessage(), //
                      e -> e.getThrown()) //
              .containsExactly( //
                      tuple("test", ex));
    }
  }

  @Test
  public void thatMultipleParametersAreLogged() throws Exception {
    try (Log4JMessageCapture capture = new Log4JMessageCapture()) {
      for (int paramCount = 1; paramCount < 5; paramCount++) {
        List<String> placeholders = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < paramCount; i++) {
          placeholders.add("{}");
          values.add(Integer.toString(i));
        }
        sut.debug(String.join(" ", placeholders), values.toArray());

        assertThat(capture.getAndClearLatestEvents()) //
                .extracting( //
                        e -> e.getMessage().getFormattedMessage(), //
                        e -> e.getThrown()) //
                .containsExactly( //
                        tuple(String.join(" ", values), null));
      }
    }
  }

  @Test
  public void thatOneParameterAndThrowableAreLogged() throws Exception {
    try (Log4JMessageCapture capture = new Log4JMessageCapture()) {
      sut.debug("{}", "1", ex);

      assertThat(capture.getAndClearLatestEvents()) //
              .extracting( //
                      e -> e.getMessage().getFormattedMessage(), //
                      e -> e.getThrown()) //
              .containsExactly( //
                      tuple("1", ex));
    }
  }

  @Test
  public void thatTwoParametersAndThrowableAreLogged() throws Exception {
    try (Log4JMessageCapture capture = new Log4JMessageCapture()) {
      sut.trace("{} {}", "1", "2", ex);
      sut.debug("{} {}", "1", "2", ex);
      sut.info("{} {}", "1", "2", ex);
      sut.warn("{} {}", "1", "2", ex);
      sut.error("{} {}", "1", "2", ex);

      assertThat(capture.getAndClearLatestEvents()) //
              .extracting( //
                      e -> e.getLevel(), //
                      e -> e.getMessage().getFormattedMessage(), //
                      e -> e.getThrown()) //
              .containsExactly( //
                      tuple(Level.TRACE, "1 2", ex), //
                      tuple(Level.DEBUG, "1 2", ex), //
                      tuple(Level.INFO, "1 2", ex), //
                      tuple(Level.WARN, "1 2", ex), //
                      tuple(Level.ERROR, "1 2", ex));
    }
  }

  @Test
  public void thatThreeParametersAndThrowableAreLogged() throws Exception {
    try (Log4JMessageCapture capture = new Log4JMessageCapture()) {
      sut.trace("{} {} {}", "1", "2", "3", ex);
      sut.debug("{} {} {}", "1", "2", "3", ex);
      sut.info("{} {} {}", "1", "2", "3", ex);
      sut.warn("{} {} {}", "1", "2", "3", ex);
      sut.error("{} {} {}", "1", "2", "3", ex);

      assertThat(capture.getAndClearLatestEvents()) //
              .extracting( //
                      e -> e.getLevel(), //
                      e -> e.getMessage().getFormattedMessage(), //
                      e -> e.getThrown()) //
              .containsExactly( //
                      tuple(Level.TRACE, "1 2 3", ex), //
                      tuple(Level.DEBUG, "1 2 3", ex), //
                      tuple(Level.INFO, "1 2 3", ex), //
                      tuple(Level.WARN, "1 2 3", ex), //
                      tuple(Level.ERROR, "1 2 3", ex));
    }
  }

  @Test
  public void thatMultipleParametersAndThrowableAreLogged() throws Exception {
    try (Log4JMessageCapture capture = new Log4JMessageCapture()) {
      for (int paramCount = 1; paramCount < 5; paramCount++) {
        List<String> placeholders = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < paramCount; i++) {
          placeholders.add("{}");
          values.add(Integer.toString(i));
        }

        Object[] valuesPlusEx = new Object[paramCount + 1];
        arraycopy(values.toArray(), 0, valuesPlusEx, 0, paramCount);
        valuesPlusEx[paramCount] = ex;
        sut.debug(String.join(" ", placeholders), valuesPlusEx);

        assertThat(capture.getAndClearLatestEvents()) //
                .extracting( //
                        e -> e.getMessage().getFormattedMessage(), //
                        e -> e.getThrown()) //
                .containsExactly( //
                        tuple(String.join(" ", values), ex));
      }
    }
  }
}
