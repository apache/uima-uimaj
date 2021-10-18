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

import org.apache.uima.util.Logger;
import org.junit.Before;
import org.junit.Test;

public class Slf4jLogger_implTest {

  private final Exception ex = new IllegalArgumentException("boom");

  private Logger sut;

  @Before
  public void setup() {
    sut = Slf4jLogger_impl.getInstance();
  }

  @Test
  public void thatSimpleMessageIsLogged() throws Exception {
    try (Log4JMessageCapture capture = new Log4JMessageCapture()) {
      sut.debug("test");
      assertThat(capture.getAndClearLatestEvents()) //
              .extracting( //
                      e -> e.getMessage().getFormattedMessage(), //
                      e -> e.getThrown()) //
              .containsExactly( //
                      tuple("test", null));
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
