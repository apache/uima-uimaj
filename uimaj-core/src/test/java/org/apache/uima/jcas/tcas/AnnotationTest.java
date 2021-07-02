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
package org.apache.uima.jcas.tcas;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AnnotationTest {
  private CAS cas;

  @BeforeEach
  public void setup() throws Exception {
    cas = CasCreationUtils.createCas();
  }

  @Test
  public void thatEmptySpanIsTrimmedToEmptySpan() throws Exception {
    cas.setDocumentText("    ");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 2, 2);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(2, 2, "");
  }

  @Test
  public void thatSpanIsTrimmedToEmptySpanStartingAtOriginalStart() {
    cas.setDocumentText("    ");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 2, 3);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(2, 2, "");
  }

  @Test
  public void thatLeadingAndTrailingWhitespaceIsRemoved() {
    cas.setDocumentText(" ab ");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 4);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(1, 3, "ab");
  }

  @Test
  public void thatInnerWhitespaceIsRemoved1() {
    cas.setDocumentText(" a b ");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 2);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(1, 2, "a");
  }

  @Test
  public void thatInnerWhitespaceIsRemoved2() {
    cas.setDocumentText(" a b ");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 2, 5);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(3, 4, "b");
  }

  @Test
  public void testSingleCharacter() {
    cas.setDocumentText(".");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(0, 1, ".");
  }

  @Test
  public void testLeadingWhitespace() {
    cas.setDocumentText(" \t\n\r.");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 5);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(4, 5, ".");
  }

  @Test
  public void testLeadingWhitespaceWithSurrogates() {
    cas.setDocumentText(" \t\n\r😀");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 6);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(4, 6, "😀");
  }

  @Test
  public void testTrailingWhitespace() {
    cas.setDocumentText(". \n\r\t");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 5);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(0, 1, ".");
  }

  @Test
  public void testTrailingWhitespaceWithSurrogates() {
    cas.setDocumentText("😀 \n\r\t");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 6);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(0, 2, "😀");
  }

  @Test
  public void testLeadingTrailingWhitespace() {
    cas.setDocumentText(" \t\n\r. \n\r\t");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 9);
    ann.trim();

    assertThat(ann).extracting(AnnotationFS::getBegin, AnnotationFS::getEnd).containsExactly(4, 5);
  }

  @Test
  public void testLeadingTrailingWhitespaceWithSurrogatesAndCustomPredicate() {
    // 𝪀 (U+1DA80) is the SIGNWRITING LOCATION-FLOORPLANE SPACE. It is not recognized by
    // Character.isWhitespace(...), so we use a custom predicate to filter it out
    cas.setDocumentText(" \t𝪀\n\r. \n𝪀\r\t");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 0, 9);
    ann.trim(codepoint -> Character.isWhitespace(codepoint) || 0x1DA80 == codepoint);

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(6, 7, ".");
  }

  @Test
  public void testBlankString() {
    cas.setDocumentText("   ");

    AnnotationFS ann = cas.createAnnotation(cas.getAnnotationType(), 1, 2);
    ann.trim();

    assertThat(ann)
            .extracting(AnnotationFS::getBegin, AnnotationFS::getEnd, AnnotationFS::getCoveredText)
            .containsExactly(1, 1, "");
  }
}
