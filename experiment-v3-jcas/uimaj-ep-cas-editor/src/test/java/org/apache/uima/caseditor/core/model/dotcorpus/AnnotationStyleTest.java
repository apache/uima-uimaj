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

package org.apache.uima.caseditor.core.model.dotcorpus;

import static org.junit.Assert.assertEquals;

import java.awt.Color;

import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.junit.Test;

/**
 * Unit test for the {@link AnnotationStyle} class.
 */
public class AnnotationStyleTest {
  /**
   * Tests the {@link AnnotationStyle#equals(Object)} method.
   */
  @Test
  public void testEquals() {
    AnnotationStyle a = new AnnotationStyle("testType", AnnotationStyle.Style.BRACKET, new Color(
            255, 255, 0), 0);

    AnnotationStyle b = new AnnotationStyle("testType", AnnotationStyle.Style.BRACKET, new Color(
            255, 255, 0), 0);

    assertEquals(a, b);
  }

  /**
   * Test the {@link AnnotationStyle#hashCode()} method.
   *
   */
  public void testHashCode() {
    AnnotationStyle a = new AnnotationStyle("testType", AnnotationStyle.Style.BRACKET, new Color(
            255, 255, 0), 0);

    AnnotationStyle b = new AnnotationStyle("testType", AnnotationStyle.Style.BRACKET, new Color(
            255, 255, 0), 0);

    assertEquals(a.hashCode(), b.hashCode());
  }
}