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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class MiscTest {

  @Test
  void test() {
    assertThat(Misc.nextHigherPowerOfX(0, 8)).isEqualTo(8);
    assertThat(Misc.nextHigherPowerOfX(-0, 8)).isEqualTo(8);
    assertThat(Misc.nextHigherPowerOfX(1, 8)).isEqualTo(8);
    assertThat(Misc.nextHigherPowerOfX(7, 8)).isEqualTo(8);
    assertThat(Misc.nextHigherPowerOfX(8, 8)).isEqualTo(8);
    assertThat(Misc.nextHigherPowerOfX(9, 8)).isEqualTo(16);
    assertThat(Misc.nextHigherPowerOfX(10 * 1024 * 1024 * 8 / 3 / 50, 4096)).isEqualTo(561152);
    assertThat(Misc.nextHigherPowerOfX(Math.max(512, 561152 / 1000), 32)).isEqualTo(576);
    assertThat(Arrays.equals(new byte[] { 0x03, 0x42 }, Misc.hex_string_to_bytearray("0342")))
            .isTrue();
  }
}
