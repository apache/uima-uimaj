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

import static org.junit.Assert.assertTrue;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.IntArrayFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IntArrayFSTest {

  private CAS cas;

  @BeforeEach
  public void setUp() {
    try {
      cas = CASInitializer.initCas(new CASTestSetup(), null);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @AfterEach
  public void tearDown() {
    cas = null;
  }

  @Test
  public void testSet() {
    IntArrayFS array = cas.createIntArrayFS(0);
    assertTrue(array != null);
    assertTrue(array.size() == 0);
    boolean exceptionCaught = false;
    try {
      array.get(0);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    array = cas.createIntArrayFS(3);
    try {
      array.set(0, 1);
      array.set(1, 2);
      array.set(2, 3);
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(false);
    }
    exceptionCaught = false;
    try {
      array.set(-1, 1);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    exceptionCaught = false;
    try {
      array.set(4, 1);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    assertTrue(array.get(0) == 1);
    assertTrue(array.get(1) == 2);
    assertTrue(array.get(2) == 3);
    exceptionCaught = false;
    try {
      array.get(-1);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    exceptionCaught = false;
    try {
      array.get(4);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    // Check that we can't create arrays smaller than 0.
    exceptionCaught = false;
    try {
      array = cas.createIntArrayFS(-1);
    } catch (CASRuntimeException e) {
      exceptionCaught = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.ILLEGAL_ARRAY_SIZE));
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testToArray() {
    // From CAS array to Java array.
    IntArrayFS array = cas.createIntArrayFS(3);
    int[] fsArray = array.toArray();
    for (int i = 0; i < 3; i++) {
      assertTrue(fsArray[i] == 0);
    }
    array.set(0, 1);
    array.set(1, 2);
    array.set(2, 3);
    fsArray = array.toArray();
    assertTrue(fsArray.length == 3);
    assertTrue(fsArray[0] == 1);
    assertTrue(fsArray[1] == 2);
    assertTrue(fsArray[2] == 3);

    // From Java array to CAS array.
    array = cas.createIntArrayFS(3);
    assertTrue(array.get(0) == 0);
    assertTrue(array.get(1) == 0);
    assertTrue(array.get(2) == 0);
    for (int i = 0; i < 3; i++) {
      array.set(i, fsArray[i]);
    }
    assertTrue(array.get(0) == 1);
    assertTrue(array.get(1) == 2);
    assertTrue(array.get(2) == 3);
    array.set(0, 0);
    assertTrue(array.get(0) == 0);
  }

}
