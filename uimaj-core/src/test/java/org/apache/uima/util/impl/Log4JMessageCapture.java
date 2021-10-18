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
import java.util.List;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.uima.util.AutoCloseableNoException;

public class Log4JMessageCapture implements AutoCloseableNoException {

  private final List<LogEvent> events = new ArrayList<>();
  private final List<LogEvent> latestEvents = new ArrayList<>();
  private final Filter filter;
  private final ConsoleAppender app;

  public Log4JMessageCapture() {
    // Tell the logger to log everything
    org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
            .getRootLogger();
    rootLogger.get().setLevel(org.apache.logging.log4j.Level.ALL);
    rootLogger.getContext().updateLoggers();

    filter = makeCapturingFilter();

    app = (ConsoleAppender) rootLogger.get().getAppenders().values().stream().findFirst().get();
    app.addFilter(filter);
  }

  private Filter makeCapturingFilter() {
    return new AbstractFilter() {
      @Override
      public Result filter(LogEvent event) {
        StackTraceElement ste = event.getSource();
        System.out.printf("[%s:%s] %s%n", ste.getFileName(), ste.getLineNumber(),
                event.getMessage().getFormattedMessage());
        LogEvent immutableEvent = event.toImmutable();
        events.add(immutableEvent);
        latestEvents.add(immutableEvent);
        return Result.DENY;
      }
    };
  }

  public List<LogEvent> getAndClearLatestEvents() {
    List<LogEvent> result = new ArrayList<>(latestEvents);
    latestEvents.clear();
    return result;
  }

  public List<LogEvent> getAllEvents() {
    return events;
  }

  @Override
  public void close() {
    app.removeFilter(filter);
  }
}
