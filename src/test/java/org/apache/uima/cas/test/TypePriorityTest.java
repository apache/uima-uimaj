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

import java.util.Properties;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;

/**
 * tests of type priorities
 * 
 * These tests set up various type priorities, and then test that the total linear order gives the
 * expected type order
 * 
 * Types: For this tests, we use types whose names encode their position in a type tree.
 * 
 * Encoding: Each type name has a root based on a single letter, e.g. a, b, c name of type is its
 * root preceeded by its ancestors to the top
 */
public class TypePriorityTest extends TestCase {

  public static final Properties casCreateProperties = new Properties();
  static {
    casCreateProperties.setProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE, "200");
  }

  private CASMgr casMgr;

  private CAS cas;

  private FSIndexRepositoryMgr irm;

  private TypeSystem ts;

  public TypePriorityTest(String arg) {
    super(arg);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    casMgr = initCAS();
    cas = casMgr.getCAS().getCurrentView();
    irm = casMgr.getIndexRepositoryMgr();
    ts = cas.getTypeSystem();
  }

  public void tearDown() {
    casMgr = null;
    cas = null;
    irm = null;
    ts = null;
  }

  private String[] makeTypeName(String[] roots) {
    String[] r = new String[roots.length * 2];
    for (int i = 0; i < roots.length; i++) {
      r[2 * i] = roots[i] + "a";
      r[2 * i + 1] = roots[i] + "b";
    }
    return r;
  }

  private String[] makeTypeName(int level) {
    if (level == 0)
      return new String[] { "" };
    return makeTypeName(makeTypeName(level - 1));
  }

  private void addTypesForLevel(TypeSystemMgr tsm, int level) {
    int rootLevel = level - 1;
    if (rootLevel >= 0)
      addTypesForLevel(tsm, rootLevel);

    String[] roots = makeTypeName(level);
    for (int i = 0; i < roots.length; i++) {
      Type parent = roots[i].equals("") ? tsm.getTopType() : tsm.getType(roots[i]);
      tsm.addType(roots[i] + "a", parent);
      tsm.addType(roots[i] + "b", parent);
      // System.out.println("added type " + roots[i] + "a and b");
    }
    // special type for another test
    tsm.addType("ac", tsm.getType("a"));
  }

  // Initialize the first CAS.
  private CASMgr initCAS() {
    // Create a CASMgr. Ensures existence of AnnotationFS type.

    CASMgr localCas = CASFactory.createCAS(200);
    // Create a writable type system.
    TypeSystemMgr tsa = localCas.getTypeSystemMgr();
    // Add new types and features.

    addTypesForLevel(tsa, 4);

    // Commit the type system.
    ((CASImpl) localCas).commitTypeSystem();
    try {
      localCas.initCASIndexes();
    } catch (CASException e2) {
      e2.printStackTrace();
      assertTrue(false);
    }

    localCas.getIndexRepositoryMgr().commit();
    // assert(cas.getIndexRepositoryMgr().isCommitted());
    return localCas;
  }

  private void check(LinearTypeOrder lo, String[] o) {
    for (int i = 0; i < o.length - 1; i++) {
      check(lo, o[i], o[i + 1]);
    }
  }

  private void checkBackwards(LinearTypeOrder lo, String[] o) {
    for (int i = 0; i < o.length - 1; i++) {
      checkBackwards(lo, o[i], o[i + 1]);
    }
  }

  private void check(LinearTypeOrder lo, String t1, String t2) {
    assertTrue(lo.lessThan(ts.getType(t1), ts.getType(t2)));
  }

  private void checkBackwards(LinearTypeOrder lo, String t1, String t2) {
    assertFalse(lo.lessThan(ts.getType(t1), ts.getType(t2)));
  }

  /*
   * Diagram to figure out what the answers should be 
   *                                                a                                                    b
   *                        aa                                             ab                           ba ... 
   *            aaa                     aab                   aba                     abb              baa ... 
   *     aaaa        aaab        aaba        aabb       abaa        abab        abba        abbb      baaa ...
   * aaaaa aaaab aaaba aaabb aabaa aabab aabba aabbb abaaa abaab ababa ababb abbaa abbab abbba abbbb baaaa ...
   */
  /**
   * Test driver.
   */
  public void testMain() throws Exception {
    LinearTypeOrderBuilder order = irm.createTypeSortOrder();
    order = irm.createTypeSortOrder();
    LinearTypeOrder lo;
    try {
      order.add(new String[] { "aaa", "bbb" });
      lo = order.getOrder();
      check(lo, "aaa", "bbb");
    } catch (CASException e) {
      assertTrue(false);
    }
  }

  public void testN1() throws Exception {
    LinearTypeOrderBuilder order = irm.createTypeSortOrder();
    order = irm.createTypeSortOrder();
    LinearTypeOrder lo;
    try {
      // aaa (and all its subtypes) come before bbb (and all its subtypes)
      order.add(new String[] { "aaa", "bbb" });
      // aa (and all its subtypes) come before 
      //   abaa (and all its subtypes) come before
      //     abbbb (and all its subtypes)
      order.add(new String[] { "aa", "abaa", "abbbb" });
      lo = order.getOrder();
      check(lo, new String[] { "aa", "abaa", "abbbb" });
      check(lo, "aaa", "bbb");

      check(lo, "aaa", "abaaa"); 
      check(lo, "aaab", "abaab");  
      check(lo, "aa", "abbbb");
      check(lo, "abaa", "abbbb");
    } catch (CASException e) {
      assertTrue(false);
    }
  }

  public void testLoop2() throws Exception {
    try {
      LinearTypeOrderBuilder obuilder;
      LinearTypeOrder order;
      obuilder = irm.createTypeSortOrder();
      obuilder.add(new String[] { "a", "b" });
      check(order = obuilder.getOrder(), "aa", "bb");
      checkBackwards(order, "bb", "aa");
      check(order, "ab", "bb");
      check(order, "aa", "ba");
      check(order, "ab", "ba");

      obuilder = irm.createTypeSortOrder();
      obuilder.add(new String[] { "b", "a" });
      check(order = obuilder.getOrder(), "ba", "aa");
      check(order, "bb", "aa");
      check(order, "ba", "ab");
      check(order, "bb", "ab");

      obuilder = irm.createTypeSortOrder();
      obuilder.add(new String[] { "a", "b" });
      obuilder.add(new String[] { "bb", "a" });
      check(order = obuilder.getOrder(), "bb", "aa"); // these two forced to reverse
      check(order, "bb", "ab"); // these two forced to reverse
      check(order, "aa", "ba");
      check(order, "ab", "ba");

      // set a child in front of the sib of its parent,
      // and one in front of the child of its prev sib.
      obuilder = irm.createTypeSortOrder();
      obuilder.add(new String[] { "aba", "aa" });
      obuilder.add(new String[] { "b", "aa" });
      check(order = obuilder.getOrder(), "aba", "aa");
      check(order, "bb", "aa"); // child of b dragged along?
      check(order, "b", "aaa"); // child of aa dragged?

      // siblings children dragged
      obuilder = irm.createTypeSortOrder();
      obuilder.add(new String[] { "ab", "aa" });
      obuilder.add(new String[] { "b", "aa" });
      check(order = obuilder.getOrder(), "ab", "aa");
      check(order, "b", "aa");
      check(order, "bb", "aa"); // child of b dragged along?
      check(order, "b", "aaa"); // child of aa dragged?

      // siblings children dragged
      obuilder = irm.createTypeSortOrder();
      obuilder.add(new String[] { "ab", "aa" });
      obuilder.add(new String[] { "b", "aa" });
      obuilder.add(new String[] { "a", "b" });

      check(order = obuilder.getOrder(), "ab", "aa");
      check(order, "b", "aa");
      check(order, "bb", "aa"); // child of b dragged along?
      check(order, "b", "aaa"); // child of aa dragged?
      check(order, "ac", "b");

    } catch (CASException e) {
      assertTrue(false);
    }
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(TypePriorityTest.class);
  }

}
