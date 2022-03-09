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

import static org.apache.uima.cas.TypeSystem.FEATURE_SEPARATOR;
import static org.apache.uima.cas.test.CASTestSetup.LEMMA_LIST_FEAT;
import static org.apache.uima.cas.test.CASTestSetup.TOKEN_TYPE;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertTrue;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class comment for StringArrayTest.java goes here.
 * 
 */
public class StringArrayTest {

  private CAS cas;

  private TypeSystem ts;

  @BeforeEach
  public void setUp() throws Exception {
    this.cas = CASInitializer.initCas(new CASTestSetup(), null);
    this.ts = this.cas.getTypeSystem();
  }

  @AfterEach
  public void tearDown() {
    this.cas = null;
    this.ts = null;
  }

  @Test
  public void testSet() {
    StringArrayFS array = this.cas.createStringArrayFS(0);
    assertTrue(array != null);
    assertTrue(array.size() == 0);
    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class) //
            .isThrownBy(() -> array.get(0));

    StringArrayFS array2 = this.cas.createStringArrayFS(3);
    array2.set(0, "1");
    array2.set(1, "2");
    array2.set(2, "3");

    String[] stringArray = array2.toStringArray();
    assertTrue(array2.size() == stringArray.length);
    for (int i = 0; i < stringArray.length; i++) {
      assertTrue(stringArray[i].equals(array2.get(i)));
    }

    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class) //
            .as("Cannot set value at index < 0") //
            .isThrownBy(() -> array2.set(-1, "1"));
    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class) //
            .as("Cannot set value at index beyond end of the array") //
            .isThrownBy(() -> array2.set(4, "1"));

    assertTrue(array2.get(0).equals("1"));
    assertTrue(array2.get(1).equals("2"));
    assertTrue(array2.get(2).equals("3"));

    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class) //
            .as("Cannot get value at index < 0") //
            .isThrownBy(() -> array2.get(-1));

    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class) //
            .as("Cannot get value at index beyond end of the array") //
            .isThrownBy(() -> array2.get(4));

    assertThatExceptionOfType(CASRuntimeException.class) //
            .as("We can't create arrays smaller than 0")
            .isThrownBy(() -> this.cas.createStringArrayFS(-1)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.ILLEGAL_ARRAY_SIZE);
  }

  @Test
  public void testToArray() {
    // From CAS array to Java array.
    StringArrayFS array = this.cas.createStringArrayFS(3);
    String[] fsArray = array.toArray();
    for (int i = 0; i < 3; i++) {
      assertTrue(fsArray[i] == null);
    }
    array.set(0, "1");
    array.set(1, "2");
    array.set(2, "3");
    fsArray = array.toArray();
    assertTrue(fsArray.length == 3);
    assertTrue(fsArray[0].equals("1"));
    assertTrue(fsArray[1].equals("2"));
    assertTrue(fsArray[2].equals("3"));

    // From Java array to CAS array.
    array = this.cas.createStringArrayFS(3);
    assertTrue(array.get(0) == null);
    assertTrue(array.get(1) == null);
    assertTrue(array.get(2) == null);
    for (int i = 0; i < 3; i++) {
      array.set(i, fsArray[i]);
    }
    assertTrue(array.get(0).equals("1"));
    assertTrue(array.get(1).equals("2"));
    assertTrue(array.get(2).equals("3"));
    array.set(0, null);
    assertTrue(array.get(0) == null);
  }

  @Test
  public void testStringArrayValue() {
    String lemmaListName = TOKEN_TYPE + FEATURE_SEPARATOR + LEMMA_LIST_FEAT;
    final Feature lemmaList = this.ts.getFeatureByFullName(lemmaListName);
    assertTrue(lemmaList != null);
    String[] javaArray = { "1", "2", "3" };
    StringArrayFS casArray = this.cas.createStringArrayFS(3);
    casArray.copyFromArray(javaArray, 0, 0, 3);
    FeatureStructure token = this.cas.createFS(this.ts.getType(CASTestSetup.TOKEN_TYPE));
    assertTrue(token.getFeatureValue(lemmaList) == null);
    token.setFeatureValue(lemmaList, casArray);
    assertTrue(((StringArrayFS) token.getFeatureValue(lemmaList)).get(0) == "1");
    String hello = "Hello.";
    casArray.set(0, hello);
    assertTrue(((StringArrayFS) token.getFeatureValue(lemmaList)).get(0) == hello);
  }

  @Test
  public void testStringArrayNullValue() throws Exception {
    String lemmaListName = TOKEN_TYPE + FEATURE_SEPARATOR + LEMMA_LIST_FEAT;
    final Feature lemmaList = this.ts.getFeatureByFullName(lemmaListName);
    assertTrue(lemmaList != null);
    StringArrayFS casArray = this.cas.createStringArrayFS(3);
    ((CASImpl) (casArray.getCAS())).setId2FSsMaybeUnconditionally(casArray);
    casArray.set(0, "1");
    casArray.set(1, null);
    casArray.set(2, "3");
    FeatureStructure token = this.cas.createFS(this.ts.getType(CASTestSetup.TOKEN_TYPE));
    assertTrue(token.getFeatureValue(lemmaList) == null);
    token.setFeatureValue(lemmaList, casArray);
    this.cas.addFsToIndexes(token);
    assertTrue(((StringArrayFS) token.getFeatureValue(lemmaList)).get(0) == "1");
    assertTrue(((StringArrayFS) token.getFeatureValue(lemmaList)).get(1) == null);
    LowLevelCAS llc = casArray.getCAS().getLowLevelCAS();
    assertTrue(llc.ll_getStringArrayValue(llc.ll_getFSRef(casArray), 1) == null);
  }
}
