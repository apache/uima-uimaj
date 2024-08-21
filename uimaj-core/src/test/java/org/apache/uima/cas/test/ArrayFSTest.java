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

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArrayFSTest {

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
  void testSet() {
    // Check that we can't create arrays of size smaller than 0.
    boolean exceptionCaught = false;
    try {
      ArrayFS array = cas.createArrayFS(-1);
      assertThat(array).isNotNull();
    } catch (CASRuntimeException e) {
      exceptionCaught = true;
      assertThat(e.getMessageKey()).isEqualTo(CASRuntimeException.ILLEGAL_ARRAY_SIZE);
    }
    assertThat(exceptionCaught).isTrue();
    ArrayFS array = cas.createArrayFS(0);
    assertThat(array.size()).isEqualTo(0);
    assertThat(array).isNotNull();
    assertThat(array.size()).isEqualTo(0);
    exceptionCaught = false;
    try {
      array.get(0);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertThat(exceptionCaught).isTrue();
    FeatureStructure fs1 = cas.createFS(ts.getType(CAS.TYPE_NAME_ANNOTATION));
    FeatureStructure fs2 = cas.createFS(ts.getType(CAS.TYPE_NAME_TOP));
    FeatureStructure fs3 = cas.createFS(ts.getType(CASTestSetup.TOKEN_TYPE));
    array = cas.createArrayFS(3);
    array.set(0, fs1);
    array.set(1, fs2);
    array.set(2, fs3);
    String[] stringArray = array.toStringArray();
    assertThat(stringArray.length).isEqualTo(3);
    for (int i = 0; i < array.size(); i++) {
      assertThat(stringArray[i]).isNotNull();
    }

    exceptionCaught = false;
    try {
      array.set(-1, fs1);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertThat(exceptionCaught).isTrue();
    exceptionCaught = false;
    try {
      array.set(4, fs1);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertThat(exceptionCaught).isTrue();
    assertThat(fs1).isEqualTo(array.get(0));
    assertThat(fs2).isEqualTo(array.get(1));
    assertThat(fs3).isEqualTo(array.get(2));
    exceptionCaught = false;
    try {
      array.get(-1);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertThat(exceptionCaught).isTrue();
    exceptionCaught = false;
    try {
      array.get(4);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertThat(exceptionCaught).isTrue();
  }

  @Test
  void testToArray() {
    // From CAS array to Java array.
    FeatureStructure fs1 = cas.createFS(ts.getType(CAS.TYPE_NAME_ANNOTATION));
    FeatureStructure fs2 = cas.createFS(ts.getType(CAS.TYPE_NAME_TOP));
    FeatureStructure fs3 = cas.createFS(ts.getType(CASTestSetup.TOKEN_TYPE));
    ArrayFS array = cas.createArrayFS(3);
    FeatureStructure[] fsArray = array.toArray();
    for (int i = 0; i < 3; i++) {
      assertThat(fsArray[i]).isNull();
    }
    array.set(0, fs1);
    array.set(1, fs2);
    array.set(2, fs3);
    fsArray = array.toArray();
    assertThat(fsArray.length).isEqualTo(3);
    assertThat(fs1).isEqualTo(fsArray[0]);
    assertThat(fs2).isEqualTo(fsArray[1]);
    assertThat(fs3).isEqualTo(fsArray[2]);

    // From Java array to CAS array.
    array = cas.createArrayFS(3);
    assertThat(array.get(0)).isNull();
    assertThat(array.get(1)).isNull();
    assertThat(array.get(2)).isNull();
    for (int i = 0; i < 3; i++) {
      array.set(i, fsArray[i]);
    }
    assertThat(fs1).isEqualTo(array.get(0));
    assertThat(fs2).isEqualTo(array.get(1));
    assertThat(fs3).isEqualTo(array.get(2));
    array.set(0, null);
    assertThat(array.get(0)).isNull();
  }

  @Test
  void testCopyToArray() {
    FeatureStructure fs1 = cas.createFS(ts.getType(CAS.TYPE_NAME_ANNOTATION));
    FeatureStructure fs2 = cas.createFS(ts.getType(CAS.TYPE_NAME_TOP));
    FeatureStructure fs3 = cas.createFS(ts.getType(CASTestSetup.TOKEN_TYPE));
    ArrayFS array = cas.createArrayFS(4);
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
    assertThat(fsArray[destinationOffset]).isEqualTo(fs2);
    assertThat(fsArray[destinationOffset + 1]).isEqualTo(fs3);
    assertThat(stringArray[destinationOffset]).isNotNull();
    assertThat(stringArray[destinationOffset + 1]).isNotNull();
    for (int i = 0; i < destinationOffset; i++) {
      assertThat(fsArray[i]).isNull();
      assertThat(stringArray[i]).isNull();
    }
    for (int i = (destinationOffset + 2); i < destiniationSize; i++) {
      assertThat(fsArray[i]).isNull();
      assertThat(stringArray[i]).isNull();
    }
  }

  @Test
  void testArraysOfArrays() {
    Type annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    AnnotationFS annot = cas.createAnnotation(annotationType, 0, 5);
    IntArrayFS intArray = cas.createIntArrayFS(3);
    intArray.set(0, 1);
    intArray.set(1, 2);
    intArray.set(2, -10);
    ArrayFS subArray1 = cas.createArrayFS(1);
    ArrayFS subArray2 = cas.createArrayFS(2);
    subArray1.set(0, subArray2);
    subArray2.set(1, annot);
    ArrayFS superArray = cas.createArrayFS(3);
    superArray.set(0, subArray1);
    superArray.set(1, subArray2);
    superArray.set(2, intArray);
    assertThat(subArray1).isEqualTo(superArray.get(0));
    assertThat(subArray2).isEqualTo(superArray.get(1));
    assertThat(intArray).isEqualTo(superArray.get(2));
    assertThat(subArray2).isEqualTo(((ArrayFS) superArray.get(0)).get(0));
    assertThat(((ArrayFS) superArray.get(1)).get(0)).isNull();
    assertThat(annot).isEqualTo(((ArrayFS) superArray.get(1)).get(1));
    assertThat(((IntArrayFS) superArray.get(2)).get(0)).isEqualTo(1);
    assertThat(((IntArrayFS) superArray.get(2)).get(1)).isEqualTo(2);
    assertThat(((IntArrayFS) superArray.get(2)).get(2)).isEqualTo(-10);
  }
}
