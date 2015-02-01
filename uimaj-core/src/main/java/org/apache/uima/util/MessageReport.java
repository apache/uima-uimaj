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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageReport {

  /**
   * Issues message at warning or fine level (fine if enabled, includes stack trace)
   * @param errorCount the count of errors used to decrease the frequency
   * @param message the message
   * @param logger the logger to use
   */
  public static void decreasingWithTrace(AtomicInteger errorCount, String message, Logger logger) {
    if (logger != null) {
      final int c = errorCount.incrementAndGet();
      final int cTruncated = Integer.highestOneBit(c); 
      // log with decreasing frequency
      if (cTruncated == c) {
        if (logger.isLoggable(Level.FINE)) {
          try { throw new Throwable();}
          catch (Throwable e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            message = "Message count: " + c + "; " + message + " Message count indicates messages skipped to avoid potential flooding.\n" + baos.toString();
            logger.log(Level.FINE, message);
          }
        } else {
          message = "Message count: " + c + "; " + message + " Message count indicates messages skipped to avoid potential flooding. Set FINE logging level for stacktrace.";
          logger.log(Level.WARNING, message);
        }
      }
    }
  }
}
