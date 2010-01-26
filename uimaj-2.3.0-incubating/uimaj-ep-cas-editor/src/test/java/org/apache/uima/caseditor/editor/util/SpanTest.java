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

package org.apache.uima.caseditor.editor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * Unit test for the <code>Span</code> class.
 */
public class SpanTest {
  /**
   * Test the Span.equals() method.
   */
  @Test
  public void testEquals() {
    Span a = new Span(100, 1000);
    Span b = new Span(100, 1000);

    assertEquals(a.equals(b), true);
  }

  /**
   * Test the Span.equals() method.
   */
  @Test
  public void testEqualsWithAnotherObject() {
    Span a = new Span(0, 0);

    assertFalse(Boolean.TRUE.equals(a));
  }

  /**
   * Test the Span.equals() method.
   */
  @Test
  public void testEqualsWithNull() {
    Span a = new Span(0, 0);

    assertEquals(a.equals(null), false);
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToWithBiggerSpan() {
    Span a = new Span(100, 1000);
    Span b = new Span(5000, 900);

    assertEquals(true, a.compareTo(b) > 0);
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToWithBiggerIntersectSpan() {
    Span a = new Span(100, 1000);
    Span b = new Span(900, 900);

    assertEquals(true, a.compareTo(b) > 0);
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToWithLowerSpan() {
    Span a = new Span(5000, 900);
    Span b = new Span(100, 1000);

    assertEquals(true, a.compareTo(b) < 0);
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToWithLowerIntersectSpan() {
    Span a = new Span(5000, 900);
    Span b = new Span(4900, 1000);

    assertEquals(true, a.compareTo(b) < 0);
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToEquals() {
    Span a = new Span(4900, 1000);
    Span b = new Span(4900, 1000);

    assertEquals(true, a.compareTo(b) == 0);
  }

  /**
   * Test the Span.IsContaining(Span) method.
   */
  @Test
  public void testIsContaining() {
    Span a = new Span(5000, 900);
    Span b = new Span(5200, 600);

    assertEquals(true, a.isContaining(b));
  }

  /**
   * Test the Span.IsContaining(Span) method.
   */
  @Test
  public void testIsContainingWithEqual() {
    Span a = new Span(5000, 900);

    assertEquals(true, a.isContaining(a));
  }

  /**
   * Test the Span.IsContaining(Span) method.
   */
  @Test
  public void testIsContainingWithLowerIntersect() {
    Span a = new Span(5000, 900);
    Span b = new Span(4500, 1000);

    assertEquals(false, a.isContaining(b));
  }

  /**
   * Test the Span.IsContaining(Span) method.
   */
  @Test
  public void testIsContainingWithHigherIntersect() {
    Span a = new Span(5000, 900);
    Span b = new Span(5000, 1000);

    assertEquals(false, a.isContaining(b));
  }
}
