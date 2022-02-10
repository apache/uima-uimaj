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

package org.apache.uima.caseditor.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.caseditor.editor.util.Span;
import org.junit.jupiter.api.Test;

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

    assertThat(a.equals(b)).isTrue();
  }

  /**
   * Test the Span.equals() method.
   */
  @Test
  public void testEqualsWithAnotherObject() {
    Span a = new Span(0, 0);

    assertThat(Boolean.TRUE.equals(a)).isFalse();
  }

  /**
   * Test the Span.equals() method.
   */
  @Test
  public void testEqualsWithNull() {
    Span a = new Span(0, 0);

    assertThat(a.equals(null)).isFalse();
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToWithBiggerSpan() {
    Span a = new Span(100, 1000);
    Span b = new Span(5000, 900);

    assertThat(a.compareTo(b) > 0).isTrue();
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToWithBiggerIntersectSpan() {
    Span a = new Span(100, 1000);
    Span b = new Span(900, 900);

    assertThat(a.compareTo(b) > 0).isTrue();
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToWithLowerSpan() {
    Span a = new Span(5000, 900);
    Span b = new Span(100, 1000);

    assertThat(a.compareTo(b) < 0).isTrue();
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToWithLowerIntersectSpan() {
    Span a = new Span(5000, 900);
    Span b = new Span(4900, 1000);

    assertThat(a.compareTo(b) < 0).isTrue();
  }

  /**
   * Test the Span.compareTo(Object) method.
   */
  @Test
  public void testCompareToEquals() {
    Span a = new Span(4900, 1000);
    Span b = new Span(4900, 1000);

    assertThat(a.compareTo(b) == 0).isTrue();
  }

  /**
   * Test the Span.IsContaining(Span) method.
   */
  @Test
  public void testIsContaining() {
    Span a = new Span(5000, 900);
    Span b = new Span(5200, 600);

    assertThat(a.isContaining(b)).isTrue();
  }

  /**
   * Test the Span.IsContaining(Span) method.
   */
  @Test
  public void testIsContainingWithEqual() {
    Span a = new Span(5000, 900);

    assertThat(a.isContaining(a)).isTrue();
  }

  /**
   * Test the Span.IsContaining(Span) method.
   */
  @Test
  public void testIsContainingWithLowerIntersect() {
    Span a = new Span(5000, 900);
    Span b = new Span(4500, 1000);

    assertThat(a.isContaining(b)).isFalse();
  }

  /**
   * Test the Span.IsContaining(Span) method.
   */
  @Test
  public void testIsContainingWithHigherIntersect() {
    Span a = new Span(5000, 900);
    Span b = new Span(5000, 1000);

    assertThat(a.isContaining(b)).isFalse();
  }
}