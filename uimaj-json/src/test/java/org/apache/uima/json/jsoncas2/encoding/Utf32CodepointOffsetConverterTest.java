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
package org.apache.uima.json.jsoncas2.encoding;

import static java.lang.Character.charCount;
import static java.lang.Character.getName;
import static java.lang.Character.toChars;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.javacrumbs.jsonunit.jsonpath.JsonPathAdapter.inPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.datasuites.ProgrammaticallyCreatedCasDataSuite;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.json.jsoncas2.JsonCas2Serializer;
import org.apache.uima.json.jsoncas2.mode.OffsetConversionMode;
import org.junit.jupiter.api.Test;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class Utf32CodepointOffsetConverterTest {
  @Test
  public void thatMappingExternalToInternalWorks() {
    int[] externalToInternal = {
        // Smily
        0,
        // " This "
        2, 3, 4, 5, 6, 7,
        // Female light-skinned turban emoji
        8, 10, 12, 13, 14,
        // " is "
        15, 16, 17, 18,
        // Telephone sign
        19,
        // " a "
        20, 21, 22,
        // Dark skinned bearded emoji
        23, 25, 27, 28, 29,
        // " test "
        30, 31, 32, 33, 34, 35,
        // Ghost emoji
        36,
        // EOS
        38 };
    String text = "🥳 This 👳🏻‍♀️ is ✆ a 🧔🏾‍♂️ test 👻";
    Utf32CodepointOffsetConverter conv = new Utf32CodepointOffsetConverter(text);
    for (int n = 0; n < externalToInternal.length; n++) {
      int mn = externalToInternal[n];
      if (mn < text.length()) {
        int cp = text.codePointAt(mn);
        int w = charCount(cp);
        System.out.printf("%2d->%2d: U+%05X (%d) %s %s%n", n, mn, cp, w,
                String.valueOf(toChars(cp)), getName(cp));
      } else {
        System.out.printf("%2d->%2d: EOS%n", n, mn);
      }
      assertThat(conv.mapExternal(n)).isEqualTo(mn);
    }
  }

  @Test
  public void thatMappingInternalToExternalWorks() {
    final int U = Utf32CodepointOffsetConverter.UNMAPPED;
    int[] internalToExternal = {
        // Smily
        0, U,
        // " This "
        1, 2, 3, 4, 5, 6,
        // Female light-skinned turban emoji
        7, U, 8, U, 9, 10, 11,
        // " is "
        12, 13, 14, 15,
        // Telephone sign
        16,
        // " a "
        17, 18, 19,
        // Dark skinned bearded emoji
        20, U, 21, U, 22, 23, 24,
        // " test "
        25, 26, 27, 28, 29, 30,
        // Ghost emoji
        31, U,
        // EOS
        32 };
    String text = "🥳 This 👳🏻‍♀️ is ✆ a 🧔🏾‍♂️ test 👻";
    int cpl = text.codePointCount(0, text.length());
    Utf32CodepointOffsetConverter conv = new Utf32CodepointOffsetConverter(text);
    for (int n = 0; n < internalToExternal.length; n++) {
      int mn = internalToExternal[n];
      if (mn < cpl) {
        if (mn != Utf32CodepointOffsetConverter.UNMAPPED) {
          int cp = text.codePointAt(n);
          int w = Character.charCount(cp);
          // System.out.printf("%2d->%2d: U+%05X (%d) %s %s%n", n, mn, cp, w,
          // String.valueOf(toChars(cp)), getName(cp));
        } else {
          // System.out.printf("%2d->%2d: EOS%n", n, mn);
        }
      }

      assertThat(conv.mapInternal(n)).isEqualTo(mn);
    }
  }

  @Test
  public void thatSerializationWithMappingWorks() throws Exception {
    JsonCas2Serializer ser = new JsonCas2Serializer();
    ser.setOffsetConversionMode(OffsetConversionMode.UTF_32);

    CAS cas = ProgrammaticallyCreatedCasDataSuite.casWithEmojiUnicodeTextAndAnnotations();

    String casJson;
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      ser.serialize(cas, os);
      casJson = new String(os.toByteArray(), UTF_8);
    }

    String expected = String.join(",",
            "{'%ID':1,'%TYPE':'uima.cas.Sofa','sofaNum':1,'sofaID':'_InitialView','mimeType':'text','sofaString':'🥳 This 👳🏻‍♀️ is ✆ a 🧔🏾‍♂️ test 👻'}",
            "{'%ID':2,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':0,'end':1}",
            "{'%ID':3,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':2,'end':6}",
            "{'%ID':4,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':7,'end':12}",
            "{'%ID':5,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':13,'end':15}",
            "{'%ID':6,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':16,'end':17}",
            "{'%ID':7,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':18,'end':19}",
            "{'%ID':8,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':20,'end':25}",
            "{'%ID':9,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':26,'end':30}",
            "{'%ID':10,'%TYPE':'uima.tcas.Annotation','@sofa':1,'begin':31,'end':32}",
            "{'%ID':11,'%TYPE':'uima.tcas.DocumentAnnotation','@sofa':1,'begin':0,'end':32,'language':'x-unspecified'}");
    expected = "[" + expected + "]";

    JsonAssertions.assertThatJson(inPath(casJson, "$.%FEATURE_STRUCTURES[*])")) //
            .isEqualTo(expected);
  }

  private static void createAnnotatedText(CAS aCas, StringBuilder aBuffer, String aText,
          String... aSuffix) {
    int begin = aBuffer.length();
    aBuffer.append(aText);
    AnnotationFS a = aCas.createAnnotation(aCas.getAnnotationType(), begin, aBuffer.length());
    aCas.addFsToIndexes(a);
    for (String s : aSuffix) {
      aBuffer.append(s);
    }
  }
}
