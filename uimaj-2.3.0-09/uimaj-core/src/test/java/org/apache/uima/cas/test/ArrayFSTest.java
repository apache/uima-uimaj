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

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

public class ArrayFSTest extends TestCase {

  private CAS cas;

  private TypeSystem ts;

  /**
   * Constructor for ArrayFSTest.
   * 
   * @param arg0
   */
  public ArrayFSTest(String arg0) {
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

  public static void main(String[] args) {
    junit.textui.TestRunner.run(ArrayFSTest.class);
  }

  public void testSet() {
    // Check that we can't create arrays of size smaller than 0.
    boolean exceptionCaught = false;
    try {
      ArrayFS array = this.cas.createArrayFS(-1);
      assertTrue(array != null);
    } catch (CASRuntimeException e) {
      exceptionCaught = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.ILLEGAL_ARRAY_SIZE));
    }
    assertTrue(exceptionCaught);
    ArrayFS array = this.cas.createArrayFS(0);
    assertTrue(array.size() == 0);
    assertTrue(array != null);
    assertTrue(array.size() == 0);
    exceptionCaught = false;
    try {
      array.get(0);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    FeatureStructure fs1 = this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_ANNOTATION));
    FeatureStructure fs2 = this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_TOP));
    FeatureStructure fs3 = this.cas.createFS(this.ts.getType(CASTestSetup.TOKEN_TYPE));
    array = this.cas.createArrayFS(3);
    try {
      array.set(0, fs1);
      array.set(1, fs2);
      array.set(2, fs3);
      String[] stringArray = array.toStringArray();
      assertTrue(stringArray.length == 3);
      for (int i = 0; i < array.size(); i++) {
	assertNotNull(stringArray[i]);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(false);
    }
    exceptionCaught = false;
    try {
      array.set(-1, fs1);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    exceptionCaught = false;
    try {
      array.set(4, fs1);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    assertTrue(array.get(0).equals(fs1));
    assertTrue(array.get(1).equals(fs2));
    assertTrue(array.get(2).equals(fs3));
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
  }

  public void testToArray() {
    // From CAS array to Java array.
    FeatureStructure fs1 = this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_ANNOTATION));
    FeatureStructure fs2 = this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_TOP));
    FeatureStructure fs3 = this.cas.createFS(this.ts.getType(CASTestSetup.TOKEN_TYPE));
    ArrayFS array = this.cas.createArrayFS(3);
    FeatureStructure[] fsArray = array.toArray();
    for (int i = 0; i < 3; i++) {
      assertTrue(fsArray[i] == null);
    }
    array.set(0, fs1);
    array.set(1, fs2);
    array.set(2, fs3);
    fsArray = array.toArray();
    assertTrue(fsArray.length == 3);
    assertTrue(fsArray[0].equals(fs1));
    assertTrue(fsArray[1].equals(fs2));
    assertTrue(fsArray[2].equals(fs3));

    // From Java array to CAS array.
    array = this.cas.createArrayFS(3);
    assertTrue(array.get(0) == null);
    assertTrue(array.get(1) == null);
    assertTrue(array.get(2) == null);
    for (int i = 0; i < 3; i++) {
      array.set(i, fsArray[i]);
    }
    assertTrue(array.get(0).equals(fs1));
    assertTrue(array.get(1).equals(fs2));
    assertTrue(array.get(2).equals(fs3));
    array.set(0, null);
    assertTrue(array.get(0) == null);
  }

  public void testCopyToArray() {
    FeatureStructure fs1 = this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_ANNOTATION));
    FeatureStructure fs2 = this.cas.createFS(this.ts.getType(CAS.TYPE_NAME_TOP));
    FeatureStructure fs3 = this.cas.createFS(this.ts.getType(CASTestSetup.TOKEN_TYPE));
    ArrayFS array = this.cas.createArrayFS(4);
    array.set(0, fs1);
    array.set(1, fs2);
    array.set(2, fs3);
    // We now have an FS array with the last element being null
    final int destinationOffset = 2;
    final int destiniationSize = 10;
    FeatureStructure[] fsArray = new FeatureStructure[destiniationSize];
    String[] stringArray = new String[destiniationSize];
    // Copy to array, skipping first element
    // This must not throw an NPE, see UIMA-726
    array.copyToArray(1, fsArray, destinationOffset, array.size() - 1);
    array.copyToArray(1, stringArray, destinationOffset, array.size() - 1);
    assertTrue(fs2.equals(fsArray[destinationOffset]));
    assertTrue(fs3.equals(fsArray[destinationOffset+1]));
    assertNotNull(stringArray[destinationOffset]);
    assertNotNull(stringArray[destinationOffset+1]);
    for (int i = 0; i < destinationOffset; i++) {
      assertNull(fsArray[i]);
      assertNull(stringArray[i]);
    }
    for (int i = (destinationOffset + 2); i < destiniationSize; i++) {
      assertNull(fsArray[i]);
      assertNull(stringArray[i]);
    }
  }
  
  public void testArraysOfArrays() {
    Type annotationType = this.ts.getType(CAS.TYPE_NAME_ANNOTATION);
    AnnotationFS annot = this.cas.createAnnotation(annotationType, 0, 5);
    IntArrayFS intArray = this.cas.createIntArrayFS(3);
    intArray.set(0, 1);
    intArray.set(1, 2);
    intArray.set(2, -10);
    ArrayFS subArray1 = this.cas.createArrayFS(1);
    ArrayFS subArray2 = this.cas.createArrayFS(2);
    subArray1.set(0, subArray2);
    subArray2.set(1, annot);
    ArrayFS superArray = this.cas.createArrayFS(3);
    superArray.set(0, subArray1);
    superArray.set(1, subArray2);
    superArray.set(2, intArray);
    assertTrue(superArray.get(0).equals(subArray1));
    assertTrue(superArray.get(1).equals(subArray2));
    assertTrue(superArray.get(2).equals(intArray));
    assertTrue(((ArrayFS) superArray.get(0)).get(0).equals(subArray2));
    assertTrue(((ArrayFS) superArray.get(1)).get(0) == null);
    assertTrue(((ArrayFS) superArray.get(1)).get(1).equals(annot));
    assertTrue(((IntArrayFS) superArray.get(2)).get(0) == 1);
    assertTrue(((IntArrayFS) superArray.get(2)).get(1) == 2);
    assertTrue(((IntArrayFS) superArray.get(2)).get(2) == -10);
  }

}
