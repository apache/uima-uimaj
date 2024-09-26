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
package org.apache.uima.fit.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.uima.fit.testing.util.DisableLogging;
import org.junit.jupiter.api.Test;

public class DisableLoggingTest {

  @Test
  public void test() {
    // get the top logger and remove all handlers
    Logger topLogger = Logger.getLogger("");
    Handler[] handlers = topLogger.getHandlers();
    for (Handler handler : handlers) {
      topLogger.removeHandler(handler);
    }

    // add a single hander that writes to a string buffer
    final StringBuffer buffer = new StringBuffer();
    Handler bufferhandler = new Handler() {
      @Override
      public void close() throws SecurityException {/* do nothing */
      }

      @Override
      public void flush() {/* do nothing */
      }

      @Override
      public void publish(LogRecord record) {
        buffer.append(record.getMessage());
      }
    };
    topLogger.addHandler(bufferhandler);

    // log to the buffer
    Logger.getLogger("foo").info("Hello!");
    assertThat(buffer.toString()).isEqualTo("Hello!");

    // disable logging, and make sure nothing is written to the buffer
    buffer.setLength(0);
    Level level = DisableLogging.disableLogging();
    Logger.getLogger("bar").info("Hello!");
    assertThat(buffer).isEmpty();

    // enable logging, and make sure things are written to the buffer
    DisableLogging.enableLogging(level);
    Logger.getLogger("baz").info("Hello!");
    assertThat(buffer.toString()).isEqualTo("Hello!");

    // try disabling logging with a logger that has its own handler
    buffer.setLength(0);
    Logger logger = Logger.getLogger("foo.bar.baz");
    logger.addHandler(new Handler() {
      @Override
      public void close() throws SecurityException {/* do nothing */
      }

      @Override
      public void flush() { /* do nothing */
      }

      @Override
      public void publish(LogRecord record) {
        buffer.append("Not disabled!");
      }
    });
    level = DisableLogging.disableLogging();
    logger.info("Hello!");
    assertThat(buffer).isEmpty();
    DisableLogging.enableLogging(level);

    // restore the original handlers
    topLogger.removeHandler(bufferhandler);
    for (Handler handler : handlers) {
      topLogger.addHandler(handler);
    }
  }
}
