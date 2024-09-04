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

package org.apache.uima.cas.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.IntArrayFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntArrayFSTest {

  private CAS cas;

  @BeforeEach
  void setUp() {
    cas = CASInitializer.initCas(new CASTestSetup(), null);
  }

  @AfterEach
  void tearDown() {
    cas = null;
  }

  @Test
  void setWithIndex() {
    var array = cas.createIntArrayFS(3);
    assertThatNoException().isThrownBy(() -> {
      array.set(0, 1);
      array.set(1, 2);
      array.set(2, 3);
    });

    assertThat(array.get(0) == 1).isTrue();
    assertThat(array.get(1) == 2).isTrue();
    assertThat(array.get(2) == 3).isTrue();
  }

  @Test
  void thatSettingWithNegativeIndexThrowsException() {
    var array = cas.createIntArrayFS(3);
    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
            .isThrownBy(() -> array.set(-1, 1));
  }

  @Test
  void thatSettingWithTooLargeIndexThrowsException() {
    var array = cas.createIntArrayFS(3);
    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
            .isThrownBy(() -> array.set(4, 1));
  }

  @Test
  void thatGettingWithNegativeIndexThrowsException() {
    var array = cas.createIntArrayFS(3);
    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class).isThrownBy(() -> array.get(-1));
  }

  @Test
  void thatGettingWithTooLargeIndexThrowsException() {
    var array = cas.createIntArrayFS(3);
    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class).isThrownBy(() -> array.get(4));
  }

  @Test
  void thatNegativeSizedArrayCannotBeCreated() {
    assertThatExceptionOfType(CASRuntimeException.class).isThrownBy(() -> cas.createIntArrayFS(-1));
  }

  @Test
  void thatEmptyArrayCanBeCreated() {
    var array = cas.createIntArrayFS(0);
    assertThat(array).isNotNull();
    assertThat(array.isEmpty()).isTrue();
    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class).isThrownBy(() -> array.get(0));
  }

  @Test
  void testToArray() {
    // From CAS array to Java array.
    IntArrayFS array = cas.createIntArrayFS(3);
    int[] fsArray = array.toArray();
    for (int i = 0; i < 3; i++) {
      assertThat(fsArray[i] == 0).isTrue();
    }
    array.set(0, 1);
    array.set(1, 2);
    array.set(2, 3);
    fsArray = array.toArray();
    assertThat(fsArray.length == 3).isTrue();
    assertThat(fsArray[0] == 1).isTrue();
    assertThat(fsArray[1] == 2).isTrue();
    assertThat(fsArray[2] == 3).isTrue();

    // From Java array to CAS array.
    array = cas.createIntArrayFS(3);
    assertThat(array.get(0) == 0).isTrue();
    assertThat(array.get(1) == 0).isTrue();
    assertThat(array.get(2) == 0).isTrue();
    for (int i = 0; i < 3; i++) {
      array.set(i, fsArray[i]);
    }

    assertThat(array.get(0) == 1).isTrue();
    assertThat(array.get(1) == 2).isTrue();
    assertThat(array.get(2) == 3).isTrue();
    array.set(0, 0);
    assertThat(array.get(0) == 0).isTrue();
  }
}
