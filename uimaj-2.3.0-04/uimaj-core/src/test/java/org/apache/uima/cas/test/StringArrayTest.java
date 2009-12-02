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
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelCAS;

/**
 * Class comment for StringArrayTest.java goes here.
 * 
 */
public class StringArrayTest extends TestCase {

  private CAS cas;

  private TypeSystem ts;

  /**
   * Constructor for ArrayFSTest.
   * 
   * @param arg0
   */
  public StringArrayTest(String arg0) {
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
    junit.textui.TestRunner.run(StringArrayTest.class);
  }

  public void testSet() {
    StringArrayFS array = this.cas.createStringArrayFS(0);
    assertTrue(array != null);
    assertTrue(array.size() == 0);
    boolean exceptionCaught = false;
    try {
      array.get(0);
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    array = this.cas.createStringArrayFS(3);
    try {
      array.set(0, "1");
      array.set(1, "2");
      array.set(2, "3");
    } catch (ArrayIndexOutOfBoundsException e) {
      assertTrue(false);
    }
    String[] stringArray = array.toStringArray();
    assertTrue(array.size() == stringArray.length);
    for (int i = 0; i < stringArray.length; i++) {
      assertTrue(stringArray[i].equals(array.get(i)));
    }
    exceptionCaught = false;
    try {
      array.set(-1, "1");
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    exceptionCaught = false;
    try {
      array.set(4, "1");
    } catch (ArrayIndexOutOfBoundsException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
    assertTrue(array.get(0).equals("1"));
    assertTrue(array.get(1).equals("2"));
    assertTrue(array.get(2).equals("3"));
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
      array = this.cas.createStringArrayFS(-1);
    } catch (CASRuntimeException e) {
      exceptionCaught = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.ILLEGAL_ARRAY_SIZE));
    }
    assertTrue(exceptionCaught);
  }

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

  public void testStringArrayValue() {
    String lemmaListName = CASTestSetup.TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
	+ CASTestSetup.LEMMA_LIST_FEAT;
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
  

  public void testStringArrayNullValue() throws Exception{
     String lemmaListName = CASTestSetup.TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
    + CASTestSetup.LEMMA_LIST_FEAT;
     final Feature lemmaList = this.ts.getFeatureByFullName(lemmaListName);
     assertTrue(lemmaList != null);
     StringArrayFS casArray = this.cas.createStringArrayFS(3);
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
     assertTrue(llc.ll_getIntArrayValue(llc.ll_getFSRef(casArray), 1) == LowLevelCAS.NULL_FS_REF);
  }

}
