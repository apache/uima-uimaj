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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.uima.fit.testing.util.HideOutput;
import org.junit.jupiter.api.Test;

public class HideOutputTest {

  @Test
  public void testHideOutput() throws IOException {
    // message that will be written to stdout and stderr
    String className = this.getClass().getName();
    String message = String.format("If you see this output, %s is failing\n", className);

    // redirect stdout and stderr to streams we can read strings from
    PrintStream oldOut = System.out;
    PrintStream oldErr = System.err;
    ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
    ByteArrayOutputStream stringErr = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stringOut));
    System.setErr(new PrintStream(stringErr));
    try {
      // check that nothing is written to stdout or stderr while hidden
      HideOutput ho = new HideOutput();
      System.out.print(message);
      System.err.print(message);
      assertThat(stringOut.toString()).isEmpty();
      assertThat(stringErr.toString()).isEmpty();

      // check that data is again written to stdout and stderr after restoring
      ho.restoreOutput();
      System.out.print(message);
      System.err.print(message);
      assertThat(stringOut.toString()).isEqualTo(message);
      assertThat(stringErr.toString()).isEqualTo(message);
    }
    // restore stdout and stderr at the end of the test
    finally {
      System.setOut(oldOut);
      System.setErr(oldErr);
      stringOut.close();
      stringErr.close();
    }
  }
}
