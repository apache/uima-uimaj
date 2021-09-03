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
package org.apache.uima.cas.serdes;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.regex.Pattern;

public class SerDesAssumptions {
  public static void assumeNotKnownToFail(Runnable aScenario, String... aPatternsAndReasons) {
    for (int i = 0; i < aPatternsAndReasons.length; i += 2) {
      String pattern = aPatternsAndReasons[i];
      String reason = aPatternsAndReasons[i + 1];
      assumeFalse(Pattern.matches(pattern, aScenario.toString()), "Skipped because: " + reason);
    }
  }
}
