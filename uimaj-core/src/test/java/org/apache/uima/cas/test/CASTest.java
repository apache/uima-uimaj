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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.text.AnnotationIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class comment for CASTest.java goes here.
 */
class CASTest {

  private CAS cas;

  private TypeSystem ts;

  @BeforeEach
  void setUp() {
    cas = CASInitializer.initCas(new CASTestSetup(), null);
    ts = cas.getTypeSystem();
  }

  @AfterEach
  void tearDown() {
    cas = null;
    ts = null;
  }

  @Test
  void testGetTypeSystem() {
    assertNotNull(cas.getTypeSystem());
  }

  @Test
  void testGetAnnotationIndex() {
    AnnotationIndex index = cas.getAnnotationIndex();
    assertNotNull(index);
    assertNotNull(index.iterator());
    boolean caughtException = false;
    try {
      cas.getAnnotationIndex(cas.getTypeSystem().getType(CAS.TYPE_NAME_TOP));
    } catch (CASRuntimeException e) {
      caughtException = true;
    }
    assertTrue(caughtException);
  }

  @Test
  void testCreateFS() {
    // Can create FS of type "Top"
    assertNotNull(cas.createFS(ts.getType(CAS.TYPE_NAME_TOP)));
    boolean caughtExc = false;
    // Can't create int FS.
    try {
      cas.createFS(ts.getType(CAS.TYPE_NAME_INTEGER));
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertEquals(CASRuntimeException.NON_CREATABLE_TYPE, e.getMessageKey());
    }
    assertTrue(caughtExc);
    caughtExc = false;
    // Can't create array with CAS.createFS().
    try {
      cas.createFS(ts.getType(CAS.TYPE_NAME_FS_ARRAY));
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertEquals(CASRuntimeException.NON_CREATABLE_TYPE, e.getMessageKey());
    }
    assertTrue(caughtExc);
    caughtExc = false;

    // Check that we can create structures that are larger than the internal
    // heap page size.
    final int arraySize = 1000000;
    // Make sure that the structure we're trying to create is actually larger
    // than the page size we're testing with.
    // assertTrue(arraySize > Heap.DEFAULT_SIZE);
    IntArrayFS array = cas.createIntArrayFS(arraySize);
    array.set(arraySize - 1, 1);

    // Can't create array subtype with CAS.createFS().
    // try {
    // this.cas.createFS(this.ts.getType(CASTestSetup.INT_ARRAY_SUB));
    // } catch (CASRuntimeException e) {
    // caughtExc = true;
    // assertTrue(e.getError() == CASRuntimeException.NON_CREATABLE_TYPE);
    // }
    // assertTrue(caughtExc);
  }

  @Test
  void testCreateCAS() {
    TypeSystemMgr tsm = CASFactory.createTypeSystem();
    tsm.commit();
  }
}
