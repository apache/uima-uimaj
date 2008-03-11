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

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.Heap;
import org.apache.uima.cas.text.AnnotationIndex;

/**
 * Class comment for CASTest.java goes here.
 * 
 */
public class CASTest extends TestCase {

  private CAS cas;

  private TypeSystem ts;

  /**
   * Constructor for CASTest.
   * 
   * @param arg0
   */
  public CASTest(String arg0) {
    super(arg0);
  }

  public void setUp() {
    try {
      this.cas = CASInitializer.initCas(new CASTestSetup());
      this.ts = this.cas.getTypeSystem();
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  public void tearDown() {
    this.cas = null;
    this.ts = null;
  }

  public void testGetTypeSystem() {
    assertTrue(this.cas.getTypeSystem() != null);
  }
  
  public void testGetAnnotationIndex() {
    AnnotationIndex index = this.cas.getAnnotationIndex();
    assertNotNull(index);
    assertTrue(index.iterator() != null);
    boolean caughtException = false;
    try {
      this.cas.getAnnotationIndex(this.cas.getTypeSystem().getType(CAS.TYPE_NAME_TOP));
    } catch (CASRuntimeException e) {
      caughtException = true;
    }
    assertTrue(caughtException);
  }

  public void testCreateFS() {
    // Can create FS of type "Top"
    assertTrue(this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_TOP)) != null);
    boolean caughtExc = false;
    // Can't create int FS.
    try {
      this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_INTEGER));
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.NON_CREATABLE_TYPE));
    }
    assertTrue(caughtExc);
    caughtExc = false;
    // Can't create array with CAS.createFS().
    try {
      this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_FS_ARRAY));
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.NON_CREATABLE_TYPE));
    }
    assertTrue(caughtExc);
    caughtExc = false;

    // Check that we can create structures that are larger than the internal
    // heap page size.
    final int arraySize = 1000000;
    // Make sure that the structure we're trying to create is actually larger
    // than the page size we're testing with.
    assertTrue(arraySize > Heap.DEFAULT_SIZE);
    IntArrayFS array = null;
    try {
      array = this.cas.createIntArrayFS(arraySize);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    try {
      array.set(arraySize - 1, 1);
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(false);
    }

    // Can't create array subtype with CAS.createFS().
    // try {
    // this.cas.createFS(this.ts.getType(CASTestSetup.INT_ARRAY_SUB));
    // } catch (CASRuntimeException e) {
    // caughtExc = true;
    // assertTrue(e.getError() == CASRuntimeException.NON_CREATABLE_TYPE);
    // }
    // assertTrue(caughtExc);
  }

  public void testCreateCAS() {
    TypeSystemMgr tsm = CASFactory.createTypeSystem();
    tsm.commit();
  }

  public void testCreateArrayFS() {
    // Has its own test class.
  }

  public void testCreateIntArrayFS() {
    // Has its own test class.
  }

  public void testCreateStringArrayFS() {
    // Has its own test class.
  }

  // public void testCreateFilteredIterator() {
  // }
  //
  // public void testCommitFS() {
  // }
  //
  // public void testGetConstraintFactory() {
  // }
  //
  // public void testCreateFeaturePath() {
  // }
  //
  // public void testGetIndexRepository() {
  // }
  //
  // public void testFs2listIterator() {
  // }
  //
  public static void main(String[] args) {
    junit.textui.TestRunner.run(CASTest.class);
  }

}
