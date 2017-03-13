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

package org.apache.uima.internal.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * JSR47 log formatter for UIMA JSR47Logger
 * 
 * Provides a log format which looks like: timestamp; threadID; sourceInfo; Message level; message
 * e.g. 7/12/04 2:15:35 PM - 10: org.apache.uima.util.TestClass.main(62): INFO: You are not logged
 * in!
 * 
 */
public class UIMALogFormatter extends Formatter {

  SimpleDateFormat tsFormatter = new SimpleDateFormat( "hh:mm:ss.SS" ); 

  private static final String CRLF = System.getProperties().getProperty("line.separator");

  public synchronized String format(LogRecord record) {
    // if record is null, return an empty String
    if (record == null) {
      return "";
    }

    StringBuffer buffer = new StringBuffer(100);

    // create timestamp
    Date timestamp = new Date(record.getMillis());
    String timestampStr = tsFormatter.format(timestamp);
    // append timestamp to the output string
    buffer.append(timestampStr);

    // append source thread ID
    buffer.append(" - ");
    buffer.append(record.getThreadID());

    // append source class if logrb() method was used, otherwise append logger name
    buffer.append(": ");
    if (record.getSourceClassName() == null || record.getSourceClassName().equals("")) {
      buffer.append(record.getLoggerName());
    } else {
      buffer.append(record.getSourceClassName());
    }

    // append source method if logrb() was used
    if (record.getSourceMethodName() != null) {
      buffer.append('.');
      buffer.append(record.getSourceMethodName());
    }

    // append message level
    buffer.append(": ");
    buffer.append(record.getLevel().getLocalizedName());

    // append message
    buffer.append(": ");
    buffer.append(record.getMessage());

    // append exception if avaialble
    if (record.getThrown() != null) {
      buffer.append(CRLF);
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      record.getThrown().printStackTrace(printWriter);
      printWriter.close();
      buffer.append(stringWriter.toString());
    }

    // append new line at the end
    buffer.append(CRLF);

    // return log entry
    return buffer.toString();
  }
}
